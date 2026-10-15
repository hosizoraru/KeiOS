# Maintenance decisions

Reviewed 2026-10-16 against source baseline `1dd382e99`. This is a bounded list of reviewed
candidates, not a repository-wide inventory. A low reference count locates a candidate; it does
not establish that a migration, platform fallback, research API, or registered entrypoint is dead.
Each unresolved item needs the evidence in its exit condition before removal.

## Addressed in this follow-up

| Item / owner | Evidence and change | Verification |
| --- | --- | --- |
| MCP development guide — `feature-mcp/.../server/McpDevTools.kt` | Module extraction `057c64282` moved runtime sources, assets, and most tests. Five source/asset paths and two test paths still pointed into `app`; the suggested test command covered only app registration tests. Correct the paths and share one command covering both test modules between snapshot and validation plan. | A regression checks the generated guide against actual files and their owning test modules. It fails on the old guide at the missing `LocalMcpService.kt` path. Run the emitted test selections and app compilation. |
| Ignored notification layout parameter — `core-notification/.../live/builder/ModernNotificationSpec.kt` | `preferOemLiveIconLayout` was suppressed as unused and never read. Remove its environment/property plumbing and the ColorOS-family probe whose result only reached this parameter. Keep HyperOS capability detection, icon selection, render policy, channels, and all actual builders. | Existing notification spec, semantic-icon, routing, island, and app share-notification tests; compile app consumers in Debug and Release. Existing test assertions remain. |
| Retired share-import total switch — `feature-github/.../model/GitHubLookupModels.kt` and `.../data/local/GitHubTrackStore.kt` | `6bb30bf0d` made share import always enabled. Loading already ignored the old key; saving still wrote `true`, and configuration/home state carried a mutable flag. Remove the write/key and redundant configuration/home fields. Keep a read-only `true` accessor for existing MCP diagnostic fields. | Existing lookup-model, store, share-flow, home-content, and MCP registration tests plus consumer compilation. Remove only the duplicate launch-policy matrix row that supplied an impossible disabled configuration; retain the test and both real flow modes. |

The old share-import preference value is left untouched in user storage and remains ignored.
`shareImportLinkageEnabled` and `github.shareImportLinkageEnabled` keep their MCP names, boolean
values, and meaning. Flow mode, app-managed installation, and download preferences remain active.
Removing the diagnostic compatibility accessor later requires a separately reviewed MCP contract
change; it is not another configurable switch.

## Retained or awaiting evidence

| Item / owner | Why it stays | Exit condition / next verification |
| --- | --- | --- |
| OS legacy stores — `app/.../os/store/OsUiStateStore.kt`, `OsInfoCacheStore.kt`, `OsCardVisibilityStore.kt` | Current reads still fall back to previous MMKV stores. Source age does not prove that an upgrading installation has migrated. | Establish a supported upgrade boundary or migrate every supported old format before dropping fallback reads. Cover absent/new/legacy/conflicting keys and preserved user selections. |
| BA account migration — `app/.../ba/support/BaAccountMigration.kt` | Initialization still imports legacy shared or per-server accounts when the new account store is empty. | A documented upgrade policy and migration fixtures must show that removing this path preserves all supported accounts, identities, and runtime/reminder data. Keep `BaAccountMigrationTest`. |
| Legacy notification builder — `core-notification/.../live/builder/LegacyNotificationBuilder.kt` | Minimum API is 35; modern Live Updates eligibility starts at Android 16. The legacy builder remains an executed branch. | Keep while API 35 is supported or another validated implementation owns that branch. Verify channel, action, progress, and dismissal behavior before replacement. |
| Compatible viewer — `app/.../student/rendering/GuideWebPresentation.kt` | A current preference selects direct WebView drawing or decoded frame presentation. These are different live rendering paths. | Retire only after an explicit compatibility decision supported by viewer/device evidence, including unsupported WebMessage capability and readiness/fallback behavior. Do not infer redundancy from similar output. |
| MiFocus template catalog — `core-notification/.../focus/MiFocusOfficialTemplateCatalog.kt` | The prior test-suite review records an intentionally retained research/shared API. | Revisit ownership and external research use explicitly before removing it or its tests; a lack of app callers is insufficient. |
| Sheet frame spikes — `ui-liquid-glass/.../sheet/` | Cloud tests cover composition, layout, and gestures; no current physical-device frame capture identifies the remaining expensive stage. | Follow the [device acceptance handoff](../performance/compose-recomposition-2026-10-13.md#macos--hyperos-acceptance-handoff). Preserve up/down resizing, materials, and effects; use comparable phase captures to select the next change. |
| Baseline Profile freshness — `app/src/release/generated/baselineProfiles/` | Runtime signatures changed after the packaged profile capture. Existing profile assets alone do not prove freshness. | Before shipping a release, run the freshness gate, recollect on the final source using a selected disposable collector, and verify packaged assets. Device work remains with the user's later test session. |

## Evidence boundaries

Deletion and simplified state are maintenance outcomes. They do not establish smaller APKs,
lower frame times, or better model-task quality. The earlier quick-gate timing comparison and
its unchanged 41-case coverage are recorded in the [performance follow-up](../performance/compose-recomposition-2026-10-13.md#p1p2-maintenance-follow-up-2026-10-16).
Detailed local logs and XML for this follow-up are under ignored `.planning/maintenance-continuation/`.

Final cloud verification passed 131 tests across `core-notification` (28), `feature-github` (23),
`feature-mcp` (34), and `app` (46), with zero failures, errors, or skips. App Release Kotlin
compilation passed as well. These checks include both modules' MCP server test selections from
the repaired guide; they do not install an APK or establish system notification presentation.
