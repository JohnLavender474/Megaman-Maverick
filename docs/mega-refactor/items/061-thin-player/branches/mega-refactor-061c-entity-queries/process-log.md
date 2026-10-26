# Process log: mega-refactor-061c-entity-queries

## Status

- State: active
- Goal: a player is an entity with a `PlayerComponent(slot)`, not a view object; callers ask the entity through entity-level query functions; `MegaPlayer` and `MegaPlayerAccess.kt` go
- Done: planning (scope, contract, plan approved); step 1
- Next: step 2 (baseline)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-26

- No branch exists yet. The folder was prepared on `master` (at `10927055f`).
- Scan: every function in `players/MegaPlayerAccess.kt` and its callers by receiver: `targetPlayer` (147 files; 423
  `body`, 23 `direction`, 15 `dead`, about 10 others; 6 uses need the view: `slot` ×2, `input` ×2, `present` ×1,
  `entity` ×1 plus the `inPlay` reads that already go through `entity`), the screens' and handlers' `player` (about 106
  uses), `players` loops (`it`, about 77). Name checks: `target` is declared in 36 files (rejected); `targetEntity` in
  none; the members `body`, `facing`, `direction` are declared in 354, 148 and 108 files. Draft `docs/SCOPE.md` written.
- The owner asked whether a component could replace the view class; the answer (from the `029` research) was yes,
  and the owner asked to fold it into this branch. `docs/SCOPE.md` rewritten: engine `PlayerComponent(slot)` and an
  entity registry in place of `PlayerView`/`MegaPlayer`; entity-level query functions; `targetPlayer` and `asPlayer`
  return the entity (proposal). Extra scan: `PlayerView` is used only by the engine players package, `MegaPlayer` and
  `PlayersTest`; the view type appears in 25 core files; `asPlayer` has 150 uses in 64 files; `players.any` 70,
  `forEach` 28, `all` 11.
- The owner approved `docs/SCOPE.md` with the four proposals (names kept, explicit `as Megaman` casts, engine, one
  branch). Draft `docs/CONTRACT.md` written; most manual checks use the `REFACTOR_TEST_029` rooms (`movement`,
  `enemy_aim`, `enemy_chase`, `enemy_drop`, `enemy_contact`, `enemy_magfly`, `pickups`, `heart`, `lives`,
  `hit_by_player`, `push_flip`, `gravity_switch`, `player_death`, `stun`) and the three `REFACTOR_TEST_031` rooms.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (7 steps; steps 3 and 4 only add the query
  functions, step 5 is the switch).
- The owner approved `docs/PLAN.md`. Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree
  created.
