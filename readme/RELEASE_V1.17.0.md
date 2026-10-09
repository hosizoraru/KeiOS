# KeiOS v1.17.0 Release Notes

<!-- markdownlint-disable MD013 -->

> 发布准备：当前公开稳定版为 v1.15.0，v1.16.0 是本地开发里程碑。本文合并 v1.16 阶段与后续 v1.17 阶段的用户可见变化。六段 Baseline Profile 已在 HyperOS Phone 完成，并在 Pad 鉴赏适配后使用 HyperOS Pad 重新完整采集。签名 APK、版本和打包结果按构建指南核对，当前尚未发布 v1.17.0。
>
> Release preparation: the published stable release is v1.15.0; v1.16.0 is a local development milestone. These notes combine the v1.16 work and subsequent v1.17 changes. The six-journey Baseline Profile was collected on HyperOS Phone and recollected on HyperOS Pad after the tablet viewer changes. Signed APK, version, and packaging checks follow the build guide; v1.17.0 has not been published yet.

## 中文

KeiOS v1.17.0 让学生图鉴多了两种鉴赏方式：可交互的动态回忆大厅，以及带动画和视角工具的 3D 模型。同时修复搜索栏输入时光标跳到开头、仅预发行仓库无法扫描包名、同仓库主应用与插件串版等影响日常使用的问题，并改善冷启动和页面滚动体验。

### 动态回忆大厅：可以欣赏，也可以交互

- 适配 WIKI 迁移后的 Spine 回忆大厅。新学生可以打开动态入口；老学生同时有 MP4 和 Spine 资源时，两种都保留，不必放弃原来的视频观看方式。
- 支持选择动作、暂停与继续、双指缩放、拖动画面和重置视角。液态控件覆盖在画面上，减少固定上下栏占用的鉴赏空间。
- 隐藏控件后，当前动画继续播放；点击画面即可恢复控件。重置视角和隐藏控件不会打断动画。
- 有关联回忆大厅 BGM 时默认播放，并提供独立静音按钮。静音不暂停动画，切到后台时按播放生命周期处理音频。
- 加载过程沿用学生详情的 Arona 动画。已下载的 Spine 素材会复用，缓存有容量限制，减少重复下载及长期占用。

### 学生 3D 模型：可旋转的模型与动画工具

- 为有模型资源且已完成对应关系的学生增加 3D 入口。按游戏角色 ID 和资源映射区分学生及服装变体；没有对应资源时不显示错误模型入口。
- 可切换可用模型和动画，旋转、缩放、移动视角，并重置视角。动画支持暂停、拖动进度、调整播放速度和循环。
- 提供描边开关与宽度调节，以及背景调色盘。默认背景适应系统深浅色模式，自选背景色会保存，控件颜色随背景调整。
- 已下载的模型可以复用，模型缓存有容量限制。关于页新增 Blue Archive Wiki 与 BlueArchiveModels 项目的鸣谢和来源入口。

### Pad 鉴赏与画面呈现

- 修复部分 OEM Pad 主页两侧出现黑边的问题，横竖屏按照当前设备方向显示。
- 3D 工具在宽横屏使用覆盖场景的液态玻璃侧栏，打开时不再拉伸模型；调色盘在栏内展开，选色后工具继续保留。竖屏及较窄窗口保留底部面板。
- 修复旋转、缩放、移动和进度拖动时动画偶发停住的问题；隐藏控件后仍可操作模型，轻点画面恢复控件。
- 输入背景颜色时，键盘不再将整个模型界面顶起；播放底栏暂时让出空间，完成输入或轻点场景即可收起键盘，动画继续播放。
- 动态回忆大厅和 3D 模型都默认使用「系统 WebView」。若画面在某些设备上显示异常，可在设置 → 界面 → 性能预取中分别选择「兼容画面呈现」，下次进入鉴赏页生效；已保存的选择继续保留。
- 兼容模式保留原有材质、描边、动画与像素分辨率。部分模拟器仍有画面传输开销；该选项不保证更高帧率。

### 搜索和弹层：输入不再打乱光标

- 修复部分搜索栏每输入一个字母，光标就跳到开头的问题。连续输入、在中间插入、选中文字和中文输入法组词都保留各自的编辑状态。
- 修复键盘弹出或收起时，弹层顶部及背景模糊出现断层的问题，保留原有玻璃材质和弹层动画。

### GitHub 追踪：仅预发行仓库和插件也能正确处理

