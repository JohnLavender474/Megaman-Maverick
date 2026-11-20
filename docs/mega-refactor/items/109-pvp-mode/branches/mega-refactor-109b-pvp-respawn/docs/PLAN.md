# Plan: mega-refactor-109b-pvp-respawn

Owner answers (2026-11-20): D1 own slot spawn, D2 no life cost, D3 1.5 s recovery flash. See `SCOPE.md`.

New API and its first caller in the branch:

| API | First caller |
|---|---|
| `SlotRespawnRule` (`OFF`, `NEAR_PARTNER`, `AT_SPAWN`) in `state/SessionRules.kt` | `MegaLevelScreen` (build of `SlotRespawn`), `SlotRespawn.onDeath` |
| `SlotRespawn` spawn-props supplier `(PlayerSlot) -> Properties` | `MegaLevelScreen` (checkpoint props for slot 0, `slotSpawnProps` for other slots) |
| `DamageRecoveryComponent.start()` | `SlotRespawn` beam end, `AT_SPAWN` form |

## Steps

### 1. Typed respawn rule

- Changes: replace `SessionRules.slotRespawn: Boolean` with `slotRespawn: SlotRespawnRule` (default `OFF`; `COOP`
  `NEAR_PARTNER`; `PVP` `AT_SPAWN`). The rule says whether a slot death costs a life (`NEAR_PARTNER` yes, `AT_SPAWN`
  no) and whether the beam end starts the recovery flash (`AT_SPAWN` only). `MegaLevelScreen` builds `SlotRespawn`
  when the rule is not `OFF` and a slot 1 exists, and passes the rule and the spawn-props supplier.
- Check: `core:test` compiles; co-op tests in `SlotRespawnTest` pass unchanged.
- Done: yes

### 2. `AT_SPAWN` form of `SlotRespawn`

- Changes: `onDeath` with `AT_SPAWN`: always start the slot respawn and return `false` (also during the level-start
  beam and the respawn routine; `defeatRunning` cannot be true in PvP). No life cost. `waitForSpot` with `AT_SPAWN`:
  after `DELAY` and not `hold()`, spawn with the supplier's props; no partner needed (both slots dead at once
  respawn each on its own). `NEAR_PARTNER` keeps the current code path.
- Check: unit tests of step 4.
- Done: yes

### 3. Spawn protection (D3)

- Changes: `DamageRecoveryComponent.start()` resets the recovery timer (the flash and `invincible` follow from it).
  `SlotRespawn.beamIn` calls it for `AT_SPAWN`.
- Check: unit test: after the beam end, the player is `invincible` while the recovery timer runs.
- Done: yes

### 4. Tests

- Changes: `SlotRespawnTest` cases for `AT_SPAWN`: a slot 0 death returns `false` and starts the slot respawn; a death
  while the routine runs returns `false`; 2 deaths in one frame both respawn with no partner; no life removed; the
  spawn uses the supplier's props; recovery starts at the beam end. Co-op cases stay.
- Check: `./gradlew core:test --tests 'com.megaman.maverick.game.screens.levels.respawn.SlotRespawnTest'`; one
  batched mutation run (see `docs/ways-of-working/git.md`).
- Done: yes

### 5. README

- Changes: `screens/levels/respawn/README.md` "Status": the slot respawn has a rule per session mode.
- Check: read.
- Done: yes

### 6. Automated checks and review before the play test

- Changes: none.
- Check: the contract automated checks; one Sonnet review subagent; play scripts in `temp/play/`; Diffity.
- Done: yes
