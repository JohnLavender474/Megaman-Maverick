# Process log: mega-refactor-029a-player-seam

## Status

- State: planned
- Goal: Player slot, a small `IPlayer` contract and a players service (engine interface plus base class); `Megaman` is the only player, in slot 0
- Done: scan; `docs/SCOPE.md` and `docs/CONTRACT.md` approved; draft `docs/PLAN.md`.
- Next: the owner reviews `docs/PLAN.md` (one open question: the test level loadout).
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-23

- No branch exists yet. The folder was prepared on `master`.
- Scan done on `504bcd015`. Draft `docs/SCOPE.md` written.
- Draft scope revised after discussion: no `IPlayer` on the entity; engine `PlayerView` interface (choice A), `Players<V>` with inline helpers, no position in the engine; game view `MegaPlayer` resolves required components at registration; capacity 1.
- Scope: added the `IMegaGameEntity.players` extension and where `game.players` lives (owner's request).
- Owner answered the open questions: names approved, new `ConstKeys.SLOT` (and `SLOTS`), `STUN_PLAYER` carries target slots (room shake: all players). Scope updated.
- Scope approved by the owner. Draft `docs/CONTRACT.md` written.
- Scope (with the test map) and contract approved by the owner. Draft `docs/PLAN.md` written.
