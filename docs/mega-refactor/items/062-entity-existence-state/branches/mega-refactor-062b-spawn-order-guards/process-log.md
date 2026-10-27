# Process log: mega-refactor-062b-spawn-order-guards

## Status

- State: merged
- Goal: engine guards on the order of spawn and destroy calls (the last call wins; no second `onSpawn` or `onDestroy`)
- Done: planning (scope, contract, plan approved); steps 1 to 7
- Next: none (merged 2026-10-27)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master` at `74b8c5a5c`. Worktree `.worktrees/mega-refactor-062b-spawn-order-guards`.
- Scan: a static review of all 393 `spawn()` and 319 `destroy()` calls in `core` (scratchpad scripts
  `classify_calls.py`, `site_ctx.py`; the unguarded list in `062b-unguarded.tsv`), the `onDestroy` bodies and the
  destroy runnables. Findings: no caller of path A or D; path B only in `FreezableEntityHandler`; path C in parents
  that destroy their children, reachable when a child and its parent die in the same frame, and not harmless (health
  is not reset on destroy, so the item roll, the disintegration, `BigJumpingJoe`'s `SniperJoe` and boss defeat orbs
  can run twice). `EventTrigger` and `MegamanTrailSpriteV2` destroy themselves in `onSpawn` (normal path). Draft
  `docs/SCOPE.md` with 3 open questions.
- Owner answers (2026-10-27): `RESPAWN_QUEUED`, `debug`, the destroy waits on path D. All scope questions closed.
- Scope approved (owner, 2026-10-27: "approved"). Draft `docs/CONTRACT.md`: the temporary suite compares old and new until the first odd call of a sequence, then checks the new engine against an oracle; eight manual checks with `--logTags GameEngine` (rooms with `CannoHoney`, `TorikoPlundge`, `BigJumpingJoe`, the `RodentMan` boss found in the live TMX files).
- Contract approved (owner, 2026-10-27: "approved"). Draft `docs/PLAN.md` (7 steps); two cases follow from the rules and are written in its design details: a destroy of a `RESPAWN_QUEUED` entity cancels the respawn, and a destroy of an entity inside its own `onDestroy` is refused (a variant of path C).
- Plan approved (owner, 2026-10-27: "Yes").
- Step 1 (baseline): clean build passes; 520 engine and 162 core tests, 0 failures; 110 unique warnings (scratchpad
  `062b-warnings-baseline.txt`).
- Step 2 (core tests on the baseline, not committed yet): in `CT/entities/EntityExistenceTest.kt`, "the ice shield is
  live after a thaw and a freeze in one frame" fails (`spawned` expected `true`, was `false`, line 83); "a child and
  its parent destroyed in one frame run the onDestroy of the child once" fails (expected `1`, was `2`, line 108).
  Both as the scope predicts.
- Step 3 (temporary suite on the baseline): `ET/TempOldGameEngine.kt` (today's engine; added: the tag name, read-only
  `isQueuedToSpawn`, `isQueuedToDestroy`, `inDestroyPhase`) and `ET/TempOldEngineContractTest.kt`. Half of the
  10,000 sequences are "clean" (only calls that are not odd, chosen from the state), so the comparison runs over whole
  sequences; the other half call at random, and nested callbacks can spawn and then destroy one entity. Result:
  passes in about 2 s; 4,554 clean sequences compared to the end; first odd call per sequence: A 606, B 529, C
  4,271 (+30 in its own `onDestroy`), D 10. With `NEW_ENGINE_FOLLOWS_RULES = true` on the baseline the oracle fails
  at once ("onDestroy of e1 with no onSpawn before it"), so it catches the old behavior; with destroys before spawns
  in the frozen copy, the comparison fails (sequence 0, step 10). Both changes undone.
- Step 4 (the guards). `RESPAWN_QUEUED`; `spawned` includes it; `GameEngine` implements the four rules, a respawn
  queue and a respawn phase after the destroy phase, and the waiting destroys of path D. The temporary suite found
  five things during the work, each fixed and then passing (plan details, written here as the plan requires):
  - The oracle's rule is "the last *accepted* call wins": `spawn`, `destroy`, then `spawn` of one entity before an
    update returns `false` for the second spawn (a spawn is already queued; the same today), and the entity ends
    destroyed. A refused call changes nothing, also the refusals of rules 1 and 3.
  - A spawn of an entity inside its own `onSpawn` is refused (a variant of path A: it ran `onSpawn` twice); the old
    engine's classification counts it as path A.
  - `reset()`/`dispose()`: callbacks during the destroys can queue a respawn or leave an entity in the destroy
    queue; the final queue clearing now sets every entity in the spawn, respawn and destroy queues to `UNSPAWNED`.
  - A destroy that cancels a respawn after the entity's `onDestroy` already ran sets `UNSPAWNED` (not
    `DESTROY_QUEUED`).
  - The respawn phase works on a copy of the respawn queue and skips cancelled entries (a cancel inside a respawn's
    `onSpawn` made the old loop throw `NoSuchElementException`); new respawns wait for the next update.
  The suite now also fails if the new engine throws, and clean sequences do not spawn an entity in its own `onSpawn`.
  Result: passes in about 2 s; 4,490 clean sequences compared to the end; first odd call per sequence: A 650, B 587,
  C 4,238 (+28 in its own `onDestroy`), D 7; 742,744 checks of the last accepted call; no alternation break, no
  exception. Permanent tests: `EntityExistenceStateTest` (21 tests; the `062a` odd-path tests rewritten for rules 1
  to 4; the random test now also checks the alternation and the last accepted call). Core: the `062a` shield test
  now expects the shield live after the thaw and freeze (the new shield test of step 2 was the same check, so it is
  removed); the child test passes. Tests: 528 engine, 163 core, 0 failures.
- Step 5 (contract checks, clean build): build and jar pass; 528 engine and 163 core tests, 0 failures; the 110
  warnings are the same set as the baseline; `contact-rule-selection.txt` unchanged. Launches with `--logTags
  GameEngine` of `REFACTOR_TEST_029` `enemy_aim` and `pickups`, `TIMBER_WOMAN --room room5`, `DESERT_MAN --room room3`,
  `RODENT_MAN --room room13` and `room16`, `INTRO_STAGE --room room12`: no exception until the timeout.
  `DESERT_MAN` logs five map errors at load (`SpawnersLayerBuilder`: `Cactus` on the `blocks` layer and `Bat` on the
  `specials` layer do not resolve to a class); the map and the builders are not touched by this branch. Bug
  candidate in `docs/TODO.md`.
- Step 6: the owner's play test passes (2026-10-27, "Done, all tests pass"; eight checks). No `GameEngine` line in the eight log files (`docs/NOTES.md`); the files moved to the scratchpad (`062b-play-logs/`).
- Before the merge: `019b` started on `master` (`f699c698f`, docs only); its folder read for overlap, written in `docs/NOTES.md`. `master`'s new commits are docs only, so no merge of `master` into this branch is needed.
- Step 7: the owner approved the rule wording and the merge (2026-10-27, "Approved"); `docs/SCOPE.md` says "the last accepted call". The temporary suite is deleted in this last commit. Clean build passes; 527 engine and 163 core tests, 0 failures.
