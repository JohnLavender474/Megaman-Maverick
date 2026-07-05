#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

USAGE = 'Usage: level_analyzer.py <path/to/level.tmx> [output.txt]'

TILE_SIZE = 32

# Layers whose Ladder-named objects become L; everything else becomes X
BLOCK_LAYERS = ('blocks', 'specials')

# Priority: higher number wins when two objects land on the same cell
PRIORITY = {
    ' ': 0,
    'W': 1,
    'X': 2,
    'L': 3,
    'D': 4,
    '\\': 5,
    'I': 6,
    'E': 7,
    '0': 8,
}

# Spawn markers always win their cell against any other object
SPAWN_PRIORITY = PRIORITY['0'] + 1


def _bbox(obj):
    x = float(obj.get('x', 0))
    y = float(obj.get('y', 0))
    w = float(obj.get('width', 0))
    h = float(obj.get('height', 0))
    return x, y, w, h


def _overlaps(px, py, pw, ph, rx, ry, rw, rh) -> bool:
    return px + pw > rx and px < rx + rw and py + ph > ry and py < ry + rh


def _center_in(px, py, pw, ph, rx, ry, rw, rh) -> bool:
    cx, cy = px + pw / 2, py + ph / 2
    return rx <= cx < rx + rw and ry <= cy < ry + rh


class Room:
    def __init__(self, name, x, y, w, h):
        self.name = name
        self.x, self.y, self.w, self.h = int(x), int(y), int(w), int(h)
        self.cols = self.w // TILE_SIZE
        self.rows = self.h // TILE_SIZE
        self.grid = [[' '] * self.cols for _ in range(self.rows)]
        self.prio = [[0] * self.cols for _ in range(self.rows)]

    def set_rect(self, px, py, pw, ph, char, priority):
        c0 = max(0, int((px - self.x) / TILE_SIZE))
        c1 = min(self.cols - 1, int((px + pw - 1 - self.x) / TILE_SIZE))
        r0 = max(0, int((py - self.y) / TILE_SIZE))
        r1 = min(self.rows - 1, int((py + ph - 1 - self.y) / TILE_SIZE))
        for r in range(r0, r1 + 1):
            for c in range(c0, c1 + 1):
                if priority >= self.prio[r][c]:
                    self.grid[r][c] = char
                    self.prio[r][c] = priority

    def set_point(self, px, py, pw, ph, char, priority):
        cx, cy = px + pw / 2, py + ph / 2
        c = max(0, min(self.cols - 1, int((cx - self.x) / TILE_SIZE)))
        r = max(0, min(self.rows - 1, int((cy - self.y) / TILE_SIZE)))
        if priority >= self.prio[r][c]:
            self.grid[r][c] = char
            self.prio[r][c] = priority

    def set_cell(self, row, col, char, priority):
        if 0 <= row < self.rows and 0 <= col < self.cols:
            if priority >= self.prio[row][col]:
                self.grid[row][col] = char
                self.prio[row][col] = priority

    def render(self) -> str:
        return '\n'.join(''.join(row) for row in self.grid)


def parse_rooms(root) -> list[Room]:
    rooms = []
    for og in root.findall('objectgroup'):
        if og.get('name') == 'game_rooms':
            for obj in og.findall('object'):
                name = obj.get('name') or f"room_{obj.get('id')}"
                x, y, w, h = _bbox(obj)
                rooms.append(Room(name, x, y, w, h))
    return rooms


def rooms_containing_center(rooms, px, py, pw, ph):
    return [r for r in rooms if _center_in(px, py, pw, ph, r.x, r.y, r.w, r.h)]


def rooms_overlapping(rooms, px, py, pw, ph):
    return [r for r in rooms if _overlaps(px, py, pw, ph, r.x, r.y, r.w, r.h)]


def populate_rooms(root, rooms: list[Room]):
    for og in root.findall('objectgroup'):
        layer = og.get('name', '')

        if layer in BLOCK_LAYERS:
            for obj in og.findall('object'):
                obj_name = obj.get('name', '')
                if obj_name == 'LadderTop':
                    continue
                px, py, pw, ph = _bbox(obj)
                if obj_name == 'Ladder':
                    char = 'L'
                elif obj_name == 'Water':
                    char = 'W'
                else:
                    char = 'X'
                prio = PRIORITY[char]
                for room in rooms_overlapping(rooms, px, py, pw, ph):
                    room.set_rect(px, py, pw, ph, char, prio)

        elif layer == 'hazards':
            for obj in og.findall('object'):
                px, py, pw, ph = _bbox(obj)
                for room in rooms_containing_center(rooms, px, py, pw, ph):
                    room.set_point(px, py, pw, ph, 'D', PRIORITY['D'])

        elif layer == 'sensors':
            for obj in og.findall('object'):
                obj_name = obj.get('name', '')
                px, py, pw, ph = _bbox(obj)
                if obj_name == 'Death':
                    for room in rooms_overlapping(rooms, px, py, pw, ph):
                        room.set_rect(px, py, pw, ph, 'D', PRIORITY['D'])
                elif obj_name == 'Gate':
                    for room in rooms_overlapping(rooms, px, py, pw, ph):
                        room.set_rect(px, py, pw, ph, '\\', PRIORITY['\\'])

        elif layer == 'enemies':
            for obj in og.findall('object'):
                px, py, pw, ph = _bbox(obj)
                for room in rooms_containing_center(rooms, px, py, pw, ph):
                    room.set_point(px, py, pw, ph, 'E', PRIORITY['E'])

        elif layer == 'items':
            for obj in og.findall('object'):
                px, py, pw, ph = _bbox(obj)
                for room in rooms_containing_center(rooms, px, py, pw, ph):
                    room.set_point(px, py, pw, ph, 'I', PRIORITY['I'])

        elif layer == 'player':
            for obj in og.findall('object'):
                spawn_name = obj.get('name', '0')
                px, py, pw, ph = _bbox(obj)
                char = spawn_name if spawn_name.isdigit() else '0'
                for room in rooms_containing_center(rooms, px, py, pw, ph):
                    room.set_point(px, py, pw, ph, char, SPAWN_PRIORITY)


