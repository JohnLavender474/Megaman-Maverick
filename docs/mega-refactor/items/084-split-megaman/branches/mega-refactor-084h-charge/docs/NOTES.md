# Notes: mega-refactor-084h-charge

## Decisions

- (2026-11-01, owner) The name is `WeaponChargeComponent`, not `ChargeComponent`. "Charge" has at least 5 meanings in the game (a rush attack, an electric wind-up, a shot wind-up, the `ChargedShot` entities, the player's weapon charge). `BusterChargeComponent` is wrong, because the rule is per weapon (`isChargeable(weapon)`).
- (2026-11-01, owner) A small separate component. `shootAnimTimer` and any larger weapon component stay in `024`. A read-only review compared 4 placements: a small component, a larger weapon component, the timer in `MegamanWeaponsHandler`, and no component. `Megaman` has 24 components, and the 15 game parts have a median of 19 lines; no merge is justified now.
- (2026-11-01, owner) Accept the change in `Test1.tmx`: a death in a charge no longer leaves the charge light until the respawn.

## Overlap with other branches

`mega-refactor-031f-menu-input` changes no file in `entities/megaman/` or `entities/state/`.
