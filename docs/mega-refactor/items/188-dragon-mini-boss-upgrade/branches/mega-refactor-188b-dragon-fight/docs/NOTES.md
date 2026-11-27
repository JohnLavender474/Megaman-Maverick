# Notes: mega-refactor-188b-dragon-fight

## Decisions

Made by the implementation agent on 2026-11-26 within the approved design (the brief of the main session and
`temp/dragon-moves/IDEAS.md`).

- **Atlas: a new `MechaDragon` atlas, frames moved.** TexturePacker names a region by its path inside the packed
  folder, so the frames moved to `assets/sprites/frames/MechaDragon/MechaDragon/` (regions `MechaDragon/<strip>`) and
  `flame_puff.png` became `frames/MechaDragon/MechaDragonFlame.png` (region `MechaDragonFlame`, the `TAG` of the
  projectile). The `Bosses1.tps` entries of the moved files were removed. The packed `Bosses1` atlas still holds the
  old `MechaDragon/...` regions, unused (no other code reads them: `MechaDragon_OLD` is commented out); a repack of
  `Bosses1` on a laptop drops them.
- **Atlas method (no TexturePacker in the cloud).** `temp/dragon-code/make_atlas.py` writes `MechaDragon.txt` and
  `.png` in the format of the other atlases: libGDX text, `filter: Nearest, Nearest`, `Basic`-like shelf packing,
  no rotation, no trim, a 1 px extrude ring (2 px between regions, as `extrude 1` writes it), regions sorted by name.
  It is deterministic (two runs give the same bytes). `temp/dragon-code/check_atlas.py` reads the atlas back: every
  region equals its frame pixel for pixel, the extrude ring copies the edge, no overlap, one region per frame. Result:
  15 regions, 0 fails. `MechaDragon.tps` is `Bosses4.tps` with the data file, the folder and `allowRotation false`.
  The owner can repack it with TexturePacker; the layout will differ, the names and pixels not.
- **Ground center row 56.** The grounded cells draw the upper body 3 to 11 px higher than the airborne cells. With
  the airborne center row 64 and the grounded center row 56, the upper body stays in place when the pose changes at
  the same body center (fly to the hang pose, the takeoff crouch to fly).
- **Feet anchor art column 76** (the middle of the `stand` feet, art x 58 to 93) goes on the land point, the bottom
  edge of `LeftLandTarget` / `RightLandTarget` (the ledge tops, tile columns 3 and 13). The braced breath stance
  spans art x 52 to 104, so the back foot reaches into the wall and the front foot over the pit edge by under a tile.
- **Map points.** `LeftLandTarget` (943) and `RightLandTarget` (944) in `children`, 32x32, bottom edge on the ledge
  top; props `left_land_target`, `right_land_target` on `399`. New key `ConstKeys.LAND`.
- **Drips** hang from the bottom edge of the `Max` object (the ceiling face, row 2), x clamped to 0.5 tile outside
  the two land points (so a drip never starts in a wall). A drip over the sealed entry hole hangs 1 tile under the
  seal. Drips that fall into a pit are destroyed when they pass the `Min` point (`LavaDrop` has no cull of its own);
  the dragon destroys its drips when it is destroyed. The telegraph is the hanging drip itself (as `LavaDropSupplier`
  does) for 0.5 s plus a `SmokePuff` at the ceiling.
- **Aim mix: `OrbAim` used as is** (current, lead 1 s, near), as `GlacierMan` does. Item `158` now has 3 callers.
  The volley keeps its 1/2/3 balls by health and its 75 degree clamp; the 10 degree spread is replaced by the mix.
- **Sweep range +30 to -45** (the drawn frames), not -50 as in IDEAS: the -45 frame is the last drawn angle.
- **Charge tell with existing frames:** the fast `charging` wing beat (it was already shown), a drift back at
  0.5 tile/s and the dragon roar during the old 1 to 2 s wait. High lane: the charge target with the greatest y of
  the other side (the low-lane objects stay in the map, unused).
- **Head fixtures follow the head box of the cell that the state timer gives** (`MechaDragonArt.cellAt`, the same
  frame rule as `Animation`), not the drawn frame: gameplay does not read the renderer (`O2-R7`). The chomp's
  "snout damager" is the head part on chomp cells 2 and 3, whose head boxes lie on the floor.
- **Legs** hurt and take damage only in the ground layout (hang, drop, stand, breath, chomp, takeoff crouch). The
  dangling airborne legs have no fixture (as the old airborne fixtures).
- **Frozen or defeated on the ledge:** the airborne `frozen` / `defeated` pose stands on its lowest foot row (133 /
  135) at the ledge top.
- **Panic** (health at or below 25%, once, after the entry): the dragon roar; tells 20% shorter (breath, land hang,
  chomp, charge wait), the return sweep, chomp 100% in the pocket. The volley is already 3 balls there.
- **Hard:** breath tell 0.6 s, sweep 0.9 s, land hang 0.4 s, chomp tell 0.36 s, charge wait 0.8 to 1.6 s (it was the
  same as normal before), return sweep always.
- **Move choice** (`MechaDragonDirector`): after a hover, land when 2 hovers are done; else charge by the old pity
  rule (`nextInt(0, 100) <= chance`, +35 per hover without a charge); else land on 50%; else hover again. After a
  breath: chomp 70% with a player in the pocket (100% in panic), 15% with none; else a second breath on 30% (at most
  2); else take off. The pocket: a live player whose center is 1 to 5 tiles ahead of the land point.
- **Sounds (existing only):** `MM2_MECHA_DRAGON_SOUND` (roar, breath inhale, charge tell, panic), `QUAKE_SOUND`
  (stomp), `FLAMETHROWER_SOUND` (sweep start), `CHOMP_SOUND` (snap), `WHOOSH_SOUND` (takeoff); `MagmaExplosion`
  plays its own.
- **`fireTimer`** is made once (the spawn made a new `Timer` and runnable); the spit time is checked by crossing.
  So `MechaDragon.kt` leaves the spawn allocation baseline.

## For the play test

- The stomp shake strength (0.005 tile, as `D1000`), the drip timing and the pocket width are first guesses.
- The front foot of the braced stance over the pit edge, and the pose pop between `fly` and the hang pose.
- With a standing player, the lead ball and the current ball fly on the same line.

## Overlap with other branches

None found (2026-11-26). `188a` is the parent (art only).

## Sources

- Art: `188a` (`ANCHORS.json`, the frames). Design: `temp/dragon-moves/IDEAS.md` (main checkout).
