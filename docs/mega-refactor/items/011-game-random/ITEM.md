# 011: One seedable, interceptable random source with named streams

- Status: planned
- Type: refactor
- Depends on: none
- Related: `008`, `010`, `016`, `055`
- Branches: none yet

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

- Two streams (gameplay, cosmetic) or more (for example one per boss)?
- In normal play, a random logged seed per run, or a fixed seed?
- Must the RNG state go into save files? (Probably not.)

## Sources

- `NOTES.md` in this folder.
