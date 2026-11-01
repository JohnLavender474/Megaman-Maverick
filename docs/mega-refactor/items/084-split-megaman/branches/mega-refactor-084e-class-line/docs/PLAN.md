# Plan: mega-refactor-084e-class-line

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-31

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`. Line numbers are at `405cf0171`.

Each step is one commit. Each step ends with `./gradlew engine:test core:test lwjgl3:jar`, plus the check that the step names. Where a step changes a rule or adds an API, the test comes first, and it must fail for the expected reason before the code changes. A swap of imports across many files uses a script; other edits do not.

## Steps

### 1. Test room `cart`

- Changes: a subagent uses the `level-builder` skill to add the room `cart` to `assets/tiled_maps/tmx/RefactorTest084.tmx`: a floor, a `tiles` layer with a tile on each block cell, a player spawn, and a `Cart` in the `specials` layer. The room does not change the other rooms. Enough floor for the cart to move.
- Check: build, then `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_084 --room cart` on the baseline code. No exception.
- Done: yes

### 2. Dead code

- Changes: delete `spawningTimer` (`M/Megaman.kt:272,444,605`) and `MegamanValues.SPAWNING_DUR`; `wallSlideGravity` (`:193,208,262`) and `MegamanValues.WALL_SLIDE_GRAVITY`; `M/components/PointsComponent.kt`; the commented block at `:316-372`; the comment inside the call at `:373`.
- Check: the dead-code `grep` in `CONTRACT.md`, without the `controllerComponent` term (step 6 removes that field).
- Done: yes

### 3. `IPointsEntity` and `IAnimatedEntity`

- Changes: remove both from the class line. `forEachAnimator` at `:250` and `getAnimator` at `M/components/SpritesComponent.kt:189` use `require(AnimationsComponent::class)`.
- Check: the class line has 13 interfaces. The compiler confirms that `IHealthEntity` still gives `Megaman` its points members.
- Done: yes

### 4. `IBehaviorsEntity`

- Changes:
  1. Test first: in `EntityQueriesTest`, `resetBehavior` ends an active behavior and does nothing for an inactive behavior, a missing behavior or a missing component.
  2. Add `IGameEntity.resetBehavior(key)` to `E/entities/EntityQueries.kt`.
  3. Script: the 17 files in `M/` import the engine `isBehaviorActive` and `isAnyBehaviorActive` as each file needs them.
  4. `Megaman.kt:179,287` call the extension. `M/components/BodyComponent.kt:104-106` becomes `if (isBehaviorActive(behaviorType)) resetBehavior(behaviorType)`.
  5. Remove the interface and its member overrides from `Megaman`. Delete `IBehaviorsEntity.kt`.
- Check: `grep -rn 'IBehaviorsEntity' core/src engine/src` finds nothing. Each new import in `M/` names `com.maverick.game.engine.entities`, so no other extension of that name resolves. The class line has 12 interfaces.
- Done: yes

### 5. `isPlayer` in place of the markers

- Changes:
  1. Tests first. `GateRuleTest` and `EntityContractsTest` register the player. New tests: gate rule and cart rule with a player and a non-player; `MegaContactFilter` keeps a gate or cart contact with a player and drops it with no player. If the `UnderwaterFan` receiver runs with no assets, a test that a player is blown and another body is not. If it does not run with no assets, write this in the process log, and the play test covers it.
  2. `GateRule.kt:15` and `FeetCartRule.kt:21,33` use `isPlayer`, at the same lines.
  3. `MegaContactFilter.kt:110-116` uses `isPlayer`, at the same lines.
  4. `UnderwaterFan.kt:224` uses `isPlayer`. `ENTITIES_TO_BLOW` (`:84`) goes.
  5. Remove `IGateTrigger` and `ICartRider` from the class line, and delete both files.
- Check: `grep -rnE 'IBehaviorsEntity|IGateTrigger|ICartRider' core/src engine/src` finds nothing. The class line has 10 interfaces.
- Done: yes

### 6. `input`

- Changes: script: the 14 files import `com.maverick.game.engine.entities.input` in place of the `M/` import. Delete `M/MegamanInput.kt` and the field `controllerComponent` (`:122,311-312`).
- Check: the dead-code `grep` in `CONTRACT.md`, with all terms. `grep -rnE 'val Megaman\.input\b' core/src/main/kotlin` finds nothing.
- Done: yes

### 7. Fixture accessors

- Changes: delete the 7 `val Megaman.*Fixture` at `M/components/BodyComponent.kt:57-76`. Script: the 13 files that imported them, and `M/components/EffectComponents.kt`, import the versions in `C/entities/EntityQueries.kt`. `IGameEntity.bodyFixture` is not added: it has no caller.
- Check: the duplicates `grep` in `CONTRACT.md`.
- Done: yes

### 8. Sprite helpers

- Changes: `getSpritePriority` (`M/components/SpritesComponent.kt:95`) becomes `getMegamanSpritePriority(out)`, with no receiver. `shouldFlipSpriteX` and `shouldFlipSpriteY` (`:50-52`) go, and the callers read `facing == Facing.LEFT` and pass `false`. Remove the 4 casts at `EndLevelEventHandler.kt:104,179` and `PlayerSpawnEventHandler.kt:98`.
- Check: the casts `grep` in `CONTRACT.md` has 16 casts.
- Done: yes

### 9. Final checks

- Changes: none in code, unless a check fails.
- Check: all automated checks in `CONTRACT.md`. Review the full diff against the "Frame order" section. Build, then `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_084 --room cart`. Compare the diff with `master`, and merge `master` first if it changed the same files (`031e` is the known overlap). Then the owner's play test.
- Done: yes
