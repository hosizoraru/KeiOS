# KeiOS 待办清单

这里记录需要稳定设备、OEM Beta 或集中验证窗口才能继续推进的路线项。

## 优先级说明

- P0：API 36 主力设备与当前 `targetSdk=37` 会直接影响现有用户链路的项目。
- P1：API 37 强制行为、OEM Beta、长期稳定性验证项目。
- P2：可选新 API、低频入口、暂缓路线项目。

## API 基线审计

官方资料核对日期：2026-04-29。
参考：[Android 16 target SDK 行为变更](https://developer.android.com/about/versions/16/behavior-changes-16)、
[Android 16 全应用行为变更](https://developer.android.com/about/versions/16/behavior-changes-all)、
[Android 16 新特性与 API](https://developer.android.com/about/versions/16/features)、
[Android 17 target SDK 行为变更](https://developer.android.com/about/versions/17/behavior-changes-17)、
[Android 17 全应用行为变更](https://developer.android.com/about/versions/17/behavior-changes-all)、
[Android 17 新特性与 API](https://developer.android.com/about/versions/17/features)、
[本地网络权限](https://developer.android.com/privacy-and-security/local-network-permission)、
[16 KB page-size 支持](https://developer.android.com/guide/practices/page-sizes)。

- [x] 构建基线为 `minSdk=35`、`compileSdk=37`、`targetSdk=37`，Java / Kotlin toolchain 为 21。
- [x] Manifest 已声明 `NEARBY_WIFI_DEVICES(maxSdk=36)`、`ACCESS_LOCAL_NETWORK`、`USE_LOOPBACK_INTERFACE`、`POST_PROMOTED_NOTIFICATIONS`、`FOREGROUND_SERVICE_SPECIAL_USE`。
- [x] 主 Activity 已启用 `enableEdgeToEdge()`，启动图标已提供 monochrome 资源，主导航与图鉴全屏图已接入预测返回路径，并显式开启 `OnBackInvokedCallback`。
- [x] 源码搜索当前未命中 Contacts、Bluetooth、Health Connect / sensor 权限、`READ_MEDIA*`、自定义 RemoteViews、`MediaStore#getVersion()`、`scheduleAtFixedRate`、WorkManager、JobScheduler、`announceForAccessibility` / `TYPE_ANNOUNCEMENT`、项目 JNI、`System.load*` 直接适配面。
- [x] 非 Home 背景导入与 BA 媒体保存 / ZIP 导出当前使用 SAF、FileProvider、显式 URI grant、DocumentFile，宽媒体权限面为 0。
- [x] 新增统一 API 版本辅助，覆盖 `SDK_INT_FULL`、API 36、API 36.1、API 37 判断。
- [x] About 构建信息中增加 Runtime API Full 与 Advanced Protection Mode 状态检测。
- [x] GitHub API Token sheet 是当前唯一密码型输入面；已使用 Compose 密码遮罩与显式显示开关，后续需要做 Android 17 物理键盘验证。

## P0 - API 36 主力机验收

### P0-A 显示、输入、返回

- [x] 在 API 36 普通窗口完成 Home、OS、BA、MCP、GitHub、Settings、About、GitHub 分享导入窗口、uCrop、OS Shell Runner、视频全屏页的 edge-to-edge、状态栏、导航栏 smoke；证据目录：`artifacts/api36-p0/smoke4/`。
- [x] 完成 API 36 IME 与聚焦输入 inset 验证，覆盖 OS Shell Runner、GitHub Token / 分享导入、Settings 权限流、BA 图鉴筛选；证据目录：`artifacts/api36-p0/p0a-display-input-accessibility-run2/`。本轮在 Xiaomi `25098PN5AC` 上采集 OS Shell Runner、GitHub API Token、BA 图鉴目录筛选的截图、UI XML 与 `dumpsys input_method`。
- [x] 完成 API 36 字体 smoke，覆盖中文、日文、拉丁字符、大字体、粗体文字、outline text 设置，以及 Home、GitHub、BA 图鉴 / 图鉴目录、Settings、About、OS Shell、GitHub 分享导入；证据目录：`artifacts/api36-p0/p0a-display-input-accessibility-run2/`。本轮使用 `font_scale=1.35`、高对比文字、`font_weight_adjustment=200`，结束后已恢复设备文字设置。
- [x] 完成预测返回验证：Nav3 主页面、设置页、学生图鉴详情、图鉴全屏图片、OS Shell Runner、GitHub 分享导入 Activity，并覆盖手势返回与按键返回；证据目录：`artifacts/api36-p0/p0a-display-input-accessibility-run2/`。已采集 `CoreBackPreview` 证据，过滤后的 KeiOS 进程 fatal 计数为 0。本轮记录到 1 条 `uiautomator` dump 进程注册冲突崩溃，归入后续 QA 工具过滤项。
- [x] 为 HyperOS / MIUI / Xiaomi、ColorOS、OriginOS、MagicOS、EMUI、One UI 增加预测返回 OEM 兼容策略，主导航返回动画会跟随左右边缘方向。
- [x] 在 Android 17 / API 37 AVD 上完成预测返回 smoke：Home -> 设置 -> 左边缘返回、Home -> About -> 右边缘返回，logcat 确认进入 `CoreBackPreview` 回调路径。

### P0-B 运行时、打包、Native 依赖

- [x] 完成打包 debug APK 的 API 36 16 KB page-size 验证，使用 `KeiOS_API36_16K` AVD；证据目录：`artifacts/api36-p0/p0b-runtime-native-api36-16k-run2/`。模拟器报告 `PAGE_SIZE=16384`，`zipalign -P 16` 退出码为 0，依赖带入的 native libraries `libandroidx.graphics.path.so` 与 `libmmkv.so` 最小 LOAD alignment 均为 `16384`，当前决策为保持 `android:pageSizeCompat` 未设置。
- [x] 完成 API 36 ART / native dependency smoke，覆盖全新安装、升级安装、启动、MMKV 读写、缓存清理、Coil GIF 解码、Media3 BGM / 视频、Shizuku 初始化、focus-api 通知构建、backdrop / liquid-glass 渲染；证据目录：`artifacts/api36-p0/p0b-runtime-native-api36-16k-run2/`。全新安装与升级安装均返回 `Success`，runtime probe 全部 `PASS`，`UnsatisfiedLinkError`、KeiOS 进程 fatal exception、KeiOS native `dlopen failed` 的运行阻断计数为 0。`AccessibilityNodeInfo.getSelection/setSelection` 的 hidden-API denied 残留进入下一轮 hidden-API 审计。
- [x] 完成 `setAccessible` / hidden API 首轮收敛：HyperOS 设置跳转与保活权限辅助的系统属性读取改用统一 PropUtils，AppOpsManagerInjector 改为公开方法探测；Shizuku private `newProcess` 兼容入口保留并受状态门控。

### P0-C 本地网络与 MCP 服务

- [x] 为 API 36 本地网络限制测试补充 `NEARBY_WIFI_DEVICES(maxSdk=36)`，并把 MCP 局域网权限请求统一接入 API 36 / API 37 权限辅助。
- [x] 在 Android 17 / API 37 AVD 开启 `RESTRICT_LOCAL_NETWORK` 后验证 MCP loopback：`127.0.0.1:38888/mcp` 可达并返回预期鉴权拦截，暂不需要 Network Security Config。
- [x] 使用 Android 16 / API 36 设备验证 MCP 局域网模式在 `RESTRICT_LOCAL_NETWORK` 下的同网段访问：同网段 `http://192.168.31.209:38888/mcp` 与设备 loopback 均返回预期 `401 Unauthorized`。
- [x] 在 Android 17 / API 37 AVD 验证 MCP `specialUse` 前台服务：前台启动被系统允许，通知权限拒绝时服务保持前台运行且无崩溃，权限恢复后可正常停止。
- [x] 在 Android 16 / API 36 真机验证 MCP `specialUse` 前台启动：`McpKeepAliveService` 以前台 type `0x40000000` 运行，服务通知为 `38887`，Live Update 通知为 `38888`。
- [x] 验证 MCP `specialUse` 前台服务在 Android 16 后台启动、省电策略、Shizuku 未激活状态下的快捷入口恢复路径；证据目录：`artifacts/api36-p0/mcp-recovery-20260429-6/`。`os.kei.benchmark` 在 `low_power=1`、standby bucket `45`、Shizuku permission `not_granted` 下启动，后台 22s 后仍保持前台 service type `0x40000000`、服务通知 `38887`、Live Update 通知 `38888`、loopback `401 Unauthorized`，未记录 `ForegroundServiceStartNotAllowedException` 或 fatal exception。

### P0-D Intent、URI Grant、文件流

- [x] 在 API 36 上验证 SAF / URI grant：非 Home 背景 `OpenDocument -> uCrop`、BA 媒体自定义保存位置、BA ZIP 导出、日志归档导出；证据目录：`artifacts/api36-p0/p0d-intent-uri-20260429-1/`。`os.kei.benchmark` 无 `READ_MEDIA*` / `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`，FileProvider 已注册；图片 `OpenDocument`、`OpenDocumentTree`、日志 `CreateDocument(application/zip)` 均进入系统文件选择器；非 Home 背景链路保留 `takePersistableUriPermission` + `UriGrantCompat.grantToIntentTargets` 后进入 uCrop，BA 单媒体 / ZIP 导出保留 `CreateDocument` / `ACTION_OPEN_DOCUMENT_TREE`、persistable read/write tree grant 与 `ContentResolver` 输出流。
- [x] 加固 GitHub `ACTION_SEND` 导入 Activity、下载器选择、外部链接打开、分享 APK 链接等 Safer Intents 高频链路，确保 action、mime、scheme、host、package 边界更明确。
- [x] 在 API 36 上完成 GitHub 分享导入 strict smoke，使用 direct release APK URL；窗口可解析 `topjohnwu/Magisk` `v27.0`，列出 `Magisk-v27.0.apk`，并保持安装确认 sheet 可见。
- [x] 在 API 36 上对 OS 用户自定义 shortcut card、OEM 设置辅助、外部浏览器 / 下载 / 分享 Intent、GitHub 分享导入做 Safer Intents strict-matching smoke；证据目录：`artifacts/api36-p0/p0d-intent-uri-20260429-1/`。GitHub `ACTION_SEND text/plain` 解析到 `GitHubShareImportActivity`，`image/png` 返回 `No activity found`；外部网页、直链 APK、文本分享解析到系统 resolver；OS shortcut 显式 Google settings 样例与 Settings launcher fallback 解析成功；OEM 应用详情、省电白名单、HyperOS 权限编辑入口解析成功，无需补充 action、filter、scheme、host 或 package 约束。
- [x] 完成用户自定义 OS shortcut intents、通知 / shortcut extras 首轮审计：MainActivity 外部 extras 已按目标页和动作白名单配对，OS Activity card 启动前补充目标可解析校验，MCP 栈顶快捷入口已用 `singleTop` 验证可稳定启动/停止。
- [x] 在 Android 17 / API 37 AVD 上完成外部 extras smoke：合法 GitHub 快捷动作进入 GitHub，错配动作进入目标页但丢弃动作，未知目标回落 Home。

### P0-E 后台任务与通知

- [x] 验证 DownloadManager、GitHub 后台刷新、BA AP / 咖啡厅 / 竞技场提醒在 Android 16 空闲、锁屏、省电模式下的调度表现；证据目录：`artifacts/api36-p0/p0e-background-notifications-20260429-1/`。API 36 小米真机 `os.kei.debug` 在 `low_power=1`、standby bucket `45` 下保留 GitHub / BA 一次性 `AlarmManager` tick 记录，`jobscheduler` 无 KeiOS 任务，`dumpsys download` 无 KeiOS 待处理下载；源码审计保留无 `WorkManager` / `JobScheduler` / fixed-rate work 路径结论，验证后已恢复 `low_power=0`、bucket `10`。
- [x] 在 Android 17 / API 37 AVD 验证 MCP Live Update / promoted notification：通知记录包含 `PROMOTED_ONGOING`、`ProgressStyle` 动作、打开/停止 PendingIntent allowlist，点击链路保持现有通知框架。
- [x] 在 API 36 验证 MCP、GitHub、BA promoted notification 记录构建，保持现有通知框架；记录包含 `PROMOTED_ONGOING`、`NotificationCompat.ProgressStyle`、action PendingIntent allowlist，以及预期通知 id `38888`、`38990`、`38889`、`38890`、`38891`。
- [x] 继续验证 GitHub / BA / 超级岛通知在 Android 16 promoted notification 与 `NotificationCompat.ProgressStyle` 下的视觉面和点击行为；证据目录：`artifacts/api36-p0/p0e-background-notifications-20260429-1/`。通知栏与锁屏截图 / XML 覆盖 GitHub、BA AP、咖啡厅、竞技场，通知记录包含 `PROMOTED_ONGOING`、`NotificationCompat.ProgressStyle`、PendingIntent allowlist；GitHub `打开` 动作进入 GitHub 页，BA `打开` 动作进入 BlueArchive 页；超级岛兼容链路保持现有 helper/source contract 与通知框架。

### P0-F QA 工具与证据

- [x] 增加 `scripts/qa/android_api_compat_probe.sh`，用于读取 API 36 / 37 设备版本、Manifest 权限、AppOps，并可按需启用 `RESTRICT_LOCAL_NETWORK` compat flag。
- [x] 增加 debug-only API 兼容 QA 入口与 `scripts/qa/android_api36_p0_smoke.sh`，用于采集 API 36 页面、URI grant、分享导入、Shell、全屏、uCrop、通知证据。
- [x] 增加 API 36 P0-A / P0-B 专用 QA 脚本：`scripts/qa/android_api36_p0a_display_input_accessibility.sh` 采集显示、输入、返回、无障碍证据，`scripts/qa/android_api36_p0b_runtime_native_smoke.sh` 采集 16 KB native library 与运行时依赖证据。
- [x] 扩展 `scripts/qa/android_api_compat_probe.sh`，输出 page size、显示 / 窗口尺寸、已启用 compat flags、alarm / job / download 状态；API 36 真机当前报告 4096-byte page 与 `1220x2656 @ 520dpi`。

## P1 - API 37 强制适配

### P1-A 本地网络、HTTP、传输安全

- [x] 为 MCP 局域网模式补充 Android 17 本地网络权限声明。
- [x] 在启动 MCP 局域网模式前请求本地网络权限。
- [x] 将 GitHub 分享链接、GitHub 下载器入口、GameKee / BA 图鉴资源链接里的 `http://` 规范化到 `https://`，并限制 DownloadManager 只接收 HTTPS 外部下载 URL。
- [x] 审计 MCP loopback HTTP 端点：API 37 AVD 开启本地网络 compat flag 后合法端口可达，未发现需要 Network Security Config 的拦截。
- [ ] 继续审计 MCP 局域网 HTTP 端点；API 37 / OEM Beta 实测拦截合法同网段流量时补充最小范围 Network Security Config。
- [ ] 评估 OkHttp HTTPS 加固准备度，覆盖 Android 17 CT 默认行为、Certificate Transparency opt-in 缺口、ECH 兼容性，并结合 GitHub、GitHub 下载跳转、GameKee、BA 媒体 CDN、MCP loopback 实测决定落地范围。

### P1-B 后台调度、资源画像、退出信号

- [x] 注册 Android 17 资源异常 profiling trigger。
- [x] 读取历史进程退出记录，识别 MemoryLimiter 或 excessive resource usage 信号。
- [x] 为 GitHub 与 BA 后台 tick 增加公平资源调度适配，减少固定轮询和 idle 强唤醒。
- [ ] 在 Android 17 AVD 上做长 idle profiling，检查 alarm window、唤醒次数、历史退出日志，并和 GitHub / BA 公平调度策略对比。

### P1-C Activity 启动、URI Grant、IntentSender

- [x] 为非 Home 背景裁剪的 FileProvider / uCrop 链路增加显式 URI grant 辅助。
- [ ] 在 Android 17 上继续验证自定义媒体保存、ZIP 导出等 URI grant 链路；chooser 或第三方目标丢权限时补充显式包授权。
- [x] 为 MCP 与 GitHub 通知点击打开 App 的 PendingIntent 增加用户可见通知动作专用的后台 Activity 启动 allowance。
- [ ] 继续审计 IntentSender 拉起界面的链路，适配 Android 17 后台启动 Activity 行为。

### P1-D 输入、媒体生命周期

- [x] 为 BA 图鉴媒体播放增加 Android 17 前台播放保护，并完成 audio hardening 验证。
- [ ] 在 Android 17 物理键盘与系统显示密码设置下验证 GitHub API Token 密码遮罩；Compose 遮罩和平台行为不一致时再接入 `ShowSecretsSetting.shouldShowPassword(...)`。
- [ ] 在 Android 17 上复验 BA 图鉴 BGM / 视频前台播放保护，覆盖切后台、锁屏、分屏、通知栏、低电量模式；在明确加入 MediaSession service 前保持当前播放生命周期契约。

### P1-E 通知、Advanced Protection、Hidden API

- [x] 验证 MCP 通知在 Android 17 promoted notification 与 Live Update 规则下的表现。
- [ ] 验证 GitHub、BA AP / 咖啡厅 / 竞技场、超级岛通知在 Android 17 promoted notification 与 Live Update 规则下的表现。
- [x] 新增 Advanced Protection Mode 状态检测入口，先在 About 构建信息中暴露当前状态。
- [ ] 为 Advanced Protection Mode 制定行为降级方案，覆盖 Shizuku、包枚举、电池优化、本地 MCP 局域网等高风险能力入口。
- [ ] 审计 Shizuku、系统设置辅助、保活权限辅助中的 hidden API / 反射风险；有官方替代路径时替换为文档化 fallback。
- [ ] 依赖升级后继续审计 Android 17 MessageQueue / `static final` 反射风险；当前 ActivityOptions、Shizuku、AppOps、OEM 设置反射路径保持公开方法探测或状态门控 fallback。
- [ ] `packageDebug` / `packageBenchmark` 后验证 Android 17 Safer Native DCL 与打包 native library 表现；源码当前项目 JNI / `System.load*` 命中数为 0，依赖 `.so` 仍需 read-only 与 16 KB 检查。

### P1-F OEM Beta 矩阵

- [ ] 在 HyperOS、ColorOS、OriginOS、MagicOS、One UI 的 Android 17 OEM Beta 设备可用后做兼容性测试。

## P2 - 可选 API、无障碍、大屏、长期跟踪

### P2-A 可选平台 API

- [x] Android 36.1 / 37.x 细分 API 使用前的统一 `SDK_INT_FULL` 判断辅助已加入。
- [ ] 评估 Android 16 embedded Photo Picker 用于未来非 Home 背景图片导入或 BA 媒体选择；在 picker UX 与 URI grant 验证完成前保持当前 SAF 兼容基线。
- [ ] 项目出现联系人导入需求时，优先评估 Android 17 Contact Picker，避免引入 `READ_CONTACTS` 宽权限。
- [ ] 项目出现跨设备连续性需求时，再评估 Android 17 Handoff API。

### P2-B 发布、工具链、依赖跟踪

- [ ] 跟踪 Android 17 post-quantum / hybrid APK signing 在 AGP、apksigner、GitHub Actions、本地发布 keystore 中的稳定支持，再规划发布管线调整。
- [ ] 继续跟踪 OkHttp、Ktor、Coil、Media3、Navigation3、Activity Compose 对 Android 36 / 37 新平台能力的稳定版支持。

### P2-C 性能、触感、遥测

- [ ] 当前动画 / 材质表现稳定后，再评估 richer haptics 与 frame-rate API 在液态底栏、slider、画廊视频、全屏媒体中的收益。
- [ ] 性能遥测路线需要更深平台信号时，再评估 `ApplicationStartInfo`、`reportFullyDrawn`、allow-while-idle alarm listener API、JobScheduler `JobDebugInfo`。

### P2-D 无障碍与包容性体验

- [ ] 当前核心 UI 迭代稳定后，在 Android 16 上做 TalkBack / 无障碍 smoke，覆盖 GitHub 分享导入、BA 拉取 / 保存 / 打包导出、Settings 权限、OS Shell 输出、通知权限弹窗；运行反馈沉默或重复时改用 pane title、焦点移动、live-region 语义。
- [ ] 自定义 Compose 输入面稳定后，验证 Android 17 文本变化无障碍反馈：`GlassSearchField`、OS Shell 命令输入、GitHub Token 输入、BA 图鉴筛选；覆盖 CJKV 输入法、物理键盘、TalkBack、文本选择。

### P2-E 大屏与窗口模式

- [ ] 在 API 36 `sw600dp` / 桌面窗口模式下 smoke MainActivity、GitHub 分享导入、uCrop、视频全屏页、OS Shell Runner；先记录方向、resizeability、edge-to-edge、IME 破损点，再进入大屏重构。
- [ ] 制定平板、折叠屏、桌面模式、横屏窗口尺寸下的大屏导航方案。
- [ ] 审计依赖竖屏假设的页面，标记需要双栏或 Navigation Rail 的界面。
- [ ] 在真正改布局前建立模拟器或云设备验证流程。
- [ ] 在 compact、medium、expanded 三类宽度下验证 Home、OS、BA、MCP、GitHub、Settings、About。
- [ ] 计划进入大屏实现阶段时重新评估 Manifest 里的方向限制。
