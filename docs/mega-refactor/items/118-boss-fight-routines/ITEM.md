# 118: Boss fights as routines, rules and leases

- Status: active
- Type: refactor
- Objectives: `O1`, `O4`, `O2`
- Depends on: none
- Related: `005`, `006`, `012`, `013`, `015`, `023`, `030`, `038`, `042`, `047`, `048`, `050`, `053`, `055`, `057`, `082`, `102`, `108`, `109`, `120`
- Branches: `118a` (merged), `118b` (merged), `118c` (merged)

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

Phased (owner, 2026-11-02): one boss first, then batches. The old flow stays until the last batch, so each branch ships alone. The batches and their reasons are in the `118a` `SCOPE.md`, "Later branches".

1. `118a` `fight-routine`: the core types and the `stageBoss` preset, with `BigAssMaverickRobot` as the only boss on the new flow. A route at `ENTER_BOSS_ROOM` sends every other boss to the old flow.
2. `118b`: `TimberWoman`, `MoonMan`, `ReactorMan` (6 fights). Adds `midBoss`.
3. `118c`: `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan`, `PreciousWoman` (10 fights).
4. `118d`: `ElecDevil`, `GutsTank`, `SigmaRat` (2 fights).
5. `118e`: `MoonHead`, `MechaDragon`, `ReactorMonkey`, `Bospider`, `PenguinMiniBoss` (5 fights). Adds `miniBoss`.
6. `118f`: `WilyFinalBoss` (1 fight). Adds `finalBoss`, and removes the `wily_room` check.
7. `118g`: remove the old screen branches, the route, the guards, and `BOSS_READY`, `BOSS_DEAD` and `BEGIN_BOSS_SPAWN`. Keep `END_BOSS_SPAWN`, `BOSS_DEFEATED`, `INTERMEDIATE_BOSS_DEAD`, the 14 hooks, `isReady` and `betweenReadyAndEndBossSpawnEvent`.
8. `118h`: `HudBars`, `BarSpec` and `BarSource`. `FillBossHealthBar` replaces `BossHealthHandler`, and `HealthFillType` goes.
9. `118i`: `BossComponent` replaces `ready`, `betweenReadyAndEndBossSpawnEvent` and `onReady`. Shared with `048` step 6. It changes AI gating by up to 1 or 2 frames in 10 files, so its contract keeps the same window or names the change.
10. `118j`: key-based gate unlock for `GauntletMaster` and bosses (`Gate.kt:196-226`). `GauntletMaster` does not become a `Fight`.

### Possible later, with no caller yet

`O1-R5` keeps these out until a real fight needs them. The design makes each one a new step or a new rule value, with no change to the core:

| Type | First caller |
|---|---|
| `Fight.cast` in place of `Fight.boss`, so that a fight has more than one boss at once; `AllDefeated` reads the whole cast, with an optional `group` | not named. `118a` holds 1 boss (`Fight.boss`), and `AllDefeated` has a `TODO` that points here |
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
5. An owner play test in the real boss rooms of the batch, then control fights. `RefactorTest118.tmx` comes only if a check needs a room that a real stage cannot give.

## Owner's input

2026-11-02, about the result "add no boolean; keep `mini`" (verbatim):

> This whole setup is bad and forces a narrow set of things that we can do. Dispatch an agent to probe deep into redesigning this significantly. Check to see if an item already exists for it.

2026-11-02, about the rollout (verbatim):

> Let's start the scoping for 118. The approach should be phased: just one boss (the intro-stage boss for simplicity), then batches of other bosses so it's easy to review and test.

2026-11-02, about a health refill at the start of a fight (verbatim):

> I assume so. The boss-setup logic in `BossComponent` should allow a way to enable/disable a setup step like this via a simple boolean.

## Decisions

- (2026-11-02, owner) Item `118` uses the design "`Fight` plus routine, rule values and leases" (design D in `NOTES.md`). No existing item covered this redesign.
- (2026-11-02, owner) `Fight` is a plain object, not an entity. `MegaLevelScreen.kt:743-745` turns off `UpdatablesSystem`, so an entity cannot run its own freeze, and a fight can outlive each participant. This replaces the earlier "invisible fight entity". `BossComponent` stays on participants.
- (2026-11-02, owner) A setup step is included or omitted by the plan, not by a boolean on `BossComponent`. A refill is a step. On an ordinary boss, the refill at `AbstractHealthEntity.kt:63` stays, because it is the reset of a pooled entity.
- (2026-11-02, owner) `013` loses its boss states. A fight runs inside `PLAYING` and across transitions. `013` calls `cancelAll()` at the level reset.
- (2026-11-02, owner) `118a` adds `Step`, and `082a` reuses it. They are one shared type in `engine`, next to `state`.
- (2026-11-02, owner) The first variation after `118a` is the `082b` boss talk, as a `say` step in a plan. `082b` needs no hook in `AbstractBoss`.
- (2026-11-02, owner) The work is phased: `BigAssMaverickRobot` first, then batches. This replaces "move all 25 fights in `118a`".
- (2026-11-02, owner) The `WilyFinalBoss` batch removes the `isLevelDefeated` check for `wily_room`, as a named intended change. The research infers that normal play cannot reach it: the write at `MegaLevelScreen.kt:829` runs on `END_LEVEL`, and `Wily` ends with `GAME_COMPLETE`.
- (2026-11-02, owner) `GauntletMaster` does not become a `Fight`. It shares only the key-based gate unlock in `118d`.
- (2026-11-02, owner) Each branch merges directly into `master`, because each one ships alone.
- (2026-11-03, owner) `FightDirector.start` does not cancel an open fight, and no `TODO` marks it. Today a fight cannot start while another one is open (`118c` `NOTES.md`). The `fights` README states it as a rule. A branch that adds a new kind of plan or a new start path (`118e` mini bosses, `118f` final boss) checks the rule in its scope.

## Open questions

- What is a mid-level boss's rematch rule? Today a re-entry spawns the boss again (inference). `118a` keeps this. Decide when `FightMemory` gets a caller.

## Sources

- `NOTES.md` in this folder: the reports "Boss setup booleans" and "Boss fight redesign". They hold the 15 design faults, the 4 designs, the stress table of variations, the mapping of the 18 bosses, the API tables and the risks.
