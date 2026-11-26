# Notes: mega-refactor-188a-axe-windup-throw

## Decisions

The owner's decisions are in `SCOPE.md` ("Decisions"). Choices made in the implementation (2026-11-26):

- `BusterCharge` hooks are all required (named arguments at every call site); both players pass `onFullyCharged = {}`.
  The half and full triggers are `TimeMarkedRunnable`s on one `Timer`.
- `WeaponWindupComponent` is added in `MegaPlayer.init()`, before `AudioComponent`, so its destroy-time `stop()`
  request is cleared by the `AudioComponent` reset and cannot stop another player's sound on the next spawn.
- The per-frame switch rule is `Megaman.stopWindupNotOfCurrentWeapon()`.
- `stopWindup()` sends a sound stop only when a windup is active, so every repeated global stop is gone (Megaman's
  input gates while B is held, the hurt check, X's per-frame stops). Same effect as intended change 4.
- Axe gate: `canFireWeapon(currentWeapon, NOT_CHARGED)`; the buster keeps its `HALF` / `FULL` next-status gates.
  The axe pause (room transition, camera turn) applies only to the axe.
- Freeze: the Megaman `Animator`'s `shouldUpdateAnimationPredicate`; it also sets time 0 each frame, because
  `Animation.reset()` leaves a stale frame index.
- Run while holding: frozen on frame 0. `run_axe_throw` (2x2, read row by row) shows the axe overhead only in frame 0;
  frames 1 to 3 are after the throw (`temp/188-axe-frames/run_axe_throw_frames.png`).
- The arc uses `services.settings.performance.fixedStep`; the world's physics delta is always that step
  (`--fixedStepScalar` only changes how much real time a step uses).
- `MegamanChargeLightTest` was dropped: it ran out of memory in CI on the spawned rig. The light reads `charging` and
  `fullyCharged`, which `MegamanWeaponChargeTest` covers at the thresholds.

## Overlap with other branches

- `187a` (intro story art): art only, no overlap.
- Others active on 2026-11-26 (`078f`, `078h`, `178b`, `185c`): none touch the weapon charge, Megaman's weapons or
  `Axe`. `185c` moves game layout and test roots; a later merge of `master` may move paths.
