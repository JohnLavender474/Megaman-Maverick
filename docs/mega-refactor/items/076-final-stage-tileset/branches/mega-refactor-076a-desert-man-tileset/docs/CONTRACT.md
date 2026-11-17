# Contract: mega-refactor-076a-desert-man-tileset

- Status: approved
- Approved: 2026-11-17

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The remap script maps every gid that the old map paints (it fails on a gid with no mapping), and reports
  that the new map paints no gid of `Tileset1` or `tomahawk_man_stage_tileset`.
- [ ] Outside the `<tileset>` elements and the CSV of the 5 tile layers, the TMX is byte-identical to `master`
  (a script diff of all other lines).
- [ ] The number of non-empty cells per tile layer is the same as on `master`.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from the worktree `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-076a-desert-man-tileset`.
Experimental checks (the new art) come first; there is no separate control list, because the branch changes no code.

| Level | Room | What to check |
|-------|------|---------------|
| `DESERT_MAN` | `room1` | Rock, rope fence, crates, rope with anchor and tie, plants on the rock. No tile seams, no wrong edge. |
| `DESERT_MAN` | `room3` | Crates, hanging crates on ropes, ladder, brass spikes on black. Climb up to `room4` and `room5`: the new gate-column art in `room5`. |
| `DESERT_MAN` | `room9` | Ladder, girder, the rock blocks and the back wall. The gap-0 join between the two right-hand blocks keeps its sand lip. |
| `DESERT_MAN` | `room10` | Long girders and pillars, the cobble ceiling, the back wall over a large area. Look for lines between tiles while the camera scrolls. |
| `DESERT_MAN` | `room11` | Climb into `room12`: many ropes and crates. |
| `DESERT_MAN` | `room14` | The new gate-column art at the gate. |
| `DESERT_MAN` | `room15` | Oasis soil, palm, plants, the bloom (it must not look like a pickup), the sandstone pillars, the wall band, the spikes in the water: they must read as deadly. |
| `DESERT_MAN` | `room16` | The room before the boss: rock and back wall. Go through the gate and fight Desert Man with `--heartTanks all --healthTanks all`: the boss room tiles. |

## Behaviors to check

- The level plays the same: every block, ladder, spike death, water and gate is where it was. (The tiles are only
  drawn; the objects did not change, and the automated TMX check shows it.)

## Intended changes

- Desert Man looks different: every tile is new art. The 86 back-wall cells where the sunset showed faintly through
  the old semi-transparent wall now show the opaque new wall.
- No gameplay change.
