# Process log: mega-refactor-063b-damage-entity-rename

## Status

- State: active
- Goal: Rename `IDamageable`/`IDamager` to `IDamageableEntity`/`IDamagerEntity` (and decide their module); `IChildEntity`/`IParentEntity` extend `IGameEntity`
- Done: planning approved; plan steps 1 to 6; play test passed; automated checks pass again
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master`. Worktree `.worktrees/mega-refactor-063b-damage-entity-rename`.
- Scan on `ce10fc4b1`: 484 references to `IDamageable`/`IDamager` on 482 lines in 170 `.kt` files (195 import lines,
  no star import, no other identifier with these prefixes, no name outside `.kt` files except `docs/mega-refactor/`).
  The engine has no caller of the two types. Only `MegamanDamageNegotiator.kt:36` depends on the module (a cast that
  `: IMegaGameEntity` would remove). `IChildEntity` (7 implementers) and `IParentEntity` (17) are implemented only by
  `MegaGameEntity` subclasses; no cast depends on them. Read `064a`: its scope is not written yet; possible overlap in
  `TubeBeamerV2.kt`, `Laser.kt` and `SmallIceCube.kt` (import lines only), written in `docs/NOTES.md`.
- Draft `docs/SCOPE.md` with two open questions: Q1 module (recommendation: keep in `engine`), Q2 include
  `IChildEntity`/`IParentEntity : IGameEntity` (recommendation: include).
- Scope approved (owner, 2026-10-27, verbatim: "Yes to all"): Q1, the damage contracts stay in `engine` (item `023`
  decides the module); Q2, `IChildEntity`/`IParentEntity : IGameEntity` are in scope. `docs/SCOPE.md` updated (the
  `core` options removed), decisions in `docs/NOTES.md`.
- `064a` scope changed (coordinator): it moves groups into an engine `GroupsComponent` and no longer touches `id`,
  `Laser.kt`, `TubeBeamerV2.kt` or `SmallIceCube.kt`. No shared file with this branch. `docs/SCOPE.md` and
  `docs/NOTES.md` updated.
- Draft `docs/CONTRACT.md`: automated checks (clean build, tests, warnings with the names mapped, a rename check
  script on the mechanical commit, greps, `.git-blame-ignore-revs`, a permanent engine test for
  `IChildEntity`/`IParentEntity`); no temporary suite (no runtime path changes); three short manual checks in real
  rooms (rooms found with a scratchpad script on the live TMX files: each room has a player spawn and is not a boss
  room).
- Contract approved (owner, 2026-10-27, verbatim: "approved").
- `064a` adds an engine `GroupsSystem`/`GroupsComponent` and may change `GameSystem.kt` (coordinator); no shared file.
  `docs/NOTES.md` updated.
- Draft `docs/PLAN.md` (6 steps): baseline; scripted rename commit with a check script that compares each changed file
  with its old content after the word replacements; `.git-blame-ignore-revs`; `IChildEntity`/`IParentEntity :
  IGameEntity` with `EngineEntityContractsTest`; contract checks; after the play test, the checks again. New API: none.
- Plan approved (owner, 2026-10-27, verbatim: "Yes to both").
- Step 1 (baseline, clean build, JDK 17 Corretto): build and jar pass; engine 549 tests, core 192, 0 failures; 88
  unique warnings (scratchpad `063b-warnings-baseline.txt`; 3 of them name `IDamageable`, so a mapped copy
  `063b-warnings-baseline-mapped.txt` is the reference for step 5).
- Step 2: `rename.sh` (scratchpad) renamed the two files with `git mv` and replaced the two words in 170 files.
  `check_rename.py` passed on the staged change and on the commit: 170 files, 484 replacements, each file equal to its
  old content after the replacements; git shows the two file renames as delete plus add (accepted by the script, as
  `SCOPE.md` expected). `engine:test core:test` passed on the same tree before the commit (549, 192, 0 failures). No
  old name left. Commit `f555536eb`.
- Step 3: `.git-blame-ignore-revs` lists `f555536eb54adfb2cf674cbfec7deaa7b7ef0cac` (`e34950842`). `git blame` on
  `DamageRule.kt:6` shows `f555536eb` without the file and `4208cb5c7` with it.
- Step 4: `IChildEntity : IGameEntity`, `IParentEntity<T> : IGameEntity`; new `EngineEntityContractsTest` (2 cases).
  Engine 551, core 192, 0 failures. Commit `1425c215e`.
- Step 5 (automated checks, clean build): build and jar pass; engine 551 (549 + 2), core 192 (the same; the 2
  `EntityContractsTest` cases now read `IDamageableEntity`/`IDamagerEntity is an IGameEntity`); 0 failures. Warnings:
  the same 88 lines as the mapped baseline, no new warning. `check_rename.py` on `f555536eb` passes; no word
  `IDamageable`/`IDamager` in `engine/`, `core/`, `lwjgl3/`; `E/damage/` holds only the two new files;
  `contact-rule-selection.txt` unchanged. Review: the commits after the rename change only `.git-blame-ignore-revs`,
  `IChildEntity.kt`, `IParentEntity.kt`, the new test and the branch docs. `064a` read again: no shared file
  (`docs/NOTES.md`).
- Owner play test (2026-10-27, verbatim: "All pass."): the three manual checks in `docs/CONTRACT.md` pass.
- Step 6: no temporary suite to delete. Automated checks again (clean build): build and jar pass; engine 551, core
  192, 0 failures; warnings the same 88 lines as the mapped baseline; `check_rename.py` on `f555536eb` passes (170
  files, 484 replacements); no old name left; `contact-rule-selection.txt` unchanged; `.git-blame-ignore-revs` lists
  `f555536eb`.
