# Process log: mega-refactor-029a-player-seam

## Status

- State: active
- Goal: Player slot, player view and players service in the engine; the game view `MegaPlayer`; `Megaman` registered in slot 0
- Done: planning approved; step 1 (branch start).
- Next: step 2 (baseline).
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
- Plan approved by the owner, with option (a): the dev start applies a level's override.
- Step 1: branch start on `master`, worktree created.
