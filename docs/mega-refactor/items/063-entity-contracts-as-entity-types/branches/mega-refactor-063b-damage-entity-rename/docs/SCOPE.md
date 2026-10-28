# Scope: mega-refactor-063b-damage-entity-rename

- Status: approved
- Approved: 2026-10-27 (owner: "Yes to all")

## Goal

The two damage contracts have names that say they are entities: `IDamageable` becomes `IDamageableEntity` and
`IDamager` becomes `IDamagerEntity`. The rename is one mechanical commit that `.git-blame-ignore-revs` lists. The two
types stay in `engine` (`E/damage/`); item `023` decides their module. `IChildEntity` and `IParentEntity` extend
`IGameEntity`. No gameplay change.

## Item

From `063` `ITEM.md`, "Proposed scope", second branch (rename only): "`IDamageable` → `IDamageableEntity`, `IDamager` →
`IDamagerEntity` (about 470 references in about 190 files; 480 in 169 per the `063a` research); a mechanical commit for
`.git-blame-ignore-revs`. Also (from the `063a` backlog): `IChildEntity`/`IParentEntity` extend `IGameEntity`."

Owner decision (2026-10-27, in `ITEM.md`, "Go with your recommendations on the damage-focused work"): "`063a` keeps
`IDamageable`/`IDamager` in `engine` and adds `: IGameEntity`. `063b` decides the module; a move to `core` goes in the
same mechanical commit as the rename, so the types move at most once."

## Current state

