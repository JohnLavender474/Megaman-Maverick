#!/usr/bin/env python3
"""
Megaman Maverick Runtime Diagnostics Analyzer

Parses a *-diagnostics.txt file produced by RuntimeDiagnostics and generates
per-frame timing charts plus a text summary.

Output charts:
  01_frame_overview.png            — total frame time, raw + rolling average
  02_frame_overview_smoothed.png   — the same, rolling average only
  03_root_processes.png            — one line per root entry
  04_root_processes_smoothed.png   — the same, rolling average only
  then, for every entry that has children, at any depth:
  NN_<path>_subprocesses.png           — children overlaid on a shared y-axis
  NN_<path>_subprocesses_smoothed.png  — the same, rolling average only
  NN_<path>_facets.png                 — one panel per child, each own y-scale
  NN_<path>_facets_smoothed.png        — the same, rolling average only
  NN_<path>_stacked.png                — children stacked, absolute ms
  NN_<path>_stacked_pct.png            — children stacked, % of parent
  summary.txt                          — the numbers, at every depth

Usage:
  python analyze.py [FILE] [--output-dir DIR] [--smooth N]
                    [--frames START:END] [--clip-percentile P | --no-clip]

  FILE       Path to a diagnostics file. Defaults to the newest
             *-diagnostics.txt found in assets/ relative to the repo root.
  --smooth   Rolling-average window in frames (default: 30, set to 1 to
             disable smoothing).
  --frames   Restrict to an inclusive frame-number range, e.g. 400:900.
  --clip-percentile / --no-clip
             Control the outlier-resistant y-axis (see compute_y_clip).
"""

import argparse
import glob
import math
import os
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Sequence, Tuple

import matplotlib
matplotlib.use('Agg')  # off-screen rendering; no display required
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np


# ---------------------------------------------------------------------------
# Compiled patterns
# ---------------------------------------------------------------------------

_FRAME_HEADER = re.compile(r'=== Frame #(\d+) \(([0-9.]+)ms\) ===')
_ENTRY_LINE   = re.compile(r'^( *)(.+?): ([0-9.]+)ms\s*$')
_ITER_SUFFIX  = re.compile(r'\[\d+\]$')   # strips "[1]", "[12]", etc.


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

@dataclass
class Entry:
    name: str
    duration_ms: float
    depth: int
    children: List['Entry'] = field(default_factory=list)

    @property
    def base_name(self) -> str:
        """'cycle[3]' → 'cycle'.  All other names are returned unchanged."""
        return _ITER_SUFFIX.sub('', self.name)


@dataclass
class Frame:
    number: int
    total_ms: float
    roots: List[Entry] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------

def parse_file(path: str) -> List[Frame]:
    """Read a diagnostics file and return a list of Frame objects."""
    frames: List[Frame] = []
    current: Optional[Frame] = None
    # Each element is the Entry that is currently "open" at that depth.
    stack: List[Entry] = []

    with open(path, 'r') as fh:
        for raw in fh:
            line = raw.rstrip('\n')
            if not line.strip():
                continue

            m = _FRAME_HEADER.match(line)
            if m:
                current = Frame(number=int(m.group(1)), total_ms=float(m.group(2)))
                frames.append(current)
                stack.clear()
                continue

            if current is None:
                continue

            m = _ENTRY_LINE.match(line)
            if not m:
                continue

            spaces, name, duration = m.group(1), m.group(2).strip(), float(m.group(3))
            depth = len(spaces) // 2
            entry = Entry(name=name, duration_ms=duration, depth=depth)

            # Pop ancestors that are at the same level or deeper.
            while stack and stack[-1].depth >= depth:
                stack.pop()

            if stack:
                stack[-1].children.append(entry)
            else:
                current.roots.append(entry)

            stack.append(entry)

    return frames


def parse_frame_range(text: str) -> Tuple[Optional[int], Optional[int]]:
    """
    '400:900' → (400, 900);  '400:' → (400, None);  ':900' → (None, 900).

    Bounds are inclusive FRAME NUMBERS, not indices — frame numbers are what the
    x-axis and the summary's spike table show, and captures can have gaps where
    the writer dropped frames.

    Raises ValueError with a user-facing message.
    """
    if text.count(':') != 1:
        raise ValueError(
            f'--frames must look like START:END, START: or :END (got {text!r})')

    start_text, end_text = text.split(':')
    if not start_text and not end_text:
        raise ValueError(f'--frames needs at least one bound (got {text!r})')

    def _bound(part: str) -> Optional[int]:
        if not part:
            return None
        try:
            value = int(part)
        except ValueError:
            raise ValueError(
                f'--frames bounds must be whole numbers (got {text!r})') from None
        if value < 0:
            raise ValueError(
                f'--frames bounds must be non-negative (got {text!r})')
        return value

    start, end = _bound(start_text), _bound(end_text)
    if start is not None and end is not None and start > end:
        raise ValueError(f'--frames start {start} is greater than end {end}')

    return start, end


# ---------------------------------------------------------------------------
# Data extraction helpers
# ---------------------------------------------------------------------------

# An entry is identified by its *path*: the tuple of base names from the root down
# to the entry itself, e.g. ('screen.render', 'WorldSystem', 'cycle'). Working in
# paths rather than in root/child pairs lets every level of the tree be charted,
# however deeply the game nests its instrumentation.
Path = Tuple[str, ...]


def _walk(entry: Entry, prefix: Path, visit) -> None:
    """Depth-first walk, calling visit(path, entry) for the entry and its descendants."""
    path = prefix + (entry.base_name,)
    visit(path, entry)
    for child in entry.children:
        _walk(child, path, visit)