- 添加追踪时，包名扫描遵守单项预发行选项和全局预发行开关。像 hyperos-fcm-fix 这类只有预发行版的仓库，开启选项后也能扫描 APK 包名并关联已安装应用。
- 像 Husi 这样在同一仓库发布主应用及插件的项目，先匹配 APK 包名，再判断该应用的正式版和预发行版，避免插件版本被主应用版本覆盖。
- 修正 Atom 模式对仅预发行仓库的分类；旧判断缓存会自动重新检查，保留已添加的追踪和原有配置。

### 启动、滚动与其他体验

- 冷启动使用与图标及系统深浅色主题协调的背景，在入口页面的本地状态准备好后淡出，减少默认值和“读取中”信息一闪而过的情况；遵循现有过渡动画开关。
- 改善学生详情，尤其是技能和等级选择区域，以及多处页面的滚动卡顿。Liquid Glass 的材质、模糊、阴影和组件动画保持完整。
- 超级岛增加自动关闭时间设置，可选择不自动关闭或指定时长；GitHub 刷新进度仍在任务结束后收尾。
- Shell 命令长时间没有输出时也能触发超时，避免一直等待。

### 从公开 v1.15.0 升级也会获得的改进

- GitHub 版本判断能处理更名后重新编号的项目、已被正式版取代的预发布，以及没有可下载文件的发行版。无法确定的比较如实显示，出乎意料的版本选择会说明原因。
- 忽略 `nightly`、`preview` 等滚动预览标签后，同一标签替换 CI 构建不会立刻让它重新提示更新。
- 刷新减少串行等待；慢项目会在刷新历史中标出排队、DNS、连接、服务器等待或下载阶段，并在导出和 MCP 中提供对应诊断。
- 发行版与 F-Droid 历史中的旧版本可以查看 APK 信息并在应用内安装；各处分享和下载遵守安装器与下载方式设置。
- 减少主要页面和玻璃控件的渲染开销，修复堆叠卡片的裁剪、按钮可达性和按压位置。材质、模糊和动效保留。
- 学生图鉴在列表惯性滚动中也可横滑翻页，音频进度条仍保留自己的拖动；MCP 展开的卡片在进程回收后可恢复。
- 不受信任的其他应用只能读取 BGM 播放状态，不能控制播放；应用自身、媒体通知及受信任控制方继续正常操作。

这些变化属于本地 v1.16 开发阶段，详细历史说明保留在 [v1.16.0 记录](RELEASE_V1.16.0.md)，本次发布将一并交付。

### 升级与安装

- 从公开 v1.15.x 或已有 v1.16 开发版升级保留设置、追踪和收藏，无需清除数据或重新添加项目。
- 正式包名 `os.kei`，支持 `arm64-v8a`，Android 15+（minSdk 35），targetSdk 37。
- 正式版本目标：`1.17.0` / `versionCode 11700999`。最终签名 APK 和校验文件将在发布时提供。

## English

KeiOS v1.17.0 adds two ways to explore the Student Guide: interactive Memorial Lobbies and student 3D models with animation and camera tools. It also fixes cursors jumping to the start of search fields, package scanning in repositories with only pre-releases, and main-app/plugin version mix-ups, while improving startup and scrolling.

### Interactive Memorial Lobbies

- Supports the WIKI Spine lobbies after their migration. New students can open the interactive entry; older students with both MP4 and Spine resources keep both viewing options.
- Choose an action, pause or resume, pinch to zoom, drag to pan, or reset the view. Liquid controls overlay the scene, leaving more space for viewing.
- Hiding controls keeps the current animation playing. Tap the scene to restore them. Resetting the view and hiding controls do not interrupt playback.
- Linked lobby BGM plays by default with a separate mute button. Muting does not pause the animation, and audio follows the player lifecycle when the app goes into the background.
- Loading uses the Student Guide Arona animation. Downloaded Spine assets are reused within a bounded cache to reduce repeated downloads and storage growth.

### Student 3D Models

- Students with available, mapped model resources have a 3D entry. Game character IDs and reviewed resource mappings distinguish students and outfit variants; an unmatched student is not given an incorrect model entry.
- Choose available models and animations, rotate, zoom, pan, and reset the view. Animation controls include pause, seeking, speed, and looping.
- Adjust outlines and their width, or choose a background from the palette. The default follows system light/dark mode, custom colors are saved, and controls adapt to the background.
- Downloaded models are reused within a bounded cache. About includes acknowledgements and source links for Blue Archive Wiki and BlueArchiveModels.

### Tablet Viewing And Presentation Options

