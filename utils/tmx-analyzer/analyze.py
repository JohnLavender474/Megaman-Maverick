#!/usr/bin/env python3
"""
Megaman Maverick TMX Analyzer

Parses a Tiled .tmx level file and produces a JSON summary.

Every room always includes its full entities[] list. Each entry in
entities[] is lightweight — just {id, layer, name} — and the full
details (size, properties, list of rooms it lives in) for every object
are stored once in the top-level entity_details map, keyed by id.
Look up an entry there when you need the heavyweight info.

Objects that overlap multiple rooms are included in every room they
overlap (no longer treated as unassigned). Only objects with no
spawn_room property AND no room overlap land in unassigned_objects.

The --room flag narrows the output to a single room rather than
gating the level of detail.

Usage:
  analyze.py TMX_PATH [-o OUT] [--room NAME]

  TMX_PATH           Path to a .tmx file (relative or absolute).
  -o, --output       JSON path. Use '-' for stdout (default).
  --room NAME        Narrow rooms[] to just this one room.
"""

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict


# Layers whose objects represent placements that belong to a room.
SPAWNER_LAYERS = {
    'enemies', 'blocks', 'items', 'hazards', 'specials',
    'decorations', 'projectiles', 'sensors', 'player', 'children',
}

# Layers ignored entirely for room assignment / counts (they are global).
IGNORED_LAYERS_FOR_ROOMS = {'backgrounds', 'foregrounds', 'TODO', 'temp'}


def _num(v):
    """Parse a numeric attribute, returning int when possible."""
    if v is None:
        return 0
    f = float(v)
    return int(f) if f.is_integer() else f


def parse_properties(obj):
    props = {}
    p = obj.find('properties')
    if p is None:
        return props
    for prop in p.findall('property'):
        name = prop.get('name')
        ptype = prop.get('type')
        val = prop.get('value')
        if val is None:
            val = (prop.text or '').strip()
        if ptype:
            props[name] = {'type': ptype, 'value': val}
        else:
            props[name] = val
    return props


def parse_object(obj):
    return {
        'id': obj.get('id'),
        'name': obj.get('name'),
        'x': _num(obj.get('x')),
        'y': _num(obj.get('y')),
        'width': _num(obj.get('width')),
        'height': _num(obj.get('height')),
        'properties': parse_properties(obj),
    }


def parse_object_layers(root):
    layers = {}
    for og in root.findall('objectgroup'):
        name = og.get('name')
        layers[name] = [parse_object(o) for o in og.findall('object')]
    return layers


def rect_overlap(ax, ay, aw, ah, bx, by, bw, bh):
    return ax < bx + bw and ax + aw > bx and ay < by + bh and ay + ah > by


def assign_rooms(obj, rooms):
    """Returns (list_of_room_names, reason_str).

    - If the object has a `spawn_room` property, that single room wins (explicit).
    - Otherwise the object lands in every game_rooms rectangle it overlaps.
      Multi-overlap is fine — the entity is included in each matching room.
    - Empty list means no spawn_room property and no overlap; the object goes
      into unassigned_objects.
    """
    sr = obj['properties'].get('spawn_room')
    if isinstance(sr, dict):
        sr = sr.get('value')
    if sr:
        return [sr], 'explicit_spawn_room'

    overlapping = []
    for r in rooms:
        if rect_overlap(obj['x'], obj['y'], obj['width'] or 1, obj['height'] or 1,
                        r['x'], r['y'], r['width'], r['height']):
            overlapping.append(r['name'])
    if overlapping:
        reason = 'single_overlap' if len(overlapping) == 1 else 'multi_overlap'
        return overlapping, reason
    return [], 'no_overlap'


