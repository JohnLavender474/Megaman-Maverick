# 119: `PLAYER_DESTROYED` and `LEVEL_FAILED`

- Status: planned
- Type: refactor
- Objectives: `O2`, `O1`
- Depends on: none
- Related: `013`, `038`, `047`, `053`, `057`, `109`, `118`, `120`
- Branches: none yet

## Goal

Make the player death event tell the truth. `PLAYER_JUST_DIED` fires from `Megaman.onDestroy`, so it fires on 7 routes, and only 3 are deaths. With 2 live players, one slot 0 death gives 2 events. Its listeners want different facts: a level reset, an owner that left, or a counted death.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

| Event | Sent by | Meaning |
|---|---|---|
| `PLAYER_DESTROYED(slot)` | `Megaman.onDestroy`, with no held branch | This player entity left the world |
| `LEVEL_FAILED` | The screen, after its death rule | The level resets |

Steps:

1. Rename `PLAYER_JUST_DIED` to `PLAYER_DESTROYED` (13 lines in 7 files, 0 tests).
2. Add `LEVEL_FAILED`. Move `GauntletMaster` to it, and move `SpiderWeb` to it if it stays a slot 0 reset.
3. Add owner filters at `PreciousGem.kt:215` and `PreciousGemCluster.kt:109`. This fixes `docs/issues/2026-11-02-slot-1-death-acts-on-p1-objects.md`.
4. Tests: `REFACTOR_TEST_047` has the rooms `slot_0_death` and `slot_1_death`.

The `died` flag and `PlayerEventKeys.DIED` have no reader yet, so `120` adds them. `MegamanClone` (`121`) sends no player event: it is a normal enemy, and a fight watches it with `runnablesOnDestroy`.

### Routing table

| Case | Fact | Who acts | Level reset and life |
|---|---|---|---|
| Slot 0 death | `PLAYER_DESTROYED(0)` | Screen runs the rule and sends `LEVEL_FAILED`. Gems filter by owner. | yes, 1 life |
| Slot 1 co-op death | `PLAYER_DESTROYED(1)` | Owned entities clean up. Screen does nothing. `047c` adds respawn. | no, no life |
| Captured P2 death (`120`) | Same event, no special path | The fight filters on its own P2 and `died`, then restores the role. | no, no life |
| Level reset of other slots | `PLAYER_DESTROYED(1)`, `died=false` in `120` | Owned entities clean up. A fight ignores it. Screen drops it. | already running |
| Level end, screen change | Same event | 0 receivers | no |
| Fall, any slot | `died=true` in `120`, same as that slot's row | Same as that slot's row. No orb ring, as today. | by slot |

## Owner's input

2026-11-02, about a held P2 that skips the event (verbatim):

> Is this setup accurate? Or should the event be fired anyway and receivers of the event determine what to do? Is the `PLAYER_JUST_DIED` event even appropriate at this point, or should it be renamed or replaced?

## Decisions

- (2026-11-02, owner) The event fires always, and receivers filter it. A held or captured player gets no skip. The skip would treat a level reset as a P2 death, and it would leave P2's gems with no cleanup.
- (2026-11-02, owner) Rename now. Do not wait for `057`, which owns the typed payload later.
- (2026-11-02, owner) The entity keeps the entity fact, and the level owner sends the level fact. `038` takes over the `LEVEL_FAILED` submit when its routine exists. `013` later owns the transition.
- (2026-11-02, owner) A fall counts as a death (`died=true`).
- (2026-11-02, owner) If both sides die in one tick, the level rule wins, and the fight ends with no result.
- (2026-11-02, owner) `047c` owns any listener filter that this item does not finish, because a death rule change needs them.
- (2026-11-02, owner) A `SpiderWeb` that is not stuck to a player still goes on a slot 0 death, through `LEVEL_FAILED`. It has no live spawner, so this has low priority.

## Open questions

## Sources

- `NOTES.md` in this folder.
