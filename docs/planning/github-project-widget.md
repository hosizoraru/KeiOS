# GitHub Project Widget / GitHub 项目追踪小组件

This page describes the implementation in current source. It is separate from the
[OEM research](android-appwidget-oem-study.md) and published release notes.

## 添加与使用

在 **GitHub 页顶栏 → 小组件图标**打开预览与添加页。先选择是否显示项目名称，
再点“添加到桌面”。支持 Pin 的桌面会处理系统确认；只有收到新实例的成功回调后，
应用才显示已添加。请求被桌面接受不代表已经添加成功。

部分 OEM 桌面可能受添加权限、组件入口或分发方式限制。如未出现确认界面，
可点“打开桌面”，长按空白处进入小组件选择器，选择 KeiOS 的“项目追踪摘要”。
部分 HyperOS 需要先进入“Android 小组件”。普通 Android 组件不等同于已接入
厂商组件库的卡片，KeiOS 不会设置审核通过标记或代替用户授权。

默认申请 2×1 横条，实际格子与 dp 尺寸由桌面决定。添加后长按并调整边缘：

| 布局 | 最小内容区，正常字号 | 展示内容 |
| --- | --- | --- |
| 小横条 | 110×72 dp | 更新数、总数与检查日期；空间有限时优先未检查、失败等状态 |
| 宽横条 | 240×72 dp | 更新数、追踪总数、需检查数量与检查日期 |
| 窄版摘要 | 110×152 dp | 更新数、已检查比例、状态、检查时间和可选的首个项目 |
| 窄宽版详情 | 240×144 dp | 摘要栏、最多两条两行项目记录、检查时间和“检查”按钮 |
| 短宽版面板 | 300×144 dp | 两列、最多四个项目组，名称与版本或状态紧邻显示 |
| 标准宽版面板 | 300×160 dp | 两列、最多四个项目组，正常字号另显示检查时的已装版本 |
| 纵向清单 | 110×268 dp | 最多四条项目记录，每条分两行显示名称和版本或状态 |
| 展开纵向清单 | 110×336 dp | 最多六条项目记录；项目不超过四项且为正常字号时，另显示已装版本 |
| 窄大面板 | 240×220 dp | 最多四条两行项目记录 |
| 大面板 | 300×220 dp | 两列、最多六个项目组，检查时间和“检查”按钮 |

这里的宽高指宿主提供的内容区，不等同于所有桌面的固定格子规格。常见的 2×2、
4×1、4×2、2×4 与更大规格会按实际宽高选择布局；横向和纵向都有各自的内容组织。
大字号优先保证可读性，增加部分布局的最小高度，并减少纵向、窄大面板与大面板的记录数。
标准宽版在放大字号时收起第三行已装版本；完整信息保留在无障碍描述中。
在 1.5 倍字号下，内容高度为 160 dp 的宽版先展示两条单列详情；双列短面板和标准面板
分别需要至少 176 dp 和 192 dp 内容高度。
同一字号下，纵向三条与四条详情分别需要至少 320 dp 和 384 dp 内容高度，
给 OEM 字体的行高留出余量。
预览展示宽版；刚添加时可能是小横条。

项目清单优先展示待更新、检查失败和未检查项目，其后是无更新提示与暂停项目。
只有待更新记录展示候选版本；其余记录明确显示检查状态，不把远端版本当作已安装版本。
已装版本来自同一条有效检查缓存，不额外扫描软件包；它反映检查时的版本。
单列详情在空间允许的更新记录中并列展示已装版本与候选版本。
“无更新”依据有效缓存和当前忽略策略，不等于忽略的版本已经安装。

项目名称默认隐藏。关闭名称时，宽版、纵向与大面板改为显示追踪、已检查、无更新、
未检查、失败与暂停六项统计，而不是留空项目区域。配置独立保存在每个组件实例中，
可通过桌面支持的重新配置入口修改。

