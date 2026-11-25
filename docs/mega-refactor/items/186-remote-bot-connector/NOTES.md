# Notes: 186 A remote bot connector

## About this research

- **Basis:** a read-only scan on 2026-11-25 at `master` commit `2189b7a`. No code was changed or run.
- **Paths:** relative to `core/src/main/kotlin/com/megaman/maverick/game/`, unless they start with `engine/` or
  `desktop-common/`.

## What exists today (the seam)

- **One input path.** Each render frame, `MegamanMaverickGame.kt:603-606` builds slot 0 from the keyboard and pads, and
  slot 1 from a pad or from the bot: `players.inputs[PlayerSlot(1)] = frameBuilders[1].build(it.held(leaderFrame, delta))`.
- **A brain gives only a mask.** `InputFrameBuilder.build(held: Long)` (`engine/.../input/InputFrameBuilder.kt`) turns
  a mask of held buttons into `PRESSED`, `JUST_PRESSED`, `JUST_RELEASED` and `RELEASED` states. One bit for each
  `MegaControllerButton`; `InputFrame` holds 2 bits for each button, up to 32 buttons.
- **A player reads its slot.** `MegaPlayers.kt:43` sets the player's `ControllerComponent.input` to
  `InputSource { inputs[slot] }`. So the player cannot tell a bot from a pad. `MegaPlayers.setBot(slot)` and `isBot`
  mark the slot (`MegamanMaverickGame.kt:482`); the group camera focus leaves a bot out.
- **The first brain.** `players/bots/FollowBrain.kt` (about 150 lines) records the leader's held mask and position, and
  replays them after `DELAY` (16/60 s). It steers toward the recorded x, and it adds a jump assist when the leader is
  2 or more tiles above and the bot is stuck for 1 s. It returns the last mask while `clock.paused`.
- **How the option is wired.** `BotKind` has one value (`FOLLOW`). `LaunchOptionsResolver.kt` parses `--bot`
  (case-insensitive), and rejects `--bot` with `--players 2` or `--mode pvp`. `DesktopRunner.kt` passes `appArgs.bot`.
  `LaunchOptionsResolverTest` checks the error text ("Allowed: FOLLOW"), so a new kind changes that test.
- **A bot session uses co-op rules** with `WARP` at the left and right view edges, slot 0 takes every pad, and the
  camera follows player 1 only.

## What does not exist

- **No perception for a brain.** `FollowBrain` reads only the two players' bodies. `BotView`, `WorldQuery` and
  `RoomStaticMap` are planned in `178b` and not built (0 hits in the code). Only the blocks near the camera are live
  (`SpawnersLayerBuilder.kt:70-72`, per the `178` notes), so terrain past the view needs the TMX-derived data.
- **No `BotBrain` interface.** `FollowBrain` has a `TODO` for it (item `178d`). A remote brain is the second brain.
- **No network code.** 0 socket, HTTP or server dependencies in the Gradle files.
- **No slot 0 bot.** The bot is always in slot 1.
- **No fixed tick, no headless run.** Items `010` and `006` are not merged. The follow delay counts render-frame time
  (`FollowBrain` `TODO`), and a just-pressed edge lasts one render frame, not one world step (`178` notes,
  "Timing that a brain must copy").

## Constraints from the objectives

| Rule | What it means for the connector |
|------|---------------------------------|
| `O2-R1` input only through `InputFrame` | The remote brain returns a mask and the frame builder makes the frame. No direct write to a player. |
| `O2-R2` random from the seeded source | The connector adds no random call. A client that wants random choices makes its own. |
| `O2-R3` one fixed tick (after `010`) | Lockstep mode waits for `010`. Free-run mode is nondeterministic by nature; say so in the protocol notes. |
| `O2-R8` local-only data stays out of the simulation | Socket state (client, buffers, latency) must not enter the simulation. Only the held mask does. |
| `O3-R1` no JVM-only API on the web path (after `032`) | Put the socket server in `desktop-common/`, behind a small interface in `core`. |
| `O1-R5` code only with a production caller | The `BotBrain` interface and each message type need a caller in the same branch. |
| `O6-R3` test tools with no window | The headless lockstep mode serves this later. |

The engine must stay free of Megaman Maverick content (`CLAUDE.md`, "Modules"), so the protocol and the snapshot go in
`core`, and only a generic input seam could go in `engine`.

## Design sketch (not approved)

```
client (script / LLM agent)                 game process
      |  JSON lines over 127.0.0.1               |
      |<----- hello {version, buttons, hz} ------|
      |<----- state {frame, player, room, ...} --|  main thread builds, socket thread sends
      |------ input {held: ["RIGHT","A"]} ------>|  socket thread stores in an AtomicLong
      |------ control {mode: "pause"} ----------->|  later branch
                                                  RemoteBrain.held() reads the AtomicLong
                                                  -> InputFrameBuilder -> players.inputs[slot]
```

- **Thread rule.** The socket thread only moves bytes. It never reads or writes an entity, a body or the engine. The
  main thread builds the snapshot into a plain immutable object and swaps it into an `AtomicReference`.
- **Input rule.** The client sends the held buttons, not edges. The frame builder derives the edges, as for a pad.
  A client that wants a one-frame tap must hold for at least one frame, then release.
- **Stale and lost input.** If no client is connected, or the client is silent for N frames, the brain returns 0
  (nothing held). A disconnect never leaves a button stuck.
- **Snapshot content, first cut.** Controlled player: position, velocity, facing, health, current weapon, and the body
  senses (`BodySense` such as `FEET_ON_GROUND`). Room: name and bounds. Near the view: live enemies (type, position,
  health), shots, and blocks. Static terrain past the view comes with `RoomStaticMap` from `178b`.
- **Units.** Send tile units, not pixels (`ConstVals.PPM`), so a client does not need the scale.

## Feasibility for an LLM driver

- **Frame by frame is not realistic.** A model answers in seconds, and the game runs at 60 frames each second.
- **Pause and think works with the current code.** `GameClock.paused` exists, and `FollowBrain` already returns its
  last mask while paused. The game pauses at a decision point, the client replies, the game resumes for a short span.
- **Goals and skills fit best.** The model says "go to the door" or "jump the gap". A fast local controller runs the
  skill. The `178b` skill layer (timed button scripts) and the path follower are that controller, so this is later work
  and builds on `178b`.
- **Lockstep needs `010` and `006`.** It removes the latency problem and gives repeatable runs for tests and learning.
- **A structured snapshot beats screenshots** for a model: less cost, no vision errors, exact positions.

## Risks

- **Overlap with `178b` and with the unpushed follow-bot branch.** Perception code and the `BotBrain` interface are
  shared concerns. Build them once, in one branch. Order decided by the owner.
- **Timing quirks** (just-pressed edge lasts one render frame; delay in render time) make frame-exact scripts flaky
  until `010`.
- **A local port is an attack surface.** Off by default, `127.0.0.1` only, a dev option that the release scripts never
  pass.
- **Protocol churn.** Version the `hello` message from the first branch, so a client can fail early on a mismatch.
