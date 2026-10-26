# Process log: mega-refactor-061c-entity-queries

## Status

- State: active
- Goal: a player is an entity with a `PlayerComponent(slot)`, not a view object; callers ask the entity through entity-level query functions; `MegaPlayer` and `MegaPlayerAccess.kt` go
- Done: planning (scope, contract, plan approved); steps 1 to 6
- Next: step 7 (the owner's play test and the merge)
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
- Step 2 (baseline, worktree on `5e07e7a7e`): clean build passes; 501 engine and 167 core tests, 0 failures; 110
  warnings.
- Step 3: engine `entities/EntityQueries.kt` (on `IGameEntity`: `body` requires `BodyComponent`; `hasBody`; `facing`
  and `direction` read `IFaceable`/`IDirectional` and fail with the part's name; `isBehaviorActive`,
  `isAnyBehaviorActive` (`false` without `BehaviorsComponent`); `input`, the entity's `ControllerComponent` source or
  `RELEASED`, with no nullable step). Engine `players/PlayerComponent.kt` (`PlayerComponent(slot)`, `playerSlot`,
  `isPlayer`). Tests: `EntityQueriesTest` (4), `PlayerComponentTest` (2). `javap`: `input` calls
  `InputSource.read()` and returns a `long`, no `box-impl`. Build passes; 507 engine and 167 core tests, 0 failures.
  The callers come in step 5.
- Step 4: core `entities/EntityQueries.kt` (on `IGameEntity`: `focus` (needed), `invincible` (`true` without
  `IDamageable`), `damaged` (`false` without `AbstractHealthEntity`), `healthRatio`, `currentHealth`, `maxHealth`,
  `translateHealth`, `depleteHealth` (needed: `IHealthEntity`), `shooting` (Megaman-only, with the `024` `TODO`), the
  six fixture getters); `lightOrNull` in `players/PlayerLightComponent.kt` and `onGravityDeviceSwitch` in
  `players/GravityDeviceComponent.kt`. Tests: core `EntityQueriesTest` (3). Build passes; 507 engine and 170 core
  tests, 0 failures. The callers come in step 5.
- Step 5 (the switch): engine `Players<E : IGameEntity>` is a slot → entity registry (`register` adds
  `PlayerComponent(slot)` and returns the entity, `unregister` removes the component, `get`, the loops over entities in
  slot order, `registeredSlots`); `IPlayers<out E : IGameEntity>`; `PlayerView`, `viewOf` and `slotOf` go. Core
  `MegaPlayers : Players<MegaGameEntity>` (plan detail, no approval needed: the registry is typed by its entity, so
  `targetPlayer`, `asPlayer` and the loops give a `MegaGameEntity` and `dead` stays a plain member read); `register`
  calls `super.register` first, then binds the input source (a failed registration changes nothing); `targetFor`
  returns the entity; `PlayerDamaged.player` is the entity. `MegaPlayer`, `MegaPlayerAccess.kt`, the core
  `PlayerComponent` and the `megaman` and `shooting` accessors go; `asPlayer` = the entity if it has a
  `PlayerComponent`; `targetPlayer` = the entity. Callers: a script (scratchpad `switch_imports.py`) moved the imports
  of the forwarding functions to the engine or core query files and renamed the `MegaPlayer` type to `MegaGameEntity`
  (189 files); a second script, on the 32 files the compiler still reported, dropped the `.entity` hops (and the casts
  they made redundant in `DarknessV3`/`V4`), turned `.megaman` into `(x as Megaman)` (19 uses), `.slot` and
  `players.slotOf(this)` into `playerSlot`, and `present` into `spawned`; the compiler-driven import script added the
  rest. The engine `input` import that the script put in `Megaman.kt` was removed, so Megaman keeps his own cached
  `input`. Tests: engine `PlayersTest` rewritten for the registry; `MegaPlayersTest` keeps the registry's own cases
  (registration, `asPlayer`, an occupied slot leaves the entity unbound, input binding, `targetFor`, the damage
  signal, world gravity); `PlayerItemRuleTest` mocks `hasComponent`. The added imports moved to their sorted place
  (185 files). One-for-one (scratchpad script over `git diff master`): 85 code lines match after undoing the renames;
  the other changed lines are the registry, the expected `DarknessV3`/`V4` and `slotOf` lines, and new or deleted
  files. Clean build passes; 505 engine and 160 core tests, 0 failures; 110 warnings, the same set as the baseline.
  Greps: no `MegaPlayer`, `PlayerView`, `viewOf`, `MegaPlayerAccess` or `megaman` accessor; `javap`: no
  `InputFrame.box-impl` in `input`. Launches of `REFACTOR_TEST_029` `movement`, `enemy_aim`, `pickups`,
  `INTRO_STAGE --room room12` and `REFACTOR_TEST_031 --room pipe_portal` ran with no exception until the timeout.
- Step 6, automated contract checks: the clean build of step 5 (counts, warnings, jar); `contact-rule-selection.txt`
  unchanged; `PlayersTest`, `PlayerComponentTest`, both `EntityQueriesTest`, `MegaPlayersTest` pass; the greps,
  `javap` and the one-for-one check as above. Waiting for the owner's play test.
- Step 7: the owner's play test passes (all manual checks in `docs/CONTRACT.md`, with `DESERT_MAN --room room6` for the
  dark room light, the first `DarknessV2` room with a player spawn). Waiting for the owner's merge approval.
