#!/usr/bin/env python3
"""Prototype tooling for rebuilding zh HTML documentation from English source."""

from __future__ import annotations

import argparse
import ast
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import asdict, dataclass
from html.parser import HTMLParser
from pathlib import Path
from typing import Iterable
from urllib.parse import unquote


REPO_ROOT = Path(__file__).resolve().parents[2]
EN_ROOT = REPO_ROOT / "src/main/resources/doc/en/html"
ZH_ROOT = REPO_ROOT / "src/main/resources/doc/zh/html"
DEFAULT_WORK = REPO_ROOT / "build/zh-docs"
DEFAULT_SEGMENTS = DEFAULT_WORK / "segments.jsonl"
DEFAULT_TRANSLATIONS = DEFAULT_WORK / "translations.jsonl"
DEFAULT_PREVIEW = DEFAULT_WORK / "preview/html"
DEFAULT_POT_ROOT = DEFAULT_WORK / "pot"
DEFAULT_PO_ROOT = DEFAULT_WORK / "po"
DEFAULT_PO_TRANSLATED_ROOT = DEFAULT_WORK / "po-translated"
DEFAULT_PO_PREVIEW = DEFAULT_WORK / "po-preview/html"
DEFAULT_PO_QA_ROOT = DEFAULT_WORK / "po-qa"
DEFAULT_CLEAN_ROOT = DEFAULT_WORK / "source-clean/html"
SKIP_TEXT_TAGS = {"script", "style", "pre", "code", "kbd", "samp"}
TEXT_RE = re.compile(r"[A-Za-z][A-Za-z0-9'/-]*")
COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)
TAG_RE = re.compile(r"<[^>]+>")
TABLE_RE = re.compile(r"<table\b[^>]*>.*?</table\s*>", re.IGNORECASE | re.DOTALL)
CODE_CONTENT_RE = re.compile(
    r"<(pre|code|kbd|samp|tt)\b[^>]*>.*?</\1\s*>",
    re.IGNORECASE | re.DOTALL,
)
RATIO_CODE_CONTENT_RE = re.compile(
    r"<(pre|code|kbd|samp|tt|var)\b[^>]*>.*?</\1\s*>",
    re.IGNORECASE | re.DOTALL,
)
OPEN_TAG_RE = re.compile(r"</?[^>]+>")
NUMBER_RE = re.compile(r"(?<![A-Za-z])\d+(?:\.\d+)*")
SHORTCUT_RE = re.compile(r"\b(?:Ctrl|ALT|Alt|Shift|Meta)-(?!Mouse\b)[A-Za-z0-9]+(?:\s*/\s*[A-Za-z0-9]+)?\b")
DANGEROUS_TAG_RE = re.compile(r"<\s*(script|iframe|object|embed)\b", re.IGNORECASE)
DANGEROUS_ATTR_RE = re.compile(r"\s(on[a-z]+)\s*=", re.IGNORECASE)
DANGEROUS_URL_RE = re.compile(r"^(?:javascript|vbscript|data):", re.IGNORECASE)
DEFAULT_ALLOWED_ENGLISH_TERMS = (
    "Logisim-evolution",
    "logisim-evolution",
    "Logisim",
    "FPGA Commander",
    "Questa Advanced Simulator",
    "QuestaSim",
    "HDL IP",
    "VHDL",
    "Verilog",
    "Altera/Intel",
    "Quartus Prime",
    "Xilinx ISE",
    "Xilinx Vivado",
    "OpenFPGA ECP5",
    "DeepL",
    "Beta",
    "MacOS",
    "MyNiceCircuit",
    "MyToplevel",
    "PgUp",
    "PgDn",
    "Delete",
    "Backspace",
    "Insert",
    "MenuKey-D",
    "Java",
    "AddTool",
    "CounterData",
    "CounterPoker",
    "GrayCounter",
    "GrayIncrementer",
    "Instance",
    "InstanceFactory",
    "InstancePainter",
    "InstanceState",
    "Library",
    "Library-Class",
    "SimpleCounter",
    "SimpleGrayCounter",
    "Nios2",
    "Nios2s",
    "nios2",
    "Intel",
    "gcc",
    "elf",
    "estatus",
    "bstatus",
    "ienable",
    "ipending",
    "Carl Burch",
    "Hendrix College",
    "Moshe Berman",
    "Brooklyn College",
    "Theldo Cruz Franqueira",
    "Pontifícia Universidade Católica de Minas Gerais",
    "David H. Hutchens",
    "Millersville University",
    "Berner Fachhochschule",
    "Haute école spécialisée bernoise",
    "Theo Kluter",
    "Torsten Maehne",
    "Tom Niget",
    "Polytech Nice-Sophia",
    "Marcin Orłowski",
    "Kevin Walsh",
    "manifest",
)
DEFAULT_ALLOWED_ENGLISH_PATTERNS = (
    r"https?://\S+",
    r"\b[A-Za-z0-9.-]+\.[A-Za-z]{2,}(?:/[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]*)?",
    r"\b(?:Ctrl|Shift|Alt|Meta)(?:-[A-Za-z0-9]+)+\b",
    r"\bCtrl[+-](?:Up|Down|Left|Right|[A-Za-z0-9]+)\b",
    r"\bDel\s*/\s*Delete\b",
    r"\b[A-Z]{2,}\b",
    r"\b[A-Z]+(?:-[A-Z]+)+\b",
    r"\b[A-Za-z]+_[A-Za-z0-9_]+\b",
    r"\b[A-Z](?:/[A-Z0-9-]+)+\b",
    r"\b[xyze]\b",
)
DEFAULT_PO_FILTER_TESTS = (
    "blank",
    "emails",
    "escapes",
    "filepaths",
    "functions",
    "newlines",
    "numbers",
    "options",
    "printf",
    "pythonbraceformat",
    "tabs",
    "untranslated",
    "urls",
    "variables",
)
DEFAULT_LINT_EXCLUDES = (
    "libs/_modelempty",
)
DEFAULT_REQUIRED_TERM_PAIRS = (
    (r"\bPin\b|\bpins?\b", "引脚", "pin"),
    (r"\bWiring\b", "接线", "wiring"),
    (r"\bwires?\b|\bWires?\b", "导线", "wire"),
    (r"\bPoke Tool\b|\bpoke\b|toolpoke\.png", "手形", "poke tool"),
    (r"\bcomponents?\b|\bComponents?\b", "组件", "component"),
    (r"\bSplitters?\b|\bsplitters?\b", "分线器", "splitter"),
    (r"\bData Bits\b", "数据位宽", "data bits"),
    (r"\bbit width\b", "位宽", "bit width"),
    (r"\bFan Out\b", "扇出", "fan out"),
    (r"\bSuite\b", "下一节", "suite navigation"),
)
SHORTCUT_EQUIVALENTS = {
    "Shift-clicking": (
        r"Shift\s*(?:键)?\s*(?:并)?\s*(?:单击|点击)",
        r"按住\s*Shift\s*键并(?:在[^，。；]*内)?(?:单击|点击)",
        r"Shift[+-](?:单击|点击)",
    ),
    "Shift-dragging": (
        r"Shift\s*(?:键)?\s*(?:并)?\s*拖动",
        r"按住\s*Shift\s*键并[^，。；]*拖动",
        r"Shift[+-]拖动",
    ),
}


@dataclass
class Segment:
    id: str
    source_path: str
    target_path: str
    index: int
    source_text: str
    source_hash: str


@dataclass
class Translation:
    id: str
    source_path: str
    source_text: str
    target_text: str
    provider: str
    model: str | None = None


@dataclass
class PoEntry:
    comments: list[str]
    msgctxt: str | None
    msgid: str
    msgstr: str


def repo_path(path: Path) -> str:
    resolved = path if path.is_absolute() else REPO_ROOT / path
    try:
        return resolved.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(path)


def source_rel(path: Path) -> str:
    return path.relative_to(EN_ROOT).as_posix()