Base: `master` at `ce10fc4b1` (after `063a` and the Starkner fix). `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. All counts are from this scan.

### The damage contracts

- `E/damage/IDamageable.kt` (`interface IDamageable : IGameEntity`: `invincible`, `canBeDamagedBy`,
  `takeDamageFrom`) and `E/damage/IDamager.kt` (`interface IDamager : IGameEntity`: `canDamage`,
  `onDamageInflictedTo`). The package `com.maverick.game.engine.damage` has no other file.
- No other engine code and no engine test names them.
- Every related type is in `core`: `C/damage/` (`IDamageNegotiator`, `DamageNegotiation`, `StandardDamageNegotiator`,
  `CustomMapDamageNegotiator`, `SelfSizeDamageNegotiator`; all 5 import `IDamager`), the contact rule
  `C/world/contacts/rules/DamageRule.kt` (the only caller of the four callbacks), `AbstractHealthEntity` and
  `MegamanDamageNegotiator`.
- References (word match, `.kt` files):

  | Name | `core` main | `core` test | `engine` main | `engine` test | `lwjgl3` |
  |---|---|---|---|---|---|
  | `IDamageable` | 183 in 79 files | 4 in 2 | 3 in 2 | 0 | 0 |
  | `IDamager` | 287 in 114 files | 4 in 2 | 3 in 2 | 0 | 0 |

  484 references on 482 lines in 170 files. 195 of these lines are imports (79 `IDamageable`, 116 `IDamager`); no
  file uses a star import of the package. No other identifier starts with `IDamageable` or `IDamager` (for example
  `IDamageNegotiator` does not match the word), and no `IDamageableEntity` or `IDamagerEntity` exists yet.
- Outside `.kt` files, the names occur only in `docs/mega-refactor/` (item and branch history). Root `CLAUDE.md`,
  Gradle files, test resources (`contact-rule-selection.txt`), `utils/` and the project skills do not name them.
- Tests: `MegaPlayersTest.kt:105` (`mockk<IDamager>()`, a strict mock; `MegaPlayers.onPlayerDamaged` only passes it on),
  `EntityQueriesTest.kt:33` (`PartsEntity : MegaGameEntity, IDamageable, ...`), `EntityContractsTest.kt:31` (asserts
  that both are subtypes of `IGameEntity`; its test names use `simpleName`, so they follow the rename).
- All implementers are `MegaGameEntity` subclasses (production and tests; item `NOTES.md`, "Implementer counts").

### The one cast that depends on the module

- `C/entities/megaman/MegamanDamageNegotiator.kt:36`: `val entity = damager as MegaGameEntity`, then
  `entity.getTag()` (`:39`) and `entity.getType()` (`:48`). `getTag()` is on `ITaggable` (engine), but `getType()`
  returns `EntityType` (`ITypable<EntityType>`, core), so only `IMegaGameEntity` gives both. `IDamagerEntity :
  IMegaGameEntity` is possible only in `core`.
- No other cast of a damager or damageable value to an entity type exists. The 4 `as IBodyEntity` casts
  (`ToxicGoopShot.kt:71`, `ReactorManProjectile.kt:186`, `DrippingToxicGoop.kt:95`, `PreciousGem.kt:223`) need
  `body`, which neither `IGameEntity` nor `IMegaGameEntity` gives. The `it as ChargedShot` (and similar) casts in the
  negotiation lambdas cast to concrete classes and stay.

### `IChildEntity` and `IParentEntity`

- `E/entities/contracts/IChildEntity.kt` (`var parent: IGameEntity?`) and `IParentEntity.kt`
  (`interface IParentEntity<T>`, `var children: Array<T>`) extend nothing.
- Implementers: `IChildEntity` 7 (`SigmaRatClaw`, `GutsTankFist`, `PetitDevilChild`, `Bolt`, `Spike`,
  `ElectrocutieChild`, `Laser`), `IParentEntity` 17. All are `MegaGameEntity` subclasses.
- No cast depends on the change. The one type check, `RocketPlatform.kt:109` (`entity is IChildEntity`), stays
  useful. The `(parent as GutsTank)` style casts cast to concrete classes and stay.

## In scope

1. **Mechanical commit (scripted).** Word rename `IDamageable` → `IDamageableEntity` and `IDamager` →
   `IDamagerEntity` in all `.kt` files of `engine`, `core` and `lwjgl3` (main and test), and the two files renamed with
   `git mv` to `IDamageableEntity.kt` and `IDamagerEntity.kt` (same folder `E/damage/`, same package). Nothing else
   goes in this commit. A check script proves that every changed line maps back to the old line by the rename only.
2. **`.git-blame-ignore-revs`.** A separate commit adds the SHA of the mechanical commit (a commit cannot hold its own
   SHA). At the merge, `master` replaces it with the squash SHA (merge rule 4).
3. `IChildEntity : IGameEntity` and `IParentEntity<T> : IGameEntity` (they stay in `E/entities/contracts/`), in a
   separate commit, with a permanent test that both are subtypes of `IGameEntity`.

New API: none. The branch renames two types and adds supertypes to existing types. It adds no member, function or
type.

## Out of scope

- A bound `T : IGameEntity` on `IParentEntity<T>`: all 17 implementers use an entity type for `T`, but no caller needs
  the bound.
- The module of the damage contracts (owner, Q1): item `023`. So `IDamagerEntity` stays `: IGameEntity`, and the cast
  at `MegamanDamageNegotiator.kt:36` stays.
- The 4 `as IBodyEntity` casts on a damager or damageable (`body` is not on an entity contract).
- The damage design (`DamageSource`, profiles, tags): item `023`. Factions and root owner: item `030`.
- `id` and `group` (item `064`, branch `064a`).
- Docs outside the branch folder that name the old types (other items' notes and logs). They are history and stay as
  they are. Root `CLAUDE.md` does not name the types, so it does not change.

## Affected areas and risks

- About 170 files, but only the lines that name the two types. Kotlin resolves the names at compile time, and no code
  reads class names at runtime for these two interfaces (no reflection on them outside `EntityContractsTest`), so a
  wrong rename shows as a compile error, not as a behavior change.
- Risk: the script changes more than the rename (for example a word in a string or a comment that is not the type).
  The check script and a review of the diff catch it. The scan found no such word: all 484 matches are the type.
- The import sort order does not change: `IDamageableEntity` still sorts before `IDamager`, and no other import sorts
  between them.
- Git may show the two renamed files as delete and add, not as a rename (small files where a name in the body also
  changes). Acceptable for 2 files of 12 and 10 lines.
- `.git-blame-ignore-revs` lists the squash commit after the merge, so `git blame` also skips the small non-mechanical
  change of this branch (item 3). It is a few lines.
- If `master` changes the same lines before the merge, the rename script can run again on the new `master`, so a
  conflict is solved by running it again, not by hand.
- `EntityContractsTest` test names change with the rename (they use `simpleName`). The test count does not change.

## Overlap with other branches

- `064a` (`.worktrees/mega-refactor-064a-instance-identity`, item `064`). Its scope changed (coordinator, 2026-10-27):
  it moves groups into an engine `GroupsComponent` and touches `GameEngine`, engine components, `MegaGameEntity`,
  `MegaGameEntities` and `GameEntityUtils.kt`. It no longer touches `id`, `Laser.kt`, `TubeBeamerV2.kt` or
  `SmallIceCube.kt`. None of these files names `IDamageable` or `IDamager`, and this branch does not edit them. No
  shared file. Details in `docs/NOTES.md`; read again before the merge.

## Decisions

Owner, 2026-10-27, verbatim: "Yes to all".

- Q1 (module of `IDamageableEntity` and `IDamagerEntity`): (a), keep them in `engine` (`E/damage/`), rename only.
  Item `023` decides the module as part of its damage design. The cast at `MegamanDamageNegotiator.kt:36` stays.
  Facts that led to it: the engine has no caller; a move to `core` would have removed 1 cast; damage is a genre concept
  that the engine scope allows; `023` may change both types and may build an engine damage pipeline, so a move now
  could be undone there.
- Q2: include `IChildEntity : IGameEntity` and `IParentEntity<T> : IGameEntity`. It adds no member, function or type.

## Open questions

None.
