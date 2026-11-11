# Scope: mega-refactor-060j-mega-player

- Status: approved
- Approved: 2026-11-10

## Goal

Refactor only. Add an abstract core base class `MegaPlayer` that owns the wiring that every player class shares, and
that is the receiver of the shared `players/` functions in place of the generic bounds. `Megaman` and `MegamanX` move
onto it. Megaman and X play the same. `060b` then builds `Protoman` on it.

## Item

`060` (Proto Man), `ITEM.md` decisions of 2026-11-10 (owner):

- An abstract `MegaPlayer` base class: `MegaPlayer : AbstractHealthEntity, IPlayer, IBodyEntity, IFaceable,
  IDirectional`. It owns the shared lifecycle wiring and the shared components, and the shared `players/` functions
  take a `MegaPlayer` receiver in place of the generic bounds. `Megaman`, X and `Protoman` extend it. `O8-R1` changes to
  allow it ("thin": only what every player class shares). It is built in a refactor-only branch, `060j`, which merges
  before `060b` goes on. The 2 last `playerSlot!!` reads (item `148`) go in the work on `060`; `060j` does them.
- Owner's input: "Keep in mind that the base class should be able to support Proto Man and other similar playable
  characters (Bass, Axl, Zero, etc.) in the future."

Research: `temp/060-iplayer-review/report.md` (the options), `temp/mega-refactor-060j-mega-player/scan.md` (this
branch's scan, with every `file:line`), `temp/mega-refactor-060b-protoman-move/scan-code.md` (the minimal component
set and the parts that X copied).

## Current state

Paths under `core/src/main/kotlin/com/megaman/maverick/game/`. `M` = `entities/megaman/Megaman.kt`, `X` =
`entities/megamanx/MegamanX.kt`. Base: `master` `845b194ad`.

- `Megaman` and `MegamanX` both extend `AbstractHealthEntity` and implement the same 6 interfaces; `Megaman` also
  implements `IScalableGravityEntity` and more.