def _timings_by_path(frames: List[Frame]) -> Tuple[List[Path], Dict[Path, Tuple[np.ndarray, np.ndarray]]]:
    """
    Returns (paths in first-seen order, {path: (frame_numbers, durations_ms)}).

    Entries sharing a path within a frame are summed. That collapses all of
    cycle[1], cycle[2], … into a single 'cycle' series whose value is the total
    time spent in cycles that frame, and likewise merges any system that runs
    more than once per frame.
    """
    totals: Dict[Path, Dict[int, float]] = defaultdict(lambda: defaultdict(float))
    order: List[Path] = []
    seen = set()

    for frame in frames:
        def visit(path: Path, entry: Entry, number: int = frame.number) -> None:
            if path not in seen:
                seen.add(path)
                order.append(path)
            totals[path][number] += entry.duration_ms

        for root in frame.roots:
            _walk(root, (), visit)

    series: Dict[Path, Tuple[np.ndarray, np.ndarray]] = {}
    for path in order:
        by_frame = totals[path]
        numbers = sorted(by_frame.keys())
        series[path] = (
            np.array(numbers, dtype=int),
            np.array([by_frame[n] for n in numbers], dtype=float),
        )
    return order, series


def _children_of(path: Path, order: List[Path]) -> List[Path]:
    """Paths one level below the given path, in first-seen order."""
    depth = len(path)
    return [p for p in order if len(p) == depth + 1 and p[:depth] == path]


def _label(path: Path) -> str:
    """'screen.render › WorldSystem › cycle' — for chart titles."""
    return ' › '.join(path)


def _filename_stem(path: Path) -> str:
    return re.sub(r'[^A-Za-z0-9_]', '_', '__'.join(path))


# ---------------------------------------------------------------------------
# Chart planning
# ---------------------------------------------------------------------------

CHART_OVERVIEW          = '01_frame_overview.png'
CHART_OVERVIEW_SMOOTHED = '02_frame_overview_smoothed.png'
CHART_ROOTS             = '03_root_processes.png'
CHART_ROOTS_SMOOTHED    = '04_root_processes_smoothed.png'

_TOP_LEVEL_CHARTS = [CHART_OVERVIEW, CHART_OVERVIEW_SMOOTHED,
                     CHART_ROOTS, CHART_ROOTS_SMOOTHED]

_CHARTS_PER_PARENT = 6


@dataclass(frozen=True)
class ParentCharts:
    """The six charts emitted for one entry that has children."""
    path: Path
    children: List[Path]
    overlay: str
    overlay_smoothed: str
    facets: str
    facets_smoothed: str
    stacked_ms: str
    stacked_pct: str

    @property
    def filenames(self) -> List[str]:
        return [self.overlay, self.overlay_smoothed, self.facets,
                self.facets_smoothed, self.stacked_ms, self.stacked_pct]


def chart_plan(order: List[Path]) -> List[ParentCharts]:
    """
    One ParentCharts per entry that has children, at any depth, numbered from 05
    in first-seen order.

    Kept free of matplotlib so the numbering and the filename derivation can be
    tested without rendering anything. The result depends only on `order` — never
    on the smoothing window — so the file list is predictable from the paths alone.
    """
    plan: List[ParentCharts] = []
    index = len(_TOP_LEVEL_CHARTS) + 1

    for path in order:
        children = _children_of(path, order)
        if not children:
            continue

        stem = _filename_stem(path)
        plan.append(ParentCharts(
            path=path,
            children=children,
            overlay=f'{index:02d}_{stem}_subprocesses.png',
            overlay_smoothed=f'{index + 1:02d}_{stem}_subprocesses_smoothed.png',
            facets=f'{index + 2:02d}_{stem}_facets.png',
            facets_smoothed=f'{index + 3:02d}_{stem}_facets_smoothed.png',
            stacked_ms=f'{index + 4:02d}_{stem}_stacked.png',
            stacked_pct=f'{index + 5:02d}_{stem}_stacked_pct.png',
        ))
        index += _CHARTS_PER_PARENT

    return plan


def expected_chart_files(order: List[Path]) -> List[str]:
    """Every chart filename a run over these paths should produce, in order."""
    names = list(_TOP_LEVEL_CHARTS)
    for parent in chart_plan(order):
        names.extend(parent.filenames)
    return names


# ---------------------------------------------------------------------------
# Smoothing
# ---------------------------------------------------------------------------

def _rolling_avg(data: np.ndarray, window: int) -> np.ndarray:
    if window <= 1 or len(data) == 0:
        return data
    # 'same' returns max(len(data), len(kernel)) elements, so a window wider than
    # the series would return MORE points than it was given and the plot call
    # would fail with a shape mismatch. Short captures and sparse series (an entry
    # present in only a few frames) both hit that, so clamp the window.
    window = min(window, len(data))
    kernel = np.ones(window) / window
    # 'same' mode keeps the array length; edges are naturally lighter-weighted.
    return np.convolve(data, kernel, mode='same')


# ---------------------------------------------------------------------------
# Palette and chrome
#
# The categorical slots are the validated default order: worst adjacent CVD
# ΔE 9.1, worst adjacent normal-vision ΔE 19.6. Three slots sit below 3:1 against
# the light surface, which obliges visible labels or a table view — the legend
# labels and summary.txt discharge that. Slots are assigned in fixed order and
# NEVER cycled; past the ceiling the tail folds into a neutral 'Other'.
# ---------------------------------------------------------------------------

_SURFACE   = '#fcfcfb'
_INK       = '#0b0b0b'
_INK_MUTED = '#52514e'
_AXIS      = '#898781'
_GRID      = '#e1e0d9'
_BASELINE  = '#c3c2b7'
_RAW       = '#c3c2b7'   # the un-smoothed ghost line
_OTHER     = '#898781'   # the folded tail — neutral, never a hue

_SERIES = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100',
           '#e87ba4', '#008300', '#4a3aa7', '#e34948']
_MAX_SERIES = 7          # 7 hued + 'Other' = the 8-hue ceiling

_DPI = 200
_CHART_WIDTH = 16.0
_LEGEND_MAX_COLS = 4

_TARGET_60FPS_MS = 1000.0 / 60.0
_TARGET_30FPS_MS = 1000.0 / 30.0


