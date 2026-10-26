# WebDAV synchronization

This describes the current source implementation. Published releases may differ.

[Main README](../README.md) · [中文说明](#中文说明)

## Operations and recovery

| Operation | Authoritative data | Result |
| --- | --- | --- |
| Sync | Both copies | Preserve local changes when the cloud matches the last verified baseline; otherwise merge supported records and conditionally upload. |
| Upload | Local | After confirmation, replace the cloud file and download it again to verify the exact content. |
| Download | Cloud | After confirmation, validate and replace the local dataset, removing local-only records. |

An empty **dataset** is a valid restore. A missing file, a blank HTTP response, HTML login page,
malformed entry, wrong dataset or unsupported schema is an error, not an empty backup. A missing
file leaves local data intact. Required KeiOS self-tracking remains in GitHub tracking snapshots.
Restored OS card lists do not get repopulated from legacy settings or missing default cards.

For recovery, first export local data if it contains anything you want to keep. Choose Upload
when the local copy is the one to keep; this also repairs an unparseable cloud backup. Choose
Download when the cloud copy is the one to keep. Review the preview before confirming. Changes
to the local dataset, cloud restore snapshot or workspace invalidate an old preview; local edits during a download also
block its replacement. Sync is a merge and does not propagate every deletion.

Each dataset is a separate operation, not a transaction across all six files. A failed local
replacement attempts to restore the previous exported snapshot. A rollback failure is reported
as an error and never establishes a successful checkpoint; this is not a crash-atomic database
transaction.

## Nutstore / Jianguoyun

Use `https://dav.jianguoyun.com/dav/`, your account and an **application password** generated in
Account Information → Security → Third-party Applications. The account login password is not
the application password. The provider documents a 500M upload limit, 600 requests per 30 minutes
for free accounts, 1,500 for paid accounts and a 750-entry listing limit. See the
[official authorization and limits guide](https://help.jianguoyun.com/?p=2064).

KeiOS spaces Nutstore requests by at least one second and reserves a conservative in-process
budget of 120 requests per 30 minutes for its sync clients. A shared per-account cooldown
honors HTTP `Retry-After` on 429/503; repeatedly tapping retry does not clear it. Other applications
using the same cloud account consume the provider's quota independently. Wait for the cooldown,
check the application password for authentication failures, and inspect per-item results rather
than repeatedly alternating Upload and Download.

A rejected application password is paused across sync clients using the same account and
credential, so background batches do not repeatedly submit it. Updating the password permits a
new attempt; explicit Test Connection retries a paused credential after an account-side issue is
resolved. Nutstore documents that repeated wrong or revoked passwords can trigger an account
attack alert: [official explanation](https://help.jianguoyun.com/?p=5609).

Sync addresses the six known files directly. It does not enumerate a directory, so finding a
backup does not depend on a provider's first listing page. This is provider-aware handling based
on documentation and local protocol tests, not a claim of acceptance against an authenticated
Nutstore account.

## Ownership and protocol

`feature-webdav` owns endpoint encoding, HTTP authentication/transport, request pacing,
verified writes and `WebDavSyncSession`. The app owns dataset adapters, domain-store replacement,
MMKV metadata, background scheduling and Miuix confirmation UI.

| File | Dataset |
| --- | --- |
| `github_tracked.json` | GitHub, Git, direct APK and F-Droid tracking |
| `ba_accounts.json` | BA accounts and reminder settings |
| `ba_catalog_favorites.json` | Student catalog favorites |
| `ba_bgm_favorites.json` | BGM favorites |
| `os_activity_cards.json` | Activity shortcut cards |
| `os_shell_cards.json` | Shell command cards |

Existing file names and published export envelopes remain supported. Legacy aliases supported
by the domain parsers are retained where they can be validated. Unknown future schemas and
partially invalid backups require review instead of being silently imported as an empty list.

- Safe GET/PROPFIND requests have bounded transient retries. An ambiguous PUT is read back rather
  than blindly replayed. Successful PUTs also require readback before recording a checkpoint.
- Reconciliation uses a strong ETag (`If-Match`) or create-only PUT (`If-None-Match: *`). A 412
  never becomes an unconditional automatic overwrite. Missing/weak validators require explicit
  user authority. Only confirmed Upload may deliberately replace an existing cloud copy.
- Credentials and redirects stay on the configured scheme, host and port. Remote path segments
  are encoded independently; Unicode, spaces, literal plus and percent signs remain distinct.
- JSON uploads and streamed responses are capped at 32 MiB. Empty, HTML and invalid UTF-8 responses
  cannot become valid backups. Directory creation verifies existing paths as collections.
- A checkpoint persists ETag, normalized content hash and time in one MMKV value. Its hash
  describes the snapshot actually sent or restored, so edits while a request runs stay dirty.
  Changing server, account or directory clears old baselines; rotating a password retains them.
- DAV4jvm remains available for legacy directory browsing; synchronization uses the direct HTTP
  transport. No MOVE/DELETE or remote scratch files are needed for normal sync.

Protocol references: [RFC 4918](https://www.rfc-editor.org/rfc/rfc4918.html) and
[RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html).

## Verification

```bash
./gradlew :feature-webdav:testDebugUnitTest :app:testDebugUnitTest --tests '*WebDav*'
./gradlew :app:assembleDebug
```

Tests cover MockEngine faults, a real disposable local HTTP server, conditional writes, verified
readback, restore/rollback, stale previews, workspace/checkpoint persistence and legacy/empty
snapshot regressions. Local HTTP tests do not verify a provider's production TLS, quota handling
or account-specific behavior. Live provider acceptance should use a disposable directory and
explicitly authorized credentials, with a full upload → local divergence → download restore
cycle for all six datasets.

## 中文说明

同步、上传和下载各自有明确的数据来源：**同步合并两端，上传以本机为准覆盖云端，下载以云端为准替换本机**。
下载会移除本机独有的记录，确认前请检查预览，需要保留的数据先导出。GitHub 追踪仍保留必需的 KeiOS 自身条目。
明确恢复的空 OS 卡片列表不会再从旧配置或默认卡片中自动补回来。

备份中合法的空数组可以恢复；云端文件不存在、空白响应、登录网页、错误数据类型、无效条目和不支持的版本都不会当作空备份清空本机。
本机数据正确时可以确认上传，覆盖损坏的云端文件，并由程序读回校验；云端数据正确时选择下载。同步会合并数据，不能代替覆盖恢复。
预览后本机数据、账号或目录改变，需要重新检查；下载期间发生本机编辑也会阻止覆盖。

坚果云需要在第三方应用管理中生成**应用密码**。配置方法和账号额度见上面的官方文档。
程序为坚果云请求限速，并在收到 429/503 后遵循服务端冷却时间；连续重试不会解除限流。
程序直接访问已知备份文件，不依赖目录列表的第一页。

六类数据分别执行，部分成功时请按条目查看结果。恢复失败会尝试回滚；回滚也失败时明确报错，不会写入成功同步记录。
当前实现和本地协议测试不等同于真实坚果云账号或实体设备验收。