def summarize(tmx_path, room_filter=None):
    tree = ET.parse(tmx_path)
    root = tree.getroot()

    map_w = int(root.get('width'))
    map_h = int(root.get('height'))
    tile_w = int(root.get('tilewidth'))
    tile_h = int(root.get('tileheight'))

    layers = parse_object_layers(root)

    raw_rooms = layers.get('game_rooms', [])
    rooms = []
    for i, r in enumerate(raw_rooms, start=1):
        rooms.append({
            'order': i,
            'name': r['name'],
            'x': r['x'], 'y': r['y'],
            'width': r['width'], 'height': r['height'],
        })
    rooms_by_name = {r['name']: r for r in rooms}

    per_room_counts = defaultdict(lambda: defaultdict(int))
    per_room_entities = defaultdict(list)
    entity_details = {}
    unassigned = []

    for layer_name, objs in layers.items():
        if layer_name == 'game_rooms' or layer_name in IGNORED_LAYERS_FOR_ROOMS:
            continue
        for obj in objs:
            room_names, reason = assign_rooms(obj, raw_rooms)
            display_name = obj['name'] if obj['name'] else f'<unnamed {layer_name}>'
            key = f'{layer_name}:{display_name}'
            valid_rooms = [rn for rn in room_names if rn in rooms_by_name]
            if valid_rooms:
                entity_details[obj['id']] = {
                    'layer': layer_name,
                    'name': obj['name'],
                    'x': obj['x'], 'y': obj['y'],
                    'width': obj['width'], 'height': obj['height'],
                    'properties': obj['properties'],
                    'rooms': valid_rooms,
                }
                for room_name in valid_rooms:
                    per_room_counts[room_name][key] += 1
                    per_room_entities[room_name].append({
                        'id': obj['id'],
                        'layer': layer_name,
                        'name': obj['name'],
                    })
            else:
                unassigned.append({
                    'layer': layer_name,
                    'id': obj['id'],
                    'name': obj['name'],
                    'x': obj['x'], 'y': obj['y'],
                    'width': obj['width'], 'height': obj['height'],
                    'reason': reason,
                })

    class_sizes = defaultdict(lambda: defaultdict(int))
    for layer_name, objs in layers.items():
        if layer_name == 'game_rooms' or layer_name in IGNORED_LAYERS_FOR_ROOMS:
            continue
        for obj in objs:
            if not obj['name']:
                continue
            key = f'{layer_name}:{obj["name"]}'
            size = f'{obj["width"]}x{obj["height"]}'
            class_sizes[key][size] += 1

    entity_classes = {}
    for key, sizes in sorted(class_sizes.items()):
        entity_classes[key] = {
            'total': sum(sizes.values()),
            'sizes_observed': [
                {'size': s, 'count': c} for s, c in sorted(sizes.items())
            ],
        }

    out = {
        'source': os.path.abspath(tmx_path),
        'map': {
            'width_tiles': map_w,
            'height_tiles': map_h,
            'tile_size': tile_w,
            'width_px': map_w * tile_w,
            'height_px': map_h * tile_h,
        },
        'rooms': [],
        'entity_details': entity_details,
        'entity_classes': entity_classes,
        'unassigned_objects': unassigned,
    }

    for r in rooms:
        if room_filter and r['name'] != room_filter:
            continue
        entry = {
            'order': r['order'],
            'name': r['name'],
            'x': r['x'], 'y': r['y'],
            'width': r['width'], 'height': r['height'],
            'entity_counts': dict(per_room_counts.get(r['name'], {})),
            'entities': per_room_entities.get(r['name'], []),
        }
        out['rooms'].append(entry)

    return out


def main():
    ap = argparse.ArgumentParser(
        description='Analyze a Megaman Maverick TMX into a JSON summary.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument('tmx', help='Path to TMX file (relative or absolute)')
    ap.add_argument('--room', help='Narrow rooms[] to just this one room')
    ap.add_argument('-o', '--output', default='-', help="Output JSON path (default: '-' for stdout)")
    args = ap.parse_args()

    if not os.path.exists(args.tmx):
        print(f'TMX not found: {args.tmx}', file=sys.stderr)
        sys.exit(1)

    result = summarize(args.tmx, room_filter=args.room)

    if args.room and not any(r['name'] == args.room for r in result['rooms']):
        print(f"Warning: room '{args.room}' not found in {args.tmx}",
              file=sys.stderr)

    text = json.dumps(result, indent=2)
    if args.output == '-':
        print(text)
    else:
        with open(args.output, 'w') as f:
            f.write(text)
        print(f'Wrote {args.output}', file=sys.stderr)


if __name__ == '__main__':
    main()
