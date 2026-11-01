# Notes: mega-refactor-084g-movement-state

## Decisions

- (2026-11-01, owner) `canMove` stays a `Megaman` member, with a `TODO` for `060`. The owner asked if a component is better. A read-only review compared 4 options on `master` at `c6b8cecf7`:
  - A component with a hook: no rule differs between characters, so it fails the rule "a component only when data or rules differ". It adds the most cost: 7 to 10 hook calls and 28 to 40 map lookups per frame, by counted reads.
  - `val IGameEntity.canMove`: no gain now, because all 10 reads have a `Megaman` receiver and no cast goes. It gives a wrong `true` on a frozen enemy, because `frozen` reads `FreezeComponent` and only `Megaman` adds that. It adds 7 to 10 `as?` casts per frame, and it has the same name as `AbstractProjectile.canMove` and 8 private members.
  - `val AbstractHealthEntity.canMove`: enemies get the same wrong value, and `048` removes that base class.
  - The member: no new API, no cost, and no wrong default. A later move changes only imports at the read sites.

- (2026-11-01, owner) Left and right use a last-pressed rule, in `fix/left-right-last-pressed`, not in this branch. This branch has no intended changes, and its test values must not change. Decisions for the fix:
  - A tie value in `RunComponent`, set in the 2 `onJustPressed` callbacks, applies only when both buttons are pressed. `InputFrame` and `PlayerInputs` do not change: menus read the same frame, and a masked button gives `SpiderWeb.kt` a false release.
  - A tap away from a wall leaves the wall.
  - For a press of both buttons in the same frame, `RIGHT` wins, as the actuator order gives.
  - The fix sets `facing` in the wall slide `init`, for the first frame of a slide.
  - `084g` merges first, unless the quicksand report changes that.

## Overlap with other branches

Checked at branch start (2026-11-01, `master` at `c6b8cecf7`):

- `mega-refactor-051e-camera-rotation`: 3 shared code files: `CrouchBehavior.kt`, `AirDashBehavior.kt` (1 changed line each) and `ControllerComponent.kt` (2 changed lines). This is its diff at that time; the branch is active, so check it again before the merge. `LevelRoomHandlerTest.kt` also changes there.

`051e` reached `master`, and `master` went into this branch at `f16f1aaa3` with no conflict.