def _apply_style() -> None:
    plt.rcParams.update({
        'figure.facecolor': _SURFACE, 'axes.facecolor': _SURFACE,
        'savefig.facecolor': _SURFACE,
        'axes.edgecolor': _BASELINE, 'axes.linewidth': 0.8,
        'axes.spines.top': False, 'axes.spines.right': False,
        'axes.titlecolor': _INK, 'axes.labelcolor': _INK_MUTED,
        'axes.labelsize': 10, 'axes.titlesize': 13,
        'grid.color': _GRID, 'grid.linestyle': '-', 'grid.linewidth': 0.8,
        'grid.alpha': 1.0,
        'xtick.color': _AXIS, 'ytick.color': _AXIS,
        'xtick.labelcolor': _INK_MUTED, 'ytick.labelcolor': _INK_MUTED,
        'xtick.labelsize': 9, 'ytick.labelsize': 9,
        'xtick.major.size': 0, 'ytick.major.size': 0,
        'xtick.minor.size': 0, 'ytick.minor.size': 0,
        'text.color': _INK,
        'legend.frameon': False, 'legend.fontsize': 9,
        'font.family': 'sans-serif',
        'figure.dpi': _DPI, 'savefig.dpi': _DPI,
    })


def _color_for(index: int) -> str:
    """
    Slot colour for series `index`. Callers must fold past _MAX_SERIES; this
    asserts rather than cycling, because a repeated hue is a lie — two different
    series rendered in the same colour cannot be told apart.
    """
    if index >= len(_SERIES):
        raise AssertionError(
            f'series index {index} is past the {len(_SERIES)}-hue ceiling; fold first')
    return _SERIES[index]


# ---------------------------------------------------------------------------
# Outlier-resistant y-axis
# ---------------------------------------------------------------------------

_CLIP_PERCENTILE = 99.5
_CLIP_HEADROOM   = 1.15
_CLIP_MIN_POINTS = 20


@dataclass(frozen=True)
class YClip:
    top: float
    applied: bool
    n_clipped: int
    n_total: int
    data_max: float


def compute_y_clip(arrays: Sequence[np.ndarray],
                   percentile: float = _CLIP_PERCENTILE,
                   headroom: float = _CLIP_HEADROOM) -> YClip:
    """
    A y-limit that a single outlier cannot inflate.

    p99.5 rather than p95: on a 2000-frame capture p95 would push 100 frames off
    the top, far too many for a tool whose job is finding spikes, while p99.5
    hides ~10 and still collapses the axis by the 3-10x a GC pause causes.

    Returns applied=False (no clipping) whenever clipping would be meaningless or
    destructive: nothing finite to plot, everything at or below zero, percentile
    disabled, too few points for a percentile to differ from the max, a threshold
    of zero (a mostly-zero series, where clipping would blank the chart), or no
    value above the threshold in the first place. All-equal data lands in that
    last case.
    """
    finite = [a[np.isfinite(a)] for a in arrays if len(a)]
    values = np.concatenate(finite) if finite else np.array([])

    n_total = int(values.size)
    if n_total == 0:
        return YClip(top=1.0, applied=False, n_clipped=0, n_total=0, data_max=0.0)

    data_max = float(values.max())
    unclipped = YClip(top=max(data_max * 1.05, 1e-9), applied=False,
                      n_clipped=0, n_total=n_total, data_max=data_max)

    if data_max <= 0.0:
        return YClip(top=1.0, applied=False, n_clipped=0,
                     n_total=n_total, data_max=data_max)
    if percentile <= 0.0 or n_total < _CLIP_MIN_POINTS:
        return unclipped

    threshold = float(np.percentile(values, percentile))
    if threshold <= 0.0:
        return unclipped

    top = threshold * headroom
    if data_max <= top:
        return unclipped

    return YClip(top=top, applied=True, n_clipped=int((values > top).sum()),
                 n_total=n_total, data_max=data_max)


def _apply_y_clip(ax: plt.Axes, clip: YClip) -> str:
    """Set the limit and return the note describing what was pushed out of view."""
    ax.set_ylim(0.0, clip.top)
    if not clip.applied:
        return ''
    return (f'y-axis clipped at p{_CLIP_PERCENTILE:g} — '
            f'{clip.n_clipped:,} of {clip.n_total:,} points above, '
            f'max {clip.data_max:,.2f} ms')


def _mark_clipped(ax: plt.Axes, nums: np.ndarray, values: np.ndarray,
                  clip: YClip, color: str) -> None:
    """
    A caret at the ceiling for every point that ran off the chart, positioned in
    x — so clipping hides the magnitude but never the existence or the location
    of a spike.
    """
    if not clip.applied or len(values) == 0:
        return
    mask = values > clip.top
    if not mask.any():
        return
    ax.plot(nums[mask], np.full(int(mask.sum()), clip.top), linestyle='none',
            marker='^', markersize=3.0, markeredgewidth=0,
            color=color, alpha=0.9, clip_on=False, zorder=5)


# ---------------------------------------------------------------------------
# Series folding
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class PlotSeries:
    label: str
    nums: np.ndarray
    values: np.ndarray
    color: str
    mean: float
    is_other: bool = False


def _align(nums_list: Sequence[np.ndarray],
           values_list: Sequence[np.ndarray]) -> Tuple[np.ndarray, np.ndarray]:
    """
    A common frame axis plus a (n_series, n_frames) matrix, zero-filled where a
    series had no entry that frame. Zero-fill is right for summing and stacking;
    line charts keep their native arrays so a sparse entry shows a gap instead of
    diving to zero.
    """
    all_frames = sorted({int(n) for nums in nums_list for n in nums.tolist()})
    frame_arr = np.array(all_frames, dtype=int)
    index = {fn: i for i, fn in enumerate(all_frames)}

    matrix = np.zeros((len(nums_list), len(all_frames)), dtype=float)
    for row, (nums, values) in enumerate(zip(nums_list, values_list)):
        for fn, value in zip(nums.tolist(), values.tolist()):
            matrix[row, index[int(fn)]] = value

    return frame_arr, matrix


