# KeiOS Documentation

This directory keeps long-lived project documentation that is useful for future
maintenance, release verification, or public review.

Start with the [project overview](../README.md), [documentation index](../readme/INDEX.md),
or [build guide](../readme/BUILD.md) ([中文](../readme/BUILD_CN.md)).

| Directory | Purpose |
| --- | --- |
| `build/` | Build, R8, packaging, and release configuration analysis |
| `performance/` | Performance plans, measurement notes, and optimization priorities |
| `planning/` | Feature plans, migration notes, and completed implementation records |
| `qa/` | Regression plans, verification checklists, and release QA notes |

## Contributor Entry Points

- [GitHub Actions](build/github-actions.md): CI builds, manual runs, downloads, checksums, and reports.
- [Build Logic](../build-logic/README.md): shared Gradle conventions and verification helpers.
- [R8 Configuration Analysis](build/r8-configuration-analysis.md): retained analysis of shrinker configuration.
- [Liquid Glass Study](planning/liquid-glass-meilox-study.md): MeiloX reference analysis, retained material updates, gesture ownership, and verification boundaries.
- [Android Widget and OEM Study](planning/android-appwidget-oem-study.md): AOSP/Google contracts, Xiaomi, OPPO, vivo, and HONOR extensions, evidence gaps, and KeiOS implementation considerations.
- [GitHub Project Widget](planning/github-project-widget.md): addition, responsive content, privacy, cached state, and implementation ownership.
- [Widget Layout and Material Study](planning/github-widget-material-study.md): system-widget references, Super Island hierarchy, soft-light surfaces, and the Xiaomi MAML glass boundary.
- [Contributing](../CONTRIBUTING.md): change scope, evidence, and documentation expectations.

Local draft notes belong in `.planning/`, which is ignored by Git. Move durable
plans into `docs/planning/` once they become useful project history.
Recorded measurements and completed QA notes describe their captured inputs;
check current source, artifacts, and device conditions before reusing a conclusion.
