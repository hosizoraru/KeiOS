import {Box3, Matrix4, Vector3} from './vendor/build/three.core.js';

// Positive skin weights form a convex combination of transformed vertices. The
// union of per-bone bind-pose boxes therefore bounds every pose without walking
// tens of thousands of vertices on each camera/render frame. Morph meshes and
// nonstandard weights retain Three.js' exact vertex path.
export function createSkinBounds() {
  const cache = new WeakMap(), point = new Vector3(), matrix = new Matrix4(), transformed = new Box3();
  function build(mesh) {
    const geometry=mesh.geometry, position=geometry.attributes.position;
    const weights=geometry.attributes.skinWeight, indices=geometry.attributes.skinIndex;
    if (!position || !weights || !indices || geometry.morphAttributes.position?.length ||
        weights.count !== position.count || indices.count !== position.count || weights.itemSize !== 4 || indices.itemSize !== 4) return null;
    const boxes=Array.from({length:mesh.skeleton.bones.length},()=>new Box3());
    let minSum=Infinity,maxSum=0;
    for(let v=0;v<position.count;v++) {
      point.fromBufferAttribute(position,v).applyMatrix4(mesh.bindMatrix);
      if (![point.x,point.y,point.z].every(Number.isFinite)) return null;
      let sum=0;
      for(let i=0;i<4;i++) {
        const weight=weights.getComponent(v,i), bone=indices.getComponent(v,i);
        if(!Number.isFinite(weight) || weight<0) return null;
        sum+=weight;
        if(weight>0) {
          if(!Number.isInteger(bone) || !boxes[bone]) return null;
          boxes[bone].expandByPoint(point);
        }
      }
      minSum=Math.min(minSum,sum); maxSum=Math.max(maxSum,sum);
    }
    if(!maxSum || !Number.isFinite(maxSum)) return null;
    return {boxes,minSum,maxSum,position,weights,indices,skeleton:mesh.skeleton,
      versions:[position.version,weights.version,indices.version],bind:mesh.bindMatrix.clone()};
  }
  return (mesh, target) => {
    if(mesh.geometry.morphAttributes.position?.length) { mesh.computeBoundingBox(); return target.copy(mesh.boundingBox); }
    const p=mesh.geometry.attributes.position,w=mesh.geometry.attributes.skinWeight,i=mesh.geometry.attributes.skinIndex;
    let entry=cache.get(mesh);
    if(!entry || entry.position!==p || entry.weights!==w || entry.indices!==i || entry.skeleton!==mesh.skeleton ||
        entry.versions[0]!==p?.version || entry.versions[1]!==w?.version || entry.versions[2]!==i?.version || !entry.bind.equals(mesh.bindMatrix)) {
      entry=build(mesh); if(entry)cache.set(mesh,entry);
    }
    if(!entry) { mesh.computeBoundingBox(); return target.copy(mesh.boundingBox); }
    target.makeEmpty();
    for(let n=0;n<entry.boxes.length;n++) {
      if(entry.boxes[n].isEmpty())continue;
      matrix.multiplyMatrices(mesh.skeleton.bones[n].matrixWorld,mesh.skeleton.boneInverses[n]);
      target.union(transformed.copy(entry.boxes[n]).applyMatrix4(matrix));
    }
    // Attribute sums need not be exactly one (including quantized weights).
    // Scale the interval conservatively before applying the inverse bind transform.
    for(const axis of ['x','y','z']) {
      const a=target.min[axis],b=target.max[axis],lo=entry.minSum,hi=entry.maxSum;
      target.min[axis]=Math.min(a*lo,a*hi,b*lo,b*hi);
      target.max[axis]=Math.max(a*lo,a*hi,b*lo,b*hi);
    }
    return target.applyMatrix4(mesh.bindMatrixInverse);
  };
}
