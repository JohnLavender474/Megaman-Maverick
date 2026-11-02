import numpy as np
import pytest
from PIL import Image

import sprite_slicer as ss

RED = (255, 0, 0, 255)
BLUE = (0, 0, 255, 255)


def sheet_of(height, width, boxes):
    sheet = np.zeros((height, width, 4), dtype=np.uint8)
    for x0, y0, x1, y1, color in boxes:
        sheet[y0:y1 + 1, x0:x1 + 1] = color
    return sheet


def config_of(animations, cell=(8, 8), rows=None, **defaults):
    return ss.parse_config({
        "name": "Test",
        "sheet": "unused.png",
        "defaults": {"cell": list(cell), "anchor_y": "bottom", "align_x": "center", **defaults},
        "rows": rows or {},
        "animations": animations,
    })


def opaque_bounds(cell):
    ys, xs = np.nonzero(cell[..., 3])
    return int(xs.min()), int(xs.max()), int(ys.min()), int(ys.max())


def test_rows_are_split_on_fully_empty_lines():
    sheet = sheet_of(10, 10, [(0, 1, 2, 2, RED), (5, 2, 6, 3, RED), (1, 6, 2, 8, RED)])
    assert ss.find_rows(sheet) == [(1, 3), (6, 8)]


def test_frames_are_split_on_fully_empty_columns_and_trimmed_vertically():
    sheet = sheet_of(6, 12, [(1, 1, 2, 2, RED), (5, 3, 6, 4, RED)])
    assert ss.find_frames(sheet, (1, 4)) == [(1, 1, 2, 2), (5, 3, 6, 4)]


def test_merge_joins_detached_spans_into_one_frame():
    sheet = sheet_of(4, 12, [(0, 0, 1, 3, RED), (3, 1, 3, 1, RED), (5, 0, 6, 3, RED), (9, 0, 9, 3, RED)])
    assert ss.find_frames(sheet, (0, 3), merge=[[1, 2]]) == [(0, 0, 1, 3), (3, 0, 6, 3), (9, 0, 9, 3)]


def test_split_at_cuts_touching_frames_and_the_right_part_starts_at_that_column():
    sheet = sheet_of(4, 10, [(1, 1, 7, 1, RED), (1, 0, 3, 3, RED), (5, 2, 7, 3, RED)])
    assert ss.find_frames(sheet, (0, 3), split_at=[5]) == [(1, 0, 4, 3), (5, 1, 7, 3)]


def test_bottom_anchor_puts_lowest_pixel_on_last_cell_row():
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 0]}])
    strip = ss.build_strips(config, sheet)["a"]
    assert strip.shape == (8, 8, 4)
    assert opaque_bounds(strip) == (3, 4, 6, 7)


def test_center_anchor_is_applied_per_animation():
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 0], "anchor_y": "center"}])
    assert opaque_bounds(ss.build_strips(config, sheet)["a"]) == (3, 4, 3, 4)


def test_strip_stacks_frames_vertically_in_sheet_order():
    sheet = sheet_of(3, 8, [(0, 0, 1, 1, RED), (4, 0, 5, 1, BLUE)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 1]}])
    strip = ss.build_strips(config, sheet)["a"]
    assert strip.shape == (16, 8, 4)
    assert tuple(strip[7, 3]) == RED
    assert tuple(strip[15, 3]) == BLUE


def test_overlap_keeps_shared_body_still_when_frame_grows_to_the_left():
    sheet = sheet_of(3, 16, [(1, 0, 2, 1, RED), (6, 0, 7, 1, BLUE), (8, 0, 9, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 1], "align_x": "overlap"}])
    strip = ss.build_strips(config, sheet)["a"]
    assert opaque_bounds(strip[:8]) == (3, 4, 6, 7)
    assert opaque_bounds(strip[8:]) == (1, 4, 6, 7)


def test_overlap_with_no_shared_pixels_falls_back_to_center():
    sheet = sheet_of(3, 16, [(1, 0, 2, 1, RED), (6, 0, 7, 1, BLUE)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 1], "align_x": "overlap"}])
    assert opaque_bounds(ss.build_strips(config, sheet)["a"][8:]) == (3, 4, 6, 7)


def test_align_to_places_first_frame_on_first_frame_of_named_strip():
    sheet = sheet_of(3, 16, [(1, 0, 2, 1, RED), (6, 0, 6, 1, BLUE), (7, 0, 8, 1, RED)])
    config = config_of([
        {"name": "base", "row": 1, "frames": [0, 0], "offsets": {0: [-2, 0]}},
        {"name": "next", "row": 1, "frames": [1, 1], "align_x": "overlap", "align_to": "base"},
    ])
    assert opaque_bounds(ss.build_strips(config, sheet)["next"]) == (0, 2, 6, 7)


def test_twin_aligns_each_frame_to_the_same_index_of_the_named_strip():
    sheet = sheet_of(3, 40, [
        (0, 0, 1, 1, RED), (4, 0, 5, 1, BLUE),
        (8, 0, 9, 1, RED), (10, 0, 11, 1, (0, 255, 0, 255)),
        (14, 0, 15, 1, BLUE), (16, 0, 17, 1, (0, 255, 0, 255)),
    ])
    config = config_of([
        {"name": "run", "row": 1, "frames": [0, 1], "offsets": {0: [-3, 0], 1: [1, 0]}},
        {"name": "run_shoot", "row": 1, "frames": [2, 3], "align_x": "overlap", "twin": "run"},
    ])
    strip = ss.build_strips(config, sheet)["run_shoot"]
    assert opaque_bounds(strip[:8]) == (0, 3, 6, 7)
    assert opaque_bounds(strip[8:]) == (4, 7, 6, 7)


