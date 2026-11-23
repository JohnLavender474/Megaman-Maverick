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

LADDER_TOP_HEIGHT = 8

GATE_SIDES = ('left', 'right', 'up', 'down')
GATE_DEFAULT_SIDE = 'left'
GATE_LONG_TILES = 3
GATE_SHORT_TILES = 2

UNLOCK_KEY_PROPS = ('boss_key', 'gauntlet_key')


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


def cmd_add_ladder(args):
    tree, root = _load(args.file)
    x = args.col * TILE
    y = _row_to_y_px(root, args.row, args.h)
    ladder = _add_object(root, _find_or_create_group(root, 'specials'), {
        'name': 'Ladder', 'x': x, 'y': y, 'width': TILE, 'height': args.h * TILE,
    })
    top = _add_object(root, _find_or_create_group(root, 'blocks'), {
        'name': 'LadderTop', 'x': x, 'y': y, 'width': TILE, 'height': LADDER_TOP_HEIGHT,
    })
    _save(tree, args.file, dry_run=args.dry_run)
    print(f'Added Ladder id={ladder.get("id")} at col={args.col} row={args.row} '
          f'(1×{args.h} tiles) and LadderTop id={top.get("id")} at its top')


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


def _fail(msg):
    print(msg, file=sys.stderr)
    sys.exit(1)


def _rect(obj):
    return (float(obj.get('x', 0)), float(obj.get('y', 0)),
            float(obj.get('width', 0)), float(obj.get('height', 0)))


def _props(obj):
    props = obj.find('properties')
    if props is None:
        return {}
    return {p.get('name'): p for p in props.findall('property')}


def _set_props(obj, values):
    props = obj.find('properties')
    if props is None:
        props = ET.Element('properties')
        obj.insert(0, props)
    for name, value, typ in values:
        prop = ET.SubElement(props, 'property', {'name': name})
        if typ is not None:
            prop.set('type', typ)
        prop.set('value', str(value))
    ordered = sorted(props.findall('property'), key=lambda p: p.get('name'))
    for p in list(props):
        props.remove(p)
    props.extend(ordered)


def _find_room(root, name):
    group = _find_group(root, 'game_rooms')
    if group is None:
        return None
    for obj in group.findall('object'):
        if obj.get('name') == name:
            return obj
    return None


def _is_boss_room(room):
    event = _props(room).get('event')
    return event is not None and event.get('value') == 'boss'


def _gate_size_px(horizontal):
    if horizontal:
        return GATE_SHORT_TILES * TILE, GATE_LONG_TILES * TILE
    return GATE_LONG_TILES * TILE, GATE_SHORT_TILES * TILE


def _gate_rooms(root, x, y, horizontal):
    w, h = _gate_size_px(horizontal)
    group = _find_group(root, 'game_rooms')
    rooms = group.findall('object') if group is not None else []
    near, far = [], []
    if horizontal:
        edge = x + w / 2
        for room in rooms:
            rx, ry, rw, rh = _rect(room)
            if ry > y or ry + rh < y + h:
                continue
            if rx + rw == edge:
                near.append(room)
            elif rx == edge:
                far.append(room)
        return {'left': near, 'right': far}
    edge = y + h / 2
    for room in rooms:
        rx, ry, rw, rh = _rect(room)
        if rx > x or rx + rw < x + w:
            continue
        if ry + rh == edge:
            near.append(room)
        elif ry == edge:
            far.append(room)
    return {'up': near, 'down': far}


def _gate_fit(root, col, row, horizontal):
    w, h = _gate_size_px(horizontal)
    x = col * TILE
    y = _row_to_y_px(root, row, h / TILE)
    sides = _gate_rooms(root, x, y, horizontal)
    if all(len(rooms) == 1 for rooms in sides.values()):
        return x, y, w, h, {side: rooms[0] for side, rooms in sides.items()}
    return None


OPPOSITE_SIDE = {'left': 'right', 'right': 'left', 'up': 'down', 'down': 'up'}


