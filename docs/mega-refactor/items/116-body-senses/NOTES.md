# Notes: 116 Body senses as typed state, then a sense history

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Scan, 2026-11-02, branch `fix/quicksand-two-players` at `8c648138c`

From a code read. No build, run or profile. Costs are from reading and calculation.

### Sites that track a prior sense by hand

| Site | Clock | Lines removed | Fit |
|---|---|---|---|
| `BabySpider.kt:64-67,91-94,200-203` | Cycle | 12 | Exact with a prior step |
| `SuicideBummer.kt:60,77,108` | Frame | 3 | Exact with a frame history |
| `BigJumpingJoe.kt:115,155,187,188` | Frame | 4 | Not exact: the flag starts true, and a freeze holds an edge |
| `Petaspark.kt:80,105,119` | Frame | 3 | No: derived from `facing`, and the hop runs in each cycle of a frame |
| `SuctionRoller.kt:72,94,115` | Frame | 3 | No: same |

Total: 25 lines in 5 files. Exact: 15 lines in 2 files, on 2 different clocks. Coyote time, just-landed and landing dust from a sense change: 0 hits. `Receivers.HIT_BY_BLOCK` (43 lines in 19 files) already gives contact edges.

### Sense API

- `isSensing`: 436 call lines in 117 files. `isSensingAny`: 16. `isSensingAll`: 2. The vararg forms allocate on each call (from the source shape).
- `setBodySense`: 41 writer lines in 13 rule files, and 1 in `BodySense.kt`.
- `resetBodySenses`: 1 caller, `BodyComponentCreator.kt:48`.
- Direct bag access: 3 lines, all in `BodySense.kt`.
- `IBodySenseListener`: 0 production users.
- 15 senses. All tiers give 2.5 cycles per frame, and `maxIterations` is 6 at scalar 1.

### Why a history by resets fails

A reset happens once for each entity life, in `destroyNow`. A second slot holds a previous life of a pooled object, and nothing reads it.

### Gameplay risks

- A cycle history read from per-frame code misses edges: about 60% at 2.5 cycles per frame (calculated).
- A frame with no world pass repeats a naive edge query. `SuicideBummer` would jump each frame during a gate transition.
- `doUpdate` false: a body skips cycles, so its entry stays from before the skip.
- `FeetSandRule.kt:61` clears the sense with no test of other sand contacts, so on 2 adjacent sand fixtures a body can read false for one cycle.
