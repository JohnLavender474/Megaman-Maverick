# Contract: mega-refactor-081f-x6-intro-fixes

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.src.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-09 (brief); scope changes 2026-11-09

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

From `.worktrees/mega-refactor-081f-x6-intro-fixes`.

| Level | What to check |
|-------|---------------|
| `X6_INTRO --room room2` (through the boss gate) | The new fight: the D-1000 track plays and loops; the larger art; the eyeball at 8 tiles, dim when closed, open while the body commits; each new attack shows its warning (throw pose, the rise, the wall shake, the jitter, the eye shake); at half HP a rage, then Overdrive; touching the art hurts X, a gap in the art does not; shots stop on the art. |
| `X6_INTRO --room room2` (through the boss gate) | The larger room: the camera follows X left and right and never scrolls up or down; the ceiling strip is at the top of the screen and the floor at the bottom; both eyeball homes are on the screen at every camera position; Wall Crash and Ceiling Crash junk falls from just under the ceiling, all pieces at the same height; the Pillar Fence spans the full width; a ram to the far wall while X is at the other end still hurts X when X walks back into the body. On Normal: a longer THINK, fewer combos, 3 Wall Crash pieces, a slower ram, longer Ceiling Crash, Spotter and Dive warnings. |
| `X6_INTRO --room room2` | Walk, jump and dash into the boss gate: the stage music fades out; X shows the run loop through the gate (from a stand, a walk, a landing or a dash end); a jump or fall pose keeps its frame. |
| `X6_INTRO --room room1` | The first gate (room1 to room2): the same pose rules; the music does not fade there. |
| `X6_INTRO --room room2` (through the boss gate) | `D1000` lands with its feet on the floor (entry, crouch, stun); touching its body hurts X (3 HP); a bomb, a pillar and a core shot hurt X (3, 3, 2); the full charge and the sabre hurt the body; on the walk it turns toward X and its box reaches the wall; the defeat explosions are at the boss. |

## Behaviors to check

Behaviors that are not tied to one level, for example the pause menu, death and respawn, game
over, or the save state.

## Intended changes

Gameplay changes that this branch makes on purpose. Leave empty if there are none.

- The `X6_INTRO` boss room fades out the stage music (`fade_out_music=true`).
- X's animation in a room transition: every pose except `run` holds its frame for the whole transition; `stand`, `land_shoot`, `run_trans`,
  `run_trans_shoot`, `dash_end` and `dash_end_shoot` become `run`; other keys stay.
- `D1000`: its body is at the boss from the spawn, so its body damages X and takes hits; it lands with the feet of
  the landing pose on the floor (56 px with the 1.25x art; was 64); it rises toward X's height, with the hit box between 0.5 tile over
  the floor and 0.5 tile under the ceiling (was a fixed 3.5 tiles), and the ram wind-up uses the same range (was 2 to
  3.5 tiles); the low limit also keeps the stand, walk, throw and ram art over the floor (floor + 84 px with the 1.25x art); it moves to
  its hover height in both directions at the rise speed; a stun at a low hover skips the stagger and falls at once;
  the `D1000` spawn rectangle reaches the room ceiling; the walk half-width is 70 px with the 1.25x art (was 64); it turns toward X on each walk frame (was once).
- Damage of `D1000` to X: body 3, bomb 3, pillar 3, core shot 2 (was 6, 6, 6, 4).
- Scope changes (owner, 2026-11-09; the values are in the CUSTOM tables of `NOTES.md`): faster pacing; the D-1000
  fight music `MMX6_D1000.ogg` loops; the eyeball spawns 8 tiles over the floor; the body art is 1.25 times larger
  and the body sizes scale with it; the new attacks, Overdrive and the director; the eyeball opens only while the
  body commits, has the Spread and Dive attacks and does 2 contact damage; the hit boxes of the body and the eyeball
  follow the drawn frame.
- Room tuning (owner play test, 2026-11-09: "too tough given the small room"; "the debris didn't fall from the very
  top"): the `X6_INTRO` `boss_room` is 24 x 14 tiles (was 16 x 14) and the `D1000` arena is 21.5 x 12 tiles (was
  11.5 x 10); the camera follows X across the room and holds the full room height; the debris starts 0.25 tile under
  the visible ceiling; every arena position is relative to the arena; Normal is easier than Hard (the table
  "Room tuning" in `NOTES.md`).
- Second tuning (owner play test, 2026-11-10): the `boss_room` is 21 x 14 tiles and the arena 18.5 x 12; the body moves
  about 10 to 15 % faster; more debris (Wall Crash 5 / 6 pieces, Ceiling Crash 5 lanes); the idle eye shoots 1 aimed
  shot every 2 s (Hard 1.4 s); the eye art is brighter (the table "Second tuning" in `NOTES.md`).

## Behaviors that must not change

- Megaman's animation, damage and play in every Megaman stage (no Megaman file changes; `PlayerDamageNegotiator`
  changes only `D1000` entries).
- The X6 values that the CUSTOM tables do not list: health 30, the damage values, the stun damage 8, the pillar.
