#!/usr/bin/env python3
import argparse
import copy
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


def _height_tiles(root) -> int:
    return int(root.get('height'))


def _row_to_y_px(root, row, h_tiles) -> float:
    return (_height_tiles(root) - row - h_tiles) * TILE


def _y_to_row(root, y_px, h_tiles) -> float:
    return _height_tiles(root) - (y_px / TILE) - h_tiles


def _find_object(root, obj_id):
    for group in root.findall('objectgroup'):
        for obj in group.findall('object'):
            if obj.get('id') == str(obj_id):
                return group, obj
    return None, None


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


def _bounds_warnings(root):
    map_w_px = int(root.get('width')) * TILE
    map_h_px = _height_tiles(root) * TILE
    warnings = []
    for group in root.findall('objectgroup'):
        for obj in group.findall('object'):
            x = float(obj.get('x', 0))
            y = float(obj.get('y', 0))
            w = float(obj.get('width', 0))
            h = float(obj.get('height', 0))
            bits = []
            if x < 0:
                bits.append(f'left edge {_fmt(-x / TILE)} tile(s) past col 0')
            if y < 0:
                bits.append(f'top edge {_fmt(-y / TILE)} tile(s) above the top of the map')
            if x + w > map_w_px:
                bits.append(f'right edge {_fmt((x + w - map_w_px) / TILE)} tile(s) past '
                            f'col {int(root.get("width"))}')
            if y + h > map_h_px:
                bits.append(f'bottom edge {_fmt((y + h - map_h_px) / TILE)} tile(s) below row 0')
            if bits:
                name = obj.get('name')
                label = f' name="{name}"' if name else ''
                warnings.append(f'  id={obj.get("id")}{label} in "{group.get("name")}": '
                                 + ', '.join(bits))
    return warnings


def _save(tree, path, dry_run=False):
    root = tree.getroot()
    _finalize(root)

    warnings = _bounds_warnings(root)
    if warnings:
        print(f'Warning: {len(warnings)} object(s) extend outside the map bounds:')
        for w in warnings:
            print(w)

    if dry_run:
        print(f'[dry-run] no changes written to {path}')
        return

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
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Initialized {args.file} ({args.width}×{args.height} tiles) '
          f'with layers: {", ".join(DEFAULT_LAYERS)}')


def cmd_add_room(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'game_rooms')
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, args.h),
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added room "{args.name}" at col={args.col} row={args.row} '
          f'({args.w}×{args.h} tiles)')