def fold_series(paths: List[Path],
                series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                max_series: int = _MAX_SERIES,
                assign_colors: bool = True) -> List[PlotSeries]:
    """
    Rank by descending mean, keep the top `max_series` with fixed slot colours,
    and sum everything else into one neutral-grey 'Other'.

    Ranking uses the same key summary.txt sorts by, so the chart and the table
    agree on what matters. Ties break on first-seen order, keeping the output
    deterministic and golden-testable.

    assign_colors=False gives every kept series the emphasis hue instead of a
    slot. Faceted charts want that: each series has its own panel, so position
    and title carry identity and colour means nothing — which is also why a facet
    grid may keep more series than the hue ceiling allows.
    """
    if not paths:
        return []

    ranked = sorted(
        enumerate(paths),
        key=lambda pair: (-float(np.mean(series[pair[1]][1])), pair[0]),
    )

    head = [p for _, p in ranked[:max_series]]
    tail = [p for _, p in ranked[max_series:]]

    plotted = [
        PlotSeries(label=path[-1], nums=series[path][0], values=series[path][1],
                   color=_color_for(i) if assign_colors else _SERIES[0],
                   mean=float(np.mean(series[path][1])))
        for i, path in enumerate(head)
    ]

    if tail:
        frame_arr, matrix = _align([series[p][0] for p in tail],
                                   [series[p][1] for p in tail])
        totals = matrix.sum(axis=0)
        plotted.append(PlotSeries(
            label=f'Other ({len(tail)} series)', nums=frame_arr, values=totals,
            color=_OTHER, mean=float(np.mean(totals)), is_other=True,
        ))

    return plotted


# ---------------------------------------------------------------------------
# Chart chrome helpers
# ---------------------------------------------------------------------------

def _legend_rows(n_labels: int, max_cols: int = _LEGEND_MAX_COLS) -> int:
    if n_labels <= 0:
        return 0
    return math.ceil(n_labels / min(n_labels, max_cols))


def _make_figure(plot_height: float, n_labels: int,
                 width: float = _CHART_WIDTH) -> Tuple[plt.Figure, plt.Axes]:
    """
    A figure tall enough that the legend sits BELOW the plot box instead of on top
    of the data. The legend costs height, which is added to the figure, so the
    plot box keeps its full width.

    tight_layout is deliberately not used anywhere — it does not reserve space for
    a figure legend and would let it collide with the x-axis label.
    """
    rows = _legend_rows(n_labels)
    legend_in = 0.0 if rows == 0 else 0.26 * rows + 0.22
    chrome_in = 0.62      # x tick labels + x label
    title_in = 0.85       # title + subtitle band

    fig_height = plot_height + legend_in + chrome_in + title_in
    fig = plt.figure(figsize=(width, fig_height))

    bottom = (legend_in + chrome_in) / fig_height
    top = 1.0 - title_in / fig_height
    ax = fig.add_axes([0.065, bottom, 0.915, top - bottom])
    return fig, ax


def _legend_below(fig: plt.Figure, handles, labels,
                  max_cols: int = _LEGEND_MAX_COLS) -> None:
    if not labels:
        return
    fig.legend(handles, labels, loc='lower center', bbox_to_anchor=(0.5, 0.008),
               ncol=min(len(labels), max_cols), frameon=False, fontsize=9,
               handlelength=1.8, handletextpad=0.5, columnspacing=2.2,
               labelspacing=0.4, borderaxespad=0.0)


def _decorate_axes(ax: plt.Axes, title: str, subtitle: str = '', note: str = '',
                   ylabel: str = 'Time (ms)', xlabel: str = 'Frame') -> None:
    """
    Title, subtitle, note and recessive chrome. Deliberately does NOT set the
    y-limit or draw a legend — callers own both, because doing them here is what
    put the legend on top of the data and let outliers set the axis.
    """
    ax.set_title(title, loc='left', fontsize=13, color=_INK,
                 pad=20 if (subtitle or note) else 8)
    if subtitle:
        ax.text(0.0, 1.012, subtitle, transform=ax.transAxes,
                ha='left', va='bottom', fontsize=9, color=_AXIS)
    if note:
        ax.text(1.0, 1.012, note, transform=ax.transAxes,
                ha='right', va='bottom', fontsize=9, color=_AXIS)

    ax.set_xlabel(xlabel, fontsize=10)
    ax.set_ylabel(ylabel, fontsize=10)
    ax.grid(axis='y', which='major')
    ax.grid(axis='x', visible=False)      # x grid is noise at ~1px per frame
    ax.yaxis.set_minor_locator(ticker.NullLocator())
    ax.yaxis.set_major_locator(ticker.MaxNLocator(6))
    ax.xaxis.set_major_formatter(ticker.StrMethodFormatter('{x:,.0f}'))
    for side in ('left', 'bottom'):
        ax.spines[side].set_color(_BASELINE)


def _set_x_extent(ax: plt.Axes, nums_list: Sequence[np.ndarray]) -> None:
    """Explicit limits, no 5% dead margin — that margin is part of the 'zoomed out' feel."""
    lows = [float(n.min()) for n in nums_list if len(n)]
    highs = [float(n.max()) for n in nums_list if len(n)]
    if not lows:
        return
    lo, hi = min(lows), max(highs)
    if lo == hi:                       # a single-point series
        lo, hi = lo - 0.5, hi + 0.5
    ax.set_xlim(lo, hi)
    ax.margins(x=0)


def _draw_budget_lines(ax: plt.Axes) -> None:
    """
    60/30 FPS budgets, drawn only where they already fit inside the current ylim.

    Order matters: this runs AFTER the limit is set from the data. Drawn
    unconditionally, the 33.3 ms line alone made a game running at ~10 ms/frame
    render its data in the bottom third of the chart.
    """
    lo, hi = ax.get_ylim()
    for value, label in ((_TARGET_60FPS_MS, '60 FPS  16.7 ms'),
                         (_TARGET_30FPS_MS, '30 FPS  33.3 ms')):
        if not lo < value < hi:
            continue
        ax.axhline(value, color=_BASELINE, linewidth=0.8, zorder=0)
        ax.annotate(label, xy=(1.0, value), xycoords=('axes fraction', 'data'),
                    xytext=(-4, 3), textcoords='offset points',
                    ha='right', va='bottom', fontsize=8.5, color=_AXIS)


