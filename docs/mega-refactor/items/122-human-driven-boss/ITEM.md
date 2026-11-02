# 122: Human-driven boss

- Status: idea
- Type: feature
- Objectives: `O8`, `O2`
- Depends on: `102`
- Related: `015`, `023`, `030`, `031`, `042`, `048`, `081`, `109`, `112`, `118`, `120`
- Branches: none yet

## Goal

Test whether a boss is fun to play. One player plays Mega Man, and another player drives a boss in that boss's own room. The first boss is `BigAssMaverickRobot` from the intro stage.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

Tier 1: the boss stays a boss and does not register as a player. It reads a slot frame. With one registered player, `targetPlayer`, the camera, the HUD, the boss bar, damage and the room all stay as they are.

1. The owner names the mode.
2. A launch path gives slot 1 a frame, and no `Megaman` registers there. `launch.players > 1` does both today (`MegamanMaverickGame.kt:321,448-456`).
3. `BigAssMaverickRobot` splits its brain at `:474-512` into `launchHand(...)` and `startOrbs()`. AI and human use these calls.
4. The owner plays it. Stop here if it is not fun.
5. A second caller from the ground fighters with a free move, for example `RodentMan`. A shared type starts only at this step.

Each brain split goes in the same pass as that boss's `048` batch, to avoid a redo with `015` and `042`.

Not in this item: a boss in a player slot (tier 2), which needs the whole table in `NOTES.md`, section 3.

## Owner's input

2026-11-02 (verbatim):

> For example, let's say I want to make it so that the PvP, one player plays as Mega Man against another player who plays as BigAssMaverickRobot (from the intro stage) or any of the robot masters or other bosses. As such, each one would be refactored to be driven by a parent "boss" entity which gets swapped out for a parent "player" entity. Similar can be done for Mega Man, X, etc., so that they can be changed out as bosses/players easily.

## Decisions

- (2026-11-02, owner) Do not convert all bosses. 18 bosses have 15,218 lines and 99 player-read lines, 5 of them are multi-entity set pieces with no fighter form, and no mode exists.
- (2026-11-02, owner) A parent swap is not the tool. It gives no controls. The real boundary is inside each boss: intent, move, steering and presentation. The fighter itself must hold the slot.
- (2026-11-02, owner) An asymmetric first mode (Mega against a human-driven boss in its own room) is enough.
- (2026-11-02, owner) This is a new item, not a sub-mode of `109`, because `109` is team-based.
- (2026-11-02, owner) The boss keeps automatic aim first, then a test.
- (2026-11-02, owner) A playable boss does not need to walk freely. New walk art for bosses with no free move is not in scope.
- (2026-11-02, owner) Boss against boss is not a goal now. It needs `023`.
- (2026-11-02, owner) Mega Man and X as boss or player use the input system that exists now, plus `030` and `120`, and need no boss conversion.

## Open questions

- What ends a round when the boss wins? Slot 0 death resets the level today (`MegaLevelScreen.kt:579-588`).

## Sources

- `NOTES.md` in this folder.
