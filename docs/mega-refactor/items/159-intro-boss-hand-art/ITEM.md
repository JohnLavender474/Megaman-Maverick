# 159: New hand and head art for the intro boss

- Status: done
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `077`
- Branches: `mega-refactor-159a-intro-boss-art` (merged, 2026-11-15)

## Goal

New `BigAssMaverickRobot` hand art (an open palm, a clap pose, a slam impact frame) and a cracked head row for the death. Branch `077a` uses the old fist frames for every attack (they are a front view, so any angle reads) and redrew only the `Head/defeated` face (aghast, no X eyes).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

- 2026-11-15: "Let's start item 159".

## Decisions

- 2026-11-15 (owner): merge target `master` directly. Claude drafts each frame from the existing frames and palette;
  the owner approves or redraws each one before it goes into the atlas. First branch scope: slam impact, clap pose,
  open palm, cracked head row.

- 2026-11-15 (owner): the new fist frames are dropped: "I'm satisfied with the boss's fists as they are. Let's just
  add a 'crack' to the defeated head's sprite." The item is done with `159a` (the cracked head). The fist drafts stay
  in `temp/159a/` (not committed).

## Open questions

## Sources

- From the `077a` backlog candidates (2026-11-15).
