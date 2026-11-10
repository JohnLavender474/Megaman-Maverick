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

Changed files: `GreenKoopaShell.kt`, `RedKoopaShell.kt`, tests.

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

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/koopa-shell-kicker`:

- `./run-game-test-coop.sh --level SMB3_1 --room room1`: walk right to the first `GreenKoopa` (about 30 tiles) and
  stomp it into a shell. Slot 1 stands close to the shell on one side; slot 0 kicks the shell from the other side. The
  shell rolls away from slot 0 (toward slot 1). Then swap the roles. The review before the play test confirms the
  room and the distances.
- `./run-game-alpha.sh --level SMB3_1 --room room1`: control with 1 player. Kick a shell from the left and from the
  right: it rolls away from Megaman, as before.
