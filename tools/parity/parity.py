#!/usr/bin/env python3
"""Parity gate: does a migrated transform reproduce BizTalk's recorded output?

Usage
-----
  parity.py list                       # maps with a recorded input+output pair
  parity.py show <map-id>              # print the input and expected output
  parity.py run <map-id> -- CMD...     # run CMD, compare stdout with expected
  parity.py run <map-id> --actual FILE # compare an already-produced file
  parity.py run --all                  # run every map with a runner registered
                                       # in tools/parity/runners.json

In CMD, `{input}` is replaced with the path to the recorded input message.
A migrated transform registers itself for `run --all` by adding

  { "<map-id>": ["java", "-jar", "target/x.jar", "--map", "MapPerson", "{input}"] }

to runners.json. Exit status is 0 on parity, 1 on any mismatch.

Comparison is on canonical XML: BOM and XML declaration dropped, the UTF-16
that BizTalk writes decoded, whitespace-only text nodes removed, and text
trimmed. Element order, attribute values and namespaces are significant —
BizTalk's XSLT is deterministic, so the migrated code must be too.
"""
from __future__ import annotations

import argparse
import difflib
import json
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
FIXTURES = HERE / "fixtures.json"
RUNNERS = HERE / "runners.json"


def read_xml_text(data: bytes) -> str:
    for bom, enc in ((b"\xff\xfe", "utf-16-le"), (b"\xfe\xff", "utf-16-be"), (b"\xef\xbb\xbf", "utf-8-sig")):
        if data.startswith(bom):
            return data.decode(enc).lstrip("\ufeff")
    if len(data) > 1 and data[1:2] == b"\x00":
        return data.decode("utf-16-le")
    return data.decode("utf-8")


def canonical(data: bytes) -> str:
    text = read_xml_text(data).strip()
    if text.startswith("<?xml"):
        text = text[text.index("?>") + 2:]
    root = ET.fromstring(text)
    for el in root.iter():
        if el.text is not None:
            el.text = el.text.strip() or None
        if el.tail is not None:
            el.tail = None
    return ET.canonicalize(ET.tostring(root, encoding="unicode"))


def pretty(canon: str) -> list[str]:
    root = ET.fromstring(canon)
    ET.indent(root)
    return ET.tostring(root, encoding="unicode").splitlines()


def load_maps() -> dict[str, dict]:
    if not FIXTURES.exists():
        sys.exit("fixtures.json missing; run tools/parity/inventory.py first")
    return {m["id"]: m for m in json.loads(FIXTURES.read_text())["maps"]}


def paired() -> dict[str, dict]:
    return {k: v for k, v in load_maps().items() if v["status"] == "paired"}


def compare(map_id: str, fixture: dict, actual: bytes) -> bool:
    expected = canonical((ROOT / fixture["expected"]).read_bytes())
    try:
        got = canonical(actual)
    except ET.ParseError as e:
        print(f"FAIL {map_id}: output is not well-formed XML ({e})")
        print(read_xml_text(actual)[:2000])
        return False
    if got == expected:
        print(f"PASS {map_id}")
        return True
    print(f"FAIL {map_id}: output differs from {fixture['expected']}")
    for line in difflib.unified_diff(pretty(expected), pretty(got), "biztalk-expected", "migrated-actual", lineterm=""):
        print("  " + line)
    return False


def run_cmd(cmd: list[str], fixture: dict) -> bytes:
    input_path = str(ROOT / fixture["input"])
    cmd = [c.replace("{input}", input_path) for c in cmd]
    proc = subprocess.run(cmd, capture_output=True, cwd=ROOT)
    if proc.returncode != 0:
        print(f"runner exited {proc.returncode}: {' '.join(cmd)}")
        print(proc.stderr.decode(errors="replace")[-2000:])
    return proc.stdout


def cmd_list(_: argparse.Namespace) -> int:
    runners = json.loads(RUNNERS.read_text()) if RUNNERS.exists() else {}
    for map_id, m in paired().items():
        flag = "runner" if map_id in runners else "no runner"
        print(f"{map_id}\n    input:    {m['input']}\n    expected: {m['expected']}\n    {flag}")
    return 0


def cmd_show(args: argparse.Namespace) -> int:
    m = paired().get(args.map_id) or sys.exit(f"unknown or unpaired map: {args.map_id}")
    print(f"# {m['btm']}\n\n## input: {m['input']}")
    print("\n".join(pretty(canonical((ROOT / m['input']).read_bytes()))))
    print(f"\n## expected: {m['expected']}")
    print("\n".join(pretty(canonical((ROOT / m['expected']).read_bytes()))))
    return 0


def cmd_run(args: argparse.Namespace) -> int:
    maps = paired()
    ok = True
    if args.all:
        runners = json.loads(RUNNERS.read_text()) if RUNNERS.exists() else {}
        if not runners:
            sys.exit("no runners registered in tools/parity/runners.json")
        for map_id, cmd in runners.items():
            m = maps.get(map_id) or sys.exit(f"runner registered for unknown/unpaired map {map_id}")
            ok &= compare(map_id, m, run_cmd(cmd, m))
        return 0 if ok else 1
    if not args.map_id:
        sys.exit("map-id required unless --all")
    m = maps.get(args.map_id) or sys.exit(f"unknown or unpaired map: {args.map_id}")
    if args.actual:
        actual = Path(args.actual).read_bytes()
    elif args.cmd:
        actual = run_cmd(args.cmd, m)
    else:
        sys.exit("give --actual FILE or -- CMD...")
    return 0 if compare(args.map_id, m, actual) else 1


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="command", required=True)
    sub.add_parser("list").set_defaults(fn=cmd_list)
    s = sub.add_parser("show"); s.add_argument("map_id"); s.set_defaults(fn=cmd_show)
    r = sub.add_parser("run")
    r.add_argument("map_id", nargs="?")
    r.add_argument("--all", action="store_true")
    r.add_argument("--actual")
    r.set_defaults(fn=cmd_run)
    argv = sys.argv[1:]
    cmd: list[str] = []
    if "--" in argv:
        cmd = argv[argv.index("--") + 1:]
        argv = argv[: argv.index("--")]
    args = p.parse_args(argv)
    args.cmd = cmd
    return args.fn(args)


if __name__ == "__main__":
    sys.exit(main())