def _gate_edge_hint(horizontal):
    if horizontal:
        return ('a left/right gate is 2x3 tiles; its --col is the edge column - 1, and both rooms '
                'must cover its 3 rows')
    return ('an up/down gate is 3x2 tiles; its --row is the edge row - 1, and both rooms '
            'must cover its 3 columns')


def _resolve_gate(root, col, row, to_room, from_side=None):
    if _find_room(root, to_room) is None:
        _fail(f'No room named "{to_room}" in game_rooms.')
    orientations = [from_side in ('left', 'right')] if from_side else [True, False]
    fits = [(hz, _gate_fit(root, col, row, hz)) for hz in orientations]
    fits = [(hz, fit) for hz, fit in fits if fit is not None]
    if not fits:
        hints = '; '.join(_gate_edge_hint(hz) for hz in orientations)
        _fail(f'Gate at col={col} row={row} does not sit on the shared edge of exactly 2 rooms: {hints}.')
    if len(fits) > 1:
        _fail(f'Gate at col={col} row={row} fits both a left/right and an up/down room edge; '
              f'give --from to choose.')
    horizontal, (x, y, w, h, sides) = fits[0]
    target_side = next((s for s, room in sides.items() if room.get('name') == to_room), None)
    if target_side is None:
        names = ', '.join(f'"{room.get("name")}"' for room in sides.values())
        _fail(f'Gate at col={col} row={row} is between rooms {names}; '
              f'--to "{to_room}" must be one of them.')
    derived_from = OPPOSITE_SIDE[target_side]
    if from_side is not None and from_side != derived_from:
        _fail(f'--from {from_side} does not match the rooms: "{to_room}" is on the {target_side} '
              f'side of the edge, so the player comes from the {derived_from} side '
              f'(--from {derived_from}).')
    source = sides[derived_from]
    return x, y, w, h, derived_from, source


def _check_gate_key(root, to_room, source, boss_key):
    target = _find_room(root, to_room)
    if boss_key is not None and _is_boss_room(target):
        _fail(f'Refused: "{to_room}" is a boss room (event=boss), so this is its entry gate. A gate '
              f'with boss_key stays locked until the fight sends UNLOCK_GATE after the boss is '
              f'defeated; only the exit gate of a boss room takes the key.')
    warnings = []
    if _is_boss_room(source):
        ref = _props(source).get('object')
        _, boss = _find_object(root, ref.get('value')) if ref is not None else (None, None)
        boss_value = _props(boss).get('boss_key') if boss is not None else None
        boss_value = boss_value.get('value') if boss_value is not None else None
        if boss_key is None:
            warnings.append(f'Warning: this gate leaves the boss room "{source.get("name")}" and has '
                            f'no boss_key, so the player can leave during the fight.')
        elif boss_value is not None and boss_value != str(boss_key):
            _fail(f'--boss-key {boss_key} does not match boss_key={boss_value} of the boss object '
                  f'id={boss.get("id")} of room "{source.get("name")}".')
    return warnings


def _gate_block_warning(root, x, y, w, h):
    group = _find_group(root, 'blocks')
    for block in (group.findall('object') if group is not None else []):
        bx, by, bw, bh = _rect(block)
        if bx <= x and by <= y and bx + bw >= x + w and by + bh >= y + h:
            return None
    return ('Warning: no block in "blocks" covers the gate rectangle; the wall block must run '
            'through the gate, or the player walks past it.')


def _write_gate(root, col, row, to_room, from_side, boss_key):
    x, y, w, h, side, source = _resolve_gate(root, col, row, to_room, from_side)
    warnings = _check_gate_key(root, to_room, source, boss_key)
    block_warning = _gate_block_warning(root, x, y, w, h)
    if block_warning:
        warnings.append(block_warning)
    gate = _add_object(root, _find_or_create_group(root, 'sensors'), {
        'name': 'Gate', 'x': _fmt(x), 'y': _fmt(y), 'width': _fmt(w), 'height': _fmt(h),
    })
    values = [('room', to_room, None)]
    if side != GATE_DEFAULT_SIDE:
        values.append(('direction', side, None))
    if boss_key is not None:
        values.append(('boss_key', boss_key, None))
    _set_props(gate, values)
    return gate, side, source, warnings


