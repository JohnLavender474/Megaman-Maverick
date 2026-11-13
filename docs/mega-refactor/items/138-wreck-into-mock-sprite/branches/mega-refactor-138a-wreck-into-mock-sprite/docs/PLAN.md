# Plan: mega-refactor-138a-wreck-into-mock-sprite

Each step ends with its check. No game launch (owner rule).

1. **Rename and rewrite.** `git mv` `MockSpriteEntity.kt` to `MapSprite.kt`; the class reads `ConstKeys.ATLAS` and
   `ConstKeys.REGION` (fail fast on a missing atlas, region or a region not in the atlas), sets the sprite bounds to
   the object bounds, `DrawingPriority(BACKGROUND, 1)`, no flip, no animation; the old mock code goes (D2, D3).
   Check: compiles.
2. **Map.** In `X6Intro_16x14_v1.tmx`, objects 218 and 219 become `MapSprite` with the rects drawn today
   (`0, 1081, 96x72` and `1868, 1014, 36x28`) and `atlas=Enemies2.txt`, `region=Wreck/wreck_a` / `Wreck/wreck_b`; the
   `key` property goes (D1). Check: a script compares the new rects with the `TickFrames.place` result.
3. **Remove `Wreck`.** Delete `Wreck.kt`; update `core/src/smokeTest/resources/entity-tags.txt`. Check: no reference
   left (grep).
4. **Test.** `MapSpriteTest`: the sprite bounds equal the object bounds, the region, no flip, `BACKGROUND` 1, and a
   throw for a missing region. Check: it passes, and fails when the priority or the bounds are broken.
5. **Checks.** `./gradlew engine:test core:test core:smokeTest lwjgl3:jar`.
6. **Review before the play test**, then the play check and Diffity.
