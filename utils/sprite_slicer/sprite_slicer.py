#!/usr/bin/env python3
"""Cut a tightly packed sprite sheet into fixed-size animation strips.

A YAML data file names each animation on the sheet and sets how its frames sit in
the cell. See README.md for the format.
"""

import argparse
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageDraw

ANCHORS_Y = ("bottom", "center")
ALIGNS_X = ("overlap", "center")
ANIMATION_KEYS = {"name", "row", "frames", "anchor_y", "align_x", "align_to", "twin", "offsets"}
DEFAULT_KEYS = {"anchor_y", "align_x", "align_to"}
ROW_KEYS = {"merge", "split_at"}

PREVIEW_SCALE = 3
PREVIEW_LABEL_WIDTH = 150
PREVIEW_BACKGROUND = (150, 150, 170, 255)
PREVIEW_CELL_LINE = (90, 90, 110, 255)
PREVIEW_GIF_SCALE = 6
PREVIEW_GIF_FRAME_MS = 125


class SlicerError(Exception):
    pass


@dataclass
class Animation:
    name: str
    row: int
    first: int
    last: int
    anchor_y: str
    align_x: str
    align_to: str = None
    twin: str = None
    offsets: dict = field(default_factory=dict)


@dataclass
class Config:
    name: str
    sheet: str
    cell: tuple
    rows: dict
    animations: list


def parse_config(raw):
    defaults = dict(raw.get("defaults") or {})
    if "cell" not in defaults:
        raise SlicerError("defaults needs a cell size")
    cell = tuple(defaults.pop("cell"))
    _check_keys(defaults, DEFAULT_KEYS, "defaults")

    rows = {int(row): value or {} for row, value in (raw.get("rows") or {}).items()}
    for row, value in rows.items():
        _check_keys(value, ROW_KEYS, f"row {row}")

    animations = [_parse_animation(entry, defaults) for entry in raw["animations"]]
    names = [animation.name for animation in animations]
    duplicates = {name for name in names if names.count(name) > 1}
    if duplicates:
        raise SlicerError(f"animation names used twice: {sorted(duplicates)}")
    return Config(raw["name"], raw["sheet"], cell, rows, animations)


def _parse_animation(entry, defaults):
    _check_keys(entry, ANIMATION_KEYS, f"animation {entry.get('name')}")
    merged = {**defaults, **entry}
    first, last = merged["frames"]
    animation = Animation(
        name=merged["name"],
        row=merged["row"],
        first=first,
        last=last,
        anchor_y=merged.get("anchor_y"),
        align_x=merged.get("align_x"),
        align_to=merged.get("align_to"),
        twin=merged.get("twin"),
        offsets={int(index): tuple(offset) for index, offset in (merged.get("offsets") or {}).items()},
    )
    if animation.anchor_y not in ANCHORS_Y:
        raise SlicerError(f"{animation.name}: unknown anchor_y {animation.anchor_y}")
    if animation.align_x not in ALIGNS_X:
        raise SlicerError(f"{animation.name}: unknown align_x {animation.align_x}")
    return animation


def _check_keys(mapping, allowed, where):
    unknown = set(mapping) - allowed
    if unknown:
        raise SlicerError(f"{where}: unknown keys {sorted(unknown)}")


def load_config(path):
    with open(path, encoding="utf-8") as file:
        return parse_config(yaml.safe_load(file))


def load_sheet(path):
    with Image.open(path) as image:
        return np.array(image.convert("RGBA"))


def _spans(filled):
    spans = []
    start = None
    for index, value in enumerate(filled):
        if value and start is None:
            start = index
        elif not value and start is not None:
            spans.append((start, index - 1))
            start = None
    if start is not None:
        spans.append((start, len(filled) - 1))
    return spans


def find_rows(sheet):
    return _spans(sheet[..., 3].any(axis=1))


def find_frames(sheet, band, merge=None, split_at=None):
    top, bottom = band
    alpha = sheet[top:bottom + 1, :, 3] > 0
    spans = _spans(alpha.any(axis=0))
    spans = _split_spans(spans, split_at or [])
    spans = _merge_spans(spans, merge or [])

    frames = []
    for left, right in spans:
        rows = np.nonzero(alpha[:, left:right + 1].any(axis=1))[0]
        frames.append((left, top + int(rows[0]), right, top + int(rows[-1])))
    return frames


def _split_spans(spans, split_at):
    result = []
    for left, right in spans:
        start = left
        for column in sorted(split_at):
            if start < column <= right:
                result.append((start, column - 1))
                start = column
        result.append((start, right))
    return result


