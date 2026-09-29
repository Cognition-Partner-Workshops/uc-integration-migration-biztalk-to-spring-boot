#!/usr/bin/env python3
"""Build the map fixture inventory for the BizTalk estate.

BizTalk has no Linux runtime, so the source of truth for a map's behavior is the
Test Map evidence the original developers left behind:

  * `<TestMapInputInstanceFilename>` in each `*.btproj.user` names the input
    instance a map was tested with (a Windows path; only the tail is useful).
  * `*_output.xml` files next to those inputs are the outputs BizTalk produced.

This script joins the two and writes `tools/parity/fixtures.json`:

    {
      "maps": [
        {
          "id": "working-with-maps/sorting-pattern/SortByMultipleFields.SortElementsDemo",
          "sample": "Working-with-Maps/Sorting-Pattern",
          "project": ".../BizTalkMapperSortingPattern.btproj",
          "btm": ".../SortByMultipleFields/SortElementsDemo.btm",
          "input": ".../TestMsg/Order_input.xml",
          "expected": ".../TestMsg/SortElementsDemo_output.xml",
          "status": "paired" | "input-only" | "unresolved"
        }
      ]
    }

Only `paired` entries are parity gates. The others are listed so nobody has to
rediscover which maps lack evidence.
"""
from __future__ import annotations

import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / "fixtures.json"
SKIP_DIRS = {"bin", "obj", "TestResults"}


def rel(p: Path) -> str:
    return p.relative_to(ROOT).as_posix()


def find_files(suffix: str) -> list[Path]:
    return sorted(
        p for p in ROOT.rglob(f"*{suffix}")
        if not (set(p.parts) & SKIP_DIRS)
    )


def sample_dir(project: Path) -> Path:
    """Top two path components (area / sample) of a project file."""
    parts = project.relative_to(ROOT).parts
    return ROOT.joinpath(*parts[:2])


def resolve_input(win_path: str, project: Path) -> Path | None:
    """Resolve a Windows Test Map path to a file under the sample directory."""
    if not win_path:
        return None
    tail = [seg for seg in re.split(r"[\\/]+", win_path) if seg]
    name = tail[-1]
    sdir = sample_dir(project)
    candidates = [p for p in sdir.rglob(name) if not (set(p.parts) & SKIP_DIRS)]
    if not candidates:
        candidates = [p for p in ROOT.rglob(name) if not (set(p.parts) & SKIP_DIRS)]
    if not candidates:
        return None
    # Prefer the candidate whose parent directory name matches the Windows path.
    if len(tail) >= 2:
        for c in candidates:
            if c.parent.name == tail[-2]:
                return c
    return candidates[0]


def expected_for(btm: Path, input_file: Path | None, project: Path) -> Path | None:
    """Locate the BizTalk-generated output for a map.

    BizTalk names Test Map output `<MapName>_output.xml`; some samples saved it
    as `<InputName>_output.xml` instead. Look in the input's directory first,
    then anywhere in the sample.
    """
    names = [f"{btm.stem}_output.xml"]
    if input_file is not None:
        names.append(f"{input_file.stem}_output.xml")
        names.append(f"{input_file.name}_output.xml")
    search_dirs = []
    if input_file is not None:
        search_dirs.append(input_file.parent)
    search_dirs.append(sample_dir(project))
    for d in search_dirs:
        for n in names:
            hits = [p for p in d.rglob(n) if not (set(p.parts) & SKIP_DIRS)]
            if hits:
                return sorted(hits)[0]
    return None


def parse_user_file(user_file: Path) -> dict[str, str]:
    """Map relative `.btm` path -> TestMapInputInstanceFilename."""
    text = user_file.read_text(encoding="utf-8-sig", errors="replace")
    try:
        root = ET.fromstring(text)
    except ET.ParseError:
        return {}
    ns = {"m": "http://schemas.microsoft.com/developer/msbuild/2003"}
    out: dict[str, str] = {}
    for f in root.iter("{http://schemas.microsoft.com/developer/msbuild/2003}File"):
        path = f.get("Path", "")
        if not path.lower().endswith(".btm"):
            continue
        el = f.find("m:TestMapInputInstanceFilename", ns)
        out[path.replace("\\", "/")] = (el.text or "").strip() if el is not None else ""
    return out


def main() -> int:
    maps = []
    for project in find_files(".btproj"):
        user_file = project.with_suffix(".btproj.user")
        test_inputs = parse_user_file(user_file) if user_file.exists() else {}
        for btm in sorted(project.parent.rglob("*.btm")):
            if set(btm.parts) & SKIP_DIRS:
                continue
            rel_btm = btm.relative_to(project.parent).as_posix()
            win_input = test_inputs.get(rel_btm, "")
            input_file = resolve_input(win_input, project)
            if input_file is not None and input_file.name.endswith("_output.xml"):
                input_file = None
            expected = expected_for(btm, input_file, project)
            if input_file is None and expected is None:
                status = "unresolved"
            elif input_file is not None and expected is not None:
                status = "paired"
            else:
                status = "input-only" if input_file is not None else "expected-only"
            sample = sample_dir(project)
            map_id = f"{rel(sample).lower()}/{rel_btm[:-4].replace('/', '.')}"
            maps.append({
                "id": map_id,
                "sample": rel(sample),
                "project": rel(project),
                "btm": rel(btm),
                "input": rel(input_file) if input_file else None,
                "expected": rel(expected) if expected else None,
                "status": status,
            })

    maps.sort(key=lambda m: m["id"])
    OUT.write_text(json.dumps({"maps": maps}, indent=2) + "\n", encoding="utf-8")
    counts: dict[str, int] = {}
    for m in maps:
        counts[m["status"]] = counts.get(m["status"], 0) + 1
    print(f"wrote {rel(OUT)}: {len(maps)} maps", ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
