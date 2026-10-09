// KeiOS-owned controls/rendering. Three.js and its addons retain their MIT notice in vendor/LICENSE.
import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {OrbitControls} from 'three/addons/controls/OrbitControls.js';
import {createModelParts} from './model-parts.js';
import {modelBackgroundColor} from './model-background.js';
import {createSkinBounds} from './model-skin-bounds.js';

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(35, 1, 0.01, 1000);
const renderer = new THREE.WebGLRenderer({alpha:false, antialias:true});
const presentation = window.keiosCreateFramePresenter?.(renderer.getContext());
let background = modelBackgroundColor('#' + new URLSearchParams(location.hash.slice(1)).get('background')) || '#0c1424';
renderer.setClearColor(background);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.NoToneMapping;
renderer.setPixelRatio(window.devicePixelRatio || 1);
document.body.appendChild(renderer.domElement);
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.screenSpacePanning = true;
controls.touches.ONE = THREE.TOUCH.ROTATE;
controls.touches.TWO = THREE.TOUCH.DOLLY_PAN;
// OrbitControls' damping is per update, including pointer-event updates. On a slow
// drawing-buffer transport, the same inertia otherwise takes much longer to settle.
const updateCamera = controls.update.bind(controls);
let cameraUpdatedAt = performance.now();
controls.update = () => {
  const now = performance.now(), elapsed = Math.max(0, now - cameraUpdatedAt);
  cameraUpdatedAt = now;
  controls.dampingFactor = 1 - Math.pow(0.95, elapsed * 60 / 1000);
  return updateCamera();
};
const loader = new GLTFLoader();
let root = null, mixer = null, action = null, clips = [], selected = '', ready = false, error = '';
let foreground = true, playing = true, generation = 0, abort = null;
let loopEnabled = true, speed = 1, outlineEnabled = true, outlineWidth = 0.25, scrubbing = false, ended = false;
let loadedUrl = '';
let parts = null;
const clock = new THREE.Timer();

function disposeScene(object) {
  if (object) {
    scene.remove(object);
    const geometry = new Set(), materials = new Set(), textures = new Set();
    object.traverse(o => {
      if (o.geometry) geometry.add(o.geometry);
      for (const material of (Array.isArray(o.material) ? o.material : [o.material]).filter(Boolean)) {
        materials.add(material);
        for (const value of Object.values(material)) if (value?.isTexture) textures.add(value);
      }
    });
    textures.forEach(t => { t.source?.data?.close?.(); t.dispose(); });
    materials.forEach(m => m.dispose()); geometry.forEach(g => g.dispose());
  }
}
function disposeModel() {
  if (mixer && root) { mixer.stopAllAction(); mixer.uncacheRoot(root); }
  disposeScene(root);
  root = null; mixer = null; action = null; clips = []; parts = null; selected = ''; ready = false;
}

function prepareMaterials(model) {
  const meshes = [];
  model.traverse(o => { if (o.isMesh) meshes.push(o); if (o.userData.optional) o.visible = false; });
  for (const mesh of meshes) {
    const sources = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
    const materials = sources.map(m => new THREE.MeshBasicMaterial({
      map:m.map, color:m.color, transparent:m.transparent, opacity:m.opacity,
      alphaTest:m.alphaTest, side:m.side, depthWrite:m.depthWrite,
    }));
    mesh.material = Array.isArray(mesh.material) ? materials : materials[0];
    sources.forEach(m => m.dispose());
    const outline = mesh.clone();
    outline.children = [];
    outline.userData = {...outline.userData, outline:true};
    const outlines = sources.map(m => new THREE.ShaderMaterial({side:THREE.BackSide, transparent:m.transparent,
      uniforms:{width:{value:outlineWidth * 0.006}, alphaMap:{value:m.map},
        useAlphaMap:{value:!!m.map && (m.alphaTest > 0 || m.transparent)},
        alphaCutoff:{value:Math.max(m.alphaTest || 0, 0.01)}, opacity:{value:m.opacity}},
      vertexShader:`
        uniform float width;
        varying vec2 vModelUv;
        #include <common>
        #include <morphtarget_pars_vertex>
        #include <skinning_pars_vertex>
        void main() {
          vModelUv = uv;
          #include <beginnormal_vertex>
          #include <morphnormal_vertex>
          #include <skinbase_vertex>
          #include <skinnormal_vertex>
          #include <begin_vertex>
          #include <morphtarget_vertex>
          #include <skinning_vertex>
          transformed += normalize(objectNormal) * width;
          #include <project_vertex>
        }`,
      fragmentShader:`
        uniform sampler2D alphaMap;
        uniform bool useAlphaMap;
        uniform float alphaCutoff;
        uniform float opacity;
        varying vec2 vModelUv;
        void main(){
          float alpha = opacity * (useAlphaMap ? texture2D(alphaMap,vModelUv).a : 1.0);
          if (alpha < alphaCutoff) discard;
          gl_FragColor=vec4(0.04,0.05,0.08,alpha);
        }`,
    }));
    // Preserve per-primitive alpha masks; a solid outline would fill transparent halos and props.
    outline.material = Array.isArray(mesh.material) ? outlines : outlines[0];
    outline.visible = outlineEnabled;
    // Shared skeleton/morph weights follow the animated mesh; camera and controls are independent.
    if (mesh.isSkinnedMesh) { outline.skeleton = mesh.skeleton; outline.bindMatrix.copy(mesh.bindMatrix); outline.bindMatrixInverse.copy(mesh.bindMatrixInverse); }
    outline.morphTargetInfluences = mesh.morphTargetInfluences;
    mesh.add(outline);
    outline.position.set(0,0,0); outline.quaternion.identity(); outline.scale.set(1,1,1);
  }
}