- Fixes side bars on the Home screen of affected OEM tablets. The layout follows the current landscape or portrait orientation.
- Wide landscape windows use a Liquid Glass tools sidebar over the 3D scene. Opening it keeps the model geometry stable; its inline palette stays open after color selection. Portrait and narrower windows retain bottom sheets.
- Fixes occasional animation stalls during rotation, zooming, panning, and seeking. Camera gestures remain available with controls hidden; tap the scene to restore controls.
- Entering a background color no longer pans the entire model screen upward. Playback controls temporarily make room for the keyboard; Done or a scene tap dismisses it while animation continues.
- Both viewers default to **System WebView**. If a device displays the scene incorrectly, choose **Compatible presentation** independently for either viewer under Settings → Interface → Performance preload. Changes apply on the next entry, and saved selections are retained.
- Compatibility mode preserves materials, outlines, animations, and pixel resolution. Some emulators still incur frame-transfer overhead; this option does not guarantee a higher frame rate.

### Search And Sheets

- Fixes affected search fields moving the cursor to the beginning after each letter. Continuous typing, insertion in the middle, text selection, and Chinese IME composition retain their editing state.
- Fixes discontinuities in sheet blur as the keyboard opens or closes, preserving the glass material and sheet animation.

### GitHub Tracking

- Package-name scanning follows the per-project pre-release option and the global setting. Repositories with only pre-releases, such as hyperos-fcm-fix, can be scanned and linked to an installed app when the option is enabled.
- Repositories publishing both a main app and plugins, such as Husi, match APK package identity before selecting each product's stable or pre-release version. A main-app version no longer replaces a plugin's version.
- Corrects Atom classification for repositories with only pre-releases. Old result caches are checked again automatically while tracked projects and settings are retained.

### Startup, Scrolling, And Other Improvements

- Cold starts use a background suited to the icon and system theme, then fade into a destination with its local state prepared. This reduces flashes of default or loading values and follows the transition-animation setting.
- Reduces scrolling stalls in student details, especially skill and level selectors, and other pages. Liquid Glass materials, blur, shadows, and component animations are preserved.
- Super Islands have a closing-time setting, including no automatic closing. GitHub refresh progress still closes when its task finishes.
- Quiet Shell commands now time out instead of waiting indefinitely.

### Also Included For Users Upgrading From v1.15.0

- GitHub version selection handles projects that restarted numbering after a rename, pre-releases superseded by stable releases, and releases without downloadable files. Uncertain comparisons remain explicit, and unexpected choices explain their reason.
- Ignoring a rolling preview tag such as `nightly` or `preview` remains effective when CI replaces the build under the same tag.
- Refreshing reduces serial waits. Slow projects identify their queue, DNS, connection, server-wait, or download phase in refresh history, exports, and MCP diagnostics.
- Older releases and F-Droid builds can show APK details and install in-app. Sharing and downloading follow the installer and download-method settings on every surface.
- Main pages and glass controls draw with less overhead. Card piles retain reachable buttons, correct clipping, and touch targeting while preserving materials, blur, and motion.
- Student Guide pages can be swiped while a list is coasting, without taking horizontal drags away from audio sliders. Expanded MCP cards restore after process recreation.
- Untrusted apps can read BGM playback state but cannot control it; the app, media notification, and trusted controllers retain their controls.

This work belongs to the local v1.16 development milestone. Its detailed [v1.16.0 record](RELEASE_V1.16.0.md) is retained, and the changes are included in this release.

### Upgrade And Package

- Upgrading from the published v1.15.x or a v1.16 development build retains settings, tracked projects, and favorites. Clearing data or adding projects again is unnecessary.
- Stable package: `os.kei`; ABI: `arm64-v8a`; Android 15+ (minSdk 35); targetSdk 37.
- Release target: `1.17.0` / `versionCode 11700999`. The final signed APK and checksum will be provided at publication.

## 发布前核对 / Before Publication

- 在最终源码上完成 Baseline Profile 采集，核对六段旅程的有效输出及生成文件，提交后通过新鲜度门禁。
- 验证正式 APK 的 R8/lint、版本、签名及 `assets/dexopt/baseline.prof` 和 `baseline.profm`。
- 本地预备 tag 在上述提交完成后指向最终发布提交，再推送并发布；移除本文的发布准备提示。
- Complete Baseline Profile collection on the final source, verify all six journeys and generated files, then commit the outputs and pass the freshness gate.
- Verify release R8/lint, version, signing, and packaged `baseline.prof` / `baseline.profm`.
- Point the local preparation tag at the final release commit before pushing and publishing, and remove the preparation notice above.

详见 [构建与发布门禁](BUILD_CN.md#v1170-发布门禁) / [Build and release gate](BUILD.md#v1170-release-gate)。
