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
[Android 17 target SDK behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17),
[Android 17 all-app behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all),
[Android 17 new features and APIs](https://developer.android.com/about/versions/17/features),
[local network permission](https://developer.android.com/privacy-and-security/local-network-permission).

- [x] Build baseline is `minSdk=35`, `compileSdk=37`, `targetSdk=37`, with Java / Kotlin toolchains on 21.
- [x] Manifest declares `NEARBY_WIFI_DEVICES(maxSdk=36)`, `ACCESS_LOCAL_NETWORK`, `USE_LOOPBACK_INTERFACE`, `POST_PROMOTED_NOTIFICATIONS`, and `FOREGROUND_SERVICE_SPECIAL_USE`.
- [x] Main Activity enables `enableEdgeToEdge()`, launcher icons provide monochrome assets, and main navigation plus guide fullscreen image flow use predictive-back paths.
- [x] Source search currently finds zero Contacts, Bluetooth, custom RemoteViews, `MediaStore#getVersion()`, `scheduleAtFixedRate`, WorkManager, or direct JobScheduler adaptation surfaces.
- [x] Add a shared API-version helper for `SDK_INT_FULL`, API 36, API 36.1, and API 37 gates.
- [x] Add Runtime API Full and Advanced Protection Mode status to About build details.

## P0 - API 36 Primary-Device Validation

- [ ] Validate edge-to-edge, IME, status bar, and navigation bar insets on Android 16 / API 36 devices or AVDs across Home, OS, BA, MCP, GitHub, Settings, About, GitHub share import window, uCrop, and video fullscreen.
- [ ] Validate predictive back across Nav3 main pages, Settings, student guide detail, guide fullscreen image, OS Shell Runner, and GitHub share import Activity with gesture and 3-button navigation.
- [x] Add constrained `NEARBY_WIFI_DEVICES(maxSdk=36)` support and route MCP LAN permission requests through shared API 36 / API 37 permission logic.
- [x] Add `scripts/qa/android_api_compat_probe.sh` for API 36 / 37 device version, manifest permission, AppOps, and optional `RESTRICT_LOCAL_NETWORK` compat-flag checks.
- [ ] Use the Android 16 `RESTRICT_LOCAL_NETWORK` compat flag to validate MCP loopback / LAN mode.
- [x] Harden high-traffic Safer Intents paths across GitHub `ACTION_SEND` import Activity, downloader selection, external links, and share-APK-link flows with stricter action, MIME, scheme, host, and package boundaries.
- [ ] Continue auditing lower-frequency user-defined OS shortcut intents plus notification / shortcut extras.
- [ ] Validate DownloadManager, GitHub background refresh, and BA AP / cafe / arena reminders under Android 16 idle, lockscreen, and battery-saver states; retain the source-audit finding that fixed-rate work paths are absent.
- [ ] Validate MCP `specialUse` foreground service recovery on Android 16 when background start limits, denied notification permission, battery saver, and inactive Shizuku states are present.
- [ ] Validate MCP, GitHub, BA, and Super Island notification visuals and tap behavior on Android 16 promoted notifications and `NotificationCompat.ProgressStyle`.
- [ ] Audit `setAccessible` / hidden-API risk points, prioritizing Shizuku, HyperOS settings jumps, and keep-alive permission helper paths.

## P1 - API 37 Enforced Adaptation

- [x] Add Android 17 local network permissions for MCP LAN mode.
- [x] Request local network permission before starting MCP in LAN mode.
- [x] Register Android 17 anomaly profiling trigger for resource anomalies.
- [x] Inspect previous process exits for MemoryLimiter or excessive resource usage signals.
- [x] Add fair background scheduling pass for GitHub and BA tick jobs.
- [x] Add Android 17 foreground-only guard and hardening validation for BA guide media playback.
- [x] Normalize `http://` GitHub share links, GitHub downloader URLs, and GameKee / BA guide asset links to `https://`; restrict DownloadManager to HTTPS external download URLs.
- [ ] Continue auditing MCP loopback / LAN HTTP endpoints; add a narrow Network Security Config only if API 37 validation blocks legitimate traffic.
- [x] Add explicit URI grant support for the non-Home background FileProvider / uCrop crop flow.
- [ ] Continue validating custom media-save and ZIP export URI grant flows on Android 17; add explicit package grants where chooser or third-party targets drop access.
- [x] Add user-visible notification action background-activity-start allowance for MCP and GitHub notification open PendingIntents.
- [ ] Continue auditing IntentSender UI launch paths for Android 17 background-activity-start behavior.
- [ ] Validate MCP, GitHub, BA AP/cafe/arena, and Super Island notification surfaces against Android 17 promoted-notification and Live Update behavior.
- [x] Add an Advanced Protection Mode status detector and expose it in About build details.
- [ ] Add behavior downgrade plans for Shizuku, package enumeration, battery-optimization, and local MCP LAN flows when Advanced Protection Mode is enabled.
- [ ] Run Android 17 AVD profiling for alarm windows, wakeups, and previous-exit logs after long idle; compare GitHub and BA tick behavior with the fair scheduling policy.
- [ ] Audit Android 17 hidden-API and reflection risk around Shizuku/system-settings helpers; replace blocked reflection paths with documented fallbacks when available.
- [ ] Evaluate HTTPS hardening readiness for OkHttp traffic, including Certificate Transparency opt-in and ECH compatibility, after checking GitHub and GameKee endpoint behavior.
- [ ] Test Android 17 OEM beta builds on HyperOS, ColorOS, OriginOS, MagicOS, and One UI when devices are available.

## P2 - Optional New APIs And Long-Term Tracking

- [x] Shared `SDK_INT_FULL` helper is in place for Android 36.1 / 37.x minor API gates.
- [ ] Prefer Android 17 Contact Picker if contact import ever becomes a product requirement, keeping broad `READ_CONTACTS` out of the app.
- [ ] Evaluate Android 17 Handoff API only after a cross-device continuity feature enters the roadmap.
- [ ] Continue tracking stable OkHttp, Ktor, Coil, Media3, Navigation3, and Activity Compose support for Android 36 / 37 platform capabilities.

## P2 - Large Screen Adaptation

- [ ] Create a large-screen navigation plan for tablet, foldable, desktop, and landscape window sizes.
- [ ] Audit pages with locked portrait assumptions and identify screens that need pane or rail layouts.
- [ ] Build emulator or cloud-device validation flow before changing layouts.
- [ ] Validate Home, OS, BA, MCP, GitHub, Settings, and About on at least compact, medium, and expanded width classes.
- [ ] Revisit manifest orientation restrictions when a large-screen implementation pass is scheduled.
