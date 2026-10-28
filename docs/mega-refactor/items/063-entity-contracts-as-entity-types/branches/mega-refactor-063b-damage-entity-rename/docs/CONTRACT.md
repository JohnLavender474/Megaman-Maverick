# Contract: mega-refactor-063b-damage-entity-rename

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-10-27

What must stay the same: everything. The branch renames two interfaces and adds a supertype to two others. No runtime
path changes:

- Kotlin resolves interface names at compile time. No code reads the names `IDamageable` or `IDamager` at runtime
  (no `simpleName`, no string, no reflection on them, except `EntityContractsTest`, whose test names follow the
  rename).
- `IChildEntity` and `IParentEntity` get `: IGameEntity` and no member. All their implementers are already
  `MegaGameEntity` subclasses, so no class gets a new member or a new override.

So the compiler and the rename check prove most of the contract. There is no temporary old-versus-new suite, because
no runtime path changes.

## Automated checks

All on a clean build (`JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
lwjgl3:jar`).

- [x] The build passes and the jar builds.
- [x] All engine and core tests pass, 0 failures. The counts are the baseline (counted at plan step 1) plus only the
  new permanent tests.
- [x] The compiler warnings are the same set as the baseline after the old names in the baseline lines are mapped to
  the new names (and the file names `IDamageable.kt`/`IDamager.kt` to `IDamageableEntity.kt`/`IDamagerEntity.kt`).
  Each new warning is listed in the process log.
- [x] **Rename check script** (scratchpad, run on the mechanical commit): every changed line in the diff of that
  commit equals its old line with the word replacements `IDamageable` → `IDamageableEntity` and `IDamager` →
  `IDamagerEntity` applied, in the same file and at the same position; the only other changes are the two file renames
  (`git mv`) in `E/damage/`; no non-`.kt` file changes. The script exits non-zero on any other change.
- [x] The mechanical commit builds on its own (`engine:test core:test` pass at that commit).
- [x] Greps on the branch head: no word `IDamageable` or `IDamager` in `engine/`, `core/` or `lwjgl3/`;
  `E/damage/` holds only `IDamageableEntity.kt` and `IDamagerEntity.kt`.
- [x] `.git-blame-ignore-revs` lists the full SHA of the mechanical commit. `git blame --ignore-revs-file
  .git-blame-ignore-revs` on a renamed import line shows the commit before the rename.
- [x] **Permanent engine test:** `IChildEntity` and `IParentEntity` are subtypes of `IGameEntity`.
- [x] `EntityContractsTest` passes with the new names (`IDamageableEntity` and `IDamagerEntity` are subtypes of
  `IGameEntity`).
- [x] `core/src/test/resources/contact-rule-selection.txt` is unchanged.

## Manual playthrough

Passed (owner, 2026-10-27: "All pass.").

One short sanity pass, after the implementation, from the worktree. Each room uses the damage path
(`DamageRule`, `DeathRule`, the damage negotiators) or a parent and child entity.

- `./run-game-alpha.sh --level INFERNO_MAN --room room1`: shoot the `SniperJoe` with normal and charged shots until it
  dies; let its bullets and the `FireballBar` fireballs hit Megaman (Megaman takes damage, blinks and is invincible for
  a short time); the `FireballBar` fireballs turn around their center as before.
- `./run-game-alpha.sh --level TIMBER_WOMAN --room room5`: the `CannoHoney` sends `Beezee`s; they hurt Megaman on
  contact; shoot the `Beezee`s and the `CannoHoney`; when the `CannoHoney` dies, its `Beezee`s are released and attack
  Megaman as before.
- `./run-game-alpha.sh --level RODENT_MAN --room room13`: touch a `Spike`; Megaman dies and respawns as before.

## Behaviors to check by review

- The diff of the mechanical commit has only the rename (the check script) and the two file renames.
- The non-mechanical commits change only the supertype lists of `IChildEntity` and `IParentEntity`, the new test,
  `.git-blame-ignore-revs` and the branch docs.

## Intended changes

None.
