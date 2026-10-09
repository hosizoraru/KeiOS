package os.kei.ui.page.main.student.section.gallery

import org.json.JSONObject

// Keep the canvas in its Vue tree for camera, loading and gesture behavior. The controller reads
// animations from the skeleton, independently of the Wiki's dropdown. Page changes fail recoverably.
internal val GameKeeLobbyFocusScript = """
    (() => {
      const player = document.querySelector('.ba-live2d-container');
      const canvas = player && player.querySelector('canvas');
      const collapse = document.querySelector('[data-report-key="wikiBaTj|tj|sideAction|collapse"]');
      if (!canvas || !canvas.width || !canvas.height || !collapse ||
          player.querySelector('.ba-pixi-live2d-view > .mask')) return 'waiting';
      // The normal Wiki layout reserves camera space for the article sidebar.
      // Its own fullscreen mode supplies the centered memorial-lobby viewport.
      if (!player.classList.contains('is-collapsed') && !document.getElementById('keios-lobby-focus')) {
        if (collapse && !document.documentElement.hasAttribute('data-keios-lobby-collapsed')) {
          document.documentElement.setAttribute('data-keios-lobby-collapsed', 'true');
          collapse.click();
          return 'waiting';
        }
      }
      if (!document.getElementById('keios-lobby-focus')) {
        for (let node = player.parentElement; node && node !== document.body; node = node.parentElement)
          node.classList.add('keios-lobby-ancestor');
        const style = document.createElement('style');
        style.id = 'keios-lobby-focus';
        style.textContent = `
          html,body { margin:0!important; overflow:hidden!important; background:transparent!important; }
          body * { visibility:hidden!important; }
          .keios-lobby-ancestor { transform:none!important; position:static!important; margin:0!important;
            padding:0!important; background:transparent!important; min-height:0!important; }
          .ba-live2d-container { position:fixed!important; inset:0!important;
            width:var(--keios-lobby-width)!important; height:var(--keios-lobby-height)!important;
            z-index:2147483640!important; }
          .ba-live2d-container .live2d-stage,.ba-live2d-container .ba-pixi-live2d-view,
          .ba-live2d-container .pixi-container { position:absolute!important; inset:0!important;
            width:100%!important; height:100%!important; }
          .ba-live2d-container,.ba-live2d-container * { visibility:visible!important; }
          .ba-live2d-container .live2d-stage { mask-image:none!important; -webkit-mask-image:none!important; }
          .ba-live2d-container canvas { width:100%!important; height:100%!important; object-fit:contain; }
        `;
        document.head.appendChild(style);
        // Android WebView can resolve vh to zero while the Wiki initializes its layout.
        // Match the actual view viewport before the Wiki's canvas resize listener runs.
        const sizePlayer = () => {
          document.documentElement.style.setProperty('--keios-lobby-height', Math.max(window.innerHeight, 1) + 'px');
          document.documentElement.style.setProperty('--keios-lobby-width', Math.max(window.innerWidth, 1) + 'px');
        };
        sizePlayer();
        window.addEventListener('resize', sizePlayer);
        document.querySelectorAll('video,audio').forEach(media => media.pause());
        window.dispatchEvent(new Event('resize'));
      }
      const rendererElement = document.querySelector('.ba-pixi-live2d-view');
      const renderer = rendererElement && rendererElement.__vue__;
      if (!renderer || !renderer.pixiApp || !renderer.spineLayers.length) return 'waiting';
      if (window.KeiViewerFrames && !window.keiosLobbyFrames) {
        const output = renderer.pixiApp.renderer;
        const frames = window.keiosCreateFramePresenter?.(output.gl);
        if (!frames) return 'waiting';
        const draw = output.render;
        output.render = function() {
          const result = draw.apply(this, arguments); frames.present(); return result;
        };
        const app = renderer.pixiApp;
        app.stop();
        const loop = window.keiosCreateFrameLoop(time => app.ticker.update(time), frames);
        app.start = () => loop.start();
        app.stop = () => loop.stop();
        window.keiosLobbyFrames = frames;
        window.keiosLobbyLoop = loop;
        loop.start();
        window.addEventListener('pagehide', () => { loop.dispose(); frames.dispose(); }, {once:true});
      }
      if (!window.keiosLobby) {
        const base = renderer.layerList && renderer.layerList.length
          ? { ...renderer.layerList[renderer.layerList.length - 1].position } : null;
        let camera = { scale: 1, panX: 0, panY: 0 };
        const applyCamera = () => {
          const screen = renderer.pixiApp.screen;
          if (!base || !(base.width > 0) || !(base.height > 0) || !screen ||
              typeof renderer.applyViewportToAll !== 'function') return false;
          const left = renderer.parsePad(base.padLeft, screen.width);
          const right = renderer.parsePad(base.padRight, screen.width);
          const top = renderer.parsePad(base.padTop, screen.height);
          const bottom = renderer.parsePad(base.padBottom, screen.height);
          const fit = Math.max(Math.max(1, screen.width - left - right) / base.width,
            Math.max(1, screen.height - top - bottom) / base.height) * camera.scale;
          const width = base.width / camera.scale, height = base.height / camera.scale;
          renderer.applyViewportToAll({ ...base, width, height,
            x: Number(base.x || 0) + (base.width - width) / 2 - camera.panX * screen.width / fit,
            y: Number(base.y || 0) + (base.height - height) / 2 + camera.panY * screen.height / fit });
          // The camera also works on a paused pose without starting its ticker or animation.
          if (window.keiosLobbyLoop) window.keiosLobbyLoop.request();
          else if (typeof renderer.pixiApp.render === 'function') renderer.pixiApp.render();
          return true;
        };
        const resize = renderer.resizeToContainer;
        if (typeof resize === 'function') renderer.resizeToContainer = function() {
          const result = resize.apply(this, arguments);
          applyCamera();
          return result;
        };
        const state = () => {
          const spine = renderer.spineLayers[renderer.spineLayers.length - 1].spine;
          const track = spine.state.tracks[0];
          return { actions: spine.skeleton.data.animations.map(animation => animation.name),
            action: track && track.animation ? track.animation.name : '' };
        };
        window.keiosLobby = {
          state,
          cameraState: () => ({ ...camera }),
          setCamera(scale, panX, panY) {
            if (![scale, panX, panY].every(Number.isFinite) || scale < 0.5 || scale > 4) return false;
            const limit = (scale + 1) / 2 - 0.15;
            camera = { scale, panX: Math.max(-limit, Math.min(limit, panX)),
              panY: Math.max(-limit, Math.min(limit, panY)) };
            return applyCamera();
          },
          setAction(name) {
            if (!state().actions.includes(name)) return false;
            return renderer.setAnimation(name, true);
          },
          setPlaying(playing) {
            renderer.spineLayers.forEach(layer => { layer.spine.state.timeScale = playing ? 1 : 0; });
            if (playing) renderer.pixiApp.start(); else renderer.pixiApp.stop();
          },
          setForeground(value) { window.keiosLobbyFrames?.setForeground(value); window.keiosLobbyLoop?.setForeground(value); }
        };
      }
      return 'ready';
    })()
""".trimIndent()

internal fun gameKeeLobbySelectActionScript(action: String): String =
    "window.keiosLobby && window.keiosLobby.setAction(${JSONObject.quote(action)})"

internal fun gameKeeLobbyCameraScript(camera: GuideLobbyCameraTransform): String =
    "window.keiosLobby && window.keiosLobby.setCamera(${camera.scale},${camera.panX},${camera.panY})"
