# Contract: mega-refactor-061b-play-phase-teleport

The gameplay that this branch must not change, and how to check it. Written ahead of time on `master`.

- Status: approved
- Approved: 2026-10-26

The player's `ready` becomes the play phase and `teleporting` becomes the teleport state; both move from player
state to entity state. Everything that waits for the player to be in play, or reacts to a teleport, must act as on
`master`: the spawn beam-in, death and respawn, the level-end beam-out, the hazards that wait for the player, the
dark rooms, rain, sand, pause, the checkpoint text, and the three teleporters (with the pipe animation, the hidden
sprite, and the air dash reset at a teleport start).

## Automated checks

- [ ] `./gradlew engine:test` passes: the 501 tests on `master`.
- [ ] `./gradlew core:test` passes: the 159 tests on `master`, minus the removed `ready`/`teleporting` cases, plus the
      new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] `contact-rule-selection.txt` is unchanged.
- [ ] Play phase tests: an entity without the component reads `IN_PLAY` and `inPlay`; `setPlayPhase` changes it;
      `setPlayPhase` without the component fails; `reset()` keeps the value.
- [ ] Teleportable tests: an entity without the component is not teleporting; `beginTeleport` calls the entity's start
      hook first and then sets the teleporter (the hook sees `teleporting == false`); `endTeleport` calls the end hook
      first and then clears it (the hook sees `teleporting == true`); `reset()` clears it; an entity without hooks is
      still marked; `beginTeleport` on an entity without the component fails.
- [ ] Code checks (grep): no `PlayerStateComponent`, no `ConstKeys.TELEPORTER`, no `MegaPlayer.ready`/`teleporting`,
      no `Megaman.ready`/`teleporting` property; no teleporter reads `ON_TELEPORT_START`/`ON_TELEPORT_END` itself; no
      `players.ready` import; `AbstractEnemy` sets no teleport hooks.
- [ ] One for one: each former `ready` reader reads `inPlay` (the same negation), and each former `ready` writer
      writes the matching phase (`false` at the spawn start → `PRE_PLAY`, `true` → `IN_PLAY`, `false` at death or
      level end → `POST_PLAY`); each former `teleporting` reader reads `teleporting` on the same entity.

## Manual playthrough

The owner plays these once, after the implementation, from the worktree.

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: during the beam-in, Megaman cannot move, shoot or
  pause; after it, all works. Die once (a pit or an enemy): the respawn behaves the same.
- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: the pipe teleport, with the pipe animation; an
  air dash into the pipe ends as before.
- `./run-game-alpha.sh --level MOON_MAN --room room11`: a `PortalHopper` teleport (Megaman hidden while in transit).
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: a `CapsuleTeleporter` teleport.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room2`: the `LaserBeamer` waits for Megaman during the beam-in,
  and fires after it.
- `./run-game-alpha.sh --level INTRO_STAGE --room room3`: the `BuilderKibbo` waits during the beam-in.
- `./run-game-alpha.sh --level INTRO_STAGE --room room12`: beat the boss; the level-end beam-out runs, and the next
  screen follows.
- `./run-game-alpha.sh --level DESERT_MAN`: in the dark, die once: the light at Megaman's last position goes out, as
  before.
- A room with sand or rain, if one is at hand: the sand splash and the rain react to Megaman as before.

## Behaviors to check by review

- `PreciousGem` hides while its owner player teleports (no room has both).
- The `SMB3_1` `BulletBillLauncher` and `TubeBeamer` wait for the player (their reads are one for one).

## Intended changes

None. The teleporter is cleared when Megaman is destroyed (on `master` the flag stays `true` until his next spawn); no
reader runs while he is destroyed, and `PreciousGem` destroys itself on `PLAYER_JUST_DIED`.
