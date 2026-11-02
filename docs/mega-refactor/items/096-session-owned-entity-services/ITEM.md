# 096: Session-owned entity factories and lists, in place of statics

- Status: idea
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `016`, `021`, `045`
- Branches: none yet

## Goal

Move the session objects that are still static to objects that the session owns, so that `O1-R1` holds.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `MegaEntityFactory` (306 lines in 185 files), `EntityFactories` (73 lines in 52 files), `MegaGameEntities` (62 lines in 45 files).
- Per-level state in companions: `Starkner.ALIVE`, `AstroAssAssaulter.FLAGS`, `SpiderWeb.WEBS_STUCK_TO_MEGAMAN`, `ToxicWater.waveAnimator`.

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

- Must two sessions run in one JVM? A desync check for `095` can run both peers in one process. This needs no shared statics, so it adds `GameObjectPools` to the list of `MegaEntityFactory` and `MegaGameEntities`.
- `SpiderWeb.WEBS_STUCK_TO_MEGAMAN` is one set for all players, and the unlock acts on `targetPlayer` (`SpiderWeb.kt:75,123`). Does it move to the session, or to a component on each player?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
