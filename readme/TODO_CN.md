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
- [x] Manifest 已声明 `NEARBY_WIFI_DEVICES(maxSdk=36)`、`ACCESS_LOCAL_NETWORK`、`USE_LOOPBACK_INTERFACE`、`POST_PROMOTED_NOTIFICATIONS`、`FOREGROUND_SERVICE_SPECIAL_USE`。
- [x] 主 Activity 已启用 `enableEdgeToEdge()`，启动图标已提供 monochrome 资源，主导航与图鉴全屏图已接入预测返回路径，并显式开启 `OnBackInvokedCallback`。
- [x] 源码搜索当前无 Contacts、Bluetooth、自定义 RemoteViews、`MediaStore#getVersion()`、`scheduleAtFixedRate`、WorkManager、JobScheduler 直接适配面。
- [x] 新增统一 API 版本辅助，覆盖 `SDK_INT_FULL`、API 36、API 36.1、API 37 判断。
- [x] About 构建信息中增加 Runtime API Full 与 Advanced Protection Mode 状态检测。

## P0 - API 36 主力机验收

- [ ] 在 Android 16 / API 36 真机或 AVD 上回归 Home、OS、BA、MCP、GitHub、Settings、About、GitHub 分享导入窗口、uCrop、视频全屏页的 edge-to-edge、IME、状态栏、导航栏 inset 表现。
- [ ] 全量验证预测返回：Nav3 主页面、设置页、学生图鉴详情、图鉴全屏图片、OS Shell Runner、GitHub 分享导入 Activity，并覆盖手势导航与三键导航。
- [x] 为 HyperOS / MIUI / Xiaomi、ColorOS、OriginOS、MagicOS、EMUI、One UI 增加预测返回 OEM 兼容策略，主导航返回动画会跟随左右边缘方向。
- [x] 在 Android 17 / API 37 AVD 上完成预测返回 smoke：Home -> 设置 -> 左边缘返回、Home -> About -> 右边缘返回，logcat 确认进入 `CoreBackPreview` 回调路径。
- [x] 为 API 36 本地网络限制测试补充 `NEARBY_WIFI_DEVICES(maxSdk=36)`，并把 MCP 局域网权限请求统一接入 API 36 / API 37 权限辅助。
- [x] 增加 `scripts/qa/android_api_compat_probe.sh`，用于读取 API 36 / 37 设备版本、Manifest 权限、AppOps，并可按需启用 `RESTRICT_LOCAL_NETWORK` compat flag。
- [x] 在 Android 17 / API 37 AVD 开启 `RESTRICT_LOCAL_NETWORK` 后验证 MCP loopback：`127.0.0.1:38888/mcp` 可达并返回预期鉴权拦截，暂不需要 Network Security Config。
- [ ] 使用 Android 16 / API 36 设备继续验证 MCP 局域网模式在 `RESTRICT_LOCAL_NETWORK` 下的同网段访问。
- [x] 加固 GitHub `ACTION_SEND` 导入 Activity、下载器选择、外部链接打开、分享 APK 链接等 Safer Intents 高频链路，确保 action、mime、scheme、host、package 边界更明确。
- [x] 完成用户自定义 OS shortcut intents、通知 / shortcut extras 首轮审计：MainActivity 外部 extras 已按目标页和动作白名单配对，OS Activity card 启动前补充目标可解析校验，MCP 栈顶快捷入口已用 `singleTop` 验证可稳定启动/停止。
- [x] 在 Android 17 / API 37 AVD 上完成外部 extras smoke：合法 GitHub 快捷动作进入 GitHub，错配动作进入目标页但丢弃动作，未知目标回落 Home。
- [ ] 验证 DownloadManager、GitHub 后台刷新、BA AP / 咖啡厅 / 竞技场提醒在 Android 16 空闲、锁屏、省电模式下的调度表现；保留当前无 fixed-rate work 路径的源码审计结果。
- [x] 在 Android 17 / API 37 AVD 验证 MCP `specialUse` 前台服务：前台启动被系统允许，通知权限拒绝时服务保持前台运行且无崩溃，权限恢复后可正常停止。
- [ ] 验证 MCP `specialUse` 前台服务在 Android 16 后台启动、省电策略、Shizuku 未激活状态下的恢复路径。
- [x] 在 Android 17 / API 37 AVD 验证 MCP Live Update / promoted notification：通知记录包含 `PROMOTED_ONGOING`、`ProgressStyle` 动作、打开/停止 PendingIntent allowlist，点击链路保持现有通知框架。
- [ ] 验证 GitHub / BA / 超级岛通知在 Android 16 promoted notification 与 `NotificationCompat.ProgressStyle` 下的视觉和点击行为。
- [x] 完成 `setAccessible` / hidden API 首轮收敛：HyperOS 设置跳转与保活权限辅助的系统属性读取改用统一 PropUtils，AppOpsManagerInjector 改为公开方法探测；Shizuku private `newProcess` 兼容入口保留并受状态门控。

