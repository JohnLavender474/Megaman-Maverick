"""
Golden-file tests for the diagnostics analyzer.

Each test builds its output, writes it under temp/, and compares it byte-for-byte
with the committed file under expected/. temp/ is gitignored and is deliberately
left behind after a run so a failure can be diffed by hand.

When a change to analyze.py intentionally changes the output:
    1. ./run_tests.sh                                 # let the affected tests fail
    2. diff expected/summary.txt temp/summary.txt     # READ it, confirm it is intended
    3. cp temp/summary.txt expected/summary.txt       # update by hand
    4. ./run_tests.sh                                 # green
There is no --update-golden flag on purpose: every golden change gets reviewed.

The primary fixture's numbers are round and hand-derived (see assets/README-ish
notes in the plan): 10 frames, eight at 16 ms, one at 20 ms, one 40 ms spike.
"""

import os
import shutil
import subprocess
from pathlib import Path

import numpy as np
import pytest

from analyze import (
    Entry,
    build_summary,
    chart_plan,
    compute_y_clip,
    expected_chart_files,
    facet_grid_shape,
    find_latest_diagnostics,
    fold_series,
    parse_file,
    parse_frame_range,
    _CHARTS_PER_PARENT,
    _SERIES,
    _children_of,
    _color_for,
    _filename_stem,
    _label,
    _rolling_avg,
    _timings_by_path,
)

HERE = Path(__file__).parent
ASSETS = HERE / 'assets'
EXPECTED = HERE / 'expected'
TEMP = HERE / 'temp'

PRIMARY = ASSETS / 'primary-diagnostics.txt'
EDGE = ASSETS / 'edge-cases-diagnostics.txt'
NO_FRAMES = ASSETS / 'no-frames-diagnostics.txt'
EMPTY = ASSETS / 'empty-diagnostics.txt'

RUN_SH = HERE / 'run.sh'


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _check(text: str, name: str) -> None:
    """Write text to temp/<name> and assert it matches expected/<name>."""
    TEMP.mkdir(exist_ok=True)
    out_file = TEMP / name
    out_file.write_text(text)
    assert out_file.read_text() == (EXPECTED / name).read_text(), (
        f'{name} differs — diff expected/{name} temp/{name}'
    )


def _render_tree(frames) -> str:
    """Snapshot the parsed dataclass tree as indented text."""
    lines = []

    def walk(entry, indent):
        lines.append(f'{"  " * indent}{entry.name} [depth={entry.depth}] {entry.duration_ms:.3f}')
        for child in entry.children:
            walk(child, indent + 1)

    for frame in frames:
        lines.append(f'frame {frame.number} total={frame.total_ms:.3f} roots={len(frame.roots)}')
        for root in frame.roots:
            walk(root, 1)
    return '\n'.join(lines) + '\n'


def _render_series(order, series) -> str:
    lines = []
    for path in order:
        nums, ms = series[path]
        lines.append(' > '.join(path))
        lines.append(f'  frames    = {[int(n) for n in nums]}')
        lines.append(f'  durations = {[f"{v:.3f}" for v in ms]}')
        lines.append(f'  points    = {len(nums)}')
    return '\n'.join(lines) + '\n'


def _run_cli(args, cwd=HERE):
    return subprocess.run([str(RUN_SH), *args], cwd=cwd, capture_output=True, text=True)


def _saved_lines(stdout: str, output_dir: str) -> str:
    """The tool's ordered '  Saved: …' log, with the output dir prefix stripped."""
    names = [
        line.split('Saved:', 1)[1].strip()
        for line in stdout.splitlines() if 'Saved:' in line
    ]
    return '\n'.join(os.path.relpath(n, output_dir) for n in names) + '\n'


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------

def test_parse_primary_tree_matches_expected():
    _check(_render_tree(parse_file(str(PRIMARY))), 'parse_tree.txt')


def test_parse_edge_cases_tree_matches_expected():
    # Pins that the pre-header junk (including the well-formed 'orphan.entry'
    # line), the blank line, the prose line and the non-numeric duration are all
    # skipped, that frame-number gaps survive, and that the trailing
    # '=== Dropped N frames ===' line the writer emits is ignored.
    #
    # KNOWN LIMITATION, deliberately pinned: '\ttabIndented' is indented with a
    # tab, and _ENTRY_LINE's leading '( *)' does not consume tabs, so it parses
    # as a depth-0 root rather than a nested entry.
    _check(_render_tree(parse_file(str(EDGE))), 'parse_tree_edge.txt')


