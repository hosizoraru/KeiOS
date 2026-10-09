import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
import {test} from 'node:test';
const script=readFileSync(new URL('../../app/src/main/assets/ba3d/frame-presentation.js',import.meta.url),'utf8');
function setup() {
  const messages=[]; let now=0, reads=0;
  const bridge={postMessage: m=>messages.push(typeof m==='string'?JSON.parse(m):new Uint8Array(m).slice())};
  const gl={drawingBufferWidth:2,drawingBufferHeight:1,RGBA:0x1908,UNSIGNED_BYTE:0x1401,
    readPixels(x,y,w,h,f,t,target) {reads++;target.set([255,0,0,255,0,255,0,255].slice(0,target.length));}};
  const context={window:{KeiViewerFrames:bridge},performance:{now:()=>now},setTimeout,clearTimeout};
  runInNewContext(script,context);
  const frame=context.window.keiosCreateFramePresenter(gl);
  return {frame,bridge,gl,messages,reads:()=>reads,advance:()=>{now+=20}};
}
test('one exact RGBA frame is in flight until native acknowledgement, without pixel scaling or compression',()=>{
  const s=setup();s.frame.present();s.advance();s.frame.present();
  assert.equal(s.reads(),1);assert.equal(s.messages.length,2);
  const p=s.messages[1];const h=new DataView(p.buffer);
  assert.equal(h.getUint32(0,true),0x4b454931);assert.equal(h.getUint32(4,true),1);
  assert.equal(h.getUint32(8,true),2);assert.equal(h.getUint32(12,true),1);
  assert.deepEqual([...p.slice(16)],[255,0,0,255,0,255,0,255]);
  s.bridge.onmessage();s.frame.present();assert.equal(s.reads(),2);
});
test('background and disposal stop readback, and a new generation rejects the old model epoch',()=>{
  const s=setup();s.frame.setForeground(false);s.frame.present();assert.equal(s.reads(),0);
  s.frame.setForeground(true);s.frame.present();s.frame.begin();
  assert.equal(s.messages.at(-1).generation,2);
  s.advance();s.frame.present();assert.equal(s.reads(),1);
  s.bridge.onmessage();
  s.advance();s.frame.present();assert.equal(new DataView(s.messages.at(-1).buffer).getUint32(4,true),2);
  s.frame.dispose();s.advance();s.frame.present();assert.equal(s.reads(),2);
});
test('invalid drawing buffer sizes never allocate or send frames',()=>{
  const s=setup();
  for(const n of [0,4097]) {s.gl.drawingBufferWidth=n;s.frame.present();}
  assert.equal(s.reads(),0);assert.equal(s.messages.length,1);
});

function loopSetup(onRender = () => {}) {
  let now=0, id=0, allowed=true, renders=0;
  const timers=new Map();
  const context={window:{},performance:{now:()=>now},setTimeout:(f,delay)=>{timers.set(++id,{f,at:now+delay});return id;},clearTimeout:i=>timers.delete(i)};
  runInNewContext(script,context);
  const loop=context.window.keiosCreateFrameLoop(()=>{renders++;onRender();}, {canRender:()=>allowed});
  function advance(ms) { const target=now+ms; while(timers.size) {const [i,t]=[...timers].sort((a,b)=>a[1].at-b[1].at)[0];if(t.at>target)break;timers.delete(i);now=t.at;t.f();}now=target; }
  return {loop,advance,allow:v=>allowed=v,renders:()=>renders,timers};
}
test('compatible animation advances without Chromium rAF and does not render while a readback is pending',()=>{
  const s=loopSetup();s.loop.start();s.advance(100);assert.ok(s.renders()>=5);
  s.allow(false);const n=s.renders();s.advance(100);assert.equal(s.renders(),n);assert.equal(s.timers.size,1);
  s.allow(true);s.advance(40);assert.ok(s.renders()>n);
  s.loop.setForeground(false);const b=s.renders();s.advance(1000);assert.equal(s.renders(),b);assert.equal(s.timers.size,0);
  s.loop.setForeground(true);s.advance(40);assert.ok(s.renders()>b);
  s.loop.dispose();s.advance(1000);assert.equal(s.timers.size,0);
});
test('a paused pose draws on request, including a request made while the last frame is still in flight',()=>{
  const s=loopSetup();s.loop.request();s.advance(20);assert.equal(s.renders(),1);assert.equal(s.timers.size,0);
  s.allow(false);s.loop.request();s.advance(100);assert.equal(s.renders(),1);
  s.allow(true);s.advance(20);assert.equal(s.renders(),2);assert.equal(s.timers.size,0);
  s.loop.start();s.advance(20);s.loop.stop();const n=s.renders();s.advance(100);assert.equal(s.renders(),n);
});
test('a transient render failure is reported but does not freeze ongoing playback until the user toggles pause',()=>{
  let first=true;
  const s=loopSetup(()=>{if(first){first=false;throw Error('transient render failure');}});
  s.loop.start();assert.throws(()=>s.advance(20),/transient render failure/);
  assert.equal(s.timers.size,1);
  s.advance(100);assert.ok(s.renders()>1);
  s.loop.dispose();
});

test('a model region retains original pixel resolution and its full-canvas position/background',()=>{
  const s=setup();s.frame.present({x:1,y:0,width:1,height:1,background:0xfffafcff});
  const p=s.messages[1],h=new DataView(p.buffer);
  assert.equal(p.length,44);assert.equal(h.getUint32(0,true),0x4b454932);
  assert.deepEqual([8,12,16,20,24,28,32].map(n=>h.getUint32(n,true)),[2,1,1,0,1,1,0xfffafcff]);
  assert.deepEqual([...p.slice(40)],[255,0,0,255]);
});
test('an invalid crop falls back to the complete framebuffer',()=>{
  const s=setup();s.frame.present({x:1,y:0,width:2,height:1,background:0xff000000});
  const p=s.messages[1];assert.equal(p.length,24);assert.equal(new DataView(p.buffer).getUint32(0,true),0x4b454931);
});