def _merge_spans(spans, merge):
    starts = {first: last for first, last in merge}
    result = []
    index = 0
    while index < len(spans):
        last = starts.get(index, index)
        result.append((spans[index][0], spans[last][1]))
        index = last + 1
    return result


def build_strips(config, sheet):
    frames_by_row = _detect_frames(config, sheet)
    _check_coverage(config, frames_by_row)
    by_name = {animation.name: animation for animation in config.animations}
    placed = {}
    for animation in config.animations:
        _place_animation(animation, by_name, config, sheet, frames_by_row, placed, [])
    return {name: np.concatenate(cells, axis=0) for name, cells in placed.items()}


def _detect_frames(config, sheet):
    result = {}
    for number, band in enumerate(find_rows(sheet), start=1):
        options = config.rows.get(number, {})
        result[number] = find_frames(sheet, band, options.get("merge"), options.get("split_at"))
    return result


def _check_coverage(config, frames_by_row):
    used = {}
    for animation in config.animations:
        if animation.row not in frames_by_row:
            raise SlicerError(f"{animation.name}: row {animation.row} is not on the sheet")
        count = len(frames_by_row[animation.row])
        for index in range(animation.first, animation.last + 1):
            if index >= count:
                raise SlicerError(f"{animation.name}: frame {index} is not in row {animation.row}")
            key = (animation.row, index)
            if key in used:
                raise SlicerError(f"row {key[0]} frame {key[1]} is in {used[key]} and {animation.name}")
            used[key] = animation.name
    for row, frames in frames_by_row.items():
        for index in range(len(frames)):
            if (row, index) not in used:
                raise SlicerError(f"row {row} frame {index} is in no animation")


def _place_animation(animation, by_name, config, sheet, frames_by_row, placed, path):
    if animation.name in placed:
        return
    if animation.name in path:
        raise SlicerError(f"alignment cycle: {' -> '.join(path + [animation.name])}")

    if animation.align_x == "overlap":
        for reference in (animation.align_to, animation.twin):
            if reference is None:
                continue
            if reference not in by_name:
                raise SlicerError(f"{animation.name}: unknown animation {reference}")
            _place_animation(by_name[reference], by_name, config, sheet, frames_by_row, placed,
                             path + [animation.name])

    boxes = frames_by_row[animation.row][animation.first:animation.last + 1]
    cells = []
    for index, box in enumerate(boxes):
        target = _target_cell(animation, index, cells, placed)
        cells.append(_place_frame(animation, index, box, sheet, config.cell, target))
    placed[animation.name] = cells


def _target_cell(animation, index, cells, placed):
    if animation.align_x != "overlap":
        return None
    if animation.twin is not None:
        reference = placed[animation.twin]
        return reference[index] if index < len(reference) else None
    if index > 0:
        return cells[index - 1]
    if animation.align_to is not None:
        return placed[animation.align_to][0]
    return None


def _place_frame(animation, index, box, sheet, cell, target):
    left, top, right, bottom = box
    image = sheet[top:bottom + 1, left:right + 1]
    cell_width, cell_height = cell
    height, width = image.shape[:2]
    label = f"{animation.name} frame {index}"
    if width > cell_width:
        raise SlicerError(f"{label} is {width} px wider than the {cell_width} px cell")
    if height > cell_height:
        raise SlicerError(f"{label} is {height} px taller than the {cell_height} px cell")

    y = cell_height - height if animation.anchor_y == "bottom" else (cell_height - height) // 2
    x = (cell_width - width) // 2
    if target is not None:
        x = _best_overlap_x(image, target, y, x)

    offset_x, offset_y = animation.offsets.get(index, (0, 0))
    x += offset_x
    y += offset_y
    if x < 0 or y < 0 or x + width > cell_width or y + height > cell_height:
        raise SlicerError(f"{label}: the offset moves it outside the cell")

    result = np.zeros((cell_height, cell_width, 4), dtype=np.uint8)
    result[y:y + height, x:x + width] = image
    return result


def _best_overlap_x(image, target, y, center_x):
    height, width = image.shape[:2]
    opaque = image[..., 3] > 0
    best_x = center_x
    best_key = (0, 0)
    for x in range(target.shape[1] - width + 1):
        area = target[y:y + height, x:x + width]
        matches = opaque & (area[..., 3] > 0) & (area == image).all(axis=2)
        key = (int(matches.sum()), -abs(x - center_x))
        if key[0] > 0 and key > best_key:
            best_key = key
            best_x = x
    return best_x


def write_strips(strips, directory):
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    for name, strip in strips.items():
        Image.fromarray(strip, "RGBA").save(directory / f"{name}.png")
    expected = {f"{name}.png" for name in strips}
    return sorted(path.name for path in directory.glob("*.png") if path.name not in expected)


