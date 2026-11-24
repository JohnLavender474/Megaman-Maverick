# 178: NPCs and AI allies that fight next to the players

- Status: active
- Type: feature
- Objectives: `O8`, `O2`, `O4`
- Depends on: `031`; later branches `006`, `010`, `015`, `030`, `112`, `128`, `082`
- Related: `029`, `047`, `079`, `081`, `102`, `108`, `109`, `113`, `118`, `120`, `128`, `171`, `175`
- Branches: [`mega-refactor-178a-bot-input`](branches/mega-refactor-178a-bot-input/process-log.md) (merged),
  [`mega-refactor-178b-ground-pathfinding`](branches/mega-refactor-178b-ground-pathfinding/process-log.md) (active)

## Goal

NPCs in levels, and AI allies that fight next to the players. An ally moves like a player character (it runs, jumps,
wall slides, wall jumps, air dashes and climbs ladders), follows and defends the players, and clears enemies. A first
pass proves the brain and the navigation; later branches add allies that are not players, and NPCs that talk.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The order, and the reason for each step:

### Phase 1: a bot in player slot 1

The bot is a real player in slot 1 whose input comes from an AI. Every player rule (moves, camera, room carry,
damage, enemy targeting) works at once, so the brain and the navigation can be proved with no faction work.

1. **`178a` bot input on slot 1.**
   - **What:** an AI input source writes slot 1's held mask in place of a pad (`MegamanMaverickGame.kt:617`), with a
     launch option to choose it. The first brain copies P1's input frames with a delay (as Tails in Sonic 2). It uses
     the slot 1 view-edge warp to catch up, and a test map `REFACTOR_TEST_178`.
   - **Why first:** this is the smallest end-to-end proof that an AI can drive a `MegaPlayer`. Item `031` already
     plans the `AiSource`. The brain needs no navigation, faction or targeting work yet.
2. **`178b` pathfinding strategies, and a ground path for walking.**
   - **What:** `PathfindingComponent` holds a strategy that the entity chooses. Today's grid search becomes the
     flyer strategy under a new name, and the 4 flyers play the same. A ground strategy reads a graph built from the
     TMX layers at level load, with walk and fall links. A follower on the main thread turns the links into button
     presses, and the bot walks to P1 by path in place of the input copy.
   - **Also (smart-bot scan, 2026-11-23):** the first perception layer (`BotView`, `WorldQuery` with an area query
     by fixture type and a line-of-sight test, `RoomStaticMap` from the TMX for the area past the view edge), the
     skill layer for the WALK and FALL links (timed button scripts that put the released frame before each press),
     brain resets on lock, warp, room transition and respawn, and a `--debugBot` overlay and trace (goal, path,
     skill).
   - **Why here:** the owner decided on separate implementations behind one abstraction (see "Decisions"). The
     ground strategy is the second caller that makes the abstraction real; with only the flyers it would be a
     speculative API. Walk and fall links come first because they need no jump simulation.
3. **Items `006` (headless boot) and `010` (one fixed tick), then `178i` bot course harness.**
   - **What:** a headless test loads a test map, runs the level with the bot at the fixed step, and checks a
     course spec (goal room or area, frame limit, no death, no warp), with a per-tick trace and metrics.
   - **Why before `178c`:** owner decision 2026-11-23. Jump links depend on hold times and arcs on one clock, and a
     course test checks them with no hand play test for each change.
4. **`178c` jump, wall-jump, air-dash and ladder links.**
   - **What:** the graph gets the links that need movement physics. Each link is checked with a simulated arc at the
     fixed step (`JumpPlanner`), for each move profile: Megaman and Proto Man share one, and X has his own.
   - **Why after `178b`:** the jump height depends on the A-button hold and on Y friction, so it must be simulated,
     not computed (`PlayerBody.kt:57-58`). It is the riskiest part of the navigation, and the walk graph shows first
     that the graph and the follower work.
   - **Also:** the skills for these link kinds (`JumpSkill`, `WallJumpSkill`, `ClimbSkill`, air dash).
