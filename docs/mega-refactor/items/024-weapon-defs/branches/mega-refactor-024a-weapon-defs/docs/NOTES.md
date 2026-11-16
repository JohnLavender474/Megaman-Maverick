# Notes: mega-refactor-024a-weapon-defs

## Decisions

- (2026-11-14, owner, brief D1) A weapon is a `WeaponDef` object with a string id; `MegamanWeapon` goes. The ids are
  the old enum names in lower case, so every derived string stays the same.
- (2026-11-14, owner, brief D2) `WeaponDef.newState()` makes one `WeaponState` per player. Moon Scythes, Precious
  Guard, Needle Spin and Rodent Claws have a state subclass for their extra timers. The defs hold no state. The claws
  `slash_index` leaves the Megaman property bag.
- (2026-11-14, owner, brief D3) Every single-weapon rule becomes a hook or a data field on the def (not the
  recommended "one flag + def compares"). This covers the move rules (Needle Spin blocks, the Rush Jet air dash and
  jetpack, the claws run stop and wall grip, the firing fixtures, the charge stop) and the look rules (shoot keys,
  air-dash key, trail keys, the tornado sprite, the animation time and restart rules). The hooks take `Megaman` until
  `024b` widens them to the loadout owner (a `TODO` at each).
- (2026-11-14, owner, brief D4) Split: `024a` refactor only; `024b` a loadout per character and the `is Megaman`
  workarounds; then `060h`.
- (2026-11-14, Claude) The jetpack handler keeps a direct reference to the Rush Jet def for its refill when another
  weapon is current (`containsWeapon(RUSH_JET)`): the handler is the Rush Jet's own mechanism.

## Overlap with other branches

- `060d` (active): `GroundSlideBehavior.kt` and `MegamanGroundSlideTest.kt`. This branch changes them in the last code
  step. If `060d` is merged by then, merge `master` first.