def detect_gates(rooms: list[Room]):
    GATE_PRIO = PRIORITY['\\']

    for i, ra in enumerate(rooms):
        for rb in rooms[i + 1:]:
            # ra right edge → rb left edge (ra is left of rb)
            if ra.x + ra.w == rb.x:
                y0 = max(ra.y, rb.y)
                y1 = min(ra.y + ra.h, rb.y + rb.h)
                for py in range(y0, y1, TILE_SIZE):
                    row_a = (py - ra.y) // TILE_SIZE
                    row_b = (py - rb.y) // TILE_SIZE
                    if ra.grid[row_a][ra.cols - 1] != 'X' and rb.grid[row_b][0] != 'X':
                        ra.set_cell(row_a, ra.cols - 1, '\\', GATE_PRIO)
                        rb.set_cell(row_b, 0, '\\', GATE_PRIO)

            # rb right edge → ra left edge (rb is left of ra)
            if rb.x + rb.w == ra.x:
                y0 = max(ra.y, rb.y)
                y1 = min(ra.y + ra.h, rb.y + rb.h)
                for py in range(y0, y1, TILE_SIZE):
                    row_a = (py - ra.y) // TILE_SIZE
                    row_b = (py - rb.y) // TILE_SIZE
                    if ra.grid[row_a][0] != 'X' and rb.grid[row_b][rb.cols - 1] != 'X':
                        ra.set_cell(row_a, 0, '\\', GATE_PRIO)
                        rb.set_cell(row_b, rb.cols - 1, '\\', GATE_PRIO)

            # ra bottom edge → rb top edge (ra is above rb)
            if ra.y + ra.h == rb.y:
                x0 = max(ra.x, rb.x)
                x1 = min(ra.x + ra.w, rb.x + rb.w)
                for px in range(x0, x1, TILE_SIZE):
                    col_a = (px - ra.x) // TILE_SIZE
                    col_b = (px - rb.x) // TILE_SIZE
                    if ra.grid[ra.rows - 1][col_a] != 'X' and rb.grid[0][col_b] != 'X':
                        ra.set_cell(ra.rows - 1, col_a, '\\', GATE_PRIO)
                        rb.set_cell(0, col_b, '\\', GATE_PRIO)

            # rb bottom edge → ra top edge (rb is above ra)
            if rb.y + rb.h == ra.y:
                x0 = max(ra.x, rb.x)
                x1 = min(ra.x + ra.w, rb.x + rb.w)
                for px in range(x0, x1, TILE_SIZE):
                    col_a = (px - ra.x) // TILE_SIZE
                    col_b = (px - rb.x) // TILE_SIZE
                    if ra.grid[0][col_a] != 'X' and rb.grid[rb.rows - 1][col_b] != 'X':
                        ra.set_cell(0, col_a, '\\', GATE_PRIO)
                        rb.set_cell(rb.rows - 1, col_b, '\\', GATE_PRIO)


def format_output(rooms: list[Room]) -> str:
    parts = []
    for room in rooms:
        std = room.cols == 16 and room.rows == 14
        size = '' if std else f' ({room.cols}×{room.rows})'
        parts.append(f'=== {room.name}{size} ===')
        parts.append(room.render())
        parts.append('')
    return '\n'.join(parts)


def analyze(tmx_path) -> str:
    tree = ET.parse(tmx_path)
    root = tree.getroot()

    rooms = parse_rooms(root)
    if not rooms:
        raise ValueError('No game_rooms layer found in TMX.')

    populate_rooms(root, rooms)
    detect_gates(rooms)
    return format_output(rooms)


def main():
    if len(sys.argv) < 2 or sys.argv[1] in ('-h', '--help'):
        print(USAGE)
        sys.exit(0 if len(sys.argv) >= 2 else 1)

    tmx_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2]) if len(sys.argv) > 2 else None

    try:
        output = analyze(tmx_path)
    except ValueError as e:
        print(str(e), file=sys.stderr)
        sys.exit(1)

    if output_path:
        output_path.write_text(output)
        print(f'Written to {output_path}')
    else:
        print(output)


if __name__ == '__main__':
    main()
