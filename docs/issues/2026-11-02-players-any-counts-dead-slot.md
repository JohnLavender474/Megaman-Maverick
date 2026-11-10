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

Scan (2026-11-09, at `437a3df09`): 36 code lines in `core/src/main/kotlin/com/megaman/maverick/game/entities/` call
`players.any` (30 files: enemy scanners and triggers, `AbstractEnemy.isMegamanShootingAtMe`, `PushableBlock`, `CartV2`,
`DropperLift`, `InfernoChainPlatform`, `FlipperPlatform`, `GravitySwitcharoo`). Each one asks "is a player here, or doing
this, now"; none needs a dead player. Since `047c` (2026-11-09), either slot can be dead for a while in co-op (the 1 s
respawn wait, the wait for a safe spot, or for good at 0 lives), and its body stays where it died, so the false trigger
is easier to meet than when the issue was found.

Intended changes:

- The 36 entity lines call `players.anyLive` in place of `players.any`. `anyLive` skips a player that is not live
  (dead, or beaming in) while another player is live; with no live player it visits every player, as `any` does.
- The `TODO` at `MegaPlayers.anyLive` goes; the `047e` part of that `TODO` (boss intros, `Fight`) is already tracked
  at the boss sites.

Changed files: the 30 entity files, `MegaPlayers.kt` (the `TODO`), tests, and a new room `dead_scanner` in
`RefactorTest047.tmx` (the map of item `047`; no existing room changes).

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
  - `MegaPlayers.anyLive`: the existing `MegaPlayersTest` cases (a dead slot and a slot not in play are skipped while
    the other is live; with no live player every player counts) stay unchanged.
- Break the code and confirm that each new test can fail (`core/src/test/CLAUDE.md`).
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

Play checks, from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/fix/players-any-dead-slot`:

- `./run-game-test-coop.sh --room dead_scanner` (new room in `RefactorTest047.tmx`, one view; a big `Cactus` at the
  left wall, a spike 2 tiles right of it, both players spawn inside the scanner): walk slot 0 to the right wall, out of
  the scanner (the last 5 tiles of floor). Walk slot 1 left into the spike. After its last volley, the `Cactus` goes
  idle (it closes and stops flashing and shooting) while slot 1 is dead, about 1 s before slot 1 beams in next to slot
  0. Do it 3 times: after the third death slot 1 stays out (0 lives), and the `Cactus` stays idle. Before the fix it
  kept flashing and spraying needles every 2 s at the dead body. Then walk slot 0 left into the scanner: the `Cactus`
  turns, faces and shoots again.
- `./run-game-alpha.sh --level DESERT_MAN --room room1`: control with 1 player. Walk right. Each `Cactus` faces
  Megaman, flashes and sprays needles when he is within about 8 tiles, and goes idle when he leaves; the
  `LampeonBandito` stands and shoots when he is near, as before.
- `./run-game-alpha.sh --level DESERT_MAN --room room10`: control with 1 player. Walk right and stand on the cart: it
  moves right while Megaman stands on it, as before.
