# Process log: mega-refactor-016e-service-names

## Status

- State: active
- Goal: Mechanical rename of `game` and the old host member names to names that fit `MegaServices`
- Done: branch start; scan; `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` approved; plan steps 1-6; the
  owner's sanity pass and merge approval
- Next: squash-merge into `mega-refactor-016-master`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-29

- Branch start on `master`. Worktree `.worktrees/mega-refactor-016e-service-names`, created from the parent `mega-refactor-016-master`.
- Scan and draft `docs/SCOPE.md` (no code change). Copied the branch folder from `master` into the worktree.
  Baseline build at `ffb8741a0` (JDK 17, `engine:test core:test lwjgl3:jar`): passes; engine 575, core 272, 0
  failures; 81 unique warnings.
- Counts: 480 declarations `game: MegaServices` in 474 files; 2,229 code tokens `game` that are a `MegaServices`
  value, on 1,775 lines in 489 files; old member names `eventsMan`, `assMan`, `audioMan`, `state` read 120, 408, 55 and
  110 times through the holder and 18, 72, 75 and 31 times through the host. One name clash found: the local `assets`
  in `MegamanMaverickGame.queueAssets()`.
- Dry run of the rename in a scratch copy (not in this branch; scripts in the session scratchpad
  `016e/`): 532 files, 2,038 lines; each changed line is a token rename only; build and tests pass with the baseline
  counts; the same warning set; normalized `javap -c -p` of all 1,861 `core` main classes equal.
- `docs/SCOPE.md` approved by the owner ("Scope approved, go with your recommendations"); the 7 answers are in
  `docs/NOTES.md`. Draft `docs/CONTRACT.md`.
- `docs/CONTRACT.md` approved by the owner ("contract approved"). Draft `docs/PLAN.md`.
- `docs/PLAN.md` approved by the owner ("plan approved").
- Plan step 1 (baseline, at `7a627814c`, code equal to `ffb8741a0`): clean build passes; engine 575, core 272, 0
  failures; 81 unique warnings (the same set as the scan); `javap -c -p` of 1,861 `core` main classes saved.
- Plan step 2, `92fadbfbe`: the local `assets` in `MegamanMaverickGame.queueAssets()` → `queuedAssets`; 1 file, 6
  lines; `core:compileKotlin` passes; `javap -c -p` of the 13 `MegamanMaverickGame` classes equal to the baseline.
- Plan step 3, `1552d49f9` (mechanical): the file lists made again from the branch code are equal to the dry-run lists
  (474 services files, 59 host files, 20 other files with a `game` code token). The first try of the list script
  matched only code declarations and gave other counts (464, 53, 26); the dry run used a plain word match on
  `MegaServices` and `MegamanMaverickGame` (comments included). The script was changed to the dry-run method; no change
  to the rename. The clash scan again: no local `assets`, `events`, `audio` or `gameState` in `MegamanMaverickGame.kt`;
  the `services` names in the 3 tests are as in the scan. `rename.py` from the dry run, not changed. `check.py`: 532
  files, 2,038 lines, 0 other differences; tokens `game` 2,229 (489 files), `assMan` 493 (417), `eventsMan` 149 (69),
  `audioMan` 143 (70), `state` 166 (62); changed-file list equal to the dry run. Clean build: engine 575, core 272, 0
  failures; 81 warnings, the same set; normalized `javap -c -p` of 1,861 classes: 0 differences;
  `contact-rule-selection.txt`: no diff. Greps: 0 old member reads; 0 `game` code tokens in non-host services files;
  632 `game` code tokens stay (581 in 51 host files, 42 in `BackgroundLayerBuilder`, 9 in `DesktopLauncher`).
- Plan step 4, `0772d9cd0`: `.git-blame-ignore-revs` lists `1552d49f9`. `git blame --ignore-revs-file` on `Met.kt:59`
  gives `b05603f527`.
- Plan step 5, `79eb753fa`: root `CLAUDE.md`, 9 lines. A later read found one more line, the entity constructor
  `(game: MegaServices)` in "Factories"; fixed in `0a270988e` (1 line). The step 5 grep did not find it, because it
  looked only for the old member names.
- Plan step 6 (automated checks, on the head after step 5): clean build passes; engine 575, core 272, 0 failures; 81
  warnings, the same set; normalized `javap -c -p`: 0 differences; greps the same as step 3. 3 smoke launches
  (`timeout 30` on `./run-game-alpha.sh`, which includes the Gradle start): no arguments, `--level REFACTOR_TEST_016
  --room gravity_change`, `--level DESERT_MAN --room room10`; each ran to the timeout with no exception. The
  `DESERT_MAN` run logs the known `SpawnersLayerBuilder` errors for `Cactus` and `Bat`
  (`docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`), also on the base.
- Open: the owner's short sanity pass (`docs/CONTRACT.md`, "Manual playthrough").
- The owner's short sanity pass and merge approval (verbatim: "approved"). Merge into the parent.