5. **`178j` room graph and goals.**
   - **What:** a `RoomGraph` built at load from the rooms, gates, teleports and lock keys (side and vertical links),
     and a `GoalSource` that listens to the level events and gives the bot's goal: stay near the leader, reach the
     exit or gate the leader goes to, hold the arena in a fight or gauntlet, or wait. Optional Tiled goal objects for
     hard cases.
   - **Why:** a gate and a side exit wait for every player, so a bot that misses one blocks the human. Today only
     side neighbours are found (`CameraRoomExits.kt:38-65`).
6. **`178k` dynamic terrain.**
   - **What:** conditional links and hazard volumes that change with time, in the order of their use in the live
     levels: gates (14 levels), timed hazards (11), surfaces that come and go (9), moving platforms (6).
   - **Why:** the static TMX graph cannot hold open state or timing (`NOTES.md`, "Smart-bot scan").

### Phase 2: a brain that fights

7. **Item `015`: the common state machine.**
   - **Why before `178d`:** the brain has states (follow, traverse, recover, warp, fight) with timers. Without `015`
     it would be one more hand-made machine, and `O4-R3` asks new AI to use the common machine.
8. **`178d` ally combat.**
   - **What:** the bot shoots enemies and defends the human. It picks a target and an action with a weighted pick
     (a generic picker taken out of `D1000Director`, its second user). It keeps companion rules: it fires on
     permission, it never blocks the player's shots, and it does not take boss kills. It forecasts threats (body
     velocity and gravity, recomputed each frame for homing shots) and dodges.
   - **Open:** today the bot blocks P1's shots (`Bullet.kt:142`, `ChargedShot.kt:146-149`); the brief decides whether
     they pass through it, or whether the rule waits for `030` and `120`.
   - **Why the bot needs no faction work yet:** its shots already hurt enemies, and enemies already target it,
     because it is a player. Its target candidates are the live enemies and bosses, with a `TODO` to use the foes
     query of `030` and `112`.

### Phase 3: slot agents, so that a bot is not a human

