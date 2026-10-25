# KeiOS Agent Guide

## Task execution

- Start with the relevant worktree status and diff; preserve unrelated and concurrent work. Read the owning source and only the project docs needed for this task.
- Complete the requested outcome through the relevant checks below. Run focused local checks when they can catch a regression; use local or disposable data, inspect UI/device behavior when the change affects it, fix failures caused by the change, and rerun invalidated checks. Skip unrelated full-suite runs and audits.
- Make routine choices from current code and conventions. Continue without pausing for routine approval. Ask only when a material product or scope decision is missing, or when the request does not authorize an external or shared-state action.
- Commit, push, release, or publish only when explicitly requested. A skill example does not authorize an architecture migration, dependency upgrade, commit, or publication.
- Keep commit subjects concise, aiming for 50 characters or fewer. Put details, rationale, and validation in the body.
- Keep long-task handoffs in ignored .planning/: objective, owning paths, decisions, verification results and inputs, remaining gates, and next action. On resume, refresh evidence whose inputs changed.
- When parallel work is authorized, keep one owner for shared worktree, device, ADB, and Gradle state.
- Report what changed, checks passed, checks not run and why, and blocked checks. Separate source, build/artifact, installation, and observed behavior claims.

## Evidence and architecture

- Use memory to locate context, then verify relevant claims against current source and task evidence. Dated captures do not establish current behavior. Check dependency and release-channel claims in the version catalog, architecture in the owning module, and performance claims against source, build, device, and capture conditions.
- The minimum supported API is Android 35; preserve it unless requirements change. Read SDK, Kotlin, Compose, and dependency versions from gradle/libs.versions.toml and affected Gradle files when the task touches them.
- Preserve current module ownership, Miuix navigation (top.yukonga.miuix.kmp.nav), StateFlow, Compose, Miuix, Coil 3, Roborazzi, and custom stores. Keep the existing Route/Screen/leaf split: routes obtain ViewModels and platform services, screens render state and callbacks, and leaves receive narrow props and emit specific events.
- MIUIX must use the latest SNAPSHOT. Other dependencies use the latest RC or final release. For dependency maintenance, verify coordinates, API compatibility, Android support, and the Miuix/Compose relationship. Resolve a concrete version conflict within scope and preserve the required release channel. A focused feature or documentation change does not require unrelated upgrades.
- For Android platform behavior, inspect local SDK sources matching the relevant compile or device API; use official sources when local sources do not resolve the question.
- Add notification types through the existing framework and channels. Framework changes require a concrete behavior need and verification of system recognition.
- Put new user-facing strings in the existing domain resources and keep supported locale keys, placeholders, and meaning aligned. Use [KeiOS Product UX Writing](.agents/skills/keios-product-ux-writing/SKILL.md) when wording or a public text contract changes.
- Keep files cohesive. Around 1,000 lines is a signal to inspect responsibility boundaries; split only when it improves ownership for the requested change.

## Skill routing

- Use android-ux-design for Android visual design and UX, and compose-expert for Compose implementation or review when those skills are available. Resolve these optional skills from the session catalog and local .agents/skills or .claude/skills directories. If absent, continue with owning sources, nearby tests, and platform guidance; record the unavailable skill without repeating an exhaustive search. Preserve Miuix and existing custom surfaces when applying general Material guidance.
- Use focused platform, accessibility, navigation, coroutine, and ViewModel skills for the behavior being changed. Load supporting references only when the active task needs them.
- For KeiOS frame-time or jank investigations, use [the KeiOS frame-performance skill](.claude/skills/keios-frame-performance/SKILL.md) when available. Use a focused performance skill for a known issue and auditing-compose-performance for a requested broad audit or broad symptom that needs measurement and diagnosis.
- For Baseline Profile collection, packaging diagnosis, or regeneration, use [KeiOS Baseline Profile](.agents/skills/keios-baseline-profile/SKILL.md). Use testing-compose-in-release-mode for performance measurements when available; otherwise follow the measurement setup linked from the local frame-performance skill. A freshness check alone can use the release gate below without loading a collection workflow.
- Apply skill checklists to the selected task and supported environment. A text edit does not need an app-wide UX audit; a Profile-only refresh does not need a new performance study. Preserve release gates when shipping is in scope.

