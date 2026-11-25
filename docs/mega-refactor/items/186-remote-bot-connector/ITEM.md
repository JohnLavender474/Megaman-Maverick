# 186: A remote bot connector: outside programs drive a player and read the world

- Status: idea
- Type: feature
- Objectives: `O8`, `O6`, `O2`
- Depends on: `178` (branch `178a` merged; the bot input seam)
- Related: `006`, `010`, `015`, `178` (branches `178b`, `178d`, `178i`), `031`
- Branches: [`mega-refactor-186a-remote-brain-input`](branches/mega-refactor-186a-remote-brain-input/process-log.md) (planned)

## Goal

An outside process (a script, a test harness, a search or learning program, or an LLM agent) connects to a running game
over a local socket. It sends the buttons that a player holds, and it receives a structured snapshot of the world
(the player, nearby enemies, shots, blocks and the room). The game treats the connection as one more bot brain: the
player it drives obeys every player rule at once.

Why:

- It proves that any program can drive a `MegaPlayer`, with no change to the player rules. `178a` proved it for an
  in-process brain (`FollowBrain`).
- It gives the owner a way to try ideas fast, in any language, with no rebuild of the game. A script can play a
  course, and an LLM can play with the game paused between decisions.
- The same connector is the base for the bot course harness (`178i`) and for replay and learning tools.

Nothing changes for a player who does not start the option. The connector is a dev option, off by default.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. The order, and the reason for each step:

1. **`186a` remote brain, input only.** A `RemoteBrain` is the second bot brain (after `FollowBrain`). A launch option
   (`--bot remote`) starts a localhost-only socket server. A client sends a held-button mask. The brain returns the
   latest mask each frame. The first caller is the `--bot` branch of `MegamanMaverickGame`. This step adds the
   `BotBrain` interface that `FollowBrain` already marks with a `TODO` (item `178d`). Reply with a `hello` message
   (protocol version, button names, frame rate) so a client can check what it talks to.
2. **`186b` world snapshot.** The main thread builds a typed snapshot each frame and hands it to the socket thread
   (the socket thread never reads the engine). First contents: the controlled player (position, velocity, facing,
   health, weapon, body senses), the room, the live enemies and shots near the view, and the blocks near the view.
   It shares its perception code with `178b` (`BotView`, `WorldQuery`, `RoomStaticMap`), so the two are built once.
3. **`186c` control modes.** What the client can ask for, in order of cost:
   - free run: the game runs at its own pace, and the latest mask wins (no change to the loop);
   - pause and think: the game pauses at a decision point (`GameClock.paused`) until the client replies;
   - lockstep: the game advances one tick for each client message. This needs the fixed tick (`010`) and the headless
     boot (`006`), so it waits for them.
4. **Later, not planned here:** slot 0 control (a script drives the only player), goal and skill commands for an LLM
   (the `178b` skill layer: timed button scripts), and a Python client in `utils/`.

## Owner's input

2026-11-25, owner (from the phone), after a read-only scan of the follow bot: asked whether a connector that outside
processes can connect to, to control a player or a bot, is feasible. The ideal is that an LLM drives a player by
scanning the world and its possibilities in real time, "though I'm sure that's not feasible with today's tech". Another
branch (not pushed yet) makes the follow bot more dynamic. Then: create a new branch, sketch the docs with no
implementation, and open a PR.

## Open questions

- **Which slot?** Today `--bot` puts the bot in slot 1 next to one human, and `--bot` with `--players 2` is an error.
  Should a remote client also be able to drive slot 0 (no human), and with what launch flags?
- **Which transport and format?** A raw TCP socket with JSON lines is the simplest for any language. A WebSocket suits
  a browser tool. A binary format is faster but harder to read. Recommendation: TCP with JSON lines first.
- **Where does the server live?** `O3-R1` says no JVM-only API on the web path (after `032`). A socket server is
  JVM-only, so it should sit in `desktop-common/` behind a small interface in `core`, not in `core` itself.
- **How does the item fit with the unpushed branch** that makes the follow bot more dynamic? The `BotBrain` interface
  and the perception code could conflict with it. The owner decides the order after that branch is pushed.
- **Security.** Bind to `127.0.0.1` only. Is a token needed, or is the dev-option and localhost rule enough?
- **Merge target.** Directly into `master`, or a parent branch? (Item start rule in `CLAUDE.md`.) Recommendation:
  directly into `master`, since each branch is safe to ship alone and off by default.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/178-npc-allies/ITEM.md` and `NOTES.md` (bot input, perception and skill plans).