function resetCamera() {
  if (!root) return;
  // Flush the previous gesture's inertia before placing the saved view. Otherwise a
  // reset during deceleration continues drifting away from its newly framed position.
  controls.enableDamping = false; updateCamera(); controls.enableDamping = true;
  cameraUpdatedAt = performance.now();
  root.updateMatrixWorld(true);
  const box = new THREE.Box3();
  root.traverseVisible(mesh => {
    if (!mesh.isMesh || mesh.userData.outline) return;
    // Hidden optional props and duplicate outlines must not enlarge the initial framing.
    if (mesh.isSkinnedMesh) { mesh.computeBoundingBox(); box.union(mesh.boundingBox.clone().applyMatrix4(mesh.matrixWorld)); }
    else { mesh.geometry.computeBoundingBox(); box.union(mesh.geometry.boundingBox.clone().applyMatrix4(mesh.matrixWorld)); }
  });
  if (box.isEmpty()) return;
  const center = box.getCenter(new THREE.Vector3()), size = box.getSize(new THREE.Vector3());
  const distance = Math.max(size.y, size.x / camera.aspect) / (2 * Math.tan(THREE.MathUtils.degToRad(camera.fov/2))) * 1.35 + size.z / 2;
  camera.position.set(center.x, center.y + distance * Math.sin(THREE.MathUtils.degToRad(5)), center.z + distance);
  camera.near = Math.max(distance / 1000, 0.001); camera.far = Math.max(distance * 100, 100);
  controls.target.copy(center); controls.minDistance = distance * 0.15; controls.maxDistance = distance * 8;
  camera.updateProjectionMatrix(); controls.update(); controls.saveState();
}

function selectAnimation(name) {
  const clip = clips.find(c => c.name === name);
  if (!clip || !mixer) return;
  parts?.apply(clip.userData || {});
  action?.stop(); action = mixer.clipAction(clip);
  action.setLoop(loopEnabled ? THREE.LoopRepeat : THREE.LoopOnce, loopEnabled ? Infinity : 1);
  action.clampWhenFinished = !loopEnabled;
  action.reset().play(); action.timeScale = speed; action.paused = !playing || scrubbing;
  selected = clip.name; ended = false;
  mixer.update(0); compatibleLoop?.request();
}

function setOptions(options) {
  speed = Math.max(0.25, Math.min(2, options.speed)); loopEnabled = !!options.loop;
  outlineEnabled = !!options.outline; outlineWidth = Math.max(0, Math.min(1, options.outlineWidth));
  if (scrubbing !== !!options.scrubbing) clock.reset();
  scrubbing = !!options.scrubbing;
  if (action) {
    action.timeScale = speed; action.setLoop(loopEnabled ? THREE.LoopRepeat : THREE.LoopOnce, loopEnabled ? Infinity : 1);
    action.clampWhenFinished = !loopEnabled; action.paused = !playing || scrubbing;
  }
  root?.traverse(o => { if (o.userData.outline) {
    o.visible=outlineEnabled;
    for (const m of (Array.isArray(o.material) ? o.material : [o.material])) m.uniforms.width.value=outlineWidth*0.006;
  } });
  compatibleLoop?.request();
}

function seek(seconds) {
  if (!action || !mixer) return;
  action.time = Math.max(0, Math.min(action.getClip().duration, seconds));
  action.enabled = true; ended = false; mixer.update(0); compatibleLoop?.request();
}

