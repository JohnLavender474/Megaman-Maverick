#!/usr/bin/env python3
"""
Megaman Maverick Runtime Diagnostics Analyzer

Parses a *-diagnostics.txt file produced by RuntimeDiagnostics and generates
per-frame timing charts.

Output charts:
  01_frame_overview.png         — total frame time across all frames
  02_root_processes.png         — one line per root-level timed entry
  03_<name>_subprocesses.png    — one chart per root with sub-process breakdown
  ...

Usage:
  python level_analyzer.py [FILE] [--output-dir DIR] [--smooth N]

  FILE       Path to a diagnostics file.  Defaults to the newest
             *-diagnostics.txt found in assets/ relative to the repo root.
  --smooth   Rolling-average window in frames (default: 30, set to 1 to
             disable smoothing).
"""

import argparse
import glob
import os
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

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


# ---------------------------------------------------------------------------
# Data extraction helpers
# ---------------------------------------------------------------------------

def _root_base_names(frames: List[Frame]) -> List[str]:
    """Distinct root base-names in first-seen order."""
    seen: List[str] = []
    for frame in frames:
        for root in frame.roots:
            if root.base_name not in seen:
                seen.append(root.base_name)
    return seen


def _root_timings(frames: List[Frame]) -> Dict[str, Tuple[np.ndarray, np.ndarray]]:
    """
    {base_name: (frame_numbers, durations_ms)}

    Multiple root entries sharing the same base name in a frame (e.g. two
    'WorldSystem' entries) are summed together.
    """
    data: Dict[str, Dict[int, float]] = defaultdict(lambda: defaultdict(float))
    for frame in frames:
        for root in frame.roots:
            data[root.base_name][frame.number] += root.duration_ms

    return {
        name: (
            np.array(sorted(fm.keys()), dtype=int),
            np.array([fm[n] for n in sorted(fm.keys())], dtype=float),
        )
        for name, fm in data.items()
    }


def _child_timings(frames: List[Frame], root_base: str) -> Dict[str, Tuple[np.ndarray, np.ndarray]]:
    """
    For a given root base-name, build {child_base_name: (frame_numbers, durations_ms)}.

    Children sharing the same base name within a frame are summed (this
    collapses all cycle[1], cycle[2], … into a single 'cycle' series whose
    value is the total time spent in cycles for that frame).
    """
    data: Dict[str, Dict[int, float]] = defaultdict(lambda: defaultdict(float))
    for frame in frames:
        for root in frame.roots:
            if root.base_name != root_base:
                continue
            for child in root.children:
                data[child.base_name][frame.number] += child.duration_ms

    return {
        name: (
            np.array(sorted(fm.keys()), dtype=int),
            np.array([fm[n] for n in sorted(fm.keys())], dtype=float),
        )
        for name, fm in data.items()
    }


# ---------------------------------------------------------------------------
# Smoothing
# ---------------------------------------------------------------------------

def _rolling_avg(data: np.ndarray, window: int) -> np.ndarray:
    if window <= 1 or len(data) == 0:
        return data
    kernel = np.ones(window) / window
    # 'same' mode keeps the array length; edges are naturally lighter-weighted.
    return np.convolve(data, kernel, mode='same')


# ---------------------------------------------------------------------------
# Chart helpers
# ---------------------------------------------------------------------------

def _apply_style():
    for candidate in ('seaborn-v0_8-whitegrid', 'seaborn-whitegrid'):
        try:
            plt.style.use(candidate)
            return
        except OSError:
            pass


def _decorate_axes(ax: plt.Axes, title: str, ylabel: str = 'Time (ms)'):
    ax.set_title(title, fontsize=13, pad=10)
    ax.set_xlabel('Frame', fontsize=11)
    ax.set_ylabel(ylabel, fontsize=11)
    ax.yaxis.set_minor_locator(ticker.AutoMinorLocator())
    ax.grid(which='major', linestyle='--', linewidth=0.5, alpha=0.7)
    ax.grid(which='minor', linestyle=':', linewidth=0.3, alpha=0.4)
    ax.set_ylim(bottom=0)
    ax.legend(loc='upper right', fontsize=9, framealpha=0.8)


_TARGET_60FPS_MS = 1000.0 / 60.0
_TARGET_30FPS_MS = 1000.0 / 30.0


# ---------------------------------------------------------------------------
# Individual chart functions
# ---------------------------------------------------------------------------