def test_parse_empty_and_no_frames_return_no_frames():
    assert parse_file(str(EMPTY)) == []
    assert parse_file(str(NO_FRAMES)) == []


# ---------------------------------------------------------------------------
# Path aggregation
# ---------------------------------------------------------------------------

def test_timings_by_path_matches_expected():
    # Pins first-seen ordering, cycle[1]+cycle[2] collapsing into one 'cycle'
    # series, preProcess summing across both cycles, and the sparse series
    # (AnimationsSystem missing from frame 3, audioMan from frames 5-8).
    order, series = _timings_by_path(parse_file(str(PRIMARY)))
    _check(_render_series(order, series), 'series.txt')


def test_timings_by_path_edge_matches_expected():
    order, series = _timings_by_path(parse_file(str(EDGE)))
    _check(_render_series(order, series), 'series_edge.txt')


def test_path_helpers_match_expected():
    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    lines = []
    for path in order:
        lines.append(f'path     = {path}')
        lines.append(f'  label  = {_label(path)}')
        lines.append(f'  stem   = {_filename_stem(path)}')
        lines.append(f'  children = {[p[-1] for p in _children_of(path, order)]}')
    _check('\n'.join(lines) + '\n', 'path_helpers.txt')


def test_filename_stem_collision_is_documented():
    # Two different paths can share a stem. Harmless today because the numeric
    # prefix disambiguates the actual filename; this assertion exists so that if
    # it ever does bite, a test names the cause.
    assert _filename_stem(('a.b',)) == _filename_stem(('a_b',)) == 'a_b'


def test_base_name_table_matches_expected():
    names = [
        'cycle', 'cycle[1]', 'cycle[12]', 'cycle[]', 'cycle[a]',
        'foo[1][2]', 'arr[0]tail', 'screen.render', 'begin/continue contacts',
    ]
    lines = [f'{name!r} -> {Entry(name, 0.0, 0).base_name!r}' for name in names]
    _check('\n'.join(lines) + '\n', 'base_names.txt')


# ---------------------------------------------------------------------------
# Smoothing
# ---------------------------------------------------------------------------

def test_rolling_avg_matches_expected():
    data = np.array([1.0, 2.0, 3.0, 4.0, 5.0])
    lines = []
    for window in (0, 1, 2, 3, 4, 5, 7):
        out = _rolling_avg(data, window)
        lines.append(f'window {window}: {[f"{v:.4f}" for v in out]}')
    lines.append(f'empty window 3: {list(_rolling_avg(np.array([]), 3))}')
    _check('\n'.join(lines) + '\n', 'rolling_avg.txt')


def test_rolling_avg_passthrough_is_identity():
    # The plot functions call this on every series; the no-op cases must not copy.
    data = np.array([1.0, 2.0, 3.0])
    empty = np.array([])
    assert _rolling_avg(data, 1) is data
    assert _rolling_avg(data, 0) is data
    assert _rolling_avg(empty, 30) is empty


# ---------------------------------------------------------------------------
# Input discovery
# ---------------------------------------------------------------------------

def test_find_latest_diagnostics():
    latest = find_latest_diagnostics(str(ASSETS / 'latest'))
    assert latest is not None
    # epoch-prefixed names ⇒ lexicographic max is the newest
    assert os.path.basename(latest) == '1700000001000-diagnostics.txt'

    TEMP.mkdir(exist_ok=True)
    empty_dir = TEMP / 'empty_assets'
    empty_dir.mkdir(exist_ok=True)
    assert find_latest_diagnostics(str(empty_dir)) is None
    assert find_latest_diagnostics(str(TEMP / 'does-not-exist')) is None


# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

def test_build_summary_matches_expected():
    # Hand-derivable from the fixture: totals are eight 16 ms frames, one 20 ms
    # and one 40 ms ⇒ mean 18.80, p99 38.20, exactly one spike frame (#9).
    # audioMan's mean is 0.20 — proving sparse series average over the frames in
    # which the entry was present, not over all 10.
    #
    # NOT covered by any fixture: the '(showing top 20 of N)' branch needs more
    # than 20 frames above p99, i.e. ~2100 frames, which is not hand-authorable.
    frames = parse_file(str(PRIMARY))
    order, series = _timings_by_path(frames)
    _check(build_summary(frames, order, series), 'summary.txt')


