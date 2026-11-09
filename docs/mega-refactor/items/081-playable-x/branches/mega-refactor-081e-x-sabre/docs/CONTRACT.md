# Contract: mega-refactor-081e-x-sabre

- Status: approved
- Approved: 2026-11-09 (brief decisions B1 to B3)

## Automated checks

- [ ] `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- [ ] X tests: `UP` + `B` starts the slash and fires no `Bullet`; a held `B` after the slash does not charge, and its
  release fires no `ChargedShot`; `UP` pressed while `B` is held does nothing; the kind (floor, air, wall); no start
  in the ground dash, the air dash, the wall grab, the kick lock or damage; the movement and cancel rules (B3); damage
  ends the slash and destroys the slash entity; a room transition, a death and a level reset also destroy it.
- [ ] Damage tests: the slash values in `X6Damage` (`NORMAL` and `SABRE_ONLY`), `D1000` 2, the `D1000Core` stun, the
  sabre-only rule (B2); a damager that is not in the tables still does 0.
- [ ] `core:smokeTest` covers the new slash entity (`init()` with the real `MegamanX` atlas).
- [ ] Megaman's tests pass with no change.
- [ ] Mutation checks; the review before the play test. No game launch.

## Manual playthrough

Experimental checks first. From `.worktrees/mega-refactor-081e-x-sabre`.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_081 --room sabre` | Floor slash: hold `UP`, press `B`. X stops and does not turn, the slash plays in front of X, and no shot comes out. Hold `B` after the slash: no charge, and the release fires nothing. A jump after the first part of the slash cancels it. |
| `REFACTOR_TEST_081 --room sabre` | Air slash: X keeps air control and can turn; a landing switches it to a floor slash in the same swing phase, with no cut and no second sound; no air dash during it. |
| `REFACTOR_TEST_081 --room sabre` | Wall slash on a wall slide: the slash goes away from the wall; X holds still; a jump gives a wall kick. |
| `REFACTOR_TEST_081 --room sabre` | Targets: a `SabreBlock` breaks in 1 or 2 floor slashes (4 hits), and the buster does not hurt it (B2); a `MetaWheelF` takes the slash, and the buster does not hurt it (B2); a `Junkroid` dies in 1 slash; a `HoverGunner` shot is cut; a `MetaDridler` takes no damage. A hit on X during a slash ends the slash. |
| `REFACTOR_TEST_081 --room sabre` | The slash sprite is not tinted and does not blink with X's damage flash; the floor body shows no holes. |
| `X6_INTRO --room room1` | The 5 `SabreBlock`s on the route break with the sabre (tiles 62, 88, 96, 124 and 170); a charged shot does not break them. The `MetaWheelF`s take only the sabre; the route stays playable. |
| `X6_INTRO --room room2` | The `D1000` fight (the room before the boss room): a slash on the open core stuns `D1000`; a slash on `D1000` does a small amount of damage. A jump plus an air slash falls a few px short of the core: stay left of the middle when the core appears, kick up the right wall, and wall slash at about 4 to 5 tiles. |
| `REFACTOR_TEST_081 --room targets` (control) | `B` alone: the buster and the charge work as before. |
| `GLACIER_MAN --room room1` (control) | Megaman: the buster and the charge as before. |

## Intended changes

- X: the sabre slash on `UP` + `B` (floor, air, wall), with the rules in `SCOPE.md` "In scope" and the brief
  decisions.
- `SabreBlock` and `MetaWheelF`: only the slash damages them; the buster does not (B2).
- The X6 enemies (`Junkroid`, `HoverGunner`, `MetaWheelF`), `X6Shot`, `D1000` and `D1000Core` take the slash.
- New test room `sabre` in `RefactorTest081.tmx`.
- X's air dash (owner, 2026-11-09, at the play test): 0.4 s (was 0.3 s), 10 t/s (was 8), the `SELECT` start 14 t/s
  (was 12.5).
- The sabre hold frame (owner, 2026-11-09): 0.05 s on the floor and the wall (was 0.183 s and 0.2 s).
- `UP` + `B` in the ground dash or its end slide ends the dash and starts a floor slash (owner, 2026-11-09).
- X's feel, first pass (owner, 2026-11-09: "slow and floaty"): a rise gravity of 42 t/s² and a fall gravity of
  60 t/s² (was 30 for both); jump 14.2 t/s, wall kick 12.48 t/s and knockback rise 5.92 t/s, so the heights do not
  change; fall limit 15 t/s (was 12); walk 6 t/s (was 5); walk start 0.05 s (was 0.1 s). The wall kick and push-off
  side speed stays 5 t/s (no longer tied to the walk). Ground dash 11 t/s with a 15 t/s start (was 8 and 12.5); the
  dash jump air speed and the dash wall kick follow at 11 t/s.
- A running slash switches kind when X lands, goes off a ledge, or starts or ends a wall slide (owner, 2026-11-09;
  X6 ends the slash). The swing phase carries over (wind-up, swing, follow-through, recovery). Same slash entity,
  so no second sound and no new hits; the target damage timers stay the hit rule.
- Megaman: no change.
