# Process log: mega-refactor-031e-bindings

## Status

- State: active
- Goal: per-slot bindings: one settings object per slot replaces `ControllerButtons`, the bindings screen rules, `--resetBindings`, `selectButtonAction` per slot
- Done:
- Next: step 2 (baseline)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. The goal is from the `031` decisions of 2026-10-25 and
  2026-10-26 (item `ITEM.md`).
- Scan on `5d4a37eac`: the root and `docs/mega-refactor/` `CLAUDE.md`, the item `ITEM.md` and `NOTES.md`, the `031d`
  folder, the 6 input issues, `C/input/`, `C/controllers/`, `E/controller/`, `E/input/`, `MegamanMaverickGame`,
  `MegaPlayers`, `MegaLevelScreen` (pause and `reset`), `LevelPauseScreen`, `ControllerSettingsScreen`,
  `GameSettingsPanel`, `DesktopLauncher`, the input tests, the `Lwjgl3Preferences` source, `~/.prefs/`, and the `051b` and
  `084a` scopes. Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md` with all 10 recommendations (verbatim: "yes"). Answers in `docs/SCOPE.md` and
  the item `ITEM.md`. One fact in the scope was corrected before the review: the keyboard prefs file exists, in the
  `031d` backup.
- Draft `docs/CONTRACT.md` written. Test rooms used (all checked in the live TMX `game_rooms` layers, each with a player
  spawn): `REFACTOR_TEST_029` `movement`, `enemy_aim` and `player_death` (`RefactorTest029.tmx`), `INTRO_STAGE` `room3`
  (`IntroStage_v3.tmx`). No new test room.
- The owner approved `docs/CONTRACT.md` ("contract approved"), with the recommendations for questions 2 to 5. Question 1:
  the owner has no prefs to protect ("I deleted the prefs, so nothing needs to be backed up"), so S1 is removed, and S2,
  S3 and X1 now only give a known start state. `docs/SCOPE.md`: the 2 text fixes, and the 2 test seams in the table of
  new APIs. Note: at this point `~/.prefs` holds 1 file and `~/.prefs.bak-031d` still exists; neither was changed.
- Draft `docs/PLAN.md` written (18 steps: branch start, baseline, `selectButtonAction` to `PlayerSettings`, the store and
  the slot file, keyboard codes, gamepad codes, deletion of the old storage, `selectButtonAction` at boot,
  `--resetBindings`, "RESET TO DEFAULTS", reserved keys, the `Enter` swap, labels, capture end, back to pause, contract
  checks, the owner's play test, merge).
- The owner approved `docs/PLAN.md` ("plan approved"), with the 3 recommendations. `docs/CONTRACT.md` Performance: the
  4 `playerSlot` reader lookups are added (a reading note, not a check).
- Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree `.worktrees/mega-refactor-031e-bindings`
  created from `master`.
