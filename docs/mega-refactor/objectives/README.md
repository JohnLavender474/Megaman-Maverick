# Objectives

The larger results that the mega refactor items serve. Change this folder only on `master`.

Each objective file gives the problems, the goal, the rules that the code must obey, the current state of each rule, the
redo risk and the linked items.

| ID | Objective | Status | Rules | Holds | Partly | Broken | Not checked |
|----|-----------|--------|-------|-------|--------|--------|-------------|
| `O1` | [Clean, composable, easy to extend](O1-clean-design.md) | active | 5 | 1 | 4 | 0 | 0 |
| `O2` | [Multiplayer](O2-multiplayer.md) | active | 10 | 1 | 5 | 4 | 0 |
| `O3` | [Web play and other platforms](O3-platforms.md) | planned | 5 | 0 | 1 | 3 | 1 |
| `O4` | [Deeper level design, enemies and bosses, more variety](O4-level-and-gameplay-depth.md) | active | 3 | 1 | 1 | 0 | 1 |
| `O5` | [Plays well on weak hardware](O5-performance.md) | active | 2 | 2 | 0 | 0 | 0 |
| `O6` | [Safe to change](O6-safe-to-change.md) | active | 4 | 1 | 2 | 1 | 0 |
| `O7` | [Content from data](O7-content-from-data.md) | planned | 3 | 0 | 1 | 2 | 0 |
| `O8` | [More playable characters and modes](O8-characters-and-modes.md) | active | 4 | 0 | 2 | 1 | 1 |
| `O9` | [Player experience](O9-player-experience.md) | active | 3 | 0 | 2 | 1 | 0 |

## Always-on rules

These rules apply to every branch, whatever the item's objectives are. The full text is in the objective file.

| Rule | Short form | Active |
|------|-----------|--------|
| `O1-R1` | No global session state | after `096` |
| `O1-R2` | Components, not new inheritance (preference) | now |
| `O1-R3` | No new fragile order | now |
| `O1-R4` | Typed state, not new property keys | now |
| `O1-R5` | Code only with a production caller | now |
| `O2-R1` | Input only through `InputFrame` | now |
| `O2-R2` | Random values only from the seeded source | now |
| `O2-R3` | One fixed tick in all play | after `010` |
| `O2-R4` | Stable iteration order; players in slot order | now |
| `O2-R5` | No logic assumes one player | now |
| `O2-R6` | No logic assumes one camera | after `094` |
| `O2-R7` | Gameplay does not depend on rendering or audio | now |
| `O2-R8` | Local-only data stays out of the simulation | now |
| `O2-R9` | Events and progress name a slot | now |
| `O3-R1` | No JVM-only API on the web path | after `032` |
| `O5-R1` | No per-frame allocation in common paths | now |
| `O5-R2` | Measure hot paths before and after | now |
| `O6-R1` | CI passes | now |
| `O6-R2` | Named checks for kept and changed behavior | now |
| `O7-R3` | No level-identity checks (preference) | now |
| `O8-R2` | Capabilities, not character checks | now |
| `O9-R1` | Fully playable with an NES-style pad, except as X | now |
| `O9-R2` | Saves stay loadable | after `007` |

## How to use this folder

- A branch reads this file and the file of each objective that its item links. It does not read the other files.
- A rule with `after <item>` waits for that item. Until the item merges, a branch may repeat the current pattern, and
  the check says `waiting`. A new kind of violation is still a `conflict`.
- A branch does not edit this folder. The current state and the redo risk change on `master`, at the merge.
- A change to a rule needs the owner's approval.
- A rule with `Strength: preference` is a design preference, not a strict rule. A branch weighs it as one cost
  among the others and does not rule out an option only because the option breaks it. A choice that goes against it
  gives the reason in `SCOPE.md` (the objective check says `conflict`, with the reason). A rule with no `Strength`
  field is strict (owner, 2026-11-10).
- A conflict between two objectives goes to the owner. No objective wins by default.
- `docs/mega-refactor/CLAUDE.md` gives the loop steps.
