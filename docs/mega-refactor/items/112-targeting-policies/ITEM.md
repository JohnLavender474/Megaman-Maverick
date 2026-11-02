# 112: Targeting as a policy that each entity owns

- Status: planned
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: `047b`
- Related: `029`, `030`, `048`, `072`, `079`, `108`, `109`, `120`, `121`
- Branches: none yet

## Goal

Each entity that has a target owns its rule, in place of one service-wide function. `targetFor` and the `targetPlayer` extension on every entity go away.

`047b` keeps `MegaPlayers.targetFor` as a temporary dispatch point, with a nearest-live-player default. This item builds the mechanism that replaces it.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `TargetPolicy` and `TargetingComponent`, with a default when the component is absent. The design is in `029` `NOTES.md`, "Targeting design".
- Reusable policies, for example `Sticky(Nearest)`. A decorator is a policy that wraps a policy.
- A rule for a part of a boss to use the target of its parent.
- The removal of `targetPlayer` from entities that have no target. This is a separate branch, and it is the larger part: 613 lines in 145 entity files on 2026-11-01.

The first branch must run with the boss work in `047e`, because the boss parts are its first real callers (`O1-R5`). `047e` depends on that branch. `047e` keeps the boss decisions, for example what a boss does when its target dies.

## Owner's input

2026-10-23 (`029` decision): `targetFor` and `targetPlayer` must not stay global. Targeting belongs to the entities that need it, as a component or policy that each owns.

2026-11-01 (verbatim):

> I imagine many entities will have different rules about which player to target, and so being able to have a component/decorator/inheritance setup might be good.

2026-11-01 (verbatim), on nearest on each read:

> This is fine for a first pass. As I started earlier, this will need to be much more refined later on.

## Decisions

- 2026-11-01: the component lives in its own item, not in `047`, because it serves `O1` and all modes. No existing item fits: `029` is done, `048` removes base classes, `030` decides who can hurt whom, and `079` is enemy design.

## Open questions

- How long does `Sticky` hold? The scan recommended until the target dies.
- `IGameComponent.reset()` is the clear point for a stateful policy, but it runs before `onDestroy`. Reads in `onDestroy` or `runnablesOnDestroy` see a cleared hold (`WilyFinalBoss.kt:293,295`, `AbstractEnemy` drop logic).
- `addComponent` keys on the exact class (`GameEntity.kt:38`). Rules must be policies in one component class, not subclasses of a component.
- `072` names `029` and `047` as the items that move `targetPlayer`. This item now does that move.

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/items/029-player-contract/NOTES.md`, "Targeting design".
- `docs/mega-refactor/items/047-co-op/NOTES.md`, "`047b` scan".
