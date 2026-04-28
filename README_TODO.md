# KeiOS Todo List

This file tracks roadmap items that need a stable device, OEM beta, or focused validation window.

## Android 17 Adaptation

- [x] Add Android 17 local network permissions for MCP LAN mode.
- [x] Request local network permission before starting MCP in LAN mode.
- [x] Register Android 17 anomaly profiling trigger for resource anomalies.
- [x] Inspect previous process exits for MemoryLimiter or excessive resource usage signals.
- [x] Add fair background scheduling pass for GitHub and BA tick jobs.
- [ ] Add Android 17 background audio validation for BA guide media playback.
- [ ] Test Android 17 OEM beta builds on HyperOS, ColorOS, OriginOS, MagicOS, and One UI when devices are available.

## Large Screen Adaptation

- [ ] Create a large-screen navigation plan for tablet, foldable, desktop, and landscape window sizes.
- [ ] Audit pages with locked portrait assumptions and identify screens that need pane or rail layouts.
- [ ] Build emulator or cloud-device validation flow before changing layouts.
- [ ] Validate Home, OS, BA, MCP, GitHub, Settings, and About on at least compact, medium, and expanded width classes.
- [ ] Revisit manifest orientation restrictions when a large-screen implementation pass is scheduled.