- Same code in both (moves): the damage negotiator, damage timer, damage recovery, `invincible`, `facing`, the event
  mask (6 keys, same order), `canBeDamagedBy`, `getDamageDuration`, `editDamageFrom`, `definePointsComponent`,
  `getType() = MEGAMAN`; the components `PlayPhase`, `Locks`, `Run`, `Audio`; freeze (X's is a copy); the progress
  callbacks and `setMaxHealthFromHeartTanks` (same text); the camera subject (same formula; the height is 1.5 in both);
  the room carry (same effect: Megaman's `!CLIMBING` is always true for X).
- The same skeleton in both: `onSpawn` (`M:288-317`, `X:129-152`), `onDestroy` (`M:319-327`, `X:154-160`), `onEvent`,
  `takeDamageFrom`, `onHealthDepleted`.
- Same idea, different values (stay in the class): body, behaviors, controller, sprites, animations, momentum,
  knockback (Y 5 for Megaman, 5.9161 for X), stun (Megaman counts wall sliding as a surface and scales by
  `movementScalar`), `direction`, the updatables (the shared lines run in a different order).
- Megaman only: `IScalableGravityEntity` (`world/contacts/rules/BodyGravityChangeRule.kt:52` checks it, so it must
  not go on `MegaPlayer`, or X starts to react to gravity fixtures), sand, gravity table, bubble, trail, gravity
  device, light, weapons, `aButtonTask`, the Mario death sound, `weaponsAttained`. The charge stays in each class.
- Shared functions: 18 in `players/` change to a `MegaPlayer` receiver (`playerBody`: `MegaPlayer` +
  `IScalableGravityEntity`). 6 keep a wider receiver because a non-player or a non-`MegaPlayer` test calls them:
  `bodyOverGround`, `isRunSidePressed`, `playerMaxRunSpeed`, `onStunPlayerEvent`, `PlayerDamageNegotiator`,
  `playerAllowsDamageFrom` (`WarpSpotTest.kt:143` calls it on an `IPlayer`).
- Item `148`: `players/PlayerAirDash.kt:48` and `entities/megaman/components/ControllerComponent.kt:76` read
  `playerSlot!!`; with a `MegaPlayer` receiver they read `slot`.
- Tests: `TestHealthPlayer` (12 test files) and `RunPlayer` (`PlayerRunTest`) are `AbstractHealthEntity` doubles that
  never call `init()`. `SecondPlayerEntity` and 7 other `IPlayer` doubles do not change. `EntityConstructorsTest` and
  the smoke tests skip abstract classes.
- Component reset runs in add order (`engine/.../GameEngine.kt:192`, an `OrderedMap`).

## In scope

- `players/MegaPlayer.kt`: the abstract class, as in the sketch in `scan.md` ("Proposed `MegaPlayer`"):
  - final `onSpawn`, `onDestroy`, `onEvent`, `takeDamageFrom`, `canBeDamagedBy`, `getDamageDuration`,
    `editDamageFrom`, `definePointsComponent`, `getType`;
  - an open `init()` that adds the shared components in the current order, and calls abstract members for the class
    components (body, momentum, knockback, stun, behaviors, controller, sprites, animations) and `cameraFocusHeight`;
  - open hooks with a caller in both or one class: `teleportableComponent()`, `onLevelDefeated(level)`,
    `onPlayerSpawn(spawnProps)`, `onPlayerDestroy()`, `onPlayerEvent(event)`, `afterPlayerDamage(damager)`;
    `onHealthDepleted` stays open.
- `Megaman` and `MegamanX` extend `MegaPlayer`; their repeated members and components go; their class lines go in the
  hooks at the same point as today. 2 line moves that change nothing (proof in `scan.md` Q2): Megaman's
  `damageTimer.setToEnd()` and `airDashConsumed = false`; X's `sabre.reset()` after `playerOnDestroy()`.
- The 18 shared functions take a `MegaPlayer` receiver; the 2 `playerSlot!!` reads become `slot`.
- Tests: a test base `TestMegaPlayer` with stubs for the abstract members; `TestHealthPlayer` and `RunPlayer` extend
  it. No component-order test (D4).
- `players/README.md`: line 6 (the base class rule), the parts diagram, "The order of a spawn".

## Out of scope

- Any new API for a future character (Bass, Axl, Zero). The future needs are notes in `docs/NOTES.md`, not code
  (`O1-R5`).
- `direction` stays in each class (a shared setter is item `058`'s camera-turn work).
- The charge, the weapons, `aButtonTask`, `IScalableGravityEntity` and every Megaman-only part.
- `Protoman` (`060b`). `CharacterDef` (`081`/`060i`). Removing `AbstractHealthEntity` (`048`).

## Affected areas and risks

- `players/` (19 files), `entities/megaman/` (`Megaman.kt`, about 6 component files), `entities/megamanx/`
  (`MegamanX.kt`, about 4 files), 14 test files.
- Reset order: components reset in add order. The template keeps the relative order of every shared component. Moved
  ones: Megaman's sand, gravity table, bubble, trail (no `reset()`), weapon charge (resets only its own timer); X's
  `Audio` and animations swap (each resets only its own data). No order test (D4); the review checks the order.
- Name trap: an abstract member with the same name as a current `internal fun Megaman.defineXComponent()` extension
  makes a delegate call itself. D3 removes the extensions: their bodies become the members.
- `onSpawn` health: today only X sets the max health from the heart tanks at spawn; the template does it for both
  (decision D1).

## Overlap with other branches

- `mega-refactor-081f-x6-intro-fixes`: does not change `MegamanX.kt`. It changes `PlayerDamageNegotiator.kt` (values
  only) and `StunComponent` (the 3-parameter constructor stays). Its worktree has uncommitted edits to
  `MegamanXValues.kt`, which `060j` only reads. Merge order does not matter; the second branch merges `master` in.
- `mega-refactor-060b-protoman-move`: paused, no code.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O8-R1` | helps | Its new text asks for `MegaPlayer`; the class holds only shared parts |
| `O8-R2` | no effect | No character checks added |
| `O1-R1` | no effect | |
| `O1-R2` | conflict (preference) | A new layer under `entities/`-level classes; owner chose it (2026-11-10); `048` must move it |
| `O1-R3` | no effect | The template keeps the order of every lifecycle line (2 independent moves) |
| `O1-R4` | no effect | No new keys |
| `O1-R5` | helps | Every hook has a caller in `Megaman` or `MegamanX`; future-character needs stay notes |
| `O2-R5` | no effect | |
| `O5-R1` | no effect | No new per-frame work |
| `O6-R1`, `O6-R2` | helps | CI and the named checks in `CONTRACT.md`; a new component-order test |
| `O7-R3` | no effect | The `MOON_MAN` check stays in `Megaman.getMaxRunSpeed` |
| `O9-R1` | no effect | |

## Decisions

Claude's calls (scan recommendations, no owner choice needed):

- `MegaPlayer` also implements `IEventListener` and `IAudioEntity`: the shared `onSpawn`, `onEvent` and the damage
  sound need them.
- `direction` stays in each class (no new API; item `058`).
- The file is `players/MegaPlayer.kt`, next to the shared functions and the README.

Owner answers to the brief (2026-11-10):

- D1: the shared `onSpawn` sets the max health from the heart tanks for every class.
- D2: `MegaPlayer` reads `MegamanValues` now, "just make sure to include a concise TODO" at the reads (a
  `PlayerValues` comes with the first character that needs other values).
- D3: move the part bodies into the classes: each `define...Component()` extension body becomes the member override
  in `Megaman.kt` and `MegamanX.kt`. Confirmed after the size check: `Megaman.kt` grows from 443 to about 700 lines,
  `MegamanX.kt` from 258 to about 550; the emptied component files go.
- D4: no component-order test.

Scope change (owner, 2026-11-10, during step 2):

- A) The lifecycle functions that only `MegaPlayer` calls after this branch (`playerSpawnStart`, `playerSpawnEnd`,
  `playerOnDestroy`, `playerOnRoomTransition`, `playerAfterDamage`, `playerEditDamage`, `playerInvincible`) become
  private members of `MegaPlayer`. README rule: a lifecycle function that every player runs is a private member of
  `MegaPlayer`; a move that only some characters use is a plain function or a generic helper that a class opts in to.
- B) The run is not an extension and not player-only (owner: "What if I want an enemy or boss that moves almost if not
  exactly like Megaman?"). Generic part in `entities/movement/Run.kt`: `RunParams` (no gate fields),
  `runStep(body, direction, run, side, delta, params)` and `maxRunSpeed(body, maxSpeed, waterScalar)`, plain
  top-level functions for any entity with a body and a `RunComponent`. Player part in `players/PlayerRun.kt`:
  `playerRunButton(player: MegaPlayer, side, params, blocked, onBlocked)`, a plain function that holds the gate
  (input, wall-slide facing, can-move rules, Rodent Claws hook) and calls `runStep`. `PlayerRunTest` splits into a
  generic `RunTest` and the gate tests.
- C) After the movement-reuse research (`temp/mega-refactor-060j-mega-player/movement-reuse.md`: no boss or enemy
  moves like the player), `playerBody`, `playerJumpBehavior`, `playerWallSlideBehavior` and `playerAirDashBehavior` do
  not split. Each becomes a plain function with an explicit `player: MegaPlayer` parameter, not an extension. The run
  split B stays (owner). Each move function typed on `MegaPlayer` gets a `TODO` for item `121` (enemy clones), which
  widens the type. The boss-to-boss duplication became item `150`.


## Open questions

See the brief.
