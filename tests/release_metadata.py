"""Shared assertion for the metadata of the release under test."""
import re


def assert_release_metadata(test, root):
    gradle = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
    version = re.search(r'^\s*versionName = "([0-9]+(?:\.[0-9]+)+)"\s*$', gradle, re.M)
    code = re.search(r"^\s*versionCode = ([1-9][0-9]*)\s*$", gradle, re.M)
    test.assertIsNotNone(version)
    test.assertIsNotNone(code)

    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    latest = changelog.split("\n## ", 2)[1]
    test.assertTrue(latest.startswith(f"{version[1]} "))
    test.assertIn(f"versionCode {code[1]}", latest)
    test.assertTrue((root / f"docs/releases/v{version[1]}.md").is_file())
