# Notes: 190 Rush in the game

Research for this item. Scan of 2026-11-27, `master` at `a0cb0469a`.

## Rush Jet today

- `MegamanWeapons.RUSH_JET` (`entities/megaman/weapons/MegamanWeapons.kt`): `fire = ::fireRushJet`, which only shoots
  the buster; `airDash` (a longer, faster air dash that ends on `UP`); `enablesJetpack = true`; `stopsChargeOnHold`.
- The moves read the def at once when the weapon is current: `JetpackBehavior` (`currentWeapon.enablesJetpack`),
  `AirDashBehavior` (`currentWeapon.airDash`), `MegamanJetpackHandler.isActive` (drain and refill of the energy).
- The look: `MEGAMAN_LOADOUT` gives Rush Jet the atlas `MEGAMAN_RUSH_JETPACK` (`Megaman_v3_RUSH_JET.txt`): every
  Megaman frame with the jet on his back. `MegamanAnimations.buildFullKey(key, weapon)` and `currentLook` pick the
  frames of the current weapon, so the jet shows the moment Rush Jet is selected.
- No Rush entity exists. `assets/sprites/frames/Buddies/` and the atlas `Buddies.txt` hold 4 old Rush strips
  (`RushJet`, `RushSpring`, `RushStand`, `RushTransformToJet`, 2 frames of 32x32 each), but no `TextureAsset` names the
  atlas and no code reads it. Their source is not recorded.

## Pause while an animation plays

- `PlayerStatsHandler` (heart tank, health fill) pauses play with `LevelStateHandler.turnOffAllSystemsButSprites()`
  and turns the systems on again when its timer ends. The animations system is off too, so an animation that must
  play during the pause has to be driven by the code that runs the pause.
- Item `012` (named, composable freezes) plans one home for such freezes.

## Official games (general knowledge, not checked against a source)

- Mega Man 3 to 6: with a Rush ability selected, `B` calls Rush, who beams down in front of Mega Man and lands on
  the ground. Mega Man touches (Jet) or jumps on (Coil) him. With Rush already out, `B` shoots the buster. When the
  weapon changes, Rush beams out.
