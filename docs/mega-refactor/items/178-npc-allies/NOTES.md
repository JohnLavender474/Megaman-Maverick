# Notes: 178 NPCs and AI allies that fight next to the players

## About this research

- **Basis:** two read-only fan-outs on 2026-11-22, with 12 agents in total, at `master` commit `384fa232d`.
- **Paths:** relative to `core/src/main/kotlin/com/megaman/maverick/game/`, unless they start with `engine/`.
- **Counts:** the agents measured them with grep. A count is a number of files or of non-import lines.
- **Checked by hand:**
  - the gate at `AbstractEnemy.kt:268`;
  - the ENEMY-ENEMY contact drop at `MegaContactFilter.kt:106`;
  - the input binding at `MegaPlayers.kt:35`;
  - 81 lines that call `overlapsGameCamera()` for sound.

## Input and control: works today

- **The input seam.** `engine/.../input/InputSource.kt` is a `fun interface` with `read(): InputFrame`.
  `ControllerComponent.input` is a public `var`. `InputFrameBuilder.build(held: Long)` makes the edges, so a brain
  sends only a held mask.
- **Input belongs to the slot.** It is bound to the slot, not to the entity (`MegaPlayers.kt:35`). Each frame, the
  game writes slot 0 and slot 1 from the devices (`MegamanMaverickGame.kt:616-617`).
- **Timing that a brain must copy:**
  - The jump stops when A is released (`players/PlayerJump.kt:55`).
  - A charged shot holds B over frames and fires on the release (`Megaman.kt:453-491`).
  - The air-dash button comes from the slot setting `selectButtonAction` (`PlayerAirDash.kt:43-52`).
  - The wall slide needs the run side held (`PlayerWallSlide.kt:25`).
  - The ground slide is DOWN held plus A just pressed (`PlayerGroundSlide.kt:63-66`).
  - A just-pressed edge lasts 1 render frame, not 1 world step (`TODO` at `MegamanXMoves.kt:66`).

## Player coupling

| # | What the check really asks | Lines | Covered by |
|---|---|---|---|
| 1 | Is the damage source a friend of the players? (`AbstractEnemy.kt:268`, `PlayerRules.kt:27`) | 20 | `030` + `120` `DamagePolicy` |
| 2 | Who do I aim at? (`targetPlayer`, `MegaGameEntity.kt:95`) | 491 | `112` |
| 3 | Is a foe inside my scanner? (`players.anyLive { overlaps(box) }`) | 23 | none |
| 4 | How do I react to a hero's shot, or to a hit on a hero? | 34 | `030` in part |
| 5 | Is a hero standing on me or touching me? (carts, springs, stomps, crushers, gates, pickups) | 68 | none for allies |
| 6 | Who gets the enhancement, score, drop and 1-up credit? | 6 | `030` |
| 7 | Camera, room, lives, respawn, fight flow, wrap | 38 | stays player-only |
| 8 | Light source in the dark | 3 | none |

Two more blocks are not in this table:

- `MegaContactFilter.kt:106` drops every contact between two `EntityType.ENEMY` entities.
- `StandardDamageNegotiator.kt:56,100,144` keys the damage amounts on `Megaman::class`.

## Targeting

- **The policy already exists as a design.** Items `112` and `029` (`NOTES.md`, "Targeting design") plan
  `TargetPolicy` and `TargetingComponent`. The reusable policies are `Nearest`, `Sticky(inner)`, `LastAttacker` and
  `RoundRobin`.
- **One function is not enough.** A single `findAttackTarget` grows into one big `when` over entity kinds, and that
  is what `112` rejects.
- **What allies add.** The candidates must be "the foes of the seeker" (from `030`), not only `IPlayers`. An area
  query `anyFoe { overlaps(box) }` replaces the 23 scanner lines of group 3.

## Pathfinding

- **How the pathfinder is chosen today.** There is one system and one factory (`MegamanMaverickGame.kt:1015-1029`).
  The engine interface is thin (`IPathfinder : Callable<PathfinderResult>`), but the types around it are made for a
  grid:
  - `PathfinderParams` has `IntPair` cells and a cell filter.
  - The result is `Array<IntPair>`.
  - The search sorts by cost only, so it is not A* (`engine/.../pathfinding/Pathfinder.kt:39`).
  - The iteration cap is 100.
