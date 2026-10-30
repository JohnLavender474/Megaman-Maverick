# Contract: mega-refactor-084a-player-state

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-30

The branch changes only where the values are stored and where the teleport hooks are kept. So the checks cover the
3 teleporters, the weapon switch, the first spawn and a respawn, and the behaviors that write `aButtonTask` and
`running`. No new test map is needed: the rooms below already exist.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - `TeleportsTest`: the start hook runs before `teleporter` is set, and the end hook runs before it is cleared (the
    hooks now come from the `TeleportableComponent` constructor). An entity without hooks still teleports.
  - `TeleportsTest`: `TeleportableComponent.reset()` clears `teleporter` and keeps the 3 hooks.
  - A new `Megaman` test with `testServices()`: after `currentWeapon = A` and then `currentWeapon = B`, the weapons
    handler sees `previous = A`, and `currentWeapon` reads `B`. The first set in `init()` sees `previous = null`.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] No code uses the deleted keys and helper: `grep -rn 'MegamanProps\|ConstKeys.RUNNING\|ON_TELEPORT_\|setStandardOnTeleportContinueProp' core/src engine/src`
  finds nothing.
- [ ] A short launch (about 15-20 s) of `REFACTOR_TEST_029` starts with no exception. This checks the `lateinit`
  fields at the first spawn.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084a-player-state`

Teleporters (the hooks):

- `./run-game-alpha.sh --level REFACTOR_TEST_031 --room pipe_portal`: enter a pipe while you charge a shot. The charge
  stops, Mega is hidden in the pipe and takes no damage, and the camera follows as before. At the exit, Mega shows
  again, gravity and collisions work, and an air dash is possible at once in the air.
- `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: use the `PortalHopper` portals, also during an air dash.
  The air dash stops at the start, Mega is hidden while in the portal, and Mega comes out at the other portal with
  the same behavior as above.
- `./run-game-alpha.sh --level WILY_STAGE_3 --room room3`: use the `CapsuleTeleporter`. The same checks as above.

Weapon switch (the `currentWeapon` setter):

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons all`: switch through all weapons with the
  select button and in the pause menu. Each switch stops a shot in progress and the HUD shows the new weapon.
  With `PRECIOUS_GUARD` out, switch weapon: the guard gems go away. Wall slide with `RODENT_CLAWS`, then switch weapon
  during the wall slide: Mega falls as normal.

Movement state (`aButtonTask`, `running`, `facing`):

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run left and right (run animation and speed), jump,
  air dash, wall slide and wall jump, ground slide. Mega faces the direction of movement.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: go through the gravity switch. After the
  switch, A in the air gives an air dash and A on the ground gives a jump, as before.

## Behaviors to check

- Death and respawn: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die and respawn. After the
  respawn Mega has the Mega Buster, faces right, does not run, and the A button jumps.
- Hooks after a respawn: `./run-game-alpha.sh --level MOON_MAN --room rest_room_1`: die once (a pit or an enemy), go
  back to a `PortalHopper` and use it. The teleporter checks above still pass. The hooks are now set once in `init()`
  and must survive the component `reset()` at death.

## Intended changes

None.