def _fmt_ms(value: float) -> str:
    """Enough decimals to be informative — sub-millisecond entries are common."""
    if value >= 1.0:
        return f'{value:.2f}'
    if value >= 0.01:
        return f'{value:.3f}'
    return f'{value:.4f}'


def _subtitle(smooth: int, show_raw: bool, n_frames: int,
              frame_range: Optional[Tuple[int, int]]) -> str:
    """The smoothing window, stated once per chart rather than on every legend row."""
    if smooth <= 1:
        parts = ['no smoothing']
    elif show_raw:
        parts = [f'{smooth}-frame rolling average; raw values in light grey']
    else:
        parts = [f'{smooth}-frame rolling average only']

    span = f'{n_frames:,} frames'
    if frame_range is not None:
        span += f' ({frame_range[0]:,}–{frame_range[1]:,})'
    parts.append(span)
    return '  ·  '.join(parts)


# ---------------------------------------------------------------------------
# Charts
# ---------------------------------------------------------------------------

def plot_frame_overview(frames: List[Frame], output_dir: str, filename: str,
                        smooth: int, clip_pct: float,
                        frame_range: Optional[Tuple[int, int]],
                        show_raw: bool) -> None:
    nums = np.array([f.number for f in frames], dtype=int)
    ms = np.array([f.total_ms for f in frames], dtype=float)

    # A single series needs no legend box — the title names it.
    fig, ax = _make_figure(plot_height=4.2, n_labels=0)

    clip = compute_y_clip([ms], clip_pct)
    if show_raw:
        ax.plot(nums, ms, color=_RAW, linewidth=0.6, zorder=1)
    smoothed = _rolling_avg(ms, smooth) if smooth > 1 else ms
    ax.plot(nums, smoothed, color=_SERIES[0], linewidth=1.6, zorder=3)

    _set_x_extent(ax, [nums])
    note = _apply_y_clip(ax, clip)
    if show_raw:
        _mark_clipped(ax, nums, ms, clip, _INK_MUTED)
    _draw_budget_lines(ax)
    _decorate_axes(ax, 'Total Frame Time',
                   subtitle=_subtitle(smooth, show_raw, len(frames), frame_range),
                   note=note)
    _save(fig, output_dir, filename)


def _plot_lines(plotted: List[PlotSeries], output_dir: str, filename: str,
                title: str, subtitle: str, smooth: int, clip_pct: float,
                show_raw: bool, budget_lines: bool) -> None:
    """Shared body of the root-processes and sub-process overlay charts."""
    if not plotted:
        return

    fig, ax = _make_figure(plot_height=5.0, n_labels=len(plotted))
    clip = compute_y_clip([s.values for s in plotted], clip_pct)

    handles, labels = [], []
    for s in plotted:
        if show_raw:
            ax.plot(s.nums, s.values, color=s.color, linewidth=0.4, alpha=0.28, zorder=1)
        smoothed = _rolling_avg(s.values, smooth) if smooth > 1 else s.values
        handle, = ax.plot(s.nums, smoothed, color=s.color, linewidth=1.6,
                          label=s.label, zorder=3)
        if show_raw:
            _mark_clipped(ax, s.nums, s.values, clip, s.color)
        handles.append(handle)
        labels.append(s.label)

    _set_x_extent(ax, [s.nums for s in plotted])
    note = _apply_y_clip(ax, clip)
    if budget_lines:
        _draw_budget_lines(ax)
    _decorate_axes(ax, title, subtitle=subtitle, note=note)
    _legend_below(fig, handles, labels)
    _save(fig, output_dir, filename)


def plot_root_processes(order: List[Path],
                        series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                        output_dir: str, filename: str, smooth: int,
                        clip_pct: float, frame_range: Optional[Tuple[int, int]],
                        show_raw: bool, n_frames: int) -> None:
    roots = [p for p in order if len(p) == 1]
    plotted = fold_series(roots, series)
    _plot_lines(plotted, output_dir, filename, 'Root Process Times per Frame',
                _subtitle(smooth, show_raw, n_frames, frame_range),
                smooth, clip_pct, show_raw, budget_lines=True)


def plot_subprocess_breakdown(path: Path, children: List[Path],
                              series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                              output_dir: str, filename: str, smooth: int,
                              clip_pct: float,
                              frame_range: Optional[Tuple[int, int]],
                              show_raw: bool, n_frames: int) -> None:
    """Children overlaid on one shared y-axis — good for comparing siblings directly."""
    plotted = fold_series(children, series)
    _plot_lines(plotted, output_dir, filename,
                f'{_label(path)} — Sub-process Breakdown',
                _subtitle(smooth, show_raw, n_frames, frame_range),
                smooth, clip_pct, show_raw, budget_lines=False)


# --- facet grid -------------------------------------------------------------

_FACET_MAX = 12
_FACET_PANEL_W = 5.2
_FACET_PANEL_H = 1.75


def facet_grid_shape(n: int) -> Tuple[int, int]:
    """(nrows, ncols) for n panels. Pure — testable without matplotlib."""
    if n <= 0:
        return (0, 0)
    ncols = 1 if n <= 1 else 2 if n <= 4 else 3
    return math.ceil(n / ncols), ncols


def _facet_panels(children: List[Path],
                  series: Dict[Path, Tuple[np.ndarray, np.ndarray]]) -> List[PlotSeries]:
    """
    One entry per panel, ranked by mean. Past _FACET_MAX children the tail becomes
    a single 'Other' panel rather than a second file — paginating would add
    filenames, and summary.txt already lists the tail exhaustively.
    """
    return fold_series(children, series, max_series=_FACET_MAX - 1, assign_colors=False)