def test_build_summary_edge_matches_expected():
    # Frame #8 has no entries at all and is the lone spike, which is the only way
    # to reach the '(no root data)' contributor line.
    frames = parse_file(str(EDGE))
    order, series = _timings_by_path(frames)
    _check(build_summary(frames, order, series), 'summary_edge.txt')


def test_build_summary_empty_returns_empty_string():
    assert build_summary([], [], {}) == ''


def test_build_summary_with_frame_range():
    frames = [f for f in parse_file(str(PRIMARY)) if 2 <= f.number <= 5]
    order, series = _timings_by_path(frames)
    _check(build_summary(frames, order, series, (2, 5)), 'summary_range.txt')


def test_build_summary_frame_range_defaults_are_identical():
    # The whole golden-stability story rests on this: omitting frame_range and
    # passing None must produce byte-identical text.
    frames = parse_file(str(PRIMARY))
    order, series = _timings_by_path(frames)
    assert build_summary(frames, order, series) == build_summary(frames, order, series, None)


# ---------------------------------------------------------------------------
# Chart planning (pure — no matplotlib rendering)
# ---------------------------------------------------------------------------

def test_chart_plan_matches_expected():
    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    _check('\n'.join(expected_chart_files(order)) + '\n', 'chart_files.txt')


def test_chart_plan_edge_matches_expected():
    order, _ = _timings_by_path(parse_file(str(EDGE)))
    _check('\n'.join(expected_chart_files(order)) + '\n', 'chart_files_edge.txt')


def test_chart_plan_pairs_each_parent_with_its_children():
    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    for parent in chart_plan(order):
        assert parent.children == _children_of(parent.path, order)

        names = parent.filenames
        assert len(names) == _CHARTS_PER_PARENT
        # a parent's charts are numbered consecutively, in the order listed
        numbers = [int(n[:2]) for n in names]
        assert numbers == list(range(numbers[0], numbers[0] + _CHARTS_PER_PARENT))
        # every chart for one parent shares the path's stem
        stem = _filename_stem(parent.path)
        assert all(n[3:].startswith(stem) for n in names)


def test_chart_plan_does_not_depend_on_smoothing():
    # The file list must be predictable from the paths alone, so the smoothed
    # twins are emitted even at --smooth 1. Nothing in the plan takes a window.
    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    assert len(expected_chart_files(order)) == 4 + _CHARTS_PER_PARENT * len(chart_plan(order))


def test_color_for_refuses_to_cycle():
    # The executable form of "never cycle categorical hues": two different series
    # in the same colour cannot be told apart, so folding is mandatory.
    assert _color_for(0) == _SERIES[0]
    assert _color_for(len(_SERIES) - 1) == _SERIES[-1]
    with pytest.raises(AssertionError):
        _color_for(len(_SERIES))


def test_facet_grid_shape():
    assert facet_grid_shape(0) == (0, 0)
    assert facet_grid_shape(1) == (1, 1)
    assert facet_grid_shape(2) == (1, 2)
    assert facet_grid_shape(4) == (2, 2)
    assert facet_grid_shape(5) == (2, 3)
    assert facet_grid_shape(12) == (4, 3)
    for n in range(1, 14):
        nrows, ncols = facet_grid_shape(n)
        assert nrows * ncols >= n
        assert ncols <= 3


def test_fold_series_matches_expected():
    # 10 synthetic paths with known means: 7 keep slot colours, 3 fold into one
    # neutral 'Other' whose values are the summed tail.
    order = [(f's{i:02d}',) for i in range(10)]
    series = {p: (np.arange(5), np.full(5, float(10 - i)))
              for i, p in enumerate(order)}
    lines = ['--- 10 paths (folds) ---']
    for s in fold_series(order, series):
        lines.append(f'{s.label:<18} color={s.color} mean={s.mean:.3f} '
                     f'points={len(s.values)} other={s.is_other}')

    lines.append('--- 6 paths (no fold) ---')
    for s in fold_series(order[:6], series):
        lines.append(f'{s.label:<18} color={s.color} mean={s.mean:.3f} other={s.is_other}')

    lines.append('--- ties break on first-seen order ---')
    tied = [('b',), ('a',), ('c',)]
    tied_series = {p: (np.arange(3), np.full(3, 1.0)) for p in tied}
    for s in fold_series(tied, tied_series, max_series=2):
        lines.append(f'{s.label:<18} color={s.color} other={s.is_other}')

    _check('\n'.join(lines) + '\n', 'fold_series.txt')


