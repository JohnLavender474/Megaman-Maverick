# 092: Remove old entity versions and the version suffixes

- Status: idea
- Type: refactor
- Depends on: `084e`
- Related: `003`, `014`, `039`, `084`
- Branches: none yet

## Goal

Keep one class for each entity. Delete the old version, and give the version in use the plain name: `Cart`, not `CartV2`. The game then has no
class that a map author can pick by mistake, and no dead contact rule for an entity that has no play effect.

Gameplay does not change. The classes in `DESERT_MAN`, `REACTOR_MAN`, `WILY_STAGE_1` and the other stages are the same code under a new name.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Counts are in `NOTES.md`.

Delete and rename:

| Old class | Class in use | Change |
|---|---|---|
| `Cart` | `CartV2` | Delete `Cart`. Rename `CartV2` to `Cart`. |
| `TubeBeamer` | `TubeBeamerV2` | Delete `TubeBeamer` and `TubeBeam`, which only `TubeBeamer` spawns. Rename `TubeBeamerV2` to `TubeBeamer`. |
| `Darkness` | `DarknessV2` | Delete `Darkness`. Rename `DarknessV2` to `Darkness`. |
| `MegamanTrailSprite` (commented out) | `MegamanTrailSpriteV2` | Delete the old file. Rename `MegamanTrailSpriteV2` to `MegamanTrailSprite`. |

Delete only, because the class in use has the plain name:

- `SpikeBot_OLD`.
- `SniperJoe_OLD`. The `SwingingPlatform` branch for a `SniperJoe` and the `SNIPER_JOE` pool then use `SniperJoe`.

With the old `Cart`, this code goes:

- `FeetCartRule`, `FixtureType.CART`, `BodySense.TOUCHING_CART`, `ConstKeys.CART`, `ContactRuleIds.FEET_CART`.
- The `CART` check and the `FEET` table entry in `MegaContactFilter`, and the `FEET`/`CART` entry in `MegaContactRules.kt`.
- `SpecialsFactory.CART` and its pool.
- 171 lines of `core/src/test/resources/contact-rule-selection.txt`, and the tests that `084e` adds for this code (`FeetCartRuleTest`, the cart cases in
  `MegaContactFilterTest`).

The rename of `CartV2` has 3 traps. `NOTES.md` gives the facts:

1. The atlas `Specials1.txt` already has `Cart/idle` and `Cart/move`, and their pixels are not the same as `CartV2/idle` and `CartV2/move`. If `TAG`
   changes and the atlas does not, the cart art changes with no error.
2. The 2 `Cart` objects in `Test1.tmx` have no `facing` property. The current `CartV2.onSpawn` throws when `facing` is missing.
3. Each live map object must change, because the Tiled object name must equal the class name: 3 in `DesertMan_16x14_v4.tmx`, and 1 in
   `RefactorTest084.tmx` after `084e` merges.

The item does not change old map versions that `LevelDefinition` does not name. It does not change screens, or the assets of `039`.

This item covers the `084e` backlog candidate about the old `Cart` (in that branch's `docs/TODO.md`). The `084e` merge must not make a second item for it.

When the item is done, close the `TOUCHING_CART` and `ConstKeys.CART` part of `docs/issues/2026-10-22-unused-contact-hooks.md`.

## Owner's input

2026-10-31: delete the old `Cart.kt` and rename `CartV2.kt` to `Cart.kt`. "The fix can entail other similar entities as well."

The review answer in `039` also applies: "we'll need to identify what is truly dead versus "not used yet" or "not used because another class/file offers
a different version that the game currently uses"." So the owner reviews each deletion in this item before a branch starts.

## Open questions

1. **Which entities does the item include?** Recommendation: the 6 old classes in "Proposed scope". Each has no use in a real stage: 0 live map
   objects, except 2 `Cart` and 1 `Darkness` in `Test1.tmx`.
2. **Delete `DarknessV4`?** It has 0 objects in all 92 maps and 0 references, but it is 751 lines and it acts differently from `DarknessV2`. It did not get
   the darkness fixes of 2026-08-31. The `039` answer names experimentation as a reason to keep code. Recommendation: delete it. Git history keeps it, and
   a kept `DarknessV4` next to a plain `Darkness` shows no relation between the two.
3. **Delete `ReactorMan_OLD` and `MechaDragon_OLD`?** Both are commented out in full. `029c` kept them on purpose, with a `TODO` to check them before use.
   Recommendation: delete them, and the 2 commented lines in `BossesFactory`. A rename does not apply, because `ReactorMan` and `MechaDragon` have the plain
   names.
4. **How does the atlas change for `Cart`?** Recommendation: remove the regions `Cart`, `Cart/idle` and `Cart/move`, and rename `CartV2/idle` and `CartV2/move`
   to `Cart/idle` and `Cart/move` in `Specials1.txt`. Change the same names in `Specials1.tps` and the frames folder, so that a repack keeps them.
   `Specials1.png` does not change, so the art stays the same.
5. **What happens to the 2 `Cart` objects in `Test1.tmx`?** Recommendation: add `facing=right` to each. They then spawn as the solid cart. The map name
   does not change.
6. **Do the screens join this item?** `LevelSelectScreen` is dead and `LevelSelectScreenV2` is live. `RobotMasterIntroScreen` is live and
   `RobotMasterIntroScreenV2` is dead. Recommendation: no. They are not entities, and `039` reviews dead code. Add them to `039`.
7. **Is the `AbstractEnemy.kt:189` check a bug?** `lastDamager != CartV2` compares with the `CartV2` companion object, so it is always true. An enemy that a
   cart kills can drop an item. Recommendation: the rename keeps this behavior, and the owner decides if it goes in `docs/issues/`. A fix changes gameplay,
   so it does not go in this item.
8. **Merge target?** Recommendation: directly into `master`, because each change is safe to ship alone. Use one branch for the carts and one for the other
   classes, so that the contact rule change has its own review.

## Sources

- `NOTES.md` in this folder.
- `items/084-split-megaman/branches/mega-refactor-084e-class-line/docs/NOTES.md`, "The two carts" (on the branch until it merges).
