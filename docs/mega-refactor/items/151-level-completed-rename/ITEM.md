# 151: Rename the level progress vocabulary from "defeated" to "completed"

- Status: active
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `007` (the planned JSON save key), `036`, `060` (`MegaPlayer` hooks)
- Branches: `151a` (`mega-refactor-151a-level-completed-rename`, started 2026-11-11)

## Goal

The saved level progress of the game state is named "defeated": `GameState.levelsDefeated`, `isLevelDefeated`,
`addLevelDefeated`, `IGameStateListener.onAddLevelDefeated`, `PlayerProgressComponent.onLevelDefeated` and the
`MegaPlayer.onLevelDefeated` hook. The word belongs to a boss (`AbstractBoss.defeated`, `BOSS_DEFEATED`), not to a
level: the set also records `INTRO_STAGE` and test levels, and the write runs on `END_LEVEL`, next to
`screenOnCompletion`. The rename makes the progress flag read as what it is, the completion of a level, and keeps it
apart from the boss defeat vocabulary.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- A symbol rename of the family to "completed" in `core/src/main` and `core/src/test` (18 Kotlin files, 69
  occurrences on 2026-11-11). The boss vocabulary (`AbstractBoss.defeated`, `IBossListener.onBossDefeated`,
  `EventType.BOSS_DEFEATED`, `MegaLockReason.BOSS_DEFEATED`, `FightRule.AllDefeated`, `ConstKeys.DEFEATED`, the atlas
  regions) stays.
- Delete `GameState.allLevelsDefeated` (0 callers; it can never be true, because the `GAME_COMPLETE` path does not
  record `WILY_STAGE_3`).
- Fix the 3 exception messages in `GameState` that say "Levels defeated" for heart tanks, health tanks and
  enhancements.
- Update the live docs that name a family identifier: `players/README.md`, the open issue
  `docs/issues/2026-11-02-boss-props-have-two-sources.md`, and the `007` and `036` notes (the `007` JSON key becomes
  `levelsCompleted`). Done-item docs stay as history.
- The save string does not change: it holds positional enum names under the prefs key `state`.

## Owner's input

2026-11-11: "Nitpick: the `onLevelDefeated` func in `MegaPlayer.kt` has a bit of a confusing name. `onLevelBeaten`,
`onLevelAccomplished`, or `onLevelCompleted` might be a better name." Then: "Is it simply a rename to make all of
these use 'completed' instead of 'defeated'? If there are no adverse effects or edge cases, then I'd like to proceed
with the rename." After the review: "Go with your recommendations."

## Decisions

- 2026-11-11: the word is "completed", for the whole family, not only the `MegaPlayer` hook.
- 2026-11-11: `allLevelsDefeated` is deleted, not renamed. The 3 wrong "Levels defeated" messages are fixed in the
  same branch. The `007` note changes its JSON key to `levelsCompleted`.
- 2026-11-11: merge target: the branch squash-merges directly into `master` (a small item; the recommendation of the
  review, accepted with "go with your recommendations").
- 2026-11-11: the rename is its own item, not a QoL change in `060c`: it crosses 18 files and a listener interface,
  and `060c` is a feature branch on `MegaPlayer`.

## Open questions

## Sources

- `NOTES.md` in this folder.
