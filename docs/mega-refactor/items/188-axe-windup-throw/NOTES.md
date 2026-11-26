# Notes: 188 Hold-to-aim axe throw on a generic weapon windup

Research for this item. Based on `master` at `2189b7a` / `19e713e` (2026-11-26).

## Scans (2026-11-26, chat session, Opus subagents)

- The axe fires on the just-released frame of SHOOT (B), then `throwAxe` runs after a fixed 0.1 s `delayRun`
  (`MegamanWeaponFire.kt` `fireAxeSwinger`). The 0.1 s matches frame 0 of the `*_axe_throw` animations (2 frames of
  0.1 s), so the axe spawns with frame 1. Frame 0 shows the axe above the head.
- The per-player `WeaponChargeComponent` (`entities/state/`) is a `Timer(full, TimeMarkedRunnable(half, onHalf))`
  with the buster sound hard-wired in `players/PlayerCharge.kt`. `Megaman.kt` resets it every frame for a weapon that
  is not chargeable, so today a hold of the axe builds nothing.
- `Axe` (`entities/projectiles/Axe.kt`) is an `ABSTRACT` body with no friction and no clamp, gravity 56.25 tiles/s²
  set in `preProcess`. The world steps with semi-implicit Euler at `settings.performance.fixedStep`, so a pure
  function can reproduce the arc exactly. `Direction.RIGHT` gravity has the same sign as `LEFT` (bug: the axe never
  comes back in a RIGHT-gravity room). `LumberJoe` also spawns `Axe` with the default direction `UP`.
- Shapes (`DrawableShapesComponent`) draw after the HUD, with no interpolation and no blending; sprites have z-order,
  alpha and the body interpolation offset. The `COLORS` atlas has a `white` region.

## Design reviews (2026-11-26)

- Reusing `chargeable = true` for the axe would switch on the buster sound, light, `_charge_*` keys and the
  enhancement; rejected in favor of the generic windup.
- `ChargeProfile : Updatable` was rejected: `Updatable` is a context-free per-frame tick, the hold advances only in
  the controller phase while SHOOT is held, and a shared profile cannot hold per-player state.
- An independent review (Opus) of the final design found: the status gate must check the current weapon (switch in
  the same frame), climbing with the axe held, the release animation sync, the stun sound stop and the co-op global
  sound stop as named changes, the preview must share the whole throw computation, and the play-test test timing
  rule. The branch `SCOPE.md` takes each finding.
