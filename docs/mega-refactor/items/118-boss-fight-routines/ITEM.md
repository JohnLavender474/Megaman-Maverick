# 118: Boss fights as routines, rules and leases

- Status: planned
- Type: refactor
- Objectives: `O1`, `O4`, `O2`
- Depends on: none
- Related: `005`, `006`, `012`, `013`, `015`, `023`, `030`, `038`, `042`, `047`, `048`, `050`, `053`, `055`, `057`, `082`, `102`, `108`, `109`, `120`
- Branches: none yet

## Goal

Make a boss fight a value that can be composed, not a fixed chain. Today one boolean (`mini`) decides 8 behaviors, the level screen owns the fight logic, and a win means that one entity reaches 0 health. So a new kind of fight needs a screen edit and a new base class hook.

After this item, one `Fight` object owns the life of a fight, one routine owns its order, win and lose are values, and each concern (bar, freeze, lock, music) is a part that a plan includes or omits.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The research is in `NOTES.md`.

```kotlin
class Fight(val key: String, val plan: FightPlan, val services: MegaServices) {
    val cast = Array<MegaGameEntity>()
    private val leases = Array<Lease>()
    fun lease(l: Lease) { leases.add(l) }
}

class FightPlan(
    val intro: Array<Step<Fight>>,
    val win: FightRule, val lose: FightRule?,
    val outro: Array<Step<Fight>>
)

sealed interface FightRule {
    data class AllDefeated(val group: String? = null) : FightRule
}

interface Lease { fun release() }
```

Rules of the design:

- **Order:** one `Routine` of steps. It receives no event for its order. Events stay as output for listeners.
- **Leases:** a step changes shared state only through a lease, and `Fight` releases the leases at close, in reverse order. So a freeze, a lock or a bar cannot stay on after a death or a skip.
- **Rules:** a rule ends the wait. A step never polls the win itself, so win, lose and a timer are parallel values, and the order stays linear.
- **No loop and no branch in the step type.** A loop is an FSM in a step (`015`). A branch is a plain `if` in a step. This stops the list from turning into a scripting language.
- **`mini` leaves.** It stays only as a legacy map key that picks a preset, until `022`.
- **`FightPlans.resolve(bossName, mapProps)`** is the one source for fight facts. It runs before the spawn, because `spawn` only queues and `onSpawn` runs later.
- **Per-participant data** stays on the participant as `BossComponent` (`048`).

Presets for today's game, from 25 live fights: `stageBoss` (11), `midBoss` (8), `miniBoss` (5), `finalBoss` (1).

### Proposed branches

