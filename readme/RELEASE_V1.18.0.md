# KeiOS v1.18.0 Release Notes

<!-- markdownlint-disable MD013 -->

> 本次更新汇总自公开 v1.17.0 以来的变化，重点改善 Liquid Sheet 刚打开和非全屏高度操作时的卡顿，并修复长发行日志、通知唤起与弹层恢复问题。
>
> This update covers the changes since the published v1.17.0. It focuses on Liquid Sheet opening and initial partial-height interaction, long release notes, notification entry, and presentation recovery.

## 中文

KeiOS v1.18.0 主要打磨日常操作的流畅度与稳定性。打开 Liquid Sheet、刚打开时拖动面板，以及浏览长发行日志时，减少不必要的渲染和整页更新；玻璃材质、模糊、折射、阴影、动画和特效保持完整。通知、超级岛和快捷方式再次唤起应用时，也能更可靠地回到对应页面和账户。

### Liquid Sheet：改善打开与初始拖动

- 减少打开 Sheet，尤其是非全屏高度面板刚打开及首次拖动时的卡顿。面板与内嵌控件复用材质和页面绘制结果，减少反复编译 Shader、图层处理和重复绘制。
- 滚动时减少可见区域之外的玻璃绘制开销，继续保留原有布局、焦点、无障碍语义和嵌套滚动行为。
- 调整内容滚动与面板拖动的协作，多层滚动内容仍能分别保持位置；到达边缘后继续按原有手势调整面板高度。
- 保留原有 Liquid Glass 视觉、动画、模糊、折射、阴影和特效。这次优化没有通过降低分辨率、替换材质或关闭效果来换取流畅度。

### GitHub：长发行日志与弹层恢复

- 修复长发行日志把绘制区域撑得过大，导致内容显示不全、滚动异常或明显卡顿的问题。长文按可见段落加载，保留 Markdown 样式、链接和完整复制。
- 保留面板的初始高度、展开与收起手势，以及摘要和正文各自的滚动行为；大字体和宽屏下也能继续阅读到底部。
- GitHub Sheet 打开时收起后方搜索编辑状态，保留搜索词和筛选结果。切换主题或字体后，键盘不会因后方搜索栏重新取得焦点而盖住内容。
- 从 Actions 通知再次进入时，清理过期的目标和弹层请求，避免打开上一次的项目或运行记录。

### 通知、超级岛与再次唤起

- 修复 HyperOS 上通过通知、超级岛或快捷方式重复进入应用后，启动画面残留并遮挡内容的问题。首次启动的背景、内容准备和过渡动画继续保留。
- 修复多个通知入口互相覆盖目标的问题。MCP、活动／卡池和 Actions 通知各自保留对应的打开目标。
- BA 制造室与一键日常通知携带正确的账户信息进入 BA，减少多账户之间跳错位置的情况。
- 页面被其他页面覆盖时，Sheet、对话框和菜单跟随当前页面的显示状态，避免旧弹层残留在新页面上；原页面状态继续保留。

### 播放、页面切换与预编译

- BGM 迷你播放器、学生语音和图库音频的进度更新集中到进度条与时间文字，减少播放期间无关区域反复更新；暂停、拖动预览和播放控制保持原有行为。
- 底栏按压、BA 账户翻页、搜索区和页面间距动画减少整页更新，保留原有动画时长、手势和布局。
- 重新整理并完整采集 HyperOS Phone Baseline Profile，覆盖启动、主页导航、常用页面、GitHub、BA 和宽窄窗口路径，并包含新的 Sheet 渲染与初始拖动。
- 更新 Miuix 与相关运行库，并使用 Gradle 9.8.1、Kotlin 2.4.21 正式版构建。Android 版本要求不变。

### 升级与安装

- 正式版本：`1.18.0` / `versionCode 11800999`。
- 正式包名：`os.kei`；支持 `arm64-v8a`；Android 15+（minSdk 35），targetSdk 37。
- 从使用相同 KeiOS Release 签名的 v1.17.x 或更早正式版覆盖升级，可保留设置、追踪、BA 账户和收藏，无需卸载或清除数据。
- Debug 使用独立包名 `os.kei.debug`。CI Benchmark 与正式版共用 `os.kei`，但 CI 签名不同；Android 仍可能拒绝直接覆盖。遇到签名或版本码不兼容时，先导出数据并核对安装渠道。
- 发布附件使用 KeiOS Release 签名 APK，并提供 SHA-256 校验文件；公开下载入口以 GitHub Releases 为准。

