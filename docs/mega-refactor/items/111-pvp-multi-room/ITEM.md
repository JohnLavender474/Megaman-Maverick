# 111: PvP across multiple rooms

- Status: idea
- Type: feature
- Objectives: `O2`
- Depends on: `109`, `094`
- Related: `051`, `095`
- Branches: none yet

## Goal

PvP matches in which players can be in different rooms at the same time. This comes after the one-room target of `109`. It lifts the same-room limit of `O2-R6` for PvP.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- None yet.

## Owner's input

2026-11-01 (verbatim):

> It seems like PvP would best be designed in just a single large room, but having the ability to support multiple rooms in PvP is definitely a worthy goal. Let's make it so that first we implement just a single large room and then tackle multi-room support afterward.

## Decisions

- 2026-11-19 (from `109`): a room with `wrap=true` has no room transitions at all. Revisit this rule when this item starts, for example to allow top and bottom exits so that wrap rooms can stack.

## Open questions

- What is the room state of each view? `CameraRoomBounds` holds one current room, `levelState.currentRoom` is one value, and `LevelRoomHandler` submits one set of transition events with one `POSITION`.
- What does a room transition do when another player stays in the old room? Today a transition destroys the enemies (`LevelRoomHandler.kt:72`), and all players go to one point (`Megaman.kt:386`).
- Which entities stay alive in a room that one player left? This connects to the culling classes in `094`: cull when no player's screen renders the entity, or keep it until its room or area is no longer occupied.
- Does the same limit stay in Spec Ops (`108`)?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
