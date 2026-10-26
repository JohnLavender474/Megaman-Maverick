# Items

Every item and branch of the mega refactor. Change this file only on `master`. The rules, IDs,
statuses and column meanings are in `CLAUDE.md`.

## Items

| ID | Item | Type | Status | Depends on | Started | Finished |
|----|------|------|--------|------------|---------|----------|
| 000 | [Bugs found during the review](items/000-bugs/ITEM.md) | bugs | done | none | 2026-10-22 | 2026-10-22 |
| 001 | [Maverick Engine](items/001-maverick-engine/ITEM.md) | refactor | active | `001b`: `016`, `010`, `032` | 2026-10-22 | |
| 002 | [CI on every branch, one Gradle job, aligned test libraries](items/002-ci-all-branches/ITEM.md) | tooling | done | none | 2026-10-22 | 2026-10-22 |
| 003 | [Level content validator (local tool in `utils/`, CI test)](items/003-level-validator/ITEM.md) | tooling | planned | `002` | | |
| 004 | [Entity build-and-init smoke test](items/004-entity-smoke-test/ITEM.md) | test | planned | `002` | | |
| 005 | [Golden damage-table test](items/005-golden-damage-test/ITEM.md) | test | planned | `004`, `002` | | |
| 006 | [Headless boot-and-run per level (stage A)](items/006-headless-boot-run/ITEM.md) | test | idea | `002`, `004` | | |
| 007 | [Versioned JSON save slots with legacy import](items/007-json-save-slots/ITEM.md) | feature | planned | none | | |
| 008 | [Dev launch options and level staging](items/008-dev-launch-options/ITEM.md) | tooling | active | none | 2026-10-22 | |
| 009 | [Allocation and GC baseline per branch](items/009-gc-baseline/ITEM.md) | tooling | planned | none | | |
| 010 | [One simulation clock: fixed tick for gameplay, separate present phase](items/010-fixed-tick/ITEM.md) | refactor | idea | `011`, `017` | | |
| 011 | [One seedable, interceptable random source with named streams](items/011-game-random/ITEM.md) | refactor | planned | none | | |
| 012 | [Named, composable freezes with a reason table](items/012-named-freezes/ITEM.md) | refactor | planned | none | | |
| 013 | [Level flow state machine; pause as an overlay](items/013-level-flow-fsm/ITEM.md) | refactor | planned | `015` | | |
| 014 | [Table-driven contact and collision rules](items/014-contact-rules-table/ITEM.md) | refactor | done | none | 2026-10-22 | 2026-10-22 |
| 015 | [One entity state machine (enter/update/exit, state-owned timers)](items/015-entity-fsm/ITEM.md) | refactor | planned | none | | |
| 016 | [Narrow services instead of `game` everywhere](items/016-narrow-services/ITEM.md) | refactor | planned | none | | |
| 017 | [Shared entity logic as systems and world-step hooks](items/017-world-step-hooks/ITEM.md) | refactor | planned | none | | |
| 018 | [Event-driven system membership; component IDs when needed](items/018-ecs-membership/ITEM.md) | refactor | idea | none | | |
| 019 | [One spawn phase per tick with destroy notifications](items/019-spawn-phase/ITEM.md) | refactor | active | none | 2026-10-22 | |
| 020 | [Cheaper broad phase with stable contact order](items/020-broad-phase/ITEM.md) | refactor | planned | none | | |
| 021 | [Entity registry with stable readable IDs and aliases](items/021-entity-registry/ITEM.md) | refactor | planned | none | | |
| 022 | [Typed property schema per entity, exported to Tiled](items/022-spawn-prop-schema/ITEM.md) | feature | planned | `021` | | |
| 023 | [Damage profiles keyed by damage tags](items/023-damage-profiles/ITEM.md) | refactor | planned | `005` | | |
| 024 | [One `WeaponDef` per weapon; loadout per character](items/024-weapon-defs/ITEM.md) | refactor | planned | `029` | | |
| 025 | [Level manifest: traits, variants and rewards instead of level-identity checks](items/025-level-manifest/ITEM.md) | refactor | idea | none | | |
| 026 | [Per-level render layers and an allocation-free draw queue](items/026-render-layers/ITEM.md) | refactor | planned | none | | |
| 027 | [Non-null APIs instead of `!!`](items/027-non-null-apis/ITEM.md) | refactor | planned | none | | |
| 028 | [Game-owned texture and animation cache](items/028-texture-cache/ITEM.md) | refactor | planned | none | | |
| 029 | [`IPlayer` contract, players service and `CharacterDef`](items/029-player-contract/ITEM.md) | refactor | done | none | 2026-10-23 | 2026-10-25 |
| 030 | [Faction and root owner instead of `owner == megaman`](items/030-faction/ITEM.md) | refactor | planned | `029`, `005` | | |
| 031 | [Input frames per player (`InputFrame`), devices and bindings](items/031-input-frames/ITEM.md) | refactor | active | `029` | 2026-10-25 | |
| 032 | [Platform-neutral `engine` module](items/032-platform-neutral-engine/ITEM.md) | refactor | planned | none | | |
| 033 | [Per-level asset manifests and texture budgets](items/033-per-level-assets/ITEM.md) | refactor | planned | `028` | | |
| 034 | [Persisted settings file](items/034-persisted-settings/ITEM.md) | feature | planned | `007` | | |
| 035 | [Aspect-ratio policy for phones](items/035-aspect-ratio/ITEM.md) | research | planned | none | | |
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
| 056 | [Typed fixture receivers](items/056-typed-fixture-receivers/ITEM.md) | refactor | dropped | none | | 2026-10-22 |
| 057 | [Typed event channels and payloads](items/057-typed-event-channels/ITEM.md) | refactor | idea | none | | |
| 058 | [Camera rotation as its own mechanism, separate from gravity](items/058-camera-rotation-mechanism/ITEM.md) | refactor | idea | none | | |
| 059 | [Max health read through from the profile](items/059-max-health-read-through/ITEM.md) | refactor | idea | none | | |
| 060 | [Proto Man as a second playable character, with `CharacterDef`](items/060-proto-man/ITEM.md) | feature | idea | `024` | | |

