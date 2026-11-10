---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-047b-player-rules code review (2026-11-02)
related-items: [047]
---

# With `--players 2`, a dead slot 1 still counts at the `players.any` sites

## Where

- `engine/src/main/kotlin/com/maverick/game/engine/players/IPlayers.kt`: `IPlayers.any` tests no life or `inPlay`.
- 74 `players.any` lines in `core/src/main/kotlin/com/megaman/maverick/game/entities/`, for example `enemies/Cactus.kt:364` and `enemies/FireDispensenator.kt:240,248`.
- `core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt`: the `TODO` at `anyLive`.

## What happens

A dead slot 1 stays in the player list, and its body stays at its last position. So it can trip a scanner. This gives
a false trigger, not a stuck state.

## What should happen

A dead player does not count while another player is live. `MegaPlayers.anyLive` does this, and with no live player it
visits all players, so a game with 1 player does not change. The 8 sites of `047b` use it. Each other site needs a
review, because some may act on purpose for a dead player.

## How to reproduce

With `--players 2`, kill player 2 inside the scanner of a `Cactus`, with player 1 away from it. The `Cactus` reacts.
Found by reading the code; not run.

## Short contract

Branch: `fix/players-any-dead-slot`, off `master`, in `.worktrees/fix/players-any-dead-slot`.

Scan (2026-11-09, at `437a3df09`): 33 code lines in `core/src/main/kotlin/com/megaman/maverick/game/entities/` call
`players.any` (25 files: enemy scanners and triggers, `AbstractEnemy.isMegamanShootingAtMe`, `PushableBlock`, `CartV2`,
`DropperLift`, `InfernoChainPlatform`, `FlipperPlatform`, `GravitySwitcharoo`). Each one asks "is a player here, or doing
this, now"; none needs a dead player. Since `047c` (2026-11-09), either slot can be dead for a while in co-op (the 1 s
respawn wait, the wait for a safe spot, or for good at 0 lives), and its body stays where it died, so the false trigger
is easier to meet than when the issue was found.

Intended changes:

- The 33 entity lines call `players.anyLive` in place of `players.any`. `anyLive` skips a player that is not live
  (dead, or beaming in) while another player is live; with no live player it visits every player, as `any` does.
- The `TODO` at `MegaPlayers.anyLive` goes; the `047e` part of that `TODO` (boss intros, `Fight`) is already tracked
  at the boss sites.

Changed files: the 25 entity files, `MegaPlayers.kt` (the `TODO`), tests.

Not changed:

- One player (single player): `anyLive` visits the one player whether it is live or not, the same as `any`.
- Both players dead: every player is visited, as today.
- `MegaLevelScreen` `anyInPlay` and `anyAlive`, and the `FEET_SAND` rule (`players.any { it.inPlay }`): they ask about
  the play phase on purpose (`047c`, `fix/quicksand-two-players`).
- `engine/` (`IPlayers.any` stays for those callers).
- `ColtonJoe` (the line is a comment).

Checks:

- Unit tests:
  - `CartV2` (the `CartV2ItemTest` rig): a dead slot 1 whose feet stay on the cart does not move it while slot 0 is
    live; a live slot 1 on the cart moves it; with one player, the old result.
  - `MegaPlayers.anyLive`: a dead or beaming slot is skipped while the other is live; with no live player every player
    counts.
- Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/players-any-dead-slot`:

- `./run-game-test-coop.sh --level REFACTOR_TEST_127 --room warp_enemy`: slot 0 stays at the far left, out of the
  `Cactus` scanner; slot 1 walks into the scanner and dies to the needles. While slot 1 is dead (the 1 s before its
  respawn), the `Cactus` goes back to idle and does not face or shoot at the dead body. To see it for longer, lose all
  lives with slot 1 first (3 deaths): then slot 1 stays out, and the `Cactus` stays idle.
- `./run-game-alpha.sh --level DESERT_MAN`: control with 1 player; the `Cactus` and the other enemies react to Megaman as
  before. The review before the play test names the room with a `Cactus` and a player spawn.
