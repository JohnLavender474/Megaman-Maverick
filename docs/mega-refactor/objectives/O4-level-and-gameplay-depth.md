# `O4`: Deeper level design, enemies and bosses, more variety

- Status: active

## Problem

- `P1`: Some players criticize the level design as basic, in comparison with modern fan games and the classic Capcom
  games. Robot master stages are fully linear (`078`).
- `P2`: Some players criticize enemy and boss patterns as basic. For example, `Starkner` only runs toward Megaman, and
  `Met` runs into a bottomless pit (`079`).
- `P3`: Levels have too little variety, and some good mechanics get little use (`078`). Before
  `051`, the camera could only follow within room bounds.

## Goal

Better, more intricate level design and gameplay that meets the standard of modern fan games and the classic Capcom
titles, with more variety. The camera zones and scrolling camera are one example.

## Done means

- Each robot master stage has at least one section that the owner rates as more than linear.
- Each boss has its new attacks, including a panic attack, mixed aiming and a richer death animation (`077`).
- A mechanic that a level introduces gets real use in that level.

## Rules

### `O4-R1`: Mechanics from Tiled

- Rule: a level mechanic is usable from Tiled, with no code for one level.
- Problem: `P3`
- Check: search for level-identity checks in the branch diff. The mechanic has map properties.
- Applies: `linked items`
- Active: `now`

### `O4-R2`: A test room for each mechanic

- Rule: a new mechanic has a room in a test map, so that it can be tried with no full stage.
- Problem: `P3`
- Check: the branch contract names the room with `--level` and `--room`.
- Applies: `linked items`
- Active: `now`

### `O4-R3`: Patterns as states

