# 178: NPCs and AI allies that fight next to the players

- Status: planned
- Type: feature
- Objectives: `O8`, `O2`, `O4`
- Depends on: `031`; later branches `015`, `030`, `112`, `128`, `082`
- Related: `029`, `047`, `079`, `081`, `102`, `108`, `109`, `113`, `118`, `120`, `128`, `171`, `175`
- Branches: none yet

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
   - **Why here:** the owner decided on separate implementations behind one abstraction (see "Decisions"). The
     ground strategy is the second caller that makes the abstraction real; with only the flyers it would be a
     speculative API. Walk and fall links come first because they need no jump simulation.
3. **`178c` jump, wall-jump, air-dash and ladder links.**
   - **What:** the graph gets the links that need movement physics. Each link is checked with a simulated arc at the
     fixed step (`JumpPlanner`), for each move profile: Megaman and Proto Man share one, and X has his own.
   - **Why after `178b`:** the jump height depends on the A-button hold and on Y friction, so it must be simulated,
     not computed (`PlayerBody.kt:57-58`). It is the riskiest part of the navigation, and the walk graph shows first
     that the graph and the follower work.

### Phase 2: a brain that fights

4. **Item `015`: the common state machine.**
   - **Why before `178d`:** the brain has states (follow, traverse, recover, warp, fight) with timers. Without `015`
     it would be one more hand-made machine, and `O4-R3` asks new AI to use the common machine.
5. **`178d` ally combat.**
   - **What:** the bot shoots enemies and defends the human. It picks a target and an action with a weighted pick
     (a generic picker taken out of `D1000Director`, its second user). It keeps companion rules: it fires on
     permission, it never blocks the player's shots, and it does not take boss kills.
   - **Why the bot needs no faction work yet:** its shots already hurt enemies, and enemies already target it,
     because it is a player. Its target candidates are the live enemies and bosses, with a `TODO` to use the foes
     query of `030` and `112`.

### Phase 3: seats, so that a bot is not a human

6. **`178e` seats.**
   - **What:** split the seat (a human's input, settings, HUD and lives) from the slot (the identity of a hero on the
     field). `MAX_PLAYERS` becomes a seat limit and a slot limit. A slot with no seat has an AI driver. Then a bot
     death can be free, a bot can be kept from pickups, and 2 humans can each have a bot.
   - **Why here:** this is the first point where "a slot is a human" breaks: lives, HUD and `--players` all count the
     bot as a human. It changes the meaning of `O2-R5`, so the brief asks the owner first.
   - **Depends on:** `175` if it is merged by then.
7. **`178f` takeover.**
   - **What:** a human who joins takes over a bot, and gives it back on leaving (as Tails, Kirby helpers and Left 4
     Dead bots).
   - **Why after `178e`:** with seats, a takeover only binds or unbinds a seat; the entity stays the same. It
     depends on `047f` (join and leave).

### Phase 4: allies and NPCs that are not players

8. **Items `030` (factions), then `112` (targeting policies).**
   - **Why:** an ally with no slot needs a side (its shots hurt enemies, not players) and enemies that can choose it.
     `112` must select from "the foes of the seeker", not only from players.
9. **`178g` NPCs in their own room and view.**
   - **What:** an ally or escort that is not a player. It has the faction `PLAYER`, and the brief decides per
     mechanic which player-only checks it joins (carts, springs, crushers, stomps). It stays in its own room
     (decision below) and acts in its own view: a virtual camera (item `128`) keeps the blocks and spawns near it,
     and the 11 enemy AIs that act only in the real camera check "in any view". It is dormant while its room is not
     the current room.
   - **Why last of the core work:** it needs `030`, `112` and `128`, and the bot of phases 1 to 3 has already proved
     the brain.
10. **`178h` re-entry.**
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

## Open questions

- **Targeting.** Recommended: use the `TargetPolicy` / `TargetingComponent` design of `112`, not a single
  `findAttackTarget` function. The candidates become "the foes of the seeker" from `030`, so enemies can choose an ally
  and an ally can choose an enemy with the same policies.
- **Ally shape.** Recommended: a bot in a player slot first (phases 1 and 2), then seats (phase 3), and allies with no
  slot only for guests, escorts and NPCs (phase 4). See `NOTES.md`, "Slots or a separate mechanism".
- **The room lock and a bot in a slot.** Does the room lock also apply to a bot in a slot? Recommended: no. The room
  transition already carries a slot player, so the lock applies to the allies of phase 4.
- **The merge target** (directly into `master`, or a parent branch): ask at the item start.
- **Player-only checks.** Which ones should an ally that is not a player join (group 5 in `NOTES.md`)?

## Sources

- `NOTES.md` in this folder.
- The research of 2026-11-22 (two read-only fan-outs, 12 agents). The full reports are in
  `temp/npc-research/findings.md` and `findings-2.md` in the main checkout; they are not committed.
