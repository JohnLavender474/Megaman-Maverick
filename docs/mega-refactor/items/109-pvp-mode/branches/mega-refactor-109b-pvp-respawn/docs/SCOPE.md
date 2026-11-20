# Scope: mega-refactor-109b-pvp-respawn

- Status: approved
- Approved: 2026-11-20

## Goal

In a PvP session, a dead player respawns alone, at a spawn point, after a short delay. The level-defeat routine never
runs in PvP: no level reset, no removal of the other player, no game over.

## Item

`109` (PvP modes). From `ITEM.md`, "Open questions":

> Respawn: `PlayerCheckpointManager` holds one current spawn, and `PLAYER_JUST_DIED` is a level flow. What are the
> respawn delay, the spawn point choice and the spawn protection? Do lives exist in a match?
>
> Must fix (owner, 2026-11-20, found in the `109a` play test): when slot 1 kills slot 0, the level-defeat routine runs
> (`MegaLevelScreen.kt` `PLAYER_JUST_DIED`, `respawn.startAtDeath()`). It removes every other player
> (`RespawnSteps.kt` `defeat()`), resets the level and costs slot 0 a life. A PvP death must respawn only that slot,
> with no level reset, and the other player must stay. The opposite case is a gap too: with `slotRespawn=false`, a
> dead slot 1 stays dead until slot 0 dies.

## Current state

- `SessionRules.slotRespawn` (`state/SessionRules.kt:10`) is `true` only in `COOP`. `PVP` has
  `SessionRules(friendlyFire = true)`, so `slotRespawn = false`.
- `MegaLevelScreen` builds `SlotRespawn` only when `rules.slotRespawn` and a slot 1 exists
  (`screens/levels/MegaLevelScreen.kt:382-388`).
- `PLAYER_JUST_DIED` (`MegaLevelScreen.kt:653-661`): with no `SlotRespawn`, the routine starts only for slot 0, so in
  PvP a slot 0 death runs `defeat()` and the full respawn, and a slot 1 death does nothing.
- `defeat()` (`respawn/RespawnSteps.kt:20-41`) destroys every other player, stops the music, removes a life, starts
  the black fade and `TURN_CONTROLLER_OFF`. Later steps reset the level (`resetLevelForRespawn`, `:600-630`) and send
  `LEVEL_RESET`.
- `SlotRespawn` (`respawn/SlotRespawn.kt`) is the co-op per-slot respawn: on a death with another player present, it
  removes a life (game over by lives sets the dead slot to `POST_PLAY`), waits `DELAY` (1 s) and for `hold()`, then
  asks `PartnerSpot` for a safe spot next to a live partner in view, spawns there, beams in with locks, then
  `IN_PLAY`. With no other player present, it returns `true`, and the screen runs the level defeat.
- The slot spawn points: `playerCheckpointMan.currentSpawnProps` (slot 0) and `slotSpawnProps(slot, props)`
  (`spawns/PlayerSlotSpawns.kt:11`) for the other slots (the `player_2` property, or 1 tile to the side).
- Lives: one shared count in `GameState` (`state/GameState.kt:285-291`).
- Spawn protection today: the beam-in damage lock only. `DamageRecoveryComponent` (`entities/state/`) gives 1.5 s of
  invincibility with a flash after a hit; `MegaPlayer.invincible` reads it (`players/MegaPlayer.kt:73-79`).
- The death fade draws only when no player is alive (`MegaLevelScreen.kt:977`), and is `init()`ed only by `defeat()`.

## In scope

- Replace `SessionRules.slotRespawn: Boolean` with a typed rule of where a slot respawns: off (single), next to a
  partner (co-op, as today), at a spawn point (PvP).
- `SlotRespawn`: the spawn place comes from the rule. The PvP form never returns `true` from `onDeath` (never the
  level defeat), also for a slot 0 death and for 2 deaths in one frame.
- PvP: the spawn point, the life cost and the spawn protection, as the owner decides (D1 to D3 in the brief).
- Unit tests for `SlotRespawn` in the PvP form.

## Out of scope

- Score, kills, match end, time limit, teams (later `109` branch).
- Lives per slot (`O2-R9`, `047`).
- New spawn map objects (more than the 2 slot spawns of a room).
- Wrap-aware distance for spawn choice.

## Affected areas and risks

- `SessionRules.kt`, `SlotRespawn.kt`, `MegaLevelScreen.kt` (`SlotRespawn` build and the spawn supplier), maybe
  `DamageRecoveryComponent.kt` (D3), `PartnerSpot.kt` (no change expected). Tests: `SlotRespawnTest` (if any) or a
  new one.
- Risk: the co-op path must keep its exact behavior (life cost, partner spot, level defeat when the last one dies).
- Risk: a death during the level-start beam (`respawn.finished == false`) in PvP. Damage is locked then, but a pit is
  not. The PvP form must not start the routine there either.

## Overlap with other branches

Active worktrees: `077g` (Glacier Man), `078d`, `078e` (art), `060k` (frame edits), `compactor-level`,
`fix/projectiles-ignore-death`. None touches `screens/levels/respawn/` or `SessionRules`. `fix/projectiles-ignore-death`
may touch projectile death handling; to check before the merge.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | no effect | Uses the existing `SlotRespawn` update point. |
| `O1-R4` Typed state | helps | A typed respawn rule in place of a bool. |
| `O1-R5` Code only with a caller | no effect | Each new type has its caller in the screen. |
| `O2-R4` Stable order, slot order | no effect | `players.forEach` in slot order. |
| `O2-R5` No logic assumes one player | helps | A slot 0 death no longer ends the session for both. |
| `O2-R7` Gameplay not from rendering | no effect | |
| `O2-R9` Events and progress name a slot | no effect | Lives stay one count (D2). |
| `O5-R1` No per-frame allocation | no effect | Spawn props are built at a respawn only, not per frame. |
| `O6-R2` Named checks | helps | Unit tests for the PvP form. |
| `O8-R2` Capabilities, not character checks | no effect | |

## Decisions

Owner, 2026-11-20 (brief D1 to D3, all as recommended):

- D1: a dead slot respawns at its own slot spawn point (checkpoint props for slot 0, `slotSpawnProps` for slot 1).
- D2: a PvP death costs no life; no game over in PvP.
- D3: after the beam-in, the 1.5 s damage-recovery flash and invincibility.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new `SlotRespawn` PvP tests.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Experimental first, then controls; simplest setup first.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_109`, room `arena`, `--players 2 --mode pvp` | Slot 1 kills slot 0: no black fade, no music stop, slot 1 stays; slot 0 beams in at its spawn after the delay. |
| same | Slot 0 kills slot 1: slot 1 beams in at its spawn after the delay. |
| same | Repeat kills past the life count: no game over. |
| `REFACTOR_TEST_109`, room `arena`, 1 player | No change: (no death source in the room; the room loads and plays.) |
| `REFACTOR_TEST_047`, room `slot_1_death`, `--players 2` | Co-op: slot 1 dies and respawns next to slot 0 as before. |
| `REFACTOR_TEST_047`, room `group_pit`, `--players 2` | Co-op: both fall; the level resets as before. |

## Behaviors to check

- Single player death and level reset (any level): unchanged.

## Intended changes

- In a PvP session, a death respawns only the dead slot. The level-defeat routine never runs.
