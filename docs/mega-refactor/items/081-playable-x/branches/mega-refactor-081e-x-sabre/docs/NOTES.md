# Notes: mega-refactor-081e-x-sabre

## Decisions

- Owner (2026-11-09, X6 intro play test): the wall kick went too far, because its side speed was tied to the
  walk (6 t/s after the feel pass); it is 5 t/s again, as a value of its own. The dash was almost the walk speed
  (8 vs 6); it is now 11 t/s with a 15 t/s start ("moderate"; X6 has about 2x the walk). The stage and D-1000 issues
  of the same play test go to a new branch `081f`.

- Owner (2026-11-09, play test): a slash that is cut on a landing is jarring. A running slash now switches kind
  on a state change (all pairs) in place of an end; this is a break from X6, which ends it. The progress maps by
  phase: each kind names its first frame with art, the last swing frame and the last frame with art, and the time
  keeps the same phase and the same fraction of it (`SabreKind.timeIn`). Same elapsed time was rejected: the arc jumps
  between small and large, and goes off for a frame. On a switch to the floor slash X stops, as at a floor start. The
  floor jump cancel reads the mapped time. The hit rule stays the target damage timers. The longest switched slash is
  0.367 s (0.267 s with no switch); its art shows for at most 0.25 s.

- Owner (2026-11-09, play test): X feels slow and floaty; make him snappy like Megaman. First pass: the jump,
  wall kick and knockback heights stay (2.4, 1.85 and 0.42 tiles), and the times go down. A full jump takes about
  0.62 s (was 0.8 s): 0.34 s up and 0.29 s down. The owner found the first try (rise 53.3, fall 75, fall limit 18;
  0.55 s) slightly too aggressive, so the values are now midway. The gravity is the rise value while X moves up, else the fall value.
  The owner gives their own changes after this pass.

- Owner (2026-11-09, play test): the sabre hold frame is 0.05 s on the floor and the wall; the total time may differ
  from X6. `UP` + `B` in the ground dash or its end slide ends the dash and starts a floor slash. X's air dash is
  faster and longer: 0.4 s, 10 t/s, `SELECT` start 14 t/s (the "moderate" option; about 4 and 4.6 tiles).

- Owner (2026-11-09, at the diff review): the sabre-only rule is a damage gate (`canBeDamagedBy` of `SabreBlock` and
  `MetaWheelF` accepts only `MegamanXSabreSlash`), not a 0 amount in the damage table.
- Owner (2026-11-09): the sabre does a static damage for every slash kind (X6 value 9 before the scale), with a `TODO`.
- Owner (2026-11-09): X's gameplay in Maverick is its own; X6 is only the template, so the link to the X6 values is not
  needed. `X6Damage` and the `X6_HEALTH` constants are removed. Each target hard-codes its damage (the same values as
  the old scale): `Junkroid` buster 8, half charge 15, full 30, sabre 23; `HoverGunner` 18, 30, 30, sabre 30;
  `SabreBlock` and `MetaWheelF` sabre 9.
- Owner (2026-11-09): the "X6" prefix goes from the code names (not from the music, the level `X6_INTRO`, its map
  files or the comments). The new names say what each class is, with a `TODO` to merge it with the Maverick class that
  does the same: `X6Debris` -> `DebrisPiece` (`BlockPiece`, `WoodCratePiece`), `X6Scenery` -> `Wreck`
  (`MockSpriteEntity`), `X6Shot` -> `JunkShot` (a shared simple enemy shot), `X6Frames` and `X6FrameDef` ->
  `TickFrames` and `TickFrameDef` (the engine `Animation` in seconds). `X6IntroEnemiesTest` keeps its name (the level
  keeps its name). The atlas folders `JunkShot/` and `Wreck/` and the `Wreck` spawner in the intro map follow.

- D1 (owner, 2026-11-06, in `081d`): the sabre has its own branch, after `081d`.
- D2 (owner, 2026-11-06, in `081d`): the sabre slashes on `UP` + `B`. No new button. The sabre scan of `081d` is in the
  `081d` branch `docs/SCOPE.md`; art drafts are in `temp/x-sabre-gifs/` and `temp/mmx-sabre-slash-custom/`.

## Overlap with other branches
