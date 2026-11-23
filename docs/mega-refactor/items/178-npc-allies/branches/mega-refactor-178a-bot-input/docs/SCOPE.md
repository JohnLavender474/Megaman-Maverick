# Scope: mega-refactor-178a-bot-input

- Status: approved
- Approved: 2026-11-22 (D1 to D4: the recommendations)

## Goal

An AI brain drives player slot 1 in place of a pad. The first brain copies P1's input with a delay and steers
back to P1's recorded position (as Tails in Sonic 2). A launch option turns it on. With no option, nothing changes.

## Item

`178`, "Proposed scope", phase 1, step 1 (copied in full):

> **`178a` bot input on slot 1.**
> - **What:** an AI input source writes slot 1's held mask in place of a pad (`MegamanMaverickGame.kt:617`), with a
>   launch option to choose it. The first brain copies P1's input frames with a delay (as Tails in Sonic 2). It uses
>   the slot 1 view-edge warp to catch up, and a test map `REFACTOR_TEST_178`.
> - **Why first:** this is the smallest end-to-end proof that an AI can drive a `MegaPlayer`. Item `031` already
>   plans the `AiSource`. The brain needs no navigation, faction or targeting work yet.

Item decisions that apply: the room lock does not apply to a bot in a player slot; each branch squash-merges into
`master`; a bot is off unless a launch option turns it on.

## Current state

Paths are relative to `core/src/main/kotlin/com/megaman/maverick/game/`.

- **Input per slot.** `MegamanMaverickGame.render()` builds one frame per slot each render frame, before the screen
  render: slot 0 from keyboard and pads, slot 1 from `slot1GamepadInput` (`MegamanMaverickGame.kt:616-617`).
  `InputFrameBuilder.build(held: Long)` makes the edges and holds the lock (`TURN_CONTROLLER_OFF` locks every
  builder, `MegamanMaverickGame.kt:730`). `MegaPlayers.register` binds the entity's `ControllerComponent.input` to
  `inputs[slot]` (`players/MegaPlayers.kt:35`). So a brain only has to give a held mask for slot 1.
- **Slot count.** `ResolvedLaunch.players` is the count of slots. 4 sites read it: the pad share
  (`MegamanMaverickGame.kt:338`), the slot 1 pad (`:340`), the `Megaman` per slot (`:466`) and the roster
  (`:1109`). `MAX_PLAYERS = 2` (`LaunchOptionsResolver.kt:57`). `--mode` and `--viewEdges` need `--players 2`
  (`LaunchOptionsResolver.kt:108-160`).
- **Session rules.** With 2 slots the default mode is `COOP`: side view edges `WALL`, bottom `WARP` after 1 s,
  `SlotRespawnRule.NEAR_PARTNER` (a death costs a shared life), gate blocks (`state/SessionRules.kt:30-37`).
- **Camera.** With 2 slots the camera follows the mean of the players (`screens/levels/camera/PlayersFocus.kt`),
  less the players that `excluded` names; today that is the players that the view edges hold
  (`MegaLevelScreen.kt:394`).
- **View-edge warp.** `CameraViewEdges` holds a live player that is past a `WARP` edge and has a free partner, then
  puts it at a safe spot next to the partner (`screens/levels/camera/CameraViewEdges.kt:189-266`). A side `WARP` is
  experimental: with the group camera both players steer the view (`TODO` at `:186`).
- **Room exits and gates.** A side exit and a gate wait for every live player (`047h`, `047k`). Top and bottom
  exits trigger on slot 0 alone.
- **Pause.** Only slot 0's frame can pause (`MegaLevelScreen.kt:860-873`). `SELECT` is next weapon or air dash, from
  the slot's `PlayerSettings.selectButtonAction` (`players/PlayerAirDash.kt:43`, `entities/megaman/Megaman.kt:496`).
- **AI source in `031`.** `031` `NOTES.md` ("Long-term shape") asks that an AI source gives held bits only, through
  the same edge builder, computes once per tick, and uses no hash order and only seeded random values.

## In scope

