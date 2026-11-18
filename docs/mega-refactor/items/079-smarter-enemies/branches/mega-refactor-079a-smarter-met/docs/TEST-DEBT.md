# Test debt: mega-refactor-079a-smarter-met

One line per rule of behavior that the final pass turns into a test (`docs/ways-of-working/gameplay.md`).

- A `ChargedShot` on the closed helmet does no damage and puts the `Met` in `STUNNED`.
- A charged hit launches one `MetHelmet` (apex 1.5 tiles half, 3 tiles full) and puts the `Met` in `STUNNED`; the
  landing of the helmet moves it to `HELMET_LAND`, then `SHIELDING`.
- `STUNNED` lasts at least `T_min` (0.89 s) and at most `T_max` (1.77 s); at `T_max` a helmet still in the air starts the fetch.
- A charged hit on the airborne helmet counts; the 3rd breaks it in the air (debris at the helmet), and the `Met` goes
  `DESPERATE_RUN` after `T_min`.
- The helmet does no damage to anyone; a block on its head sends it down; a block under its feet makes it rest.
- A destroyed `Met` releases its helmet: it falls free, rests on a block, and is culled off camera.
- The shot that launched the helmet does not count again on the airborne helmet.
- An exposed `Met` takes the normal damage of a charged shot; it dies when the damage reaches its health, else it is
  stunned and the hit counts.
- `GravityFrame.velocityToLocal` is the inverse of `velocityToWorld` for the 4 directions.
- The 3rd charged hit in one spawn breaks the helmet and puts the `Met` in `DESPERATE_RUN`.
- A plain `Bullet` damages a stunned `Met` as before (15).
- A `run_only` `Met` takes charged-shot damage as before and never is stunned.
- A pooled `Met` that spawns again has its helmet (`helmetHits` reset).
- The instant-kill damagers (`GutsTankFist`, `BigAssMaverickRobotOrb`, `PurpleBlast`) still kill the `Met`.
- A running `Met` stops (whole body on the ledge) and shields at a `PIT` when no jump fits; it walks on at `FLOOR` and `SAFE_DROP`.
- A frozen `Met` is not stunned.
- Round 1 changed `GroundProbe.checkAhead` (`distance` became `lead`): `FLOOR` while a thin strip just past the front
  edge (`lead` + 0.1 tile) is over ground; the drop and `DEATH` checks use a body-wide catch column past the strip.
  Rewrite the `GroundProbeTest` cases on the old body-wide column (at least "gives FLOOR until the body center
  reaches the edge"), and add: the `Met` stops with its whole body on the ledge.
- A `Met` that stops at a `PIT` or a `WALL` keeps its facing (no turn); a desperate `Met` stands and faces the player.
- At the end of `POP_UP`, a `Met` whose way is blocked (no jump fits) goes straight to `SHIELDING`; it never enters
  `RUNNING` (no `Run` frame).
- After `T_min`, a resting helmet sends the `Met` to `FETCH_HELMET`; touching it gives `PICKUP_HELMET`, then
  `SHIELDING` with the helmet's hit count.
- A blocked way, a culled helmet or the 4 s timeout gives `CRY` (1 s), then `DESPERATE_RUN`; the helmet is released.
- A desperate `Met` that touches a free helmet (no owner, not seated) claims it and puts it on.
- A helmet breaks on its own 3rd charged hit (its count travels with it), also when it has no owner.
- A charged shot on a stunned (helmetless) `Met` only damages it; it does not count toward a helmet.
- A shot on the closed helmet does no damage, also through its bounce or an explosion: the stunned `Met` stays shielded while that shot touches it.
- No charged shot pushes a helmeted `Met` back.
- A flying helmet turns in 45-degree steps; a seated one is upright.
- A free resting helmet (no owner) draws behind Mega Man and the `Met`s, upside down, with a slow brightness pulse.
- A knocked-off helmet drifts in the shot direction (about 2.3 tiles half, 4.9 tiles full) and rests away from the head.
- A helmetless (desperate) `Met` never shoots, after a helmet break and after a lost helmet.
- Shots pass through a free helmet that rests on the ground (no `SHIELD`, no `BODY`); a flying one still deflects them.
- `HalfDomeShield`: a shot whose top is below the dome center (local frame) passes; a shot that reaches the dome deflects at the circle, not at the body rectangle; 4 gravity directions.
- The shot that launched a helmet passes through that helmet (no second bounce back into the `Met`).
- A `run_only` `Met` keeps its run speed when it leaves the ground (as before the branch).
- `requestState` throws on an edge that is not in `TRANSITIONS`; a charged hit during `PICKUP_HELMET` stuns or breaks.
- The half dome sits on the side of the feet for each gravity direction.
- A helmet that is consumed (picked up) in this frame cannot be claimed by another `Met`.
- The knocked-off helmet drifts away from the side the shot came from (decided by positions, not by the shot velocity).