def normalize_text(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def text_hash(text: str) -> str:
    return hashlib.sha1(normalize_text(text).encode("utf-8")).hexdigest()


def make_po_id(po_rel: str, msgctxt: str | None, msgid: str) -> str:
    raw = f"{po_rel}\0{msgctxt or ''}\0{msgid}".encode("utf-8")
    return hashlib.sha1(raw).hexdigest()[:16]


def make_id(rel_path: str, index: int, text: str) -> str:
    raw = f"{rel_path}\0{index}\0{normalize_text(text)}".encode("utf-8")
    return hashlib.sha1(raw).hexdigest()[:16]


def split_ws(text: str) -> tuple[str, str, str]:
    match = re.match(r"^(\s*)(.*?)(\s*)$", text, re.DOTALL)
    if not match:
        return "", text, ""
    return match.group(1), match.group(2), match.group(3)


def should_translate(text: str, tag_stack: list[str]) -> bool:
    if any(tag in SKIP_TEXT_TAGS for tag in tag_stack):
        return False
    normalized = normalize_text(text)
    if not normalized:
        return False
    if not TEXT_RE.search(normalized):
        return False
    if normalized.startswith(("http://", "https://", "mailto:")):
        return False
    return True


def looks_like_code_block(text: str) -> bool:
    stripped = text.strip()
    if not stripped:
        return False
    if stripped.startswith(("package ", "import ")):
        return True
    lines = [line for line in stripped.splitlines() if line.strip()]
    if len(lines) < 3:
        return False
    code_markers = 0
    for line in lines:
        compact = line.strip()
        if re.match(r"(package|import)\s+[\w.]+;", compact):
            code_markers += 1
        elif re.match(r"(public|private|protected|class|interface|enum|static|final)\b", compact):
            code_markers += 1
        elif compact.startswith(("//", "/*", "*", "*/")):
            code_markers += 1
        elif re.search(r"[{};]\s*$", compact):
            code_markers += 1
        elif re.search(r"\b[A-Za-z_][A-Za-z0-9_]*\s*\([^)]*\)\s*(?:\{|;)?$", compact):
            code_markers += 1
    return code_markers >= max(3, len(lines) // 3)


class SegmentingParser(HTMLParser):
    def __init__(
        self,
        rel_path: str,
        translations: dict[str, str] | None = None,
        collect: bool = True,
        keep_comments: bool = False,
    ) -> None:
        super().__init__(convert_charrefs=False)
        self.rel_path = rel_path
        self.translations = translations or {}
        self.collect = collect
        self.keep_comments = keep_comments
        self.events: list[str] = []
        self.segments: list[Segment] = []
        self.tag_stack: list[str] = []
        self.segment_index = 0

    def handle_decl(self, decl: str) -> None:
        self.events.append(f"<!{decl}>")

    def handle_pi(self, data: str) -> None:
        self.events.append(f"<?{data}>")

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.events.append(self.get_starttag_text() or self._format_starttag(tag, attrs))
        self.tag_stack.append(tag.lower())

    def handle_startendtag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.events.append(self.get_starttag_text() or self._format_starttag(tag, attrs, closed=True))

    def handle_endtag(self, tag: str) -> None:
        self.events.append(f"</{tag}>")
        tag = tag.lower()
        for i in range(len(self.tag_stack) - 1, -1, -1):
            if self.tag_stack[i] == tag:
                del self.tag_stack[i:]
                break

    def handle_data(self, data: str) -> None:
        if not should_translate(data, self.tag_stack):
            self.events.append(data)
            return

        self.segment_index += 1
        segment_id = make_id(self.rel_path, self.segment_index, data)
        normalized = normalize_text(data)
        segment = Segment(
            id=segment_id,
            source_path=self.rel_path,
            target_path=self.rel_path,
            index=self.segment_index,
            source_text=normalized,
            source_hash=text_hash(data),
        )
        if self.collect:
            self.segments.append(segment)
            self.events.append(data)
            return

        target = self.translations.get(segment_id)
        if target:
            left, _middle, right = split_ws(data)
            self.events.append(f"{left}{target}{right}")
        else:
            self.events.append(data)

    def handle_entityref(self, name: str) -> None:
        self.events.append(f"&{name};")

    def handle_charref(self, name: str) -> None:
        self.events.append(f"&#{name};")

    def handle_comment(self, data: str) -> None:
        if self.keep_comments:
            self.events.append(f"<!--{data}-->")

    def unknown_decl(self, data: str) -> None:
        self.events.append(f"<![{data}]>")

    def render(self) -> str:
        html = "".join(self.events)
        html = re.sub(r'<html([^>]*)\blang="en"([^>]*)>', r'<html\1lang="zh"\2>', html, count=1)
        html = re.sub(r'<html([^>]*)\blang=en([^>]*)>', r'<html\1lang="zh"\2>', html, count=1)
        html = re.sub(
            r'(<meta[^>]+http-equiv=["\']Content-Language["\'][^>]+content=["\'])en(["\'][^>]*>)',
            r"\1zh\2",
            html,
            flags=re.IGNORECASE,
        )
        html = re.sub(
            r'(<META[^>]+http-equiv=["\']Content-Language["\'][^>]+content=["\'])en(["\'][^>]*>)',
            r"\1zh\2",
            html,
            flags=re.IGNORECASE,
        )
        return html

    @staticmethod
    def _format_starttag(tag: str, attrs: list[tuple[str, str | None]], closed: bool = False) -> str:
        parts = [tag]
        for key, value in attrs:
            if value is None:
                parts.append(key)
            else:
                escaped = value.replace('"', "&quot;")
                parts.append(f'{key}="{escaped}"')
        close = " /" if closed else ""
        return f"<{' '.join(parts)}{close}>"


def iter_html_files(all_files: bool, files: list[str] | None) -> list[Path]:
    if files:
        return [EN_ROOT / f for f in files]
    if all_files:
        return sorted(EN_ROOT.rglob("*.html"))
    sample_path = Path(__file__).with_name("sample_targets.txt")
    return [EN_ROOT / line.strip() for line in sample_path.read_text(encoding="utf-8").splitlines() if line.strip()]


def extract_file(path: Path) -> list[Segment]:
    rel = source_rel(path)
    parser = SegmentingParser(rel_path=rel, collect=True)
    parser.feed(path.read_text(encoding="utf-8"))
    parser.close()
    return parser.segments


def write_jsonl(path: Path, rows: Iterable[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True))
            handle.write("\n")


def read_jsonl(path: Path) -> list[dict]:
    if not path.exists():
        return []
    rows = []
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def po_literal(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def parse_po_literal(line: str) -> str:
    return ast.literal_eval(line.strip())


def parse_po(path: Path) -> list[PoEntry]:
    entries: list[PoEntry] = []
    comments: list[str] = []
    msgctxt: str | None = None
    msgid: str | None = None
    msgstr: str | None = None
    current: str | None = None

    def flush() -> None:
        nonlocal comments, msgctxt, msgid, msgstr, current
        if msgid is not None:
            entries.append(PoEntry(comments=comments, msgctxt=msgctxt, msgid=msgid, msgstr=msgstr or ""))
        comments = []
        msgctxt = None
        msgid = None
        msgstr = None
        current = None

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.rstrip("\n")
        if not line:
            flush()
            continue
        if line.startswith("#"):
            comments.append(line)
            continue
        if line.startswith("msgctxt "):
            current = "msgctxt"
            msgctxt = parse_po_literal(line[len("msgctxt ") :])
            continue
        if line.startswith("msgid "):
            current = "msgid"
            msgid = parse_po_literal(line[len("msgid ") :])
            continue
        if line.startswith("msgstr "):
            current = "msgstr"
            msgstr = parse_po_literal(line[len("msgstr ") :])
            continue
        if line.lstrip().startswith('"') and current:
            value = parse_po_literal(line)
            if current == "msgctxt":
                msgctxt = (msgctxt or "") + value
            elif current == "msgid":
                msgid = (msgid or "") + value
            elif current == "msgstr":
                msgstr = (msgstr or "") + value
            continue
        raise ValueError(f"Unsupported PO line in {repo_path(path)}: {line}")
    flush()
    return entries


def write_po(path: Path, entries: list[PoEntry]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    parts: list[str] = []
    for entry in entries:
        parts.extend(entry.comments)
        if entry.msgctxt is not None:
            parts.append(f"msgctxt {po_literal(entry.msgctxt)}")
        parts.append(f"msgid {po_literal(entry.msgid)}")
        parts.append(f"msgstr {po_literal(entry.msgstr)}")
        parts.append("")
    path.write_text("\n".join(parts), encoding="utf-8", newline="\n")


def should_translate_po_entry(entry: PoEntry) -> bool:
    if not entry.msgid:
        return False
    if entry.msgid in {"en", "#########"}:
        return False
    if looks_like_code_block(entry.msgid):
        return False
    if not TEXT_RE.search(TAG_RE.sub(" ", entry.msgid)):
        return False
    return True


def po_entry_inside_code_content(entry: PoEntry, source_html: str | None) -> bool:
    if not source_html:
        return False
    normalized_msgid = normalize_text(TAG_RE.sub(" ", entry.msgid))
    if not normalized_msgid:
        return False
    for match in CODE_CONTENT_RE.finditer(source_html):
        normalized_block = normalize_text(TAG_RE.sub(" ", match.group(0)))
        if normalized_msgid and normalized_msgid in normalized_block:
            return True
    return False


def tool_executable(name: str) -> Path:
    explicit_dir = os.environ.get("TRANSLATE_TOOLKIT_BIN")
    candidates: list[Path] = []
    if explicit_dir:
        candidates.append(Path(explicit_dir) / name)
        candidates.append(Path(explicit_dir) / f"{name}.exe")
    found = shutil.which(name)
    if found:
        candidates.append(Path(found))
    candidates.append(DEFAULT_WORK / "vendor/bin" / f"{name}.exe")
    candidates.append(DEFAULT_WORK / "vendor/bin" / name)
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise SystemExit(
        f"{name} was not found. Install Translate Toolkit, for example:\n"
        f"  python -m pip install --target {repo_path(DEFAULT_WORK / 'vendor')} translate-toolkit"
    )


def run_translate_tool(name: str, args: list[str]) -> None:
    executable = tool_executable(name)
    env = os.environ.copy()
    vendor = DEFAULT_WORK / "vendor"
    if vendor.exists():
        env["PYTHONPATH"] = str(vendor) + os.pathsep + env.get("PYTHONPATH", "")
    completed = subprocess.run([str(executable), *args], cwd=REPO_ROOT, env=env, text=True, capture_output=True)
    if completed.returncode != 0:
        if completed.stdout:
            print(completed.stdout, file=sys.stderr)
        if completed.stderr:
            print(completed.stderr, file=sys.stderr)
        raise SystemExit(completed.returncode)


def is_relative_to(path: Path, parent: Path) -> bool:
    try:
        path.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def clean_build_output(path: Path) -> None:
    if not path.exists():
        return
    if not is_relative_to(path, DEFAULT_WORK):
        raise SystemExit(f"Refusing to clean output outside {repo_path(DEFAULT_WORK)}: {repo_path(path)}")
    if path.resolve() == DEFAULT_WORK.resolve():
        raise SystemExit(f"Refusing to clean the whole work root: {repo_path(path)}")
    shutil.rmtree(path)


def sanitize_html(source: Path, output: Path) -> None:
    vendor = DEFAULT_WORK / "vendor"
    if vendor.exists() and str(vendor) not in sys.path:
        sys.path.insert(0, str(vendor))
    try:
        from lxml import html
    except ImportError as exc:
        raise SystemExit(
            "HTML sanitizing requires lxml, which is installed with Translate Toolkit. "
            "Install it with: python -m pip install translate-toolkit"
        ) from exc
    raw = COMMENT_RE.sub("", source.read_text(encoding="utf-8"))
    document = html.fromstring(raw)
    rendered = html.tostring(
        document,
        encoding="unicode",
        method="html",
        pretty_print=True,
        doctype='<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">',
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(rendered, encoding="utf-8", newline="\n")


def load_glossary(path: Path) -> dict[str, str]:
    terms: dict[str, str] = {}
    if not path.exists():
        return terms
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.lstrip().startswith("#") or line.strip() == "terms:":
            continue
        match = re.match(r"^\s{2,}([^:#][^:]*):\s*(.+?)\s*$", line)
        if match:
            terms[match.group(1).strip().strip('"')] = match.group(2).strip().strip('"')
    return terms


def apply_glossary_pseudo(text: str, glossary: dict[str, str]) -> str:
    result = text
    for source, target in sorted(glossary.items(), key=lambda item: len(item[0]), reverse=True):
        if re.search(r"[A-Za-z]", source):
            result = re.sub(rf"\b{re.escape(source)}\b", target, result)
    return result


def estimate_visible_text(path: Path) -> str:
    raw = path.read_text(encoding="utf-8")
    clean = COMMENT_RE.sub(" ", raw)
    clean = re.sub(r"(?is)<script\b.*?</script>|<style\b.*?</style>", " ", clean)
    text = TAG_RE.sub(" ", clean)
    return normalize_text(text.replace("&nbsp;", " "))


def command_inventory(args: argparse.Namespace) -> int:
    files = sorted(EN_ROOT.rglob("*.html"))
    rows = []
    total_chars = 0
    for path in files:
        chars = len(estimate_visible_text(path))
        total_chars += chars
        rows.append((chars, source_rel(path)))
    print(f"files={len(files)}")
    print(f"visible_text_chars={total_chars}")
    print(f"approx_english_tokens={round(total_chars / 4)}")
    print("\nLargest pages:")
    for chars, rel in sorted(rows, reverse=True)[: args.top]:
        print(f"{round(chars / 4):>6} tokens  {rel}")
    return 0


def command_extract(args: argparse.Namespace) -> int:
    paths = iter_html_files(args.all, args.files)
    missing = [repo_path(path) for path in paths if not path.exists()]
    if missing:
        raise SystemExit(f"Missing source files: {', '.join(missing)}")
    segments: list[Segment] = []
    for path in paths:
        segments.extend(extract_file(path))
    write_jsonl(args.output, (asdict(segment) for segment in segments))
    print(f"extracted_segments={len(segments)}")
    print(f"output={repo_path(args.output)}")
    return 0


def command_po_extract(args: argparse.Namespace) -> int:
    paths = iter_html_files(args.all, args.files)
    missing = [repo_path(path) for path in paths if not path.exists()]
    if missing:
        raise SystemExit(f"Missing source files: {', '.join(missing)}")
    output_root = DEFAULT_POT_ROOT if args.pot and args.output_root == DEFAULT_PO_ROOT else args.output_root
    count = 0
    for source in paths:
        rel = Path(source_rel(source))
        tool_input = source
        if not args.no_sanitize:
            tool_input = args.clean_root / rel
            sanitize_html(source, tool_input)
        output = output_root / rel.with_suffix(".pot" if args.pot else ".po")
        output.parent.mkdir(parents=True, exist_ok=True)
        tool_args = [
            "--progress=none",
            "--duplicates=msgctxt",
            "-i",
            str(tool_input),
            "-o",
            str(output),
        ]
        if args.pot:
            tool_args.insert(1, "--pot")
        run_translate_tool(
            "html2po",
            tool_args,
        )
        count += 1
    print(f"{'pot' if args.pot else 'po'}_files={count}")
    print(f"output_root={repo_path(output_root)}")
    return 0


def command_po_to_jsonl(args: argparse.Namespace) -> int:
    rows = []
    for po_file in sorted(args.input_root.rglob("*.po")):
        po_rel = po_file.relative_to(args.input_root).as_posix()
        source_path = str(Path(po_rel).with_suffix(".html")).replace("\\", "/")
        template = args.template_root / source_path if args.template_root else None
        source_html = template.read_text(encoding="utf-8") if template and template.exists() else None
        for entry in parse_po(po_file):
            if not should_translate_po_entry(entry):
                continue
            if po_entry_inside_code_content(entry, source_html):
                continue
            rows.append(
                {
                    "id": make_po_id(po_rel, entry.msgctxt, entry.msgid),
                    "source_path": source_path,
                    "target_path": source_path,
                    "po_path": po_rel,
                    "msgctxt": entry.msgctxt,
                    "source_text": entry.msgid,
                    "source_hash": text_hash(entry.msgid),
                }
            )
    write_jsonl(args.output, rows)
    print(f"po_segments={len(rows)}")
    print(f"output={repo_path(args.output)}")
    return 0


def command_po_update(args: argparse.Namespace) -> int:
    command = [
        "--progress=none",
        "-i",
        str(args.input_root),
        "-o",
        str(args.output_root),
    ]
    if args.template_root:
        command.extend(["-t", str(args.template_root)])
    run_translate_tool("pot2po", command)
    files = sorted(args.output_root.rglob("*.po")) if args.output_root.exists() else []
    print(f"po_files={len(files)}")
    print(f"output_root={repo_path(args.output_root)}")
    return 0


def command_po_apply(args: argparse.Namespace) -> int:
    translations = {row["id"]: row["target_text"] for row in read_jsonl(args.translations)}
    written = 0
    for po_file in sorted(args.input_root.rglob("*.po")):
        po_rel = po_file.relative_to(args.input_root).as_posix()
        entries = parse_po(po_file)
        for entry in entries:
            entry_id = make_po_id(po_rel, entry.msgctxt, entry.msgid)
            if should_translate_po_entry(entry) and entry_id in translations:
                entry.msgstr = translations[entry_id]
            elif entry.msgid == "en":
                entry.msgstr = "zh"
            elif entry.msgid == "#########" or (entry.msgid and not should_translate_po_entry(entry)):
                entry.msgstr = entry.msgid
        output = args.output_root / po_file.relative_to(args.input_root)
        write_po(output, entries)
        written += 1
    print(f"translated_po_files={written}")
    print(f"output_root={repo_path(args.output_root)}")
    return 0


def command_po_filter(args: argparse.Namespace) -> int:
    tests = [] if args.all_tests else args.test or list(DEFAULT_PO_FILTER_TESTS)
    if not args.no_clean_output:
        clean_build_output(args.output_root)
    command = [
        "--progress=none",
        f"--language={args.language}",
    ]
    for test in tests:
        command.extend(["--test", test])
    for excluded in args.exclude_filter:
        command.extend(["--excludefilter", excluded])
    command.extend([str(args.input_root), str(args.output_root)])
    run_translate_tool("pofilter", command)
    failures = sorted(args.output_root.rglob("*.po")) if args.output_root.exists() else []
    print(f"qa_files={len(failures)}")
    print(f"output_root={repo_path(args.output_root)}")
    if args.fail_on_qa and failures:
        return 1
    return 0


def command_po_render(args: argparse.Namespace) -> int:
    rendered = 0
    for po_file in sorted(args.input_root.rglob("*.po")):
        rel = po_file.relative_to(args.input_root).with_suffix(".html")
        source = args.template_root / rel if (args.template_root / rel).exists() else EN_ROOT / rel
        if not source.exists():
            print(f"skip_missing_template={repo_path(source)}", file=sys.stderr)
            continue
        output = args.output_root / rel
        output.parent.mkdir(parents=True, exist_ok=True)
        run_translate_tool(
            "po2html",
            [
                "--progress=none",
                "--fuzzy",
                "-i",
                str(po_file),
                "-t",
                str(source),
                "-o",
                str(output),
            ],
        )
        html = output.read_text(encoding="utf-8")
        html = COMMENT_RE.sub("", html)
        html = re.sub(r'<html([^>]*)\blang="en"([^>]*)>', r'<html\1lang="zh"\2>', html, count=1)
        html = re.sub(r'<html([^>]*)\blang=en([^>]*)>', r'<html\1lang="zh"\2>', html, count=1)
        html = re.sub(
            r'(<meta[^>]+http-equiv=["\']Content-Language["\'][^>]+content=["\'])en(["\'][^>]*>)',
            r"\1zh\2",
            html,
            flags=re.IGNORECASE,
        )
        html = preserve_code_content(html, source.read_text(encoding="utf-8"))
        output.write_text(html, encoding="utf-8", newline="\n")
        rendered += 1
    print(f"rendered_files={rendered}")
    print(f"output_root={repo_path(args.output_root)}")
    return 0


def build_prompt(batch: list[dict], glossary: dict[str, str]) -> str:
    glossary_text = "\n".join(f"- {source} => {target}" for source, target in glossary.items())
    payload = [{"id": row["id"], "source": protect_html_tags(row["source_text"])[0]} for row in batch]
    return (
        "Translate the following Logisim-evolution HTML documentation fragments from English to Simplified Chinese.\n"
        "Requirements:\n"
        "- Return valid JSON only, with shape {\"translations\":[{\"id\":\"...\",\"target\":\"...\"}]}.\n"
        "- Preserve every digit sequence and keyboard shortcut exactly: 12 must remain 12, Alt-2 must remain Alt-2, and Ctrl-5 / 6 must remain Ctrl-5 / 6.\n"
        "- Preserve file names, code identifiers, and product names unless a glossary term says otherwise.\n"
        "- HTML tags have been replaced by placeholders like ⟦TAG0⟧. Preserve every placeholder exactly once, and do not translate placeholders.\n"
        "- Keep placeholder order when it reads naturally. You may reorder inline placeholders when needed for correct Chinese grammar or to fix awkward inherited structure, but never drop, duplicate, or modify them.\n"
        "- Preserve each placeholder's semantic scope: linked text should remain linked, emphasized labels should remain emphasized, and placeholders must not attach to the wrong noun phrase.\n"
        "- Translate only the human-readable text around placeholders.\n"
        "- Use natural technical Chinese suitable for EDA/digital logic documentation.\n"
        "- Prefer neutral, instruction-style Chinese. Avoid 你; use 您 only when a direct second-person pronoun is necessary.\n"
        "- Use full-width Chinese punctuation in translated prose.\n"
        "- Translate user-interface labels and attribute names according to the glossary and existing zh UI strings.\n"
        "- For navigation text, translate Next as 下一节 and Back as 返回.\n"
        "- Do not add explanations.\n"
        "- Apply this glossary exactly where relevant:\n"
        f"{glossary_text}\n\n"
        f"Segments:\n{json.dumps(payload, ensure_ascii=False)}"
    )


def protect_html_tags(text: str) -> tuple[str, list[str]]:
    tags: list[str] = []

    def replace(match: re.Match[str]) -> str:
        tags.append(match.group(0))
        return f"⟦TAG{len(tags) - 1}⟧"

    return OPEN_TAG_RE.sub(replace, text), tags


def restore_html_tags(text: str, tags: list[str]) -> str:
    restored = text
    for index, tag in enumerate(tags):
        restored = restored.replace(f"⟦TAG{index}⟧", tag)
    return restored


def call_deepseek(batch: list[dict], glossary: dict[str, str], args: argparse.Namespace) -> list[Translation]:
    api_key = os.environ.get("DEEPSEEK_API_KEY")
    if not api_key:
        raise SystemExit("DEEPSEEK_API_KEY is not set")
    base_url = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    request_body = {
        "model": args.model,
        "messages": [
            {"role": "system", "content": "You are a careful Simplified Chinese technical documentation translator."},
            {"role": "user", "content": build_prompt(batch, glossary)},
        ],
        "response_format": {"type": "json_object"},
    }
    if args.thinking != "enabled":
        request_body["temperature"] = args.temperature
    if args.thinking != "default":
        request_body["thinking"] = {"type": args.thinking}
    if args.thinking == "enabled":
        request_body["reasoning_effort"] = args.reasoning_effort
    request = urllib.request.Request(
        f"{base_url}/chat/completions",
        data=json.dumps(request_body).encode("utf-8"),
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST",
    )
    for attempt in range(args.retries + 1):
        try:
            with urllib.request.urlopen(request, timeout=args.timeout) as response:
                data = json.loads(response.read().decode("utf-8"))
            content = data["choices"][0]["message"]["content"]
            parsed = json.loads(content)
            by_id = {item["id"]: item["target"] for item in parsed["translations"]}
            translations = []
            for row in batch:
                _protected, tags = protect_html_tags(row["source_text"])
                target = restore_html_tags(by_id[row["id"]], tags)
                translations.append(
                    Translation(
                        id=row["id"],
                        source_path=row["source_path"],
                        source_text=row["source_text"],
                        target_text=target,
                        provider="deepseek",
                        model=args.model,
                    )
                )
            return translations
        except (urllib.error.URLError, urllib.error.HTTPError, KeyError, json.JSONDecodeError) as exc:
            if attempt >= args.retries:
                raise RuntimeError(f"DeepSeek batch failed after {attempt + 1} attempts: {exc}") from exc
            time.sleep(2**attempt)
    raise AssertionError("unreachable")


def command_translate(args: argparse.Namespace) -> int:
    rows = read_jsonl(args.input)
    if args.limit:
        rows = rows[: args.limit]
    glossary = load_glossary(args.glossary)
    existing = {row["id"] for row in read_jsonl(args.output)}
    rows = [row for row in rows if row["id"] not in existing]
    translations: list[Translation] = []

    if args.provider in {"pseudo", "copy"}:
        for row in rows:
            target = row["source_text"] if args.provider == "copy" else "[待译] " + apply_glossary_pseudo(row["source_text"], glossary)
            translations.append(
                Translation(
                    id=row["id"],
                    source_path=row["source_path"],
                    source_text=row["source_text"],
                    target_text=target,
                    provider=args.provider,
                    model=None,
                )
            )
    elif args.provider == "deepseek":
        for start in range(0, len(rows), args.batch_size):
            batch = rows[start : start + args.batch_size]
            translations.extend(call_deepseek(batch, glossary, args))
            print(f"translated={min(start + args.batch_size, len(rows))}/{len(rows)}", file=sys.stderr)
    else:
        raise SystemExit(f"Unknown provider: {args.provider}")

    existing_rows = read_jsonl(args.output)
    write_jsonl(args.output, [*existing_rows, *(asdict(item) for item in translations)])
    print(f"new_translations={len(translations)}")
    print(f"output={repo_path(args.output)}")
    return 0


def command_render(args: argparse.Namespace) -> int:
    translations = {row["id"]: row["target_text"] for row in read_jsonl(args.translations)}
    segment_rows = read_jsonl(args.segments)
    source_paths = sorted({row["source_path"] for row in segment_rows})
    args.output_root.mkdir(parents=True, exist_ok=True)
    rendered = 0
    for rel in source_paths:
        source_file = EN_ROOT / rel
        parser = SegmentingParser(rel_path=rel, translations=translations, collect=False, keep_comments=args.keep_comments)
        parser.feed(source_file.read_text(encoding="utf-8"))
        parser.close()
        output_file = args.output_root / rel
        output_file.parent.mkdir(parents=True, exist_ok=True)
        output_file.write_text(parser.render(), encoding="utf-8", newline="\n")
        rendered += 1
    print(f"rendered_files={rendered}")
    print(f"output_root={repo_path(args.output_root)}")
    return 0


def command_lint(args: argparse.Namespace) -> int:
    files = sorted(args.root.rglob("*.html"))
    if args.include:
        include_paths = [(args.root / include).resolve() for include in args.include]

        def is_included(path: Path) -> bool:
            resolved = path.resolve()
            return any(resolved == include_path or include_path in resolved.parents for include_path in include_paths)

        files = [path for path in files if is_included(path)]
    exclude_values = ([] if args.no_default_excludes else list(DEFAULT_LINT_EXCLUDES)) + list(args.exclude)
    if exclude_values:
        exclude_paths = [(args.root / exclude).resolve() for exclude in exclude_values]

        def is_excluded(path: Path) -> bool:
            resolved = path.resolve()
            return any(resolved == exclude_path or exclude_path in resolved.parents for exclude_path in exclude_paths)

        files = [path for path in files if not is_excluded(path)]
    warnings = []
    suppressed = []
    for path in files:
        raw = path.read_text(encoding="utf-8")
        rel = repo_path(path)
        source_path = matching_source_path(path, args.root, args.source_root)
        source_raw = source_path.read_text(encoding="utf-8") if source_path else ""
        source_parse_warnings = html_parse_warnings(source_raw) if args.ignore_inherited and source_raw else []
        for warning in html_parse_warnings(raw):
            formatted = f"{rel}: {warning}"
            if args.ignore_inherited and warning in source_parse_warnings:
                suppressed.append(formatted)
            else:
                warnings.append(formatted)
        if "<!--" in raw:
            formatted = f"{rel}: contains HTML comments"
            if args.ignore_inherited and "<!--" in source_raw:
                suppressed.append(formatted)
            else:
                warnings.append(formatted)
        for item, count in new_dangerous_markup(raw, source_raw).items():
            warnings.append(f"{rel}: added security-sensitive markup {item} x{count}")
        if not re.search(r"<html[^>]*lang=[\"']zh[\"']", raw, flags=re.IGNORECASE):
            warnings.append(f"{rel}: html lang is not zh")
        if not re.search(r"\bcharset\s*=\s*[\"']?utf-8\b", raw, flags=re.IGNORECASE):
            warnings.append(f"{rel}: missing UTF-8 charset declaration")
        for match in re.finditer(
            r"<meta\b[^>]*http-equiv=[\"']Content-Language[\"'][^>]*>",
            raw,
            flags=re.IGNORECASE,
        ):
            tag = match.group(0)
            content = re.search(r"\bcontent=[\"']([^\"']+)[\"']", tag, flags=re.IGNORECASE)
            if content and content.group(1).lower() != "zh":
                warnings.append(f"{rel}: Content-Language is not zh")
        visible = visible_text(raw)
        if "[待译]" in visible:
            warnings.append(f"{rel}: contains pseudo-translation TODO markers")
        ratio = english_ratio(visible_text(raw, skip_code_content=True), args.allowed_english_term)
        if ratio > args.max_english_ratio:
            warnings.append(f"{rel}: high English/CJK character ratio {ratio:.2f}")
        for term in args.banned_term:
            if re.search(rf"\b{re.escape(term)}\b", visible):
                warnings.append(f"{rel}: visible banned English term {term!r}")
                break
        link_scan_raw = COMMENT_RE.sub(" ", raw)
        for attr, value in re.findall(r'\b(href|src)=["\']([^"\']+)["\']', link_scan_raw, flags=re.IGNORECASE):
            stripped_value = value.strip()
            link_path = link_path_without_fragment(stripped_value)
            if value != stripped_value:
                formatted = f"{rel}: {attr} target has surrounding whitespace {value!r}"
                if args.ignore_inherited and source_has_same_link(source_raw, attr, value):
                    suppressed.append(formatted)
                else:
                    warnings.append(formatted)
            if link_path and re.search(r"\s", link_path):
                formatted = f"{rel}: {attr} target contains suspicious whitespace {value!r}"
                if args.ignore_inherited and source_has_same_link(source_raw, attr, value):
                    suppressed.append(formatted)
                else:
                    warnings.append(formatted)
            if DANGEROUS_URL_RE.search(stripped_value):
                continue
            if should_skip_link(stripped_value):
                continue
            target_candidate = unresolved_link_target(path, args.root, args.fallback_root, stripped_value)
            target = target_candidate.resolve()
            if not target.exists():
                formatted = f"{rel}: missing {attr} target {value}"
                if args.ignore_inherited and source_link_target_missing(source_path, args.source_root, attr, value, stripped_value, source_raw):
                    suppressed.append(formatted)
                else:
                    warnings.append(formatted)
                continue
            if not path_exists_exact(target_candidate):
                formatted = f"{rel}: {attr} target case mismatch {value}"
                warnings.append(formatted)
            fragment = link_fragment(stripped_value)
            if attr.lower() == "href" and fragment and target.suffix.lower() in {".html", ".htm"}:
                anchors = html_anchors(target)
                if fragment not in anchors:
                    formatted = f"{rel}: missing href anchor {value}"
                    if args.ignore_inherited and source_href_anchor_missing(source_path, args.source_root, value, stripped_value, source_raw):
                        suppressed.append(formatted)
                    else:
                        warnings.append(formatted)
    print(f"checked_files={len(files)}")
    print(f"warnings={len(warnings)}")
    if args.ignore_inherited:
        print(f"suppressed_inherited={len(suppressed)}")
    for warning in warnings[: args.max_warnings]:
        print(warning)
    if args.fail_on_warnings and warnings:
        return 1
    return 0


def command_check_javahelp(args: argparse.Namespace) -> int:
    warnings = []
    for lang in args.lang:
        map_path = args.doc_root / f"map_{lang}.jhm"
        contents_path = args.doc_root / lang / "contents.xml"
        if not map_path.exists():
            warnings.append(f"{repo_path(map_path)}: missing JavaHelp map")
            continue
        if not contents_path.exists():
            warnings.append(f"{repo_path(contents_path)}: missing JavaHelp contents")
            continue
        map_ids = {}
        for elem in ET.parse(map_path).iter("mapID"):
            target = elem.attrib.get("target")
            url = elem.attrib.get("url")
            if target and url and target not in map_ids:
                map_ids[target] = url
        contents_targets = []
        image_targets = []
        for elem in ET.parse(contents_path).iter("tocitem"):
            target = elem.attrib.get("target")
            image = elem.attrib.get("image")
            if target:
                contents_targets.append(target)
            if image:
                image_targets.append(image)
        for required in args.required_target:
            if required not in contents_targets:
                contents_targets.append(required)
        for target in sorted(set(contents_targets)):
            url = map_ids.get(target)
            if not url:
                warnings.append(f"{repo_path(contents_path)}: target {target!r} has no mapID in {map_path.name}")
                continue
            warnings.extend(javahelp_url_warnings(args.doc_root, map_path, target, url))
        for target in sorted(set(image_targets)):
            url = map_ids.get(target)
            if not url:
                warnings.append(f"{repo_path(contents_path)}: image target {target!r} has no mapID in {map_path.name}")
                continue
            warnings.extend(javahelp_url_warnings(args.doc_root, map_path, target, url))
    print(f"checked_langs={len(args.lang)}")
    print(f"warnings={len(warnings)}")
    for warning in warnings[: args.max_warnings]:
        print(warning)
    if args.fail_on_warnings and warnings:
        return 1
    return 0


def should_skip_link(value: str) -> bool:
    if not value:
        return True
    lowered = value.lower()
    return lowered.startswith(("http://", "https://", "mailto:"))


def html_parse_warnings(raw: str) -> list[str]:
    vendor = DEFAULT_WORK / "vendor"
    if vendor.exists() and str(vendor) not in sys.path:
        sys.path.insert(0, str(vendor))
    try:
        from lxml import html
    except ImportError:
        return ["lxml is not available; skipped HTML parser validation"]
    parser = html.HTMLParser(recover=True)
    try:
        html.document_fromstring(raw, parser=parser)
    except Exception as exc:  # pragma: no cover - defensive parser guard
        return [f"HTML parser failed: {exc}"]
    serious = []
    for error in parser.error_log:
        if error.level_name in {"ERROR", "FATAL"}:
            serious.append(f"HTML parser {error.level_name.lower()} line {error.line}: {error.message}")
    return serious


def english_ratio(text: str, allowed_terms: Iterable[str]) -> float:
    filtered = text
    for term in sorted(set(DEFAULT_ALLOWED_ENGLISH_TERMS) | set(allowed_terms), key=len, reverse=True):
        filtered = re.sub(rf"(?<![A-Za-z0-9_-]){re.escape(term)}(?![A-Za-z0-9_-])", " ", filtered)
    for pattern in DEFAULT_ALLOWED_ENGLISH_PATTERNS:
        filtered = re.sub(pattern, " ", filtered)
    ascii_letters = len(re.findall(r"[A-Za-z]", filtered))
    cjk_chars = len(re.findall(r"[\u3400-\u9fff]", filtered))
    if not cjk_chars:
        return float(ascii_letters) if ascii_letters else 0.0
    return ascii_letters / cjk_chars


def resolve_link_target(path: Path, root: Path, fallback_root: Path | None, value: str) -> Path:
    return unresolved_link_target(path, root, fallback_root, value).resolve()


def unresolved_link_target(path: Path, root: Path, fallback_root: Path | None, value: str) -> Path:
    link_path = link_path_without_fragment(value)
    if not link_path:
        return path
    local_target = path.parent / link_path
    if local_target.exists() or fallback_root is None:
        return local_target
    try:
        rel_file = path.resolve().relative_to(root.resolve())
    except ValueError:
        return local_target
    fallback_target = fallback_root / rel_file.parent / link_path
    return fallback_target if fallback_target.exists() else local_target


def link_path_without_fragment(value: str) -> str:
    return value.split("#", 1)[0]


def link_fragment(value: str) -> str:
    if "#" not in value:
        return ""
    return unquote(value.split("#", 1)[1])


def html_anchors(path: Path) -> set[str]:
    raw = path.read_text(encoding="utf-8")
    anchors = set()
    for attr in ("id", "name"):
        for match in re.finditer(rf'\b{attr}=["\']([^"\']+)["\']', raw, flags=re.IGNORECASE):
            anchors.add(match.group(1))
    return anchors


def javahelp_url_warnings(doc_root: Path, map_path: Path, target: str, url: str) -> list[str]:
    warnings = []
    path_part = link_path_without_fragment(url)
    fragment = link_fragment(url)
    target_path = doc_root / path_part
    label = f"{repo_path(map_path)}: mapID {target!r}"
    if not target_path.exists():
        warnings.append(f"{label} points to missing file {url}")
        return warnings
    if not path_exists_exact(target_path):
        warnings.append(f"{label} has path case mismatch {url}")
    if fragment and target_path.suffix.lower() in {".html", ".htm"}:
        anchors = html_anchors(target_path.resolve())
        if fragment not in anchors:
            warnings.append(f"{label} points to missing anchor {url}")
    return warnings


def path_exists_exact(path: Path) -> bool:
    if not path.exists():
        return False
    absolute = Path(os.path.abspath(str(path)))
    parts = absolute.parts
    if not parts:
        return True
    current = Path(parts[0])
    for part in parts[1:]:
        try:
            names = {child.name for child in current.iterdir()}
        except OSError:
            return False
        if part not in names:
            return False
        current = current / part
    return True


def matching_source_path(path: Path, root: Path, source_root: Path | None) -> Path | None:
    if source_root is None:
        return None
    try:
        rel_file = path.resolve().relative_to(root.resolve())
    except ValueError:
        return None
    source_file = source_root / rel_file
    return source_file if source_file.exists() else None


def matching_source_html(path: Path, root: Path, source_root: Path | None) -> str:
    source_file = matching_source_path(path, root, source_root)
    if source_file is None:
        return ""
    return source_file.read_text(encoding="utf-8")


def source_has_same_link(source_raw: str, attr: str, value: str) -> bool:
    return any(
        source_attr.lower() == attr.lower() and source_value == value
        for source_attr, source_value in re.findall(r'\b(href|src)=["\']([^"\']+)["\']', source_raw, flags=re.IGNORECASE)
    )


def source_link_target_missing(
    source_path: Path | None,
    source_root: Path | None,
    attr: str,
    value: str,
    stripped_value: str,
    source_raw: str,
) -> bool:
    if source_path is None or source_root is None or not source_has_same_link(source_raw, attr, value):
        return False
    if DANGEROUS_URL_RE.search(stripped_value) or should_skip_link(stripped_value):
        return False
    target = resolve_link_target(source_path, source_root, None, stripped_value)
    return not target.exists()


def source_href_anchor_missing(
    source_path: Path | None,
    source_root: Path | None,
    value: str,
    stripped_value: str,
    source_raw: str,
) -> bool:
    if source_path is None or source_root is None or not source_has_same_link(source_raw, "href", value):
        return False
    fragment = link_fragment(stripped_value)
    if not fragment:
        return False
    target = resolve_link_target(source_path, source_root, None, stripped_value)
    return target.exists() and target.suffix.lower() in {".html", ".htm"} and fragment not in html_anchors(target)


def dangerous_markup_counter(raw: str) -> Counter[str]:
    counter: Counter[str] = Counter()
    for tag in DANGEROUS_TAG_RE.findall(raw):
        counter[f"tag:{tag.lower()}"] += 1
    for attr in DANGEROUS_ATTR_RE.findall(raw):
        counter[f"attr:{attr.lower()}"] += 1
    for attr, value in re.findall(r'\b(href|src)=["\']([^"\']+)["\']', raw, flags=re.IGNORECASE):
        match = DANGEROUS_URL_RE.match(value.strip())
        if match:
            counter[f"{attr.lower()}:{match.group(0).lower()}"] += 1
    return counter


def new_dangerous_markup(raw: str, source_raw: str) -> Counter[str]:
    target = dangerous_markup_counter(raw)
    source = dangerous_markup_counter(source_raw)
    return target - source


def tag_signature(text: str) -> list[str]:
    return [re.sub(r"\s+", " ", tag.strip()) for tag in OPEN_TAG_RE.findall(text)]


def tag_counts(tags: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for tag in tags:
        counts[tag] = counts.get(tag, 0) + 1
    return counts


def token_counter(pattern: re.Pattern[str], text: str) -> Counter[str]:
    return Counter(match.group(0) for match in pattern.finditer(text))


def compact_counter(counter: Counter[str]) -> str:
    return ", ".join(f"{token}x{count}" if count > 1 else token for token, count in sorted(counter.items()))


def shortcut_preserved(token: str, target: str) -> bool:
    if token in target:
        return True
    return any(re.search(pattern, target, flags=re.IGNORECASE) for pattern in SHORTCUT_EQUIVALENTS.get(token, ()))


def missing_shortcuts(source_shortcuts: Counter[str], target: str) -> Counter[str]:
    missing: Counter[str] = Counter()
    for token, count in source_shortcuts.items():
        target_count = target.count(token)
        if target_count >= count:
            continue
        if shortcut_preserved(token, target):
            continue
        missing[token] = count - target_count
    return missing


def visible_text(text: str, *, skip_code_content: bool = False) -> str:
    text = COMMENT_RE.sub(" ", text)
    text = re.sub(r"(?is)<script\b.*?</script>|<style\b.*?</style>", " ", text)
    if skip_code_content:
        text = RATIO_CODE_CONTENT_RE.sub(" ", text)
        text = re.sub(r"&lt;/?[A-Za-z0-9_-]+&gt;", " ", text)
        text = TABLE_RE.sub(lambda match: " " if looks_like_mnemonic_table(match.group(0)) else match.group(0), text)
    return normalize_text(TAG_RE.sub(" ", text))


def looks_like_mnemonic_table(html_fragment: str) -> bool:
    plain = normalize_text(TAG_RE.sub(" ", COMMENT_RE.sub(" ", html_fragment)))
    tokens = TEXT_RE.findall(plain)
    if len(tokens) < 80:
        return False
    short_ratio = sum(1 for token in tokens if len(token) <= 7) / len(tokens)
    lowercase_ratio = sum(1 for token in tokens if token.lower() == token) / len(tokens)
    ascii_letters = len(re.findall(r"[A-Za-z]", plain))
    cjk_chars = len(re.findall(r"[\u3400-\u9fff]", plain))
    return short_ratio >= 0.88 and lowercase_ratio >= 0.75 and ascii_letters > max(cjk_chars * 5, 120)


def preserve_code_content(rendered_html: str, source_html: str) -> str:
    source_blocks = CODE_CONTENT_RE.findall(source_html)
    target_matches = list(CODE_CONTENT_RE.finditer(rendered_html))
    source_matches = list(CODE_CONTENT_RE.finditer(source_html))
    if not source_blocks or len(source_matches) != len(target_matches):
        return rendered_html
    result = []
    last = 0
    for target_match, source_match in zip(target_matches, source_matches):
        result.append(rendered_html[last : target_match.start()])
        result.append(source_match.group(0))
        last = target_match.end()
    result.append(rendered_html[last:])
    return "".join(result)


def command_check_translations(args: argparse.Namespace) -> int:
    rows = read_jsonl(args.input)
    warnings = []
    for row in rows:
        source = row.get("source_text", "")
        target = row.get("target_text", "")
        label = f"{row.get('source_path', '?')}:{row.get('id', '?')}"
        if looks_like_code_block(source):
            if target != source:
                warnings.append(f"{label}: code block changed")
            continue
        if "[待译]" in target:
            warnings.append(f"{label}: contains pseudo-translation marker")
        if re.search(r"⟦TAG\d+⟧", target):
            warnings.append(f"{label}: contains unresolved HTML placeholder marker")
        if "你" in target:
            warnings.append(f"{label}: uses 你; prefer neutral wording or 您 in documentation")
        source_tags = tag_signature(source)
        target_tags = tag_signature(target)
        if tag_counts(source_tags) != tag_counts(target_tags):
            warnings.append(f"{label}: HTML tag set or attributes changed")
        elif source_tags != target_tags and args.flag_tag_reorder and not args.allow_tag_reorder:
            warnings.append(f"{label}: HTML tag order changed; review if intentional")
        visible_source = visible_text(source)
        visible_target = visible_text(target)
        if not args.disable_required_terms:
            for source_pattern, required_target, term_label in DEFAULT_REQUIRED_TERM_PAIRS:
                if re.search(source_pattern, visible_source, flags=re.IGNORECASE) and required_target not in visible_target:
                    warnings.append(f"{label}: expected glossary term {required_target!r} for {term_label}")
        source_numbers = token_counter(NUMBER_RE, source)
        target_numbers = token_counter(NUMBER_RE, target)
        if source_numbers != target_numbers:
            warnings.append(
                f"{label}: number tokens changed ({compact_counter(source_numbers)} -> {compact_counter(target_numbers)})"
            )
        source_shortcuts = token_counter(SHORTCUT_RE, source)
        missing_shortcut_tokens = missing_shortcuts(source_shortcuts, visible_target)
        if missing_shortcut_tokens:
            warnings.append(
                f"{label}: keyboard shortcuts changed ({compact_counter(source_shortcuts)} -> {compact_counter(token_counter(SHORTCUT_RE, target))})"
            )
        for term in args.banned_term:
            if re.search(rf"\b{re.escape(term)}\b", visible_target):
                warnings.append(f"{label}: visible banned English term {term!r}")
                break
        ratio = english_ratio(visible_text(target, skip_code_content=True), args.allowed_english_term)
        if ratio > args.max_english_ratio:
            warnings.append(f"{label}: high English/CJK character ratio {ratio:.2f}")
    print(f"checked_translations={len(rows)}")
    print(f"warnings={len(warnings)}")
    for warning in warnings[: args.max_warnings]:
        print(warning)
    if args.fail_on_warnings and warnings:
        return 1
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.set_defaults(func=lambda _args: parser.print_help() or 0)
    sub = parser.add_subparsers()

    inventory = sub.add_parser("inventory", help="Summarize English HTML documentation size")
    inventory.add_argument("--top", type=int, default=20)
    inventory.set_defaults(func=command_inventory)

    extract = sub.add_parser("extract", help="Extract translatable segments from English HTML")
    extract.add_argument("--all", action="store_true", help="Extract all English HTML pages")
    extract.add_argument("--sample", action="store_true", help="Extract tools/zh_docs/sample_targets.txt")
    extract.add_argument("--files", nargs="*", help="Specific paths relative to en/html")
    extract.add_argument("--output", type=Path, default=DEFAULT_SEGMENTS)
    extract.set_defaults(func=command_extract)

    po_extract = sub.add_parser("po-extract", help="Extract HTML with Translate Toolkit html2po")
    po_extract.add_argument("--all", action="store_true", help="Extract all English HTML pages")
    po_extract.add_argument("--sample", action="store_true", help="Extract tools/zh_docs/sample_targets.txt")
    po_extract.add_argument("--files", nargs="*", help="Specific paths relative to en/html")
    po_extract.add_argument("--output-root", type=Path, default=DEFAULT_PO_ROOT)
    po_extract.add_argument("--clean-root", type=Path, default=DEFAULT_CLEAN_ROOT)
    po_extract.add_argument("--no-sanitize", action="store_true", help="Use source HTML directly instead of lxml-normalized HTML")
    po_extract.add_argument("--pot", action="store_true", help="Write POT templates instead of PO files")
    po_extract.set_defaults(func=command_po_extract)

    po_update = sub.add_parser("po-update", help="Create or update PO files from POT templates with Translate Toolkit pot2po")
    po_update.add_argument("--input-root", type=Path, default=DEFAULT_POT_ROOT)
    po_update.add_argument("--template-root", type=Path, default=None, help="Existing PO files to reuse as translation memory")
    po_update.add_argument("--output-root", type=Path, default=DEFAULT_PO_ROOT)
    po_update.set_defaults(func=command_po_update)

    po_to_jsonl = sub.add_parser("po-to-jsonl", help="Convert extracted PO files to translation JSONL")
    po_to_jsonl.add_argument("--input-root", type=Path, default=DEFAULT_PO_ROOT)
    po_to_jsonl.add_argument("--template-root", type=Path, help="HTML template root used to skip code-like content")
    po_to_jsonl.add_argument("--output", type=Path, default=DEFAULT_SEGMENTS)
    po_to_jsonl.set_defaults(func=command_po_to_jsonl)

    translate = sub.add_parser("translate", help="Translate extracted segments")
    translate.add_argument("--input", type=Path, default=DEFAULT_SEGMENTS)
    translate.add_argument("--output", type=Path, default=DEFAULT_TRANSLATIONS)
    translate.add_argument("--glossary", type=Path, default=Path(__file__).with_name("glossary.yml"))
    translate.add_argument("--provider", choices=["pseudo", "copy", "deepseek"], default="pseudo")
    translate.add_argument("--model", default="deepseek-v4-pro")
    translate.add_argument("--batch-size", type=int, default=30)
    translate.add_argument("--limit", type=int, default=0)
    translate.add_argument("--temperature", type=float, default=0.2)
    translate.add_argument(
        "--thinking",
        choices=["default", "enabled", "disabled"],
        default="disabled",
        help="DeepSeek V4 thinking mode; disabled is the default for translation throughput",
    )
    translate.add_argument("--reasoning-effort", choices=["high", "max"], default="high")
    translate.add_argument("--timeout", type=int, default=120)
    translate.add_argument("--retries", type=int, default=2)
    translate.set_defaults(func=command_translate)

    render = sub.add_parser("render", help="Render zh HTML preview from translations")
    render.add_argument("--segments", type=Path, default=DEFAULT_SEGMENTS)
    render.add_argument("--translations", type=Path, default=DEFAULT_TRANSLATIONS)
    render.add_argument("--output-root", type=Path, default=DEFAULT_PREVIEW)
    render.add_argument("--keep-comments", action="store_true")
    render.set_defaults(func=command_render)

    po_apply = sub.add_parser("po-apply", help="Apply translation JSONL back into PO files")
    po_apply.add_argument("--input-root", type=Path, default=DEFAULT_PO_ROOT)
    po_apply.add_argument("--translations", type=Path, default=DEFAULT_TRANSLATIONS)
    po_apply.add_argument("--output-root", type=Path, default=DEFAULT_PO_TRANSLATED_ROOT)
    po_apply.set_defaults(func=command_po_apply)

    po_filter = sub.add_parser("po-filter", help="Run Translate Toolkit pofilter on translated PO files")
    po_filter.add_argument("--input-root", type=Path, default=DEFAULT_PO_TRANSLATED_ROOT)
    po_filter.add_argument("--output-root", type=Path, default=DEFAULT_PO_QA_ROOT)
    po_filter.add_argument("--language", default="zh_CN")
    po_filter.add_argument("--test", action="append", default=[])
    po_filter.add_argument("--all-tests", action="store_true", help="Run every pofilter test instead of the curated low-noise default set")
    po_filter.add_argument("--exclude-filter", action="append", default=[])
    po_filter.add_argument("--no-clean-output", action="store_true", help="Do not clear the pofilter output directory before running")
    po_filter.add_argument("--fail-on-qa", action="store_true")
    po_filter.set_defaults(func=command_po_filter)

    po_render = sub.add_parser("po-render", help="Render translated PO files with Translate Toolkit po2html")
    po_render.add_argument("--input-root", type=Path, default=DEFAULT_PO_TRANSLATED_ROOT)
    po_render.add_argument("--output-root", type=Path, default=DEFAULT_PO_PREVIEW)
    po_render.add_argument("--template-root", type=Path, default=DEFAULT_CLEAN_ROOT)
    po_render.set_defaults(func=command_po_render)

    lint = sub.add_parser("lint", help="Lint rendered zh HTML preview")
    lint.add_argument("--root", type=Path, default=DEFAULT_PREVIEW)
    lint.add_argument(
        "--fallback-root",
        type=Path,
        default=ZH_ROOT,
        help="Resolve preview-relative links against this final doc root if assets are not copied into the preview",
    )
    lint.add_argument(
        "--source-root",
        type=Path,
        default=EN_ROOT,
        help="Allow security-sensitive markup that already exists in the corresponding source page",
    )
    lint.add_argument("--banned-term", action="append", default=["Pin", "Pins", "pin", "pins"])
    lint.add_argument("--allowed-english-term", action="append", default=[])
    lint.add_argument(
        "--include",
        action="append",
        default=[],
        help="Only lint this file or directory relative to --root; may be repeated while keeping link resolution rooted at --root",
    )
    lint.add_argument(
        "--exclude",
        action="append",
        default=[],
        help="Skip this file or directory relative to --root; may be repeated",
    )
    lint.add_argument(
        "--no-default-excludes",
        action="store_true",
        help="Also lint template-only paths that are excluded by default",
    )
    lint.add_argument(
        "--ignore-inherited",
        action="store_true",
        help="Suppress selected warnings when the corresponding English source has the same problem",
    )
    lint.add_argument("--max-english-ratio", type=float, default=0.2)
    lint.add_argument("--max-warnings", type=int, default=50)
    lint.add_argument("--fail-on-warnings", action="store_true")
    lint.set_defaults(func=command_lint)

    check_javahelp = sub.add_parser("check-javahelp", help="Validate JavaHelp TOC/map targets")
    check_javahelp.add_argument("--doc-root", type=Path, default=REPO_ROOT / "src/main/resources/doc")
    check_javahelp.add_argument("--lang", action="append", default=["en", "zh"], help="Language code to check; may be repeated")
    check_javahelp.add_argument(
        "--required-target",
        action="append",
        default=["top", "guide", "tutorial", "libs"],
        help="Map target that must resolve even if it is not listed in contents.xml",
    )
    check_javahelp.add_argument("--max-warnings", type=int, default=80)
    check_javahelp.add_argument("--fail-on-warnings", action="store_true")
    check_javahelp.set_defaults(func=command_check_javahelp)

    check_translations = sub.add_parser("check-translations", help="QA checks for translation JSONL")
    check_translations.add_argument("--input", type=Path, default=DEFAULT_TRANSLATIONS)
    check_translations.add_argument("--banned-term", action="append", default=["Pin", "Pins", "pin", "pins"])
    check_translations.add_argument("--allowed-english-term", action="append", default=[])
    check_translations.add_argument("--max-english-ratio", type=float, default=0.35)
    check_translations.add_argument("--flag-tag-reorder", action="store_true", help="Warn when inline HTML tag order changes")
    check_translations.add_argument("--allow-tag-reorder", action="store_true", help=argparse.SUPPRESS)
    check_translations.add_argument(
        "--disable-required-terms",
        action="store_true",
        help="Disable required glossary consistency checks such as pin -> 引脚 and poke -> 手形",
    )
    check_translations.add_argument("--max-warnings", type=int, default=80)
    check_translations.add_argument("--fail-on-warnings", action="store_true")
    check_translations.set_defaults(func=command_check_translations)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
