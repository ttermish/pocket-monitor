from contextlib import redirect_stdout
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import package_release as release


class ReleasePackageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "repo"
        self.root.mkdir()
        files = {
            ".gitignore": "androidApp/build/\n",
            "README.md": "![Preview](docs/images/preview.png)",
            "README.zh-CN.md": "[English](README.md)",
            "docs/images/preview.png": "committed fixture image",
            "androidApp/src/main/assets/licenses/fixture.txt": "dependency license",
        }
        for name, text in files.items():
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
        for args in (["init", "-q"], ["add", "."],
                     ["-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "-qm", "fixture"]):
            subprocess.run(["git", *args], cwd=self.root, check=True, capture_output=True)
        self.revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=self.root, text=True).strip()
        self.apk_dir = self.root / "androidApp/build/outputs/apk/release"
        self.apk_dir.mkdir(parents=True)
        (self.apk_dir / "app.apk").write_bytes(b"fixture apk")
        self.metadata = {"elements": [{"versionName": "0.9.0", "versionCode": 9, "outputFile": "app.apk"}]}
        self.write_metadata()
        bundle = self.root / "androidApp/build/outputs/bundle/release/androidApp-release.aab"
        bundle.parent.mkdir(parents=True)
        bundle.write_bytes(b"fixture aab")
        self.dependencies = Path(self.temp.name) / "dependencies"
        self.dependencies.mkdir()
        self.dependency_data = b"pinned fixture dependency"
        (self.dependencies / "fixture.aar").write_bytes(self.dependency_data)
        self.output = Path(self.temp.name) / "output"

    def write_metadata(self):
        (self.apk_dir / "output-metadata.json").write_text(json.dumps(self.metadata))

    def package(self, tag="v0.9.0"):
        dependencies = {"fixture.aar": ("https://example.invalid/unused", hashlib.sha256(self.dependency_data).hexdigest())}
        with patch.object(release, "ROOT", self.root), patch.object(release, "DEPENDENCIES", dependencies), \
             patch.dict(os.environ, {"EXPECTED_TAG": tag, "GITHUB_REPOSITORY": "owner/project", "ANDROID_SIGNING_CERT_SHA256": "fixture"}), \
             patch("sys.argv", ["package_release.py", "--output", str(self.output), "--dependency-dir", str(self.dependencies)]), \
             redirect_stdout(io.StringIO()):
            release.main()

    def test_full_package_checksums_docs_and_uses_committed_text(self):
        # Uncommitted edits must not leak into docs labeled with an earlier source commit.
        (self.root / "README.md").write_text("uncommitted text")
        self.package()
        info = json.loads((self.output / "BUILD_INFO.json").read_text())
        self.assertEqual(info["commit"], self.revision)
        self.assertEqual(info["versionName"], "0.9.0")
        self.assertNotIn("uncommitted", (self.output / "README.md").read_text())
        self.assertIn(self.revision, (self.output / "README.md").read_text())
        with ZipFile(self.output / "pocket-monitor-0.9.0-docs.zip") as docs:
            self.assertIn("pocket-monitor-0.9.0/docs/images/preview.png", docs.namelist())
        checksums = {}
        for line in (self.output / "SHA256SUMS").read_text().splitlines():
            digest, name = line.split("  ", 1)
            checksums[name] = digest
            self.assertEqual(digest, hashlib.sha256((self.output / name).read_bytes()).hexdigest())
        self.assertIn("pocket-monitor-0.9.0-docs.zip", checksums)
        self.assertEqual(set(checksums), {p.name for p in self.output.iterdir()} - {"SHA256SUMS"})

    def test_tag_mismatch_does_not_create_release_output(self):
        with self.assertRaisesRegex(SystemExit, "Tag does not match"):
            self.package("v0.8.0")
        self.assertFalse(self.output.exists())

    def test_nonempty_directory_is_preserved(self):
        self.output.mkdir()
        existing = self.output / "old.apk"
        existing.write_bytes(b"existing release")
        with self.assertRaisesRegex(SystemExit, "must be empty"):
            self.package()
        self.assertEqual(existing.read_bytes(), b"existing release")
        self.assertEqual(list(self.output.iterdir()), [existing])

    def test_dependency_checksum_failure_stops_packaging(self):
        (self.dependencies / "fixture.aar").write_bytes(b"corrupt dependency")
        with self.assertRaisesRegex(SystemExit, "Dependency checksum mismatch"):
            self.package()
        self.assertFalse((self.output / "SHA256SUMS").exists())


if __name__ == "__main__":
    unittest.main()
