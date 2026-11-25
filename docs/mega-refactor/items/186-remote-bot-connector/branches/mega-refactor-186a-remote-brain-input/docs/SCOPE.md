# Scope: mega-refactor-186a-remote-brain-input

DRAFT. Written ahead of the branch from a read-only scan (2026-11-25, `master` `2189b7a`). It still goes through the
brief and the owner's answers before it is approved.

- Status: draft
- Approved: no

## Goal

A local client controls the bot player over a socket. The first step sends buttons only. The client reads no world state
yet (that is `186b`). Nothing changes for a game that does not start `--bot remote`.

## Item

Item `186`, step 1 ("`186a` remote brain, input only"), copied from `ITEM.md`: a `RemoteBrain` is the second bot brain
(after `FollowBrain`). A launch option (`--bot remote`) starts a localhost-only socket server. A client sends a
held-button mask. The brain returns the latest mask each frame. The branch adds the `BotBrain` interface that
`FollowBrain` marks with a `TODO` (item `178d`). The server replies with a `hello` message.

## Current state

- Input seam: `MegamanMaverickGame.kt:603-606` (slot 1 from `followBrain.held(leaderFrame, delta)`), `InputFrameBuilder`,
  `MegaPlayers.kt:43`.
- Bot wiring: `BotKind` (one value), `LaunchOptionsResolver.kt` (`parseBot`, rejects `--players 2` and `--mode pvp`),
  `DesktopRunner.kt` (`--bot`), `MegamanMaverickGame.kt:337-346` and `:481-484` (`setBot`, selectButtonAction copy).
- `FollowBrain` is the only brain, held directly as `FollowBrain?` (`MegamanMaverickGame.kt:236`).
- Details and the other findings: item `NOTES.md`.

## In scope

- A `BotBrain` interface with the one method the game loop calls (`held(...)`: the mask for this frame). Its callers are
  `FollowBrain` and `RemoteBrain` in the same branch (`O1-R5`).
- `BotKind.REMOTE` and `--bot remote` (an optional `--botPort`, default chosen in the brief), with the resolver rules
  and test cases next to the follow ones.
- `RemoteBrain` in `core`: reads the latest mask from an `AtomicLong`; returns 0 when no client is connected or the
  client was silent for N frames; returns the last mask while `clock.paused` as `FollowBrain` does.
- A socket server in `desktop-common/` behind a small interface that `core` defines (so `core` has no JVM socket code,
  `O3-R1`). Binds to `127.0.0.1` only. One client at a time. JSON lines.
- Messages: server `hello` (protocol version, button names, frame rate); client `input` (list of held button names).
  A bad message gets an `error` reply and is ignored; it never stops the game.
- A short protocol doc in the branch folder, and one example client script in `temp/` (not committed) used for the
  manual check.

## Out of scope

- World snapshot or any read of entities (`186b`).
- Pause and lockstep modes (`186c`; lockstep waits for `010` and `006`).
- Driving slot 0 with no human; two clients; any remote (non-localhost) access.
- Navigation, skills, goals, combat (`178b`, `178c`, `178d`).
- Any change to player rules, cameras or session rules.

## Affected areas and risks

- `MegamanMaverickGame.kt` (brain field, the two `--bot` branches), `LaunchOptionsResolver.kt` and its test,
  `DesktopRunner.kt` (flag help, port), `players/bots/`, a new class in `desktop-common/`.
- Risk: an overlap with the unpushed follow-bot branch (same files, same `BotBrain` seam). Decide the order first.
- Risk: the socket thread touching game state. Rule: it only writes an `AtomicLong` and reads an immutable `hello`.
- Risk: a stuck button after a disconnect. Rule: no client means 0 held.

## Overlap with other branches

- `mega-refactor-178b-ground-pathfinding` (active): touches `players/bots/` and the bot loop; check at branch start.
- The unpushed follow-bot branch: not visible from the cloud session. The owner names the order.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` | no effect | Each new type has a caller in this branch (`BotBrain`: 2 brains). |
| `O2-R1` | helps | Input still reaches a player only through an `InputFrame`. |
| `O2-R2` | no effect | No random call. |
| `O2-R3` | waiting | Free-run input is not deterministic; lockstep waits for `010`. |
| `O2-R8` | no effect | Socket state never enters the simulation; only the mask does. |
| `O3-R1` | no effect | The server sits in `desktop-common/`; `core` gets only an interface. |
| `O6-R3` | no effect | Tests use a fake client over a loopback socket or a direct call; no window. |

## Open questions

See "Open questions" in the item `ITEM.md` (slot, transport, server home, order with the unpushed branch, token,
merge target).

# Contract

## Automated checks

- [ ] `./gradlew engine:test`, `core:test`, `core:smokeTest` pass.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] Resolver tests: `--bot remote` is accepted with 1 player, rejected with `--players 2` and `--mode pvp`; the
  error text lists both kinds; `--bot follow` results are unchanged.
- [ ] A `RemoteBrain` unit test: a mask set from another thread is returned by `held()`; no client gives 0; a silent
  client gives 0 after N frames; paused gives the last mask.
- [ ] A server test over loopback: `hello` on connect; a valid `input` sets the mask; a bad line gets `error` and
  keeps the connection; a disconnect gives 0.
- [ ] CI result of the PR (the cloud proxy may block the Gradle download; say so if the checks did not run).

## Manual playthrough

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_178` (`--bot remote`) | The example client moves the bot right, jumps and shoots; releasing all buttons stops it; killing the client stops it at once. |
| `REFACTOR_TEST_178` (`--bot follow`) | Same as before this branch. |

## Behaviors to check

- With no `--bot`, no port is opened and nothing else changes.
- A game with `--bot remote` and no client behaves as a bot that holds nothing.
- Pause (menu) while a client sends input: the bot does not act while paused.

## Intended changes

- New launch option value `--bot remote`. No gameplay change.
