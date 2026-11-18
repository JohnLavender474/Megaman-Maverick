# Contract: mega-refactor-079a-smarter-met

- Status: approved
- Approved: 2026-11-18

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new `GroundProbe`, `JumpPlanner` and `Met` reaction tests.
- [ ] `./gradlew core:smokeTest` passes (`Met` loads every new region of `Enemies1`; `entity-tags.txt` unchanged).
- [ ] `./gradlew detektMain core:verifyRandomRule` passes (the dodge chance uses `services.random`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] After the repack of `Enemies1`, a script shows that each region that did not change keeps its name and its
  pixels (`docs/ways-of-working/assets.md`).
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-079a-smarter-met`.
Experimental checks first, then the control checks.

Experimental (`REFACTOR_TEST_079`, new map; room names are final when the map is built):

- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room pit_edge`: the `Met` runs toward a pit with a `Death` area
  and no far side in reach (Mega Man starts beyond the pit). It stops with its whole body on the edge, keeps facing Mega Man and shields; it never falls in. Walk to the right wall and back to respawn the `Met`.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room safe_drop`: the `Met` walks off a ledge onto safe ground 2
  tiles lower. Walk to the right wall and back to respawn the `Met`.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room gap_jump`: with Megaman on the far side of a 3-tile gap, the
  `Met` jumps the gap. With Megaman on its own side, it stops at the gap and shields. Walk to the right wall and back to respawn the
  `Met`s.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room step_jump`: the `Met` jumps up a 1-tile step and a 2-tile
  step; at a wall too high to clear, it stops and shields (Mega Man starts on the high floor at the right; stand on the 2-tile
  step to draw the `Met` up). Walk to the right wall and back to respawn the `Met`.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room platform_jump`: walk left and stand on the right platform
  (the one next to the `Met`); the `Met` jumps to it; a platform with a low ceiling in the path is not tried. Walk to the right wall and back to respawn the `Met`.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room charged_stagger`: a charged shot on a `Met` with its helmet
  does no damage. The helmet flies up as its own object (higher for a full charge, which also pushes the `Met` back a
  little), follows the head and falls back on it. Meanwhile the `Met` is dizzy with spinning stars and plain shots
  hurt it; the helmet passes through Mega Man and deflects shots. When the helmet lands, the `Met` is
  pushed down, comes back up and shields. The stun never ends before about 0.9 s nor lasts longer than about 1.8 s. Walk to
  the left wall and back to respawn the `Met`s (the room is 40 tiles wide).
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_break`: the 3rd charged hit (any mix of half and
  full) breaks the helmet into pieces, also a helmet in the air (shoot it while it flies). The `Met` then runs without its helmet (no shots), with short pauses; it still
  avoids the pits of the room and dies to plain shots. Walk to the left wall and back to respawn the `Met`.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_break --difficulty hard`: the hard-mode `Met` turns to
  Megaman before each run, runs longer, and dodges some plain shots (helmet down, or a hop when the hop is safe).
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room turned_met`: a `Met` on a ceiling and one on a wall (turned by
  `GravityChange` areas) still shield, pop up, shoot, run and stop at their edges. Walk to the left wall and back to
  respawn the `Met`s.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room frozen --weapons FRIGID_SHOT`: hit an exposed `Met` with the
  ice shot; the frozen `Met` stays frozen and does not jump or stagger while frozen. Walk to the left wall and back to
  respawn the `Met`s.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_pit`: walk right; a test shooter hits the `Met` with a half charge, and a test mover drops the flying helmet into the pit. The helmet falls out of view, and the `Met` cries and goes desperate; it never jumps into the pit. Walk to the left wall and back to repeat.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_platform`: a half charge stuns the `Met`, and the test mover puts its helmet on the 2-tile platform. The `Met` jumps up, picks the helmet up and shields again. Walk to the left wall and back to repeat.
- `./run-game-alpha.sh --level REFACTOR_TEST_079 --room helmet_swap`: a half charge stuns the near `Met` A; kill A with plain shots while its helmet flies, and the helmet rests free. The far `Met` B takes 3 half charges, goes desperate after the 3rd, runs into A's free helmet and puts it on. Walk to the left wall and back to repeat.

Control:

- `./run-game-alpha.sh --level DESERT_MAN --room room9`: the 2 `Met`s act as above in a real stage; no `Met` falls
  into a pit.
- `./run-game-alpha.sh --level DESERT_MAN --room room10`: the 5 `Met`s on the long floor.
- `./run-game-alpha.sh --level GAUNTLET_1 --room room1`: the waves with `Met`s end; every `Met` can be killed.
- `./run-game-alpha.sh --level WILY_STAGE_2 --room room9 --heartTanks all --healthTanks all`: in the next fight,
  `GutsTank` launches its running `Met`s as before (they run straight and fall as today).
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_block`: the `run_only` `Met` runs as before.

## Behaviors to check

- A `Met` that dies drops items as before.
- A `Met` hit by the instant-kill damagers (`GutsTankFist`, `BigAssMaverickRobotOrb`, `PurpleBlast`) still explodes.
- A pooled `Met` that respawns has its helmet back (helmet hits reset in `onSpawn`).

## Intended changes

- A `Met` (not `run_only`) does not run into a `DEATH` area or a fall with no ground within 4 tiles; it may still
  drop onto safe ground.
- A `Met` jumps across gaps, up steps and to a higher platform under the jump rules in `SCOPE.md`.
- A `ChargedShot` on the closed helmet does no damage: it stuns the `Met` and knocks the helmet off as a
  `MetHelmet` entity (it drifts away, rests on blocks, passes through Mega Man). On an exposed `Met` a charged shot
  does normal damage; a surviving `Met` is stunned. The 3rd charged hit on a helmet breaks it.
- A stunned `Met` fetches its helmet (worried run, jumps) and puts it on, or cries and goes desperate when it cannot.
  A helmetless `Met` runs with short pauses and never shoots; it puts on any free helmet it touches.
- The closed helmet and the flying helmet deflect shots with a half-dome shield (radius 0.6 tile at the feet): a shot
  over the dome passes. This also applies to `run_only` `Met`s.
- A hard-mode `Met` shields for 0.8 s (not 1.15 s), faces the player before a run, runs longer and dodges about 1
  shot in 3 (owner, 2026-11-18).
- New `Met` and `SnowMet` frames: helmet, stunned with stars, helmet landing, worried run, pickup, cry, no-helmet run,
  stand and jump, helmet pieces, jump.
