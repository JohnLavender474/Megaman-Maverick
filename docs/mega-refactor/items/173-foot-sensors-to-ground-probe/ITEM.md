# 173: Copied foot-sensor pairs to `GroundProbe`

- Status: idea
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `079`, `042`, `171`
- Branches: none yet

## Goal

8 enemies copy a `CONSUMER` foot-sensor pair to find the ground ahead: `RedKoopa`, `RedKoopaShell`, `DarkSerket`,
`RatRobot`, `DrillTankXT`, `FireMet`, `SpikeBot`, `Darspider` (also `BunbyTank`). `079a` added the shared world probe
`GroundProbe` (`entities/utils/`). Move these enemies to `GroundProbe.checkAhead`, so that one helper owns the ground
check.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

The behavior is not the same, so each enemy needs a decision and a test room:

- The probe counts only `STATIC` solids, so a moving platform becomes a pit.
- The probe never counts `COLLIDE_UP_ONLY` as ground.
- The probe uses one `lead` in place of each sensor offset.
- The probe sees `DEATH` where `SpikeBot` and `Darspider` do not.

## Owner's input

None yet.

## Open questions

- For each difference above: change `GroundProbe`, or accept the new behavior for that enemy?
- One branch for all 9 enemies, or one per group?

## Sources

- `NOTES.md` in this folder.
- Branch `docs/TODO.md` and `docs/SCOPE.md` of `079a`:
  `items/079-smarter-enemies/branches/mega-refactor-079a-smarter-met/docs/`.