9. **`178e` slot agents.**
   - **What:** each player slot (the identity of a hero on the field) gets a `PlayerSlotAgent` that drives it: a
     `HumanPlayerSlotAgent` (a human's input, settings, HUD and lives) or a `BotPlayerSlotAgent` (an AI brain).
     `MAX_PLAYERS` becomes a human limit and a slot limit. Then a bot death can be free, a bot can be kept from
     pickups, and 2 humans can each have a bot.
   - **Why here:** this is the first point where "a slot is a human" breaks: lives, HUD and `--players` all count the
     bot as a human. It changes the meaning of `O2-R5`, so the brief asks the owner first.
   - **Depends on:** `175` if it is merged by then.
10. **`178f` takeover.**
   - **What:** a human who joins takes over a bot, and gives it back on leaving (as Tails, Kirby helpers and Left 4
     Dead bots).
   - **Why after `178e`:** a takeover only replaces the slot's `BotPlayerSlotAgent` with a `HumanPlayerSlotAgent`;
     the entity stays the same. It
     depends on `047f` (join and leave).

### Phase 4: allies and NPCs that are not players

11. **Items `030` (factions), then `112` (targeting policies).**
   - **Why:** an ally with no slot needs a side (its shots hurt enemies, not players) and enemies that can choose it.
     `112` must select from "the foes of the seeker", not only from players.
12. **`178g` NPCs in their own room and view.**
   - **What:** an ally or escort that is not a player. It has the faction `PLAYER`, and the brief decides per
     mechanic which player-only checks it joins (carts, springs, crushers, stomps). It stays in its own room
     (decision below) and acts in its own view: a virtual camera (item `128`) keeps the blocks and spawns near it,
     and the 11 enemy AIs that act only in the real camera check "in any view". It is dormant while its room is not
     the current room.
   - **Why last of the core work:** it needs `030`, `112` and `128`, and the bot of phases 1 to 3 has already proved
     the brain.
13. **`178h` re-entry.**
    - **What:** the `TODO`s of the room rule: teleport the NPC on a room transition, and scripted events where it
      comes back into play later, with scenes from item `082`.

### Later, as their own items or branches

- Talking NPCs, shops and story scenes: item `082`.
- An ally that turns hostile: item `120`.
- Bots in PvP (`109`) and in Spec Ops (`108`).

## Owner's input

2026-11-22 (verbatim):

> I think it would be very cool to support NPCs in the game, including ones that fight alongside the player(s). This
> means that the NPCs will need to support autonomous control of running, jumping, wall climbing, air dashing,
> defending the player(s), helping to clear out enemies, and overall being "smart".

2026-11-22, on the pathfinder (verbatim):

> I was envisioning having a completely separate pathfinding implementation with a layer of abstraction so that the
> component/entity can choose which implementation to use.

2026-11-22, on the camera (verbatim):

> Ideally, NPCs are not looked into the current view but act in their own view. Having NPCs locked to a single room
> for the first-pass impl might be enough for now, and mark it a TODO to either (a) teleport them on room transition
> or (b) have scripted events where they re-enter the gameplay later on. A mix of both might be good to have.

## Decisions

- 2026-11-22, owner: dialogue and talking NPCs are wanted, but not in the first pass. Mark them with `TODO`s.
- 2026-11-22, owner: pathfinding has separate implementations behind one abstraction, and the component or entity
  chooses one. Flyers keep today's implementation under a new name. Grounded entities get a new implementation that
  knows the player's moves and limits.
- 2026-11-22, owner: an NPC acts in its own view, not in the shared camera view.
- 2026-11-22, owner: in the first pass, an NPC is locked to one room. `TODO`s for (a) a teleport on room transition
  and (b) scripted events where it comes back into play; a mix of both is wanted.
- 2026-11-22, owner accepted the recommendation: targeting uses the `TargetPolicy` / `TargetingComponent` design of
  `112`, not a single `findAttackTarget` function. The candidates become "the foes of the seeker" from `030`, so
  enemies can choose an ally and an ally can choose an enemy with the same policies.
- 2026-11-22, owner: the names of the human and AI sides of a player slot (called "seat" in the research). A sealed
  interface `PlayerSlotAgent` drives a player slot. Its implementations are `HumanPlayerSlotAgent` and
  `BotPlayerSlotAgent`. "Controller" is not used, because it means gamepad and keyboard input; the implementation names
  are as long as the interface name, so each one is clear on its own.

  ```kotlin
  sealed interface PlayerSlotAgent
  class HumanPlayerSlotAgent(val device: InputDevice, val settings: PlayerSettings, val hud: PlayerHud) : PlayerSlotAgent
  class BotPlayerSlotAgent(val brain: BotBrain) : PlayerSlotAgent

  players.assign(PlayerSlot(1), BotPlayerSlotAgent(FollowBrain()))
  ```
- 2026-11-22, owner accepted the recommendation: the ally shapes come in stages. A bot in a player slot first (phases
  1 and 2), then slot agents (phase 3), and allies with no slot only for guests, escorts and NPCs (phase 4). See
  `NOTES.md`, "Slots or a separate mechanism".
- 2026-11-22, owner accepted the recommendation: the room lock does not apply to a bot in a player slot. The room
  transition already carries a slot player, so the lock applies only to the allies with no slot (phase 4).
- 2026-11-22, owner accepted the recommendation: each branch of this item squash-merges directly into `master`. No
  parent branch. A bot is off unless a launch option turns it on, so each branch is safe to ship alone.
- 2026-11-23, owner accepted the recommendation (smart-bot scan): the gaps go into this item. `178b` gains perception,
  the WALK and FALL skills, brain resets and `--debugBot`; `178c` gains the other skills; new branches `178j` (room
  graph and goals) and `178k` (dynamic terrain); `178d` gains threat forecast and dodge.
- 2026-11-23, owner: the bot course harness (`006`, `010`, then `178i`) comes before `178c`.

## Open questions

- **Player-only checks.** Which ones should an ally that is not a player join (group 5 in `NOTES.md`)?
- **Can the bot lead?** `PlayersFocus` leaves the bot out of the camera focus (`PlayersFocus.kt:61`), so a bot that
  goes ahead alone is held at the view edge and warped. Owner, 2026-11-23: decide later, after gameplay tests show
  what plays best.
- **Shot block.** Do P1's shots pass through the bot (3 projectile classes), or does the rule wait for `030` and
  `120`? Decided in the `178d` brief.

## Sources

- `NOTES.md` in this folder.
- The research of 2026-11-22 (two read-only fan-outs, 12 agents). The full reports are in
  `temp/npc-research/findings.md` and `findings-2.md` in the main checkout; they are not committed.
- The smart-bot scan of 2026-11-23 (6 agents): `temp/bot-ai-research/` in the main checkout, not committed.