def cmd_add_block(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'blocks')
    _add_object(root, group, {
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, args.h),
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added block at col={args.col} row={args.row} ({args.w}×{args.h} tiles)')


def cmd_add_water(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'specials')
    _add_object(root, group, {
        'name': 'Water',
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, args.h),
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added Water at col={args.col} row={args.row} ({args.w}×{args.h} tiles)')


def cmd_add_death(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'sensors')
    _add_object(root, group, {
        'name': 'Death',
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, args.h),
        'width': args.w * TILE, 'height': args.h * TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added Death at col={args.col} row={args.row} ({args.w}×{args.h} tiles)')


def cmd_add_spawn(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, 'player')
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, 1),
        'width': TILE, 'height': TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added spawn "{args.name}" at col={args.col} row={args.row}')


def cmd_add_marker(args):
    tree, root = _load(args.file)
    group = _find_or_create_group(root, args.layer)
    _add_object(root, group, {
        'name': args.name,
        'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, 1),
        'width': TILE, 'height': TILE,
    })
    _save(tree, args.file, dry_run=args.dry_run)
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
        h_tiles = float(o.get('height', 0)) / TILE
        row = _fmt(_y_to_row(root, float(o.get('y', 0)), h_tiles))
        w = _fmt(float(o.get('width', 0)) / TILE)
        h = _fmt(h_tiles)
        name = o.get('name', '')
        label = f' name="{name}"' if name else ''
        print(f'  id={o.get("id")}{label} col={col} row={row} w={w} h={h}')


def cmd_remove(args):
    tree, root = _load(args.file)
    group, obj = _find_object(root, args.id)
    if obj is None:
        print(f'No object with id={args.id}.', file=sys.stderr)
        sys.exit(1)
    group.remove(obj)
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Removed object id={args.id} from "{group.get("name")}"')


def cmd_resize(args):
    if args.w is None and args.h is None:
        print('Specify at least one of --w or --h.', file=sys.stderr)
        sys.exit(1)
    tree, root = _load(args.file)
    group, obj = _find_object(root, args.id)
    if obj is None:
        print(f'No object with id={args.id}.', file=sys.stderr)
        sys.exit(1)
    old_w = _fmt(float(obj.get('width', 0)) / TILE)
    old_h = _fmt(float(obj.get('height', 0)) / TILE)
    if args.w is not None:
        obj.set('width', str(args.w * TILE))
    if args.h is not None:
        old_y = float(obj.get('y', 0))
        old_h_px = float(obj.get('height', 0))
        bottom_px = old_y + old_h_px
        new_h_px = args.h * TILE
        obj.set('y', _fmt(bottom_px - new_h_px))
        obj.set('height', str(new_h_px))
    _save(tree, args.file, dry_run=args.dry_run)
    new_w = args.w if args.w is not None else old_w
    new_h = args.h if args.h is not None else old_h
    print(f'Resized object id={args.id} in "{group.get("name")}" '
          f'from {old_w}×{old_h} to {new_w}×{new_h} tiles (bottom-left unchanged)')


def _translate_dimension(args, pos_attr, size_attr, anchor_far):
    tree, root = _load(args.file)
    group, obj = _find_object(root, args.id)
    if obj is None:
        print(f'No object with id={args.id}.', file=sys.stderr)
        sys.exit(1)
    old_pos = float(obj.get(pos_attr, 0))
    old_size = float(obj.get(size_attr, 0))
    new_size = old_size + args.by * TILE
    if new_size < TILE:
        print(f'Resulting {size_attr} would be less than 1 tile; aborting.', file=sys.stderr)
        sys.exit(1)
    if anchor_far:
        new_pos = (old_pos + old_size) - new_size
    else:
        new_pos = old_pos
    obj.set(pos_attr, _fmt(new_pos))
    obj.set(size_attr, _fmt(new_size))
    _save(tree, args.file, dry_run=args.dry_run)
    return group, old_size / TILE, new_size / TILE


def cmd_translate_width(args):
    group, old_w, new_w = _translate_dimension(args, 'x', 'width', anchor_far=args.leftward)
    anchor = 'right edge fixed' if args.leftward else 'left edge fixed'
    print(f'Translated width of object id={args.id} in "{group.get("name")}" '
          f'from {_fmt(old_w)} to {_fmt(new_w)} tiles ({anchor})')


def cmd_translate_height(args):
    group, old_h, new_h = _translate_dimension(args, 'y', 'height', anchor_far=not args.downward)
    anchor = 'top edge fixed' if args.downward else 'bottom edge fixed'
    print(f'Translated height of object id={args.id} in "{group.get("name")}" '
          f'from {_fmt(old_h)} to {_fmt(new_h)} tiles ({anchor})')


def cmd_translate_x(args):
    tree, root = _load(args.file)
    group, obj = _find_object(root, args.id)
    if obj is None:
        print(f'No object with id={args.id}.', file=sys.stderr)
        sys.exit(1)
    obj.set('x', _fmt(float(obj.get('x', 0)) + args.by * TILE))
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Translated object id={args.id} in "{group.get("name")}" '
          f'{args.by:+d} col(s)')


def cmd_translate_y(args):
    tree, root = _load(args.file)
    group, obj = _find_object(root, args.id)
    if obj is None:
        print(f'No object with id={args.id}.', file=sys.stderr)
        sys.exit(1)
    obj.set('y', _fmt(float(obj.get('y', 0)) - args.by * TILE))
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Translated object id={args.id} in "{group.get("name")}" '
          f'{args.by:+d} row(s)')