def plot_subprocess_facets(path: Path, children: List[Path],
                           series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                           output_dir: str, filename: str, smooth: int,
                           clip_pct: float,
                           frame_range: Optional[Tuple[int, int]],
                           show_raw: bool, n_frames: int) -> None:
    """
    Small multiples — one panel per child, EACH WITH ITS OWN Y-SCALE.

    This is the only view in which a 0.001 ms child's shape is as legible as a
    4 ms sibling's; on the shared-scale overlay the small one is a flat line on
    the baseline. Every panel uses the same hue on purpose: position and title
    carry identity, so no colour is generated and the hue ceiling never applies.
    """
    panels = _facet_panels(children, series)
    if not panels:
        return

    nrows, ncols = facet_grid_shape(len(panels))
    legend_labels = ['Raw', f'{smooth}-frame rolling average'] if (show_raw and smooth > 1) else []
    legend_in = 0.0 if not legend_labels else 0.48
    header_in = 1.05

    fig_height = _FACET_PANEL_H * nrows + header_in + legend_in + 0.35
    fig, axes = plt.subplots(nrows, ncols, squeeze=False, sharex=True, sharey=False,
                             figsize=(_FACET_PANEL_W * ncols, fig_height))

    handles: List = []
    for i, ax in enumerate(axes.flat):
        if i >= len(panels):
            ax.set_axis_off()
            continue

        s = panels[i]
        clip = compute_y_clip([s.values], clip_pct)

        if show_raw:
            raw_handle, = ax.plot(s.nums, s.values, color=_RAW, linewidth=0.5, alpha=0.55)
        smoothed = _rolling_avg(s.values, smooth) if smooth > 1 else s.values
        avg_handle, = ax.plot(s.nums, smoothed, color=_SERIES[0], linewidth=1.4)
        if show_raw and not handles and legend_labels:
            handles = [raw_handle, avg_handle]

        _set_x_extent(ax, [s.nums])
        note = _apply_y_clip(ax, clip)
        if show_raw:
            _mark_clipped(ax, s.nums, s.values, clip, _INK_MUTED)

        # The mean in the title restores the magnitude that independent y-scales remove.
        ax.set_title(f'{s.label}   mean {_fmt_ms(s.mean)} ms', loc='left', fontsize=10,
                     color=_INK, pad=6)
        if note:
            ax.text(1.0, 1.01, f'clipped · max {_fmt_ms(clip.data_max)} ms',
                    transform=ax.transAxes, ha='right', va='bottom',
                    fontsize=8, color=_AXIS)

        ax.grid(axis='y', which='major')
        ax.grid(axis='x', visible=False)
        ax.yaxis.set_major_locator(ticker.MaxNLocator(3))
        ax.yaxis.set_minor_locator(ticker.NullLocator())
        ax.xaxis.set_major_formatter(ticker.StrMethodFormatter('{x:,.0f}'))
        for side in ('left', 'bottom'):
            ax.spines[side].set_color(_BASELINE)
        if i % ncols == 0:
            ax.set_ylabel('ms', fontsize=9)
        # sharex hides tick labels on any panel that has a grid cell below it, even
        # when that cell is empty — so re-enable them wherever no panel follows.
        if i + ncols >= len(panels):
            ax.set_xlabel('Frame', fontsize=9)
            ax.tick_params(labelbottom=True)

    fig.suptitle(f'{_label(path)} — Sub-process Shape', x=0.012, y=1.0 - 0.30 / fig_height,
                 ha='left', fontsize=13, color=_INK)
    subtitle = (_subtitle(smooth, show_raw, n_frames, frame_range)
                + '  ·  each panel has its own y-scale')
    fig.text(0.012, 1.0 - 0.62 / fig_height, subtitle, ha='left', va='top',
             fontsize=9, color=_AXIS)

    bottom = (legend_in + 0.42) / fig_height
    fig.subplots_adjust(left=0.075, right=0.99, top=1.0 - header_in / fig_height,
                        bottom=bottom, hspace=0.62, wspace=0.20)
    if handles and legend_labels:
        _legend_below(fig, handles, legend_labels)

    _save(fig, output_dir, filename)


# --- stacked charts ---------------------------------------------------------

def _stack_matrix(plotted: List[PlotSeries]) -> Tuple[np.ndarray, np.ndarray]:
    return _align([s.nums for s in plotted], [s.values for s in plotted])


def plot_subprocess_stacked(path: Path, children: List[Path],
                            series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                            output_dir: str, filename: str, clip_pct: float,
                            frame_range: Optional[Tuple[int, int]],
                            n_frames: int) -> None:
    """Children stacked in absolute milliseconds — total height is the parent's cost."""
    plotted = fold_series(children, series)
    if not plotted:
        return

    frame_arr, matrix = _stack_matrix(plotted)
    if frame_arr.size == 0:
        return

    fig, ax = _make_figure(plot_height=5.0, n_labels=len(plotted))
    ax.stackplot(frame_arr, matrix, labels=[s.label for s in plotted],
                 colors=[s.color for s in plotted], alpha=0.9)

    clip = compute_y_clip([matrix.sum(axis=0)], clip_pct)
    _set_x_extent(ax, [frame_arr])
    note = _apply_y_clip(ax, clip)
    _decorate_axes(ax, f'{_label(path)} — Stacked Sub-process Budget',
                   subtitle=_subtitle(1, False, n_frames, frame_range) + '  ·  absolute time',
                   note=note)
    handles, labels = ax.get_legend_handles_labels()
    _legend_below(fig, handles, labels)
    _save(fig, output_dir, filename)


def plot_subprocess_proportions(path: Path, children: List[Path],
                                series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                                output_dir: str, filename: str,
                                frame_range: Optional[Tuple[int, int]],
                                n_frames: int) -> None:
    """
    The same stack normalised to % of the parent, so composition is readable
    regardless of magnitude and no outlier can inflate the axis — the limit is
    0-100 by construction.

    The 'unaccounted' band is the parent's own self-time, which the absolute
    chart drops silently.
    """
    plotted = fold_series(children, series)
    if not plotted:
        return

    parent_nums, parent_values = series[path]
    frame_arr, matrix = _align(
        [s.nums for s in plotted] + [parent_nums],
        [s.values for s in plotted] + [parent_values],
    )
    if frame_arr.size == 0:
        return

    child_matrix, parent_row = matrix[:-1], matrix[-1]
    child_sum = child_matrix.sum(axis=0)
    # max() guards the case where repeated child entries exceed the parent total.
    denominator = np.maximum(parent_row, child_sum)
    unaccounted = np.maximum(denominator - child_sum, 0.0)

    stack = np.vstack([child_matrix, unaccounted])
    with np.errstate(invalid='ignore', divide='ignore'):
        percent = np.where(denominator > 0.0, stack / denominator * 100.0, 0.0)

    labels = [s.label for s in plotted] + ['unaccounted (self time)']
    colors = [s.color for s in plotted] + [_GRID]

    fig, ax = _make_figure(plot_height=5.0, n_labels=len(labels))
    ax.stackplot(frame_arr, percent, labels=labels, colors=colors, alpha=0.9)

    _set_x_extent(ax, [frame_arr])
    ax.set_ylim(0.0, 100.0)
    _decorate_axes(ax, f'{_label(path)} — Sub-process Share of Parent',
                   subtitle=_subtitle(1, False, n_frames, frame_range) + '  ·  % of parent',
                   ylabel='% of parent')
    ax.yaxis.set_major_formatter(ticker.PercentFormatter())
    handles, legend_labels = ax.get_legend_handles_labels()
    _legend_below(fig, handles, legend_labels)
    _save(fig, output_dir, filename)


