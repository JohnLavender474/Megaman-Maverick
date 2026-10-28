# Notes: mega-refactor-016a-typed-level-state

## Decisions

- 2026-10-28 (owner, gate 1, "Scope approved"): the 13 recommendations in `SCOPE.md` "Open questions" are
  accepted: `val levelState = LevelState()` on the game; the name `levelState`; the 11 internal keys in scope and no
  `IPropertizable` on the game; the kept game functions change only their bodies; call sites use
  `game.levelState.<field>`; camera flags as private fields; the 2 unreachable differences accepted; a public
  `weaponsAttained` on the game; the 11 unused `ConstKeys` removed; key names kept; package `levels`; real rooms for
  the manual checks; the temporary suite against a real game object.
- 2026-10-28 (owner, gate 2, "Manual checks pass. Contract approved"): `CONTRACT.md` approved. The owner ran the
  manual checks before any code change; that run is a baseline run on the old code. The contract play test after the
  implementation is still required.

## Overlap with other branches

- 2026-10-28: no other code branch is active. The merge target is the parent `mega-refactor-016-master`
  (coordinator message, 2026-10-28: the branches of `016` merge into the parent, "Parent branches" in
  `docs/mega-refactor/CLAUDE.md` on `master`). The branch start `44c6b5fa3` is an ancestor of the parent
  (`06901895e`).
