# Notes: mega-refactor-084e-class-line

## Decisions

## The two carts

Found on 2026-10-31, when the owner reported that Mega passes through the `Cart` in the `cart` room. This is not a defect of this branch: `Cart.kt`, `MegaCollisionHandler.kt` and the engine world code have no change here.

- `Cart` is a `DYNAMIC` body with no `BLOCK` fixture. `MegaCollisionHandler.trySpecialCollission` and `StandardCollisionHandler` act only on a `STATIC` and `DYNAMIC` pair, so `Cart` and Mega, both `DYNAMIC`, do not collide. `FeetCartRule` sets the owner, the sense and a property, and no code reads them.
- `CartV2` extends `Block`, so it is `STATIC` and solid. `DesertMan_16x14_v4.tmx` holds 3, each with only `facing=right`. It moves in `shouldMove()`, when the feet of a player overlap its body. It does not use `FeetCartRule`.

## Overlap with other branches

Checked at branch start (2026-10-31, `master` at `405cf0171`):

- `mega-refactor-031e-bindings` (owner's play test next): 2 shared code files, `AirDashBehavior.kt` and `ControllerComponent.kt`, in the imports only on this branch.
- `mega-refactor-051-master` and `mega-refactor-051d-auto-scroll`: no shared code file. Both change other `core` files, for example `MegaLevelScreen.kt`.

Checked again before the merge (2026-11-01, `master` at `bc2929700`):

- `mega-refactor-031e-bindings`: the same 2 files.
- `mega-refactor-051-master` and `mega-refactor-051-fix-scroll-room-scope`: 1 shared code file, `GravityBlock.kt`. Both this branch and `051` delete the same import, `megaman.components.headFixture`, so a merge can stop on these import lines. `051-master` keeps the deletion.