1. `118a` `fight-routine`: move the 6 branches at `MegaLevelScreen.kt:691-804` (114 lines) into steps and presets. The screen keeps 1 `start` call and 1 `update` call. Add `Step`, `Routine`, `Fight`, `FightPlan`, `FightDirector`, `FightPlans.resolve`, the 3 presets, `Lease` and `FightRule.AllDefeated`. Remove the 12 forced prop lines, and `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN`. Keep `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `INTERMEDIATE_BOSS_DEAD`, the 14 hooks, `isReady` and `betweenReadyAndEndBossSpawnEvent`. All 25 live fights are its real callers. No boss AI changes.
2. `118b`: `HudBars`, `BarSpec` and `BarSource`. `FillBar` replaces `BossHealthHandler`, and `HealthFillType` goes.
3. `118c`: the `finalBoss` preset, and `BossComponent` replaces `ready`, `betweenReadyAndEndBossSpawnEvent` and `onReady`. Shared with `048` step 6. It changes AI gating by up to 1 or 2 frames in 10 files, so its contract keeps the same window or names the change.
4. `118d`: key-based gate unlock for `GauntletMaster` and bosses (`Gate.kt:196-226`). `GauntletMaster` does not become a `Fight`.

### Possible later, with no caller yet

`O1-R5` keeps these out until a real fight needs them. The design makes each one a new step or a new rule value, with no change to the core:

| Type | First caller |
|---|---|
| `say` step, `parallel`, `Routine.skip` | `082a`, `082b` |
| Second bar, group `BarSource` | not named |
| `setHealth` step, `FightMemory` (partial or carried-over health, rematch rule) | not named |
| `lockArena`, `lockCamera`, `followRooms` | not named |
| `swapCast`, `nextFight` | not named |
| `Survive`, `AnyOf`, `Fled` rules | not named; maybe `108` |
| `waves` step | `GauntletMaster`, if the owner wants it |
| `restorePlayer` step | `120` |
| Present-player steps | `047e` |

### Behavior check

No test covers boss timing. Layers 1 to 5 of `NOTES.md`, "Behavior check for the classic game", apply to `118a`:

1. A temporary contract suite: a frozen copy of the 6 `when` branches in test sources, and `Fight`, run on the same scripted frames, with an ordered trace per frame.
2. A matrix for that suite: 3 presets, the `Wily` `null` exit, the level-defeated skip, slot 0 dead at defeat and at dead, `PLAYER_SPAWN` during the intro, gate unlock.
3. A permanent suite: routine order, each lease released on interrupt, one `resolve` call per fight.
4. An ordered log diff for one play of each preset.
5. An owner play test on `RefactorTest118.tmx`, then control fights.

## Owner's input

2026-11-02, about the result "add no boolean; keep `mini`" (verbatim):

> This whole setup is bad and forces a narrow set of things that we can do. Dispatch an agent to probe deep into redesigning this significantly. Check to see if an item already exists for it.

2026-11-02, about a health refill at the start of a fight (verbatim):

> I assume so. The boss-setup logic in `BossComponent` should allow a way to enable/disable a setup step like this via a simple boolean.

## Decisions

- (2026-11-02, owner) Item `118` uses the design "`Fight` plus routine, rule values and leases" (design D in `NOTES.md`). No existing item covered this redesign.
- (2026-11-02, owner) `Fight` is a plain object, not an entity. `MegaLevelScreen.kt:743-745` turns off `UpdatablesSystem`, so an entity cannot run its own freeze, and a fight can outlive each participant. This replaces the earlier "invisible fight entity". `BossComponent` stays on participants.
- (2026-11-02, owner) A setup step is included or omitted by the plan, not by a boolean on `BossComponent`. A refill is a step. On an ordinary boss, the refill at `AbstractHealthEntity.kt:63` stays, because it is the reset of a pooled entity.
- (2026-11-02, owner) `013` loses its boss states. A fight runs inside `PLAYING` and across transitions. `013` calls `cancelAll()` at the level reset.
- (2026-11-02, owner) `118a` adds `Step`, and `082a` reuses it. They are one shared type in `engine`, next to `state`.
- (2026-11-02, owner) The first variation after `118a` is the `082b` boss talk, as a `say` step in a plan. `082b` needs no hook in `AbstractBoss`.
- (2026-11-02, owner) `118a` removes the `isLevelDefeated` check for `wily_room`, as a named intended change. The research infers that normal play cannot reach it: the write at `MegaLevelScreen.kt:829` runs on `END_LEVEL`, and `Wily` ends with `GAME_COMPLETE`.
- (2026-11-02, owner) `GauntletMaster` does not become a `Fight`. It shares only the key-based gate unlock in `118d`.
- (2026-11-02, owner) Each branch merges directly into `master`, because each one ships alone.

## Open questions

- What is a mid-level boss's rematch rule? Today a re-entry spawns the boss again (inference). `118a` keeps this. Decide when `FightMemory` gets a caller.

## Sources

- `NOTES.md` in this folder: the reports "Boss setup booleans" and "Boss fight redesign". They hold the 15 design faults, the 4 designs, the stress table of variations, the mapping of the 18 bosses, the API tables and the risks.
