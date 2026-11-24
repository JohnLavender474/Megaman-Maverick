---
status: open
verified: question
area: core
gameplay-change: yes
found-by: owner (2026-10-30)
related-items: [042]
---

# Make the `BigAssMaverickRobot` hand motion less uniform

This issue is a design refinement of the `INTRO_STAGE` boss, not a bug. The owner says that the boss is good, but that
its hands spin in constant, perfect circles. This does not look natural and does not give much diversity. The owner
decides the new motion.

## Where

- Hand: `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/BigAssMaverickRobotHand.kt`. The `ROTATE` state
  is at `:306-313`. It is the only state that moves the hand on the circle.
- Boss: `core/src/main/kotlin/com/megaman/maverick/game/entities/bosses/BigAssMaverickRobot.kt`. The constants
  `HAND_RADIUS`, `HAND_ROTATION_MIN_SPEED` and `HAND_ROTATION_MAX_SPEED` are at `:120-122`. The speed suppliers are at
  `:342-346` (left) and `:378-382` (right).
- Motion: `engine/src/main/kotlin/com/maverick/game/engine/motion/RotatingLine.kt`. It is shared with 6 other entities
  (`SigmaRatClaw`, `RotationAnchor`, `PetitDevil`, `FireballBar`, `LaserBeamer`, `Saw`), so a change for this boss must
  not change its behavior.
- Map: `assets/tiled_maps/tmx/IntroStage_v3.tmx` (the live map of `LevelDefinition.INTRO_STAGE`). The boss room is
  `boss_room` (object 330). The boss is object 335. The hand circle centers are objects 342 (`Left`) and 344 (`Right`).

## What happens

Each hand has a `RotatingLine` with a fixed radius of 2 tiles (`HAND_RADIUS`). In the `ROTATE` state, the hand center
is set to the end of the line on each update.

- The path is always a circle. Only the angle changes.
- The speed depends only on the boss health: 160 degrees/s at full health and 256 degrees/s at no health
  (`HAND_ROTATION_MIN_SPEED` and `HAND_ROTATION_MAX_SPEED`, each multiplied by `ConstVals.PPM`). One turn takes 2.25 s
  at full health and about 1.41 s at no health. In one health state, the speed does not change during the turn.
- The direction never changes. The left hand starts at 180 degrees with a negative speed. The right hand starts at 0
  degrees with a positive speed.
- The two hands start as mirror images. They lose this only because a launched hand stops its `RotatingLine` while the
  other hand continues. Their speed stays the same.

Values from the map, by calculation (tiles above the floor top at y 1440):

| Value | Result |
|---|---|
| Distance between the two circle centers | 6 tiles |
| Height of the hand center on the circle | 2.5 to 6.5 tiles |
| Height of the hand block top on the circle | 3 to 7 tiles |
| Height of the head damage circle | 6 to 9 tiles |
| Smallest distance between the hand centers | 2 tiles |
| Smallest gap between the two hand blocks | 0.5 tiles |

## What should happen

The hands move in a way that looks less mechanical and gives more diversity. The form is not chosen. The owner chooses
it.

Possible directions (not chosen; the fix branch decides with the owner):

- A variable speed in each turn, for example slow at the top and fast at the bottom.
- A non-circular path, for example an ellipse, or a radius that grows and shrinks.
- Different values for each hand, so that the two hands do not stay mirror images.
- A pause or a change of direction at some points.
- A set of motion patterns. The boss changes the pattern over time or as its health decreases.

The motions `ArcMotion`, `Pendulum` and `SineWave` are also in `engine/.../motion/`. The fix branch checks if one of
them fits before it adds a new motion. A change to `RotatingLine` must keep it the same for the other 6 entities.

Limits for each direction:

1. **The hands must not come much nearer to each other.** `BigAssMaverickRobot` throws an `IllegalStateException` if
   Megaman stands on both hands when it selects the hand to launch (`BigAssMaverickRobot.kt:489-493`). A stand check
   uses his feet fixture, which is 0.5 tiles wide (`megaman/components/BodyComponent.kt:110-111`). The smallest block
   gap today is also 0.5 tiles, so there is no margin. A path that moves the hands nearer makes this crash possible.
2. **The hands are platforms.** The hand block is active only in `ROTATE` (`BigAssMaverickRobotHand.kt:387-391`). A new
   path changes when Megaman can stand on a hand and how high it takes him toward the head. Its height range must keep
   the head reachable. Fast changes of speed or direction can also throw him off the block. Neither is checked yet.
3. **The return must end on the path.** `launch()` stores the launch point as `returnTarget`
   (`BigAssMaverickRobotHand.kt:246`), and the `RETURN` state goes back to it (`:331-342`). This works today because
   the `RotatingLine` stops during `LAUNCH` and `RETURN`. A motion that continues during a launch makes the hand jump
   when it returns to `ROTATE`.
4. **The start point stays the first path point.** `MOVE_TO_ORIGIN` moves the hand to `rotationStartPos` (`:165`,
   `:276-288`), and the boss becomes ready only when both hands are in `ROTATE` (`BigAssMaverickRobot.kt:397-398`).
5. **The hand motion stops when the boss is not ready.** The `ROTATE` state moves the hand only when `owner.ready` is
   true and `betweenReadyAndEndBossSpawnEvent` is false (`BigAssMaverickRobotHand.kt:307`).

Must not change: the launch attack (`LAUNCH` and `RETURN`), the launch timing, the orb attacks, the hand damage rules,
the boss health and damage table, the spawn and defeat sequences, and the map.

## How to reproduce

From the main checkout:

```
./run-mega-man-maverick-alpha.sh --level INTRO_STAGE --room room12
```

`room12` holds the last player spawn before the boss. Go right into `boss_room`. When the boss is ready, watch the hands
between launches: each hand follows the same circle at the same speed.

## Notes

- This is a design refinement of one boss, the same type of work as
  [2026-10-29-refine-wily-final-boss-fight.md](2026-10-29-refine-wily-final-boss-fight.md). A `fix/<slug>` branch off
  `master` does it, with a short contract that says which direction the owner chose, what changes, and what must not
  change. If the work grows (for example new attacks or a new phase pattern), make it a small item instead.
- Item `077` does not fit: it is about new attacks for the 8 robot masters, and this boss is not one of them. Item `079`
  does not fit: it is about simple enemies. Item `042` does not fit: it is a refactor, so it must keep the gameplay. Its
  `LimbMotion` proposal does include the `LAUNCH` and `RETURN` code of this hand, so whichever of the two branches
  merges second must rebase on the other.
- The speed constants are in degrees/s, but the suppliers multiply them by `ConstVals.PPM`, a pixel scale. A branch that
  changes the speed must use the real values above, not the constants alone.
- The values in `What happens` come from reading the code and the map, not from a play test.
