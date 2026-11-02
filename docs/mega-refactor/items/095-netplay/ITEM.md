# 095: Netplay with lockstep (`O2` phase 3)

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `094`, `010`, `011`, `055`
- Related: `031`, `047`, `012`, `013`, `097`, `108`, `109`
- Branches: none yet

## Goal

Two machines play one session through lockstep. Players are fixed at session start. This is phase 3 of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- The session setup: settings that change gameplay go to all peers.
- Input frames as the payload, with an input delay buffer.
- Pause and pause-menu actions as inputs (`LevelPauseScreen.kt:562`, `:578`).
- A desync check that compares state for each tick.
- The review of the 34 hash-collection declarations and the 17 animation reads in `O2`.
- The transport. Web needs its own transport (see `O3`).
- A two-window test on one device as the first test, before two PCs, the internet and web (see "Two-window test" in `NOTES.md`). It needs options to host and join a session, scripted inputs for one window, and fake delay and packet loss.

## Owner's input

None yet. The objectives audit of 2026-11-01 found this gap.

## Open questions

### Effect of the dedicated modes

- The owner's decision of 2026-11-01 puts Spec Ops (`108`) and PvP (`109`) on the net. Questions marked **(mode)** become smaller, because a mode session need not touch the save file.
- PvP adds a new need: input delay matters more in a fight between players. Is lockstep with input delay still the correct target for PvP, or does PvP make rollback necessary? Rollback is now "a possible later upgrade" (`O2`). The research of 2026-11-01 is in `NOTES.md`. Two product questions remain: are matches with strangers or at about 150 ms ping or more a target, and is 4 or more players on the net a first target?

### Connection type

- Which connection type does the first version use: peer-to-peer, listen server, relay or dedicated server? `NOTES.md` compares them and recommends peer-to-peer plus a relay fallback with lockstep. A dedicated server would replace lockstep with state sync and prediction, and it needs hosting. This depends on the first product question above: are matches with strangers a target?

### Views

- What does each peer see: one shared camera as in phase 1, or one view for each player? `O2-R6` and `O2-R7` require cull and spawn to use a set of views that is the same on all peers. So each peer must simulate the view of the other peer and render only its own (inference). Confirm.
- Sounds gated by a view (`094`) must use the local view, or both peers run the audio request from one sim state. Which one? `O2-R7` keeps audio out of the simulation.

### Session

- Whose progress does a session use? `O2` sends gameplay settings at session start, but says nothing about `GameState` (heart tanks, health tanks, weapons, lives). Options: the host's save, a merge, a fixed loadout. **(mode)**
- After a session, what does each peer save? `saveState()` writes one local file. **(mode)**
- Which launch options are session data, and which are refused in a session? `--pathfinding`, `--worldContainer`, `--fixedStepScalar`, and the dev gifts `--weapons`, `--heartTanks`, `--healthTanks` change the simulation. `O2-R8` covers only settings that gameplay reads.

### Scope of lockstep

- Does lockstep cover only the level screen, or all screens? Menus, level select, `GetWeaponScreen`, game over and the boss intro read `MegaPlayers.menuInput` and call `setCurrentScreen`. Who drives them on a net session, and how do peers enter a level on one tick?
- Cosmetic entities are game entities: `FallingLeaves.kt:107`, `UnderWaterBubble.kt:88`, `FloatingEmber.kt:144` and `Rain` use the camera. Do they stay in the simulation and in the checked state, or become local only? This also decides what the cosmetic stream of `011` covers.
- What state does the per-tick check compare? `O2` says "the state of both machines". Options: bodies, health and random state only; all entities; a hash of all components.

### Failure

- What happens when a peer leaves or stalls? `O2` fixes the players at session start, and local play allows a leave. Options: end the session, or drop the slot at an agreed tick and go on.
- `--pauseOnFocusLost` and `pauseOnMinimized` call `game.pause()` from the window listener (`DesktopLauncher.kt:177`). `O2` makes pause an input. Does focus loss pause all peers, do nothing, or does a net session refuse these options?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
