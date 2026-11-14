# TODO: mega-refactor-060c-protoman-buster

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- **X: on a ladder `B` is always the buster** (scope change 2, for every character). For `081g`, which owns X's
  ladder: with `UP` held on a ladder, `B` must not start the sabre.
- **`WhiteBurst` has an `EXPLOSION` fixture.** At a `Laser` end, a projectile that explodes on an explosion (for
  example `BunbyRedRocket`, `IceBomb`, `Snowball`) or a `SmallIceCube` reacts to the burst. Proto Man's deflect spark
  turns the fixture off (`ConstKeys.COLLIDE`); check whether the laser needs it.

## Bug candidates

- **`ProtomanTest` and `MegamanXTest` fail when they run alone** (found 2026-11-11, also on `master` `aef725ce1`). With
  `--tests` on one class, every test fails in `beforeEach` with `ClassCastException: java.lang.Object cannot be cast to
  TextureAtlas`: `MegaEntityFactory.init(services)` preloads 25 `Bullet`, and `Bullet.init()` loads its regions from the
  relaxed assets mock, whose generic atlas getter returns an `Object`. In the full suite an earlier test has filled
  `Bullet`'s static region cache, so the load is skipped. The tests depend on the order. `PushableBlockTest` passes
  alone. Related: item `028` (the companion region caches).

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

## Before the merge

- Done 2026-11-14 (owner request): the in-play hit-box drawing of `PlayerSabreSlash` (`prodShapeSuppliers`) is
  removed; the hit box draws only with `--debugShapes`, as before the branch.
