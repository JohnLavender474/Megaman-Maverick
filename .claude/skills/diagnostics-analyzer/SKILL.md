---
name: diagnostics-analyzer
description: Use when you need to profile the game's runtime performance — investigating FPS drops, frame-time spikes, slowdowns in a specific room or fight, or when asked to chart/analyze a *-diagnostics.txt file.
---

# Diagnostics Analyzer

Turns a runtime diagnostics capture into per-frame timing charts and a text
summary, so a bogdown can be traced from "the game stutters in Wily 3 room 5" to
"WorldSystem's collision cycles are eating 20 ms on those frames".

## How diagnostics get produced

`RuntimeDiagnostics` (`engine/.../diagnostics/RuntimeDiagnostics.kt`) records a
tree of named timings per frame and streams them to a file on a background
thread. Instrumentation is opt-in and null-safe: every call site is
`diagnostics?.beginEntry(...)` / `diagnostics?.endEntry()`, so with diagnostics
off there is no overhead beyond a null check.

- `MegamanMaverickGame.render()` brackets the *entire* frame with `beginFrame()` /
  `endFrame()`, so the frame total is the real cost of the frame and the root
  entries account for essentially all of it. Roots are `clearScreen`, `runQueue`,
  `autoPerf`, `controllerPoller`, `screenController`, `screen.render`,
  `screen.draw`, `screen.drawShapes`, `screenshot`, `eventsMan`, `audioMan`,
  `updatables`, `debugText`, `notification`, `reclaimPools`. During asset loading
  the roots are `assMan.update`, `postCreate`, and `loadingText` instead.
- The game engine runs inside `screen.render()`, so the systems are one level
  below it, not roots. The instance is passed into most systems in
  `createGameEngine()` — `WorldSystem`, `BehaviorsSystem`, `SpritesSystem`,
  `AnimationsSystem`, `UpdatablesSystem`, `CullablesSystem`, `MotionSystem`,
  `PointsSystem`, the pathfinding system, `DrawableShapesSystem`, `AudioSystem`.
  `ControllerSystem` and `FontsSystem` are *not* given a diagnostics instance, so
  they never appear and fall into `screen.render`'s unattributed remainder.
- Nesting is unlimited. `WorldSystem` is the deepest: it opens
  `buildBodyArray`, `updateWorldContainer`, and one `cycle[N]` per fixed-step
  iteration, each of which contains `preProcess`, `bodyProcess`,
  `collectContacts`, `processContacts` (itself holding three more), and
  `resolveCollisions`, `postProcess` — five levels below the frame.

To add coverage to something not yet instrumented, thread the game's
`diagnostics` field to the call site and wrap it in `beginEntry`/`endEntry` —
pairs must match, and the class is **not** thread safe (render thread only).

## Capturing a run

Launch with `--diagnostics`. The file is written to the process working
directory, which for `./gradlew lwjgl3:run` is `assets/`, named
`<epoch-millis>-diagnostics.txt`.

Keep captures short and targeted — every frame appends several lines, so a few
minutes in the problem room beats a full playthrough. Play the segment that
stutters, then quit cleanly so `dispose()` flushes the writer.

## Always start with `--help`

The tool's own docs are the source of truth for arguments and outputs:

```bash
utils/diagnostics/run.sh --help
```

Then run it:

```bash
utils/diagnostics/run.sh                        # newest *-diagnostics.txt in assets/
utils/diagnostics/run.sh path/to/file.txt       # explicit capture
utils/diagnostics/run.sh --smooth 1             # raw, unsmoothed lines
utils/diagnostics/run.sh --frames 1150:1350     # zoom into a frame window
utils/diagnostics/run.sh --no-clip              # let outliers set the y-axis
utils/diagnostics/run.sh --output-dir /tmp/out  # custom output dir
```

`--frames` takes inclusive frame *numbers* (captures have gaps) and restricts the
summary as well as the charts, so every statistic describes that window. At
~1.6 px per frame on a 2000-frame capture it is the most effective way to see
per-frame behaviour.

