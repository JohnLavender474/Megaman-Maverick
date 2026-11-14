# Ways of working

How work on this repo is done: the owner's preferences for the approach to a task, as opposed to how the code looks.
Read this file before you start a task, and give it to each subagent that does one.

How Kotlin code looks and is shaped is in `docs/CODE_STYLE.md`. The process of refactor branches (planning, briefs,
reviews, merges) is in `docs/mega-refactor/CLAUDE.md`.

## Art

- **Fix art in the art, not in code.** When a sprite frame sits wrong (a pose a few pixels off center, a frame that
  is not aligned with the frames next to it), move the pixels in the sprite files: the frame strip in
  `assets/sprites/frames/`, the packed atlas in `assets/sprites/sprite_sheets/`, and the slicer data file in
  `utils/sprite_slicer/sheets/` when the slicer makes that strip. Do not add a draw offset in code for it. A code
  offset hides the fault in the art from the next reader, and every other user of the frame must repeat it.

  A draw offset in code stays correct where the shift comes from game state and not from the art, for example the
  rotation of a sprite with the gravity direction.

  Example (owner, 2026-11-12, branch `081g`): X sat 1 to 2 px right of the ladder while he climbed. The fix moved the
  `climb` and `climb_shoot` frames 2 px left in the frame strips and in the atlas, not a `translateX` in the sprite
  pre-process.

- **Trace X6 poses; do not design them.** When you redraw an X6 pose for a Maverick sheet, take the limb shapes from
  the X6 frame, shrunk to the sheet's scale, and change only the style: flat artist colors, a black outline, a black
  sole, the artist's cuff lines. Reuse the artist's own parts (the head, the `stand` legs) where the pose matches.
  Compare each frame next to the X6 frame at the same scale. Draw outlines so that each edge steps at most 1 px per
  row. (Hand-made leg shapes in the sabre work, 2026-11-05, came out "super obese"; legs traced from X6 were "far
  better".)
- **A small X6 sprite may be larger than X6.** Ratio 2 is the default for a converted sprite (the map and the
  bosses). When ratio 2 makes a small enemy look squished or too low on detail, propose a smaller ratio (for example
  1.6) for that object only, and scale its body, offsets and shots by the same factor (owner, 2026-11-03, Junkroid and
  Hover Gunner). In the demake tools, record the ratio in the object's `catalog/nessprite-<name>.json`.

## Git and processes

- **Stage by path.** Never use `git add -A` or `git add .`. The owner keeps local edits during a play test (for
  example a muted run script), and other sessions change files. Run `git status --short` before each commit. Before a
  squash merge, run `git diff --stat <base> <branch>` with no path filter, and confirm the list of files.
- **The main checkout is shared.** All sessions that work on `master` use the same index there, and a plain
  `git commit` takes what another session staged. Before a merge or a commit in the main checkout, run
  `git status --short`. If a change is staged or modified that is not yours, stop and ask the owner. Commit with
  explicit paths where you can, and check `git show --stat HEAD` after the commit.
- **Select a process by its PID.** Several sessions run Gradle at once on this machine. List the candidates with
  the PID, the age (`etimes`), the working folder (`readlink /proc/<pid>/cwd`) and the parent, keep only those whose
  folder is your worktree, and act on those PIDs. Never use `pkill -f <name>`, and never take `pgrep ... | head -1`
  as yours. (A `pkill -f` on 2026-11-11 stopped the test JVMs of other sessions.) A `pgrep -f` pattern also matches
  the shell whose command line holds it. A Gradle daemon's folder is `~/.gradle/daemon/<version>`, not the worktree.

## Gameplay iteration

- **Iterate fast; test when the design is final.** While the owner play-tests and changes a moveset or a feel (a
  boss's attacks, timings, motion), each round is: change the production code, build the game
  (`./gradlew lwjgl3:jar`), and hand it back for play. Do not write new unit tests, run mutation checks, update
  broken tests or dispatch review agents in these rounds. Small changes are made directly, not through an agent.
  When the owner says the moveset is final, do one pass: tests for the final design, mutation checks, the full
  automated checks and the review before the play test. Meanwhile, keep the spec in plain words: one line per rule of
  behavior that must stay true (and one line per existing test that a round broke), in the branch
  `docs/TEST-DEBT.md`. The final pass turns each line into a test. The merge rules
  do not change: a branch still merges only
  with every check of its contract passed. (Owner, 2026-11-14, branch `077a`: waiting for test rewrites at each
  boss change was too slow.)

## Levels

- **A level draft is a playable skeleton.** Before you draft a TMX level, read `assets/tiled_maps/tmx/Test8.tmx`,
  `IntroStage_v3.tmx`, `ReactorMan_v3.tmx` and `InfernoMan_16x14_v2.tmx` from start to end.
  - Put real block geometry in the `blocks` layer (floors, walls, ceilings, platforms), not only spawn points. Copy
    the block patterns of `Test8.tmx`.
  - Give each spawn object the entity's real size: copy it from a reference level, or read the entity's `onSpawn`.
  - Every alternative path joins the main path again; no dead ends.
  - Tile layers in a level draft: ask the owner whether to paint them to match the blocks or to leave them out
    (default: leave them out; no empty placeholder layers). A test map is different: it always has a painted `tiles`
    layer (`docs/mega-refactor/CLAUDE.md`, "Test maps").
- **Read room geometry from map objects, not from hard-coded values.** When an entity needs a place in its room (a
  floor, a wall, a ceiling, an arena, a spawn or target point), give its map object an object property that refers
  to the map object of that place, and read it in `onSpawn` with `!!`, so that a missing reference fails fast. Do
  not compute the place from constants or from offsets to another position (for example "the floor is 2 tiles below
  the landing point"). Such values break without an error when the room changes, and the next reader cannot see
  where they come from. Refer to the block objects that are already in the map where you can, in place of new
  marker objects. A value that is not a place in the room, for example a speed, a time or a size of the entity
  itself, stays in code.

  Example: `TimberWoman` reads its `ground` and `wall_*` references. In branch `077a` (owner, 2026-11-14), the arena
  of `BigAssMaverickRobot` changed from offsets to the landing point to `ground`, `left_wall` and `right_wall`
  references to the floor and wall blocks of the boss room.
- **A gauntlet wave holds only enemies that can die.** A `GauntletMaster` wave ends only when all its enemies are
  destroyed, so an enemy that cannot take damage locks the player in the room. Before you put an enemy in a wave,
  read its class: look for `invincible` (for example `SpringHead`) and other no-damage logic such as a permanent
  shield, and check that the enemy cannot be culled or stuck out of reach. Keep drafts simple, and check each enemy's
  real behavior in its code.
