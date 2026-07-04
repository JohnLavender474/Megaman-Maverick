#!/usr/bin/env python3
import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

TILE = 32

# Object layers a fresh level always has (order preserved in the file)
DEFAULT_LAYERS = ('game_rooms', 'blocks', 'player', 'enemies', 'hazards', 'TODO')

# Layers that hold single-tile name-only markers via `add-marker`
MARKER_LAYERS = ('enemies', 'hazards', 'TODO')


def _fmt(n) -> str:
    n = float(n)
    return str(int(n)) if n == int(n) else str(n)


def _max_object_id(root) -> int:
    ids = [int(o.get('id')) for o in root.iter('object') if o.get('id')]
    return max(ids) if ids else 0


def _max_layer_id(root) -> int:
    ids = []
    for tag in ('layer', 'objectgroup', 'imagelayer', 'group'):
        ids += [int(e.get('id')) for e in root.iter(tag) if e.get('id')]
    return max(ids) if ids else 0


def _finalize(root):
    root.set('nextobjectid', str(_max_object_id(root) + 1))
    root.set('nextlayerid', str(_max_layer_id(root) + 1))


def _find_group(root, name):
    for og in root.findall('objectgroup'):
        if og.get('name') == name:
            return og
    return None


def _find_or_create_group(root, name):
    og = _find_group(root, name)
    if og is None:
        og = ET.SubElement(root, 'objectgroup')
        og.set('id', str(_max_layer_id(root) + 1))
        og.set('name', name)
    return og


def _add_object(root, group, attribs):
    obj = ET.SubElement(group, 'object')
    obj.set('id', str(_max_object_id(root) + 1))
    for key, val in attribs.items():
        obj.set(key, str(val))
    return obj


def _load(path):
    tree = ET.parse(path)
    return tree, tree.getroot()


def _save(tree, path):
    root = tree.getroot()
    _finalize(root)
    ET.indent(tree, space=' ')
    xml = ET.tostring(root, encoding='unicode').replace(' />', '/>')
    header = '<?xml version="1.0" encoding="UTF-8"?>\n'
    Path(path).write_text(header + xml + '\n', encoding='utf-8')


# ---- tile-layer CSV helpers (for insert-cols / insert-rows) ----

def _layer_grid(layer):
    w = int(layer.get('width'))
    h = int(layer.get('height'))
    data = layer.find('data')
    flat = [v for v in data.text.replace('\n', '').split(',') if v.strip() != '']
    rows = [flat[i * w:(i + 1) * w] for i in range(h)]
    return data, w, h, rows


def _write_grid(data, rows):
    body = ',\n'.join(','.join(r) for r in rows)
    data.text = '\n' + body + '\n'


# ---- commands ----

def cmd_init(args):
    root = ET.Element('map', {
        'version': '1.10', 'tiledversion': '1.12.2',
        'orientation': 'orthogonal', 'renderorder': 'right-down',
        'width': str(args.width), 'height': str(args.height),
        'tilewidth': str(TILE), 'tileheight': str(TILE),
        'infinite': '0', 'nextlayerid': '1', 'nextobjectid': '1',
    })
    ET.SubElement(root, 'tileset', {'firstgid': '1', 'source': '../tsx/Tileset1.tsx'})
    for i, name in enumerate(DEFAULT_LAYERS, start=1):
        ET.SubElement(root, 'objectgroup', {'id': str(i), 'name': name})
    tree = ET.ElementTree(root)
    _save(tree, args.file)
    print(f'Initialized {args.file} ({args.width}×{args.height} tiles) '
          f'with layers: {", ".join(DEFAULT_LAYERS)}')


def cmd_add_room(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'game_rooms')
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': args.row * TILE,
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file)
    print(f'Added room "{args.name}" at col={args.col} row={args.row} '
          f'({args.w}×{args.h} tiles)')


def cmd_add_block(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'blocks')
    _add_object(root, group, {
        'x': args.col * TILE, 'y': args.row * TILE,
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file)
    print(f'Added block at col={args.col} row={args.row} ({args.w}×{args.h} tiles)')


def cmd_add_spawn(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'player')
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': args.row * TILE,
        'width': TILE, 'height': TILE,
    })
    _save(tree, args.file)
    print(f'Added spawn "{args.name}" at col={args.col} row={args.row}')


def cmd_add_marker(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, args.layer)
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': args.row * TILE,
        'width': TILE, 'height': TILE,
    })
    _save(tree, args.file)
    print(f'Added marker "{args.name}" in {args.layer} '
          f'at col={args.col} row={args.row}')


def cmd_list(args):
    _, root = _load(args.file)
    group = _find_group(root, args.layer)
    if group is None:
        print(f'No layer named "{args.layer}".', file=sys.stderr)
        sys.exit(1)
    objs = group.findall('object')
    if not objs:
        print(f'Layer "{args.layer}" is empty.')
        return
    print(f'Layer "{args.layer}" ({len(objs)} objects):')
    for o in objs:
        col = _fmt(float(o.get('x', 0)) / TILE)
        row = _fmt(float(o.get('y', 0)) / TILE)
        w = _fmt(float(o.get('width', 0)) / TILE)
        h = _fmt(float(o.get('height', 0)) / TILE)
        name = o.get('name', '')
        label = f' name="{name}"' if name else ''
        print(f'  id={o.get("id")}{label} col={col} row={row} w={w} h={h}')


def cmd_remove(args):
    tree, root = _load(args.file)
    for group in root.findall('objectgroup'):
        for obj in group.findall('object'):
            if obj.get('id') == str(args.id):
                group.remove(obj)
                _save(tree, args.file)
                print(f'Removed object id={args.id} from "{group.get("name")}"')
                return
    print(f'No object with id={args.id}.', file=sys.stderr)
    sys.exit(1)