`run.sh` creates/reuses a venv in `utils/diagnostics/.venv`. Output defaults to
`<capture-name>_output/` beside the input file.

## Reading the output

Start with `summary.txt` — it is plain text, so read it directly rather than
guessing from the PNGs:

- **Total Frame Time** — min/mean/p50/p95/p99/max. Compare against the budget for
  the active `Performance` mode (60 FPS → 16.7 ms, 30 FPS → 33.3 ms). A healthy
  mean with a bad p99 means spikes, not sustained load.
- **Process Summary** — the full entry tree at every depth, indented, sorted by
  mean within each level, with mean/p50/p95/p99 and `avg%` of the mean frame.
  This is the "where does the time go" table.
- **Spike Frames** — frames above the p99 total, with the top contributing root
  and its frame number. Use those frame numbers to jump back into the raw
  capture.

Then the charts:

- `01_frame_overview.png` — total frame time. FPS-budget lines are drawn only when
  they already fall inside the data range, so they can never inflate the axis.
  Sustained plateau above the budget = a heavy room; isolated needles = spawn/load
  hitches or GC.
- `03_root_processes.png` — one line per root; shows which phase of the frame owns
  a plateau. Capped at 7 series plus a neutral `Other (N series)` — `summary.txt`
  lists what went into `Other`.
- Then six charts per entry that has children, at *any* depth, named by path
  (e.g. `13_screen_render__WorldSystem_facets.png`):
  `_subprocesses` (children overlaid, shared y-axis — good for comparing siblings),
  `_facets` (**one panel per child, each with its own y-scale** — the only view
  where a 0.001 ms child's shape is as legible as a 4 ms sibling's),
  `_stacked` (absolute ms), and `_stacked_pct` (% of parent, with an
  `unaccounted (self time)` band showing the parent's own cost).
- Every line chart has a `_smoothed` twin containing only the rolling average, with
  no raw ghost line — use it for trends, the other for spikes.

The y-axis is clipped at p99.5 by default so one outlier can't flatten everything
else. Clipped points are drawn as carets along the top edge and counted in a note
at the top right (`3 of 1,500 points above, max 4.05 ms`) — nothing is hidden
silently. Pass `--no-clip` to see the true extent.

Check the raw capture for a trailing `=== Dropped N frames ===` line. The writer
uses a bounded queue and drops frames rather than stalling the render thread or
growing without limit, so a non-zero count means the capture has gaps (visible as
jumps in frame numbering) — the timings that *were* recorded are still accurate.

One parsing detail matters when interpreting: iteration suffixes are collapsed, so
`cycle[1]`, `cycle[2]`, … sum into a single `cycle` series and a rising `cycle`
line can mean *more* fixed-step iterations rather than slower ones. At 60 FPS with
a 1/150 s fixed step the accumulator wants 2.5 cycles per frame, so it alternates
between 2 and 3 — a capture looks bimodal, with p95 well above p50, for entirely
structural reasons. Chase the frames running 4+ cycles, not that oscillation.

To read an individual spike frame in full, grep the raw capture:

```bash
grep -A 40 '=== Frame #12345 ' assets/<capture>-diagnostics.txt
```

## Reporting back

Lead with the frame-time distribution versus the budget, name the entry that owns
the regression with its numbers and its path, and say whether it is sustained load
or spikes. Sanity-check the capture first: each parent should slightly exceed the
sum of its children, and the roots should sum to nearly the frame total. A large
remainder at any level is uninstrumented work — inside `screen.render` that means
the level screen's own logic, `ControllerSystem`, and `FontsSystem`; at frame level
it means GC or driver time.

Async work is invisible here: `AsyncPathfindingSystem` runs on a thread pool, so a
near-zero timing means only that the handoff was cheap, not that pathfinding was.
Reference the specific PNGs by filename so the user can open them.