## Verification by change

Before running device scripts, inspect the target and side effects: AVD helpers can build, install, replace packages, or erase data through optional flags. A documented option is not authorization to use it.

Choose checks that can falsify the changed behavior. For build and test commands, consult [readme/BUILD.md](readme/BUILD.md), affected Gradle configuration, and nearby tests. Use only the row relevant to the change.

| Change | Relevant evidence |
| --- | --- |
| Instructions or documentation only | Diff, referenced paths, links, and claim consistency; validate skill structure when a skill changes |
| Kotlin logic or state flow | Focused existing tests, meaningful regression coverage for changed behavior, and compilation of affected consumers |
| Compose UI or localized copy | Relevant screenshot, semantics, or resource checks; inspect on device or emulator when layout, input, insets, or transitions change |
| R8, dependencies, or release packaging | Affected release build and packaged-artifact checks; debug success alone cannot establish release behavior |
| Performance | Release or benchmark measurements for the affected journey under comparable device and runtime conditions |
| Baseline Profile generation | Completed collection task, fresh generated files, and packaged profile verification |

- Run required checks once for the final relevant inputs. Expand or repeat them for changes, failures, or unresolved concerns. Reuse still-valid results and state what they cover.
- Add tests for observable behavior and meaningful regressions. Avoid tests that only mirror implementation text. Review screenshot differences before updating goldens.
- For adaptive UI, cover affected phone/tablet window classes and input modes; use the task's device matrix rather than unrelated screens.
- For release work, run scripts/qa/baseline_profile_freshness.sh and account separately for uncommitted runtime changes: the script compares committed refs. Regeneration follows the Baseline Profile skill. A controlled AVD can establish collection and packaging; user-perceived performance requires suitable release/benchmark measurements.
- A missing device limits device claims; continue source and other independent verification the task permits.

## Maintaining instructions

- Keep this file for repository-wide constraints and verification gates. Put specialized workflows in skills; descriptions should identify the actual task, and supporting detail should load only when needed.
- Keep dated measurements and evolving findings in their owning planning documents instead of copying them into general instructions. Treat memory as a locator, not refreshed evidence. Generated memory storage is updated only through the supported workflow on explicit user request.
- When revising instructions, validate changed paths, links, and skill routing against representative requests. Format validation alone does not prove better task outcomes; claim quality or efficiency gains only after comparable task runs.

Instruction design: [OpenAI’s Astra guidance](https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra) recommends task-relevant context and fewer redundant prescriptions. The verification and authorization rules here are project practices for all agents, including Sol; they do not change model settings or grant permissions.

## macOS APFS copies

- Only when a task needs an independent project or experiment copy, prefer system `/bin/cp -cRp project project-experiment` on the same APFS volume. First confirm that the source and destination parent are on the same clone-capable filesystem, and that the destination does not exist and is outside the source tree. Do not copy the whole project for ordinary edits.
- Cloned files initially share data blocks but remain independently editable through copy-on-write; they are not hard links. Later writes, new dependencies, and build/DerivedData outputs still consume space; whole-file rewrites can lose the sharing benefit.
- Across volumes or without clone support, `cp -c` falls back to ordinary copying. Command success alone does not prove cloning or space savings. Assess available space before copying, especially large directories, to avoid an unintended full copy. Do not sum logical directory sizes as physical storage growth. Clones replace neither Git nor backups on another disk.
- References: Apple [cp(1)](https://github.com/apple-oss-distributions/file_cmds/blob/main/cp/cp.1) and [clonefile(2)](https://github.com/apple-oss-distributions/xnu/blob/main/bsd/man/man2/clonefile.2).