## Branches

| Branch | Item | Goal | Status | Started | Finished |
|--------|------|------|--------|---------|----------|
| [`mega-refactor-001a-package-rename`](items/001-maverick-engine/branches/mega-refactor-001a-package-rename/process-log.md) | 001 | Rename the engine root package to a Maverick Engine identity | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-001b-host-contract`](items/001-maverick-engine/branches/mega-refactor-001b-host-contract/process-log.md) | 001 | Add `IMaverickGame` and `MaverickGame` | blocked | | |
| [`mega-refactor-019a-remove-second-spawn-pass`](items/019-spawn-phase/branches/mega-refactor-019a-remove-second-spawn-pass/process-log.md) | 019 | Run one spawn pass per frame instead of two | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-002a-ci-all-branches`](items/002-ci-all-branches/branches/mega-refactor-002a-ci-all-branches/process-log.md) | 002 | CI on every branch push, one Gradle job, aligned test libraries, JDK toolchain | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-008a-dev-launch-options`](items/008-dev-launch-options/branches/mega-refactor-008a-dev-launch-options/process-log.md) | 008 | Dev launch options and level staging | merged | 2026-10-22 | 2026-10-22 |
| `mega-refactor-019b-destroy-notification` | 019 | Tell a spawner at once when its entity is destroyed; one spawn phase per tick | planned | | |
| [`mega-refactor-029a-player-seam`](items/029-player-contract/branches/mega-refactor-029a-player-seam/process-log.md) | 029 | Player slot, player view and players service; `Megaman` in slot 0 | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029b-enemy-players`](items/029-player-contract/branches/mega-refactor-029b-enemy-players/process-log.md) | 029 | Enemies use the players service in place of `megaman` | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029c-boss-players`](items/029-player-contract/branches/mega-refactor-029c-boss-players/process-log.md) | 029 | Bosses and the boss damage listener use the players service | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029d-world-players`](items/029-player-contract/branches/mega-refactor-029d-world-players/process-log.md) | 029 | World entities use `IPlayer` and the players service | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029e-player-profile`](items/029-player-contract/branches/mega-refactor-029e-player-profile/process-log.md) | 029 | Lives, tanks, enhancements and weapons owned in one session profile | merged | 2026-10-23 | 2026-10-24 |
| [`mega-refactor-029f-level-players`](items/029-player-contract/branches/mega-refactor-029f-level-players/process-log.md) | 029 | Level screen, handlers and UI use the players service; remove `game.megaman` | merged | 2026-10-24 | 2026-10-24 |
| `mega-refactor-029g-behavior-split` | 029 | Split the player behaviors into factory functions | merged | 2026-10-25 | 2026-10-25 |
| [`mega-refactor-031a-input-frame`](items/031-input-frames/branches/mega-refactor-031a-input-frame/process-log.md) | 031 | One `InputFrame` per player slot; gameplay reads move to it | merged | 2026-10-25 | 2026-10-25 |
| [`mega-refactor-031b-engine-input`](items/031-input-frames/branches/mega-refactor-031b-engine-input/process-log.md) | 031 | Input frames as an engine mechanism; `ControllerSystem` dispatches from each entity's own input source | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-031c-input-pipeline`](items/031-input-frames/branches/mega-refactor-031c-input-pipeline/process-log.md) | 031 | A new input pipeline with the same behavior: device adapters, held masks, an engine edge builder with the lock; the poller goes | active | 2026-10-26 | |
| [`mega-refactor-014a-fixture-type`](items/014-contact-rules-table/branches/mega-refactor-014a-fixture-type/process-log.md) | 014 | Narrow the engine fixture type from `Any` | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014b-typed-fixture-receivers`](items/014-contact-rules-table/branches/mega-refactor-014b-typed-fixture-receivers/process-log.md) | 014 | One typed receiver registration in place of the hand-written extension trios | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014c-contact-rules-table`](items/014-contact-rules-table/branches/mega-refactor-014c-contact-rules-table/process-log.md) | 014 | Replace the contact `if/else` chain with an engine rule table and per-pair rule classes | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014d-contact-receivers`](items/014-contact-rules-table/branches/mega-refactor-014d-contact-receivers/process-log.md) | 014 | Replace the `Megaman` checks in the contact rules with receivers and capabilities; `MegaCollisionHandler` body flags | merged | 2026-10-22 | 2026-10-22 |
