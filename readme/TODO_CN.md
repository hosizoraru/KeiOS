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
[Android 17 target SDK 行为变更](https://developer.android.com/about/versions/17/behavior-changes-17)、
[Android 17 全应用行为变更](https://developer.android.com/about/versions/17/behavior-changes-all)、
[Android 17 新特性与 API](https://developer.android.com/about/versions/17/features)、
[本地网络权限](https://developer.android.com/privacy-and-security/local-network-permission)。

- [x] 构建基线为 `minSdk=35`、`compileSdk=37`、`targetSdk=37`，Java / Kotlin toolchain 为 21。
- [x] Manifest 已声明 `ACCESS_LOCAL_NETWORK`、`USE_LOOPBACK_INTERFACE`、`POST_PROMOTED_NOTIFICATIONS`、`FOREGROUND_SERVICE_SPECIAL_USE`。
- [x] 主 Activity 已启用 `enableEdgeToEdge()`，启动图标已提供 monochrome 资源，主导航与图鉴全屏图已接入预测返回路径。
- [x] 源码搜索当前无 Contacts、Bluetooth、自定义 RemoteViews、`MediaStore#getVersion()`、`scheduleAtFixedRate`、WorkManager、JobScheduler 直接适配面。

## P0 - API 36 主力机验收

- [ ] 在 Android 16 / API 36 真机或 AVD 上回归 Home、OS、BA、MCP、GitHub、Settings、About、GitHub 分享导入窗口、uCrop、视频全屏页的 edge-to-edge、IME、状态栏、导航栏 inset 表现。
- [ ] 全量验证预测返回：Nav3 主页面、设置页、学生图鉴详情、图鉴全屏图片、OS Shell Runner、GitHub 分享导入 Activity，并覆盖手势导航与三键导航。
- [ ] 使用 Android 16 compat flag `RESTRICT_LOCAL_NETWORK` 验证 MCP loopback / 局域网模式；必要时评估为 API 36 opt-in 测试补充 `NEARBY_WIFI_DEVICES` 的受限声明与授权说明。
- [ ] 审计 Android 16 Safer Intents：GitHub `ACTION_SEND` 导入 Activity、下载器选择、外部链接打开、通知 / shortcut extras，确保外部输入都有 action、mime、scheme、host、package 边界校验。
- [ ] 验证 DownloadManager、GitHub 后台刷新、BA AP / 咖啡厅 / 竞技场提醒在 Android 16 空闲、锁屏、省电模式下的调度表现；保留当前无 fixed-rate work 路径的源码审计结果。
- [ ] 验证 MCP `specialUse` 前台服务在 Android 16 后台启动、通知权限拒绝、省电策略、Shizuku 未激活状态下的恢复路径。
- [ ] 验证 MCP / GitHub / BA / 超级岛通知在 Android 16 promoted notification 与 `NotificationCompat.ProgressStyle` 下的视觉和点击行为。
- [ ] 审计 `setAccessible` / hidden API 风险点，优先覆盖 Shizuku、HyperOS 设置跳转、保活权限辅助路径。

## P1 - API 37 强制适配

- [x] 为 MCP 局域网模式补充 Android 17 本地网络权限声明。
- [x] 在启动 MCP 局域网模式前请求本地网络权限。
- [x] 注册 Android 17 资源异常 profiling trigger。
- [x] 读取历史进程退出记录，识别 MemoryLimiter 或 excessive resource usage 信号。
- [x] 为 GitHub 与 BA 后台 tick 增加公平资源调度适配，减少固定轮询和 idle 强唤醒。
- [x] 为 BA 图鉴媒体播放增加 Android 17 前台播放保护，并完成 audio hardening 验证。
- [ ] 审计 MCP loopback / 局域网 HTTP 端点，以及 GameKee / GitHub 跳转资源里的明文网络策略；API 37 实测拦截合法流量时补充 Network Security Config。
- [ ] 在 Android 17 上验证 FileProvider、uCrop、自定义媒体保存、ZIP 导出等 URI grant 链路；chooser 或第三方目标丢权限时补充显式包授权。
- [ ] 审计 PendingIntent 与 IntentSender 拉起界面的链路，适配 Android 17 后台启动 Activity 行为；显式放行仅用于用户可见的通知动作。
- [ ] 验证 MCP、GitHub、BA AP / 咖啡厅 / 竞技场、超级岛通知在 Android 17 promoted notification 与 Live Update 规则下的表现。
- [ ] 为 Advanced Protection Mode 制定兼容方案，覆盖 Shizuku、包枚举、电池优化、本地 MCP 局域网等高风险能力入口。
- [ ] 在 Android 17 AVD 上做长 idle profiling，检查 alarm window、唤醒次数、历史退出日志，并和 GitHub / BA 公平调度策略对比。
- [ ] 审计 Shizuku、系统设置辅助、保活权限辅助中的 hidden API / 反射风险；有官方替代路径时替换为文档化 fallback。
- [ ] 评估 OkHttp HTTPS 加固准备度，覆盖 Certificate Transparency opt-in 与 ECH 兼容性，并结合 GitHub / GameKee 端点实测决定落地范围。
- [ ] 在 HyperOS、ColorOS、OriginOS、MagicOS、One UI 的 Android 17 OEM Beta 设备可用后做兼容性测试。

## P2 - 可选新 API 与长期跟踪

- [ ] Android 36.1 / 37.x 细分 API 真正使用前，引入统一 `SDK_INT_FULL` / extension version 判断辅助，避免只用 `SDK_INT` 误判小版本能力。
- [ ] 项目出现联系人导入需求时，优先评估 Android 17 Contact Picker，避免引入 `READ_CONTACTS` 宽权限。
- [ ] 项目出现跨设备连续性需求时，再评估 Android 17 Handoff API。
- [ ] 继续跟踪 OkHttp、Ktor、Coil、Media3、Navigation3、Activity Compose 对 Android 36 / 37 新平台能力的稳定版支持。

## P2 - 大屏适配

- [ ] 制定平板、折叠屏、桌面模式、横屏窗口尺寸下的大屏导航方案。
- [ ] 审计依赖竖屏假设的页面，标记需要双栏或 Navigation Rail 的界面。
- [ ] 在真正改布局前建立模拟器或云设备验证流程。
- [ ] 在 compact、medium、expanded 三类宽度下验证 Home、OS、BA、MCP、GitHub、Settings、About。
- [ ] 计划进入大屏实现阶段时重新评估 Manifest 里的方向限制。