# ---------------------------------------------------------------------------
# Text summary
# ---------------------------------------------------------------------------

def build_summary(frames: List[Frame], order: List[Path],
                  series: Dict[Path, Tuple[np.ndarray, np.ndarray]],
                  frame_range: Optional[Tuple[int, int]] = None) -> str:
    """
    Render the full text of summary.txt. Returns '' when there is nothing to
    report, so that the caller writes no file at all.

    When frame_range is set, every statistic here — percentiles, spike detection,
    the whole tree — is over that window only, and a line saying so is added.
    """
    if not frames:
        return ''

    total_ms   = np.array([f.total_ms for f in frames], dtype=float)
    frame_mean = float(np.mean(total_ms))
    p99_total  = float(np.percentile(total_ms, 99))

    def _stats(ms: np.ndarray) -> dict:
        return {
            'mean': float(np.mean(ms)),
            'p50':  float(np.percentile(ms, 50)),
            'p95':  float(np.percentile(ms, 95)),
            'p99':  float(np.percentile(ms, 99)),
        }

    path_stats = {path: _stats(series[path][1]) for path in order}

    # Column width — widest name at any depth, including its indentation.
    name_col = max((len(p[-1]) + 2 * (len(p) - 1) for p in order), default=10) + 2

    def _stat_line(s: dict, indent: str) -> str:
        avg_pct = (s['mean'] / frame_mean * 100) if frame_mean > 0 else 0.0
        return (
            f'{indent}'
            f'mean {s["mean"]:7.2f} ms  '
            f'p50 {s["p50"]:7.2f} ms  '
            f'p95 {s["p95"]:7.2f} ms  '
            f'p99 {s["p99"]:7.2f} ms  '
            f'avg% {avg_pct:5.1f}%'
        )

    lines: List[str] = []

    # ---- Section 1: Session Overview ----
    lines.append('=== Session Overview ===')
    lines.append(f'  Frames analyzed : {len(frames):,}')
    lines.append(f'  Total session   : {len(frames) / 60.0:.1f} s  (estimated at 60 fps)')
    if frame_range is not None:
        lines.append(f'  Frame range     : {frame_range[0]:,}–{frame_range[1]:,}'
                     f'  (restricted by --frames)')
    lines.append('')

    lines.append('=== Total Frame Time ===')
    lines.append(f'  min    : {float(np.min(total_ms)):8.2f} ms')
    lines.append(f'  mean   : {frame_mean:8.2f} ms')
    lines.append(f'  p50    : {float(np.percentile(total_ms, 50)):8.2f} ms')
    lines.append(f'  p95    : {float(np.percentile(total_ms, 95)):8.2f} ms')
    lines.append(f'  p99    : {p99_total:8.2f} ms')
    lines.append(f'  max    : {float(np.max(total_ms)):8.2f} ms')
    lines.append('')

    lines.append('=== Process Summary ===')

    def _emit(path: Path) -> None:
        label = ('  ' * (len(path) - 1)) + path[-1]
        lines.append(_stat_line(path_stats[path], '  ' + label.ljust(name_col)))
        children = sorted(_children_of(path, order),
                          key=lambda p: path_stats[p]['mean'], reverse=True)
        for child in children:
            _emit(child)

    roots = [p for p in order if len(p) == 1]
    for root in sorted(roots, key=lambda p: path_stats[p]['mean'], reverse=True):
        _emit(root)
    lines.append('')

    # ---- Section 2: Spike Frames ----
    spike_frames = sorted(
        [f for f in frames if f.total_ms > p99_total],
        key=lambda f: f.total_ms, reverse=True,
    )
    pct_session = len(spike_frames) / len(frames) * 100
    lines.append(f'=== Spike Frames (total > p99 = {p99_total:.2f} ms) ===')
    lines.append(f'  {len(spike_frames)} spike frames  ({pct_session:.1f}% of session)')
    lines.append('')

    if spike_frames:
        lines.append(f'  {"Frame":>6}  {"Total":>9}  Top Contributor')
        lines.append(f'  {"------":>6}  {"---------":>9}  ' + '-' * 42)
        for frame in spike_frames[:20]:
            if frame.roots:
                top = max(frame.roots, key=lambda e: e.duration_ms)
                pct = top.duration_ms / frame.total_ms * 100 if frame.total_ms > 0 else 0.0
                contributor = f'{top.base_name} {top.duration_ms:.2f} ms ({pct:.1f}%)'
            else:
                contributor = '(no root data)'
            lines.append(f'  {frame.number:>6}  {frame.total_ms:>8.2f} ms  {contributor}')
        if len(spike_frames) > 20:
            lines.append(f'  (showing top 20 of {len(spike_frames)})')

    return '\n'.join(lines) + '\n'


def generate_summary(frames: List[Frame], order: List[Path],
                     series: Dict[Path, Tuple[np.ndarray, np.ndarray]], output_dir: str,
                     frame_range: Optional[Tuple[int, int]] = None) -> None:
    text = build_summary(frames, order, series, frame_range)
    if not text:
        return

    path = os.path.join(output_dir, 'summary.txt')
    with open(path, 'w') as fh:
        fh.write(text)
    print(f'  Saved: {path}')


