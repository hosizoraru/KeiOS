# Android 小组件与 OEM 扩展调研

调研日期：2026-10-26。KeiOS 源码基点：`695220c85`。

本文保留最初的功能选型与实现准备调研。调研阶段只整理文档；后续已开始实现标准
AppWidget，见 [GitHub 项目追踪小组件](github-project-widget.md)。本文中的厂商契约
与待验收项不代表该实现已获得厂商准入或通过全部设备验收。

## 结论与证据范围

建议先建立标准 Android AppWidget 能力，再按宿主能力适配厂商扩展。
“能在桌面添加”“进入厂商组件库”“进入负一屏服务分发”是不同的验收目标；
写入厂商 meta-data 并不等于已经获得组件库展示或分发资格。

| 来源 | 相关技术路径 | 本轮确认的重点 | 证据范围 |
| --- | --- | --- | --- |
| AOSP | AppWidget 服务、桌面 Host、Pin 请求 | 宿主负责展示和加桌确认；服务端规定周期更新下限与位图内存限制 | 官方规范、Android 15 固定标签源码 |
| Google | AppWidget / RemoteViews / Jetpack Glance | 生命周期、持久化状态、自适应布局、更新与图片预算 | 官方开发指南、API 与版本文档 |
| Xiaomi | 标准 AppWidget 加小米扩展 | 独立进程、曝光刷新、组件版本、恢复协议、单独审核 | 官方技术、设计、审核、提交说明的公开索引正文 |
| OPPO | 泛在服务 / SeedlingSupportSDK 等卡片路径 | 桌面、负一屏、小组件、流体云属于不同能力与版本 | 官方索引可读；具体接入正文受限 |
| vivo | 原生 AppWidget 加原子组件协议；另有快应用卡片 | 10M 桌面内存、12 小时周期、固定规格、禁止滑动、版本与审核 | 旧版官方门户的完整技术、产品、接入正文 |
| HONOR | 原生 widget 卡片；另有 JS 卡片 | 发现标识、荣耀专用 provider、宿主圆角、折叠屏、审核 | 官方原生开发正文及设计、审核入口 |

部分公开规范仍以 MIUI、MagicUI 和早期 OriginOS 为背景。下文记录的是
查到的公开契约，不能替代当前 HyperOS / ColorOS / OriginOS / MagicOS
具体机型、桌面版本、地区和发行渠道的实测。没有找到完整协议的部分标为待确认。

## AOSP 与 Google：共同基础

### 渲染、状态与交互