def _shift_objects(root, axis, threshold_px, delta_px):
    pos_attr = 'x' if axis == 'col' else 'y'
    size_attr = 'width' if axis == 'col' else 'height'
    splits = []
    for group in root.findall('objectgroup'):
        for obj in list(group.findall('object')):
            pos = float(obj.get(pos_attr, 0))
            size = float(obj.get(size_attr, 0))
            if pos >= threshold_px:
                obj.set(pos_attr, _fmt(pos + delta_px))
            elif pos + size > threshold_px:
                near_size = threshold_px - pos
                far_pos = threshold_px + delta_px
                far_size = pos + size - threshold_px

                obj.set(size_attr, _fmt(near_size))

                far = copy.deepcopy(obj)
                far.set('id', str(_max_object_id(root) + 1))
                far.set(pos_attr, _fmt(far_pos))
                far.set(size_attr, _fmt(far_size))
                group.append(far)

                splits.append(
                    f'  id={obj.get("id")} in "{group.get("name")}" split: '
                    f'near {size_attr}={_fmt(near_size / TILE)} tiles, '
                    f'far id={far.get("id")} {pos_attr}={_fmt(far_pos / TILE)} '
                    f'{size_attr}={_fmt(far_size / TILE)} tiles')
    return splits


def _report_splits(splits, at, unit):
    if splits:
        print(f'Split {len(splits)} straddling object(s) at {unit} {at}:')
        for s in splits:
            print(s)


def cmd_insert_cols(args):
    tree, root = _load(args.file)
    threshold = args.at_col * TILE
    delta = args.count * TILE

    splits = _shift_objects(root, 'col', threshold, delta)

    # grow map + shift tile-layer columns
    root.set('width', str(int(root.get('width')) + args.count))
    for layer in root.findall('layer'):
        data, w, h, rows = _layer_grid(layer)
        c = min(max(args.at_col, 0), w)
        rows = [r[:c] + ['0'] * args.count + r[c:] for r in rows]
        layer.set('width', str(w + args.count))
        _write_grid(data, rows)

    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Inserted {args.count} column(s) at col {args.at_col}; '
          f'objects fully past shifted right by {args.count} tiles.')
    _report_splits(splits, args.at_col, 'col')


def cmd_insert_rows(args):
    tree, root = _load(args.file)
    map_h = _height_tiles(root)
    threshold = (map_h - args.at_row) * TILE
    delta = args.count * TILE

    splits = _shift_objects(root, 'row', threshold, delta)

    # grow map + shift tile-layer rows
    root.set('height', str(map_h + args.count))
    for layer in root.findall('layer'):
        data, w, h, rows = _layer_grid(layer)
        r = min(max(h - args.at_row, 0), h)
        blank = [['0'] * w for _ in range(args.count)]
        rows = rows[:r] + blank + rows[r:]
        layer.set('height', str(h + args.count))
        _write_grid(data, rows)

    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Inserted {args.count} row(s) below row {args.at_row}; '
          f'row {args.at_row} and everything above it shifts up by {args.count} '
          f'(row number increases); objects below row {args.at_row} keep their row number.')
    _report_splits(splits, args.at_row, 'row')


def _add_rect_args(p, name=False):
    if name:
        p.add_argument('name')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    p.add_argument('--w', type=int, required=True)
    p.add_argument('--h', type=int, required=True)


def _add_dry_run_arg(p):
    p.add_argument('--dry-run', action='store_true',
                   help='print what would change (including any out-of-bounds warnings) '
                        'without writing the file')