# ---------------------------------------------------------------------------
# Utilities
# ---------------------------------------------------------------------------

def _save(fig: plt.Figure, output_dir: str, filename: str) -> None:
    path = os.path.join(output_dir, filename)
    # No bbox_inches='tight' — it would undo the deliberate legend spacing.
    fig.savefig(path, dpi=_DPI, facecolor=fig.get_facecolor())
    plt.close(fig)
    print(f'  Saved: {path}')


def find_latest_diagnostics(assets_dir: str) -> Optional[str]:
    """Return the path of the newest *-diagnostics.txt in assets_dir, or None."""
    matches = glob.glob(os.path.join(assets_dir, '*-diagnostics.txt'))
    return max(matches) if matches else None  # epoch prefix → lexicographic max = newest


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    repo_root    = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', '..'))
    default_assets = os.path.join(repo_root, 'assets')

    parser = argparse.ArgumentParser(
        description='Analyze a Megaman Maverick runtime diagnostics file and produce timing charts.'
    )
    parser.add_argument(
        'file', nargs='?', default=None,
        help='Path to the diagnostics file. '
             'Defaults to the newest *-diagnostics.txt in assets/.',
    )
    parser.add_argument(
        '--output-dir', default=None,
        help='Directory for output PNGs. Defaults to diagnostics_output/ '
             'next to the input file.',
    )
    parser.add_argument(
        '--smooth', type=int, default=30, metavar='N',
        help='Rolling-average window in frames (default: 30). Set to 1 to disable.',
    )
    parser.add_argument(
        '--frames', default=None, metavar='START:END',
        help='Restrict to an inclusive frame-number range: 400:900, 400: or :900. '
             'Applies to the charts AND summary.txt, so every statistic describes '
             'that window only.',
    )
    parser.add_argument(
        '--clip-percentile', type=float, default=_CLIP_PERCENTILE, metavar='P',
        help='Clip the y-axis at this percentile of the plotted values so a single '
             'outlier cannot flatten the chart (default: 99.5). Clipped points are '
             'drawn as carets at the top edge and counted in the chart note.',
    )
    parser.add_argument(
        '--no-clip', action='store_true',
        help='Disable percentile y-clipping; let outliers set the axis.',
    )
    args = parser.parse_args()

    # ---- resolve input file ----
    diag_file = args.file
    if diag_file is None:
        diag_file = find_latest_diagnostics(default_assets)
        if diag_file is None:
            print(f'error: no *-diagnostics.txt files found in {default_assets}',
                  file=sys.stderr)
            sys.exit(1)
        print(f'Using: {diag_file}')

    if not os.path.isfile(diag_file):
        print(f'error: file not found: {diag_file}', file=sys.stderr)
        sys.exit(1)

    # ---- resolve output directory ----
    stem = os.path.splitext(os.path.basename(diag_file))[0]
    output_dir = args.output_dir or os.path.join(
        os.path.dirname(os.path.abspath(diag_file)), stem + '_output'
    )
    os.makedirs(output_dir, exist_ok=True)

    # ---- parse ----
    print(f'Parsing {diag_file} …')
    frames = parse_file(diag_file)
    if not frames:
        print('error: no frame data found in file.', file=sys.stderr)
        sys.exit(1)

    # ---- restrict the frame window, before anything is derived from it ----
    frame_range: Optional[Tuple[int, int]] = None
    if args.frames is not None:
        try:
            start, end = parse_frame_range(args.frames)
        except ValueError as exc:
            print(f'error: {exc}', file=sys.stderr)
            sys.exit(1)

        covered = (frames[0].number, frames[-1].number)
        frames = [f for f in frames
                  if (start is None or f.number >= start)
                  and (end is None or f.number <= end)]
        if not frames:
            print(f'error: no frames in range {args.frames} '
                  f'(capture covers {covered[0]:,}–{covered[1]:,})', file=sys.stderr)
            sys.exit(1)
        frame_range = (frames[0].number, frames[-1].number)

    clip_pct = 0.0 if args.no_clip else args.clip_percentile

    banner = f'  {len(frames):,} frames  |  smooth window = {args.smooth}'
    if frame_range is not None:
        banner += f'  |  frames {frame_range[0]:,}–{frame_range[1]:,}'
    banner += '  |  y-clip ' + ('off' if clip_pct <= 0 else f'p{clip_pct:g}')
    print(banner)

    # ---- plot ----
    _apply_style()
    print('Generating charts …')

    order, series = _timings_by_path(frames)
    n_frames = len(frames)

    for filename, show_raw in ((CHART_OVERVIEW, True), (CHART_OVERVIEW_SMOOTHED, False)):
        plot_frame_overview(frames, output_dir, filename, args.smooth, clip_pct,
                            frame_range, show_raw)

    for filename, show_raw in ((CHART_ROOTS, True), (CHART_ROOTS_SMOOTHED, False)):
        plot_root_processes(order, series, output_dir, filename, args.smooth,
                            clip_pct, frame_range, show_raw, n_frames)

    # One block per entry that has children, at any depth — the game nests systems
    # inside screen.render, and the world cycles inside those again.
    for parent in chart_plan(order):
        path, children = parent.path, parent.children

        for filename, show_raw in ((parent.overlay, True), (parent.overlay_smoothed, False)):
            plot_subprocess_breakdown(path, children, series, output_dir, filename,
                                      args.smooth, clip_pct, frame_range, show_raw, n_frames)

        for filename, show_raw in ((parent.facets, True), (parent.facets_smoothed, False)):
            plot_subprocess_facets(path, children, series, output_dir, filename,
                                   args.smooth, clip_pct, frame_range, show_raw, n_frames)

        plot_subprocess_stacked(path, children, series, output_dir, parent.stacked_ms,
                                clip_pct, frame_range, n_frames)
        plot_subprocess_proportions(path, children, series, output_dir, parent.stacked_pct,
                                    frame_range, n_frames)

    generate_summary(frames, order, series, output_dir, frame_range)

    print(f'\nDone.  Charts written to: {output_dir}')


if __name__ == '__main__':
    main()
