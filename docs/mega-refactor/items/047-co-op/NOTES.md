# Notes: 047 Co-op with multiple controllers

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

All controller and behavior logic assumes one player. Add support for more than one controller
and a co-op mode.

## `047b` scan (2026-11-01)

Line numbers are from `master` at `3e05f8cff`. These are recommendations from the scan. The owner's decisions are in `ITEM.md`, "Decisions". The `AbstractBoss` component and the hold time are not decided yet. The branch list still fits `108` and `109`, because all 4 topics apply to both.

Owner's input (2026-11-01, verbatim):

> Since friendly fire is one of the open questions, I think it needs to be part of a suite of toggles that each change value depending on whether the gameplay is single player, co-op, or PvP.

> I imagine many entities will have different rules about which player to target, and so being able to have a component/decorator/inheritance setup might be good.

### Findings

- Friendly fire: `Megaman.canBeDamagedBy` refuses only the player's own shots (`Megaman.kt:426-432`). `Bullet.kt:142` and `ChargedShot.kt:145-148` explode when a target refuses damage. So a change to `canBeDamagedBy` alone makes shots explode on a teammate, and they do not pass through. A teammate's shield also deflects a shot (`Bullet.kt:156`). One filter in `ProjectileRule.begin` covers all projectiles, for `BODY` and `SHIELD`.
- The owner check goes 2 levels deep. `047b` can use the same check with a `TODO` for `030`.
- Targeting: 613 `targetPlayer` lines in 145 entity files go through `MegaPlayers.targetFor`, which ignores the seeker. `hasBody` does not test if a player is alive.
- A new default also reaches readers that are not enemy aim. 23 files in `blocks`, `sensors`, `special`, `hazards` and `projectiles` read `targetPlayer`, for example `Gate`, `Lift` and `RisingLavaRiver`. Each one needs a review.
- `ChargedShot.kt:118-119` reads `targetPlayer`, so a nearest default makes its known fault worse. It must read the owner first.
- Pickups: each item sends the slot. `MegaLevelScreen.kt:621-644` drops it, and `PlayerStatsHandler.kt:58` acts on slot 0. Lives and currency are one shared pool.

### Recommended: session rules

A typed, read-only rules object, chosen at session start and read through `MegaServices`. This agrees with `O2-R8`. `047b` builds only the friendly fire toggle, because it is the only one with a reader in that branch. The other toggles come with the branches that read them. The current behavior is the PvP value of friendly fire.

| Toggle | Single | Co-op | PvP | Branch |
|---|---|---|---|---|
| Friendly fire | off (no effect) | off, shots pass through | on | `047b` |
| Lives | one pool | shared or per player | none | `047c` |
| Result of a death | level reset | the 3 options in `ITEM.md` | respawn | `047c` |
| Respawn place and delay | checkpoint | near the lead | spawn point choice | `047c` |
| Gravity scope | world | world or per player | to decide | later |
| Damage between players | not used | not used | size score or per weapon | `023`, `024` |

A team field has no reader with 2 players. It waits for `109` and `030`.

### Recommended: targeting

- Use the component and policy design in `029` `NOTES.md`, "Targeting design": `TargetPolicy`, and `TargetingComponent` with a default when the component is absent. A decorator such as `Sticky(Nearest)` is a policy that wraps a policy. Inheritance is rejected (`048`).
- `targetFor` stays as the temporary dispatch point, so the 613 lines need no edit. The removal of `targetPlayer` from entities with no target is a separate change.
- The default is `Nearest`, with no stored state. A target can flip when the two players are at almost equal distance, so the play test must check this.
- The first user of the component is `AbstractBoss`, with `Sticky(Nearest)`, and the hold lasts until the target dies. The scope step must confirm this against each boss.

### Other recommendations

- `047b` builds the small rules object now, and `102` extends it.
- `--mode coop|pvp` chooses the rules. `--players 2` defaults to co-op. `--mode pvp` with 1 player is a launch error.
- Health and weapon energy go to the player who collects them. Lives, currency, heart tanks and health tanks stay shared. Slot 1 has no HUD until `047d`.
