# OEM adaptation review: Xiaomi, OPPO, vivo and HONOR

Reviewed on 2026-10-23 against source commit
`444adfc573eb72e0d7a6a600410df379e127c1ba`.

This is a documentation and source review. It does not establish observed behavior
on four OEMs. No runtime changes, dependency upgrades, builds or device operations
were performed for this review. Three read-only agents researched OPPO, vivo and
HONOR with medium reasoning effort; the parent owned source integration and writes.

The subsequent implementation is tracked separately in the
[implementation and verification notes](oem-adaptation-implementation-2026-10-23.md).
The findings below describe the reviewed commit before those changes.

The version catalog currently specifies minimum API 35, compile/target API 37 and
Media3 1.11.1. These are observed project versions, not a dependency update proposal.

## Findings to act on

### 1. Complete state recovery behind the fair-memory receiver

Priority: high. Confidence: confirmed source gap; OEM-triggered recovery untested.

The shared ITGSA receiver is already registered without a Xiaomi brand restriction.
Its KILL branch calls a save callback, but `KeiOSApp.kt:173-179` supplies a no-op.
The comment claiming relevant state is already written through does not cover
the OS editor drafts held by `remember` in `OsPageOverlayState.kt:99-102`, or the
ordinary mutable input fields in `GitHubTrackEditorPageStateHolder.kt:24-60`.
Explicit OS editor save actions commit the values; editing alone does not.

The [fair-memory contract](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2304)
requires state preservation before termination and a Binder reply within three
seconds. Registration alone cannot establish either recovery or deadline compliance.

Recommended work: preserve a narrow editor draft through an appropriate durable
snapshot, restore it without applying unsaved edits, and keep the callback bounded.
Verify draft recovery after process death, cancel/save behavior, cache release and
callback timing under load. Existing smoke evidence in `itgsa-fair-memory.md` does
not establish these journeys. A second vendor-specific receiver is unnecessary.

### 2. Review BA AP alert semantics on the ordinary notification path

Priority: medium. Confidence: confirmed contract mismatch if a threshold alert is
expected to float or alert on the lock screen; no OriginOS reproduction.

