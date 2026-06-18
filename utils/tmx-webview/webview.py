#!/usr/bin/env python3
"""
TMX Web Viewer — Megaman Maverick

Parses ASCII grid output from utils/tmx-visualizer/visualize.py (or Claude-generated
ASCII in the same format) and either validates it or renders it as a self-contained
HTML page.

Usage:
  webview.py INPUT [--validate] [-o OUTPUT]

  INPUT        Path to a .viz.txt file.
  --validate   Validate only; print errors/warnings; exit 1 if any errors.
  -o OUTPUT    HTML output path (default: /tmp/tmx-webview.html).
"""

import argparse
import os
import re
import sys
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple


# ── data model ────────────────────────────────────────────────────────────────

@dataclass
class Cell:
    raw: str    # 5-char content as it appeared inside brackets
    code: str   # stripped version used for lookup
    line: int   # 1-based source line


@dataclass
class Room:
    name: str
    origin_x: int
    origin_y: int
    width: int   # tiles
    height: int  # tiles
    rows: List[List[Cell]]
    header_line: int


@dataclass
class KeyEntry:
    code: str
    description: str
    line: int


@dataclass
class ParseResult:
    rooms: List[Room]
    key_entries: Dict[str, KeyEntry]
    errors: List[Tuple[int, str]]    # (lineno, message)
    warnings: List[Tuple[int, str]]


# ── constants ─────────────────────────────────────────────────────────────────

HEADER_RE = re.compile(
    r'^===\s+(\S+)\s+origin\((\d+),(\d+)\)px\s+(\d+)x(\d+)\s+tiles\s+===$'
)
KEY_START_RE = re.compile(r'^=====\s+KEY\s+=====\s*$')
KEY_ENTRY_RE = re.compile(r'^\[(.{1,5})\]\s+(.+)$')
COMBO_SECTION_RE = re.compile(r'^--\s+combined')
CELL_RE = re.compile(r'\[(.{5})\]')
PLAYER_SPAWN_RE = re.compile(r'^PS-?\d*\s*$')

# Codes that are always valid without a KEY entry.
def _is_fixed(code: str) -> bool:
    return code in ('', 'X', '.') or bool(PLAYER_SPAWN_RE.match(code))


# ── parser ────────────────────────────────────────────────────────────────────

def parse(text: str) -> ParseResult:
    lines = text.split('\n')
    errors: List[Tuple[int, str]] = []
    warnings: List[Tuple[int, str]] = []
    rooms: List[Room] = []
    key_entries: Dict[str, KeyEntry] = {}

    # Locate KEY section boundary.
    key_start_line: Optional[int] = None
    for i, line in enumerate(lines):
        if KEY_START_RE.match(line.strip()):
            key_start_line = i
            break

    body_lines = lines[:key_start_line] if key_start_line is not None else lines
    key_lines  = lines[key_start_line + 1:] if key_start_line is not None else []

    # Parse KEY section.
    for offset, line in enumerate(key_lines):
        lineno = key_start_line + 2 + offset  # 1-based
        stripped = line.strip()
        if not stripped or COMBO_SECTION_RE.match(stripped):
            continue
        m = KEY_ENTRY_RE.match(stripped)
        if m:
            code = m.group(1).strip()
            desc = m.group(2).strip()
            key_entries[code] = KeyEntry(code=code, description=desc, line=lineno)

    # Parse room sections from the body.
    cur_name: Optional[str] = None
    cur_ox = cur_oy = cur_w = cur_h = 0
    cur_rows: List[List[Cell]] = []
    cur_header_line = 0

    def _finalize():
        if cur_name is None:
            return
        if len(cur_rows) != cur_h:
            errors.append((cur_header_line,
                f"room '{cur_name}' ended with {len(cur_rows)} rows, expected {cur_h}"))
        rooms.append(Room(
            name=cur_name,
            origin_x=cur_ox, origin_y=cur_oy,
            width=cur_w, height=cur_h,
            rows=cur_rows,
            header_line=cur_header_line,
        ))

    for i, line in enumerate(body_lines):
        lineno = i + 1
        stripped = line.strip()
        if not stripped:
            continue

        m = HEADER_RE.match(stripped)
        if m:
            _finalize()
            cur_name        = m.group(1)
            cur_ox          = int(m.group(2))
            cur_oy          = int(m.group(3))
            cur_w           = int(m.group(4))
            cur_h           = int(m.group(5))
            cur_rows        = []
            cur_header_line = lineno
            continue

        if cur_name is None:
            continue  # content before first header — skip

        cells_raw = CELL_RE.findall(line)
        if not cells_raw:
            continue

        if len(cells_raw) != cur_w:
            errors.append((lineno,
                f"room '{cur_name}' row {len(cur_rows) + 1} has {len(cells_raw)} cells, "
                f"expected {cur_w}"))

        row = [Cell(raw=r, code=r.strip(), line=lineno) for r in cells_raw]
        cur_rows.append(row)

    _finalize()

    # Validate cell codes against KEY.
    used_codes: set = set()
    for room in rooms:
        for row in room.rows:
            for cell in row:
                if _is_fixed(cell.code):
                    continue
                used_codes.add(cell.code)
                if cell.code not in key_entries:
                    errors.append((cell.line,
                        f"unknown code '{cell.code}' — not defined in KEY"))

    # Warn about KEY entries that were never used.
    for code, entry in key_entries.items():
        if code not in used_codes:
            warnings.append((entry.line,
                f"KEY defines '[{code}]' but it appears in no room"))

    return ParseResult(rooms=rooms, key_entries=key_entries,
                       errors=errors, warnings=warnings)


