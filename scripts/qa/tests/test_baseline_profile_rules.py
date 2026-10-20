import os
from pathlib import Path
import tempfile
import unittest

from scripts.qa.baseline_profile_rules import (
    JOURNEYS,
    audit_profiles,
    capture_profiles,
    merged_flags,
    read_profile,
)


class BaselineProfileRulesTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.root = Path(self.folder.name)

    def profile(self, name, text):
        path = self.root / name
        path.write_text(text)
        return read_profile(path)

    def test_reads_class_method_and_comment_rules(self):
        profile = self.profile("rules", "# comment\n\nLa/b/C;\nHSLa/b/C;->run([I)V\n")
        self.assertEqual(profile.flags, {"La/b/C;": "", "La/b/C;->run([I)V": "HS"})

    def test_rejects_invalid_rules_and_repeated_flags(self):
        for text in ["HHLa/b/C;\n", "QLa/b/C;\n", "HS La/b/C;\n", "not a rule\n"]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                self.profile("invalid", text)

    def test_duplicate_signatures_fail_even_with_different_flags(self):
        baseline = self.profile("base", "HSLa/b/C;->run()V\nPLa/b/C;->run()V\n")
        startup = self.profile("startup", "HSLa/b/C;->run()V\n")
        result = audit_profiles(baseline, startup, coverage={})
        self.assertEqual(result["baseline_duplicates"], 1)
        self.assertTrue(result["errors"])

    def test_merge_uses_androidx_first_sorted_rule_instead_of_flag_union(self):
        first = self.profile("first", "PLa/b/C;->run()V\n")
        second = self.profile("second", "HSLa/b/C;->run()V\n")
        self.assertEqual(merged_flags([first, second]), {"La/b/C;->run()V": "HS"})

    def test_startup_membership_and_missing_coverage_fail(self):
        base = self.profile("base", "La/b/C;\n")
        startup = self.profile("startup", "La/b/Missing;\n")
        result = audit_profiles(base, startup, coverage={"renderer": "La/b/Renderer;"})
        self.assertEqual(result["startup_missing_from_baseline"], 1)
        self.assertIn("Missing coverage: renderer", result["errors"])

    def test_capture_only_rules_fail(self):
        profile = self.profile("base", "HLos/kei/profilecapture/ViewerHarness;->start()V\n")
        result = audit_profiles(profile, profile, coverage={})
        self.assertTrue(any("capture-only" in error for error in result["errors"]))

    def exports(self):
        device = self.root / "device"
        device.mkdir()
        for index, journey in enumerate(JOURNEYS):
            kind = "startup" if index == 0 else "baseline"
            (device / f"BaselineProfileGenerator_{journey}-{kind}-prof.txt").write_text(
                f"HSLa/b/Path{index};->run()V\n"
            )
        return device

    def test_capture_requires_all_six_fresh_exports(self):
        device = self.exports()
        self.assertEqual(len(capture_profiles(self.root, 0)[str(device)]), 6)
        one = next(device.iterdir())
        os.utime(one, (1, 1))
        with self.assertRaisesRegex(ValueError, "stale"):
            capture_profiles(self.root, 2)
        one.unlink()
        with self.assertRaisesRegex(ValueError, "Incomplete"):
            capture_profiles(self.root, 0)

    def test_accepts_identical_dated_sibling_but_rejects_disagreement(self):
        device = self.exports()
        one = next(device.iterdir())
        dated = one.with_name(one.stem + "-2026-10-20-00-00-00.txt")
        dated.write_bytes(one.read_bytes())
        self.assertEqual(len(capture_profiles(self.root, 0)[str(device)]), 6)
        dated.write_text("La/b/Different;\n")
        with self.assertRaisesRegex(ValueError, "disagree"):
            capture_profiles(self.root, 0)

    def test_old_appended_rules_and_wrong_startup_union_fail(self):
        self.exports()
        captures = capture_profiles(self.root, 0)
        raw = [profile for journeys in captures.values() for profile in journeys.values()]
        text = "".join(flags + signature + "\n" for signature, flags in merged_flags(raw).items())
        base = self.profile("base", text)
        startup = self.profile("startup", "HSLa/b/Path0;->run()V\n")
        self.assertFalse(audit_profiles(base, startup, captures, coverage={})["errors"])
        appended = self.profile("appended", text + "La/b/Old;\n")
        result = audit_profiles(appended, base, captures, coverage={})
        self.assertEqual(result["capture"]["extra_signatures"], 1)
        self.assertIn("Startup does not match only the fresh startup journey", result["errors"])


if __name__ == "__main__":
    unittest.main()