def _shift_objects(root, axis, threshold_px, delta_px):
    """Shift objects fully at/past threshold; return list of straddler descriptions."""
    pos_attr = 'x' if axis == 'col' else 'y'
    size_attr = 'width' if axis == 'col' else 'height'
    straddlers = []
    for group in root.findall('objectgroup'):
        for obj in group.findall('object'):
            pos = float(obj.get(pos_attr, 0))
            size = float(obj.get(size_attr, 0))
            if pos >= threshold_px:
                obj.set(pos_attr, _fmt(pos + delta_px))
            elif pos + size > threshold_px:
                straddlers.append(
                    f'  id={obj.get("id")} in "{group.get("name")}" '
                    f'({pos_attr}={_fmt(pos / TILE)} {size_attr}={_fmt(size / TILE)} tiles)')
    return straddlers


def _report_straddlers(straddlers, at, unit):
    if straddlers:
        print(f'Left {len(straddlers)} straddling object(s) in place at {unit} {at}:')
        for s in straddlers:
            print(s)


def cmd_insert_cols(args):
    tree, root = _load(args.file)
    threshold = args.at_col * TILE
    delta = args.count * TILE

    straddlers = _shift_objects(root, 'col', threshold, delta)

    # grow map + shift tile-layer columns
    root.set('width', str(int(root.get('width')) + args.count))
    for layer in root.findall('layer'):
        data, w, h, rows = _layer_grid(layer)
        c = min(max(args.at_col, 0), w)
        rows = [r[:c] + ['0'] * args.count + r[c:] for r in rows]
        layer.set('width', str(w + args.count))
        _write_grid(data, rows)

    _save(tree, args.file)
    print(f'Inserted {args.count} column(s) at col {args.at_col}; '
          f'objects fully past shifted right by {args.count} tiles.')
    _report_straddlers(straddlers, args.at_col, 'col')


def cmd_insert_rows(args):
    tree, root = _load(args.file)
    threshold = args.at_row * TILE
    delta = args.count * TILE

    straddlers = _shift_objects(root, 'row', threshold, delta)

    # grow map + shift tile-layer rows
    root.set('height', str(int(root.get('height')) + args.count))
    for layer in root.findall('layer'):
        data, w, h, rows = _layer_grid(layer)
        r = min(max(args.at_row, 0), h)
        blank = [['0'] * w for _ in range(args.count)]
        rows = rows[:r] + blank + rows[r:]
        layer.set('height', str(h + args.count))
        _write_grid(data, rows)

    _save(tree, args.file)
    print(f'Inserted {args.count} row(s) at row {args.at_row}; '
          f'objects fully past shifted down by {args.count} tiles.')
    _report_straddlers(straddlers, args.at_row, 'row')


def _add_rect_args(p, name=False):
    if name:
        p.add_argument('name')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    p.add_argument('--w', type=int, required=True)
    p.add_argument('--h', type=int, required=True)


def build_parser():
    parser = argparse.ArgumentParser(
        prog='level_builder.py',
        description='Build and edit Megaman-Maverick TMX levels (tile-unit coordinates).')
    sub = parser.add_subparsers(dest='command', required=True)

    p = sub.add_parser('init', help='create a fresh minimal TMX')
    p.add_argument('file')
    p.add_argument('--width', type=int, required=True, help='map width in tiles')
    p.add_argument('--height', type=int, required=True, help='map height in tiles')
    p.set_defaults(func=cmd_init)

    p = sub.add_parser('add-room', help='add a game_rooms rectangle')
    p.add_argument('file')
    _add_rect_args(p, name=True)
    p.set_defaults(func=cmd_add_room)

    p = sub.add_parser('add-block', help='add a blocks rectangle (may span rooms)')
    p.add_argument('file')
    _add_rect_args(p)
    p.set_defaults(func=cmd_add_block)

    p = sub.add_parser('add-spawn', help='add a single-tile player spawn')
    p.add_argument('file')
    p.add_argument('name', help='"0" for stage start, "1".."n" for respawns')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    p.set_defaults(func=cmd_add_spawn)

    p = sub.add_parser('add-marker',
                       help='add a single-tile name-only marker (no props)')
    p.add_argument('file')
    p.add_argument('--layer', required=True, choices=MARKER_LAYERS)
    p.add_argument('--name', required=True,
                   help='e.g. TODO, "?", or a direct name like SniperJoe')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    p.set_defaults(func=cmd_add_marker)

    p = sub.add_parser('list', help='list objects in a layer (with ids)')
    p.add_argument('file')
    p.add_argument('layer')
    p.set_defaults(func=cmd_list)

    p = sub.add_parser('remove', help='remove an object by id')
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.set_defaults(func=cmd_remove)

    p = sub.add_parser('insert-cols',
                       help='open a vertical gap; shift everything fully past it right')
    p.add_argument('file')
    p.add_argument('--at-col', type=int, required=True)
    p.add_argument('--count', type=int, required=True, help='columns to insert')
    p.set_defaults(func=cmd_insert_cols)

    p = sub.add_parser('insert-rows',
                       help='open a horizontal gap; shift everything fully past it down')
    p.add_argument('file')
    p.add_argument('--at-row', type=int, required=True)
    p.add_argument('--count', type=int, required=True, help='rows to insert')
    p.set_defaults(func=cmd_insert_rows)

    return parser


def main():
    args = build_parser().parse_args()
    args.func(args)


if __name__ == '__main__':
    main()