- Rule: new enemy and boss behavior uses the common state machine.
- Problem: `P2`
- Check: review the new entity logic for sequential `if` chains and hand-run timers.
- Applies: `linked items`
- Active: after `015`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O4-R1` | `partly` | `051` holds: `CameraSectionsLayerBuilder.kt:51-94` reads all zone, scroll and crush properties from the map. Old mechanics do not hold: low gravity is code for one level on 9 `MOON_MAN` lines (for example `AbstractItem.kt:112`, `BodyComponent.kt:432`), and the Inferno frozen variant is code (`FrozenBlock.kt:47`, `RisingLavaRiver.kt:165`). | `025` |
| `O4-R2` | `holds` | `RefactorTest051.tmx` is live (`LevelDefinition.kt:157`). The `051` contracts name 37 rooms, and all 37 exist with a player spawn. All 12 rooms with a crush surface are in a contract. `079a` (2026-11-18): `RefactorTest079.tmx` is live (`REFACTOR_TEST_079`) with 12 rooms, one per `Met` check; each room respawns its enemies, so a check repeats with no restart. `077g` (2026-11-20): `RefactorTest077.tmx` rooms `glacier_man` and `glacier_man_boss` (a copy of the live boss room). | none needed |
| `O4-R3` | `not checked` (waiting) | The rule starts after `015`. `IState.kt:5-12` has transitions only. Of 158 enemy and boss files at `5b4d76c5a`, 24 use `StateMachine`, 30 use `Loop`, and 82 use `Timer(` with neither. 0 enemy or boss files were added since then. `079a` (2026-11-18): `Met` uses a hand-made request/enter pattern on `EnumStateMachineBuilder` (`requestState`, `enterState`, an allowed-edge table); it is a candidate for the first `015` branch (`items/015-entity-fsm/ITEM.md`, "Candidates for the first branch"). `077g` (2026-11-20): `GlacierMan` moves between states with `goTo` and a tell state on `EnumStateMachineBuilder`; its fight tests read the state from the animation key. `078g` (2026-11-23): `TimberWalker` (first pass, test map only) uses a hand-made `when` on an enum state with `setState`; no tests yet (deferred to item `180`). | `015` |

## Redo risk

`015` must convert the `Met` of `079a` and the other hand-made state machines to `Fsm<S>`. The `Met` has an allowed-edge table and tests (`MetChargedHitTest`, `MetHelmetTest`, `MetMovementTest`), which lower the risk of the move. The `GlacierMan` of `077g` is in the same case, with 8 fight specs on `GlacierManRig`. The `TimberWalker` of `078g` has no tests yet; item `180` writes them before `015` can move it safely. `051` is data-driven and needs no redo.

## Items

| Item | Status | Note |
|------|--------|------|
| `051` | done | [X-style camera bounds, auto-scroll and other camera behaviors](../items/051-camera-behaviors/ITEM.md) (main) |
| `073` | dropped | [More gravity-block puzzles in Moon Man's stage](../items/073-moon-man-gravity-puzzles/ITEM.md) (main) |
| `074` | dropped | [More Starkner sections in Moon Man's stage](../items/074-moon-man-starkner-sections/ITEM.md) (main) |
| `075` | dropped | [A larger oasis section in Desert Man's stage](../items/075-desert-man-oasis/ITEM.md) (main) |
| `077` | active | [Better boss fights for every boss: new and panic attacks, aiming, death animations](../items/077-robot-master-attacks/ITEM.md) (main) |
| `078` | idea | [Stage redesigns (epic)](../items/078-robot-master-alt-paths/ITEM.md) (main) |
| `180` | idea | [Timber Walker: refine with an artist and put into the game](../items/180-timber-walker-integration/ITEM.md) (main) |
| `166` | dropped | [Desert Man's stage rework (epic)](../items/166-desert-man-stage/ITEM.md) (main) |
| `079` | active | [Smarter behavior for simple enemies](../items/079-smarter-enemies/ITEM.md) (main) |
| `080` | dropped | [A larger MockingBit and MockingByte section in Timber Woman's stage](../items/080-timber-woman-mocking-section/ITEM.md) (main) |
| `089` | idea | [Conditional crush surfaces](../items/089-conditional-crush-surfaces/ITEM.md) (main) |
| `015` | planned | [One entity state machine (enter/update/exit, state-owned timers)](../items/015-entity-fsm/ITEM.md) |
| `026` | planned | [Per-level render layers and an allocation-free draw queue](../items/026-render-layers/ITEM.md) |
| `036` | planned | [Checkpoints as typed Tiled data: respawn point, triggers, order](../items/036-checkpoints-as-data/ITEM.md) |
| `042` | planned | [Split god classes and extract reusable motion and attack components](../items/042-reusable-components/ITEM.md) |
| `058` | idea | [Camera rotation as its own mechanism, separate from gravity](../items/058-camera-rotation-mechanism/ITEM.md) |
| `068` | done | [Floor slopes for extra levels](../items/068-slopes/ITEM.md) |
| `123` | idea | [X6 flat step-down off a slope](../items/123-slope-flat-step-down/ITEM.md) (main) |
| `124` | idea | [Slope pass-through from below](../items/124-slope-pass-through/ITEM.md) (main) |
| `076` | done | [Better tileset art for every level](../items/076-final-stage-tileset/ITEM.md) |
| `086` | idea | [Player movement modifiers as a component](../items/086-player-movement-modifiers/ITEM.md) |
| `099` | dropped | [Use camera zones and scroll sections in real stages](../items/099-camera-sections-in-stages/ITEM.md) (main) |
| `115` | idea | [Partial sink and stuck behavior in sand](../items/115-partial-sink-stuck-in-sand/ITEM.md) (main) |
| `117` | idea | [Crush with a falling pushable block](../items/117-pushable-block-crush/ITEM.md) (main) |
| `118` | done | [Boss fights as routines, rules and leases](../items/118-boss-fight-routines/ITEM.md) |
| `121` | idea | [Enemy clones of a playable character](../items/121-playable-character-clones/ITEM.md) (main) |
| `136` | idea | [Co-op: a player can skip a spike floor in a tall room](../items/136-tall-room-spike-skip/ITEM.md) |
| `150` | idea | [Shared movement helpers for bosses and enemies](../items/150-boss-movement-helpers/ITEM.md) |
| `157` | idea | [D-1000 overdrive on the shared boss health phases](../items/157-d1000-health-phases/ITEM.md) |
| `158` | idea | [A shared lead-aim helper for bosses](../items/158-boss-lead-aim-helper/ITEM.md) |
| `159` | done | [New hand and head art for the intro boss](../items/159-intro-boss-hand-art/ITEM.md) (main) |
| `171` | idea | [A shared ledge-walk helper for enemies](../items/171-ledge-walk-helper/ITEM.md) |
| `172` | idea | [Mega Man picks up and wears a `Met` helmet](../items/172-wear-met-helmet/ITEM.md) (main) |
| `173` | idea | [Copied foot-sensor pairs to `GroundProbe`](../items/173-foot-sensors-to-ground-probe/ITEM.md) |
| `177` | idea | [Backgrounds drawn per room](../items/177-backgrounds-per-room/ITEM.md) |
| `178` | active | [NPCs and AI allies that fight next to the players](../items/178-npc-allies/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

## Open questions

- Which games are the standard for comparison? A named list would make "done" easier to judge.