## P1 - API 37 强制适配

- [x] 为 MCP 局域网模式补充 Android 17 本地网络权限声明。
- [x] 在启动 MCP 局域网模式前请求本地网络权限。
- [x] 注册 Android 17 资源异常 profiling trigger。
- [x] 读取历史进程退出记录，识别 MemoryLimiter 或 excessive resource usage 信号。
- [x] 为 GitHub 与 BA 后台 tick 增加公平资源调度适配，减少固定轮询和 idle 强唤醒。
- [x] 为 BA 图鉴媒体播放增加 Android 17 前台播放保护，并完成 audio hardening 验证。
- [x] 将 GitHub 分享链接、GitHub 下载器入口、GameKee / BA 图鉴资源链接里的 `http://` 规范化到 `https://`，并限制 DownloadManager 只接收 HTTPS 外部下载 URL。
- [x] 审计 MCP loopback HTTP 端点：API 37 AVD 开启本地网络 compat flag 后合法端口可达，未发现需要 Network Security Config 的拦截。
- [ ] 继续审计 MCP 局域网 HTTP 端点；API 37 / OEM Beta 实测拦截合法同网段流量时补充最小范围 Network Security Config。
- [x] 为非 Home 背景裁剪的 FileProvider / uCrop 链路增加显式 URI grant 辅助。
- [ ] 在 Android 17 上继续验证自定义媒体保存、ZIP 导出等 URI grant 链路；chooser 或第三方目标丢权限时补充显式包授权。
- [x] 为 MCP 与 GitHub 通知点击打开 App 的 PendingIntent 增加用户可见通知动作专用的后台 Activity 启动 allowance。
- [ ] 继续审计 IntentSender 拉起界面的链路，适配 Android 17 后台启动 Activity 行为。
- [x] 验证 MCP 通知在 Android 17 promoted notification 与 Live Update 规则下的表现。
- [ ] 验证 GitHub、BA AP / 咖啡厅 / 竞技场、超级岛通知在 Android 17 promoted notification 与 Live Update 规则下的表现。
- [x] 新增 Advanced Protection Mode 状态检测入口，先在 About 构建信息中暴露当前状态。
- [ ] 为 Advanced Protection Mode 制定行为降级方案，覆盖 Shizuku、包枚举、电池优化、本地 MCP 局域网等高风险能力入口。
- [ ] 在 Android 17 AVD 上做长 idle profiling，检查 alarm window、唤醒次数、历史退出日志，并和 GitHub / BA 公平调度策略对比。
- [ ] 审计 Shizuku、系统设置辅助、保活权限辅助中的 hidden API / 反射风险；有官方替代路径时替换为文档化 fallback。
- [ ] 评估 OkHttp HTTPS 加固准备度，覆盖 Certificate Transparency opt-in 与 ECH 兼容性，并结合 GitHub / GameKee 端点实测决定落地范围。
- [ ] 在 HyperOS、ColorOS、OriginOS、MagicOS、One UI 的 Android 17 OEM Beta 设备可用后做兼容性测试。

## P2 - 可选新 API 与长期跟踪

- [x] Android 36.1 / 37.x 细分 API 使用前的统一 `SDK_INT_FULL` 判断辅助已加入。
- [ ] 项目出现联系人导入需求时，优先评估 Android 17 Contact Picker，避免引入 `READ_CONTACTS` 宽权限。
- [ ] 项目出现跨设备连续性需求时，再评估 Android 17 Handoff API。
- [ ] 继续跟踪 OkHttp、Ktor、Coil、Media3、Navigation3、Activity Compose 对 Android 36 / 37 新平台能力的稳定版支持。

## P2 - 大屏适配

- [ ] 制定平板、折叠屏、桌面模式、横屏窗口尺寸下的大屏导航方案。
- [ ] 审计依赖竖屏假设的页面，标记需要双栏或 Navigation Rail 的界面。
- [ ] 在真正改布局前建立模拟器或云设备验证流程。
- [ ] 在 compact、medium、expanded 三类宽度下验证 Home、OS、BA、MCP、GitHub、Settings、About。
- [ ] 计划进入大屏实现阶段时重新评估 Manifest 里的方向限制。