- **The 4 users all fly or swim:** `Bat`, `DeathBat`, `FloatingCan` and `SubmarineJoe`. The result consumer sets a
  velocity straight at the next cell.
- **A strategy seam (sketch):**

  ```kotlin
  interface PathStrategy<R> { fun plan(): R }        // runs on the worker thread
  class PathfindingComponent<R>(var strategy: PathStrategy<R>, var consumer: (R) -> Unit, …)
  // GridPathStrategy: today's code under a new name; it keeps the per-frame block snapshot
  // GroundPathStrategy: an immutable TMX graph plus a move profile -> typed links (WALK, JUMP, WALL_JUMP, …)
  ```

  The system keeps only the shared parts: the interval, async or sync, and the timeout.
- **What to rename.** Renaming `WorldPathfinder` touches 3 files, and renaming `MegaPathfinderFactory` touches 3. The
  gate toggles at `MegaLevelScreen.kt:733-753` switch the systems by class.
- **The ground graph comes from the TMX layers at load:** `blocks*`, `hazards`, `specials` (ladder, water, gravity)
  and `game_rooms`. The live world holds only the blocks near the camera (`SpawnersLayerBuilder.kt:70-72`,
  `Block.kt:90-95`). Moving platforms (`BlocksFactory.kt:13-31`) need special links or live sensing.
- **Move profiles.**
  - Megaman and Proto Man share every move value (`MegamanValues.kt`, `ProtomanValues.kt`).
  - X differs on almost every value (`MegamanXValues.kt`).
  - Jump speed: 25 for Megaman, 14.2 for X.
  - Air dash: 10 u/s for 0.1 to 0.25 s for Megaman, 13 u/s for 0.5 s for X.
  - Wall jump: up 20 and push 6 for Megaman, a kick of 12.48 for X.
- **What runs where.** `GroundProbe` (FLOOR, SAFE_DROP, PIT, WALL) and `JumpPlanner` (a single jump arc at the fixed
  step) read the live world. They belong to the follower on the main thread, never to the search thread.
- **Code placement.** The engine can hold the link graph, A* and a move profile data class. Core holds the values,
  the layer names and the button mapping.

## An ally away from the camera

| Mechanism | Size | Does item `128` cover it? |
|---|---|---|
| Camera cull (`getGameCameraCullingLogic`, `CULL_OUT_OF_BOUNDS`) | 55 / 96 files | yes |
| Map spawn on overlap with the single `spawnCameraBounds` | 3 files | yes, if it becomes a set |
| A room transition culls every enemy and shot in the level | 30 files (`AbstractEnemy.kt:126`) | no |
| A room transition moves only `MegaPlayer` bodies | `MegaPlayer.kt:319-337` | no |
| No spawns during a transition | `SpawnersLayerBuilder.kt:70-73` | no |
| AI that acts only in the real view (`overlapsGameCamera()`) | 11 files | no |
| Sound only in the real view | 81 lines | no (keep it as it is) |
| An `ABSTRACT` body updates only in the view | `IBodyExtensions.kt:23-45` | no |
| One `currentRoom` (`SPAWN_ROOM`, `CULL_ROOM`) | 15 files | no |

With the room lock (owner decision 2026-11-22), the gaps of a first-pass NPC are these:

- the cull and the spawns near it (item `128`);
- the 11 AI gates and the `ABSTRACT` bodies (change them to "in any view");
- dormancy while its room is not current.

A bot in a player slot is carried by the room transition and held in the view by the view-edge rules, so it needs
none of these.

## Enemy and boss AI ("bespoke")

**What "bespoke" means.** Each enemy is one class, with one update lambda that senses, decides, moves, shoots and
animates. The classes have no shared names for actions or conditions, and most of the tuning values are companion
constants.

- 40 of 131 enemy files and 4 of 62 boss files read spawn props.
- In none of the samples can Tiled change the state times, the rules or the weights.