def _next_unlock_key(root):
    keys = [0]
    for obj in root.iter('object'):
        for name, prop in _props(obj).items():
            if name in UNLOCK_KEY_PROPS and prop.get('value', '').isdigit():
                keys.append(int(prop.get('value')))
    return str(max(keys) + 1)


def cmd_add_gate(args):
    tree, root = _load(args.file)
    gate, side, source, warnings = _write_gate(
        root, args.col, args.row, args.to, args.from_side, args.boss_key)
    _save(tree, args.file, dry_run=args.dry_run)
    key = f', boss_key={args.boss_key}' if args.boss_key is not None else ''
    print(f'Added Gate id={gate.get("id")} at col={args.col} row={args.row}: '
          f'"{source.get("name")}" -> "{args.to}", player from {side}{key}')
    for w in warnings:
        print(w)


def cmd_add_boss_room(args):
    tree, root = _load(args.file)
    rect = (args.col, args.row, args.w, args.h)
    room = _find_room(root, args.name)
    if room is not None and any(v is not None for v in rect):
        _fail(f'Room "{args.name}" exists; give no --col/--row/--w/--h, or choose a new name.')
    if room is None:
        if any(v is None for v in rect):
            _fail(f'No room named "{args.name}"; give --col, --row, --w and --h to add it.')
        room = _add_object(root, _find_or_create_group(root, 'game_rooms'), {
            'name': args.name,
            'x': args.col * TILE, 'y': _row_to_y_px(root, args.row, args.h),
            'width': args.w * TILE, 'height': args.h * TILE,
        })
    if 'event' in _props(room) or 'object' in _props(room):
        _fail(f'Room "{args.name}" already has an event or object property.')
    for group, label in (((args.entry_from, args.entry_col, args.entry_row),
                          '--entry-from, --entry-col and --entry-row'),
                         ((args.exit_to, args.exit_col, args.exit_row),
                          '--exit-to, --exit-col and --exit-row')):
        if any(v is not None for v in group) and any(v is None for v in group):
            _fail(f'Give {label} together.')

    key = _next_unlock_key(root)
    boss = _add_object(root, _find_or_create_group(root, 'children'), {
        'name': args.boss,
        'x': _fmt(args.boss_col * TILE), 'y': _fmt(_row_to_y_px(root, args.boss_row, 1)),
        'width': TILE, 'height': TILE,
    })
    boss_props = [('boss_key', key, None)]
    if args.mini:
        boss_props.append(('mini', 'true', 'bool'))
    _set_props(boss, boss_props)
    _set_props(room, [('event', 'boss', None), ('object', boss.get('id'), 'object')])
    print(f'Boss room "{args.name}": event=boss, object={boss.get("id")} '
          f'("{args.boss}" in children, boss_key={key}{", mini=true" if args.mini else ""})')

    warnings = []
    if args.entry_from is not None:
        gate, side, source, w = _write_gate(root, args.entry_col, args.entry_row, args.name, None, None)
        if source.get('name') != args.entry_from:
            _fail(f'The entry gate at col={args.entry_col} row={args.entry_row} is between '
                  f'"{source.get("name")}" and "{args.name}", not "{args.entry_from}".')
        warnings += w
        print(f'Added entry Gate id={gate.get("id")}: "{args.entry_from}" -> "{args.name}", '
              f'player from {side}, no key')
    if args.exit_to is not None:
        gate, side, source, w = _write_gate(root, args.exit_col, args.exit_row, args.exit_to, None, key)
        if source.get('name') != args.name:
            _fail(f'The exit gate at col={args.exit_col} row={args.exit_row} does not leave '
                  f'"{args.name}" (it leaves "{source.get("name")}").')
        warnings += w
        print(f'Added exit Gate id={gate.get("id")}: "{args.name}" -> "{args.exit_to}", '
              f'player from {side}, boss_key={key}')
        if not args.mini:
            warnings.append('Warning: a full boss fight unlocks no gate unless the boss object has '
                            'end=false (mid boss); set it in Tiled, or remove the exit gate.')

    _save(tree, args.file, dry_run=args.dry_run)
    for w in warnings:
        print(w)


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

    p = sub.add_parser('add-ladder',
                       help='add a 1-tile-wide Ladder to specials and a 32x8 px LadderTop '
                            'to blocks at its top; --row is the bottom row, and the top '
                            'edge (row + h) must be level with the surface it reaches; '
                            'the ladder tiles are not painted')
    p.add_argument('file')
    p.add_argument('--col', type=int, required=True)
    p.add_argument('--row', type=int, required=True,
                   help='bottom row of the ladder (the row above the floor)')
    p.add_argument('--h', type=int, required=True, help='ladder height in tiles')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_ladder)

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

    p = sub.add_parser(
        'add-gate',
        help='add a Gate to sensors, centered on the shared edge of 2 rooms',
        description='Add a Gate to the sensors layer. A left/right gate is 2x3 tiles (64x96 px) '
                    'and its --col is the edge column - 1; an up/down gate is 3x2 tiles '
                    '(96x64 px) and its --row is the edge row - 1. Rules: "room" names the room '
                    'the gate LEADS TO (--to); "direction" is the side the player comes FROM '
                    '(--from; the Gate puts its touch fixture on that side; left is the default '
                    'and is not written); only the EXIT gate of a boss room takes boss_key. '
                    'The wall block must run through the gate (add it with add-block).')
    p.add_argument('file')
    p.add_argument('--col', type=int, required=True, help='left column of the gate rectangle')
    p.add_argument('--row', type=int, required=True, help='bottom row of the gate rectangle')
    p.add_argument('--to', required=True, help='the room the gate leads to')
    p.add_argument('--from', dest='from_side', required=True, choices=GATE_SIDES,
                   help='the side the player comes from; must match the room layout')
    p.add_argument('--boss-key',
                   help='lock the gate until the fight with this boss_key is won; refused when '
                        '--to is a boss room (entry gate)')
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_gate)

    p = sub.add_parser(
        'add-boss-room',
        help='make a room a boss room (event=boss, object=boss marker in children), with '
             'optional entry and exit gates',
        description='Set event=boss and object on a room (existing, or new with --col/--row/--w/'
                    '--h), and add a 1-tile boss marker in the children layer with boss_key '
                    '(the next free boss_key/gauntlet_key in the map) and mini=true with --mini. '
                    'Other boss properties are set by hand in Tiled. --entry-from adds the entry '
                    'gate (no key) and --exit-to adds the exit gate (with the boss_key); their '
                    '--*-col/--*-row are as for add-gate, and the player side comes from the room '
                    'layout.')
    p.add_argument('file')
    p.add_argument('name', help='the boss room name')
    p.add_argument('--col', type=int, help='new room only')
    p.add_argument('--row', type=int, help='new room only')
    p.add_argument('--w', type=int, help='new room only')
    p.add_argument('--h', type=int, help='new room only')
    p.add_argument('--boss', default='?', help='boss class name, e.g. ReactorMonkey (default "?")')
    p.add_argument('--boss-col', type=float, required=True, help='marker column (may be .5)')
    p.add_argument('--boss-row', type=float, required=True, help='marker bottom row (may be .5)')
    p.add_argument('--mini', action='store_true', help='mini boss fight (mini=true on the boss)')
    p.add_argument('--entry-from', help='the room before the boss room')
    p.add_argument('--entry-col', type=int)
    p.add_argument('--entry-row', type=int)
    p.add_argument('--exit-to', help='the room after the boss room')
    p.add_argument('--exit-col', type=int)
    p.add_argument('--exit-row', type=int)
    _add_dry_run_arg(p)
    p.set_defaults(func=cmd_add_boss_room)

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
