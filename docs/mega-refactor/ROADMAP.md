# Roadmap

The recommended order of the work. Change this file only on `master`.

- Last full review: 2026-11-02, at `master` commit `806b5c7d6`.

## How to use this file

- This file holds only the order: stages, tracks and the reason for each stage. Dependencies and statuses are in
  `ITEMS.md`, the rationale of an item is in its `ITEM.md`, and each issue is in `docs/issues/README.md`.
- The order is advice, not a rule. If the owner picks other work, the work goes on, and this file changes to match.
- A stage goes before the next one. Tracks in one stage have no dependency on each other. No overlap scan was done
  for them, so a branch still scans for overlap, and branches still merge one at a time.
- `→` means "in this order". Items in one cell with no `→` can run in parallel.
- When a branch merges, remove what it finished (`CLAUDE.md`, "Branch merge", step 6). Add a new item to a stage or to
  "Any time" when it gets a row in `ITEMS.md`.
- After a full review, change the commit in "Last full review".

## Stage 1: crashes, co-op faults and small safe items

Reason: these are small and independent. They fix live crashes and known co-op faults before more co-op work builds on
them. `091` also gives a reliable check that a room loads.

| Crashes                                               | Co-op faults                 | Small items                                  |
|-------------------------------------------------------|------------------------------|----------------------------------------------|
| `CeilingCrusher` pooled scratch object                | Dead slot 1 at `players.any` | `072` remove `IMegaGameEntity`               |
| 4 silent missing regions                              | Koopa shell roll direction   | `091` check that a room loads with no window |
|                                                       |                              | `065` every `init()` calls `super.init()`    |
|                                                       |                              | `105`, `106`, `107` smoke test checks        |

## Stage 2: foundations

Reason: most later work waits for these items.

| Co-op          | Determinism            | Logic structure     | Safety and data           | Characters       |
|----------------|------------------------|---------------------|---------------------------|------------------|
| `038` → `047c` | `011` random source    | `015` state machine | `005` damage test         | `081` playable X |
|                |                        | `012` freezes       | `021` entity registry     |                  |
|                | `017` world-step hooks |                     | `009` allocation baseline |                  |

## Stage 3: what the foundations unlock

Reason: each item here depends on an item in stage 2. `047e` and the first `112` branch run together, because the boss
parts are the first real callers of the policy.

| Co-op                              | Time             | Logic                 | Data                        | Characters             |
|------------------------------------|------------------|-----------------------|-----------------------------|------------------------|
| `047d` HUD and pause               | `010` fixed tick | `013` level flow      | `022` → `041`, `050`, `101` | `069` dash button      |
| `047e` with first `112` branch     |                  | `023` damage profiles |                             | `113` character select |
| `030` factions, then rest of `112` |                  | `082` dialogue        |                             |                        |

`010` is an `idea`, and it must be `planned` before this stage.

## Stage 4: modes and replay

Reason: the modes need the finished co-op rules, `030` and `102`. Replay needs `010`.

| Modes                                        | Replay                                        | Characters                          |
|----------------------------------------------|-----------------------------------------------|-------------------------------------|
| `102` game modes → `108` Spec Ops, `109` PvP | `006` headless boot → `055` record and replay | `024` `WeaponDef` → `060` Proto Man |
| `047f` join and leave                        |                                               |                                     |

`102` is an `idea`, and it must be `planned` before this stage.

## Stage 5: larger goals

Reason: these items need most of the stages above.

| Multiplayer                                | Platforms                                       |
|--------------------------------------------|-------------------------------------------------|
| `094` split screen                         | `032` neutral engine, with `001b` host contract |
| `110` campaign co-op, `111` multi-room PvP | `028` → `033` assets                            |
| `095` netplay                              | `035` → `054` web and Android, `088` web player |

## Any time

These items do not block the stages. Many are `idea`, so they are not agreed yet.

- Performance: `018`, `020`, `026`, `044`, `045`, `046`, `067`, `116`
- Saves and checkpoints: `007` → `034`; `036` → `037`
- Tooling: `003`, `008`, `039`, `040`, `043`, `090`, `100`
- Cleanup: `027`, `066`, `071`, `083`, `087`, `092`, `093`, `096`, `097`, `098`, `103`, `104`
- Design: `025`, `042`, `048`, `049`, `052`, `053`, `057`, `058`, `059`, `070`, `086`, `114`
- Boss fights: `118f` → `118g`. `082b` after `082a`. `118i` before `048` step 6. `013` and `047e` can use the fight routine now
- Death events: `119` before `047c`
- Later features: `120` after `030`, `047c`, `047e`, `112`, `118`, `119`; `121` after `081`; `122` after `102`
- Content: `073` to `080`, `089`, `099`, `115`, `117`; `123` after `017`; `124`

## Issues

### Closed by an item

An item in this list closes these issues, so they need no fix branch of their own.

- `011`: `superRandom`, `OrderedSet.random`
- `010`: frame delta clamp
- `012`, `013`: pause during a gate, health fill during a pause, `START` release, focus-loss pause
- `038`: respawn room music, coins on respawn, `SPAWN` damage lock
- `036`: silent checkpoint change, same-name "4" spawns
- `007`, `034`: volume not saved, health tank fill not saved
- `058`, `114`: `down` gravity camera, `BLOCK`-`GRAVITY_CHANGE` rule
- `030`: explosions from Mega's shots
- `118`: boss props with two sources
- `119`: slot 1 death acts on P1's objects
- `047c`: weapon `spawned` sets, `throwAxe` live check

### Separate fixes, in parallel at any time

- Engine: `CullablesSystem`, `InsertionOrderPriorityQueue.size`, `retainAll`, `GameLine.rotate`, `Float.round`,
  `PathfindingComponent`, the `replace` regex, `LocksComponent.reset` hook
- Build: `engine` Gradle dependencies, `jamepadVersion`, JDK 25
- Content: 14 unspawned TMX objects, 5 `DESERT_MAN` enemies, `DuoBallCanon`, 12 TexturePacker projects, background
  `section`, bottom-left spawn, 10 unused classes, `WILY_CASTLE_SCREEN`
- Contacts: `MagmaWave`, `PLAYER`-`DEATH`, unused hooks, `hitExplosion` shapes, `BouncingPebble`
- Input and menus: `P` key, held press, `START` or `A` rebind, `Esc` on level select
- Other core: `isPerformanceSubpar`, Needle Spin bounce, stun bounce, `RobotMasterIntroScreen.dispose`, `Saw` type `t`,
  `CameraRoomTransition.cancel`, raw viewport size, `runQueue` order, end-level beam flag, `DarknessV4`, enemy drop block check,
  `PushableBlock` rest velocity and ledge sliver (`fix/pushable-block-ledge-sliver`),
  ledge sliver pose (`fix/ledge-sliver-nudge`), double `init()` in the factory preload, slot 1 camera-edge death,
  cart kill drop check

### Waiting for an owner decision

- The final Dr. Wily fight
- The `BigAssMaverickRobot` hand motion
- Enemies that copy the target's direction
- A cart that kills a boss in one hit
- Hits on a boss during its intro