## English

KeiOS v1.18.0 improves everyday responsiveness and reliability. Liquid Sheet opening, initial partial-height dragging, and long release-note browsing do less redundant rendering and page-wide work while preserving glass materials, blur, refraction, shadows, animations, and effects. Notification, Super Island, and shortcut reentry also return more reliably to the intended page and account.

### Liquid Sheet Opening And Initial Interaction

- Reduces stalls when opening a Sheet, particularly at a partial-height detent and during the first drag. Sheets and embedded controls reuse material and page recordings to reduce repeated shader compilation, layer processing, and drawing.
- Reduces glass drawing outside the visible scroll area while retaining layout, focus, accessibility semantics, and nested scrolling.
- Improves coordination between content scrolling and panel dragging. Nested content retains its independent scroll positions, and edge gestures continue to resize the panel.
- Preserves Liquid Glass visuals, animations, blur, refraction, shadows, and effects. The optimization does not lower resolution, substitute materials, or disable effects.

### GitHub Release Notes And Sheets

- Fixes long release notes creating oversized drawing areas that could cause missing content, broken scrolling, or severe stalls. Long documents load visible blocks while keeping Markdown styling, links, and full-text copying.
- Preserves opening height, panel expansion and collapse gestures, and separate summary/document scrolling. Large fonts and wide windows can still reach the end of the document.
- Opening a GitHub Sheet collapses search editing behind it while retaining the query and filtered results. Theme or font changes no longer let the covered search field bring the keyboard over the content.
- Notification reentry clears stale Actions targets and presentation requests instead of reopening the previous project or run.

### Notifications, Super Islands, And Reentry

- Fixes a launch surface remaining over the app after repeated notification, Super Island, or shortcut entry on HyperOS. The initial launch background, content preparation, and transition animation are retained.
- Prevents overlapping notification entries from replacing each other's destinations. MCP, calendar/pool, and Actions notifications retain their own open targets.
- Craft Chamber and one-tap daily notifications open BA with the correct account information, reducing incorrect destinations in multi-account use.
- Sheets, dialogs, and menus follow their owning page's visibility when another page covers it, preventing old overlays from remaining above a new page while preserving page state.

### Playback, Page Transitions, And Precompilation

- BGM mini-player, student voice, and gallery audio progress update the relevant slider and clock without repeatedly updating unrelated page content. Pause, seek previews, and playback controls retain their behavior.
- Bottom-bar presses, BA account paging, search chrome, and animated page spacing do less page-wide work while retaining their animation timing, gestures, and layout.
- Refreshes the complete HyperOS Phone Baseline Profile across startup, main navigation, common routes, GitHub, BA, and wide/compact window paths, including the new Sheet rendering and initial-drag paths.
- Updates Miuix and related runtime libraries, with Gradle 9.8.1 and Kotlin 2.4.21 final for builds. Android requirements remain unchanged.

### Upgrade And Package

- Release version: `1.18.0` / `versionCode 11800999`.
- Stable package: `os.kei`; ABI: `arm64-v8a`; Android 15+ (minSdk 35); targetSdk 37.
- Installing over v1.17.x or an earlier stable build signed with the same KeiOS Release certificate retains settings, tracked projects, BA accounts, and favorites. Uninstalling or clearing data is unnecessary.
- Debug uses the separate package `os.kei.debug`. CI Benchmark shares `os.kei` with the release but uses a different signing certificate; Android may reject an overwrite. Export data and check the installed channel if signing or version-code compatibility prevents installation.
- Release assets use the KeiOS Release-signed APK and include a SHA-256 checksum. GitHub Releases remains the authoritative public download channel.

详见 [构建与发布门禁](BUILD_CN.md#v1180-发布门禁) / [Build and release gate](BUILD.md#v1180-release-gate)。
