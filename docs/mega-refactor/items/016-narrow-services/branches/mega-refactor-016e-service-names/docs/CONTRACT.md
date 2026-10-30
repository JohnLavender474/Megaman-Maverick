# Contract: mega-refactor-016e-service-names

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "contract approved")

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-29).
The branch only renames names that Kotlin resolves at compile time: the value `game` of type `MegaServices` →
`services`, and the members `eventsMan`, `assMan`, `audioMan`, `state` → `events`, `assets`, `audio`, `gameState` on
`MegaServices` and on `MegamanMaverickGame`. No code reads these names at run time. So the main proof is that the
compiled code is the same, not a play test.

## Automated checks

All from the worktree root. The baseline is the parent at `ffb8741a0`.

- [x] `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar` passes.
      Engine tests: 575, core tests: 272, 0 failures (the baseline counts; the branch adds and removes no test).
- [x] Warnings: the same set of unique `^w:` lines as the baseline (81), after the old names are mapped to the new
      names (`grep "^w:" <log> | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u`).
- [x] Line check (`check.py`) on the mechanical commit: each changed line is the old line with only the listed token
      renames. The changed file and token counts match the dry run in `SCOPE.md` section 5 (532 files, 2,038 lines),
      or the difference is explained in the process log.
- [x] Bytecode check: `javap -c -p` of every `core` main class, before and after the branch, is equal after the name
      map (old and new names to one placeholder, constant-pool indexes removed, whitespace ignored). 0 differences.
- [x] Pre-step commit: `git show` of it changes only `MegamanMaverickGame.queueAssets()`, the local `assets` →
      `queuedAssets` (6 lines).
- [x] Greps on `core/src`, code only (comments and strings excluded):
      - no member read `.assMan`, `.eventsMan` or `.audioMan` stays, and no `.state` read on a `MegaServices` or host
        receiver stays;
      - no code token `game` stays in a file that names `MegaServices` (except where the value is the host);
      - the code tokens `game` that stay are all in the host files (screens, layer builders, backgrounds,
        `ScreenController`, `MegamanMaverickGame.kt`, host tests) and in `lwjgl3`.
- [x] `.git-blame-ignore-revs` lists the mechanical commit, in the format of the existing entries. `git blame
      --ignore-revs-file .git-blame-ignore-revs` on one renamed line shows a commit before the mechanical commit.
- [x] `contact-rule-selection.txt`: no diff against the base.
- [x] Short smoke launches (about 15 to 20 seconds each; a clean exit or the timeout with no exception in the log is a
      pass):
      - `./run-game-alpha.sh` (the start screens; host renames in the screens).
      - `./run-game-alpha.sh --level REFACTOR_TEST_016 --room gravity_change` (entities, gravity, camera flags).
      - `./run-game-alpha.sh --level DESERT_MAN --room room10` (enemies, pathfinding, sounds).

No temporary old-versus-new suite: the bytecode check does that work for a rename.

Results (2026-10-29, after plan step 5 at `79eb753fa`; the code is the same at `0a270988e`): clean build passes on the baseline, after the mechanical commit
and on the head; each time engine 575, core 272 tests, 0 failures; 81 unique warnings, the same set as the baseline
(also the same before the name map). Pre-step `92fadbfbe`: 1 file, 6 lines; `javap -c -p` of the 13
`MegamanMaverickGame` classes equal to the baseline. Mechanical commit `1552d49f9`: `check.py` gives 532 files, 2,038
changed lines, 0 lines with another difference; tokens `game` 2,229 (489 files), `assMan` 493 (417), `eventsMan` 149
(69), `audioMan` 143 (70), `state` 166 (62); all equal to the dry run. The file lists made again from the branch code
(474 services files, 59 host files, 20 other files with a `game` code token) and the list of changed files are equal to
the dry-run lists. Bytecode: `javap -c -p` of all 1,861 `core` main classes, after the name map: 0 differences. Greps:
0 member reads `.assMan`, `.eventsMan`, `.audioMan`; 0 `.state` reads on a `services` or host `game` receiver; 0 code
tokens `game` in a services file that is not a host file; 632 code tokens `game` stay, all host values: 581 in 51 host
files, 42 in `BackgroundLayerBuilder` (`params.game`), 9 in `DesktopLauncher`. The old names that stay in code are
locals, parameters and properties of other classes (decision 5), for example `val eventsMan = services.events` in
`Megaman.kt` and the `eventsMan`/`audioMan` properties of `MegaLevelScreen`. `.git-blame-ignore-revs` commit
`0772d9cd0`: `git blame --ignore-revs-file` on `Met.kt:59` shows `b05603f527` (2025-12-29), before the mechanical commit.
`contact-rule-selection.txt`: no diff against the base. 3 smoke launches (`timeout 30` on the run script, which
includes the Gradle start): the start screens, `REFACTOR_TEST_016 gravity_change`, `DESERT_MAN room10`; each ran to the
timeout with no exception. The `DESERT_MAN` log has the known `SpawnersLayerBuilder` errors for `Cactus` in `blocks`
and `Bat` in `specials` (`docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`); they are on the base too and
not from this branch.

## Manual playthrough

The bytecode check proves that the compiled game is the same. So this branch asks for one short sanity pass only. The
combined play test of the parent merge into `master` covers `016a` to `016e` together.

From `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-016e-service-names`:

- `./run-game-alpha.sh`: the title screen, the main menu and the stage select show and play their sounds; start a
  level from the stage select.
- `./run-game-alpha.sh --level DESERT_MAN --room room10`: Megaman moves, shoots, takes damage; enemies spawn, act and
  die with their sounds and drops; pause and resume.

Result (2026-10-29): the owner's sanity pass passes (verbatim: "approved").

## Behaviors to check

None beyond the list above. The save file is not touched: `GameState` only gets a new member name on the holder and
the host, and its class, its fields and the save format do not change.

## Intended changes

None.