def _on_background(cell, scale):
    image = Image.fromarray(cell, "RGBA")
    background = Image.new("RGBA", image.size, PREVIEW_BACKGROUND)
    background.alpha_composite(image)
    return background.resize((image.width * scale, image.height * scale), Image.NEAREST)


def write_preview(config, strips, directory):
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    cell_width, cell_height = config.cell
    longest = max(strip.shape[0] // cell_height for strip in strips.values())
    size = (PREVIEW_LABEL_WIDTH + longest * cell_width * PREVIEW_SCALE,
            len(strips) * cell_height * PREVIEW_SCALE)
    preview = Image.new("RGBA", size, (30, 30, 30, 255))
    draw = ImageDraw.Draw(preview)

    for line, (name, strip) in enumerate(strips.items()):
        top = line * cell_height * PREVIEW_SCALE
        draw.text((4, top + 4), name, fill=(255, 255, 255, 255))
        frames = [strip[y:y + cell_height] for y in range(0, strip.shape[0], cell_height)]
        for column, cell in enumerate(frames):
            left = PREVIEW_LABEL_WIDTH + column * cell_width * PREVIEW_SCALE
            preview.paste(_on_background(cell, PREVIEW_SCALE), (left, top))
            draw.rectangle((left, top, left + cell_width * PREVIEW_SCALE - 1,
                            top + cell_height * PREVIEW_SCALE - 1), outline=PREVIEW_CELL_LINE)
        pictures = [_on_background(cell, PREVIEW_GIF_SCALE).convert("RGB") for cell in frames]
        pictures[0].save(directory / f"{name}.gif", save_all=True, append_images=pictures[1:],
                         duration=PREVIEW_GIF_FRAME_MS, loop=0)
    preview.save(directory / "preview.png")


def to_game_atlas(packed):
    lines = packed.strip().splitlines()
    result = []
    region = None
    for line in lines:
        if line.startswith("filter:"):
            result.append("filter: Nearest,Nearest")
        elif line.startswith("pma:"):
            continue
        elif line.startswith("  bounds:"):
            x, y, width, height = re.findall(r"\d+", line)
            result += [
                "  rotate: false",
                f"  xy: {x}, {y}",
                f"  size: {width}, {height}",
                f"  orig: {width}, {height}",
                "  offset: 0, 0",
                "  index: -1",
            ]
        elif line.startswith("  "):
            raise SlicerError(f"{region}: unexpected atlas line: {line.strip()}")
        else:
            region = line
            result.append(line)
    return "\n".join(result) + "\n"


def pack_atlas(config, frames_dir, atlas_dir):
    atlas_dir = Path(atlas_dir)
    with tempfile.TemporaryDirectory() as work:
        work = Path(work)
        sheet = work / f"{config.name}.png"
        data = work / f"{config.name}.txt"
        files = [str(Path(frames_dir) / f"{animation.name}.png") for animation in config.animations]
        subprocess.run([
            "TexturePacker", "--format", "libgdx", "--sheet", str(sheet), "--data", str(data),
            "--trim-mode", "None", "--disable-rotation", "--algorithm", "MaxRects",
            "--border-padding", "1", "--shape-padding", "2", "--extrude", "0",
            "--texture-format", "png", "--opt", "RGBA8888", "--size-constraints", "AnySize",
            "--max-size", "1024", *files,
        ], check=True, stdout=subprocess.DEVNULL)
        atlas_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(sheet, atlas_dir / sheet.name)
        (atlas_dir / data.name).write_text(to_game_atlas(data.read_text()))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("config", help="YAML data file for the sheet")
    parser.add_argument("--frames-dir", required=True, help="folder for the animation strips")
    parser.add_argument("--atlas-dir", help="folder for the packed atlas; omit to skip packing")
    parser.add_argument("--preview-dir", help="folder for the preview sheet and GIFs")
    args = parser.parse_args(argv)

    config_path = Path(args.config)
    try:
        config = load_config(config_path)
        strips = build_strips(config, load_sheet(config_path.parent / config.sheet))
        stale = write_strips(strips, args.frames_dir)
    except SlicerError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    frame_total = sum(strip.shape[0] // config.cell[1] for strip in strips.values())
    print(f"{len(strips)} strips, {frame_total} frames: {args.frames_dir}")
    if stale:
        print(f"warning: not in the data file, not packed: {stale}", file=sys.stderr)
    if args.preview_dir:
        write_preview(config, strips, args.preview_dir)
        print(f"preview: {args.preview_dir}")
    if args.atlas_dir:
        pack_atlas(config, args.frames_dir, args.atlas_dir)
        print(f"atlas: {args.atlas_dir}/{config.name}.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
