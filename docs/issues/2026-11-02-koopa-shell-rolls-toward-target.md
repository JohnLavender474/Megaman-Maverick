---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-047b-player-rules enemy review (2026-11-01)
related-items: [047, 112]
---

# A Koopa shell takes its roll direction from the target, not from the player who kicked it

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/GreenKoopaShell.kt:150,187`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/enemies/RedKoopaShell.kt:151,188`

## What happens

The roll direction reads `targetPlayer`. With `--players 2`, the nearest player can be a different player from the
one who kicked the shell. So the shell can roll toward the kicker.

## What should happen

The shell rolls away from the player who kicked it. The stomp path knows that player.

## How to reproduce

Found by reading the code. Not run. With `--players 2`, stand both players next to a shell, with the other player
nearer to the shell, and kick it.

## Short contract

Branch: `fix/koopa-shell-kicker`, off `master`, in `.worktrees/fix/koopa-shell-kicker`.

Scan (2026-11-09): in `GreenKoopaShell` and `RedKoopaShell` (the same code), `IDLE` starts the roll when
`shouldStartRolling()` finds a live player whose side fixtures, feet or body touch the shell (`players.anyLive`, since
`fix/players-any-dead-slot`). Then `startRolling()` and the `IDLE` branch both set the roll direction from
`targetPlayer` (the nearest live player), not from the player who touched the shell.

Intended changes:

- The touch test returns the kicker: the first live player in slot order that touches the shell (the same tests as
  today). The shell rolls away from the kicker's x, in both classes. The duplicate direction line in the `IDLE` branch
  goes.

Changed files: `GreenKoopaShell.kt`, `RedKoopaShell.kt`, tests, and a new room `koopa_kick` in `RefactorTest047.tmx`.

Not changed:

- One player: the kicker is that player, the same as `targetPlayer`, so the direction is the same as today.
- What counts as a touch, the roll speed, the bounce at walls, the shell against shell, the kick sound.
- `GreenKoopa`, `RedKoopa`, `Goomba`.

Checks:

- Unit test with 2 players and a real shell (spawn through the engine, the updatables): the kicker touches the shell
  from one side while the other player stands nearer on the other side, untouched: the shell rolls away from the
  kicker. The mirror case. One player: the old direction.
- Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/koopa-shell-kicker`. On the same
floor a player who touches the shell is always nearer than one who does not, so a side kick rolls away from the kicker
also on `master`; the fix shows on a stomp, with the other player standing very close. The room `koopa_kick` is new in
`RefactorTest047.tmx` (flat floor, walls, one `GreenKoopa`, spawn `23`; review before the play test, 2026-11-09).

- `./run-game-test-coop.sh --room koopa_kick`: stomp the `GreenKoopa` into a shell on the floor. Slot 1 stands to the
  right of the shell, as close as it can without touching it (walking into it kicks it, so stop just before). Slot 0
  jumps and lands on the LEFT half of the shell: the shell rolls RIGHT, away from slot 0, and hits slot 1 (on `master`
  it rolled LEFT). Make a new shell (the Koopa comes back after the room is entered again, or kick the shell into a
  wall and catch it) and swap: slot 1 on the left, slot 0 lands on the RIGHT half: the shell rolls LEFT.
- `./run-game-test-coop.sh --room koopa_kick`: a plain side kick by either player (walk into the shell): it rolls away
  from the kicker, as before.
- `./run-game-alpha.sh --level SMB3_1`: control with 1 player (the level start; `room1` has no player spawn). Walk left
  into `room1`, to the `GreenKoopa` on the step pyramid (about 31 tiles), make a shell on the flat floor, kick it from
  each side and stomp it: it rolls away from Megaman, as before.
