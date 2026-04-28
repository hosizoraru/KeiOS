# KeiOS 待办清单

这里记录需要稳定设备、OEM Beta 或集中验证窗口才能继续推进的路线项。

## Android 17 适配

- [x] 为 MCP 局域网模式补充 Android 17 本地网络权限声明。
- [x] 在启动 MCP 局域网模式前请求本地网络权限。
- [x] 注册 Android 17 资源异常 profiling trigger。
- [x] 读取历史进程退出记录，识别 MemoryLimiter 或 excessive resource usage 信号。
- [x] 为 GitHub 与 BA 后台 tick 增加公平资源调度适配，减少固定轮询和 idle 强唤醒。
- [ ] 验证 Android 17 下 BA 图鉴媒体的后台音频行为。
- [ ] 在 HyperOS、ColorOS、OriginOS、MagicOS、One UI 的 Android 17 OEM Beta 设备可用后做兼容性测试。

## 大屏适配

- [ ] 制定平板、折叠屏、桌面模式、横屏窗口尺寸下的大屏导航方案。
- [ ] 审计依赖竖屏假设的页面，标记需要双栏或 Navigation Rail 的界面。
- [ ] 在真正改布局前建立模拟器或云设备验证流程。
- [ ] 在 compact、medium、expanded 三类宽度下验证 Home、OS、BA、MCP、GitHub、Settings、About。
- [ ] 计划进入大屏实现阶段时重新评估 Manifest 里的方向限制。
