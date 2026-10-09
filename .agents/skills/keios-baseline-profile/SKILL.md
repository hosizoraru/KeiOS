---
name: keios-baseline-profile
description: >-
  Collect or regenerate KeiOS Baseline Profiles and verify release packaging.
  Use for profile collection or packaging failures, not general UI performance audits.
---

# KeiOS Baseline Profile

Deliver fresh generated profiles for the intended source and verify that the
release APK packages them. Collection, packaging, and measured performance are
separate outcomes. Paths in commands are relative to the repository root.

## Inputs and scope

- Read [coverage and collection plan](../../../docs/planning/baseline-profile-coverage.md)
  for the current journeys, replay limits, and acceptance evidence. Confirm them
  against `baselineprofile/src/` rather than copying historical counts or timings.
- Inspect [producer configuration](../../../baselineprofile/build.gradle.kts)
  and [app configuration](../../../app/build.gradle.kts) for connected-device,
  variant, and generated-source wiring. Use [build guide](../../../readme/BUILD.md)
  when build commands or environment setup need resolving.
- State the intended device, journey set, and runtime budget before expensive capture.
  Keep journeys deterministic and bounded around useful hot paths. Changes to
  journey/replay limits must update their existing contract test and coverage plan.
- Bind the intended device with `ANDROID_SERIAL` and use one SDK ADB installation/server.
  A Phone/Tablet matrix can use comma-separated serials in the same variable; require
  all six journeys and fresh exports for each selected device before accepting its merge.
  Preserve other tasks' device, ADB, and Gradle sessions; use an isolated available
  target or report the specific conflict.
- On physical devices, verify both collector APK identities before installation:
  `os.kei.profilecapture` and `os.kei.baselineprofile.capture`. Preserve existing
  release/debug/diagnostic/old producer packages and data. Connected tasks may
  uninstall their APKs; never use the main-package benchmark variant for collection.
  Snapshot window/system settings and verify restoration after success or failure.
- Permission grants are a prerequisite to all journeys. The producer's
  `prepareProfileCapturePermissions()` runs after installation, grants declared
  runtime permissions and corresponding AppOps, handles the verified HyperOS
  installed-app gate, and checks readback. Fix a failed preflight before expensive
  collection. Keep authorization limited to the disposable capture identity; do
  not use blanket operations against release/debug/diagnostic packages.
- The producer temporarily uses priority DND and restores the previous mode. Include
  `zen_mode` in the host snapshot for interrupted-run recovery. On OEM Pads, keep
  taps outside status-bar bounds and wait for chrome geometry to settle; preserve
  failure screenshots/coordinates when a system overlay blocks navigation.

## Collection and acceptance

Use the explicit collection task with the selected serial:

```bash
ANDROID_SERIAL=<selected-avd-serial> ./gradlew :app:generateReleaseBaselineProfile
```

Instrumentation success alone is insufficient. Verify the true Gradle exit status,
fresh per-journey and merged outputs, and both generated files under
`app/src/release/generated/baselineProfiles/`: `baseline-prof.txt` and
`startup-prof.txt`. Correlate outputs with this run's source and target so old
files cannot stand in for a completed capture.

Build the affected release APK using the current build guide and check its
`assets/dexopt/baseline.prof` and `assets/dexopt/baseline.profm` entries. Record
source revision plus relevant working-tree changes, target/API, task result,
generated outputs, and APK identity in the task evidence. Resolve failures caused
by the requested changes and rerun only invalidated checks.

For the release freshness gate, use
[scripts/qa/baseline_profile_freshness.sh](../../../scripts/qa/baseline_profile_freshness.sh).
It compares committed runtime sources with the profile commit; inspect uncommitted
runtime changes separately. A pre-commit stale result is not by itself proof that
fresh local collection failed, and a passing committed-ref check does not cover
uncommitted changes. Keep both facts explicit before delivery.

A controlled AVD proves collection and packaging. If performance comparison is
requested, use comparable release/benchmark conditions and the relevant performance
skill; do not turn collection duration or profile rule counts into a speedup claim.
