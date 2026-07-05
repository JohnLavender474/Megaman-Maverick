#!/usr/bin/env python3
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

USAGE = 'Usage: level_analyzer.py <path/to/level.tmx> [output.txt]'

HELP = """\
level_analyzer.py — analyze a Tiled TMX level into plain text and/or JSON.

USAGE
  level_analyzer.py <level.tmx>              Print the plain-text room grid to stdout.
  level_analyzer.py <level.tmx> <out.txt>    Write <out.txt> AND a sibling <out.json>.
  level_analyzer.py -h | --help              Show this help.

TMX files live under assets/tiled_maps/tmx/. Stdlib-only, no venv needed.

OUTPUTS
  Plain text  One ASCII grid per room (one tile = one char). Rooms are labelled
              "=== <name> ===", with "(cols×rows)" appended for non-standard
              sizes (standard room = 16×14 = 512×448 px).
  JSON        Written only when an output path is given, as <out>.json beside it.
              Shape: {"data": {"map": {"layers": {...}}}} where layers holds:
                game_rooms  dict keyed by room name -> {x, y, width, height,
                            entities}. entities maps a layer name -> [object ids]
                            present in that room (populated layers only).
                entity layers (blocks, specials, hazards, sensors, enemies,
                            items, player) -> list of objects, each:
                            {id, name, x, y, width, height, game_rooms,
                             properties}. Coords are raw TMX pixels; game_rooms
                             is the list of room names the object's bounding box
                             OVERLAPS (so one object can belong to several rooms);
                             properties is [{name, value, type}] (raw TMX values,
                             type defaults to "string").

LAYERS ANALYZED
  Only game_rooms, blocks, specials, hazards, sensors, enemies, items, player.
  All other object groups (children, backgrounds, decorations, ...) are ignored
  by both outputs.

PLAIN-TEXT CHARACTER LEGEND
  X  block/platform      L  ladder           W  water        E  enemy
  D  death hazard        I  item             \\  gate/opening  0  player start
  1..9  respawn point    (space)  air

  Gates (\\) are also auto-detected: any tile on a shared edge between two rooms
  that is not blocked by X on both sides is marked \\. Direction arrows from the
  level-designer format are NOT emitted; infer room flow by hand.
"""

TILE_SIZE = 32

# Layers whose Ladder-named objects become L; everything else becomes X
BLOCK_LAYERS = ('blocks', 'specials')

# Entity layers included in the JSON output, in emission order. Same set the
# plain-text visualization is concerned with (minus game_rooms, handled apart).
ENTITY_LAYERS = ('blocks', 'specials', 'hazards', 'sensors', 'enemies', 'items', 'player')

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


def _num(v: float):
    """Return an int when the value is integral, else a float (clean JSON)."""
    return int(v) if float(v).is_integer() else v


def _effective_bbox(obj):
    """Bounding box for overlap tests, expanding shape-only objects.

    Rectangles use their width/height. A polygon/polyline object (no width or
    height) uses the bounding box of its points, offset by the object origin.
    A zero-size object stays a point.
    """
    x, y, w, h = _bbox(obj)
    if w == 0 and h == 0:
        shape = obj.find('polygon')
        if shape is None:
            shape = obj.find('polyline')
        if shape is not None and shape.get('points'):
            pts = [p.split(',') for p in shape.get('points').split()]
            xs = [float(a) for a, b in pts]
            ys = [float(b) for a, b in pts]
            return x + min(xs), y + min(ys), max(xs) - min(xs), max(ys) - min(ys)
    return x, y, w, h


def _properties(obj) -> list:
    """Extract an object's Tiled properties as {name, value, type} entries.

    Values are kept as their raw TMX string; Tiled's default (string) type is
    made explicit when the type attribute is omitted.
    """
    props = []
    node = obj.find('properties')
    if node is None:
        return props
    for p in node.findall('property'):
        props.append({
            'name': p.get('name', ''),
            'value': p.get('value', ''),
            'type': p.get('type', 'string'),
        })
    return props


def _unique_room_names(rooms: list[Room]) -> list[str]:
    """Room names for JSON keys, suffixing duplicates (_2, _3, ...)."""
    seen = {}
    names = []
    for room in rooms:
        base = room.name
        seen[base] = seen.get(base, 0) + 1
        names.append(base if seen[base] == 1 else f'{base}_{seen[base]}')
    return names


def build_json(root, rooms: list[Room]) -> dict:
    room_names = _unique_room_names(rooms)

    # room name -> {layer -> [ids]}, only populated layers appear
    room_entities = {name: {} for name in room_names}
    layers = {}

    for og in root.findall('objectgroup'):
        layer = og.get('name', '')
        if layer not in ENTITY_LAYERS:
            continue

        entries = []
        for obj in og.findall('object'):
            oid = int(obj.get('id'))
            bx, by, bw, bh = _effective_bbox(obj)
            member_names = []
            for room, name in zip(rooms, room_names):
                if _overlaps(bx, by, bw, bh, room.x, room.y, room.w, room.h):
                    member_names.append(name)
                    room_entities[name].setdefault(layer, []).append(oid)

            x, y, w, h = _bbox(obj)
            entries.append({
                'id': oid,
                'name': obj.get('name', ''),
                'x': _num(x), 'y': _num(y),
                'width': _num(w), 'height': _num(h),
                'game_rooms': member_names,
                'properties': _properties(obj),
            })
        layers[layer] = entries

    game_rooms = {}
    for room, name in zip(rooms, room_names):
        game_rooms[name] = {
            'x': room.x, 'y': room.y,
            'width': room.w, 'height': room.h,
            'entities': room_entities[name],
        }

    # game_rooms first, then entity layers in ENTITY_LAYERS order
    ordered = {'game_rooms': game_rooms}
    for layer in ENTITY_LAYERS:
        if layer in layers:
            ordered[layer] = layers[layer]

    return {'data': {'map': {'layers': ordered}}}


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


def analyze_json(tmx_path) -> dict:
    tree = ET.parse(tmx_path)
    root = tree.getroot()

    rooms = parse_rooms(root)
    if not rooms:
        raise ValueError('No game_rooms layer found in TMX.')

    return build_json(root, rooms)


def main():
    if len(sys.argv) >= 2 and sys.argv[1] in ('-h', '--help'):
        print(HELP)
        sys.exit(0)
    if len(sys.argv) < 2:
        print(USAGE, file=sys.stderr)
        sys.exit(1)

    tmx_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2]) if len(sys.argv) > 2 else None

    try:
        output = analyze(tmx_path)
        json_data = analyze_json(tmx_path) if output_path else None
    except ValueError as e:
        print(str(e), file=sys.stderr)
        sys.exit(1)

    if output_path:
        output_path.write_text(output)
        print(f'Written to {output_path}')
        json_path = output_path.with_suffix('.json')
        json_path.write_text(json.dumps(json_data, indent=2) + '\n')
        print(f'Written to {json_path}')
    else:
        print(output)


if __name__ == '__main__':
    main()
