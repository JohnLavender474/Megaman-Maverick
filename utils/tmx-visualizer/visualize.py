#!/usr/bin/env python3

import argparse
import os
import sys
import xml.etree.ElementTree as ET

LAYER_PREFIX = {
    'enemies': 'E',
    'hazards': 'H',
    'items': 'I',
    'specials': 'S',
    'sensors': 'N',
}

CELL_W = 5

def _num(v):
    if v is None:
        return 0
    f = float(v)
    return int(f) if f.is_integer() else f

def parse_object(obj):
    return {
        'id': obj.get('id'),
        'name': obj.get('name'),
        'x': _num(obj.get('x')),
        'y': _num(obj.get('y')),
        'width': _num(obj.get('width')),
        'height': _num(obj.get('height')),
    }

def parse_object_layers(root):
    layers = {}
    for og in root.findall('objectgroup'):
        layers[og.get('name')] = [parse_object(o) for o in og.findall('object')]
    return layers


def camel_initials(name):
    inits = [c for c in name if c.isupper()]
    if not inits:
        inits = [name[0].upper()] if name else ['?']
    return ''.join(inits)[:3]

def derive_token(name, used):
    base = camel_initials(name)
    candidates = [base]
    letters = ''.join(c for c in name if c.isalpha())
    if letters:
        candidates.append(letters[:3].capitalize())
    trunc = base[:2] if len(base) >= 2 else base
    for d in range(1, 100):
        candidates.append((trunc + str(d))[:3])
    for c in candidates:
        if c and c not in used:
            used.add(c)
            return c
    n = 0
    while True:
        c = str(n)[:3]
        if c not in used:
            used.add(c)
            return c
        n += 1

def cell(code):
    return '[' + (code or '').center(CELL_W) + ']'

def itile(px, origin):
    return int(round((px - origin) / 32.0))

def rect_overlap(ax, ay, aw, ah, bx, by, bw, bh):
    return ax < bx + bw and ax + aw > bx and ay < by + bh and ay + ah > by

def overlaps_room(obj, room):
    return rect_overlap(obj['x'], obj['y'],
                        obj['width'] or 1, obj['height'] or 1,
                        room['x'], room['y'], room['width'], room['height'])

def build_code_map(layers):
    code_map = {}
    legend = {}
    used_per_prefix = {}

    for layer, prefix in LAYER_PREFIX.items():
        used = used_per_prefix.setdefault(prefix, set())
        for obj in layers.get(layer, []):
            name = obj['name']
            if (layer, name) in code_map:
                continue
            if name:
                token = derive_token(name, used)
                code = f'{prefix}-{token}'
                legend[code] = f'{layer} : {name}'
            else:
                code = f'{prefix}-?'
                legend[code] = f'{layer} : <unnamed>'
            code_map[(layer, name)] = code

    used_b = used_per_prefix.setdefault('B', set())
    for obj in layers.get('blocks', []):
        name = obj['name']
        if not name or ('blocks', name) in code_map:
            continue
        token = derive_token(name, used_b)
        code = f'B-{token}'
        legend[code] = f'blocks : {name}'
        code_map[('blocks', name)] = code

    player_codes = {}
    for i, obj in enumerate(layers.get('player', []), start=1):
        code = f'PS-{i}'
        player_codes[obj['id']] = code
        legend[code] = f'Player spawn #{i}'
    return code_map, player_codes, legend

def symbol_desc(sym, legend):
    if sym == 'X':
        return 'blocks : Block'
    return legend.get(sym, sym)

def combo_key(symbols, combo_state, legend):
    fs = frozenset(symbols)
    if fs in combo_state['map']:
        return combo_state['map'][fs]
    parts = sorted(symbols)
    joined = '/'.join(parts)
    if len(joined) <= CELL_W:
        key = joined
    else:
        key = '&' + str(combo_state['n'])
        combo_state['n'] += 1
    combo_state['map'][fs] = key
    combo_state['desc'][key] = [symbol_desc(s, legend) for s in parts]
    return key