[OriginOS notification guidance](https://dev.vivo.com.cn/documentCenter/doc/577),
section 6.1, says ordinary ongoing notifications do not produce floating or
lock-screen alerts and remain user-removable. The guidance is scoped to mainland
OriginOS; it does not establish the behavior of every Android 16/17 promoted or
native-media surface.

`BaApNotificationDispatcher.kt:40-55` sends `running=true` and inherits
`ongoing=running` from `McpNotificationHelper.kt:335-344`. Its active refresh
explicitly sets `ongoing=true`; this is an existing persistent tracking choice,
not evidence that it is merely a one-shot event. The sync coordinator also has
threshold, acknowledgement and dismissal policies.

If the intended contract includes an attention-grabbing threshold reminder,
separate that event from persistent tracking semantics. Changing one caller's flag
is insufficient: `ModernNotificationSpec.kt:72-79` can derive ongoing from running,
and `ModernNotificationBuilder.kt:17,38` uses silent notifications. Preserve genuine
MCP foreground-service and media lifecycles. Test first alert, refresh, user
dismissal, acknowledgement and fallback on both ordinary and promoted paths.

### 3. Validate folding and window changes on supported API 35/36 devices

Priority: medium. Confidence: conditional source risk, not an observed UI defect.

Current window metrics and panes adapt through `LocalWindowInfo`.
`MainActivity.kt:150-155`, however, releases the manifest portrait restriction only
in `onCreate` when `smallestScreenWidthDp >= 600`. The activity handles screen
configuration changes itself and does not reevaluate that policy in an
`onConfigurationChanged` override.

An outer-screen launch followed by unfolding therefore deserves testing where
API 35/36 or an OEM compatibility mode still honors the original restriction.
Do not report it as an Android 17 portrait-lock bug: qualifying API 37 large-screen
windows ignore those restrictions. Also check editor/Sheet continuity, split and
floating windows, IME and secondary activities.

References: [Xiaomi large-screen technical guidance](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2043),
[HONOR multi-window interaction](https://developer.honor.com/cn/docs/11101/guides/05.multi-window_interaction),
[Android 17 large-screen behavior](https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored).

## Existing integrations worth retaining

| Area | Current source coverage | Remaining evidence |
| --- | --- | --- |
| Media and artwork | `MediaSessionService`, default Media3 notification provider, metadata artwork and bitmap loading, queue-specific artwork updates, semantic controls, audio-focus/noisy handling | ColorOS, OriginOS and MagicOS notification-center/lock-screen controls, cover replacement, rapid track switching, pause/resume and focus loss |
| Document selection | AndroidX OpenDocument plus `CATEGORY_OPENABLE`, MIME filtering and stream-readable URI access; CreateDocument exports | Each OEM's actual provider UI, cancel/reselect, streamed reads, export and applicable retained-access behavior |
| Fair memory | Shared ITGSA actions, cache release and Binder callback; no brand gate | Unsaved drafts and bounded recovery, as described above |
| Standard live notifications | Android 16+ ProgressStyle/promotion route with manifest permission; Xiaomi protocol and fallback have their own policy | User promotion settings, eligible scenarios, updates, completion and dismissal on each ROM |
| Background work | JobService completion/stop handling, scheduler and separate keepalive ownership | OEM battery restrictions and timing; keepalive does not establish that jobs will execute |
| Window adaptation | Current-window metrics and responsive panes | Fold transitions, split/floating windows and process/configuration continuity |

The media sources are `BaGuideBgmMediaSessionService.kt`,
`BaGuideBgmMediaNotificationProviderFactory.kt`, `BaGuideBgmMediaItemMapper.kt`,
`BaGuideBgmNativeMediaController.kt`, `BaGuideBgmMediaButtonPreferences.kt` and
`GuideMediaPlayerAudioSupport.kt`. Xiaomi-specific drag sharing remains isolated
in `BaGuideBgmMediaOemCompat.kt`.

The reviewed documents do not establish a requirement for a proprietary OPPO,
vivo or HONOR media SDK, private artwork keys or a replacement for Media3.
This is bounded research, not proof that no additional partner documentation exists.
Standard MediaSession support also does not guarantee admission to a branded island.

The [Xiaomi picker guidance](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2220)
describes the standard picker path across the four OEMs. The
[vivo document-permission guidance](https://dev.vivo.com.cn/documentCenter/doc/957)
also favors picker-based access. No mandatory replacement picker SDK was identified.
Text-only HTTPS sharing is not a URI attachment; missing URI grant flags on that
path are not an attachment-sharing defect.

## Optional OEM capabilities and admission boundaries

| OEM | Capability | Assessment for KeiOS |
| --- | --- | --- |
| Xiaomi | Super Island / Focus notifications | Existing integration; separately verify production AppID, signing identity and scenario admission in the developer portal. Source cannot prove that administrative state. Local notification integration does not itself require MiPush cloud service. |
| OPPO | Fluid Cloud | No native Seedling adapter found in the inspected notification sources. The card-field reference places specific controls behind whitelist/platform coordination. Establish a supported business scenario before implementing an adapter. Standard ProgressStyle does not guarantee Fluid Cloud placement. |
| vivo | Atomic notifications / island | No native adapter found. The guide describes scenario/application review and permission/rollout requirements. Match a legitimate finite journey and fallback before adding it. Keepalive alone is not such a journey. |
| HONOR | YOYO Suggestions global reach | No adapter found. It is a service/card integration with agreed scenarios, platform configuration and review, rather than a mandatory replacement for standard music controls. BA/GitHub/MCP eligibility is unconfirmed. |

For vivo atomic notifications, account for deduplication with ordinary
notifications, user deletion, fallback, update frequency and expiry. The reviewed
rules specify at least ten seconds between updates, expiry after two hours
without an update and an eight-hour overall limit. These belong to the atomic
notification contract, not to MediaSession progress updates.

Xiaomi tap sharing and OPPO proactive split/floating launches are also optional
product features. Their lifecycle, device and platform restrictions do not justify
changing ordinary chooser, browser or APK-install behavior.

## Official source register

`Live` means article content was read through the web tool or rendered browser
by the parent or assigned agent. `Index` means only excerpts from the official
search index were available; conclusions from those sources remain provisional.
Dates below are displayed article update dates where available, not crawler dates.

| OEM | Document | Evidence | Displayed update |
| --- | --- | --- | --- |
| Xiaomi | [Super Island development](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2131) | Live | 2026-01-29 |
| Xiaomi | [Integration and admission](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2132) | Live | 2025-12-05 |
| Xiaomi | [Media integration](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2161) | Live | 2025-11-20 |
| Xiaomi | [Document picker](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2220) | Live | 2025-12-26 |
| Xiaomi | [Fair running memory](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2304) | Live | 2026-04-28 |
| Xiaomi | [Large-screen technical guidance](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2043) | Live | 2025-12-04 |
| Xiaomi | [Android 17 adaptation](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2297) | Live; retains beta-era wording | 2026-05-06 |
| Xiaomi | [Tap sharing](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2135) | Live | 2025-10-23 |
| OPPO | [Fluid Cloud card settings](https://open.oppomobile.com/new/developmentDoc/info?id=13330) | Index; live navigation timed out | Not established |
| OPPO | [Android 16 Beta 3](https://open.oppomobile.com/new/developmentDoc/info?id=13339) | Index; preview scope | Not established |
| OPPO | [Android 16 compatibility](https://open.oppomobile.com/new/developmentDoc/info?id=13346) | Index; preview-era qualifications | Not established |
| OPPO | [Proactive split/floating launch](https://open.oppomobile.com/documentation/page/info?id=11171) | Index | Not established |
| vivo | [OriginOS notification styles](https://dev.vivo.com.cn/documentCenter/doc/577) | Live | 2025-10-20 |
| vivo | [Atomic notification integration](https://dev.vivo.com.cn/documentCenter/doc/894) | Live | 2025-09-01 |
| vivo | [Atomic notification rules](https://dev.vivo.com.cn/documentCenter/doc/895) | Live | 2025-09-03 |
| vivo | [Atomic notification technical specification](https://dev.vivo.com.cn/documentCenter/doc/896) | Live | 2025-09-03 |
| vivo | [Document permission standard](https://dev.vivo.com.cn/documentCenter/doc/957) | Live | 2025-12-25 |
| HONOR | [Android 17 compatibility](https://developer.honor.com/cn/docs/adaptation_guide/guides/android_17_compatibility_adaptation_guide) | Live, rendered by parent | Not displayed |
| HONOR | [YOYO global reach introduction](https://developer.honor.com/cn/doc/guides/101452) | Index | Not established |
| HONOR | [YOYO configuration](https://developer.honor.com/cn/doc/guides/101455) | Index | Not established |
| HONOR | [YOYO design](https://developer.honor.com/cn/doc/guides/101538) | Index | Not established |
| HONOR | [All-files access standard](https://developer.honor.com/cn/doc/guides/101678) | Index | Not established |
| HONOR | [Multi-window interaction](https://developer.honor.com/cn/docs/11101/guides/05.multi-window_interaction) | Index | Not established |

OEM Android-version guides are cross-checking aids. Use matching Android SDK/AOSP
sources and current Android documentation for platform behavior. In particular,
do not turn Android 18 implicit-URI-grant or cleartext-attribute migration plans
into unconditional Android 17 failures, or use an Android 16 preview page as final
Android 17 evidence.

## Verification boundaries and next work

Only the report is changed. Verified the relevant source statements, 16 referenced
source/document basenames, syntax of 24 official URLs and whitespace.
URL syntax does not establish article-body availability; the register records
that separately. No app build was needed for this documentation-only change.
Existing Xiaomi media receipts are separate historical evidence. This review
does not refresh them or claim four-OEM visual acceptance.

Suggested order:

1. Implement and test durable unsaved-draft recovery behind fair-memory handling.
2. Resolve threshold-alert versus persistent-tracking behavior across notification
   builders, then validate ordinary OriginOS delivery and dismissal.
3. Run a bounded device matrix for media/artwork, SAF, fold/window transitions and
   background execution. Record ROM/API, package/signature, permissions and
   notification settings separately from source/build results.
4. Add a native OEM card adapter only after confirming its scenario and admission.
