import tempfile
from pathlib import Path
import unittest
from zipfile import ZipFile

from package_docs import online_readme, package_documentation


class DocumentationPackageTest(unittest.TestCase):
    revision = "a" * 40
    prefix = "pocket-monitor-0.1.3"

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / "source.zip"
        self.output = self.root / "output"
        self.files = {
            "README.md": "[![Preview](docs/images/preview.png)](docs/images/preview.png)\n[Guide](docs/README.md#start)",
            "README.zh-CN.md": "![界面](docs/images/preview.png)\n[English](README.md)",
            "docs/README.md": "[Home](../README.md)\n[截图](images/preview.png)",
            "docs/images/preview.png": b"unchanged image bytes",
            "THIRD_PARTY_NOTICES.md": "[Guide](docs/README.md)",
            "LICENSE": "fixture project license",
            "androidApp/src/main/assets/licenses/upstream.md": "[Upstream-only link](README.upstream)",
            "androidApp/src/main/kotlin/App.kt": "fixture source code",
            "scripts/helper.py": "fixture tool",
        }

    def package(self):
        with ZipFile(self.source, "w") as archive:
            for name, data in self.files.items():
                archive.writestr(self.prefix + "/" + name, data)
        package_documentation(self.source, self.output, self.prefix, "owner/project", self.revision)

    def test_offline_archive_keeps_relative_links_images_and_licenses(self):
        self.package()
        with ZipFile(self.output / (self.prefix + "-docs.zip")) as archive:
            names = archive.namelist()
            self.assertEqual(archive.read(self.prefix + "/README.md").decode(), self.files["README.md"])
            self.assertEqual(archive.read(self.prefix + "/docs/images/preview.png"), self.files["docs/images/preview.png"])
            self.assertIn(self.prefix + "/LICENSE", names)
            self.assertIn(self.prefix + "/androidApp/src/main/assets/licenses/upstream.md", names)
            self.assertNotIn(self.prefix + "/androidApp/src/main/kotlin/App.kt", names)
            self.assertNotIn(self.prefix + "/scripts/helper.py", names)

    def test_downloaded_readme_uses_commit_pinned_images_and_document_links(self):
        self.package()
        text = (self.output / "README.md").read_text()
        raw = f"https://raw.githubusercontent.com/owner/project/{self.revision}/docs/images/preview.png"
        self.assertIn(f"[![Preview]({raw})]({raw})", text)
        self.assertIn(f"https://github.com/owner/project/blob/{self.revision}/docs/README.md#start", text)
        self.assertEqual((self.output / "LICENSE").read_text(), self.files["LICENSE"])

    def test_missing_screenshot_stops_export_before_writing_an_archive(self):
        del self.files["docs/images/preview.png"]
        with self.assertRaisesRegex(ValueError, "missing docs/images/preview.png"):
            self.package()
        self.assertFalse(self.output.exists())

    def test_missing_linked_document_stops_export(self):
        self.files["docs/README.md"] += "\n[Missing](missing.md)"
        with self.assertRaisesRegex(ValueError, "missing missing.md"):
            self.package()

    def test_export_requires_both_languages_but_does_not_invent_a_license(self):
        del self.files["LICENSE"]
        self.package()
        self.assertFalse((self.output / "LICENSE").exists())
        del self.files["README.zh-CN.md"]
        with self.assertRaisesRegex(ValueError, "missing README.zh-CN.md"):
            self.package()

    def test_html_unicode_anchors_external_urls_and_code_examples(self):
        text = '''<img src="docs/截图.png"><a href="docs/guide.md#setup">Guide</a>
[External](https://example.com/file.png) [Jump](#preview) [Space](docs/file%20name.md)
```md
![Example](not-an-actual-image.png)
```
'''
        result = online_readme(text, "owner/project", self.revision)
        self.assertIn(f'https://raw.githubusercontent.com/owner/project/{self.revision}/docs/%E6%88%AA%E5%9B%BE.png', result)
        self.assertIn(f'https://github.com/owner/project/blob/{self.revision}/docs/file%20name.md', result)
        self.assertIn('[External](https://example.com/file.png) [Jump](#preview)', result)
        self.assertIn('![Example](not-an-actual-image.png)', result)


if __name__ == "__main__":
    unittest.main()
