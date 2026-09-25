#!/usr/bin/env python3
"""Check local documentation targets and translation parity without dependencies."""
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlsplit
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def main():
    errors = []
    resources = ROOT / "shared/src/commonMain/composeResources"
    source = None
    for path in [resources / "values/strings.xml", *sorted(resources.glob("values-*/strings.xml"))]:
        entries = ET.parse(path).getroot().findall("string")
        strings = {entry.attrib["name"]: "".join(entry.itertext()) for entry in entries}
        if len(strings) != len(entries):
            errors.append(f"{path.relative_to(ROOT)}: duplicate resource keys")
        if source is None:
            source = strings
            continue
        if strings.keys() != source.keys():
            errors.append(f"{path.relative_to(ROOT)}: missing {source.keys() - strings.keys()}, extra {strings.keys() - source.keys()}")
        for key in strings.keys() & source.keys():
            placeholders = lambda value: sorted(re.findall(r"%\d+\$[ds]", value))
            if placeholders(strings[key]) != placeholders(source[key]):
                errors.append(f"{path.relative_to(ROOT)}: {key} has mismatched placeholders")
    files = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=ROOT
    ).decode().split("\0")
    for name in set(files):
        if not name.endswith(".md"):
            continue
        # Vendored upstream notices can link to files in their original source tree.
        if name.startswith("androidApp/src/main/assets/licenses/"):
            continue
        path = ROOT / name
        if not path.is_file():
            continue
        text = re.sub(r"```.*?```", "", path.read_text(), flags=re.S)
        targets = re.findall(r"\[[^\]]*\]\(([^)\s]+)(?:\s+[^)]*)?\)", text)
        targets += re.findall(r'(?:src|href)="([^"]+)"', text)
        for target in targets:
            url = urlsplit(target)
            if url.scheme or url.netloc or not url.path:
                continue
            if not (path.parent / unquote(url.path)).exists():
                errors.append(f"{name}: missing local target {target}")
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"Documentation targets and {len(source)} translation keys verified.")


if __name__ == "__main__":
    main()
