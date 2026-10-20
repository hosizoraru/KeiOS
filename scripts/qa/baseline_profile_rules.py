#!/usr/bin/env python3
"""Audit generated ART rules and, optionally, their complete fresh capture union."""

import argparse
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import re


JOURNEYS = (
    "startupAndFirstScroll",
    "mainPagesAndNavigation",
    "commonRoutesAndChrome",
    "gitHubTrackingCore",
    "baOfficeAndCatalogCore",
    "adaptiveLargeScreenCore",
)
RULE = re.compile(r"([HSP]*)(L[^;\s]+;(?:->[^\s]+)?)")
EXPORT = re.compile(
    r"BaselineProfileGenerator_(\w+)-(baseline|startup)-prof"
    r"(?:-\d{4}(?:-\d{2}){5})?\.txt"
)
CAPTURE_ONLY = ("Los/kei/profilecapture/", "Los/kei/baselineprofile/")
GLASS = "Los/kei/ui/page/main/widget/glass/"
SHEET = "Los/kei/ui/page/main/widget/sheet/"
REQUIRED_COVERAGE = {
    "home": "Los/kei/ui/page/main/home/",
    "main_navigation": "Los/kei/ui/page/main/host/",
    "compose_lazy": "Landroidx/compose/foundation/lazy/",
    "sheet_renderer": GLASS + "SheetGlassNode;",
    "shader_pool": GLASS + "LiquidShaderPool;",
    "decoration_geometry": GLASS + "LiquidDecorationGeometry;",
    "sheet_viewport": SHEET + "LiquidSheetViewportDrawNode;",
    "scene_source": SHEET + "SceneBackdropScopeKt;->SceneBackdropHost",
}


@dataclass
class Profile:
    path: Path
    lines: list[str]
    flags: dict[str, str]

    @property
    def duplicates(self) -> int:
        return len(self.lines) - len(self.flags)


def read_profile(path: Path) -> Profile:
    lines = []
    flags = {}
    for number, raw in enumerate(path.read_text().splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        match = RULE.fullmatch(line)
        if match is None or len(set(match[1])) != len(match[1]):
            raise ValueError(f"{path}:{number}: invalid ART rule: {line}")
        lines.append(line)
        flags.setdefault(match[2], match[1])
    if not lines:
        raise ValueError(f"Empty profile: {path}")
    return Profile(path, lines, flags)


def merged_flags(profiles: list[Profile]) -> dict[str, str]:
    # AndroidX 1.5 sorts raw rules and retains the first rule for each descriptor.
    # A manual flag union would produce a different result from its merge task.
    result = {}
    for line in sorted(line for profile in profiles for line in profile.lines):
        match = RULE.fullmatch(line)
        result.setdefault(match[2], match[1])
    return result


def capture_profiles(root: Path, started_at: float) -> dict[str, dict[str, Profile]]:
    groups = {}
    for path in sorted(root.rglob("BaselineProfileGenerator_*-prof*.txt")):
        match = EXPORT.fullmatch(path.name)
        if match is None:
            continue
        journey, kind = match[1], match[2]
        if journey not in JOURNEYS:
            raise ValueError(f"Unexpected capture journey: {path}")
        expected_kind = "startup" if journey == JOURNEYS[0] else "baseline"
        if kind != expected_kind or path.stat().st_mtime < started_at:
            raise ValueError(f"Wrong kind or stale capture export: {path}")
        profile = read_profile(path)
        journeys = groups.setdefault(str(path.parent), {})
        previous = journeys.get(journey)
        if previous is not None and previous.lines != profile.lines:
            raise ValueError(f"Dated and undated exports disagree: {path}")
        journeys[journey] = profile
    if not groups:
        raise ValueError(f"No capture exports: {root}")
    for device, journeys in groups.items():
        missing = set(JOURNEYS) - journeys.keys()
        if missing:
            raise ValueError(f"Incomplete capture in {device}: {sorted(missing)}")
    return groups


def audit_profiles(
    baseline: Profile,
    startup: Profile,
    captures: dict[str, dict[str, Profile]] | None = None,
    coverage: dict[str, str] = REQUIRED_COVERAGE,
) -> dict:
    errors = []
    for profile in (baseline, startup):
        if profile.duplicates:
            errors.append(f"{profile.path}: {profile.duplicates} duplicate signatures")
        if any(token in line for token in CAPTURE_ONLY for line in profile.lines):
            errors.append(f"{profile.path}: capture-only rules")
    missing_startup = startup.flags.keys() - baseline.flags.keys()
    if missing_startup:
        errors.append(f"{len(missing_startup)} startup signatures missing from baseline")
    counts = {
        name: sum(signature.startswith(prefix) for signature in baseline.flags)
        for name, prefix in coverage.items()
    }
    errors.extend(f"Missing coverage: {name}" for name, count in counts.items() if count == 0)
    result = {
        "baseline_rules": len(baseline.flags),
        "startup_rules": len(startup.flags),
        "baseline_duplicates": baseline.duplicates,
        "startup_duplicates": startup.duplicates,
        "startup_missing_from_baseline": len(missing_startup),
        "coverage": counts,
        "source_sha256": {
            profile.path.name: hashlib.sha256(profile.path.read_bytes()).hexdigest()
            for profile in (baseline, startup)
        },
    }
    if captures is not None:
        raw = [profile for journeys in captures.values() for profile in journeys.values()]
        expected = merged_flags(raw)
        expected_startup = merged_flags([journeys[JOURNEYS[0]] for journeys in captures.values()])
        missing = expected.keys() - baseline.flags.keys()
        extra = baseline.flags.keys() - expected.keys()
        flags = sum(baseline.flags[key] != expected[key] for key in expected.keys() & baseline.flags.keys())
        if missing or extra or flags:
            errors.append(f"Fresh merge mismatch: {len(missing)} missing, {len(extra)} extra, {flags} flags")
        if startup.flags != expected_startup:
            errors.append("Startup does not match only the fresh startup journey")
        result["capture"] = {
            "device_output_directories": list(captures),
            "journeys_per_device": len(JOURNEYS),
            "raw_rule_lines": sum(len(profile.lines) for profile in raw),
            "within_journey_duplicates": sum(profile.duplicates for profile in raw),
            "cross_journey_duplicates": sum(len(profile.flags) for profile in raw) - len(expected),
            "missing_signatures": len(missing),
            "extra_signatures": len(extra),
            "flag_mismatches": flags,
            "androidx_merge_policy": "sorted first rule per descriptor",
        }
    result["errors"] = errors
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    repo = Path(__file__).resolve().parents[2]
    parser.add_argument("--profiles", type=Path, default=repo / "app/src/release/generated/baselineProfiles")
    parser.add_argument("--capture-dir", type=Path)
    parser.add_argument("--capture-start-marker", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if bool(args.capture_dir) != bool(args.capture_start_marker):
        parser.error("--capture-dir and --capture-start-marker must be provided together")
    try:
        captures = None
        if args.capture_dir is not None:
            captures = capture_profiles(
                args.capture_dir, int(args.capture_start_marker.read_text().strip()) / 1000,
            )
        result = audit_profiles(
            read_profile(args.profiles / "baseline-prof.txt"),
            read_profile(args.profiles / "startup-prof.txt"),
            captures,
        )
    except (OSError, ValueError) as error:
        result = {"errors": [str(error)]}
    text = json.dumps(result, indent=2) + "\n"
    if args.output is not None:
        args.output.write_text(text)
    print(text, end="")
    return int(bool(result["errors"]))


if __name__ == "__main__":
    raise SystemExit(main())