def plot_frame_overview(frames: List[Frame], output_dir: str, smooth: int) -> None:
    nums = np.array([f.number for f in frames], dtype=int)
    ms   = np.array([f.total_ms for f in frames], dtype=float)

    fig, ax = plt.subplots(figsize=(16, 4))

    ax.plot(nums, ms, color='#aaaaaa', linewidth=0.6, label='Raw')
    if smooth > 1:
        ax.plot(nums, _rolling_avg(ms, smooth), color='#d62728',
                linewidth=1.8, label=f'{smooth}-frame rolling avg')

    ax.axhline(_TARGET_60FPS_MS, color='#2ca02c', linewidth=1.0,
               linestyle='--', label='60 FPS budget (16.7 ms)')
    ax.axhline(_TARGET_30FPS_MS, color='#ff7f0e', linewidth=1.0,
               linestyle='--', label='30 FPS budget (33.3 ms)')

    p50, p95, p99 = np.percentile(ms, [50, 95, 99])
    ax.axhline(p50, color='#1f77b4', linewidth=0.8, linestyle=':',
               label=f'p50 = {p50:.2f} ms')
    ax.axhline(p95, color='#9467bd', linewidth=0.8, linestyle=':',
               label=f'p95 = {p95:.2f} ms')
    ax.axhline(p99, color='#8c564b', linewidth=0.8, linestyle=':',
               label=f'p99 = {p99:.2f} ms')

    _decorate_axes(ax, f'Total Frame Time  ({len(frames):,} frames)')
    fig.tight_layout()
    _save(fig, output_dir, '01_frame_overview.png')


def plot_root_processes(frames: List[Frame], output_dir: str, smooth: int) -> None:
    timings = _root_timings(frames)
    if not timings:
        return

    palette = plt.rcParams['axes.prop_cycle'].by_key()['color']
    fig, ax = plt.subplots(figsize=(16, 5))

    for i, (name, (nums, ms)) in enumerate(timings.items()):
        color = palette[i % len(palette)]
        ax.plot(nums, ms, color=color, linewidth=0.4, alpha=0.35)
        smoothed = _rolling_avg(ms, smooth)
        label = f'{name}' if smooth <= 1 else f'{name} ({smooth}-frame avg)'
        ax.plot(nums, smoothed, color=color, linewidth=1.8, label=label)

    _decorate_axes(ax, 'Root Process Times per Frame')
    fig.tight_layout()
    _save(fig, output_dir, '02_root_processes.png')


def plot_subprocess_breakdown(frames: List[Frame], root_base: str,
                               chart_index: int, output_dir: str, smooth: int) -> None:
    """One chart per root entry showing each immediate child's time per frame."""
    timings = _child_timings(frames, root_base)
    if not timings:
        return

    # Also pull the root's own total so we can show it as a reference line.
    root_totals = _root_timings(frames).get(root_base)

    palette = plt.rcParams['axes.prop_cycle'].by_key()['color']
    fig, ax = plt.subplots(figsize=(16, 5))

    if root_totals is not None:
        r_nums, r_ms = root_totals
        ax.plot(r_nums, _rolling_avg(r_ms, smooth), color='#000000',
                linewidth=1.0, linestyle='--', alpha=0.4, label=f'{root_base} total')

    for i, (name, (nums, ms)) in enumerate(timings.items()):
        color = palette[i % len(palette)]
        ax.plot(nums, ms, color=color, linewidth=0.4, alpha=0.3)
        smoothed = _rolling_avg(ms, smooth)
        label = f'{name}' if smooth <= 1 else f'{name} ({smooth}-frame avg)'
        ax.plot(nums, smoothed, color=color, linewidth=1.8, label=label)

    _decorate_axes(ax, f'{root_base} — Sub-process Breakdown')
    fig.tight_layout()

    safe = re.sub(r'[^A-Za-z0-9_]', '_', root_base)
    _save(fig, output_dir, f'{chart_index:02d}_{safe}_subprocesses.png')