1. **Launch option `--bot follow`.** Puts a bot in slot 1 (a third `ResolvedLaunch` value, `bot: BotKind?`, enum
   `BotKind { FOLLOW }`). Errors: `--bot` with `--players 2`, `--bot` with `--mode pvp`, an unknown kind, a level
   with no `MEGAMAN`. The mode with a bot is `COOP`. `--viewEdges` is allowed with a bot. The 4 slot-count sites
   read a new `ResolvedLaunch.slots` (players plus bots).
2. **`FollowBrain`** (`players/bots/FollowBrain.kt`, new). Each render frame, before the screen render, it records
   P1's held bits (from slot 0's built frame, so the lock is kept) and P1's position in a ring buffer allocated once.
   It gives slot 1 the held bits from a set delay back, with these rules:
   - `START` is never copied.
   - If the bot is more than a set distance from P1's recorded position on X, it presses toward it in place of the
     copied `LEFT` / `RIGHT`.
   - If P1's recorded position is a set height above the bot, and the bot stands and has not moved on X for a set
     time, it presses `A` for a full jump.
   - It records nothing while the game is paused or while P1 is not live, and gives `0` while the bot is not live.
   - No random values; the delay and the limits are companion constants, tuned at the play test.
3. **Game wiring.** With a bot: slot 0 gets every pad (`PadShare.ALL`), slot 1 has no pad, and slot 1's frame comes
   from `frameBuilders[1].build(brain.held(...))`. Slot 1's `selectButtonAction` is set to slot 0's at start, so a
   copied `SELECT` does the same thing for both.
4. **Bot slot on the players service.** `MegaPlayers.isBot(slot)`, set once in `postCreate()`. First caller: the
   camera focus (decision D1). `TODO` for item `178e` (`BotPlayerSlotAgent` replaces it).
5. **Camera and view edges with a bot.** The camera follows P1 only: `PlayersFocus` leaves out a bot slot (D1). The
   default view edges of a bot session are the co-op ones with `left` and `right` set to `WARP`, so a bot that falls
   behind at a side warps to P1 (D2); `--viewEdges` still overrides them. A bot death costs a shared life, as in
   co-op; a `TODO` at `SlotRespawn` names `178e` for a free bot death (D3).
6. **Test map** `RefactorTest178.tmx`, `LevelDefinition.REFACTOR_TEST_178` (`TEST_LEVEL`), built by a subagent with
   the `level-builder` skill. Rooms (one per check): `follow_flat`, `follow_steps`, `follow_pit`, `follow_outrun`,
   `follow_exit` + `follow_exit_next`, `follow_enemies`. Each has a player spawn and painted `tiles`.
7. **Tests.** `LaunchOptionsResolverTest` cases for `--bot`; a `FollowBrainTest` for the delay, the `START` mask,
   the steer, the jump assist and the pause.

## Out of scope

- Navigation and pathfinding (`178b`, `178c`), combat choices (`178d`), slot agents, a free bot death and a bot HUD
  (`178e`), takeover (`178f`).
- A bot with 2 humans (needs more than 2 slots; `178e`).
- Bots in PvP (`109`).
- Changes to co-op with 2 humans.

## Affected areas and risks

- `MegamanMaverickGame.kt` (input step, `create`, `postCreate`), `LaunchOptions.kt`, `LaunchOptionsResolver.kt`,
  `DesktopLauncher.kt`, `players/MegaPlayers.kt`, `screens/levels/MegaLevelScreen.kt` (focus `excluded`), the new
  `players/bots/FollowBrain.kt`, `levels/LevelDefinition.kt`, the new TMX. 9 production files.
- A bot that is stuck in view (a wall it cannot jump) holds P1 at a side exit or a gate, which wait for every live
  player. The view-edge warp frees it only once it is out of view.
- The delay is counted in time over render frames, not on a fixed tick (`O2-R3`, waiting on `010`): at a low frame
  rate the copy is coarser.

## Overlap with other branches

