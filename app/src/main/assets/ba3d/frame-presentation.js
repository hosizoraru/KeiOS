// Lossless drawing-buffer transport. No platform services are exposed to the page.
window.keiosCreateFramePresenter = function(gl) {
  const bridge = window.KeiViewerFrames;
  if (!bridge || !gl) return null;
  let generation = 0, pending = false, awaitingAck = false, disposed = false, foreground = true;
  let packet = null, buffer = null, fence = null, timer = 0, length = 0, last = -Infinity;
  let sent = 0, acknowledged = 0, readStarted = 0, readMs = 0, bridgeMs = 0, postedAt = 0;
  const webgl2 = typeof gl.getBufferSubData === 'function';
  function cancelRead() {
    clearTimeout(timer); timer = 0;
    if (fence) gl.deleteSync(fence);
    fence = null;
    // A posted old-generation message still needs its acknowledgement before another can queue.
    if (!awaitingAck) pending = false;
  }
  bridge.onmessage = () => { acknowledged++; bridgeMs = performance.now() - postedAt; awaitingAck = false; pending = false; };
  function post() { readMs = performance.now() - readStarted; postedAt = performance.now(); sent++; awaitingAck = true; bridge.postMessage(packet); }
  function begin() {
    cancelRead(); ++generation;
    bridge.postMessage(JSON.stringify({generation}));
  }
  begin();
  return {
    begin,
    stats() { return {sent, acknowledged, readMs, bridgeMs, pending, awaitingAck}; },
    canRender() { return !disposed && foreground && !pending; },
    setForeground(value) { foreground = !!value; },
    present(region) {
      const now = performance.now();
      if (disposed || !foreground || pending || now - last < 1000 / 60) return;
      const w = gl.drawingBufferWidth, h = gl.drawingBufferHeight;
      if (w < 1 || h < 1 || w > 4096 || h > 4096) return;
      const cropped = region && [region.x,region.y,region.width,region.height,region.background].every(Number.isInteger)
        && region.x >= 0 && region.y >= 0 && region.width > 0 && region.height > 0
        && region.x + region.width <= w && region.y + region.height <= h;
      const x = cropped ? region.x : 0, y = cropped ? region.y : 0;
      const rw = cropped ? region.width : w, rh = cropped ? region.height : h;
      const offset = cropped ? 40 : 16, bytes = rw * rh * 4;
      if (!packet || packet.byteLength !== bytes + offset) packet = new ArrayBuffer(bytes + offset);
      const header = new DataView(packet);
      header.setUint32(0, cropped ? 0x4B454932 : 0x4B454931, true); header.setUint32(4, generation, true);
      header.setUint32(8, w, true); header.setUint32(12, h, true);
      if (cropped) {
        header.setUint32(16,x,true); header.setUint32(20,y,true); header.setUint32(24,rw,true); header.setUint32(28,rh,true);
        header.setUint32(32,region.background,true); header.setUint32(36,0,true);
      }
      const token = generation;
      pending = true; last = now; readStarted = now;
      if (!webgl2) {
        gl.readPixels(x, y, rw, rh, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(packet, offset));
        post(); return;
      }
      // Asynchronous pack buffers avoid waiting for GPU completion in the render callback.
      const previous = gl.getParameter(gl.PIXEL_PACK_BUFFER_BINDING);
      buffer ||= gl.createBuffer(); gl.bindBuffer(gl.PIXEL_PACK_BUFFER, buffer);
      if (length !== bytes) { length = bytes; gl.bufferData(gl.PIXEL_PACK_BUFFER, length, gl.STREAM_READ); }
      gl.readPixels(x, y, rw, rh, gl.RGBA, gl.UNSIGNED_BYTE, 0);
      fence = gl.fenceSync(gl.SYNC_GPU_COMMANDS_COMPLETE, 0); gl.flush();
      gl.bindBuffer(gl.PIXEL_PACK_BUFFER, previous);
      const poll = () => {
        timer = 0;
        if (disposed || token !== generation || !fence) return;
        const result = gl.clientWaitSync(fence, 0, 0);
        if (result === gl.TIMEOUT_EXPIRED) { timer = setTimeout(poll, 4); return; }
        gl.deleteSync(fence); fence = null;
        if (result === gl.WAIT_FAILED) { pending = false; return; }
        const binding = gl.getParameter(gl.PIXEL_PACK_BUFFER_BINDING);
        gl.bindBuffer(gl.PIXEL_PACK_BUFFER, buffer);
        gl.getBufferSubData(gl.PIXEL_PACK_BUFFER, 0, new Uint8Array(packet, offset));
        gl.bindBuffer(gl.PIXEL_PACK_BUFFER, binding);
        post();
      };
      timer = setTimeout(poll, 0);
    },
    dispose() {
      disposed = true; cancelRead(); if (buffer) gl.deleteBuffer(buffer);
      buffer = null; packet = null; bridge.onmessage = null;
    },
  };
};

// Chromium throttles rAF when its WebView draw functor is bypassed. Keep only the
// compatible path on a bounded timer, with readback acknowledgement as back-pressure.
window.keiosCreateFrameLoop = function(render, presentation) {
  let running = false, foreground = true, disposed = false, dirty = false, timer = 0, last = -Infinity;
  function cancel() { clearTimeout(timer); timer = 0; }
  function schedule() {
    if (!disposed && foreground && !timer && (running || dirty))
      timer = setTimeout(tick, Math.max(0, Math.ceil(1000 / 60 - (performance.now() - last))));
  }
  function tick() {
    timer = 0;
    if (disposed || !foreground || (!running && !dirty)) return;
    // A transient renderer exception must not silently kill the scheduler. Preserve the
    // browser error report and schedule the next frame even when this callback throws.
    try {
      if (presentation.canRender()) { dirty = false; last = performance.now(); render(last); }
      else last = performance.now();
    } finally { schedule(); }
  }
  return {
    start() { running = true; schedule(); },
    stop() { running = false; cancel(); },
    request() { dirty = true; schedule(); },
    setForeground(value) { foreground = !!value; if (!foreground) cancel(); else schedule(); },
    dispose() { disposed = true; cancel(); },
  };
};
