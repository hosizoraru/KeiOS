# KeiOS 待办清单

这里记录需要稳定设备、OEM Beta 或集中验证窗口才能继续推进的路线项。

## Android 17 适配

官方资料核对日期：2026-04-29。
参考：[target SDK 行为变更](https://developer.android.com/about/versions/17/behavior-changes-17)、
[全应用行为变更](https://developer.android.com/about/versions/17/behavior-changes-all)、
[新特性与 API](https://developer.android.com/about/versions/17/features)。

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

### Android 17 源码审计备注

- Contacts、Bluetooth、自定义 RemoteViews 路径经源码搜索后当前无适配面。
- 大屏行为变更放在大屏适配章节跟踪，因为项目当前暂无大屏实现计划。

## 大屏适配

- [ ] 制定平板、折叠屏、桌面模式、横屏窗口尺寸下的大屏导航方案。
- [ ] 审计依赖竖屏假设的页面，标记需要双栏或 Navigation Rail 的界面。
- [ ] 在真正改布局前建立模拟器或云设备验证流程。
- [ ] 在 compact、medium、expanded 三类宽度下验证 Home、OS、BA、MCP、GitHub、Settings、About。
- [ ] 计划进入大屏实现阶段时重新评估 Manifest 里的方向限制。
