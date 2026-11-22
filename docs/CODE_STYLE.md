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

## Names

- **Explicit, not clever.** A name says what it computes in plain words, and a reader with no context reads it one
  way only. Do not use a pun or a word with a second meaning in the game (for example "blocks" next to block
  entities). When a clever name seems better, give it to the owner as a suggestion; do not choose it.

  ```kotlin
  fun Megaman.currentWeaponPrevents(move: BehaviorType)   // not firingBlocks(move)
  ```

- **Long where a short name has two meanings** (owner, 2026-11-22, item `178`). Use the longer name when a short one
  can mean two things in the code or the game. This applies most to a new type next to existing concepts. Build the
  name from existing type names. Search the code to make sure that no other thing already uses the word, for example
  "Controller" for gamepad and keyboard input. Give each implementation the full name of its interface, so that each
  one reads clearly on its own. A long name costs little at a declaration and at a few call sites.

  ```kotlin
  sealed interface PlayerSlotAgent                     // not Agent, Seat or Controller
  class HumanPlayerSlotAgent(...) : PlayerSlotAgent    // not Human
  class BotPlayerSlotAgent(...) : PlayerSlotAgent      // not Bot
  ```

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

- **A guard is its own statement.** Do not put a `?: return`, `?: continue` or `?: break` inside a loop header, a
  condition or a call argument. Put the null check on its own line before the code that uses the value. Then the
  jump target is easy to see, and a `return` in a lambda cannot end the outer function by mistake.

  ```kotlin
  entities.forEach { entity ->
      val cullables = entity.getComponent(CullablesComponent::class)?.cullables?.values() ?: return@forEach
      for (cullable in cullables) {
          ...
      }
  }
  // not: for (cullable in cullables ?: return) { ... }
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

## Allocation and pooled objects

- **No `new` of a math object in per-frame code.** In code that runs each frame (an `update`, an updatable, a body
  `preProcess`, a sprite or shape supplier, a system), do not call `Vector2(...)`, `GameRectangle(...)` or a
  constructor of another type in `GameObjectPools`. Choose by how long the value lives:

  | The value lives | Use |
  |---|---|
  | Only in this frame | a pool scratch: `GameObjectPools.fetch(Vector2::class)`, or a helper with no `out`, such as `body.getCenter()` |
  | For the life of the entity or the spawn | an owned field, made once and set each time: `private val home = Vector2()`, then `home.set(...)` |
  | A list for the spawn | an `OwnedArray`: `points.clear()`, then `points.add().set(...)` |
  | Once per spawn or event, in a pooled entity | an owned field, set each time (see the next rule) |
  | Once per level load or screen build, outside a pooled entity | a plain `new` |

  ```kotlin
  body.preProcess.put(ConstKeys.DEFAULT) {
      val size = GameObjectPools.fetch(Vector2::class).set(WIDTH, HEIGHT)   // not Vector2(WIDTH, HEIGHT)
      body.setSize(size)
  }
  ```

- **A pooled entity makes its objects once.** An entity from `MegaEntityFactory` lives through many spawns. Make
  its vectors, shapes, sprites, timers, motions and lambdas in field initializers or in `init()` (or the first time
  a spawn needs one more, then keep it). `onSpawn`, `onDestroy` and event handlers only `set(...)`, `reset()`,
  `clear()`, hide or show; they do not call a constructor, and `onDestroy` does not remove what the next spawn
  makes again. A value that a spawn keeps (an anchor, a home point) is an owned field, never a pool scratch. A timer
  is set again with `resetDuration(...)`; a cullable with `reset()` before `putCullable(...)`; a motion with its
  `set(...)` (`SineWave`, `Trajectory`, `RotatingLine`) or its fields and `reset()` (`Pendulum`). A list whose size
  depends on the spawn grows only, and hides what this spawn does not use. A value that only goes into a child's spawn
  props is a pool scratch (the child copies it). `SpawnAllocationGuardTest` scans these bodies for constructor calls
  against a baseline that only shrinks.

  ```kotlin
  private val anchor = Vector2()
  private lateinit var pendulum: Pendulum                 // made in init() with anchor

  override fun onSpawn(spawnProps: Properties) {
      spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(anchor)   // not getCenter(Vector2())
      pendulum.length = spawnProps.get(ConstKeys.LENGTH, Float::class)!! * ConstVals.PPM
      pendulum.reset()
  }
  ```

- **Never keep a scratch.** A pool scratch is valid until the end of the frame after next, and then another caller
  gets it. Do not put one in a field, a list, a delayed runnable or a lambda that runs later; copy it. An entity
  copies what it keeps from its spawn props into its own fields (`trajectory.set(...)`, not
  `trajectory = spawnProps.get(...)`). No `free()` exists. The rules and the `--poolChecks` debug flag are in
  `core/.../utils/README.md`.
- **No pool fetch in a default parameter.** Take an `out` parameter, or give a function with no `out` that returns a
  scratch (`PoolOwnershipGuardTest` checks it).

## Tests

- **Drive the public flow.** Spawn for real, hit through the public damage path, and read results through public
  state. No reflection (`getDeclaredField`, `isAccessible`) and no other access to private state. If a value cannot be
  reached through a flow, ask the owner before you widen a visibility.
- More rules for the `core` tests (heap, natives, the test services) are in `core/src/test/CLAUDE.md`.