组件展示应用最近保存的检查结果。点击背景打开 GitHub 页；宽版“检查”按钮打开
GitHub 页并执行现有检查流程。组件进程不直接联网，12 小时平台周期也只重新读取
本地快照，不保证定时执行远程更新。无数据、未检查、失败和暂停追踪分别显示，
不会把未知状态解释为全部已更新。

## Implementation

- Source: `app/src/main/java/os/kei/appwidget/github/`. Standard XML `RemoteViews`,
  with ten size-selected variants: strip, banner, compact, a narrow wide list,
  two wide grids, two tall lists, and narrow/wide expanded panels.
- Updates use valid sizes advertised in each widget's options to select those variants.
  When sizes are absent, the standard responsive layout map remains available to the host.
  This also avoids defaulting a single-size panel to the strip when a host applies an update
  without passing its measured view size.
- The provider and private pin-confirmation receiver use `:widgetProvider`; application
  startup in that process skips the main-process initialization.
- The main process projects GitHub persistence changes into a bounded, atomic JSON snapshot.
  Cross-process file locking protects snapshot and per-instance configuration updates.
  No widget-process HTTP client or repeated status polling is introduced.
- Counts follow the existing valid-cache and release-ignore rules. Each project contributes
  at most one update; an eligible stable release takes precedence over a prerelease.
  Up to six bounded name/version/status records are retained, with actionable states first.
  Installed versions remain separate from candidate versions. Persisted project statuses use
  explicit storage IDs with legacy-name compatibility rather than R8-sensitive enum names.
- Direct activity PendingIntents distinguish widget instances and open/check actions.
  The pin callback is explicit, one-shot and mutable only so the system can supply the new ID.
  Ownership, request token, expiry, and previously existing IDs are checked before applying
  the requested privacy setting.
- ID restoration, reconfiguration, deletion, process recovery and privacy changes retain
  their own persistent state contracts. Each update replaces the nested content view, including
  accessibility descriptions, even when the host reuses its outer view.
- Light/dark colors and normal/large-font layouts use native text and drawable resources.
  The soft-light material combines a translucent base, diffuse light, edge reflection and
  separate project surfaces. There is no wallpaper sampling or animated glass renderer in
  the host. See [the system-widget and material study](github-widget-material-study.md)
  for the inspected references and native Xiaomi glass boundary.

## Verification Boundaries

Focused tests cover projection, persistence, pin identity, configuration and native layout
rendering. Native raster checks include minimum dimensions, supported languages, dark mode and
1.5× font. On 2026-10-27, an R8 diagnostic build was checked on a physical HyperOS 4 launcher:
the existing 4×2 widget retained its layout through package replacement and configuration
changes, privacy mode removed project details from the accessibility tree, and the background
opened GitHub tracking. The English import preview was also checked with a real export.
Local evidence is retained in ignored `.planning/github-widget-physical-20261027/`.

HyperOS launcher behavior depends on its installed version, permissions and device setup.
Other device sizes and display configurations require separate validation. This implementation
does not establish OPPO/vivo/HONOR catalog approval, power measurements, or a completed Baseline
Profile refresh.

## English Usage

Open **GitHub toolbar → widget icon**, choose whether to show project names, then request
addition. If the launcher does not show a confirmation, use its widget picker; some HyperOS
versions place standard widgets under “Android widgets.”

The default is a small update-count strip. Resize it into a banner, summary, grouped wide
panel, tall list or large panel. Wide panels use two columns for up to four or six projects;
narrower panels use one column. Projects prioritize updates and unsuccessful checks.
Normal text can show the installed version recorded by the check separately from its update
candidate. Large fonts reduce secondary detail and row count. With names off, panels show
six count statistics across the content area.
Names are off by default and configured per widget. The widget reads saved results; its platform
refresh does not initiate a network check. Tap the background to open GitHub, or use **Check**
in a list/panel layout to open GitHub and start the normal check workflow.
