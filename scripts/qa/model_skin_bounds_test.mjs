import assert from 'node:assert/strict';
import {test} from 'node:test';
import {createSkinBounds} from '../../app/src/main/assets/ba3d/model-skin-bounds.js';
import * as THREE from '../../app/src/main/assets/ba3d/vendor/build/three.core.js';
function model(weights=[1,0,0,0,0.25,0.75,0,0,0.4,0.6,0,0]) {
 const geometry=new THREE.BufferGeometry();
 geometry.setAttribute('position',new THREE.Float32BufferAttribute([-1,0,0,1,1,0,0,2,1],3));
 geometry.setAttribute('skinIndex',new THREE.Uint16BufferAttribute([0,1,0,0,0,1,0,0,0,1,0,0],4));
 geometry.setAttribute('skinWeight',new THREE.Float32BufferAttribute(weights,4));
 const parent=new THREE.Bone(),child=new THREE.Bone();parent.add(child);child.position.y=1;
 const mesh=new THREE.SkinnedMesh(geometry,new THREE.MeshBasicMaterial());mesh.add(parent);mesh.updateMatrixWorld(true);
 mesh.bind(new THREE.Skeleton([parent,child]));return {mesh,parent,child};
}
function contains(fast,exact) {for(const axis of ['x','y','z']) {assert.ok(fast.min[axis]<=exact.min[axis]+1e-6,axis+' min');assert.ok(fast.max[axis]>=exact.max[axis]-1e-6,axis+' max');}}
test('bone bounds conservatively contain exact animated vertices across orbit-independent poses and non-unit weight sums',()=>{
 for(const weights of [undefined,[0.9,0,0,0,0.2,0.6,0,0,0.4,0.8,0,0]]) {
  const {mesh,parent,child}=model(weights),bounds=createSkinBounds(),fast=new THREE.Box3();
  for(let step=0;step<50;step++) {
   parent.rotation.set(step*.03,step*.1,step*.05);child.rotation.z=Math.sin(step)*1.2;child.position.x=Math.cos(step)*2;
   mesh.updateMatrixWorld(true);bounds(mesh,fast);mesh.computeBoundingBox();contains(fast,mesh.boundingBox);
  }
 }
});
test('subsequent poses do not resample geometry, while edited attributes invalidate the bind-pose cache',()=>{
 const {mesh,child}=model(),bounds=createSkinBounds(),fast=new THREE.Box3(),p=mesh.geometry.attributes.position;
 let reads=0;const get=p.getX.bind(p);p.getX=v=>{reads++;return get(v)};
 bounds(mesh,fast);assert.equal(reads,3);reads=0;
 child.rotation.z=.5;mesh.updateMatrixWorld(true);bounds(mesh,fast);assert.equal(reads,0);
 p.setX(0,-20);p.needsUpdate=true;bounds(mesh,fast);assert.equal(reads,3);
 mesh.computeBoundingBox();contains(fast,mesh.boundingBox);
});
test('morph targets and negative weights preserve the exact Three.js bounding path',()=>{
 for(const morph of [false,true]) {
  const {mesh}=model(morph?undefined:[1.2,-0.2,0,0,0.2,0.8,0,0,0.4,0.6,0,0]);
  if(morph) {mesh.geometry.morphAttributes.position=[new THREE.Float32BufferAttribute([-5,0,0,1,5,0,0,2,5],3)];mesh.updateMorphTargets();mesh.morphTargetInfluences[0]=.8;}
  const fast=createSkinBounds()(mesh,new THREE.Box3());mesh.computeBoundingBox();assert.ok(fast.equals(mesh.boundingBox));
 }
});
