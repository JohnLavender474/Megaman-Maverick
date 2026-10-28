# Notes: mega-refactor-063b-damage-entity-rename

## Decisions

- 2026-10-27 (owner, verbatim: "Yes to all"), on the two open questions of `SCOPE.md`:
  - Q1: `IDamageableEntity` and `IDamagerEntity` stay in `engine` (`E/damage/`). Item `023` decides their module as
    part of its damage design. The cast at `MegamanDamageNegotiator.kt:36` stays.
  - Q2: `IChildEntity` and `IParentEntity` extend `IGameEntity` in this branch.

## Overlap with other branches

- `064a` (`.worktrees/mega-refactor-064a-instance-identity`).
  - Scan (2026-10-27): its `SCOPE.md` was the empty template; its `ITEM.md` planned `id` on `IMegaGameEntity` and
    edits in `Laser.kt`, `TubeBeamerV2.kt` and `SmallIceCube.kt` (which also hold renamed references).
  - Scope change (coordinator, 2026-10-27): `064a` moves groups into an engine `GroupsComponent`. It touches
    `GameEngine`, engine components, `MegaGameEntity`, `MegaGameEntities` and `GameEntityUtils.kt`, and no longer
    touches `id`, `Laser.kt`, `TubeBeamerV2.kt` or `SmallIceCube.kt`. None of its files names `IDamageable` or
    `IDamager`, and this branch edits none of them. No shared file now.
  - Update (coordinator, 2026-10-27, at the plan step): `064a` adds an engine `GroupsSystem` and `GroupsComponent`
    (new package `E/groups/`) and may add join/leave hooks or open functions to `E/systems/GameSystem.kt`. It does not
    touch `E/entities/contracts/`. This branch edits neither `E/groups/` nor `GameSystem.kt`, and its new test file
    (`EngineEntityContractsTest.kt`) has a name of its own. No shared file. If `064a` adds code that names
    `IDamageable` or `IDamager` before this branch merges, the rename script runs again on the new `master`. Read
    again at plan step 5.
  - Read again at plan step 5 (2026-10-27, `064a` at `39c03be48`, scope and contract approved, no code yet). Its
    scope: hooks in `E/systems/GameSystem.kt`, new `E/groups/` (`GroupsComponent`, `GroupsSystem`),
    `MegamanMaverickGame.createGameEngine()`, `MegaGameEntity`, `MegaGameEntities`, `GameEntityUtils.kt`, root
    `CLAUDE.md`, and new tests. None of these files names `IDamageable`, `IDamager`, `IChildEntity` or
    `IParentEntity` on `master`, and this branch changes none of them (it does not change root `CLAUDE.md`). No shared
    file. Either merge order works; if `064a` merges first with new code that names the old types, run `rename.sh`
    again.
