# Process log: mega-refactor-024a-weapon-defs

## Status

- State: active
- Goal: Refactor only: Megaman's 10 weapons become `WeaponDef` objects in a registry; Megaman plays the same
- Brief:
- Done: branch start
- Next: scan, `SCOPE.md`, `CONTRACT.md`, brief
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-14

- Branch start. Owner decision: the `024` branches squash-merge directly into `master`. Proposed split:
  `024a` (refactor only, Megaman's weapons as `WeaponDef`s), `024b` (a loadout per character; removes the
  `is Megaman` workarounds for X and Proto Man). `060h` builds on them.
