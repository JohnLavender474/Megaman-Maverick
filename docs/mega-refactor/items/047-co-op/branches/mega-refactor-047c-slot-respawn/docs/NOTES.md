# Notes: mega-refactor-047c-slot-respawn

## Scan (2026-11-09, 2 read-only agents)

Paths under `core/src/main/kotlin/com/megaman/maverick/game/`.

### Death and respawn path

- `AbstractHealthEntity.onHealthDepleted` (`:113-128`) destroys; `Megaman.onHealthDepleted` (`Megaman.kt:332-339`)
  adds the explosion ring. `killPlayerIfOutsideLevel` (`PlayerLifecycle.kt:104-109`) destroys with no depletion.
- `engine.destroy` queues; `destroyNow` resets every component, then `onDestroy` (`GameEngine.kt:186-197`).
- `playerOnDestroy` (`PlayerLifecycle.kt:69-73`): removes the listener; `PLAYER_JUST_DIED` with `PlayerEventKeys.SLOT`.
  Fires on every destroy (`TODO` `:68`, `119`), also on `defeat()`'s destroy of slot 1 and on `engine.reset`.
- `MegaLevelScreen.onEvent` (`:605-618`): slot 0 only → `respawn.startAtDeath()`. `TODO` `:610-613` lists 3 options.
- Routine (`respawn/`): `RespawnContext.lead` = slot 0 (`RespawnContext.kt:16-19`); `AFTER_DEFEAT` =
  `WaitAndFadeOut` (`DeathFadeOut.ON_DEATH_DELAY` 4 s), `CheckLives`, `AwaitEventsRun`, `BeginRespawn`,
  `BeamUntilEventsRun`, `ResetLevel`, `BeamIn`, `Ready`. `defeat` (`RespawnSteps.kt:21-41`) destroys the other slots
  (`:25`), stops the music, removes 1 life (`:30`).
- `resetLevelForRespawn` (`MegaLevelScreen.kt:556-583`): camera reset, fights cancelled, all systems on, enemies
  destroyed, spawners re-armed, slot 0 at the checkpoint, other slots at `slotSpawnProps`.
- A slot 1 death alone: unspawned until the next reset. `engine.spawn` works on `UNSPAWNED` and on `DESTROY_QUEUED`
  (`GameEngine.kt:82-117`). `PlayPhaseComponent.reset()` does nothing; `isLive` needs `liveNow` and `IN_PLAY`
  (`MegaPlayers.kt:56`).
- `playerSpawnStart` (`PlayerLifecycle.kt:40-62`): listener, body bottom centre, velocity 0, facing, session gravity
  from the spawn `direction` (`:52-55`), full health, clears the `MOVEMENT` and `DAMAGE` locks.
- `beginRespawn` (`RespawnSteps.kt:43-61`) and `Ready` (`:145-165`) set `PRE_PLAY` / `IN_PLAY`, the `SPAWN` locks and
  gravity for all slots; `Ready` sends `PLAYER_READY` (lead slot) and the beam sound.
- `BeamInView` (`respawn/BeamInView.kt`): fade-in, "READY" blink, pre-beam 1 s, beam down 0.5 s, landing 0.35 s; slot 0
  only (`TODO` `:48`). Not drawn while not `IN_PLAY` (`megaman/components/SpritesComponent.kt:106`, `:137`).
- Lives: `GameState.kt:268-284`, one pool, slot unused.

### Listeners

- `PLAYER_JUST_DIED`: `GauntletMaster.kt:168-171` → `resetGauntlet` (`:190-201`): drops its children untracked, wave
  0, `INACTIVE`; restarts only on a room entry. With no level reset the room soft-locks. `SpiderWeb.kt:136-138`
  (`047e`). `PreciousGem.kt:215`, `PreciousGemCluster.kt:109`: any player death destroys any player's guard.
- `PLAYER_DONE_DYIN` (lead only): `DrWily.kt:121`, `LaserBeamer.kt:180`, `QuestionBlock.kt:126-130`,
  `RisingLavaRiver.kt:225`.
- `PLAYER_READY` (lead only): `DarknessV2.kt:247`, `DarknessV4.kt:332`, `RisingLavaRiver.kt:221`,
  `SpawnersLayerBuilder.kt:128`, `:149` (room spawners). A slot respawn must not send these 2 events.
- `players.all { landed }` boss intros (`GutsTank.kt:275`, `:329`, `ElecDevil.kt:335`, `WilyFinalBoss.kt:439`) and
  `Fight.leadPlayerAlive` (`fights/Fight.kt:24-26`): `047e`.
- `CameraViewEdges` (`:197-203`, `:243-312`): `hasFreePartner`, the `047c` `TODO` (`:252`), the warp search.
- `PlayersFocus.inGroup` (`:52`): a dead player is out of the focus. `CameraRoomExits` (`:86-89`, `:123`): with fewer
  than 2 live players, no walls.

### Bugs

- `2026-11-02-weapon-spawned-sets-survive-death.md`: `MegamanWeaponsHandler.reset()` (`:156`) has no caller; stale
  entries are used at `:334-349` (moon scythes), `:373-390` (precious guard), `:523-537` (`onChangeWeapon`). The issue's
  anchor `Megaman.kt:338` is stale. Safer fix: `reset()` in `Megaman.onDestroy` after `playerOnDestroy()`.
- `2026-11-02-throw-axe-no-live-check.md`: `delayRun` at `:660` (uncancelled), `throwAxe` at `:1006`. Fix: a live check
  at the top of `throwAxe`.

## Overlap with other branches (2026-11-09)

- `134a`: `MegaLevelScreen.kt`, `PlayersFocus.kt`, `Megaman.kt` (uncommitted). A respawn is a teleport; `134b` snaps.
- `081f`: `PlayerDamageNegotiator.kt` only.
- `compactor-level`: `LevelDefinition.kt` only.

## Decisions

See `SCOPE.md`.