def test_compute_y_clip_matches_expected():
    rng = np.random.default_rng(0)
    spiky = np.concatenate([np.full(999, 1.0), np.array([100.0])])
    cases = [
        ('empty', [np.array([])], 99.5),
        ('all zeros', [np.zeros(50)], 99.5),
        ('all equal', [np.full(100, 5.0)], 99.5),
        ('single point', [np.array([7.0])], 99.5),
        ('19 points (below min)', [np.full(19, 1.0) + rng.random(19)], 99.5),
        ('20 points, no outlier', [np.linspace(1.0, 2.0, 20)], 99.5),
        ('1000 points, 100x spike', [spiky], 99.5),
        ('same, percentile 0', [spiky], 0.0),
        ('same, percentile 50', [spiky], 50.0),
        ('with nan and inf', [np.array([1.0, np.nan, np.inf] + [2.0] * 30)], 99.5),
        ('99.9% zeros', [np.concatenate([np.zeros(999), np.array([5.0])])], 99.5),
        ('two arrays', [np.full(15, 1.0), np.full(15, 50.0)], 99.5),
    ]
    lines = []
    for name, arrays, pct in cases:
        c = compute_y_clip(arrays, pct)
        lines.append(f'{name:<26} top={c.top:>10.4f} applied={str(c.applied):<5} '
                     f'clipped={c.n_clipped:>4} total={c.n_total:>5} '
                     f'max={c.data_max:.4f}')
    _check('\n'.join(lines) + '\n', 'y_clip.txt')


def test_parse_frame_range_matches_expected():
    cases = ['0:0', '400:900', '400:', ':900', '0:', ':0',
             '', '1200', 'a:900', '400:b', '-5:900', ':', '900:100', '1:2:3', ' : ']
    lines = []
    for text in cases:
        try:
            lines.append(f'{text!r:<12} -> {parse_frame_range(text)}')
        except ValueError as exc:
            lines.append(f'{text!r:<12} -> ValueError: {exc}')
    _check('\n'.join(lines) + '\n', 'frame_range.txt')


# ---------------------------------------------------------------------------
# CLI end-to-end
#
# Relative paths and cwd=HERE keep stdout/stderr machine-independent.
# main()'s default-input path is deliberately untested: it reads the repo's
# gitignored assets/ dir, so it would pass or fail depending on whether the
# developer happens to have captured a session. find_latest_diagnostics, the
# function behind it, is covered above.
# ---------------------------------------------------------------------------

