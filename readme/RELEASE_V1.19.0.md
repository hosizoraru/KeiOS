# KeiOS v1.19.0 Release Notes

<!-- markdownlint-disable MD013 -->

> 本次更新汇总自 v1.18.0 起的变化，重点改善音乐播放、桌面图标与日常操作。
>
> This update covers the changes since v1.18.0, with improvements to music playback, launcher icons, and everyday use.

## 中文

### 音乐播放

- 修复 BGM 封面错配或缺失的问题，让播放器与通知显示对应曲目的图片。
- 从播放通知返回音乐页时，保留当前曲目、播放队列和暂停位置；即使曲目未收藏，也不会跳回收藏列表中的其他歌曲。
- 改善通知栏播放控制、切歌与停止操作，以及后台播放后返回页面时的状态显示。

### 桌面与日常操作

- 修正两种桌面图标的形状、主题显示与测试版角标。离开设置后仍保留所选图标，更新后修复图标消失或重复的问题。
- OS 卡片、GitHub 追踪与 BGM 收藏导入统一为每次选择一个文件，取消后可以重新选择。设置背景图时不再长期保留原图访问权限。
- 应用被系统关闭后，重新打开可继续最近未完成的 OS 活动／Shell 卡片与 GitHub 追踪编辑；主动取消编辑则丢弃草稿。
- AP 与咖啡厅 AP 达到阈值时按通知设置提醒，提醒可以划除，后续 AP 更新不重复打扰。
- 调整窗口大小后，方向行为随手机和大窗口切换。

### 升级

- 支持 Android 15 及以上版本、`arm64-v8a` 设备。
- 从相同来源的正式版覆盖升级即可保留设置、追踪、BA 账户和收藏，无需卸载或清除数据。下载见 [最新稳定版](https://github.com/hosizoraru/KeiOS/releases/latest)。

## English

### Music Playback

- Fix incorrect or missing BGM artwork so the player and playback notification show the image for the current track.
- Returning from a playback notification keeps the current track, queue, and paused position. Songs outside favorites no longer get replaced by a different song from the favorites list.
- Improve notification playback controls, track changes, Stop, and the playback state shown when returning from the background.

### Launcher And Everyday Use

- Correct shapes, themed appearance, and test-build badges for both launcher designs. Your selection is kept when leaving Settings, and updates recover missing or duplicate icons.
- OS card, GitHub tracking, and BGM favorite imports select one file at a time and let you choose again after cancellation. Setting a background no longer keeps long-term access to the original image.
- Recent unfinished OS Activity/Shell card and GitHub tracking edits can be recovered when reopening after the system closes the app. Explicitly cancelling an edit discards its draft.
- AP and Cafe AP thresholds alert according to your notification settings. Reminders can be dismissed, and later AP updates stay silent.
- Resizing the window updates orientation behavior to suit phone and larger windows.

### Upgrading

- Supports Android 15 or later on `arm64-v8a` devices.
- Install over a stable build from the same source to keep settings, tracked projects, BA accounts, and favorites. There is no need to uninstall or clear data. Downloads are available from [Latest Stable Release](https://github.com/hosizoraru/KeiOS/releases/latest).
