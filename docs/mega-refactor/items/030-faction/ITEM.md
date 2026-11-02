# 030: Faction and root owner instead of `owner == megaman`

- Status: planned
- Type: refactor
- Objectives: `O2`, `O8`
- Depends on: `029`, `005`
- Related: `014`, `023`, `045`, `048`, `120`, `121`
- Branches: none yet

## Goal

Decide who can hurt whom by faction, copied at spawn, not by `owner == megaman` checks.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `Faction` plus `ownerPlayerIndex`, copied at spawn from the root owner. Owner changes (reflect) update both.
- Replace the `megaman` owner checks and the `Megaman::class` keys (15 lines on 2026-10-31; the 37 `is Megaman` checks are gone) with faction or capability checks.
- One explosion-owner convention. Golden test first, because today's two conventions give different results.
- Enhancements and score read from the owning player's profile.

## Owner's input

Answer to review item 29 (2026-10-22, verbatim):

> yes

Item `029` research (2026-10-23, verbatim):

> This might be outside the scope of the current item, but it seems to me a mistake that entities like projectiles can have only one `owner`.

## Decisions

- (2026-11-02, owner, from `048`) One side per entity. No entity needs 2 different side values. `BulletBill` needs no special case: its side as an actor is enemy, and its source is a launcher that is not a player, so the 2 answers agree.
- (2026-11-02, owner, from `048`) Storage stays apart. The actor side is in `FactionComponent`. The source side is a snapshot beside `owner`, in the owner part that `048` step 9 moves. Slot identity for friendly fire is a third, separate fact (`SessionRules.kt:13`). The reasons do not depend on `BulletBill`: a shot needs a side fixed at fire time, and `FactionComponent` can mean "is an actor" only if shots do not have it. Readers such as `CartV2.kt:151`, `ToxicWaterfall.kt:137` and `CeilingCrusher.kt:160` ask that question.
- (2026-11-02, owner, from `048`) `FactionComponent` goes on actors only: players, enemies, bosses and boss parts. It answers hostility and actor tests only. Its first readers are the 14 lines in 8 files in `048` `NOTES.md`.
- (2026-11-02, owner, from `048`) Hazards get no side yet. `SmokePuff` keeps `IHazard`. A source with no faction must not count as neutral, or the 11 hazard shots with `OWNER pairTo this` fail.
- (2026-11-02, owner, from `120`) `DamagePolicy` (`120`) reads the faction. For a slot entity, the faction derives from the capture value, so "both" cannot be stored. The side stays read-only until `120`.

## Open questions

- Must Mega's own projectile explosions damage enemies? Today the default `explodeAndDie` path says no. (Decision E.)
- In co-op, can player shots hurt or push other players?
- Is `Faction { PLAYER, ENEMY, NEUTRAL }` enough if PvP comes (`102`)? All players are one faction in that enum. Do teams go in the faction, or in a separate team ID?
- This item plans to read enhancements and score from "the owning player's profile". `007` and `047` plan one session profile, so `ownerPlayerIndex` selects nothing for enhancements. What must credit go to: the slot for score only, or nothing?
- Do reflected shots switch faction, as `ReactorManProjectile` does today?
- One `owner` per entity is probably not enough (owner's input, 2026-10-23). Keep an instigator (the player or entity responsible) and a causer (the entity that hit), or an owner chain? Examples: a reflected shot, an explosion spawned by a projectile, a shot from a turret that a player placed. See `029` `NOTES.md`, "Co-op gaps and ideas", and the `owners` idea in `NOTES.md` here.

## Sources

- `NOTES.md` in this folder.