def test_cli_primary_end_to_end():
    out_dir = TEMP / 'cli_out'
    shutil.rmtree(out_dir, ignore_errors=True)

    result = _run_cli(['assets/primary-diagnostics.txt',
                       '--output-dir', 'temp/cli_out', '--smooth', '3'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'

    _check(_saved_lines(result.stdout, str(out_dir)), 'cli_saved.txt')

    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    expected_files = sorted(expected_chart_files(order) + ['summary.txt'])
    assert sorted(os.listdir(out_dir)) == expected_files

    for name in os.listdir(out_dir):
        path = out_dir / name
        assert path.stat().st_size > 0, f'{name} is empty'
        if name.endswith('.png'):
            assert path.read_bytes()[:4] == b'\x89PNG', f'{name} is not a PNG'

    # proves main() still routes the summary through build_summary
    assert (out_dir / 'summary.txt').read_text() == (EXPECTED / 'summary.txt').read_text()


def test_cli_edge_cases_end_to_end():
    # Real value here: matplotlib has to survive a single-point series, a frame
    # with no entries, and a gappy frame axis.
    out_dir = TEMP / 'cli_out_edge'
    shutil.rmtree(out_dir, ignore_errors=True)

    result = _run_cli(['assets/edge-cases-diagnostics.txt', '--output-dir', 'temp/cli_out_edge'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'

    _check(_saved_lines(result.stdout, str(out_dir)), 'cli_saved_edge.txt')

    order, _ = _timings_by_path(parse_file(str(EDGE)))
    assert sorted(os.listdir(out_dir)) == sorted(expected_chart_files(order) + ['summary.txt'])


def test_cli_missing_file_exits_1():
    result = _run_cli(['assets/does-not-exist.txt'])
    assert result.returncode == 1
    assert result.stderr.strip() == 'error: file not found: assets/does-not-exist.txt'


def test_cli_no_frames_exits_1():
    result = _run_cli(['assets/no-frames-diagnostics.txt',
                       '--output-dir', 'temp/cli_out_noframes'])
    assert result.returncode == 1
    assert result.stderr.strip() == 'error: no frame data found in file.'


def test_cli_empty_file_exits_1():
    result = _run_cli(['assets/empty-diagnostics.txt', '--output-dir', 'temp/cli_out_empty'])
    assert result.returncode == 1
    assert result.stderr.strip() == 'error: no frame data found in file.'


def test_cli_wide_capture_end_to_end():
    # The only end-to-end coverage of the fold path, the 'Other' band, the facet
    # panel cut and the clip note all firing at once: 12 roots (folds to 7+Other)
    # and a parent with 14 children (cuts to 11 panels + Other).
    out_dir = TEMP / 'cli_out_wide'
    shutil.rmtree(out_dir, ignore_errors=True)

    result = _run_cli(['assets/wide-diagnostics.txt',
                       '--output-dir', 'temp/cli_out_wide', '--smooth', '5'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'

    _check(_saved_lines(result.stdout, str(out_dir)), 'cli_saved_wide.txt')

    order, _ = _timings_by_path(parse_file(str(ASSETS / 'wide-diagnostics.txt')))
    assert sorted(os.listdir(out_dir)) == sorted(expected_chart_files(order) + ['summary.txt'])
    for name in os.listdir(out_dir):
        path = out_dir / name
        assert path.stat().st_size > 0, f'{name} is empty'
        if name.endswith('.png'):
            assert path.read_bytes()[:4] == b'\x89PNG', f'{name} is not a PNG'


def test_cli_frames_range_end_to_end():
    out_dir = TEMP / 'cli_out_frames'
    shutil.rmtree(out_dir, ignore_errors=True)

    result = _run_cli(['assets/primary-diagnostics.txt',
                       '--output-dir', 'temp/cli_out_frames',
                       '--frames', '2:5', '--smooth', '3'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'

    # the file list follows the FILTERED frames — a window can contain fewer paths
    frames = [f for f in parse_file(str(PRIMARY)) if 2 <= f.number <= 5]
    order, _ = _timings_by_path(frames)
    assert sorted(os.listdir(out_dir)) == sorted(expected_chart_files(order) + ['summary.txt'])

    summary = (out_dir / 'summary.txt').read_text()
    assert 'Frames analyzed : 4' in summary
    assert 'Frame range     : 2–5' in summary


def test_cli_frames_invalid_exits_1():
    for arg, expected in (
        ('9:2', 'error: --frames start 9 is greater than end 2'),
        ('abc', "error: --frames must look like START:END, START: or :END (got 'abc')"),
        ('900:1000', 'error: no frames in range 900:1000 (capture covers 0–9)'),
    ):
        result = _run_cli(['assets/primary-diagnostics.txt',
                           '--output-dir', 'temp/cli_out_badrange', '--frames', arg])
        assert result.returncode == 1, f'{arg}: {result.stdout}'
        assert result.stderr.strip() == expected, arg


def test_cli_no_clip_end_to_end():
    # --no-clip changes pixels only; it must never change which files are written.
    out_dir = TEMP / 'cli_out_noclip'
    shutil.rmtree(out_dir, ignore_errors=True)

    result = _run_cli(['assets/primary-diagnostics.txt',
                       '--output-dir', 'temp/cli_out_noclip', '--no-clip'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'

    order, _ = _timings_by_path(parse_file(str(PRIMARY)))
    assert sorted(os.listdir(out_dir)) == sorted(expected_chart_files(order) + ['summary.txt'])


def test_cli_default_output_dir():
    # Pins the '<stem>_output/' derivation without writing into assets/.
    TEMP.mkdir(exist_ok=True)
    copied = TEMP / 'copy-diagnostics.txt'
    shutil.copy(PRIMARY, copied)
    shutil.rmtree(TEMP / 'copy-diagnostics_output', ignore_errors=True)

    result = _run_cli(['temp/copy-diagnostics.txt'])
    assert result.returncode == 0, f'{result.stdout}\n{result.stderr}'
    assert (TEMP / 'copy-diagnostics_output' / 'summary.txt').is_file()
