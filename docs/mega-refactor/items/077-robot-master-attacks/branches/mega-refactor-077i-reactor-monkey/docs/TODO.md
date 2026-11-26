# TODO: mega-refactor-077i-reactor-monkey

## Tasks

- Before the merge: a test room for the monkey in `RefactorTest077.tmx` (a copy of `mini_boss_room_1`, and one of the
  water room) and the fight tests of `TEST-DEBT.md`, once the owner says the moveset is final (`O4-R2`, `gameplay.md`).
- Before the merge: repack `ReactorMonkey` with TexturePacker on a laptop (`ReactorMonkey.tps`, Basic) if the owner
  wants the TexturePacker layout; check by script that names and pixels stay.
- Later cleanup: remove the unused `Bosses1` `ReactorMonkey/{stand,stand_damaged,throw,throw_damaged}` frames once no
  code reads them, and repack `Bosses1`. The `Projectiles1` `ReactorMonkeyBall/0..3` stay while the calm ball uses
  them (or move them into `ReactorMonkey` with that repack).
- Set the chest-beat passes constant after the owner's play test (art default 4).

## Deferred moves (art missing; each has a `TODO` in the code)

- Bank throw: needs `throw_up` (+ `_damaged`, `mad_throw_up`, `mad_throw_up_damaged`).
- Chest-Beat Shockwave: needs a `RadWave` entity strip and a fist-slam frame.
- Gorilla Leap: needs `leap_crouch` and a real (non-`ABSTRACT`) body or a scripted arc; off in the water room.
- Fallout Pool: needs a `FalloutPool` strip; off in the water room.
- Reactor Shake: needs rubble and dust marks.
- Knuckle Charge: needs an art check of the on-all-fours pose height (at most 2.5 tiles).

## Backlog candidates

- Move `OrbAim` out of `bosses/bigassmaverickrobot/` (it now has 4 callers).

## Bug candidates