| Pattern | Enemy files | Boss files |
|---|---|---|
| `updatablesComponent.add` lambda | 125 | 24 |
| `enum class *State` | 72 | 26 |
| engine `StateMachine` | 15 | 10 |
| `Timer(` | 112 | 28 |
| `*Tuning` rows | 0 | 13 |
| `GroundProbe` / `JumpPlanner` | 1 | 0 |
| `BehaviorsComponent` | 0 | 0 |

**Why an ally cannot reuse an enemy brain:**

- An enemy sets its own velocity. An ally on the player body must press buttons, or it loses the player physics.
- Enemy senses are tied to `targetPlayer`, but an ally needs its foes.
- The goals are different: an enemy attacks, and an ally follows, protects and keeps out of the player's way.
- The decision code shares state with the enemy's animation keys.

**What an ally can reuse:**

- the player behaviors, as its actions;
- `GroundProbe` and `JumpPlanner`, as its senses;
- the weighted pick of `D1000Director` (`GlacierMan.pickMove` is a second copy of the same idea);
- `Step` and `Routine`, for scripted actions;
- `Fsm<S>`, after item `015`.

## Slots or a separate mechanism

The options:

- **S:** the ally takes a player slot, with input from an AI.
- **R:** a separate ally roster. Allies are not players, and the teams decide damage and targeting.
- **H:** S plus a seat layer. The slot stays the identity of the hero on the field (`O2-R5`). A seat is a human's
  input, settings, HUD and lives, and it binds to a slot. An AI driver drives a slot that has no seat.

| Scenario | S | R | H |
|---|---|---|---|
| 1 human + 1 AI | yes; its deaths cost shared lives | partly, until `030` and `112` | yes; a lives policy for each seat |
| 2 humans + 1 AI each | needs 4 slots; the bots count as humans | yes; same gaps as above | yes: 2 seats, 4 slots |
| A guest for one stage | takes P2's slot | good fit | a slot with no seat |
| A human takes over a bot, then gives it back | easy | hard | easy |
| An ally turns hostile (`120`) | as planned | flip its faction; it can never be played | as S |
| An escort that cannot fight | bad | right fit | not a slot; use R |
| PvP with bots | yes, up to the slot limit | needs teams | yes |
| Spec Ops with 1 human + bot | yes, with shared lives | weak | yes |

**What shipped games do (from the web survey):**

- **A seat that a human or a bot fills:** every game where a human can take over a bot uses this model. Examples:
  Sonic 2 Tails, Kirby helpers, Left 4 Dead, Halo 5, Gears 2, Baldur's Gate 3.
- **A slot chosen at setup:** Smash Bros. and Mario Kart.
- **A follower with no slot:** Diablo II and III.

The survey found no action platformer where an AI ally has its own lives and HUD. Tails has neither, and he respawns
for free.

**Measured counts:**

- 633 `targetPlayer` lines;
- 126 group sites (`players.forEach`, `anyLive`, and the others) in 65 files;
- 25 `PlayerSlot(0)` reads in 15 files;
- `MAX_PLAYERS = 2` (`LaunchOptionsResolver.kt:57`).

## External techniques

| Technique | Use here | Source |
|---|---|---|
| Tails: a delayed copy of P1's input, and a respawn when off screen | the first follow brain (`178a`) | info.sonicretro.org/SPG:Player_2 |
| Kirby: the ally warps to the player when off screen | catch-up recovery | wikirby.com/wiki/Ally |
| A jump-link nav graph built from tiles (Pignole) | the ground graph (`178b`, `178c`) | gamedeveloper.com, "The Hobbyist Coder #3: 2D platformers pathfinding" |
| Grid A* with a jump state, plus a 2-state follower (Branicki) | the follower | code.tutsplus.com, "A* pathfinding for 2D grid-based platformers: making a bot follow the path" |
| Forward-model A* (Baumgarten, Mario AI) | short local checks only | github.com/jumoel/mario-astar-robinbaumgarten |
| Utility scoring (Dragon Age: Inquisition) | the combat choices (`178d`) | Game AI Pro 3, chapter 31 |
| Ellie's rules (The Last of Us): stay close, never block, fire on permission | the companion rules (`178d`) | Game AI Pro 2, chapter 35 |
| Left 4 Dead bots: a joining human takes over a bot | the takeover (`178f`) | Valve, "The AI Systems of Left 4 Dead" (2009) |
