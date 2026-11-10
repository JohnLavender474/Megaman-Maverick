# Plan: mega-refactor-146a-player-marker

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

Each step compiles and its tests pass before the next step starts. Gradle runs: check the free memory first (no run
under about 2 GiB available) and stop own idle daemons after the step.

## Steps

### 1. Engine: `IPlayer` and the typed `Players`

- Changes: `engine/.../players/IPlayer.kt` (`interface IPlayer : IGameEntity` with `slot`),
  `PlayerNotRegisteredException.kt` next to it. `IPlayers<out E : IPlayer>`, the inline helpers `<E : IPlayer>`,
  `Players<E : IPlayer>`. Engine test doubles (`ScoredEntity` and the `PlayersTest` entities) implement `IPlayer`.
- Tests first: `IPlayer.slot` throws before registration and after unregistration, and gives the slot after
  registration.
- Check: `./gradlew engine:test`.
- Done: yes (2026-11-10)

### 2. Core: the player classes and the players service

- Changes: `Megaman` and `MegamanX` implement `IPlayer`. `MegaPlayers : Players<IPlayer>` and its helpers
  (`targetFor`, `isLive`, `firstLive`, `forEachLive`, `anyLive`) on `IPlayer`. `asPlayer`, `rootPlayer` and
  `targetPlayer` return `IPlayer`. `Event.isDeathOf(player: IPlayer)`. `PlayerRoster` holds the roster as `IPlayer`.
- Check: `./gradlew core:compileKotlin` shows the call sites that must change (input for step 3).
- Done: yes (2026-11-10)

### 3. Core: the call sites

- Changes: the APIs that take a player as `MegaGameEntity` take `IPlayer` (the 14 groups in `SCOPE.md`, for example
  `ItemEntity.contactWithPlayer`, `AbstractBoss.onPlayerDamaged`, the respawn and camera classes, the screen `player`
  getters). `playerSlot!!` on an `IPlayer` becomes `slot`. Where a site needs a `MegaGameEntity` member, take the
  narrowest fix (widen the parameter; a `where T : MegaGameEntity, T : IPlayer` bound for a function) and write it in
  the process log.
- Check: `./gradlew core:compileKotlin lwjgl3:jar`.
- Done: yes (2026-11-10)

### 4. Core tests

- Changes: the player test doubles (`TestHealthPlayer`, `SecondPlayerEntity`, `TestPlayerEntity`, `TestPlayer`,
  `FieldsTestPlayerEntity`, `RulesTestEntity`, `SitePlayer`, `Walker`) implement `IPlayer`. The `mockk` player in
  `LevelRoomHandlerTest` becomes a real double. New tests: `asPlayer` is `null` for the roster character that is not in
  a slot; `Event.isDeathOf` is true only for the player of the event slot.
- Check: `./gradlew core:test core:smokeTest`.
- Done: yes (2026-11-10)

### 5. Docs

- Changes: the root `CLAUDE.md` lines on the player and `targetPlayer`; the `players/` README if the branch has one
  by then (`060a` adds one). Process log.
- Check: read the changed lines against the code.
- Done: yes (2026-11-10)

### 6. Review before the play test

- Changes: none, unless the review finds problems. Choose the `X6_INTRO` room and the real boss stage room for the
  contract.
- Check: the review agents, as in `docs/mega-refactor/CLAUDE.md`. Then the play checks and Diffity.
- Done: review yes (2026-11-10); play test and Diffity open

## Tag steps (scope change 2026-11-10)

### 7. `TagsComponent` and the accessors

- Changes: `core/.../entities/state/TagsComponent.kt` (ordered tags, fixed at construction, `reset()` keeps them);
  `MegaGameEntity.init()` adds `defineTagsComponent()`, default `this::class.simpleName`; `hasTag`, `hasAnyTag`,
  `tags` (throw if the component is missing); `MegaGameEntities` adds and removes under each tag.
- Tests first: runtime class name for a subclass; tags survive destroy and spawn; index per tag; missing component
  throws.
- Check: `./gradlew core:test`.
- Done: yes (2026-11-10)

### 8. Readers, overrides and `ITaggable`

- Changes: the 41 `getTag()` readers move to the accessors, own `TAG` or the tag list (see D7). The 148 duplicate
  overrides go; the 7 that differ and the 4 bases override `defineTagsComponent()`. `ITaggable` leaves
  `IMegaGameEntity`, `IProjectileEntity` and the engine; test stubs that override `getTag()` change.
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`.
- Done: yes (2026-11-10)

### 9. Tag golden test

- Changes: a smoke test that builds every concrete entity class and checks its tags against the investigation
  (`temp/mega-refactor-146a-player-marker/tags/result.json`): own name or the different `TAG`, then the base name.
- Check: `./gradlew core:smokeTest`; a mutation (drop the base tag in `Block`) fails it.
- Done: yes (2026-11-10)

### 10. Docs and a second review before the play test

- Changes: `CLAUDE.md` (conventions: `TAG` and the tags component); process log. Review agents for the tag diff and
  the 2 new play checks; then Diffity.
- Check: as in step 6.
- Done: review yes (2026-11-10); play test and Diffity open

## Spawn flow steps (scope change 2026-11-10)

### 11. Engine: `init()` before `canSpawn`

- Changes: `GameEngine.spawn()` inits an uninitialized entity before `canSpawn`; `spawnNow` drops its init; the
  factory preload sets `initialized`. Engine tests first: `canSpawn` sees an initialized entity; a rejected entity is
  initialized once and not again at its next spawn.
- Check: `./gradlew engine:test core:test core:smokeTest`.
- Done: yes (2026-11-10)

### 12. The `canSpawn` contract fixes

- Changes: `HeartTank`, `HealthTank` parse in `onSpawn`; `TellySaucer` per D10; tests for each; `CLAUDE.md`.
- Check: `./gradlew core:test core:smokeTest lwjgl3:jar`.
- Done: yes (2026-11-10)

### 13. Review before the play test (third)

- Changes: review agents for steps 11-12 and the play checks (a tank room, a `TellySaucer` room in 1 and 2 players).
- Done: review yes (2026-11-10); play test and Diffity open