def render_room(room, layers, code_map, player_codes, legend,
                combo_state, used_codes, used_combos):
    cols = max(1, int(round(room['width'] / 32.0)))
    rows = max(1, int(round(room['height'] / 32.0)))
    occ = [[[] for _ in range(cols)] for _ in range(rows)]
    anch = [[set() for _ in range(cols)] for _ in range(rows)]

    def add(sym, obj):
        c0 = itile(obj['x'], room['x'])
        r0 = itile(obj['y'], room['y'])
        w = max(1, int(round((obj['width'] or 32) / 32.0)))
        h = max(1, int(round((obj['height'] or 32) / 32.0)))
        for dr in range(h):
            for dc in range(w):
                c, r = c0 + dc, r0 + dr
                if 0 <= r < rows and 0 <= c < cols:
                    occ[r][c].append(sym)
                    if dr == 0 and dc == 0:
                        anch[r][c].add(sym)

    for obj in layers.get('blocks', []):
        if not overlaps_room(obj, room):
            continue
        name = obj['name']
        add(code_map[('blocks', name)] if name else 'X', obj)

    for layer in LAYER_PREFIX:
        for obj in layers.get(layer, []):
            if overlaps_room(obj, room):
                add(code_map[(layer, obj['name'])], obj)

    for obj in layers.get('player', []):
        if overlaps_room(obj, room):
            add(player_codes[obj['id']], obj)

    lines = [f"=== {room['name']}  origin({room['x']},{room['y']})px  "
             f"{cols}x{rows} tiles ==="]
    for r in range(rows):
        row_cells = []
        for c in range(cols):
            syms = set(occ[r][c])
            if not syms:
                code = ''
            elif len(syms) == 1:
                s = next(iter(syms))
                if s == 'X':
                    code = 'X'
                elif s in anch[r][c]:
                    code = s
                    used_codes.add(s)
                else:
                    code = '.'
                    used_codes.add(s)
            else:
                code = combo_key(syms, combo_state, legend)
                used_combos.add(code)
                for s in syms:
                    if s != 'X':
                        used_codes.add(s)
            row_cells.append(cell(code))
        lines.append(''.join(row_cells))
    return '\n'.join(lines)

def build_legend(legend, used_codes, combo_state, used_combos):
    lines = ['===== KEY =====',
             'Each cell is one 32px tile, 5 chars wide, wrapped in [ ].',
             '',
             '[     ]  empty space',
             '[  X  ]  Block (geometry)',
             '[  .  ]  continuation of a multi-tile entity (anchor is up/left)']
    dynamic = sorted(c for c in used_codes if c not in ('X', '.'))
    if dynamic:
        lines.append('')
    for code in dynamic:
        lines.append(f'{cell(code)}  {legend.get(code, "?")}')
    if used_combos:
        lines.append('')
        lines.append('-- combined (multi-object) tiles --')
        for key in sorted(used_combos):
            lines.append(f'{cell(key)}  combined tile:')
            for part in combo_state['desc'][key]:
                lines.append(f'    - {part}')
    return '\n'.join(lines)

def visualize(tmx_path, room_filter=None):
    root = ET.parse(tmx_path).getroot()
    layers = parse_object_layers(root)

    rooms = []
    for r in layers.get('game_rooms', []):
        rooms.append({'name': r['name'], 'x': r['x'], 'y': r['y'],
                      'width': r['width'], 'height': r['height']})

    code_map, player_codes, legend = build_code_map(layers)

    combo_state = {'map': {}, 'desc': {}, 'n': 1}
    used_codes = set()
    used_combos = set()
    blocks = []
    for room in rooms:
        if room_filter and room['name'] != room_filter:
            continue
        blocks.append(render_room(room, layers, code_map, player_codes,
                                  legend, combo_state, used_codes, used_combos))

    grids = '\n\n'.join(blocks) if blocks else '(no rooms to render)'
    key = build_legend(legend, used_codes, combo_state, used_combos)
    return grids, key

def output_paths_for(out_value):
    base = out_value
    for suffix in ('.viz.txt', '.legend.txt', '.txt'):
        if base.endswith(suffix):
            base = base[:-len(suffix)]
            break
    return f'{base}.viz.txt', f'{base}.legend.txt'

def main():
    ap = argparse.ArgumentParser(
        description='Render a Megaman Maverick TMX as an ASCII grid.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument('tmx', help='Path to TMX file (relative or absolute)')
    ap.add_argument('--room', help='Render just this one room')
    ap.add_argument('-o', '--output', default='-',
                    help="Output base name (default '-' for stdout). Writes "
                         "<base>.viz.txt (grids) and <base>.legend.txt (key). "
                         "e.g. -o temp/out -> temp/out.viz.txt + temp/out.legend.txt")
    args = ap.parse_args()

    if not os.path.exists(args.tmx):
        print(f'TMX not found: {args.tmx}', file=sys.stderr)
        sys.exit(1)

    grids, key = visualize(args.tmx, room_filter=args.room)

    if args.room and args.room not in grids:
        print(f"Warning: room '{args.room}' not found in {args.tmx}",
              file=sys.stderr)

    if args.output == '-':
        print(grids)
        print()
        print(key)
    else:
        grid_path, legend_path = output_paths_for(args.output)
        with open(grid_path, 'w') as f:
            f.write(grids + '\n')
        with open(legend_path, 'w') as f:
            f.write(key + '\n')
        print(f'Wrote {grid_path} and {legend_path}', file=sys.stderr)

if __name__ == '__main__':
    main()
