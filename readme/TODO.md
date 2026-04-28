# KeiOS Todo List

This file tracks roadmap items that need a stable device, OEM beta, or focused validation window.

## Android 17 Adaptation

Official references checked on 2026-04-29:
[target SDK behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17),
[all-app behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all),
[new features and APIs](https://developer.android.com/about/versions/17/features).

- [x] Add Android 17 local network permissions for MCP LAN mode.
- [x] Request local network permission before starting MCP in LAN mode.
- [x] Register Android 17 anomaly profiling trigger for resource anomalies.
- [x] Inspect previous process exits for MemoryLimiter or excessive resource usage signals.
- [x] Add fair background scheduling pass for GitHub and BA tick jobs.
- [x] Add Android 17 foreground-only guard and hardening validation for BA guide media playback.
- [ ] Audit cleartext network policy for MCP loopback/LAN HTTP endpoints and any GameKee/GitHub redirected asset URLs; add a Network Security Config if API 37 validation shows blocked legitimate traffic.
- [ ] Validate FileProvider, uCrop, custom media-save, and ZIP export URI grant flows on Android 17; add explicit package grants where chooser or third-party targets drop access.
- [ ] Audit PendingIntent and IntentSender UI launches for Android 17 background-activity-start behavior; keep explicit launch allowances limited to user-visible notification actions.
- [ ] Validate MCP, GitHub, BA AP/cafe/arena, and Super Island notification surfaces against Android 17 promoted-notification and Live Update behavior.
- [ ] Add an Advanced Protection Mode compatibility plan for Shizuku, package enumeration, battery-optimization, and local MCP LAN flows.
- [ ] Run Android 17 AVD profiling for alarm windows, wakeups, and previous-exit logs after long idle; compare GitHub and BA tick behavior with the fair scheduling policy.
- [ ] Audit Android 17 hidden-API and reflection risk around Shizuku/system-settings helpers; replace blocked reflection paths with documented fallbacks when available.
- [ ] Evaluate HTTPS hardening readiness for OkHttp traffic, including Certificate Transparency opt-in and ECH compatibility, after checking GitHub and GameKee endpoint behavior.
- [ ] Test Android 17 OEM beta builds on HyperOS, ColorOS, OriginOS, MagicOS, and One UI when devices are available.

### Android 17 Source-Audit Notes

- Contacts, Bluetooth, and custom RemoteViews paths are currently non-applicable after source search.
- Large-screen behavior changes are tracked in the large-screen section because the project has no active large-screen implementation plan.

## Large Screen Adaptation

- [ ] Create a large-screen navigation plan for tablet, foldable, desktop, and landscape window sizes.
- [ ] Audit pages with locked portrait assumptions and identify screens that need pane or rail layouts.
- [ ] Build emulator or cloud-device validation flow before changing layouts.
- [ ] Validate Home, OS, BA, MCP, GitHub, Settings, and About on at least compact, medium, and expanded width classes.
- [ ] Revisit manifest orientation restrictions when a large-screen implementation pass is scheduled.
