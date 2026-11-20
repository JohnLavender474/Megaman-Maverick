# Notes: mega-refactor-077g-glacier-man

## Decisions

- 2026-11-19 (Claude, at the scan): test rooms `glacier_man` and `glacier_man_boss` in `RefactorTest077.tmx`, as
  `077c` did for `ElecDevil`. Reason: a check starts at the boss with no stage run.
- 2026-11-19 (Claude, at the scan): the mixed aim reuses `OrbAim` from `bosses/bigassmaverickrobot/` (its 3rd caller),
  with no move. A move to a shared package is a backlog candidate.
- 2026-11-19 (Claude, step 5): the timed drop uses the existing keys `ConstKeys.DELAY` (the still time, a `Float`;
  its presence turns the timed drop on) and `ConstKeys.DURATION` (the shake time, a `Float`; default 0.25 s, the
  `master` value), and `ConstKeys.STATE` = `FallingIcicleState.FORM` for the form strip. The form time is fixed by the
  art (4 × 0.15 s), not by the caller. Reason: no new key (`O1-R4`), and with none of these props the `STILL` →
  player under → `SHAKE` path is the `master` code.
- 2026-11-19 (Claude, step 6): the ceiling line is a scan of the `Block` bodies (`EntityType.BLOCK`), once per icicle
  at its spawn: the lowest block bottom over the icicle's column that is at or above the top of Glacier Man's body;
  with no block, 14 tiles above his feet (the top of the aim arena). Reason: D1 forbids a map change, the code has no
  ceiling read, and the scan gives the stepped ceiling of `GlacierMan_16x14_v2.tmx` `boss_room` (row 3 in the middle,
  row 4 at the sides) with no map data. `FloatingCanHole` is an enemy, not a block, so icicles form in its columns
  (D1).
- 2026-11-19 (Claude, step 6): the wave uses 1-tile columns between the arena walls. The lead icicle takes the column
  of the `OrbAim.LEAD` point (row `icicleLeadTime`); the safe gap is 2 columns (2.5 tiles free between the icicle
  bodies), at a random place that is not the lead column and not under Glacier Man's body (the player cannot stand
  there); the other icicles take random free columns. The lead icicle forms first, then the others in x order from
  Glacier Man's side of the arena outward.
- 2026-11-19 (Claude, step 6): the pound strip is the tell of Icicle Rain (frames 0 and 1 before the impact), so the
  move has no `TELL` state and no tell field, as `DUCK`.
- 2026-11-19 (Claude, step 6): the double rain pounds twice, one wave per pound, and the second pound waits until no
  icicle of the first wave is alive. Reason: each wave has its own safe gap, and the player must not have to stand in
  both gaps at once.
- 2026-11-19 (Claude, step 6): the defeat shatters the live icicles of the boss (`explodeAndDie`), and a destroy of
  the boss (a death and respawn in the room, `LEVEL_RESET`) removes them. Reason: no icicle hits the player after the
  win, and the restarted fight starts with an empty ceiling. A `FIRED` stun stops the icicles that have not spawned;
  the spawned icicles keep their timed drop.
- 2026-11-19 (Claude, step 7): Sled Ram fires its snowballs on the way back, when Glacier Man crosses the middle of the
  arena: one straight forward and one straight back (the `master` straight velocity). Reason: at the bounce he is at
  the wall, so a shot toward the wall hits the wall at once. After the second wall (or 3 s at most) he brakes as after
  the sled. The body clamp in x goes up to the ram speed only in the ram (the jump keeps its 10 tiles/s clamp).
  `FIRED` interrupts it as the other moves. 2026-11-20 (owner, play round 1): no snowballs in Sled Ram; the
  snowball part of this decision is void.
- 2026-11-19 (Claude, step 8): the phase-2 pose is a state `PHASE_START`. The roll-over only sets a pending flag; the
  pose starts at the first update where he is on the ground and not in `INIT`, `JUMP` or `FIRED`, and it ends the move
  in progress as a `FIRED` stun does. Reason: a pose in the air or in the intro looks wrong, and the stun of a fire hit
  stays as on `master`. He takes damage during the pose, as `DesertMan` and `ElecDevil` at their phase start. The
  flash is a cyan sprite tint that blinks every 0.1 s (the `ElecDevil` pattern; cyan for ice). Sound
  `CHILL_SHOOT_SOUND`; the shake has the values of the pound with 0.5 s (the `077b` phase shake time).
- 2026-11-19 (Claude, step 9): the defeat time is the sequence (1.65 s) + `AbstractBoss.DEFAULT_DEFEAT_DURATION`
  (passed as `defeatDur`, the `BigAssMaverickRobot` pattern); `AbstractBoss` does not change. The explosions start at
  the end of the shatter, and the sprite is hidden from then on (the last shatter frame shows the block in pieces, so
  nothing of him is left). The shards use the `IceShard.spawn5` default tag (`BreakableIce` shards, the art of a
  breaking ice block). The old `defeated` strip stays in the atlas, unused.

## Overlap with other branches

Checked 2026-11-19 at branch start (the diffs and the uncommitted files of each worktree):

- `mega-refactor-078d-reactor-man-art`: frames and atlases of the toxic water and Reactor Man; `README.md` credits.
  No shared file with this branch, unless this branch edits the credits (it does not plan to).
- `mega-refactor-109a-screen-wrap`: docs only so far.
- `mega-refactor-060k-protoman-frame-edits`, `compactor-level`: no shared file.
- `fix/projectiles-ignore-death`: `DeathRule.kt`; no shared file.
- `077d` and `077e`: planned, no branch.
