# Notes: mega-refactor-118c-batch-2

## Decisions

- (2026-11-03, owner) No cancel in `FightDirector.start`, and no `TODO`. The README states the rule. See "Can a fight start while another one is open?".

## Can a fight start while another one is open?

Asked for `WILY_STAGE_3`, where gate `578` is the exit of the `PreciousWoman` room and the entry of the `TimberWoman` room. After `118c` both fights are on the new flow, and this is the only place where 2 new-flow boss rooms touch. `FightDirector.start` replaces the current fight with no cancel (`FightDirector.kt:17-25`).

Today it cannot happen, and if it did, no lease would stay held:

1. The fight closes before the player can reach the next gate. The gate opens on `INTERMEDIATE_BOSS_DEAD`, which `run` delivers at the end of frame E+1. `AwaitBossGone` ends in the tick of E+2, and the fight closes in that tick. The walk to the gate and the room transition take many more frames.
2. An open fight at that point holds no lease. `ReleaseWorld` releases the world hold at the end of the intro, and `HideBossHealthBar` releases the bar at the defeat. Both run before `ExitMidBossFight`.
3. The one path that keeps a lease held cannot reach the next room. With slot 0 dead at the defeat, the bar lease stays and the fight stays open, but no exit event goes out, so the gate stays shut. `PLAYER_SPAWN_INIT` cancels the fight.

These facts hold for the current plans and maps, but no code enforces them. They hold because each moved fight's room is shut by a gate that opens only on the fight's exit event, after the leases are released. A later plan with no gate, or a boss that the game spawns in another way, could break them.

## Overlap with other branches
