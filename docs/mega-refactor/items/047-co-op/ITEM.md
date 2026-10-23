# 047: Co-op with multiple controllers

- Status: planned
- Type: feature
- Depends on: `029`, `031`
- Related: `007`, `008`, `013`, `030`, `038`, `051`
- Branches: none yet

## Goal

Support more than one controller and a co-op mode.

## Owner's input

From the first request (summary): all controller and behavior logic assumes one player. Add support for more than one controller and a co-op mode.

Item `029` research (2026-10-23, verbatim):

> I'd say just 2 players, but ideally the design should be able to scale up to 4 (or even more) players.

> Save states should probably be player-count agnostic, anyways. Let's say that there's co-op for the main campaign (intro stage + 8 robot masters + wily stages). Let's say that on day 1, 2 players are playing through the game. Then, on day 2, only one player wants to play. On day 2, the player can pick up where the 2-player gameplay from the previous day left off. In this case, save states are agnostic to player count; one player is registered by default on bootup, and a 2nd (or nth) player can register or de-register after the fact. (This might confirm the following design decision: camera follows only the primary player, and the primary player is treated as "the source of truth", and anything that the primary player earns is shared with the other players, for the sake of simplicity. You can fold this parenthetical into an existing or new item's notes.)

> Having it behind an interface might not be sufficient if nth players (other than the primary player) can drop in and out of the game on the fly.

## Decisions

- H (2026-10-22): lives are a shared pool or per player, by game mode. Use an interface (for example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap. There will be a maximum number of players.
- H (2026-10-22): split co-op into sequential items, easiest first: (1) shared screen (M–L: camera rule, off-screen rule, room transitions and respawn with 2 players); (2) split screen (L–XL: two cameras and viewports; spawn, cull and checkpoint rules must handle 2 cameras); (3) online (XL: lockstep on top of `010`, `011` and `031`, after `055`).

- 2026-10-23: the target is 2 players; the design must scale to 4 or more.
- 2026-10-23: one player is registered at boot. More players can join and leave during play. Saves do not depend on the player count.

## Open questions

- Camera: follow one player, or a group bounds? The owner leans to "the camera follows the primary (lead) player" (2026-10-23).
- The lead player as the source of truth, with earnings shared by all players: confirm. This is one profile for the session. The lead slot is a session policy, not a helper in the players query API (`029` decision).
- What happens when the lead player leaves? Does the next slot become the lead?
- Respawn with drop-in and drop-out: is a respawn routine behind an interface (`038`) enough?
- The other shared-screen rules: see `029` `NOTES.md`, "Co-op gaps and ideas".
- Boss intro landing checks (`GutsTank`, `ElecDevil`, `WilyFinalBoss`) use `players.all { landed }` after `029c`. A dead player or one who waits to respawn never lands, so the boss would never start. Rule needed: skip players who are not present? (`TODO` comments at the four sites.)

## Sources

- `NOTES.md` in this folder.
