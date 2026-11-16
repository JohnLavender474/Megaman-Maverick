# 153: A current-weapon icon on every player's HUD, and a HUD that fades when a player is behind it

- Status: idea
- Type: feature
- Objectives: `O9`, `O8`
- Depends on: none
- Related: `060c` (adds the first icon, for Proto Man's sabre and buster slots), `024` (weapon definitions: the icon
  is part of a `WeaponDef`), `047` (co-op HUD), `094` (HUD per view)
- Branches: none yet

## Goal

- Every player's HUD always shows an icon of the current weapon, next to the health bar. Today only a Mega Man
  special weapon shows, as its ammo bar. The Mega Buster, X and Proto Man show nothing.
- The HUD draws with a lower alpha while a player overlaps it, so that the player is not hidden behind it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `PlayerStatsHandler` draws one icon for the current weapon of the player in slot 0, for every player class: Mega
  Man (each `MegamanWeapon`), X (his buster) and Proto Man (his buster; his special weapons come with `060h`). `060l`
  (2026-11-16) removed the 2 slot icons that `060c` added for Proto Man.
- The health bar, the weapon bar and the icon draw with a lower alpha while a player's body overlaps their screen
  area.
- Every player's HUD weapon icons go in a `Hud` atlas (the owner chose `Hud` over `Bits` in `060c`). `060l`
  (2026-11-16, owner) deleted the `Hud` atlas (`TextureAsset.HUD`, `Hud.txt`, `Hud.png`, `assets/sprites/frames/Hud/`)
  with Proto Man's 2 slot icons, the only regions in it, so this item makes the atlas again.

## Owner's input

2026-11-13 (owner, during `060c`): "If there's not already an item for it, then I think we need to create an item to
make it so that a current-weapon icon is always displayed for any player's HUD. Furthermore, slight change we can
integrate into the same item: HUD's alpha should be reduced when player overlaps it so that the player isn't hidden
too much behind it."

## Open questions

- Where does each icon come from: the pause-menu weapon sprites, or new small HUD icons?
- How low is the alpha, and does it fade in and out or change at once?
- With `047` co-op, does the fade apply when any player overlaps the HUD, or only the HUD's own player?

## Sources

- `NOTES.md` in this folder.