# ── renderer ──────────────────────────────────────────────────────────────────

_CATEGORY_COLORS: Dict[str, Tuple[str, str]] = {
    '':   ('#e8e8e8', '#aaa'),   # empty         bg, fg
    'X':  ('#4a4a4a', '#ccc'),   # block
    '.':  ('#888888', '#ccc'),   # continuation
    'PS': ('#27ae60', '#fff'),   # player spawn
    'E':  ('#e74c3c', '#fff'),   # enemies
    'H':  ('#e67e22', '#fff'),   # hazards
    'B':  ('#2980b9', '#fff'),   # named blocks
    'S':  ('#8e44ad', '#fff'),   # specials
    'N':  ('#16a085', '#fff'),   # sensors
    'I':  ('#f39c12', '#fff'),   # items
}

_COMBO_PALETTE = [
    ('#c0392b', '#fff'), ('#d35400', '#fff'), ('#1a5276', '#fff'),
    ('#145a32', '#fff'), ('#6c3483', '#fff'), ('#0e6655', '#fff'),
    ('#784212', '#fff'), ('#1b2631', '#fff'),
]


def _cell_colors(code: str, combo_map: Dict[str, int]) -> Tuple[str, str]:
    if code == '':
        return _CATEGORY_COLORS['']
    if code == 'X':
        return _CATEGORY_COLORS['X']
    if code == '.':
        return _CATEGORY_COLORS['.']
    if PLAYER_SPAWN_RE.match(code):
        return _CATEGORY_COLORS['PS']
    if code.startswith('&'):
        idx = combo_map.get(code, 0)
        return _COMBO_PALETTE[idx % len(_COMBO_PALETTE)]
    prefix = code[0] if code else ''
    return _CATEGORY_COLORS.get(prefix, ('#bdc3c7', '#333'))


def _esc(s: str) -> str:
    return (str(s)
            .replace('&', '&amp;')
            .replace('<', '&lt;')
            .replace('>', '&gt;')
            .replace('"', '&quot;'))


def render_html(result: ParseResult, source_path: str) -> str:
    source_name = os.path.basename(source_path)

    # Assign stable indices to combo codes for consistent palette assignment.
    combo_map: Dict[str, int] = {}
    for code in result.key_entries:
        if code.startswith('&') and code not in combo_map:
            combo_map[code] = len(combo_map)

    # Build room sections.
    rooms_html_parts = []
    for room in result.rooms:
        row_parts = []
        for row in room.rows:
            cell_parts = []
            for cell in row:
                bg, fg = _cell_colors(cell.code, combo_map)
                display = cell.code if cell.code else '&nbsp;'
                cell_parts.append(
                    f'<span class="cell" style="background:{bg};color:{fg}"'
                    f' title="{_esc(cell.code)}">{_esc(display)}</span>'
                )
            row_parts.append('<div class="row">' + ''.join(cell_parts) + '</div>')

        rooms_html_parts.append(
            f'<section class="room">'
            f'<h2>{_esc(room.name)}'
            f'<span class="dims">'
            f'origin({room.origin_x},{room.origin_y})px'
            f'&nbsp;&nbsp;{room.width}&times;{room.height} tiles'
            f'</span></h2>'
            f'<div class="grid">{"".join(row_parts)}</div>'
            f'</section>'
        )

    rooms_html = '\n'.join(rooms_html_parts)

    # Build legend.
    legend_rows = []
    for code, entry in result.key_entries.items():
        bg, fg = _cell_colors(code, combo_map)
        legend_rows.append(
            f'<tr>'
            f'<td><span class="swatch" style="background:{bg};color:{fg}">{_esc(code)}</span></td>'
            f'<td>{_esc(entry.description)}</td>'
            f'</tr>'
        )
    legend_html = (
        '<table class="legend"><tbody>' + ''.join(legend_rows) + '</tbody></table>'
        if legend_rows else '<p style="color:#666">No KEY entries.</p>'
    )

    return f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>TMX Web View — {_esc(source_name)}</title>
