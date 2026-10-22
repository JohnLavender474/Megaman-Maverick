# Items

Every item of the mega refactor and every branch, with their status. Change this file only on
`master`. See `CLAUDE.md` for the rules, the IDs and the statuses.

- **Items:** one row per item. The item folder `items/<id>-<slug>/` holds `ITEM.md` (the item:
  goal, scope, owner's input, open questions) and `NOTES.md` (research).
- **Branches:** one row per branch. A branch belongs to one item. Its folder is
  `items/<id>-<slug>/branches/<branch>/`.
- **Started:** the date the first branch of the item (or the branch) was created.
- **Finished:** the date the item was done or dropped (or the branch merged or dropped).

## Open decisions

Decisions that block or shape many items. The letters are the names from the review discussion
(2026-10-22).

| Decision | Question | Items |
|----------|----------|-------|
| A | Bugs in GitHub Issues (install and authenticate `gh`) or in `docs/issues/`? | `000` |
| B | A separate `smokeTest` Gradle suite, or Kotest tags in `src/test`? | `004`, `005`, `006` |
| C | Level validator in Kotlin (`tools` subproject, `utils/` wrapper) or Python with a manifest? | `003` |
| D | Fixed tick: 60 Hz? Accept a physics retune? Slow down or skip time on slow PCs? | `010` |
| E | Are the `DarkSerketClaw` size lookup and the "Mega explosions do not hurt enemies" rule intended? | `005`, `023`, `030` |
| F | Render plan from Tiled marker layers, or from a property list? | `026` |
| G | Checkpoints forward-only? What survives a respawn? | `036`, `037` |
| H | Co-op: shared lives? Maximum players? | `029`, `031`, `047` |
| I | Number of save slots? Save the health tank fill? | `007` |
| J | Commit to the 16×14 view on all platforms? | `035`, `054` |
| K | Which entity needed the second spawn pass? | `019` |
| L | Publish the engine? (Sets the root package.) | `001` |
| M | Host name: `IMaverickGame`/`MaverickGame` or `IMaverickApp`/`MaverickApp`? | `001` |
| N | Rename `GameEngine` in the package rename? | `001` |
| O | Does the engine base class own the frame loop? | `001` |

## Items

| ID | Item | Type | Status | Depends on | Started | Finished |
|----|------|------|--------|------------|---------|----------|
| 000 | [Bugs found during the review](items/000-bugs/ITEM.md) | bugs | idea | none | | |
| 001 | [Maverick Engine](items/001-maverick-engine/ITEM.md) | refactor | planned | `001b`: `016`, `010`, `032` | | |
| 002 | [CI on every branch, one Gradle job, aligned test libraries](items/002-ci-all-branches/ITEM.md) | tooling | planned | none | | |
| 003 | [Level content validator (local tool in `utils/`, CI test)](items/003-level-validator/ITEM.md) | tooling | planned | `002` | | |
| 004 | [Entity build-and-init smoke test](items/004-entity-smoke-test/ITEM.md) | test | planned | `002` | | |
| 005 | [Golden damage-table test](items/005-golden-damage-test/ITEM.md) | test | planned | `004`, `002` | | |
| 006 | [Headless boot-and-run per level (stage A)](items/006-headless-boot-run/ITEM.md) | test | idea | `002`, `004` | | |
| 007 | [Versioned JSON save slots with legacy import](items/007-json-save-slots/ITEM.md) | feature | planned | none | | |
| 008 | [Dev launch options and level staging](items/008-dev-launch-options/ITEM.md) | tooling | planned | none | | |
| 009 | [Allocation and GC baseline per branch](items/009-gc-baseline/ITEM.md) | tooling | planned | none | | |
| 010 | [One simulation clock: fixed tick for gameplay, separate present phase](items/010-fixed-tick/ITEM.md) | refactor | idea | `011`, `017` | | |
| 011 | [One seedable, interceptable random source with named streams](items/011-game-random/ITEM.md) | refactor | planned | none | | |
| 012 | [Named, composable freezes with a reason table](items/012-named-freezes/ITEM.md) | refactor | planned | none | | |
| 013 | [Level flow state machine; pause as an overlay](items/013-level-flow-fsm/ITEM.md) | refactor | planned | `015` | | |
| 014 | [Table-driven contact and collision rules](items/014-contact-rules-table/ITEM.md) | refactor | planned | none | | |
| 015 | [One entity state machine (enter/update/exit, state-owned timers)](items/015-entity-fsm/ITEM.md) | refactor | planned | none | | |
| 016 | [Narrow services instead of `game` everywhere](items/016-narrow-services/ITEM.md) | refactor | planned | none | | |
| 017 | [Shared entity logic as systems and world-step hooks](items/017-world-step-hooks/ITEM.md) | refactor | planned | none | | |
| 018 | [Event-driven system membership; component IDs when needed](items/018-ecs-membership/ITEM.md) | refactor | idea | none | | |
| 019 | [One spawn phase per tick with destroy notifications](items/019-spawn-phase/ITEM.md) | refactor | idea | none | | |
| 020 | [Cheaper broad phase with stable contact order](items/020-broad-phase/ITEM.md) | refactor | planned | none | | |
| 021 | [Entity registry with stable readable IDs and aliases](items/021-entity-registry/ITEM.md) | refactor | planned | none | | |
| 022 | [Typed property schema per entity, exported to Tiled](items/022-spawn-prop-schema/ITEM.md) | feature | planned | `021` | | |
| 023 | [Damage profiles keyed by damage tags](items/023-damage-profiles/ITEM.md) | refactor | planned | `005` | | |
| 024 | [One `WeaponDef` per weapon; loadout per character](items/024-weapon-defs/ITEM.md) | refactor | planned | `029` | | |
| 025 | [Level manifest: traits, variants and rewards instead of level-identity checks](items/025-level-manifest/ITEM.md) | refactor | idea | none | | |
| 026 | [Per-level render layers and an allocation-free draw queue](items/026-render-layers/ITEM.md) | refactor | planned | none | | |
| 027 | [Non-null APIs instead of `!!`](items/027-non-null-apis/ITEM.md) | refactor | planned | none | | |
| 028 | [Game-owned texture and animation cache](items/028-texture-cache/ITEM.md) | refactor | planned | none | | |
| 029 | [`IPlayer` contract, players service and `CharacterDef`](items/029-player-contract/ITEM.md) | refactor | planned | none | | |
| 030 | [Faction and root owner instead of `owner == megaman`](items/030-faction/ITEM.md) | refactor | planned | `029`, `005` | | |
| 031 | [Input intents per player (`InputFrame`), sources and router](items/031-input-frames/ITEM.md) | refactor | idea | `029` | | |
| 032 | [Platform-neutral `engine` module](items/032-platform-neutral-engine/ITEM.md) | refactor | planned | none | | |
| 033 | [Per-level asset manifests and texture budgets](items/033-per-level-assets/ITEM.md) | refactor | planned | `028` | | |
| 034 | [Persisted settings file](items/034-persisted-settings/ITEM.md) | feature | planned | `007` | | |
| 035 | [Aspect-ratio policy for phones](items/035-aspect-ratio/ITEM.md) | research | idea | none | | |
| 036 | [Checkpoints as typed Tiled data: respawn point, triggers, order](items/036-checkpoints-as-data/ITEM.md) | feature | planned | none | | |
| 037 | [Explicit respawn state and named `on_respawn` actions](items/037-respawn-state/ITEM.md) | refactor | planned | `036` | | |
| 038 | [One ordered respawn routine instead of an event chain](items/038-respawn-routine/ITEM.md) | refactor | planned | none | | |
| 039 | [Retire unused assets and review dead code](items/039-retire-dead-assets-code/ITEM.md) | tooling | planned | none | | |
| 040 | [Atlas checks and one-command repack (keep TexturePacker)](items/040-atlas-checks/ITEM.md) | tooling | idea | none | | |
| 041 | [Lifecycle policy per entity, room inferred from geometry](items/041-lifecycle-policy/ITEM.md) | refactor | planned | `021`, `022` | | |
| 042 | [Split god classes and extract reusable motion and attack components](items/042-reusable-components/ITEM.md) | refactor | planned | none | | |
| 043 | [Lighter clone without a history rewrite](items/043-lighter-clone/ITEM.md) | tooling | planned | none | | |
| 044 | [Lazy debug and log strings](items/044-lazy-logging/ITEM.md) | refactor | planned | none | | |
| 045 | [Clear ownership rules and debug checks for `GameObjectPools`](items/045-pool-ownership/ITEM.md) | refactor | planned | none | | |
| 046 | [Backgrounds as pure functions of the camera, with visible-cell drawing](items/046-backgrounds/ITEM.md) | refactor | planned | none | | |
| 047 | [Co-op with multiple controllers](items/047-co-op/ITEM.md) | feature | planned | `029`, `031` | | |
| 048 | [Replace entity inheritance with components](items/048-components-not-inheritance/ITEM.md) | refactor | planned | none | | |
| 049 | [Reduce repetitive object-oriented patterns](items/049-reduce-repetitive-patterns/ITEM.md) | refactor | idea | none | | |
| 050 | [Define entities in data files](items/050-entities-in-data-files/ITEM.md) | feature | planned | `021`, `022` | | |
| 051 | [X-style camera bounds, auto-scroll and other camera behaviors](items/051-camera-behaviors/ITEM.md) | feature | planned | none | | |
| 052 | [Make values configurable](items/052-configurable-values/ITEM.md) | refactor | idea | none | | |
| 053 | [Clean up event-driven logic](items/053-event-cleanup/ITEM.md) | refactor | idea | none | | |
| 054 | [Support web browsers and Android](items/054-web-and-android/ITEM.md) | feature | planned | `032`, `033`, `035` | | |
| 055 | [Record and replay for contract checks](items/055-record-and-replay/ITEM.md) | feature | planned | `010`, `011`, `006`, `031` | | |

## Branches

| Branch | Item | Goal | Status | Started | Finished |
|--------|------|------|--------|---------|----------|
| [`mega-refactor-001a-package-rename`](items/001-maverick-engine/branches/mega-refactor-001a-package-rename/process-log.md) | 001 | Rename the engine root package to a Maverick Engine identity | planned | | |
| [`mega-refactor-001b-host-contract`](items/001-maverick-engine/branches/mega-refactor-001b-host-contract/process-log.md) | 001 | Add `IMaverickGame` and `MaverickGame` | blocked | | |