None. The active branches (`060k`, `078e`, `078f`, `078g`, `compactor-level`, `fix/projectiles-ignore-death`) touch
art, levels and projectiles, not input, launch options or the camera focus.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | waiting | The brain is owned by the game object, like the slot 1 pad today. |
| `O1-R3` | no effect | The brain runs in the input step, where the pad ran. |
| `O1-R4` | helps | `BotKind` enum and typed `ResolvedLaunch` field; no property keys. |
| `O1-R5` | no effect | Each new API has a caller in the branch: `isBot` (focus), `slots` (4 sites), `FollowBrain` (input step). No `BotBrain` interface until a second brain (`TODO` for `178d`). |
| `O2-R1` | helps | The bot writes only slot 1's `InputFrame`, through the same edge builder. |
| `O2-R2` | no effect | No random values. |
| `O2-R3` | waiting | The delay uses frame time; `TODO` for `010`. |
| `O2-R4` | no effect | No hash-ordered iteration; the ring is an array. |
| `O2-R5` | helps | The bot is named by its slot (`isBot(slot)`); the leader is slot 0 by name. |
| `O2-R6` | waiting | One camera, as today. |
| `O2-R7` | no effect | The brain reads body positions, not render or audio state. |
| `O2-R8` | no effect | The bot reads no bindings; it takes slot 0's `selectButtonAction`, a session setting. |
| `O2-R9` | no effect | No new events. |
| `O4-R2` | helps | Each check has a room in `REFACTOR_TEST_178`. |
| `O4-R3` | waiting | The brain has no states beyond one assist timer; `178d` moves to `015`. |
| `O5-R1` | no effect | The ring is allocated once; no per-frame allocation. |
| `O6-R1`, `O6-R2` | helps | Named tests for the option and the brain. |
| `O8-R2` | no effect | No character checks. |
| `O8-R3` | helps | The modes are named: a bot session is `COOP` with a bot in slot 1; `SINGLE` and `PVP` do not change. |

## Decisions

Owner, 2026-11-22, all as recommended:

- D1: in a bot session the camera follows P1 only; the bot never moves the view.
- D2: the side view edges are `WARP` in a bot session; a bot that falls behind warps to P1.
- D3: a bot death costs a shared life, as in co-op; a free bot death comes with `178e` (`TODO`). So a P1 death with
  the bot alive also costs a life and respawns P1 next to the bot, as in co-op.
- D4: the launch option is `--bot follow`; an error with `--players 2` or `--mode pvp`.

## Open questions

None.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with the new `--bot` cases in `LaunchOptionsResolverTest` and `FollowBrainTest`.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-178a-bot-input`.

Experimental checks:

1. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_flat --bot follow`: a second Megaman runs and jumps
   after P1 with a short delay, and stops near P1 when P1 stops. Pause with `START`: the bot does not pause or
   unpause the game.
2. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_steps --bot follow`: P1 climbs the steps; the bot
   copies the jumps, and when a copy fails it jumps up by itself.
3. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_pit --bot follow`: the bot falls in the pit after a
   bad copy, leaves the view at the bottom, and warps next to P1.
4. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_outrun --bot follow`: P1 runs at the tall wall
   and jumps at the last moment (less than about 1.5 tiles before it); the bot's copy hits the wall, the bot is left
   behind, leaves the view at the left side and warps next to P1 (D2). An early jump lets the copy clear the wall.
5. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_exit --bot follow`: at the side exit P1 waits for
   the bot, and both go into `follow_exit_next`.
6. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_enemies --bot follow`: the bot's copied shots hurt
   the `Met`s, and the `Met`s aim at the bot when it is nearer.

Control checks:

7. `./run-game-alpha.sh --level REFACTOR_TEST_178 --room follow_flat`: 1 player, no bot, as before.
8. `./run-game-test-coop.sh --room group_wide`: co-op with 2 humans and 2 devices, as before (group camera, side
   walls).

## Behaviors to check

- A bot death costs a shared life and respawns the bot next to P1 (D3).
- A P1 death with the bot alive, and a death of both.
- `--bot follow --players 2` and `--bot follow --mode pvp` exit with an error (code 2).

## Intended changes

- New launch option `--bot follow`: a bot in slot 1, camera on P1, side view edges `WARP`. No change with no option.
