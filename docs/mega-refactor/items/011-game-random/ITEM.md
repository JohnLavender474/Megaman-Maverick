# 011: One seedable, interceptable random source with named streams

- Status: done
- Type: refactor
- Objectives: `O2`, `O6`
- Depends on: none
- Related: `008`, `010`, `016`, `055`
- Branches: `mega-refactor-011a-game-random` (merged 2026-11-15)

## Goal

Route every random call through one proxy that can use a fixed seed or a stubbed value.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Add a `GameRandom` interface in `engine` with seeded, scripted and recording implementations.
- Gameplay and cosmetic streams, seeded from `--seed` (`008`) or from the replay header.
- Delegate `getRandom*` to the gameplay stream. Delete `superRandom`. Fix `OrderedSet.random()`, which reorders the set. Move `.random()`, `.shuffle()` and `MathUtils.random` calls to the streams.
- A CI check that rejects new direct random calls outside the RNG package.
- Replace the wall-clock spawn-time ordering (`MegaGameEntity.kt:54`) with a counter.

## Owner's input

Answer to review item 10 (2026-10-22, verbatim):

> I agree with this: a proxy should exist for all "random" calls so that we can intercept them with either a fixed seed or an outright stubbed value

## Open questions

- Must the RNG state go into save files? (Probably not.)

## Decisions

- (2026-11-14, owner) Merge target: each branch of this item squash-merges directly into `master`.
- (2026-11-14, owner, `011a` `D1`) Access: inject `MegaServices.random: GameRandom`; a helper with no `services` takes a `GameRandom` parameter.
- (2026-11-14, owner, `011a` `D2`) One stream, not gameplay and cosmetic streams. A split is a later decision.
- (2026-11-14, owner, `011a` `D3`) A clock seed per run, printed at start, and `--randSeed <long>` to fix it (renamed from `--seed` in the `011a` diff review, 2026-11-15).
- (2026-11-14, owner, `011a` `D4`) Guard: a `core` source-scan test (`RandomSourceGuardTest`). A type-aware check is item `161`.
- (2026-11-15) Done with `011a`. The scripted and recording sources of the proposed scope have no caller yet; they go with replay (`055`).

## Sources

- `NOTES.md` in this folder.
