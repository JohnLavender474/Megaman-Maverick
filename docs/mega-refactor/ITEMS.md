# Items

Every item and branch of the mega refactor. Change this file only on `master`. The rules, IDs,
statuses and column meanings are in `CLAUDE.md`.

## Items

| ID | Item | Type | Objectives | Status | Depends on | Started | Finished |
|----|------|------|------------|--------|------------|---------|----------|
| 000 | [Bugs found during the review](items/000-bugs/ITEM.md) | bugs | `O6` | done | none | 2026-10-22 | 2026-10-22 |
| 001 | [Maverick Engine](items/001-maverick-engine/ITEM.md) | refactor | `O1` | active | `001b`: `016`, `010`, `032` | 2026-10-22 | |
| 002 | [CI on every branch, one Gradle job, aligned test libraries](items/002-ci-all-branches/ITEM.md) | tooling | `O6` | done | none | 2026-10-22 | 2026-10-22 |
| 003 | [Level content validator (local tool in `utils/`, CI test)](items/003-level-validator/ITEM.md) | tooling | `O6`, `O7` | planned | `002` | | |
| 004 | [Entity build-and-init smoke test](items/004-entity-smoke-test/ITEM.md) | test | `O6` | done | `002` | 2026-11-01 | 2026-11-01 |
| 005 | [Golden damage-table test](items/005-golden-damage-test/ITEM.md) | test | `O6` | planned | `004`, `002` | | |
| 006 | [Headless boot-and-run per level (stage A)](items/006-headless-boot-run/ITEM.md) | test | `O6` | idea | `002`, `004` | | |
| 007 | [Versioned JSON save slots with legacy import](items/007-json-save-slots/ITEM.md) | feature | `O9` | planned | none | | |
| 008 | [Dev launch options and level staging](items/008-dev-launch-options/ITEM.md) | tooling | `O6` | active | none | 2026-10-22 | |
| 009 | [Allocation and GC baseline per branch](items/009-gc-baseline/ITEM.md) | tooling | `O5`, `O6` | planned | none | | |
| 010 | [One simulation clock: fixed tick for gameplay, separate present phase](items/010-fixed-tick/ITEM.md) | refactor | `O2`, `O6`, `O5` | idea | `011`, `017` | | |
| 011 | [One seedable, interceptable random source with named streams](items/011-game-random/ITEM.md) | refactor | `O2`, `O6` | planned | none | | |
| 012 | [Named, composable freezes with a reason table](items/012-named-freezes/ITEM.md) | refactor | `O1`, `O2` | planned | none | | |
| 013 | [Level flow state machine; pause as an overlay](items/013-level-flow-fsm/ITEM.md) | refactor | `O1`, `O2` | planned | `015` | | |
| 014 | [Table-driven contact and collision rules](items/014-contact-rules-table/ITEM.md) | refactor | `O1` | done | none | 2026-10-22 | 2026-10-22 |
| 015 | [One entity state machine (enter/update/exit, state-owned timers)](items/015-entity-fsm/ITEM.md) | refactor | `O1`, `O4` | planned | none | | |
| 016 | [Narrow services instead of `game` everywhere](items/016-narrow-services/ITEM.md) | refactor | `O1`, `O2` | done | none | 2026-10-28 | 2026-10-30 |
| 017 | [Shared entity logic as systems and world-step hooks](items/017-world-step-hooks/ITEM.md) | refactor | `O1`, `O5` | planned | none | | |
| 018 | [Event-driven system membership; component IDs when needed](items/018-ecs-membership/ITEM.md) | refactor | `O5`, `O1` | idea | none | | |
| 019 | [One spawn phase per tick with destroy notifications](items/019-spawn-phase/ITEM.md) | refactor | `O1`, `O2` | done | none | 2026-10-22 | 2026-11-03 |
| 020 | [Cheaper broad phase with stable contact order](items/020-broad-phase/ITEM.md) | refactor | `O5`, `O2` | planned | none | | |
| 021 | [Entity registry with stable readable IDs and aliases](items/021-entity-registry/ITEM.md) | refactor | `O7`, `O3` | planned | none | | |
| 022 | [Typed property schema per entity, exported to Tiled](items/022-spawn-prop-schema/ITEM.md) | feature | `O7` | planned | `021` | | |
| 023 | [Damage profiles keyed by damage tags](items/023-damage-profiles/ITEM.md) | refactor | `O1` | planned | `005` | | |
| 024 | [One `WeaponDef` per weapon; loadout per character](items/024-weapon-defs/ITEM.md) | refactor | `O8`, `O1` | active | `029` | 2026-11-14 | |
| 025 | [Level manifest: traits, variants and rewards instead of level-identity checks](items/025-level-manifest/ITEM.md) | refactor | `O7`, `O1` | idea | none | | |
| 026 | [Per-level render layers and an allocation-free draw queue](items/026-render-layers/ITEM.md) | refactor | `O5`, `O4` | planned | none | | |
| 027 | [Non-null APIs instead of `!!`](items/027-non-null-apis/ITEM.md) | refactor | `O1` | planned | none | | |
| 028 | [Game-owned texture and animation cache](items/028-texture-cache/ITEM.md) | refactor | `O1`, `O3` | planned | none | | |
| 029 | [`IPlayer` contract, players service and `CharacterDef`](items/029-player-contract/ITEM.md) | refactor | `O2`, `O8`, `O1` | done | none | 2026-10-23 | 2026-10-25 |
| 030 | [Faction and root owner instead of `owner == megaman`](items/030-faction/ITEM.md) | refactor | `O2`, `O8` | planned | `029`, `005` | | |
| 031 | [Input frames per player (`InputFrame`), devices and bindings](items/031-input-frames/ITEM.md) | refactor | `O2`, `O9` | done | `029` | 2026-10-25 | 2026-11-01 |
| 032 | [Platform-neutral `engine` module](items/032-platform-neutral-engine/ITEM.md) | refactor | `O3`, `O1` | planned | none | | |
| 033 | [Per-level asset manifests and texture budgets](items/033-per-level-assets/ITEM.md) | refactor | `O3`, `O5` | planned | `028` | | |
| 034 | [Persisted settings file](items/034-persisted-settings/ITEM.md) | feature | `O9` | planned | `007` | | |
| 035 | [Aspect-ratio policy for phones](items/035-aspect-ratio/ITEM.md) | research | `O3` | planned | none | | |
| 036 | [Checkpoints as typed Tiled data: respawn point, triggers, order](items/036-checkpoints-as-data/ITEM.md) | feature | `O7`, `O4` | planned | none | | |
| 037 | [Explicit respawn state and named `on_respawn` actions](items/037-respawn-state/ITEM.md) | refactor | `O1` | planned | `036` | | |
| 038 | [One ordered respawn routine instead of an event chain](items/038-respawn-routine/ITEM.md) | refactor | `O1`, `O2` | done | none | 2026-11-06 | 2026-11-06 |
| 039 | [Retire unused assets and review dead code](items/039-retire-dead-assets-code/ITEM.md) | tooling | `O1`, `O3` | planned | none | | |
| 040 | [Atlas checks and one-command repack (keep TexturePacker)](items/040-atlas-checks/ITEM.md) | tooling | `O6` | idea | none | | |
| 041 | [Lifecycle policy per entity, room inferred from geometry](items/041-lifecycle-policy/ITEM.md) | refactor | `O7` | planned | `021`, `022` | | |
| 042 | [Split god classes and extract reusable motion and attack components](items/042-reusable-components/ITEM.md) | refactor | `O1`, `O4` | planned | none | | |
| 043 | [Lighter clone without a history rewrite](items/043-lighter-clone/ITEM.md) | tooling | `O1` | planned | none | | |
| 044 | [Lazy debug and log strings](items/044-lazy-logging/ITEM.md) | refactor | `O5` | planned | none | | |
| 045 | [Clear ownership rules and debug checks for `GameObjectPools`](items/045-pool-ownership/ITEM.md) | refactor | `O1`, `O5` | planned | none | | |
| 046 | [Backgrounds as pure functions of the camera, with visible-cell drawing](items/046-backgrounds/ITEM.md) | refactor | `O5`, `O1` | planned | none | | |
| 047 | [Co-op with multiple controllers](items/047-co-op/ITEM.md) | feature | `O2` | active | `029`, `031` | 2026-11-01 | |
| 048 | [Replace entity inheritance with components](items/048-components-not-inheritance/ITEM.md) | refactor | `O1` | planned | none | | |
| 049 | [Reduce repetitive object-oriented patterns](items/049-reduce-repetitive-patterns/ITEM.md) | refactor | `O1` | idea | none | | |
| 050 | [Define entities in data files](items/050-entities-in-data-files/ITEM.md) | feature | `O7` | planned | `021`, `022` | | |
| 051 | [X-style camera bounds, auto-scroll and other camera behaviors](items/051-camera-behaviors/ITEM.md) | feature | `O4`, `O1`, `O2` | done | none | 2026-10-30 | 2026-11-01 |
| 052 | [Make values configurable](items/052-configurable-values/ITEM.md) | refactor | `O1`, `O7` | idea | none | | |
| 053 | [Clean up event-driven logic](items/053-event-cleanup/ITEM.md) | refactor | `O1` | idea | none | | |
| 054 | [Support web browsers and Android](items/054-web-and-android/ITEM.md) | feature | `O3` | planned | `032`, `033`, `035` | | |
| 055 | [Record and replay for contract checks](items/055-record-and-replay/ITEM.md) | feature | `O6`, `O2` | planned | `010`, `011`, `006`, `031` | | |
| 056 | [Typed fixture receivers](items/056-typed-fixture-receivers/ITEM.md) | refactor | `O1` | dropped | none | | 2026-10-22 |
| 057 | [Typed event channels and payloads](items/057-typed-event-channels/ITEM.md) | refactor | `O1` | idea | none | | |
| 058 | [Camera rotation as its own mechanism, separate from gravity](items/058-camera-rotation-mechanism/ITEM.md) | refactor | `O1`, `O4` | idea | none | | |
| 059 | [Max health read through from the profile](items/059-max-health-read-through/ITEM.md) | refactor | `O2`, `O1` | idea | none | | |
| 060 | [Proto Man as a playable character](items/060-proto-man/ITEM.md) | feature | `O8` | active | `081a`, `084`; `024` for `060h`; `081` for `060i` | 2026-11-10 | |
| 061 | [Thin player view; locks and signals as components and functions](items/061-thin-player/ITEM.md) | refactor | `O2`, `O8`, `O1` | done | `029`, `031` | 2026-10-26 | 2026-10-26 |
| 062 | [One engine-owned entity existence state](items/062-entity-existence-state/ITEM.md) | refactor | `O1` | done | none | 2026-10-26 | 2026-10-27 |
| 063 | [Entity contracts are entity types](items/063-entity-contracts-as-entity-types/ITEM.md) | refactor | `O1` | done | none | 2026-10-27 | 2026-10-28 |
| 064 | [Entity instance identity: `id` and `group`](items/064-entity-instance-identity/ITEM.md) | refactor | `O1` | done | none | 2026-10-27 | 2026-10-28 |
| 065 | [Every `init()` calls `super.init()`](items/065-init-calls-super/ITEM.md) | refactor | `O1` | planned | none | | |
| 066 | [`GameSystem.reset()` clears pending removes](items/066-game-system-reset-pending-removes/ITEM.md) | refactor | `O1` | done | none | 2026-11-14 | 2026-11-14 |
| 067 | [PPM 16 with 1x pixel art](items/067-ppm-16/ITEM.md) | refactor | `O5`, `O3` | planned | none | | |
| 068 | [Floor slopes for extra levels](items/068-slopes/ITEM.md) | feature | `O8`, `O4` | done | none | 2026-11-03 | 2026-11-03 |
| 069 | [A dash button for X](items/069-dash-button/ITEM.md) | feature | `O8` | dropped | `031`, `081a` | | 2026-11-05 |
| 070 | [More flexible controller settings](items/070-flexible-controls/ITEM.md) | feature | `O9` | idea | `031` | | |
| 071 | [`getEntity()` without a cast to `MegaGameEntity`](items/071-typed-get-entity/ITEM.md) | refactor | `O1` | idea | none | | |
| 072 | [Remove `IMegaGameEntity`](items/072-remove-imegagameentity/ITEM.md) | refactor | `O1` | planned | `016` | | |
| 073 | [More gravity-block puzzles in Moon Man's stage](items/073-moon-man-gravity-puzzles/ITEM.md) | feature | `O4` | idea | none | | |
| 074 | [More Starkner sections in Moon Man's stage](items/074-moon-man-starkner-sections/ITEM.md) | feature | `O4` | idea | none | | |
| 075 | [A larger oasis section in Desert Man's stage](items/075-desert-man-oasis/ITEM.md) | feature | `O4` | idea | none | | |
| 076 | [A new tileset for the final stage](items/076-final-stage-tileset/ITEM.md) | feature | `O9`, `O4` | idea | none | | |
| 077 | [Better boss fights for every boss: new and panic attacks, aiming, death animations](items/077-robot-master-attacks/ITEM.md) | feature | `O4` | active | none | 2026-11-13 | |
| 078 | [Alt paths in the robot master stages](items/078-robot-master-alt-paths/ITEM.md) | feature | `O4` | idea | none | | |
| 079 | [Smarter behavior for simple enemies](items/079-smarter-enemies/ITEM.md) | feature | `O4` | idea | none | | |
| 080 | [A larger MockingBit and MockingByte section in Timber Woman's stage](items/080-timber-woman-mocking-section/ITEM.md) | feature | `O4` | idea | none | | |
| 081 | [Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`](items/081-playable-x/ITEM.md) | feature | `O8` | active | `047b`, `084` | 2026-11-06 | |
| 082 | [Dialogue boxes, in-level dialogue scenes and cutscenes](items/082-dialogue-and-cutscenes/ITEM.md) | feature | `O9` | planned | `012`, `031` | | |
| 083 | [`PathfinderResult.targetReached` means what it says](items/083-pathfinder-target-reached/ITEM.md) | refactor | `O1` | done | none | 2026-11-14 | 2026-11-14 |
| 084 | [Split `Megaman` into player components](items/084-split-megaman/ITEM.md) | refactor | `O8`, `O1` | done | none | 2026-10-30 | 2026-11-01 |
| 085 | [Remove the dead `ConstKeys.FOCUS` override in `Megaman`](items/085-dead-focus-override/ITEM.md) | refactor | `O1` | dropped | none | | 2026-10-31 |
| 086 | [Player movement modifiers as a component](items/086-player-movement-modifiers/ITEM.md) | refactor | `O1`, `O4` | idea | none | | |
| 087 | [Reset order for components](items/087-component-reset-order/ITEM.md) | refactor | `O1` | idea | none | | |
| 088 | [Web player (TeaVM Wasm)](items/088-web-player/ITEM.md) | feature | `O3` | idea | `021`, `032` | | |
| 089 | [Conditional crush surfaces](items/089-conditional-crush-surfaces/ITEM.md) | feature | `O4` | idea | `051` | | |
| 090 | [Branch name in the game window title](items/090-branch-window-title/ITEM.md) | tooling | `O6` | planned | none | | |
| 091 | [Check that a test room loads with no visible game window](items/091-room-load-check/ITEM.md) | tooling | `O6` | planned | none | | |
| 092 | [Remove old entity versions and the version suffixes](items/092-old-entity-versions/ITEM.md) | refactor | `O1`, `O7` | idea | `084e` | | |
| 093 | [Use or remove `MegaRegionProcessor`](items/093-mega-region-processor/ITEM.md) | refactor | `O1` | idea | none | | |
| 094 | [Split screen (`O2` phase 2)](items/094-split-screen/ITEM.md) | feature | `O2` | idea | `047`, `010` | | |
| 095 | [Netplay with lockstep (`O2` phase 3)](items/095-netplay/ITEM.md) | feature | `O2` | idea | `094`, `010`, `011`, `055` | | |
| 096 | [Session-owned entity factories and lists, in place of statics](items/096-session-owned-entity-services/ITEM.md) | refactor | `O1` | idea | none | | |
| 097 | [Stable order for event keys in `EventsManager`](items/097-stable-event-key-order/ITEM.md) | refactor | `O2`, `O1` | idea | none | | |
| 098 | [Typed fixture and body state in place of property bags](items/098-typed-fixture-state/ITEM.md) | refactor | `O1` | idea | none | | |
| 099 | [Use camera zones and scroll sections in real stages](items/099-camera-sections-in-stages/ITEM.md) | feature | `O4` | idea | `051` | | |
| 100 | [CI result before a merge](items/100-ci-before-merge/ITEM.md) | tooling | `O6` | idea | `002` | | |
| 101 | [Typed Tiled fields and checks for camera sections and rooms](items/101-map-object-schema/ITEM.md) | tooling | `O7`, `O6` | idea | `022` | | |
| 102 | [A game mode concept](items/102-game-modes/ITEM.md) | feature | `O8`, `O2` | idea | none | | |
| 103 | [Remove the dead `ScreenController` path or give it a real use](items/103-dead-screen-controller/ITEM.md) | refactor | `O1` | idea | none | | |
| 104 | [Remove the dead `LevelSelectScreen`](items/104-dead-level-select-screen/ITEM.md) | refactor | `O1` | idea | none | | |
| 105 | [Fail a test task that runs 0 tests](items/105-empty-test-run-check/ITEM.md) | test | `O6` | idea | `004` | | |
| 106 | [Make a new line in the known-failures list visible](items/106-known-failures-growth-check/ITEM.md) | test | `O6` | idea | `004` | | |
| 107 | [Tighter floor on the entity class count in the smoke test](items/107-tighter-entity-class-floor/ITEM.md) | test | `O6` | idea | `004` | | |
| 108 | [Spec Ops co-op missions](items/108-spec-ops-mode/ITEM.md) | feature | `O2`, `O8` | idea | `047`, `102` | | |
| 109 | [PvP modes](items/109-pvp-mode/ITEM.md) | feature | `O2`, `O8` | idea | `047`, `102`, `030` | | |
| 110 | [Co-op in the main campaign](items/110-co-op-campaign/ITEM.md) | feature | `O2` | idea | `108`, `109` | | |
| 111 | [PvP across multiple rooms](items/111-pvp-multi-room/ITEM.md) | feature | `O2` | idea | `109`, `094` | | |
| 112 | [Targeting as a policy that each entity owns](items/112-targeting-policies/ITEM.md) | refactor | `O1`, `O2` | planned | `047b` | | |
| 113 | [Character select screen](items/113-character-select/ITEM.md) | feature | `O8`, `O9` | idea | `081` | | |
| 114 | [Gravity from components](items/114-gravity-components/ITEM.md) | refactor | `O1` | idea | none | | |
| 115 | [Partial sink and stuck behavior in sand](items/115-partial-sink-stuck-in-sand/ITEM.md) | feature | `O4`, `O1` | idea | none | | |
| 116 | [Body senses as typed state, then a sense history](items/116-body-senses/ITEM.md) | refactor | `O1`, `O5` | idea | none | | |
| 117 | [Crush with a falling pushable block](items/117-pushable-block-crush/ITEM.md) | feature | `O4` | idea | none | | |
| 118 | [Boss fights as routines, rules and leases](items/118-boss-fight-routines/ITEM.md) | refactor | `O1`, `O4`, `O2` | done | none | 2026-11-02 | 2026-11-06 |
| 119 | [`PLAYER_DESTROYED` and `LEVEL_FAILED`](items/119-player-left-world-event/ITEM.md) | refactor | `O2`, `O1` | planned | none | | |
| 120 | [A player that turns hostile and comes back](items/120-hostile-player/ITEM.md) | feature | `O8`, `O2` | idea | `030`, `047c`, `047e`, `112`, `118`, `119` | | |
| 121 | [Enemy clones of a playable character](items/121-playable-character-clones/ITEM.md) | feature | `O4`, `O8` | idea | `081` | | |
| 122 | [Human-driven boss](items/122-human-driven-boss/ITEM.md) | feature | `O8`, `O2` | idea | `102` | | |
| 123 | [X6 flat step-down off a slope](items/123-slope-flat-step-down/ITEM.md) | feature | `O4`, `O8` | idea | `017`, `068` | | |
| 124 | [Slope pass-through from below](items/124-slope-pass-through/ITEM.md) | feature | `O4` | idea | `068` | | |
| 125 | [Spatial or room index for spawners, if a measure shows a cost](items/125-spawner-index/ITEM.md) | refactor | `O5` | idea | none | | |
| 126 | [Movement and damage blocks as lock reasons](items/126-movement-damage-lock-reasons/ITEM.md) | refactor | `O1`, `O8` | idea | `048` | | |
| 127 | [View edge warp: test the warp spot for solids](items/127-warp-spot-solids/ITEM.md) | feature | `O2` | done | `047` | 2026-11-07 | 2026-11-09 |
| 128 | [Cull near players, not only by the camera view](items/128-cull-near-players/ITEM.md) | refactor | `O2`, `O5` | idea | none | | |
| 129 | [View edge warp visuals, phase 2](items/129-warp-visuals/ITEM.md) | feature | `O9`, `O2` | idea | `047` | | |
| 130 | [`LevelCamera` and `SharedView`: the camera stack out of `MegaLevelScreen`](items/130-level-camera/ITEM.md) | refactor | `O1`, `O2` | planned | `047` (`047h`) | | |
| 131 | [Room exit: a player past the exit wall outside the shared span](items/131-exit-outside-span/ITEM.md) | feature | `O2` | idea | `047` | | |
| 132 | [Spread the players after a room transition](items/132-spread-after-transition/ITEM.md) | feature | `O2`, `O9` | idea | `047` | | |
| 133 | [A cue that shows who waits at a room exit](items/133-exit-wait-cue/ITEM.md) | feature | `O9`, `O2` | idea | `047` | | |
| 134 | [Draw bodies and the camera between physics steps (render interpolation)](items/134-render-interpolation/ITEM.md) | refactor | `O9`, `O5` | done | none | 2026-11-07 | 2026-11-09 |
| 135 | [A player held below the view is left behind at a side room exit](items/135-held-player-left-behind/ITEM.md) | feature | `O2` | idea | `047` | | |
| 136 | [Co-op: a player can skip a spike floor in a tall room by falling below the view](items/136-tall-room-spike-skip/ITEM.md) | feature | `O2`, `O4` | idea | `047` | | |
| 137 | [One falling-piece entity in place of `DebrisPiece`, `BlockPiece` and `WoodCratePiece`](items/137-falling-piece-entity/ITEM.md) | refactor | `O1` | done | none | 2026-11-13 | 2026-11-13 |
| 138 | [Merge `Wreck` into `MockSpriteEntity`](items/138-wreck-into-mock-sprite/ITEM.md) | refactor | `O1` | done | none | 2026-11-13 | 2026-11-13 |
| 139 | [A shared simple enemy shot, starting with `JunkShot`](items/139-shared-enemy-shot/ITEM.md) | refactor | `O1` | active | none | 2026-11-13 | |
| 140 | [`TickFrames` as the engine `Animation`, with durations in seconds](items/140-tick-frames-seconds/ITEM.md) | refactor | `O1` | idea | none | | |
| 141 | [The sabre damage by slash kind and by target](items/141-sabre-damage-rules/ITEM.md) | feature | `O8` | idea | `081` | | |
| 142 | [Remove the reflection from `FriendlyFireTest` and `EntityExistenceTest`](items/142-tests-no-reflection/ITEM.md) | test | `O6` | idea | none | | |
| 143 | [View edge warp: the cap does not run while no partner is in view](items/143-warp-cap-out-of-view/ITEM.md) | feature | `O2` | idea | `047` | | |
| 144 | [View edge warp beside a partner on a ladder](items/144-warp-ladder-partner/ITEM.md) | feature | `O2` | idea | `047` | | |
| 145 | [View edge warp: the landing after the cap tests only solids](items/145-warp-fallback-hazards/ITEM.md) | feature | `O2` | idea | `047` | | |
| 146 | [A marker `IPlayer` interface, so a player has a type](items/146-player-marker-interface/ITEM.md) | refactor | `O1`, `O8` | done | none | 2026-11-10 | 2026-11-10 |
| 147 | [Render interpolation for riders and for bodies that move once per frame](items/147-interpolation-riders-frame-moved/ITEM.md) | refactor | `O9`, `O5` | idea | none | | |
| 148 | [The last player reads take `IPlayer`](items/148-last-player-reads/ITEM.md) | refactor | `O1`, `O8` | done | none | 2026-11-10 | 2026-11-11 |
| 149 | [Tag changes at runtime, with an index that follows them](items/149-runtime-tag-changes/ITEM.md) | refactor | `O1`, `O7` | idea | none | | |
| 150 | [Shared movement helpers for bosses and enemies](items/150-boss-movement-helpers/ITEM.md) | refactor | `O1`, `O4` | idea | none | | |
| 151 | [Rename the level progress vocabulary from "defeated" to "completed"](items/151-level-completed-rename/ITEM.md) | refactor | `O1` | done | none | 2026-11-11 | 2026-11-11 |
| 152 | [Record the last Wily stage as completed at the ending](items/152-record-final-stage-completed/ITEM.md) | feature | `O9` | idea | none | | |
| 153 | [A current-weapon icon on every player's HUD, and a HUD that fades when a player is behind it](items/153-hud-weapon-icon-and-fade/ITEM.md) | feature | `O9`, `O8` | idea | none | | |
| 154 | [Spawners test the camera after it moves](items/154-spawn-after-camera-move/ITEM.md) | refactor | `O9`, `O1` | dropped | none | | 2026-11-13 |
| 155 | [LibGDX arrays for the falling-piece region keys](items/155-falling-piece-gdx-arrays/ITEM.md) | refactor | `O1` | idea | `060`, `139` | | |
| 156 | [`WhiteBurst` has an `EXPLOSION` fixture that projectiles react to](items/156-white-burst-explosion-fixture/ITEM.md) | refactor | `O1` | idea | none | | |
| 157 | [D-1000 overdrive on the shared boss health phases](items/157-d1000-health-phases/ITEM.md) | refactor | `O1`, `O4` | idea | none | | |
| 158 | [A shared lead-aim helper for bosses](items/158-boss-lead-aim-helper/ITEM.md) | refactor | `O1`, `O4` | idea | none | | |
| 159 | [New hand and head art for the intro boss](items/159-intro-boss-hand-art/ITEM.md) | feature | `O4`, `O9` | idea | none | | |

## Branches

| Branch | Item | Goal | Status | Started | Finished |
|--------|------|------|--------|---------|----------|
| [`mega-refactor-146a-player-marker`](items/146-player-marker-interface/branches/mega-refactor-146a-player-marker/process-log.md) | 146 | A marker `IPlayer`, tags in a `TagsComponent`, and `init()` before `canSpawn` | merged | 2026-11-10 | 2026-11-10 |
| [`mega-refactor-060a-shared-run-body`](items/060-proto-man/branches/mega-refactor-060a-shared-run-body/process-log.md) | 060 | Refactor only: move Megaman's run, body and momentum into `players/` functions with parameters; Megaman plays the same | merged | 2026-11-10 | 2026-11-10 |
| [`mega-refactor-060b-protoman-move`](items/060-proto-man/branches/mega-refactor-060b-protoman-move/process-log.md) | 060 | `Protoman` in slot 0 on a test level: idle, step, walk, jump and fall, with an atlas sliced from the sheet | merged | 2026-11-10 | 2026-11-11 |
| [`mega-refactor-060c-protoman-buster`](items/060-proto-man/branches/mega-refactor-060c-protoman-buster/process-log.md) | 060 | Proto Man's sabre and buster, X6 Zero style (`060f` folded in): sabre and buster weapon slots on `B` and `SELECT`, a crouch guard with the shield on `DOWN`, and crouch attacks; shared `players/` sabre, buster and charge | merged | 2026-11-11 | 2026-11-14 |
| [`mega-refactor-060d-protoman-slide-ladder`](items/060-proto-man/branches/mega-refactor-060d-protoman-slide-ladder/process-log.md) | 060 | Proto Man's slide and ladder, with Megaman's shared rules | active | 2026-11-14 | |
| [`mega-refactor-060j-mega-player`](items/060-proto-man/branches/mega-refactor-060j-mega-player/process-log.md) | 060 | Refactor only: an abstract `MegaPlayer` base class that owns the shared player wiring and is the receiver of the shared `players/` functions; `Megaman` and X move onto it and play the same | merged | 2026-11-10 | 2026-11-11 |
| [`mega-refactor-024a-weapon-defs`](items/024-weapon-defs/branches/mega-refactor-024a-weapon-defs/process-log.md) | 024 | Refactor only: Megaman's 10 weapons become `WeaponDef` objects in a registry; Megaman plays the same | active | 2026-11-14 | |
| [`mega-refactor-118a-fight-routine`](items/118-boss-fight-routines/branches/mega-refactor-118a-fight-routine/process-log.md) | 118 | Core fight types, with `BigAssMaverickRobot` as the only boss on the new flow | merged | 2026-11-02 | 2026-11-02 |
| [`mega-refactor-118b-batch-1`](items/118-boss-fight-routines/branches/mega-refactor-118b-batch-1/process-log.md) | 118 | `TimberWoman`, `MoonMan` and `ReactorMan` (6 fights) on the fight flow | merged | 2026-11-03 | 2026-11-03 |
| [`mega-refactor-118c-batch-2`](items/118-boss-fight-routines/branches/mega-refactor-118c-batch-2/process-log.md) | 118 | `GlacierMan`, `DesertMan`, `InfernoMan`, `RodentMan` and `PreciousWoman` (10 fights) on the fight flow | merged | 2026-11-03 | 2026-11-04 |
| [`mega-refactor-118d-batch-3`](items/118-boss-fight-routines/branches/mega-refactor-118d-batch-3/process-log.md) | 118 | `ElecDevil`, `GutsTank` and `SigmaRat` (2 live fights) on the fight flow | merged | 2026-11-04 | 2026-11-05 |
| [`mega-refactor-118e-batch-4`](items/118-boss-fight-routines/branches/mega-refactor-118e-batch-4/process-log.md) | 118 | The mini bosses `MoonHead`, `MechaDragon` and `ReactorMonkey` (5 live fights) on the fight flow with `miniBossPlan` | merged | 2026-11-05 | 2026-11-05 |
| [`mega-refactor-118f-final-boss`](items/118-boss-fight-routines/branches/mega-refactor-118f-final-boss/process-log.md) | 118 | `WilyFinalBoss` (1 live fight) on the fight flow with `finalBossPlan`, and no level check for `wily_room` | merged | 2026-11-05 | 2026-11-05 |
| [`mega-refactor-118g-remove-old-flow`](items/118-boss-fight-routines/branches/mega-refactor-118g-remove-old-flow/process-log.md) | 118 | Remove the old boss flow: the screen branches, the route, the guards, `BOSS_READY`, `BOSS_DEAD`, `BEGIN_BOSS_SPAWN` and the parts with no caller left | merged | 2026-11-05 | 2026-11-05 |
| [`mega-refactor-118h-bar-fill`](items/118-boss-fight-routines/branches/mega-refactor-118h-bar-fill/process-log.md) | 118 | The fight runs the boss bar fill, the screen only draws the bar, and `HealthFillType` goes | merged | 2026-11-05 | 2026-11-05 |
| [`mega-refactor-118i-boss-phase`](items/118-boss-fight-routines/branches/mega-refactor-118i-boss-phase/process-log.md) | 118 | A `BossComponent` holds the boss phase, and a fight step starts the boss AI; `ready` and `betweenReadyAndEndBossSpawnEvent` go | merged | 2026-11-05 | 2026-11-05 |
| [`mega-refactor-118j-gate-unlock`](items/118-boss-fight-routines/branches/mega-refactor-118j-gate-unlock/process-log.md) | 118 | 1 keyed `UNLOCK_GATE` event from a boss fight and a `GauntletMaster`; `INTERMEDIATE_BOSS_DEAD`, `GAUNTLET_CLEARED` and `AbstractBoss.bossKey` go | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-081a-player-lifecycle`](items/081-playable-x/branches/mega-refactor-081a-player-lifecycle/process-log.md) | 081 | Move the shared player lifecycle out of `Megaman` into functions that a second player class can call; Megaman plays the same | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-081b-x-run-jump-shoot`](items/081-playable-x/branches/mega-refactor-081b-x-run-jump-shoot/process-log.md) | 081 | Phase 1 of X: `MegamanX` in slot 0 on `X6_INTRO`, with run, jump and the buster | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-081c-x-wall-slide-air-dash`](items/081-playable-x/branches/mega-refactor-081c-x-wall-slide-air-dash/process-log.md) | 081 | Phase 2 of X: wall slide with the wall kick, and the air dash | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-081d-x-dash-sabre`](items/081-playable-x/branches/mega-refactor-081d-x-dash-sabre/process-log.md) | 081 | Phase 3 of X (ground dash, dash jump, dash wall kick), less knockback, the owner's X tuning and the X6 feel fixes; the sabre moved to `081e` | merged | 2026-11-06 | 2026-11-07 |
| [`mega-refactor-081e-x-sabre`](items/081-playable-x/branches/mega-refactor-081e-x-sabre/process-log.md) | 081 | Phase 4 of X: the Z-sabre slash on `UP` + `B` | merged | 2026-11-07 | 2026-11-09 |
| [`mega-refactor-081f-x6-intro-fixes`](items/081-playable-x/branches/mega-refactor-081f-x6-intro-fixes/process-log.md) | 081 | Fix the X6 intro stage and `D1000` problems that the `081e` play test found | merged | 2026-11-09 | 2026-11-12; TODO: the D-1000 fight is AI-generated and needs more scrutiny (branch NOTES, "AI-generated fight") |
| [`mega-refactor-081g-x-crouch-ladder`](items/081-playable-x/branches/mega-refactor-081g-x-crouch-ladder/process-log.md) | 081 | X's crouch, ladder and climb (open question 10); Megaman's climb shared in `players/PlayerClimb.kt` | merged | 2026-11-12 | 2026-11-13 |
| [`mega-refactor-127a-warp-spot-solids`](items/127-warp-spot-solids/branches/mega-refactor-127a-warp-spot-solids/process-log.md) | 127 | Test the view edge warp spot for solids, hazards and enemies, and wait for a safe spot | merged | 2026-11-07 | 2026-11-09 |
| [`mega-refactor-038a-respawn-routine`](items/038-respawn-routine/branches/mega-refactor-038a-respawn-routine/process-log.md) | 038 | One `RespawnRoutine` runs the death and the respawn in a fixed order, in place of the 5-event chain; the public events stay | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-038b-level-reset`](items/038-respawn-routine/branches/mega-refactor-038b-level-reset/process-log.md) | 038 | Split `PLAYER_SPAWN` into a spawn event for each slot and a `LEVEL_RESET` that the respawn routine sends; move the level-reset listeners | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-001a-package-rename`](items/001-maverick-engine/branches/mega-refactor-001a-package-rename/process-log.md) | 001 | Rename the engine root package to a Maverick Engine identity | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-001b-host-contract`](items/001-maverick-engine/branches/mega-refactor-001b-host-contract/process-log.md) | 001 | Add `IMaverickGame` and `MaverickGame` | blocked | | |
| [`mega-refactor-019a-remove-second-spawn-pass`](items/019-spawn-phase/branches/mega-refactor-019a-remove-second-spawn-pass/process-log.md) | 019 | Run one spawn pass per frame instead of two | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-002a-ci-all-branches`](items/002-ci-all-branches/branches/mega-refactor-002a-ci-all-branches/process-log.md) | 002 | CI on every branch push, one Gradle job, aligned test libraries, JDK toolchain | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-008a-dev-launch-options`](items/008-dev-launch-options/branches/mega-refactor-008a-dev-launch-options/process-log.md) | 008 | Dev launch options and level staging | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-019b-destroy-notification`](items/019-spawn-phase/branches/mega-refactor-019b-destroy-notification/process-log.md) | 019 | Tell a spawner at once when its entity is destroyed; one spawn phase per tick | merged | 2026-10-27 | 2026-10-27 |
| [`mega-refactor-019c-merge-spawn-queues`](items/019-spawn-phase/branches/mega-refactor-019c-merge-spawn-queues/process-log.md) | 019 | Map spawns go straight to the engine; one spawn queue; `PlayerCheckpointManager` rename | merged | 2026-11-03 | 2026-11-03 |
| [`mega-refactor-029a-player-seam`](items/029-player-contract/branches/mega-refactor-029a-player-seam/process-log.md) | 029 | Player slot, player view and players service; `Megaman` in slot 0 | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029b-enemy-players`](items/029-player-contract/branches/mega-refactor-029b-enemy-players/process-log.md) | 029 | Enemies use the players service in place of `megaman` | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029c-boss-players`](items/029-player-contract/branches/mega-refactor-029c-boss-players/process-log.md) | 029 | Bosses and the boss damage listener use the players service | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029d-world-players`](items/029-player-contract/branches/mega-refactor-029d-world-players/process-log.md) | 029 | World entities use `IPlayer` and the players service | merged | 2026-10-23 | 2026-10-23 |
| [`mega-refactor-029e-player-profile`](items/029-player-contract/branches/mega-refactor-029e-player-profile/process-log.md) | 029 | Lives, tanks, enhancements and weapons owned in one session profile | merged | 2026-10-23 | 2026-10-24 |
| [`mega-refactor-029f-level-players`](items/029-player-contract/branches/mega-refactor-029f-level-players/process-log.md) | 029 | Level screen, handlers and UI use the players service; remove `game.megaman` | merged | 2026-10-24 | 2026-10-24 |
| `mega-refactor-029g-behavior-split` | 029 | Split the player behaviors into factory functions | merged | 2026-10-25 | 2026-10-25 |
| [`mega-refactor-031a-input-frame`](items/031-input-frames/branches/mega-refactor-031a-input-frame/process-log.md) | 031 | One `InputFrame` per player slot; gameplay reads move to it | merged | 2026-10-25 | 2026-10-25 |
| [`mega-refactor-031b-engine-input`](items/031-input-frames/branches/mega-refactor-031b-engine-input/process-log.md) | 031 | Input frames as an engine mechanism; `ControllerSystem` dispatches from each entity's own input source | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-031c-input-pipeline`](items/031-input-frames/branches/mega-refactor-031c-input-pipeline/process-log.md) | 031 | A new input pipeline with the same behavior: device adapters, held masks, an engine edge builder with the lock; the poller goes | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-031d-devices`](items/031-input-frames/branches/mega-refactor-031d-devices/process-log.md) | 031 | Devices: the keyboard aliases, every connected gamepad, the stick threshold, the triggers | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-031e-bindings`](items/031-input-frames/branches/mega-refactor-031e-bindings/process-log.md) | 031 | Per-slot bindings: one settings object per slot replaces `ControllerButtons`; the bindings screen rules; `--resetBindings` | merged | 2026-10-30 | 2026-11-01 |
| [`mega-refactor-031f-menu-input`](items/031-input-frames/branches/mega-refactor-031f-menu-input/process-log.md) | 031 | Menus and skip screens read frames, with `Esc` and the back action; the `controllerPoller` facade goes | merged | 2026-11-01 | 2026-11-01 |
| `mega-refactor-051-master` | 051 | Parent branch of `051c`, `051d` and `051e`: collects them so that the owner can play them together before `master` | merged | 2026-10-31 | 2026-11-01 |
| [`mega-refactor-051a-camera-read-interface`](items/051-camera-behaviors/branches/mega-refactor-051a-camera-read-interface/process-log.md) | 051 | Read-only camera interface in `engine`; `getGameCamera()` becomes a property | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-051b-camera-behaviors`](items/051-camera-behaviors/branches/mega-refactor-051b-camera-behaviors/process-log.md) | 051 | The camera built from behavior components; the level screen talks to a camera control interface; `GameCameraState` goes | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-051c-camera-zones`](items/051-camera-behaviors/branches/mega-refactor-051c-camera-zones/process-log.md) | 051 | Tiled camera zones, players-only edge blocks and `CrusherComponent`; levels with no zones play the same | merged | 2026-10-30 | 2026-10-31 |
| [`mega-refactor-051d-auto-scroll`](items/051-camera-behaviors/branches/mega-refactor-051d-auto-scroll/process-log.md) | 051 | Tiled scroll sections that take control of the camera, with edge blocks, stop modes and chains | merged | 2026-10-31 | 2026-10-31 |
| [`mega-refactor-051-fix-scroll-room-scope`](items/051-camera-behaviors/branches/mega-refactor-051-fix-scroll-room-scope/process-log.md) | 051 | Fix: scroll kept edges and the done test apply only in the rooms of the section, and edges do not act on the limits of the last room | merged | 2026-10-31 | 2026-11-01 |
| [`mega-refactor-051e-camera-rotation`](items/051-camera-behaviors/branches/mega-refactor-051e-camera-rotation/process-log.md) | 051 | Remove `RotatableCamera` and `GameCameraState`: plain cameras, a `CameraRotation` part, and `rotating` on `ICameraView` | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-061a-thin-player`](items/061-thin-player/branches/mega-refactor-061a-thin-player/process-log.md) | 061 | Thin `MegaPlayer`, free functions over the entity, engine locks, an immediate damage signal | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-061b-play-phase-teleport`](items/061-thin-player/branches/mega-refactor-061b-play-phase-teleport/process-log.md) | 061 | `ready` and `teleporting` as entity state (play phase, teleportable); no player wrappers | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-061c-entity-queries`](items/061-thin-player/branches/mega-refactor-061c-entity-queries/process-log.md) | 061 | A player is an entity with a `PlayerComponent`; callers ask the entity; `MegaPlayer` and `MegaPlayerAccess.kt` go | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-062a-entity-state`](items/062-entity-existence-state/branches/mega-refactor-062a-entity-state/process-log.md) | 062 | One engine-owned `EntityExistenceState` on each entity, written only by the engine; `spawned` and `dead` derived from it | merged | 2026-10-26 | 2026-10-26 |
| [`mega-refactor-062b-spawn-order-guards`](items/062-entity-existence-state/branches/mega-refactor-062b-spawn-order-guards/process-log.md) | 062 | Engine guards on the order of spawn and destroy calls; the last call wins | merged | 2026-10-27 | 2026-10-27 |
| [`mega-refactor-062c-existence-readers`](items/062-entity-existence-state/branches/mega-refactor-062c-existence-readers/process-log.md) | 062 | Readers of `dead` and `spawned` ask named questions of the existence state | merged | 2026-10-27 | 2026-10-27 |
| [`mega-refactor-062d-queued-spawn-query`](items/062-entity-existence-state/branches/mega-refactor-062d-queued-spawn-query/process-log.md) | 062 | Replace `engine.contains(..., containedIfQueuedToSpawn)` and the pool return of a rejected spawn with the existence state | merged | 2026-10-27 | 2026-10-27 |
| [`mega-refactor-063a-contracts-are-entities`](items/063-entity-contracts-as-entity-types/branches/mega-refactor-063a-contracts-are-entities/process-log.md) | 063 | Entity contracts extend `IGameEntity`/`IMegaGameEntity`; casts that existed only for entity members go | merged | 2026-10-27 | 2026-10-27 |
| [`mega-refactor-063b-damage-entity-rename`](items/063-entity-contracts-as-entity-types/branches/mega-refactor-063b-damage-entity-rename/process-log.md) | 063 | Rename `IDamageable`/`IDamager` to `IDamageableEntity`/`IDamagerEntity` (and decide their module); `IChildEntity`/`IParentEntity` extend `IGameEntity` | merged | 2026-10-27 | 2026-10-28 |
| [`mega-refactor-064a-instance-identity`](items/064-entity-instance-identity/branches/mega-refactor-064a-instance-identity/process-log.md) | 064 | Groups as an engine `GroupsComponent` with an engine group query; `group` leaves `MegaGameEntity` and `MegaGameEntities` | merged | 2026-10-27 | 2026-10-28 |
| [`mega-refactor-064b-map-object-id`](items/064-entity-instance-identity/branches/mega-refactor-064b-map-object-id/process-log.md) | 064 | `id` becomes a `core` component present only on entities spawned from a map; `RANDOM_ID_POOL` goes; runtime readers use entity references | merged | 2026-10-28 | 2026-10-28 |
| [`mega-refactor-068a-slopes`](items/068-slopes/branches/mega-refactor-068a-slopes/process-log.md) | 068 | X6-style floor slopes for every body with a `FEET` fixture; main levels unchanged | merged | 2026-11-03 | 2026-11-03 |
| `mega-refactor-016-master` | 016 | Parent branch of `016`: collects the `016` branches before they merge into `master` | merged | 2026-10-28 | 2026-10-30 |
| [`mega-refactor-016a-typed-level-state`](items/016-narrow-services/branches/mega-refactor-016a-typed-level-state/process-log.md) | 016 | Typed `LevelState` fields and camera flags replace the `game` property bag | merged | 2026-10-28 | 2026-10-28 |
| [`mega-refactor-016b-entity-host-reads`](items/016-narrow-services/branches/mega-refactor-016b-entity-host-reads/process-log.md) | 016 | The host-only reads in entities get a home (`DrWily` screen switch, `Starkner` `paused`, `selectButtonAction`, debug shapes, `weaponsAttained`) | merged | 2026-10-28 | 2026-10-29 |
| [`mega-refactor-016c-entity-services`](items/016-narrow-services/branches/mega-refactor-016c-entity-services/process-log.md) | 016 | Entities take `MegaServices` in place of `MegamanMaverickGame`; factory, test helper, `IMegaGameEntity` cleanup; the `016a` forwards become direct reads | merged | 2026-10-29 | 2026-10-29 |
| [`mega-refactor-016d-system-services`](items/016-narrow-services/branches/mega-refactor-016d-system-services/process-log.md) | 016 | Contact rules and the pathfinder factory take narrow services in place of `MegamanMaverickGame`; the creation order in `create()` allows it | merged | 2026-10-29 | 2026-10-29 |
| [`mega-refactor-016e-service-names`](items/016-narrow-services/branches/mega-refactor-016e-service-names/process-log.md) | 016 | Mechanical rename of `game` and the old host member names to names that fit `MegaServices` | merged | 2026-10-29 | 2026-10-29 |
| [`mega-refactor-014a-fixture-type`](items/014-contact-rules-table/branches/mega-refactor-014a-fixture-type/process-log.md) | 014 | Narrow the engine fixture type from `Any` | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014b-typed-fixture-receivers`](items/014-contact-rules-table/branches/mega-refactor-014b-typed-fixture-receivers/process-log.md) | 014 | One typed receiver registration in place of the hand-written extension trios | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014c-contact-rules-table`](items/014-contact-rules-table/branches/mega-refactor-014c-contact-rules-table/process-log.md) | 014 | Replace the contact `if/else` chain with an engine rule table and per-pair rule classes | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-014d-contact-receivers`](items/014-contact-rules-table/branches/mega-refactor-014d-contact-receivers/process-log.md) | 014 | Replace the `Megaman` checks in the contact rules with receivers and capabilities; `MegaCollisionHandler` body flags | merged | 2026-10-22 | 2026-10-22 |
| [`mega-refactor-084a-player-state`](items/084-split-megaman/branches/mega-refactor-084a-player-state/process-log.md) | 084 | Typed player state in place of the property bag (`facing`, `aButtonTask`, `currentWeapon`, `running`) and the teleport hook lambdas | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-084d-damage-reaction`](items/084-split-megaman/branches/mega-refactor-084d-damage-reaction/process-log.md) | 084 | Stun, knockback, recovery flash and freeze as player components, one per mechanism | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-084c-movement`](items/084-split-megaman/branches/mega-refactor-084c-movement/process-log.md) | 084 | Post-action momentum, room-transition carry and the `AButtonTask` transitions out of `Megaman` | merged | 2026-10-30 | 2026-10-30 |
| [`mega-refactor-084b-effects`](items/084-split-megaman/branches/mega-refactor-084b-effects/process-log.md) | 084 | Trail sprites, underwater bubbles, the death orbs and the out-of-bounds kill out of `Megaman` | merged | 2026-10-30 | 2026-10-31 |
| [`mega-refactor-084e-class-line`](items/084-split-megaman/branches/mega-refactor-084e-class-line/process-log.md) | 084 | Remove the interfaces on `Megaman` that have no reason left, and the dead player code | merged | 2026-10-31 | 2026-11-01 |
| [`mega-refactor-084f-focus-progress`](items/084-split-megaman/branches/mega-refactor-084f-focus-progress/process-log.md) | 084 | Move the camera focus and the game state listener off the `Megaman` class line (10 to 8 interfaces), and remove the dead `ConstKeys.FOCUS` reads | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-084g-movement-state`](items/084-split-megaman/branches/mega-refactor-084g-movement-state/process-log.md) | 084 | Movement state out of `Megaman`: `canMove`, the run state, the crouch timers, and typed values in place of 4 property bag lines | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-084h-charge`](items/084-split-megaman/branches/mega-refactor-084h-charge/process-log.md) | 084 | Move the charge timer and the charge reads off the `Megaman` class; the charge rules stay for `024` | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-004a-entity-smoke-test`](items/004-entity-smoke-test/branches/mega-refactor-004a-entity-smoke-test/process-log.md) | 004 | Headless `smokeTest` suite: build and `init()` every entity class with real atlas data | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-047a-second-slot`](items/047-co-op/branches/mega-refactor-047a-second-slot/process-log.md) | 047 | With `--players 2`, a second `Megaman` plays in a level on its own device | merged | 2026-11-01 | 2026-11-01 |
| [`mega-refactor-047b-player-rules`](items/047-co-op/branches/mega-refactor-047b-player-rules/process-log.md) | 047 | Session rules, friendly fire, targeting, `ChargedShot` and pickups | merged | 2026-11-01 | 2026-11-02 |
| [`mega-refactor-047g-shared-camera`](items/047-co-op/branches/mega-refactor-047g-shared-camera/process-log.md) | 047 | Shared screen phase 1: a group camera with edge walls in one room with no exits | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-047h-side-exits`](items/047-co-op/branches/mega-refactor-047h-side-exits/process-log.md) | 047 | Shared screen phase 2: side room exits with 2 players (gather at the exit) | merged | 2026-11-06 | 2026-11-06 |
| [`mega-refactor-047j-coop-bottom-warp`](items/047-co-op/branches/mega-refactor-047j-coop-bottom-warp/process-log.md) | 047 | The co-op default `bottom=WARP` for tall rooms | merged | 2026-11-06 | 2026-11-07 |
| [`mega-refactor-047c-slot-respawn`](items/047-co-op/branches/mega-refactor-047c-slot-respawn/process-log.md) | 047 | A dead co-op player respawns near the lead, at a safe spot (shared screen phase 6) | merged | 2026-11-09 | 2026-11-09 |
| [`mega-refactor-134a-draw-interpolation`](items/134-render-interpolation/branches/mega-refactor-134a-draw-interpolation/process-log.md) | 134 | Draw bodies and the camera between the last 2 physics steps, with `--interpolation` (on by default) | merged | 2026-11-07 | 2026-11-09 |
| [`mega-refactor-134b-interpolation-audit`](items/134-render-interpolation/branches/mega-refactor-134b-interpolation-audit/process-log.md) | 134 | Fix the known render interpolation hitches: bodies placed outside the world steps, bodiless sprites, the shape renderer, snaps | merged | 2026-11-09 | 2026-11-09 |
| [`mega-refactor-151a-level-completed-rename`](items/151-level-completed-rename/branches/mega-refactor-151a-level-completed-rename/process-log.md) | 151 | Rename `levelsDefeated`, `isLevelDefeated`, `onLevelDefeated` and the rest of the family to "completed"; delete `allLevelsDefeated` | merged | 2026-11-11 | 2026-11-11 |
| [`mega-refactor-137a-falling-piece-entity`](items/137-falling-piece-entity/branches/mega-refactor-137a-falling-piece-entity/process-log.md) | 137 | One falling-piece entity for `DebrisPiece`, `BlockPiece` and `WoodCratePiece` | merged | 2026-11-13 | 2026-11-13 |
| [`mega-refactor-138a-wreck-into-mock-sprite`](items/138-wreck-into-mock-sprite/branches/mega-refactor-138a-wreck-into-mock-sprite/process-log.md) | 138 | Merge `Wreck` into `MockSpriteEntity` | merged | 2026-11-13 | 2026-11-13 |
| [`mega-refactor-139a-shared-enemy-shot`](items/139-shared-enemy-shot/branches/mega-refactor-139a-shared-enemy-shot/process-log.md) | 139 | A shared simple enemy shot, starting with `JunkShot` | merged | 2026-11-13 | 2026-11-13 |
| [`mega-refactor-077a-intro-boss`](items/077-robot-master-attacks/branches/mega-refactor-077a-intro-boss/process-log.md) | 077 | A richer intro-stage boss fight and shared boss health phases | merged | 2026-11-13 | 2026-11-15 |