标准 AppWidget 的界面由外部宿主承载。经典 RemoteViews 仅支持规定的
布局、控件和操作；不能把现有 Miuix、Compose UI 或 Liquid Glass
Composable 直接放进去。Glance 使用 Compose runtime，但有自己的组件体系，
并不直接兼容 Compose UI。新 Remote Compose / DrawInstructions 路径需要另行验证
API 与宿主支持，不能据此承诺现有玻璃渲染器可以直接复用。
参见 [AppWidget 概览](https://developer.android.com/develop/ui/views/appwidgets)、
[RemoteViews API](https://developer.android.com/reference/android/widget/RemoteViews)、
[Glance 概览](https://developer.android.com/develop/ui/compose/glance)。

GlanceAppWidget 应视为被动、无状态的界面描述。业务数据、每个组件实例的配置、
展示快照与更新时间需要持久化，不能依赖应用进程中的 ViewModel 或 StateFlow
一直存活。进程被回收后，应仍能恢复选中的账号与组件内容。
参见 [Glance 状态与生命周期](https://developer.android.com/develop/ui/compose/glance/glance-app-widget)。

加桌前检查 `isRequestPinAppWidgetSupported()`，发出请求后按成功回调处理实例 ID；
方法返回接受请求不等于用户已完成添加。页面跳转使用直接启动 Activity 的
PendingIntent，避免通过 Receiver / Service 再启动页面。多实例需要独立的
Intent 身份，不能仅靠 extras 区分。普通固定跳转优先 immutable；需要宿主
填入实例 ID、配置或集合项信息的 PendingIntent 应单独确认可变性与显式目标。
参见 [AOSP 固定小组件](https://source.android.com/docs/core/display/widgets-shortcuts)、
[AppWidgetManager API](https://developer.android.com/reference/android/appwidget/AppWidgetManager)、
[小组件交互增强](https://developer.android.com/develop/ui/views/appwidgets/enhance)。

### 更新、尺寸与平台差异

- `updatePeriodMillis` 的平台周期下限为 30 分钟，`0` 表示关闭该周期机制。
  业务数据变更或用户操作可以触发更新；WorkManager 仍受后台调度限制，
  不能承诺准点刷新。Receiver 内避免网络和耗时操作。
  参见 [高级更新机制](https://developer.android.com/develop/ui/views/appwidgets/advanced)。
- 按宿主实际提供的 dp 尺寸设计响应式布局。`2x2` 等格子规格不是跨桌面的
  固定像素尺寸；手机、平板、折叠和旋转状态需要不同布局验收。
  参见 [小组件布局](https://developer.android.com/develop/ui/views/appwidgets/layouts)。
- 标准集合组件允许滚动；小型集合优先评估 `RemoteCollectionItems`，
  不必从旧 RemoteViewsService 方案起步。旧 `setRemoteAdapter(viewId, Intent)`
  已在 API 35 弃用。厂商审核可能另外禁止滚动。
  参见 [集合组件](https://developer.android.com/develop/ui/views/appwidgets/collections)、
  [RemoteViews API](https://developer.android.com/reference/android/widget/RemoteViews)。
- API 35 已有 `setWidgetPreview()` 生成预览能力。仍需准备兼容的静态预览、
  空状态和不含真实私人数据的展示内容。
  参见 [预览 API](https://developer.android.com/reference/android/appwidget/AppWidgetManager#setWidgetPreview(android.content.ComponentName,int,android.widget.RemoteViews))。
- Android 15 的 force-stop 会取消 PendingIntent，并停用、置灰应用的小组件，
  直到用户重新启动应用。普通进程回收、强行停止、停用与各厂商“冻结”
  必须分开验证，不能承诺应用被强行停止后组件仍持续更新。
  参见 [Android 15 stopped state 变化](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state)。

### 位图内存：不要误记为 Android 17 才有上限

Google 的 [Android 17 target 行为说明](https://developer.android.com/about/versions/17/behavior-changes-17)
明确要求 target 37 的 RemoteViews 更新遵守图片内存预算，给出
`6 * displayWidth * displayHeight` 字节的计算方式，并说明多尺寸与方向变体
需要合并考虑。URI 图片仍由宿主解码，不能作为无限内存的替代路径。

但固定版本的
[AOSP Android 15 AppWidgetServiceImpl](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-15.0.0_r1/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java)
已经在 `computeMaximumWidgetBitmapMemory()` 设置相同形式的上限，
在 `updateAppWidgetInstanceLocked()` 对普通应用超限抛出异常。
这与新文档中旧 target 仅告警的描述，不能合并成“所有旧系统都只告警”。
本轮未据此推断任一 OEM 当前实现；实施时应核对目标版本源码与实际更新结果。

KeiOS 的设计应从 API 35 起控制图片预算：按实际显示尺寸解码、复用共同图片、
限制变体数量；替换图片时构建新的完整 RemoteViews，避免部分更新累计图片动作；
保留无位图的失败布局。还需要同时检查序列化大小、宿主解码内存与厂商预算，
这些不是同一个指标。

## Xiaomi：原生扩展、独立进程与曝光刷新

主要来源：[技术规范](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1584)、
[设计规范](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1664)、
[审核标准](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1586)、
[提交说明](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1588)、
[FAQ](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1591)。
本轮读取到官方检索索引中的技术正文，技术页标注更新于 2024-10-17；
页面直接读取不稳定，提交前应再核对原页及当前审核要求。

| 协议点 | 公开规范含义 | KeiOS 后续应做的验证 |
| --- | --- | --- |
| `miuiWidget=true` | 标识小米组件 | 与普通 provider 的发现、重复展示和非小米兼容性 |
| `:widgetProvider` | Receiver / Service / Provider 使用专门组件进程，Activity 不在该进程 | 冷启动初始化与跨进程数据访问 |
| `miuiWidgetRefresh=exposure` | 支持宿主曝光刷新 | 不支持该能力的桌面仍有标准更新路径 |
| `miuiWidgetRefreshMinInterval` | 曝光刷新最小间隔，规范下限为 10 秒 | 本地刷新去重；该值不代表可每 10 秒联网 |
| `miui.appwidget.action.APPWIDGET_UPDATE` | 小米更新事件 | 与标准生命周期事件共同处理 |
| `miuiWidgetVersion` | application 级组件版本，组件变更时递增 | 与 APK 版本及审核记录分别管理 |
| ID 恢复协议 | 除标准恢复外有旧新 ID 与 `miuiIdChangedComplete` 协作 | 组件配置迁移、恢复完成确认与幂等 |

技术页规定组件进程内存不超过 **35M**，审核页出现 **40M**。
这是公开文档冲突；先按 35M 做保守设计预算，当前正式标准需提交前确认。
技术页还限制组件进程承担的工作和唤起其他进程；媒体播放器有专门例外，
不能把例外推广到普通信息卡片。

审核页禁止输入和滚动；设计页给出手机及平板规格。背景、根布局
`@android:id/background`、缩放和大屏编辑参数也有约束。大屏编辑 PendingIntent
需要允许宿主补充配置参数，应与普通点击跳转区别处理。旧示例的 flags
不能整段照搬到 target 37 的所有入口。

普通 Android widget 与进入小米组件中心的审核是两条路径。后者涉及应用上架
和单独提交；本轮没有申请或联系厂商。

## vivo：原子组件有明确的原生协议

原生入口位于旧版官方门户，不能只搜索新版快应用文档：
[原子组件产品入口](https://dev.vivo.com.cn/promote/atomicComponent)、
[产品规范](https://dev.vivo.com.cn/documentCenter/doc/841)、
[UI 技术规范](https://dev.vivo.com.cn/documentCenter/doc/845)、
[接入指导](https://dev.vivo.com.cn/documentCenter/doc/850)。
本轮通过浏览器读取完整正文；产品、技术页更新于 2024-11-15，
接入指导更新于 2025-08-14。

技术规范以原生 AppWidget 为基础，在每个组件 Receiver 上增加：

- `vivo_widget=true`、`vivo.widget.description`。
- `vivoWidgetVersion`：正整数、只能递增，按组件管理，与小米 application 级版本不同。
- 可选 `shortcutinfo0` 至 `shortcutinfo3`：长按菜单入口，需要单独核对目标 Activity 与路由。

产品与技术约束会影响功能设计：

- 组件在桌面的内存占用不得超过 **10M**；这不是“小米组件进程预算”的同一口径。
  技术页把单个或累计超过 **100K** 的 Bitmap 数据列为大数据风险；
  不能把它误写为 Android 通用 Binder 硬上限。
- 周期刷新间隔至少 **12 小时**，同时允许应用或用户主动触发刷新。
  这不意味着所有数据变更都必须等待 12 小时。
- 规格为 `2x2`、`4x2`、`4x4`；原子组件禁用拖拽缩放，禁止上下左右滑动。
  通用 AppWidget 的 resize/滚动体验不能直接作为原子组件方案。
- 每次更新重新构造 RemoteViews；避免大图传输，URI 图片同样需要控制宿主解码尺寸。
  背景不应依赖一张拉伸图片。
- 文档提供有限的布局补间动效，并不意味着能运行应用内的 Compose 玻璃动效。
  隐私、异常状态与预览的明暗主题也有独立要求。

接入指导明确要求应用已在 vivo 应用商店上架、首次申请接入权限、提交含组件的 APK
进行审核，再按通过的内容发布。大幅布局或功能变更与普通 bug 修复有不同版本规则。
文档所述无额外加桌界面的“一键添加”也以审核通过为前提，不能将它与标准
Pin 请求的通用能力混为一谈。

技术页的组件库跳转入口使用 `com.bbk.launcher2.permission.JUMP_ORIGIN`，
并注明设备版本及平板限制；示例还包含 `SystemProperties` 读取和版本浮点比较。
后续应先验证公开入口、可解析的宿主和能力支持，不直接复制 hidden API 及该版本比较方式。

另有 [快应用智能卡片](https://developers.vivo.com/product/d/quickApp)、
[添加负一屏卡片](https://developers.vivo.com/doc/d/a4f0fb7895b240a882df6afb68fc2703)
和 IoT 场景的服务接入。快应用页的 `@system.card` 属于另一条技术路径，
不能作为 KeiOS 原生 APK 的原子组件实现依据。

## HONOR：保留通用 provider，谨慎选择专用发现

来源：[原生开发指导](https://developer.honor.com/cn/doc/guides/100171)、
[设计指导](https://developer.honor.com/cn/doc/guides/100170)、
[测试指导](https://developer.honor.com/en/doc/guides/100173)、
[审核入口](https://developer.honor.com/cn/doc/guides/101481)。
原生开发正文通过浏览器读取；其兼容表以 MagicUI 6.0 为分界。

- 以 Google 原生 widget 为基础，增加 `com.hihonor.widget.type=honorcard`
  供荣耀快服务等入口识别。
- 将 `android.appwidget.provider` **替换**为 `android.appwidget.honor.provider`
  会按文档变为荣耀专用：MagicUI 6.0 以下和其他厂商不展示。
  因此通用组件不应直接做这一替换；普通 provider 加类型标记是优先验证的路径。
- 指南规定宿主裁切外层 16dp 圆角；内部图片仍需自行处理。
  平板、折叠屏按实际尺寸提供布局，不能只按手机规格放大。
- 文档禁止用原生 widget 生命周期回调做**埋点统计**，
  不等于禁止在 `onUpdate` 等回调中渲染和更新数据。
- 指南建议文字用 dp、避免随系统字体缩放，这是特定卡片契约。
  不能据此改掉整个应用或通用 widget 的可访问性策略；
  是否需要独立厂商布局，应结合当前审核与大字体验收决定。

原生 widget 卡片与 JS 卡片为不同接入路径；进入服务分发和完成普通桌面添加，
仍需分别验证。

## OPPO：确认框架，保留协议核验缺口

[官方索引中的版本定义与 SDK 更新记录](https://open.oppomobile.com/documentation/page/search?keyword=ColorOS+Watch)
可确认泛在服务及 SeedlingSupportSDK：

| 文档中的能力版本 | 对应系统 | 该版增加的相关入口 |
| --- | --- | --- |
| 1.0 / 1000001 | ColorOS 13.1+ | 桌面、负一屏卡片 |
| 1.1 / 1001001 | ColorOS 13.2+ | 小组件卡片 |
| 2.0 / 2000000 | ColorOS 14+ | 流体云卡片 |

SDK 索引可见 `SeedlingTool.isSupportSeedlingCard`、
`SeedlingTool.updateAllCardData`、`SeedlingHostEnum` 等能力。
这些版本是公开历史定义，不代表当前最新版。流体云与桌面组件虽出现在同一框架，
不能据此把通知场景当作桌面 AppWidget 协议。

[具体官方接入文档入口](https://open.oppomobile.com/new/developmentDoc/info?id=13330)
本轮未获得完整正文：页面读取失败，其匿名文档接口返回“登录失效”。
这只能证明该入口的本轮访问受限，不能推断所有 OPPO 文档都必须登录。
尚不能确认当前 SDK 坐标、签名/权限、准入流程、布局协议、更新配额和各宿主生命周期。
实施 OEM 能力前应补齐原页与 SDK 合同；本轮不虚构一个 manifest 开关作为接入方案。

## 对 KeiOS 的落地建议

以下是基于官方契约与当前源码的工程建议，尚未实现或完成性能验证。

当前 [版本目录](../../gradle/libs.versions.toml) 的 minSdk 为 **35**，
compileSdk / targetSdk 为 **37**。尚无 AppWidget provider 或 Glance 依赖；
已有 RemoteViews 用途属于通知。检索时 Glance 最新稳定版为 **1.2.0**，
还有 1.3.0 alpha；若采用，按项目 RC/正式版策略重新核对
[发布记录](https://developer.android.com/jetpack/androidx/releases/glance)，本轮不改依赖。

1. **先确定第一张信息卡的内容与状态。** 候选包括 BA 冷却/办公提醒、
   GitHub 更新摘要、常用操作入口。优先显示少量关键内容、时间戳与明确落地页，
   避免第一版依赖内部滚动、高频联网或持续动画。
2. **建立可持久化的数据快照。** 业务逻辑继续由现有领域持有；
   组件读取最小快照与每实例配置，数据变更后请求更新。
   账号切换、WebDAV 恢复和退出登录应同步处理组件数据与隐私状态。
3. **先验证 RemoteViews 与 Glance 的适配成本。** 原生 RemoteViews 对根 ID、
   厂商布局与进程的控制更直接；Glance 可简化声明式布局，但不复用当前 Compose UI，
   仍需验证上述 OEM 契约。以小型原型决定，不预先迁移现有 UI 架构。
4. **为组件进程缩小启动范围。** 当前
   [KeiOSApp.onCreate](../../app/src/main/java/os/kei/KeiOSApp.kt)
   没有组件进程分支，会配置应用运行环境、图片加载、监听、同步及延迟任务。
   小米专门进程不应直接继承整套启动工作。
   [KeiMmkv](../../core-prefs/src/main/java/os/kei/core/prefs/KeiMmkv.kt)
   的默认入口也未显式选择多进程模式；应选定快照、Provider 或经验证的多进程存储方案，
   不能假定当前单进程缓存天然同步。
5. **按能力组合厂商行为。** 通用加桌与生命周期保留；曝光刷新、厂商详情页、
   版本和审核配置单独适配。vivo 禁止 resize 与通用响应式组件的要求存在实质差异，
   必要时使用独立 provider/布局，同时验收重复发现问题。初始标准组件无需 hidden API。
6. **另定桌面材质。** 保留品牌配色、轮廓、层次和清晰度；
   不能承诺采样桌面实时背景或复用应用内 AGSL/RenderEffect 动效。
   大图模拟玻璃又会消耗图片预算，且可能违反 vivo 背景适配要求。
   可先做适应明暗壁纸的轻量材质，再以宿主实测决定增强效果。

时间类内容优先评估宿主支持的 TextClock / Chronometer，避免每秒唤醒应用。
其余数据展示“截至何时”，刷新失败保留可解释的旧数据/错误状态。
涉及私人账号与提醒内容时，应明确脱敏、未授权、离线、无数据与配置丢失的展示方式。

## 后续验收与未完成事项

| 验收层 | 要覆盖的输入 | 通过标准 |
| --- | --- | --- |
| 标准协议 | API 35 与 37 系统、target 37 APK；标准及第三方桌面 | 添加、配置、重配、多实例、删除、Pin 取消/成功、ID 恢复 |
| 数据与恢复 | 应用进程回收、重启、force-stop 后重新启动；切换账号、数据恢复 | 不串账号、不复活已删除配置、不依赖内存中的旧数据 |
| 更新与内存 | 全量/部分更新、多个尺寸、多个实例、大图、URI 读取 | 无超限拒绝、Binder 错误或宿主持续内存增长；错误布局可用 |
| 外观 | 深浅壁纸、明暗主题、支持的语言、大字体、横屏、平板和折叠 | 无裁切、错误缩放、低对比或点击区域偏移 |
| Xiaomi | 当前系统桌面与组件中心；专门进程 | 启动与内存预算、曝光/普通更新、恢复、版本及大屏编辑行为 |
| vivo | 原子组件库与普通 widget 入口 | 12 小时周期与主动更新、固定规格、无滑动、版本、审核前后发现行为 |
| HONOR | 标准 provider 与类型标记组合；折叠/平板宿主 | 跨厂商可见性、圆角、字号、直接跳转与卡片入口 |
| OPPO | 补齐协议后，区分桌面/负一屏/其他 Seedling Host | SDK 能力判断、准入、生命周期、更新限制与普通 widget 回退 |
| Release | 经 R8 与资源压缩的 APK | Receiver、配置页、厂商元数据、预览资源、深链和图片 URI 均有效 |

最初调研已核对上述官方来源、对应源码与文档入口，当时尚未实现小组件或进行构建、
设备验收与功耗测量。后续标准组件的实现与边界见
[GitHub 项目追踪小组件](github-project-widget.md)，不能以本表推定 OEM 验收通过。
厂商审核与功耗测量仍需独立证据；本项工作没有采集或更新 Baseline Profile。