def build_parser():
    parser = argparse.ArgumentParser(
        prog='level_builder.py',
        description='Build and edit Megaman-Maverick TMX levels (tile-unit coordinates). '
                     'Row 0 is the bottom of the map; row numbers increase upward.')
    sub = parser.add_subparsers(dest='command', required=True)

    p = sub.add_parser('init', help='create a fresh minimal TMX')
    p.add_argument('file')
    p.add_argument('--width', type=int, required=True, help='map width in tiles')
    p.add_argument('--height', type=int, required=True, help='map height in tiles')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_init)

    p = sub.add_parser('add-room', help='add a game_rooms rectangle')
    p.add_argument('file')
    _add_rect_args(p, name=True)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_room)

    p = sub.add_parser('add-block', help='add a blocks rectangle (may span rooms)')
    p.add_argument('file')
    _add_rect_args(p)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_block)

    p = sub.add_parser('add-water', help='add a Water rectangle to specials')
    p.add_argument('file')
    _add_rect_args(p)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_water)

    p = sub.add_parser('add-death', help='add a Death rectangle to sensors')
    p.add_argument('file')
    _add_rect_args(p)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_death)

    p = sub.add_parser('add-spawn', help='add a single-tile player spawn')
    p.add_argument('file')
    p.add_argument('name', help='"0" for stage start, "1".."n" for respawns')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_spawn)

    p = sub.add_parser('add-marker',
                       help='add a single-tile name-only marker (no props)')
    p.add_argument('file')
    p.add_argument('--layer', required=True, choices=MARKER_LAYERS)
    p.add_argument('--name', required=True,
                   help='e.g. TODO, "?", or a direct name like SniperJoe')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_marker)

    p = sub.add_parser('list', help='list objects in a layer (with ids)')
    p.add_argument('file')
    p.add_argument('layer')
    p.set_defaults(func=cmd_list)

    p = sub.add_parser('remove', help='remove an object by id')
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_remove)

    p = sub.add_parser('resize',
                       help='resize a room/block/object by id, keeping bottom-left fixed')
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.add_argument('--w', type=int, help='new width in tiles')
    p.add_argument('--h', type=int, help='new height in tiles')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_resize)

    p = sub.add_parser('translate-width',
                       help="grow/shrink an object's width by --by tiles from its current size")
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.add_argument('--by', type=int, required=True,
                   help='tiles to add to the width (negative to shrink); '
                        'resulting width must be >= 1 tile')
    g = p.add_mutually_exclusive_group()
    g.add_argument('--leftward', action='store_true',
                   help='grow/shrink from the left edge (right edge stays fixed)')
    g.add_argument('--rightward', action='store_true',
                   help='grow/shrink from the right edge (left edge stays fixed) [default]')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_translate_width)

    p = sub.add_parser('translate-height',
                       help="grow/shrink an object's height by --by tiles from its current size")
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.add_argument('--by', type=int, required=True,
                   help='tiles to add to the height (negative to shrink); '
                        'resulting height must be >= 1 tile')
    g = p.add_mutually_exclusive_group()
    g.add_argument('--upward', action='store_true',
                   help='grow/shrink from the top edge (bottom edge stays fixed) [default]')
    g.add_argument('--downward', action='store_true',
                   help='grow/shrink from the bottom edge (top edge stays fixed)')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_translate_height)

    p = sub.add_parser('translate-x', help='move an object horizontally by --by tiles')
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.add_argument('--by', type=int, required=True,
                   help='tiles to move right (negative to move left)')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_translate_x)

    p = sub.add_parser('translate-y', help='move an object vertically by --by tiles')
    p.add_argument('file')
    p.add_argument('--id', type=int, required=True)
    p.add_argument('--by', type=int, required=True,
                   help='tiles to move up (negative to move down); row 0 is the bottom of the map')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_translate_y)

    p = sub.add_parser('insert-cols',
                       help='open a vertical gap; shift everything fully past it right')
    p.add_argument('file')
    p.add_argument('--at-col', type=int, required=True)
    p.add_argument('--count', type=int, required=True, help='columns to insert')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_insert_cols)

    p = sub.add_parser('insert-rows',
                       help='open a horizontal gap below --at-row; row --at-row and above '
                            'gain to their row number, everything below keeps its row number')
    p.add_argument('file')
    p.add_argument('--at-row', type=int, required=True,
                   help='row 0 is the bottom of the map; new rows are inserted below this row')
    p.add_argument('--count', type=int, required=True, help='rows to insert')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_insert_rows)

    return parser


def main():
    args = build_parser().parse_args()
    args.func(args)


if __name__ == '__main__':
    main()
