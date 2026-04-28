# KeiOS Todo List

This file tracks roadmap items that need a stable device, OEM beta, or focused validation window.

## Priority Legend

- P0: API 36 primary-device paths and current `targetSdk=37` paths that directly affect existing user flows.
- P1: API 37 enforced behavior, OEM beta, and long-run stability validation.
- P2: optional new APIs, low-frequency surfaces, and deferred roadmap items.

## API Baseline Audit

Official references checked on 2026-04-29:
[Android 16 target SDK behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16),
[Android 16 all-app behavior changes](https://developer.android.com/about/versions/16/behavior-changes-all),
[Android 16 new features and APIs](https://developer.android.com/about/versions/16/features),
[Android 17 target SDK behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17),
[Android 17 all-app behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all),
[Android 17 new features and APIs](https://developer.android.com/about/versions/17/features),
[local network permission](https://developer.android.com/privacy-and-security/local-network-permission),
[16 KB page-size support](https://developer.android.com/guide/practices/page-sizes).

- [x] Build baseline is `minSdk=35`, `compileSdk=37`, `targetSdk=37`, with Java / Kotlin toolchains on 21.
- [x] Manifest declares `NEARBY_WIFI_DEVICES(maxSdk=36)`, `ACCESS_LOCAL_NETWORK`, `USE_LOOPBACK_INTERFACE`, `POST_PROMOTED_NOTIFICATIONS`, and `FOREGROUND_SERVICE_SPECIAL_USE`.
- [x] Main Activity enables `enableEdgeToEdge()`, launcher icons provide monochrome assets, main navigation plus guide fullscreen image flow use predictive-back paths, and `OnBackInvokedCallback` is explicitly enabled.
- [x] Source search currently finds zero Contacts, Bluetooth, Health Connect / sensor permission, `READ_MEDIA*`, custom RemoteViews, `MediaStore#getVersion()`, `scheduleAtFixedRate`, WorkManager, direct JobScheduler, `announceForAccessibility` / `TYPE_ANNOUNCEMENT`, project JNI, or `System.load*` adaptation surfaces.
- [x] Background-image import and BA media save / ZIP export currently use SAF, FileProvider, explicit URI grants, and DocumentFile, with zero broad media-permission surface.
- [x] Add a shared API-version helper for `SDK_INT_FULL`, API 36, API 36.1, and API 37 gates.
- [x] Add Runtime API Full and Advanced Protection Mode status to About build details.
- [x] Identify the only password-style input surface as the GitHub API token sheet; it already uses Compose password masking plus an explicit reveal control and needs Android 17 physical-keyboard validation.

## P0 - API 36 Primary-Device Validation

### P0-A Display, Input, And Back

- [x] Run API 36 normal-window edge-to-edge, status bar, and navigation bar smoke across Home, OS, BA, MCP, GitHub, Settings, About, GitHub share import window, uCrop, OS Shell Runner, and video fullscreen; artifacts: `artifacts/api36-p0/smoke4/`.
- [x] Complete API 36 IME and focused-input inset smoke across OS Shell Runner, GitHub token/share import surfaces, Settings permission flows, and BA guide filters; artifacts: `artifacts/api36-p0/p0a-display-input-accessibility-run2/`. The run captured screenshots, UI XML, and `dumpsys input_method` for OS Shell Runner, GitHub API Token, and BA catalog filtering on Xiaomi `25098PN5AC`.
- [x] Complete API 36 typography smoke with Chinese, Japanese, Latin, large-font, bold-text, and outline-text settings across Home, GitHub, BA guide/catalog, Settings, About, OS Shell, and GitHub share import; artifacts: `artifacts/api36-p0/p0a-display-input-accessibility-run2/`. The run used `font_scale=1.35`, high-contrast text, `font_weight_adjustment=200`, and restored device text settings afterward.
- [x] Complete predictive-back validation across Nav3 main pages, Settings, student guide detail, guide fullscreen image, OS Shell Runner, and GitHub share import Activity with gesture and key back; artifacts: `artifacts/api36-p0/p0a-display-input-accessibility-run2/`. `CoreBackPreview` evidence was captured, and KeiOS process fatal count in the filtered evidence is 0. One `uiautomator` dump-process registration crash remains recorded as QA-tool noise for follow-up filtering.
- [x] Add predictive-back OEM compatibility for HyperOS / MIUI / Xiaomi, ColorOS, OriginOS, MagicOS, EMUI, and One UI so main-navigation return transitions follow the active swipe edge.
- [x] Complete Android 17 / API 37 AVD predictive-back smoke: Home -> Settings -> left-edge back, Home -> About -> right-edge back, with logcat confirming the `CoreBackPreview` callback path.

### P0-B Runtime, Packaging, And Native Dependencies

- [x] Complete API 36 16 KB page-size validation on packaged debug APK using the `KeiOS_API36_16K` AVD; artifacts: `artifacts/api36-p0/p0b-runtime-native-api36-16k-run2/`. The emulator reports `PAGE_SIZE=16384`, `zipalign -P 16` exits 0, and transitive native libraries `libandroidx.graphics.path.so` plus `libmmkv.so` both report minimum LOAD alignment `16384`, so the current decision is to keep `android:pageSizeCompat` unset.
- [x] Complete API 36 ART / native-dependency smoke after fresh install and upgrade: app startup, MMKV read/write, cache clear, Coil GIF decode, Media3 BGM/video, Shizuku init, focus-api notification build, and backdrop/liquid-glass rendering; artifacts: `artifacts/api36-p0/p0b-runtime-native-api36-16k-run2/`. Fresh and upgrade installs both return `Success`, runtime probes all return `PASS`, and the app runtime blocker count for `UnsatisfiedLinkError`, app fatal exception, and app native `dlopen failed` is 0. Residual hidden-API denied logs for `AccessibilityNodeInfo.getSelection/setSelection` remain tracked under the next hidden-API audit.
- [x] Complete the first `setAccessible` / hidden-API reduction pass: HyperOS settings and keep-alive property reads now use shared PropUtils, AppOpsManagerInjector uses public-method probing, and the Shizuku private `newProcess` compatibility path remains guarded by runtime status.

### P0-C Local Network And MCP Service

- [x] Add constrained `NEARBY_WIFI_DEVICES(maxSdk=36)` support and route MCP LAN permission requests through shared API 36 / API 37 permission logic.
- [x] Validate MCP loopback on an Android 17 / API 37 AVD with `RESTRICT_LOCAL_NETWORK` enabled: `127.0.0.1:38888/mcp` remains reachable and returns the expected auth rejection, with no Network Security Config needed.
- [x] Validate MCP LAN mode on Android 16 / API 36 hardware with `RESTRICT_LOCAL_NETWORK` enabled: same-subnet `http://192.168.31.209:38888/mcp` and device loopback both return the expected `401 Unauthorized`.
- [x] Validate MCP `specialUse` foreground service behavior on Android 17 / API 37 AVD: foreground start is allowed, denied notification permission keeps the service running without crashes, and restored permission allows normal stop.
- [x] Validate MCP `specialUse` foreground start on Android 16 / API 36 hardware: `McpKeepAliveService` runs foreground with type `0x40000000`, service notification `38887`, and live update notification `38888`.
- [x] Validate MCP `specialUse` foreground service recovery through the MCP shortcut path on Android 16 when background start limits, battery saver, and inactive Shizuku states are present; artifacts: `artifacts/api36-p0/mcp-recovery-20260429-6/`. `os.kei.benchmark` started with `low_power=1`, standby bucket `45`, and Shizuku permission `not_granted`; after 22s in the background it kept foreground service type `0x40000000`, service notification `38887`, Live Update notification `38888`, and loopback `401 Unauthorized`, with no `ForegroundServiceStartNotAllowedException` or fatal exception recorded.

### P0-D Intents, URI Grants, And File Flows

- [x] Validate API 36 SAF / URI-grant flows for non-Home background `OpenDocument -> uCrop`, BA media custom save location, BA ZIP export, and log archive export; artifacts: `artifacts/api36-p0/p0d-intent-uri-20260429-1/`. `os.kei.benchmark` has no `READ_MEDIA*`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE` permissions and keeps FileProvider registered; image `OpenDocument`, `OpenDocumentTree`, and log `CreateDocument(application/zip)` all open the system picker; the non-Home background path keeps `takePersistableUriPermission` plus `UriGrantCompat.grantToIntentTargets` before uCrop, and BA single-media / ZIP export keeps `CreateDocument` / `ACTION_OPEN_DOCUMENT_TREE`, persistable read/write tree grants, and `ContentResolver` output streams.
- [x] Harden high-traffic Safer Intents paths across GitHub `ACTION_SEND` import Activity, downloader selection, external links, and share-APK-link flows with stricter action, MIME, scheme, host, and package boundaries.
- [x] Run API 36 GitHub share-import strict smoke with a direct release APK URL; the window resolves `topjohnwu/Magisk` `v27.0`, lists `Magisk-v27.0.apk`, and keeps the install confirmation sheet visible.
- [x] Run API 36 Safer Intents strict-matching smoke for OS user shortcut cards, OEM settings helpers, external browser/download/share intents, and GitHub share import; artifacts: `artifacts/api36-p0/p0d-intent-uri-20260429-1/`. GitHub `ACTION_SEND text/plain` resolves to `GitHubShareImportActivity`, while `image/png` returns `No activity found`; external web, direct APK, and text-share intents resolve to the system resolver; the explicit Google settings shortcut sample and Settings launcher fallback resolve successfully; OEM app details, battery-optimization allowlist, and HyperOS permission editor helpers resolve successfully, so no extra action, filter, scheme, host, or package constraints were needed.
- [x] Complete the first lower-frequency OS shortcut and notification / shortcut extras audit: MainActivity external extras are paired by target-page and action allowlists, OS Activity cards now verify a resolvable target before launch, and the top-of-stack MCP shortcut is verified through `singleTop` start/stop delivery.
- [x] Complete Android 17 / API 37 AVD external-extras smoke: valid GitHub shortcut action opens GitHub, mismatched actions open only the target page, and unknown targets fall back to Home.

### P0-E Background Work And Notifications

- [x] Validate DownloadManager, GitHub background refresh, and BA AP / cafe / arena reminders under Android 16 idle, lockscreen, and battery-saver states; artifacts: `artifacts/api36-p0/p0e-background-notifications-20260429-1/`. On API 36 Xiaomi hardware, `os.kei.debug` retained GitHub / BA one-shot `AlarmManager` tick records under `low_power=1` and standby bucket `45`, `jobscheduler` showed no KeiOS jobs, and `dumpsys download` showed no pending KeiOS downloads. The source audit keeps the no `WorkManager` / `JobScheduler` / fixed-rate work-path finding, and validation restored `low_power=0` plus bucket `10`.
- [x] Validate MCP Live Update / promoted notification on Android 17 / API 37 AVD: records include `PROMOTED_ONGOING`, `ProgressStyle` actions, and notification PendingIntent allowlists while preserving the existing notification framework.
- [x] Validate API 36 MCP, GitHub, and BA promoted notification record construction without changing the notification framework: records include `PROMOTED_ONGOING`, `NotificationCompat.ProgressStyle`, action PendingIntent allowlists, and expected notification ids `38888`, `38990`, `38889`, `38890`, `38891`.
- [x] Validate GitHub, BA, and Super Island notification visual surfaces and tap behavior on Android 16 promoted notifications and `NotificationCompat.ProgressStyle`; artifacts: `artifacts/api36-p0/p0e-background-notifications-20260429-1/`. Notification shade and lockscreen screenshots / XML cover GitHub, BA AP, cafe, and arena; records include `PROMOTED_ONGOING`, `NotificationCompat.ProgressStyle`, and PendingIntent allowlists. GitHub `Open` enters the GitHub page, BA `Open` enters the BlueArchive page, and the Super Island-compatible path keeps the existing helper/source contract and notification framework.

### P0-F QA Tooling And Evidence

- [x] Add `scripts/qa/android_api_compat_probe.sh` for API 36 / 37 device version, manifest permission, AppOps, and optional `RESTRICT_LOCAL_NETWORK` compat-flag checks.
- [x] Add debug-only API compatibility QA entry points and `scripts/qa/android_api36_p0_smoke.sh` for API 36 screen, URI-grant, share-import, shell, fullscreen, uCrop, and notification evidence capture.
- [x] Add dedicated API 36 P0-A / P0-B QA scripts: `scripts/qa/android_api36_p0a_display_input_accessibility.sh` for display, input, back, and accessibility evidence, plus `scripts/qa/android_api36_p0b_runtime_native_smoke.sh` for 16 KB native library and runtime dependency evidence.
- [x] Extend `scripts/qa/android_api_compat_probe.sh` to print page size, display/window sizing, enabled compat flags, and alarm/job/download state relevant to this checklist; API 36 hardware reports 4096-byte pages and `1220x2656 @ 520dpi`.

## P1 - API 37 Enforced Adaptation

### P1-A Local Network, HTTP, And Transport Security

- [x] Add Android 17 local network permissions for MCP LAN mode.
- [x] Request local network permission before starting MCP in LAN mode.
- [x] Normalize `http://` GitHub share links, GitHub downloader URLs, and GameKee / BA guide asset links to `https://`; restrict DownloadManager to HTTPS external download URLs.
- [x] Audit MCP loopback HTTP endpoints: API 37 AVD with the local-network compat flag enabled can reach the legitimate port, and no Network Security Config block was observed.
- [x] Continue auditing MCP LAN HTTP endpoints; artifacts: `artifacts/api37-p1/p1a-network-security-20260429-1/`. The API 37 AVD local-only MCP loopback listened on `127.0.0.1:38888` and returned `401 Unauthorized` for unauthenticated `/mcp`; API 36 hardware LAN mode listened on `[::]:38888`, and both in-device plus same-subnet host requests to `http://192.168.31.209:38888/mcp` returned `401 Unauthorized`. The implementation scope stays on manifest permissions, runtime permission, and bearer-token rejection, with Network Security Config left empty; OEM Beta same-subnet coverage moves with the P1-F matrix.
- [x] Evaluate HTTPS hardening readiness for OkHttp traffic, including Android 17 CT default behavior, Certificate Transparency opt-in gaps, and ECH compatibility, after checking GitHub, GitHub download redirects, GameKee, BA media CDN, and MCP loopback behavior; artifacts: `artifacts/api37-p1/p1a-network-security-20260429-1/`. GitHub API, GitHub release redirects, GameKee pages, and the BA media CDN probe passed TLS verification, and GitHub / GameKee certificate chains include CT SCTs. OkHttp 5.3.2 source probes cover `NetworkSecurityPolicy` / Conscrypt / `ConnectionSpec`, with the current app surface exposing no ECH-specific hook. The implementation scope stays on Android 17 default CT plus existing HTTPS normalization, while certificate pinning, CT exception config, and forced `domainEncryption` wait for mature OkHttp / platform ECH integration.

### P1-B Background Scheduling, Resource Profiling, And Exit Signals

- [x] Register Android 17 anomaly profiling trigger for resource anomalies.
- [x] Inspect previous process exits for MemoryLimiter or excessive resource usage signals.
- [x] Add fair background scheduling pass for GitHub and BA tick jobs.
- [ ] Run Android 17 AVD profiling for alarm windows, wakeups, and previous-exit logs after long idle; compare GitHub and BA tick behavior with the fair scheduling policy.

### P1-C Activity Launch, URI Grants, And IntentSender

- [x] Add explicit URI grant support for the non-Home background FileProvider / uCrop crop flow.
- [ ] Continue validating custom media-save and ZIP export URI grant flows on Android 17; add explicit package grants where chooser or third-party targets drop access.
- [x] Add user-visible notification action background-activity-start allowance for MCP and GitHub notification open PendingIntents.
- [ ] Continue auditing IntentSender UI launch paths for Android 17 background-activity-start behavior.

### P1-D Input And Media Lifecycle

- [x] Add Android 17 foreground-only guard and hardening validation for BA guide media playback.
- [ ] Validate GitHub API token password masking on Android 17 with a physical keyboard and the system show-password setting; adopt `ShowSecretsSetting.shouldShowPassword(...)` only if Compose masking diverges from platform behavior.
- [ ] Re-validate BA guide BGM/video foreground-only playback on Android 17 during app switch, screen lock, split-screen, notification shade, and low-power mode; keep the playback lifecycle contract explicit until a MediaSession service is intentionally added.

### P1-E Notifications, Advanced Protection, And Hidden APIs

- [x] Validate MCP notification surfaces against Android 17 promoted-notification and Live Update behavior.
- [ ] Validate GitHub, BA AP/cafe/arena, and Super Island notification surfaces against Android 17 promoted-notification and Live Update behavior.
- [x] Add an Advanced Protection Mode status detector and expose it in About build details.
- [ ] Add behavior downgrade plans for Shizuku, package enumeration, battery-optimization, and local MCP LAN flows when Advanced Protection Mode is enabled.
- [ ] Audit Android 17 hidden-API and reflection risk around Shizuku/system-settings helpers; replace blocked reflection paths with documented fallbacks when available.
- [ ] Audit Android 17 MessageQueue / `static final` reflection risk after dependency upgrades; keep the current ActivityOptions, Shizuku, AppOps, and OEM-settings reflection paths on public-method or guarded fallback behavior.
- [ ] Validate Android 17 Safer Native DCL and packaged native library behavior after `packageDebug` / `packageBenchmark`; current source has zero project JNI or `System.load*`, and transitive `.so` artifacts need read-only and 16 KB checks.

### P1-F OEM Beta Matrix

- [ ] Test Android 17 OEM beta builds on HyperOS, ColorOS, OriginOS, MagicOS, and One UI when devices are available.

## P2 - Optional APIs, Accessibility, Large Screens, And Long-Term Tracking

### P2-A Optional Platform APIs

- [x] Shared `SDK_INT_FULL` helper is in place for Android 36.1 / 37.x minor API gates.
- [ ] Evaluate Android 16 embedded Photo Picker for future non-Home background image import or BA media selection; keep the current SAF path as the compatibility baseline until picker UX and URI grants are validated.
- [ ] Prefer Android 17 Contact Picker if contact import ever becomes a product requirement, keeping broad `READ_CONTACTS` out of the app.
- [ ] Evaluate Android 17 Handoff API only after a cross-device continuity feature enters the roadmap.

### P2-B Release, Toolchain, And Dependency Tracking

- [ ] Track Android 17 post-quantum / hybrid APK signing support in AGP, apksigner, GitHub Actions, and local release keystores before changing the release pipeline.
- [ ] Continue tracking stable OkHttp, Ktor, Coil, Media3, Navigation3, and Activity Compose support for Android 36 / 37 platform capabilities.

### P2-C Performance, Haptics, And Telemetry

- [ ] Evaluate richer haptics and frame-rate APIs for liquid bottom bar, sliders, gallery video, and fullscreen media after the current animation / material behavior is stable.
- [ ] Evaluate `ApplicationStartInfo`, `reportFullyDrawn`, allow-while-idle alarm listener APIs, and JobScheduler `JobDebugInfo` only when the performance telemetry roadmap needs deeper platform signals.

### P2-D Accessibility And Inclusive UX

- [ ] Run Android 16 TalkBack / accessibility smoke after the current core UI iteration stabilizes, covering dynamic feedback across GitHub share import, BA fetch/save/export flows, Settings permission flows, OS Shell output, and notification permission prompts; replace deprecated announcement patterns with pane titles, focus moves, or live-region semantics where runtime feedback is silent or duplicated.
- [ ] Validate Android 17 text-change accessibility after custom Compose input surfaces stabilize: `GlassSearchField`, OS Shell command input, GitHub token input, and BA guide filters; cover CJKV IME, physical keyboard, TalkBack, and text selection.

### P2-E Large Screen And Windowing

- [ ] Run API 36 `sw600dp` / desktop-window smoke for MainActivity, GitHub share import, uCrop, video fullscreen, and OS Shell Runner; record orientation, resizeability, edge-to-edge, and IME breakages before the broader large-screen redesign.
- [ ] Create a large-screen navigation plan for tablet, foldable, desktop, and landscape window sizes.
- [ ] Audit pages with locked portrait assumptions and identify screens that need pane or rail layouts.
- [ ] Build emulator or cloud-device validation flow before changing layouts.
- [ ] Validate Home, OS, BA, MCP, GitHub, Settings, and About on at least compact, medium, and expanded width classes.
- [ ] Revisit manifest orientation restrictions when a large-screen implementation pass is scheduled.