def plot_subprocess_proportions(frames: List[Frame], root_base: str,
                                 chart_index: int, output_dir: str) -> None:
    """
    Stacked-area chart showing what fraction of a root's budget each child
    consumes on average — useful for a single-glance 'where does the time go'
    view.
    """
    timings = _child_timings(frames, root_base)
    if not timings:
        return

    # Align all series to a common frame axis.
    all_frames = sorted({n for nums, _ in timings.values() for n in nums.tolist()})
    if not all_frames:
        return

    frame_arr = np.array(all_frames, dtype=int)
    names = list(timings.keys())
    matrix = np.zeros((len(names), len(all_frames)), dtype=float)

    frame_idx = {fn: i for i, fn in enumerate(all_frames)}
    for row, name in enumerate(names):
        nums, ms = timings[name]
        for fn, val in zip(nums.tolist(), ms.tolist()):
            matrix[row, frame_idx[fn]] = val

    palette = plt.rcParams['axes.prop_cycle'].by_key()['color']
    fig, ax = plt.subplots(figsize=(16, 5))

    ax.stackplot(frame_arr, matrix,
                 labels=names,
                 colors=[palette[i % len(palette)] for i in range(len(names))],
                 alpha=0.75)

    _decorate_axes(ax, f'{root_base} — Stacked Sub-process Budget')
    ax.legend(loc='upper right', fontsize=9, framealpha=0.8)
    fig.tight_layout()

    safe = re.sub(r'[^A-Za-z0-9_]', '_', root_base)
    _save(fig, output_dir, f'{chart_index:02d}_{safe}_stacked.png')


# ---------------------------------------------------------------------------
# Text summary
# ---------------------------------------------------------------------------

def generate_summary(frames: List[Frame], output_dir: str) -> None:
    if not frames:
        return

    total_ms   = np.array([f.total_ms for f in frames], dtype=float)
    frame_mean = float(np.mean(total_ms))
    p99_total  = float(np.percentile(total_ms, 99))

    root_names   = _root_base_names(frames)
    root_timings = _root_timings(frames)

    def _stats(ms: np.ndarray) -> dict:
        return {
            'mean': float(np.mean(ms)),
            'p50':  float(np.percentile(ms, 50)),
            'p95':  float(np.percentile(ms, 95)),
            'p99':  float(np.percentile(ms, 99)),
        }

    root_stats  = {n: _stats(root_timings[n][1]) for n in root_names}
    child_stats = {
        n: {cn: _stats(cms) for cn, (_, cms) in _child_timings(frames, n).items()}
        for n in root_names
    }

    # Column width — widest name across roots and all children.
    all_names = list(root_names) + [
        cn for children in child_stats.values() for cn in children
    ]
    name_col = max((len(n) for n in all_names), default=10) + 2

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
    lines.append('')

    lines.append('=== Total Frame Time ===')
    lines.append(f'  min    : {float(np.min(total_ms)):8.2f} ms')
    lines.append(f'  mean   : {frame_mean:8.2f} ms')
    lines.append(f'  p50    : {float(np.percentile(total_ms, 50)):8.2f} ms')
    lines.append(f'  p95    : {float(np.percentile(total_ms, 95)):8.2f} ms')
    lines.append(f'  p99    : {p99_total:8.2f} ms')
    lines.append(f'  max    : {float(np.max(total_ms)):8.2f} ms')
    lines.append('')

    lines.append('=== Root Process Summary ===')
    for root_name in sorted(root_names, key=lambda n: root_stats[n]['mean'], reverse=True):
        label = root_name.ljust(name_col)
        lines.append(_stat_line(root_stats[root_name], f'  {label}'))
        children = child_stats[root_name]
        for child_name in sorted(children, key=lambda n: children[n]['mean'], reverse=True):
            child_label = child_name.ljust(name_col)
            lines.append(_stat_line(children[child_name], f'    {child_label}'))
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

    path = os.path.join(output_dir, 'summary.txt')
    with open(path, 'w') as fh:
        fh.write('\n'.join(lines) + '\n')
    print(f'  Saved: {path}')


# ---------------------------------------------------------------------------
# Utilities
# ---------------------------------------------------------------------------

def _save(fig: plt.Figure, output_dir: str, filename: str) -> None:
    path = os.path.join(output_dir, filename)
    fig.savefig(path, dpi=150)
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
    print(f'  {len(frames):,} frames  |  smooth window = {args.smooth}')

    # ---- plot ----
    _apply_style()
    print('Generating charts …')

    plot_frame_overview(frames, output_dir, args.smooth)
    plot_root_processes(frames, output_dir, args.smooth)

    chart_idx = 3
    for root_name in _root_base_names(frames):
        plot_subprocess_breakdown(frames, root_name, chart_idx, output_dir, args.smooth)
        chart_idx += 1
        plot_subprocess_proportions(frames, root_name, chart_idx, output_dir)
        chart_idx += 1

    generate_summary(frames, output_dir)

    print(f'\nDone.  Charts written to: {output_dir}')


if __name__ == '__main__':
    main()
