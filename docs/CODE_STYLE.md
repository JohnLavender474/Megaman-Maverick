# Code style

The rules for how Kotlin code in `engine/` and `core/` looks and is shaped. Read this file before you write or
review code. The review before a play test (`docs/mega-refactor/CLAUDE.md`) checks the diff against it.

Game conventions (entity `TAG`s, tags, texture regions, `props`, `ConstVals.PPM`, fixtures, logging) stay in the repo
`CLAUDE.md`, under "Conventions". Rules for refactor branches (an API needs a caller, a `TODO` for a deferred decision,
separate concerns) are in `docs/mega-refactor/CLAUDE.md`, under "During the branch".

Old code does not follow every rule. Apply the rules to new and changed code; do not reformat untouched code only for
them.

## Types and collections

- **LibGDX collections** for fields, parameters, return types and data types: `Array`, `ObjectMap`, `OrderedMap`,
  `ObjectSet`, not Kotlin's `List`, `Map` or `Set`. Helpers: `gdxArrayOf`, `objectMapOf`, `objectSetOf`,
  `orderedMapOf`.

  ```kotlin
  class SabreParams(val moves: ObjectMap<SabreState, Array<SabreMove>>)   // not Map<SabreState, List<SabreMove>>
  ```

- **One shape for every case.** When a value can have one or many entries for some owners, make it a collection for
  all of them, so that one rule handles each case (a list of 1 move, not a single move next to a list of moves).

## Calls

- **Named arguments for a `null` or a literal boolean**, where the meaning is not clear from the call.

  ```kotlin
  playerChargeComponent(this, halfTime = null, fullTime = TIME_TO_FULL_CHARGE)   // not (this, null, ...)
  requestToPlaySound(SoundAsset.WHIP, loop = false)
  ```

- **One entry point per interface method.** Do not add an overload or a second method next to it to pass one more
  value (for example a `draw(batch, offset)` next to `override fun draw(batch)`). Put the value in a field of the
  object, which the caller sets before the call (as `Background.cameraDrawOffset` and `GameSprite.drawOffsetX`), or
  change the interface for all of its implementations. Then each caller goes through the same contract, and a later
  change cannot miss a path.

## Errors

- **Fail fast.** A state that only a mistake can cause (bad map data, a wrong label, a missing entity) throws at once;
  do not fall back to a default with `as?` or a silent `return`. Where the raw error would be unclear (for example a
  bare `ClassCastException`), catch it and throw a custom exception made for that case. Its message says what is
  wrong and how to fix it, and the original error is its `cause`. Put the class next to the code that throws it
  (examples: `engine/.../components/MissingComponentException.kt`, `core/.../world/collisions/InvalidSlopeLabelException.kt`),
  and test that it is thrown.

## Layout

- **No blank lines between short statements.** Several one-line calls or assignments in a function or lambda body
  stay in one block. A blank line goes only between real groups: multi-line blocks, a `return` after a block, class
  members.

  ```kotlin
  override fun onSpawn(spawnProps: Properties) {
      super.onSpawn(spawnProps)
      trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
      body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
  }
  ```

## Comments

- **No comments in code**, except a `TODO` at the code for an edge case that a change knowingly accepts, or for a
  deferred decision. The `TODO` says what the case is, when it happens, and where the follow-up is tracked, in one
  line where possible.

  ```kotlin
  // TODO: widen the player type when an enemy clone calls this (item 121)
  ```

## Timing and units

- **Seconds and tiles.** Timings are `Timer`s in seconds, updated with `delta`. Sizes and speeds are in tiles, scaled
  by `ConstVals.PPM`. A value ported from another game (for example X6 frames or pixels per frame) is converted once
  into a named constant, not counted in frames at run time.

## Tests

- **Drive the public flow.** Spawn for real, hit through the public damage path, and read results through public
  state. No reflection (`getDeclaredField`, `isAccessible`) and no other access to private state. If a value cannot be
  reached through a flow, ask the owner before you widen a visibility.
- More rules for the `core` tests (heap, natives, the test services) are in `core/src/test/CLAUDE.md`.
