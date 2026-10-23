# OEM adaptation implementation

Date: 2026-10-23. Source base: `444adfc573eb72e0d7a6a600410df379e127c1ba`;
the implementation and verification below describe changes validated on a working
tree based on this commit, before the responsibility-based commits.
This follows [the OEM documentation review](oem-adaptation-review-2026-10-23.md).

## Scope

Implemented the shared recovery, ordinary-notification and window-policy changes,
and documented client-side protocol implementations for OPPO, vivo and HONOR.
Admission applications, production credentials and vendor-console operations are
outside this task. No dependency versions were changed.

Native protocol support is an explicit integration API, not a claim that every
current BA/MCP/GitHub notification is automatically eligible for an OEM card.
Business scenes, milestones, stable journey IDs and landing actions remain caller
inputs. No sample transport, navigation or order scene is assigned to unrelated
KeiOS features. Existing notification channels and the standard fallback remain.

## Shared changes

### Editor recovery and fair memory

`EditorRecoveryStore` keeps small prepared snapshots in a dedicated app-private
MMKV store. The ITGSA KILL callback now retries pending writes and synchronizes
them without reading Compose state. One failed slot does not prevent another
slot's write or the final synchronization attempt.

OS Activity/shell editor input is owned by the page ViewModel; GitHub input
retains its existing state holder. Opening or replacing an editor stages its
identity and fields together. Restoring input does not save a card, add a
subscription, execute a command or resume a running task.

Restored editors wait for the authoritative card/track list, and recovered edit
targets are reconciled before use. Repository/submit guards also reject writes
against an unloaded list. Explicit cancellation clears the recovery record.
GitHub submission stages data, awaits persistence, and then clears only the
unchanged editor revision. Failed or cancelled saves keep input recoverable.

Recovery records expire after seven days and are bounded to 256 Ki characters.
Oversized records are discarded as a whole rather than restoring truncated
commands, intents or URLs. Shell output, installed-app lists, bitmaps and running
operations are not snapshots. Existing backup exclusions remain in force.

References: [ITGSA fair-memory contract](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2304).
This implementation does not establish actual OEM KILL timing or physical recovery.

### BA reminders and ordinary notification behavior

AP and Cafe AP threshold events explicitly use non-ongoing delivery. Later AP
refreshes are silent. Modern rendering respects the caller's explicit ongoing
contract and `onlyAlertOnce`; custom delete actions remain attached even without
a secondary button. Actual MCP/transfer ongoing lifecycles remain explicit.

Xiaomi focus JSON, artwork/progress and account-specific actions are retained.
Channel settings and user preferences still govern alert presentation; the
change does not force an alert through a disabled or quiet channel.

Reference: [OriginOS notification guidance, section 6.1](https://dev.vivo.com.cn/documentCenter/doc/577).
Ordinary notification rules are not assumed to define all promoted/media surfaces.

### Window transitions

`MainWindowOrientationPolicy` is evaluated on creation and configuration changes.
Small windows retain the phone portrait preference; large windows request full
user orientation. Unchanged requests are skipped. This does not opt out of
Android 17 adaptive-window rules.

Reference: [Android 17 orientation and resizing](https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored).
Policy tests model fold/unfold and resize sequences; they are not physical
foldable acceptance.

## Native protocol APIs

### OPPO

The SDK-free `IntelligentIntent` Provider client, progress-card mapper and
per-journey adapter implement CREATE/UPDATE/END with stable entity identity,
validated payloads, failure results and retryable END cleanup. Provider clients
are released. Remote image references remain owned by the caller until cleanup.
This does not introduce Seedling SDK binaries.

The standard notification owner must retain ordinary delivery and provide
configured intent/service/entity/milestone values. Provider acknowledgment is
separate from visible card recognition.

Primary sources, full bodies retrieved through the official public document API:
[client transport](https://open.oppomobile.com/documentation/page/info?id=13558),
[data structure](https://open.oppomobile.com/documentation/page/info?id=13565),
[progress template](https://open.oppomobile.com/documentation/page/info?id=13567),
[integration preparation](https://open.oppomobile.com/documentation/page/info?id=13572).

### vivo

The local template-4 mapper includes ordinary-card, capsule and island data.
Capability probing falls back when unavailable. The planner covers update
frequency, expiry, foreground landing-page presentation, termination and durable
removal tombstones. The dispatcher sends one notification identity, strips OEM
extras on ordinary fallback and never turns a failed END into a new alert.

`VivoAtomicNotificationRuntime` accepts an existing framework-built notification
and an explicit business binding. Its app-owned MMKV journal and private delete
receiver preserve removal across process recreation and relay the original
delete action. The owner receives a retry deadline with no stale queued content;
it must retry with the latest state. MediaSession notifications are rejected.

No global notification-switch query is used as proof of atomic permission.
Unconfigured bindings use ordinary delivery. Native success remains unverified.

Primary sources: [integration](https://dev.vivo.com.cn/documentCenter/doc/894),
[rules](https://dev.vivo.com.cn/documentCenter/doc/895),
[local technical specification](https://dev.vivo.com.cn/documentCenter/doc/896).
VPush/server JSON is not used as local notification extras.

### HONOR

The SDK-free order-feedback DTO and Provider client implement normal/progress
presentation, typed fields, update/dismiss statuses, minimum submission interval
and configuration/permission/transport failure results. Omitted terminal
`endTime` means immediate dismissal. Background update/end attempts are not
blocked by an invented Activity-visibility requirement. Deferred configuration
checks retain only Application Context so the client can have application scope.

The documented access permission and targeted package/provider visibility are
declared. `keios.oem.honor.appId` configures the real application ID. Without it,
the enable flag is false and the context-based client refuses transmission.
Using a string resource for the ID is KeiOS's type-preserving choice; it is not
claimed as an OEM-mandated runtime value type.

The owner still supplies intent/business identifiers, presentation and journey
lifecycle. No cloud global-reach credentials or SDK binaries were added.
Provider submission does not establish login, authorization or visible display.

Primary source: [global-reach development guide](https://developer.honor.com/cn/doc/guides/101454),
read in full. Its statement that the user must open the app does not define
current Activity visibility as a hard requirement for every update/dismiss call.

## Verification

Focused final-input checks: 197 tests passed, zero failures/errors/skips:

- `core-notification`: 110, including ordinary alert behavior and three OEM protocols.
- `app`: 81, including draft recovery, load/commit ordering, failure/cancellation,
  stale completion, atomic target replacement and orientation policy.
- `feature-mcp`: six action/account routing regressions.

The app and affected consumers compiled. `lintVitalRelease` and `assembleRelease`
passed with R8 enabled. The APK is non-debuggable, arm64-v8a, minimum API 35 and
target API 37, and retains the existing release signer. APK signature verification
and 16 KiB ZIP alignment passed. Embedded `baseline.prof` and `baseline.profm`
match their own compiled intermediates; this does not establish profile freshness.
Packaged defaults have an empty HONOR application ID and disabled enable flag,
and the vivo delete receiver is unexported. The artifact is a working-tree
validation build, not a new tagged release.
No devices, AVDs, installed applications or app data were changed.

The freshness script reports existing Baseline Profiles stale against earlier
media/picker changes; this OEM implementation adds further runtime inputs.
This is not a release-readiness or performance acceptance claim. Recollection
remains a separate shipping gate; the shared HyperOS AVD was not used.