<style>
  *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    font-family: monospace;
    background: #12121e;
    color: #ddd;
    padding: 20px 24px 48px;
  }}
  h1 {{
    font-size: 0.95em;
    color: #666;
    margin-bottom: 28px;
    letter-spacing: 0.04em;
  }}
  .room {{
    margin-bottom: 48px;
  }}
  h2 {{
    font-size: 0.95em;
    color: #7fb3d3;
    margin-bottom: 10px;
    font-weight: bold;
  }}
  .dims {{
    font-size: 0.8em;
    color: #555;
    margin-left: 14px;
    font-weight: normal;
  }}
  .grid {{
    display: inline-block;
    border: 1px solid #2a2a3a;
    line-height: 0;
  }}
  .row {{
    display: flex;
  }}
  .cell {{
    display: inline-block;
    width: 44px;
    height: 18px;
    font-size: 9px;
    line-height: 18px;
    text-align: center;
    border-right: 1px solid rgba(0,0,0,0.18);
    border-bottom: 1px solid rgba(0,0,0,0.18);
    overflow: hidden;
    white-space: nowrap;
    cursor: default;
    user-select: none;
  }}
  h3 {{
    color: #888;
    font-size: 0.85em;
    margin: 32px 0 10px;
    text-transform: uppercase;
    letter-spacing: 0.06em;
  }}
  .legend {{
    border-collapse: collapse;
  }}
  .legend td {{
    padding: 3px 16px 3px 4px;
    font-size: 0.82em;
    color: #bbb;
    vertical-align: middle;
  }}
  .swatch {{
    display: inline-block;
    padding: 1px 6px;
    border-radius: 3px;
    font-size: 0.78em;
    min-width: 42px;
    text-align: center;
  }}
</style>
</head>
<body>
<h1>TMX Web View &mdash; {_esc(source_name)}</h1>
{rooms_html}
<h3>Legend</h3>
{legend_html}
</body>
</html>'''


# ── entry point ───────────────────────────────────────────────────────────────

def _load_input(input_path: str) -> str:
    """Read the viz text, auto-appending a companion .legend.txt if present."""
    with open(input_path, 'r', encoding='utf-8') as f:
        text = f.read()

    # If no KEY section in the viz file, look for a sibling .legend.txt.
    if not KEY_START_RE.search(text):
        base = input_path
        for suffix in ('.viz.txt', '.txt'):
            if base.endswith(suffix):
                base = base[:-len(suffix)]
                break
        legend_path = base + '.legend.txt'
        if os.path.exists(legend_path):
            with open(legend_path, 'r', encoding='utf-8') as f:
                text = text.rstrip('\n') + '\n\n' + f.read()

    return text


def main():
    ap = argparse.ArgumentParser(
        description='Validate or render ASCII TMX grid output as HTML.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    ap.add_argument('input', help='Path to .viz.txt file')
    ap.add_argument('--validate', action='store_true',
                    help='Validate only; exit 1 if errors found')
    ap.add_argument('-o', '--output', default='/tmp/tmx-webview.html',
                    help='HTML output path (default: /tmp/tmx-webview.html)')
    args = ap.parse_args()

    if not os.path.exists(args.input):
        print(f'File not found: {args.input}', file=sys.stderr)
        sys.exit(1)

    text = _load_input(args.input)

    result = parse(text)

    for lineno, msg in sorted(result.errors):
        print(f'ERROR line {lineno}: {msg}')
    for lineno, msg in sorted(result.warnings):
        print(f'WARNING line {lineno}: {msg}')

    if result.errors:
        summary = f'{len(result.errors)} error(s)'
        if result.warnings:
            summary += f', {len(result.warnings)} warning(s)'
        if args.validate:
            print(f'\n{summary} — fix errors before rendering.', file=sys.stderr)
        sys.exit(1)

    if args.validate:
        ok = f'OK — {len(result.rooms)} room(s), {len(result.key_entries)} KEY entries'
        if result.warnings:
            ok += f', {len(result.warnings)} warning(s)'
        print(ok)
        return

    html = render_html(result, args.input)
    with open(args.output, 'w', encoding='utf-8') as f:
        f.write(html)
    print(f'Wrote {args.output}')


if __name__ == '__main__':
    main()
