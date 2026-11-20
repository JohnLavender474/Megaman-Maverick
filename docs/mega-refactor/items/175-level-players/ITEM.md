# 175: `LevelPlayers`: player spawns, deaths and slot respawn out of `MegaLevelScreen`

- Status: idea
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: none
- Related: `013`, `038`, `047`, `109`, `119`, `130`
- Branches: none yet

## Goal

`MegaLevelScreen` holds the player spawn and death logic: the checkpoint spawn props of each slot, the spawn of all
slots at a level reset, the build of `SlotRespawn` from the session's `SlotRespawnRule`, and the death dispatch of
`PLAYER_JUST_DIED` (slot respawn or the level routine). Each new mode adds to it (`047` co-op, `109b` PvP). Move it into
one class, so the screen stays a level orchestrator and a new respawn rule changes one file.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `LevelPlayers` in `screens/levels/respawn/`. It owns:
  - the spawn props of a slot (checkpoint props for slot 0, `slotSpawnProps` for the other slots);
  - the spawn of all slots at a level reset;
  - the `SlotRespawn` build from `SessionRules.slotRespawn`;
  - the death dispatch, which starts a slot respawn or `RespawnRoutine.startAtDeath()`.
- The screen keeps one call for a death and one call for the spawn of all slots.
- No dependency: it can merge before `013`. Then the `013` level flow drives `LevelPlayers` in place of the screen.

```kotlin
class LevelPlayers(services, rule, checkpoints: PlayerCheckpointManager, respawn: RespawnRoutine, slotRespawn: SlotRespawn?) {
    fun spawnProps(slot: PlayerSlot): Properties
    fun spawnAll()
    fun onDeath(slot: PlayerSlot)
}
```

Risks:

- `resetLevelForRespawn()` mixes the player spawns with the camera and enemy resets. The order of these steps must stay
  the same.
- `130` also edits `resetLevelForRespawn()` (the camera resets). The branch that merges second takes in the other.

## Owner's input

2026-11-20 (session of `109b`): the owner asked whether an item already moves the spawn and respawn logic out of
`MegaLevelScreen`, "so that `MegaLevelScreen` stays clean and focused only on level orchestration". None did (`013`
and `130` cover only parts). Owner accepted the recommendation: a new item, not a wider `013`.

## Open questions

- Does `LevelPlayers` also own the `PlayerCheckpointManager`, or only read it?
- Name: `LevelPlayers` or `PlayerSpawns`.

## Sources

- `NOTES.md` in this folder.