async function load(config) {
  const token = ++generation;
  abort?.abort(); abort = new AbortController(); error = ''; disposeModel(); loadedUrl = config.url; ended = false;
  presentation?.begin();
  try {
    const response = await fetch(config.url, {signal:abort.signal, cache:'no-store'});
    if (!response.ok) throw new Error(`Model HTTP ${response.status}`);
    const data = await response.arrayBuffer();
    const gltf = await loader.parseAsync(data, './');
    if (token !== generation) { disposeScene(gltf.scene); return; }
    root = gltf.scene; parts = createModelParts(root); parts.apply(); prepareMaterials(root); scene.add(root);
    clips = gltf.animations; mixer = new THREE.AnimationMixer(root);
    mixer.addEventListener('finished', event => { if (event.action === action) { playing=false; ended=true; compatibleLoop?.stop(); } });
    selectAnimation(clips.find(c=>c.name===config.defaultAnimation)?.name || clips.find(c=>c.name==='Cafe_Reaction')?.name || clips.find(c=>c.name==='Idle')?.name || clips[0]?.name);
    mixer.update(0); resetCamera(); ready = true; compatibleLoop?.request();
  } catch (e) { if (token === generation && e.name !== 'AbortError') error = String(e.message || e); }
}

function resize() {
  const w = Math.max(innerWidth,1), h = Math.max(innerHeight,1);
  // Android WebView can resolve percentage body heights to zero even with a valid viewport.
  document.documentElement.style.height = document.body.style.height = `${h}px`;
  renderer.setSize(w,h); camera.aspect=w/h; camera.updateProjectionMatrix();
}
addEventListener('resize', resize); resize();
let regionExtent = {width:0,height:0,canvasWidth:0,canvasHeight:0,shrinkAt:0}, lastRegion = null;
let cropMs = 0, renderMs = 0;
const meshBox = new THREE.Box3(), projected = new THREE.Vector3();
const skinBounds = createSkinBounds();
function updateModelBounds() {
  if (!root) return;
  root.updateMatrixWorld(true);
  root.traverseVisible(mesh => {
    if (!mesh.isSkinnedMesh || mesh.userData.outline) return;
    mesh.boundingBox ||= new THREE.Box3();
    skinBounds(mesh, mesh.boundingBox);
    mesh.boundingSphere ||= new THREE.Sphere();
    mesh.boundingBox.getBoundingSphere(mesh.boundingSphere);
    mesh.boundingSphere.radius += outlineEnabled ? outlineWidth * 0.006 : 0;
    for (const child of mesh.children) if (child.userData.outline) child.boundingSphere = mesh.boundingSphere;
  });
}
function presentationRegion() {
  if (!root) return null;
  let left=Infinity, right=-Infinity, bottom=Infinity, top=-Infinity, nearIntersection=false;
  root.traverseVisible(mesh => {
    if (!mesh.isMesh || mesh.userData.outline) return;
    if (mesh.isSkinnedMesh) meshBox.copy(mesh.boundingBox);
    else { if (!mesh.geometry.boundingBox) mesh.geometry.computeBoundingBox(); meshBox.copy(mesh.geometry.boundingBox); }
    // Project each mesh's object-space box directly. A merged world-axis box becomes
    // unnecessarily huge while orbiting, even when the visible character remains small.
    if (outlineEnabled) meshBox.expandByScalar(outlineWidth * 0.006);
    if (meshBox.isEmpty()) return;
    for (let x=0;x<2;x++) for(let y=0;y<2;y++) for(let z=0;z<2;z++) {
      projected.set(x?meshBox.max.x:meshBox.min.x,y?meshBox.max.y:meshBox.min.y,z?meshBox.max.z:meshBox.min.z);
      projected.applyMatrix4(mesh.matrixWorld).applyMatrix4(camera.matrixWorldInverse);
      if (projected.z >= -camera.near) { nearIntersection=true; return; }
      projected.applyMatrix4(camera.projectionMatrix);
      left=Math.min(left,projected.x);right=Math.max(right,projected.x);bottom=Math.min(bottom,projected.y);top=Math.max(top,projected.y);
    }
  });
  // Near-plane intersections need the complete frame; clipping corners is unsafe there.
  if (nearIntersection || !Number.isFinite(left)) return null;
  const w=renderer.domElement.width,h=renderer.domElement.height;
  const x=Math.max(0,Math.floor((left+1)*w/2)-4),y=Math.max(0,Math.floor((bottom+1)*h/2)-4);
  const endX=Math.min(w,Math.ceil((right+1)*w/2)+4),endY=Math.min(h,Math.ceil((top+1)*h/2)+4);
  if (endX<=x || endY<=y) return null;
  // Quantize and retain extents briefly to avoid texture/buffer churn during a gesture.
  // Shrink after a sustained smaller pose; zooming out must not retain a full-frame transfer.
  const now=performance.now(), neededWidth=Math.min(w,Math.ceil((endX-x)/128)*128), neededHeight=Math.min(h,Math.ceil((endY-y)/128)*128);
  if (regionExtent.canvasWidth !== w || regionExtent.canvasHeight !== h)
    regionExtent = {width:0,height:0,canvasWidth:w,canvasHeight:h,shrinkAt:0};
  if (neededWidth < regionExtent.width || neededHeight < regionExtent.height) {
    regionExtent.shrinkAt ||= now;
    if (now-regionExtent.shrinkAt >= 350) { regionExtent.width=neededWidth; regionExtent.height=neededHeight; regionExtent.shrinkAt=0; }
  } else regionExtent.shrinkAt=0;
  regionExtent.width=Math.max(regionExtent.width,neededWidth);
  regionExtent.height=Math.max(regionExtent.height,neededHeight);
  return {x:Math.min(x,w-regionExtent.width),y:Math.min(y,h-regionExtent.height),
    width:regionExtent.width,height:regionExtent.height,background:(0xff000000 | parseInt(background.slice(1),16))>>>0};
}
function renderFrame(timestamp) {
  // Readback/back-pressure may exceed 50ms. Clamping every frame made animation run
  // slower than real time during camera gestures, despite its playing flag remaining true.
  clock.update(timestamp); if (playing) mixer?.update(clock.getDelta());
  controls.update();
  const boundsAt=performance.now(); updateModelBounds(); cropMs=performance.now()-boundsAt;
  const renderingAt=performance.now(); renderer.render(scene,camera); renderMs=performance.now()-renderingAt;
  if (ready && presentation) {
    const cropAt=performance.now(); lastRegion = presentationRegion(); cropMs+=performance.now()-cropAt;
    presentation.present(lastRegion);
  }
}
const compatibleLoop = presentation ? window.keiosCreateFrameLoop(renderFrame, presentation) : null;
controls.addEventListener('change', () => compatibleLoop?.request());
function loop() {
  clock.reset(); cameraUpdatedAt = performance.now();
  if (compatibleLoop) { if (playing) compatibleLoop.start(); else compatibleLoop.request(); }
  else renderer.setAnimationLoop(renderFrame);
}
function setForeground(value) {
  foreground=!!value; presentation?.setForeground(foreground); compatibleLoop?.setForeground(foreground);
  if (foreground) loop(); else renderer.setAnimationLoop(null);
}
loop();
window.keiosModel = {
  load, resetCamera, selectAnimation, setOptions, seek,
  setBackground(value) {
    const color = modelBackgroundColor(value);
    if (!color) return false;
    background = color; renderer.setClearColor(color); compatibleLoop?.request(); return true;
  },
  setPlaying(value) {
    if (playing !== !!value) clock.reset();
    playing=!!value;
    if (playing && ended && action) { action.reset().play(); ended=false; }
    if (action) action.paused=!playing || scrubbing;
    if (playing) compatibleLoop?.start(); else compatibleLoop?.stop(); compatibleLoop?.request();
  },
  setForeground,
  state() { return {ready,error,url:loadedUrl,actions:clips.map(c=>c.name),durations:clips.map(c=>c.duration),selected,playing,
    time:action?.time || 0,duration:action?.getClip().duration || 0,ended,speed,loop:loopEnabled,outline:outlineEnabled,outlineWidth,
    foreground,scrubbing,actionPaused:action?.paused || false,background,presentationRegion:lastRegion,presentation:presentation?.stats() || null,
    renderMs,cropMs,renderCalls:renderer.info.render.calls,
    visibleParts:parts?.visibleTagged() || [],
    camera:camera.position.toArray(),target:controls.target.toArray(),size:[innerWidth,innerHeight],
    canvasSize:[renderer.domElement.clientWidth,renderer.domElement.clientHeight]}; },
  dispose() { ++generation; abort?.abort(); renderer.setAnimationLoop(null); compatibleLoop?.dispose(); presentation?.dispose(); disposeModel(); controls.dispose(); renderer.dispose(); },
};
addEventListener('pagehide',()=>window.keiosModel.dispose(),{once:true});
