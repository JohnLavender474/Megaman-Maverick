# 037: Explicit respawn state and named `on_respawn` actions

- Status: planned
- Type: refactor
- Depends on: `036`
- Related: `038`, `046`
- Branches: none yet

## Goal

Restore a known state on respawn (backgrounds, music, flags) instead of hand-placed `EventTrigger` workarounds.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- On respawn: reset to the level's initial state (backgrounds, BG offsets, music, flags marked reset-on-respawn), then apply the checkpoint state, then run `on_respawn` actions from a named-action registry.
- Remove the 18 `EventTrigger` workarounds for background visibility (DesertMan, WilyStage1).

## Owner's input

Answer to review item 36 (2026-10-22, verbatim):

> yes!

## Open questions

- Which states must survive a respawn (collected items, opened gates, defeated mini-bosses)? Today mini-bosses and pickups come back. (Decision G.)

## Sources

- `NOTES.md` in this folder.
- `items/036-checkpoints-as-data/NOTES.md`: the full spawn and checkpoint review.
