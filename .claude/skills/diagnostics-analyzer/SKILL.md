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

- `MegamanMaverickGame.render()` brackets the frame with `beginFrame()` /
  `endFrame()` and times `controllerPoller`, `eventsMan`, and `audioMan`
  directly.
- The instance is passed into most systems in `createGameEngine()`
  (`MegamanMaverickGame.kt`), so each system times itself as a root entry —
  `WorldSystem`, `BehaviorsSystem`, `SpritesSystem`, `AnimationsSystem`,
  `UpdatablesSystem`, `CullablesSystem`, `MotionSystem`, `PointsSystem`,
  the pathfinding system, `DrawableShapesSystem`, `AudioSystem`.
- Nesting is unlimited. `WorldSystem` is the deepest: it opens
  `buildBodyArray`, `updateWorldContainer`, and one `cycle[N]` per fixed-step
  iteration, each of which contains `preProcess`, `bodyProcess`,
  `collectContacts`, `processContacts`, `resolveCollisions`, `postProcess`.

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
utils/diagnostics/run.sh --output-dir /tmp/out  # custom output dir
```

`run.sh` creates/reuses a venv in `utils/diagnostics/.venv`. Output defaults to
`<capture-name>_output/` beside the input file.

## Reading the output

Start with `summary.txt` — it is plain text, so read it directly rather than
guessing from the PNGs:

- **Total Frame Time** — min/mean/p50/p95/p99/max. Compare against the budget for
  the active `Performance` mode (60 FPS → 16.7 ms, 30 FPS → 33.3 ms). A healthy
  mean with a bad p99 means spikes, not sustained load.
- **Root Process Summary** — each root and its immediate children with
  mean/p50/p95/p99 and `avg%` of the mean frame. This is the "where does the time
  go" table.
- **Spike Frames** — frames above the p99 total, with the top contributing root
  and its frame number. Use those frame numbers to jump back into the raw
  capture.

Then the charts:

- `01_frame_overview.png` — total frame time with FPS-budget and percentile
  reference lines. Sustained plateau above the budget = a heavy room; isolated
  needles = spawn/load hitches or GC.
- `02_root_processes.png` — one line per root; shows which system owns a plateau.
- `NN_<root>_subprocesses.png` — a root's immediate children over time.
- `NN_<root>_stacked.png` — the same children stacked, for proportions at a
  glance.

Two parsing details matter when interpreting: iteration suffixes are collapsed
(`cycle[1]`, `cycle[2]`, … sum into one `cycle` series, so a rising `cycle` line
can mean *more* fixed-step iterations rather than slower ones), and the charts
only go one level deep per root. To see inside `cycle`, grep the raw capture for
a spike frame:

```bash
grep -A 40 '=== Frame #12345 ' assets/<capture>-diagnostics.txt
```

## Reporting back

Lead with the frame-time distribution versus the budget, name the root (and
child) that owns the regression with its numbers, and say whether it is
sustained load or spikes. Point out anything the capture *doesn't* cover —
uninstrumented work shows up only as the gap between the frame total and the sum
of the roots (rendering/`screen.render`, GC, and driver time all live there).
Reference the specific PNGs by filename so the user can open them.