def test_animations_may_refer_to_strips_defined_later():
    sheet = sheet_of(3, 8, [(0, 0, 1, 1, RED), (4, 0, 5, 1, RED)])
    config = config_of([
        {"name": "late", "row": 1, "frames": [1, 1], "twin": "early", "align_x": "overlap"},
        {"name": "early", "row": 1, "frames": [0, 0]},
    ])
    assert set(ss.build_strips(config, sheet)) == {"late", "early"}


def test_offset_moves_one_frame_in_image_coordinates():
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 0], "offsets": {0: [2, -1]}}])
    assert opaque_bounds(ss.build_strips(config, sheet)["a"]) == (5, 6, 5, 6)


@pytest.mark.parametrize("animations, message", [
    ([{"name": "a", "row": 1, "frames": [0, 0], "offsets": {0: [9, 0]}}], "outside"),
    ([{"name": "a", "row": 1, "frames": [0, 0], "align_to": "nope", "align_x": "overlap"}], "nope"),
    ([{"name": "a", "row": 2, "frames": [0, 0]}], "row 2"),
    ([{"name": "a", "row": 1, "frames": [0, 3]}], "frame 1"),
])
def test_bad_animation_data_raises_a_named_error(animations, message):
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    with pytest.raises(ss.SlicerError, match=message):
        ss.build_strips(config_of(animations), sheet)


def test_frame_larger_than_cell_raises_a_named_error():
    sheet = sheet_of(3, 12, [(0, 0, 9, 1, RED)])
    with pytest.raises(ss.SlicerError, match="wider"):
        ss.build_strips(config_of([{"name": "a", "row": 1, "frames": [0, 0]}]), sheet)


def test_align_cycle_raises_a_named_error():
    sheet = sheet_of(3, 8, [(0, 0, 1, 1, RED), (4, 0, 5, 1, RED)])
    config = config_of([
        {"name": "a", "row": 1, "frames": [0, 0], "twin": "b", "align_x": "overlap"},
        {"name": "b", "row": 1, "frames": [1, 1], "twin": "a", "align_x": "overlap"},
    ])
    with pytest.raises(ss.SlicerError, match="cycle"):
        ss.build_strips(config, sheet)


def test_frame_on_sheet_that_no_animation_uses_raises_a_named_error():
    sheet = sheet_of(3, 8, [(0, 0, 1, 1, RED), (4, 0, 5, 1, RED)])
    with pytest.raises(ss.SlicerError, match=r"row 1 frame 1"):
        ss.build_strips(config_of([{"name": "a", "row": 1, "frames": [0, 0]}]), sheet)


def test_frame_that_two_animations_use_raises_a_named_error():
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([
        {"name": "a", "row": 1, "frames": [0, 0]},
        {"name": "b", "row": 1, "frames": [0, 0]},
    ])
    with pytest.raises(ss.SlicerError, match=r"row 1 frame 0"):
        ss.build_strips(config, sheet)


@pytest.mark.parametrize("key, value", [("anchor_y", "top"), ("align_x", "left")])
def test_unknown_placement_method_raises_a_named_error(key, value):
    with pytest.raises(ss.SlicerError, match=value):
        config_of([{"name": "a", "row": 1, "frames": [0, 0], key: value}])


def test_write_strips_writes_one_png_per_animation(tmp_path):
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 0]}])
    ss.write_strips(ss.build_strips(config, sheet), tmp_path)
    written = np.array(Image.open(tmp_path / "a.png"))
    assert written.shape == (8, 8, 4)
    assert tuple(written[7, 3]) == RED


def test_write_strips_keeps_and_reports_png_files_that_the_config_does_not_name(tmp_path):
    Image.new("RGBA", (8, 8)).save(tmp_path / "old.png")
    sheet = sheet_of(3, 3, [(0, 0, 1, 1, RED)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 0]}])
    assert ss.write_strips(ss.build_strips(config, sheet), tmp_path) == ["old.png"]
    assert (tmp_path / "old.png").exists()


def test_preview_writes_sheet_and_one_gif_per_animation(tmp_path):
    sheet = sheet_of(3, 8, [(0, 0, 1, 1, RED), (4, 0, 5, 1, BLUE)])
    config = config_of([{"name": "a", "row": 1, "frames": [0, 1]}])
    ss.write_preview(config, ss.build_strips(config, sheet), tmp_path)
    gif = Image.open(tmp_path / "a.gif")
    assert gif.n_frames == 2
    assert (tmp_path / "preview.png").stat().st_size > 0


def test_unknown_animation_key_raises_a_named_error():
    with pytest.raises(ss.SlicerError, match="offset"):
        config_of([{"name": "a", "row": 1, "frames": [0, 0], "offset": {0: [1, 0]}}])


def test_atlas_text_is_converted_to_the_format_of_the_game_atlases():
    packed = """T.png
size: 100, 200
format: RGBA8888
filter: Linear, Linear
repeat: none
pma: false
run
  bounds: 1, 2, 48, 96
stand
  bounds: 51, 1, 48, 48
"""
    assert ss.to_game_atlas(packed) == """T.png
size: 100, 200
format: RGBA8888
filter: Nearest,Nearest
repeat: none
run
  rotate: false
  xy: 1, 2
  size: 48, 96
  orig: 48, 96
  offset: 0, 0
  index: -1
stand
  rotate: false
  xy: 51, 1
  size: 48, 48
  orig: 48, 48
  offset: 0, 0
  index: -1
"""


def test_atlas_conversion_rejects_a_rotated_region():
    packed = "T.png\nsize: 1, 1\nformat: RGBA8888\nfilter: Linear, Linear\nrepeat: none\n" \
             "run\n  bounds: 1, 2, 48, 96\n  rotate: 90\n"
    with pytest.raises(ss.SlicerError, match="run"):
        ss.to_game_atlas(packed)
