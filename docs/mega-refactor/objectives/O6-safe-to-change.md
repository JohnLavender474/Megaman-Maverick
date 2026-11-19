# `O6`: Safe to change

- Status: active

## Problem

- `P1`: Most gameplay has no tests. The main proof that a change is safe is a manual play test by the owner, which
  limits how much can change at one time.
- `P2`: A run cannot be repeated today, so two runs cannot be compared (`010`, `055`).
- `P3`: Content errors, for example a wrong entity name in a map, appear only during play or are skipped (`003`).
- `P4`: A launch to check a room opens a window on the owner's desktop (`091`).

## Goal

A large change is safe because a check finds the break, not because a person happens to see it.

## Done means

- CI builds and tests each branch.
- Every level boots headless, and a map error fails before a play test.
- A recorded run can replay and be compared, so a contract check needs less manual play.

## Rules

### `O6-R1`: CI passes

- Rule: `engine:test`, `core:test`, `core:smokeTest` and `lwjgl3:jar` pass on each branch before a merge.
- Problem: `P1`
- Check: the CI result of the branch.
- Applies: `always-on`
- Active: `now`

### `O6-R2`: Named checks

- Rule: each behavior that a branch must keep or change has a named check in its contract (the
  "Contract" part of `SCOPE.md`; `CONTRACT.md` in branches planned before 2026-11-19; owner
  accepted the recommendation, 2026-11-19).
- Problem: `P1`
- Check: read the contract.
- Applies: `always-on`
- Active: `now`

### `O6-R3`: Test tools with no window

- Rule: a new test tool runs with no game window and ends with a clear pass or fail.
- Problem: `P4`
- Check: run the tool on a machine with no display.
- Applies: `linked items`
- Active: `now`

### `O6-R4`: Content errors fail early

- Rule: a map object that the game cannot use fails a check before play.
- Problem: `P3`
- Check: run the level validator.
- Applies: `linked items`
- Active: after `003`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O6-R1` | `partly` | `ci.yml:3-5,30` runs on a push to any branch, but only 7 CI runs exist, and 1 of 52 merged branches has a result. `HEAD` is 94 commits past `origin/master`. `master` has no required check. Merges use local checks. Update of 2026-11-01: `004a` has a CI result (run `41368452607`), and CI also runs `core:smokeTest`. `master` is 112 commits past `origin/master`. | `100` |
| `O6-R2` | `partly` | All 50 merged branches with a folder have a `CONTRACT.md`. 8 `fix/*` squashes have none; `docs/issues/README.md:12` asks for a short contract. The contracts were not read line by line. On 2026-11-01 the branch contract template gained `core:test`, and the `fix/` rule gained named checks. | none needed |
| `O6-R3` | `holds` | 0 test files use `Lwjgl3Application`. `HeadlessHarness.kt` uses `HeadlessApplication`, with no window: `core:smokeTest` passes with no `DISPLAY` and no `WAYLAND_DISPLAY`. `ci.yml` has no `xvfb` line. `P4` stays open: a room check still opens a window. | `091` |
| `O6-R4` | `broken` (waiting) | A bad name logs an error and skips the object (`SpawnersLayerBuilder.kt:77-82`). A name scan finds 14 objects in 7 live maps that do not resolve, which matches `docs/issues/2026-10-22-misplaced-tmx-objects.md`. | `003` |

## Redo risk

None found.

## Items

| Item | Status | Note |
|------|--------|------|
| `000` | done | [Bugs found during the review](../items/000-bugs/ITEM.md) (main) |
| `002` | done | [CI on every branch, one Gradle job, aligned test libraries](../items/002-ci-all-branches/ITEM.md) (main) |
| `003` | planned | [Level content validator (local tool in `utils/`, CI test)](../items/003-level-validator/ITEM.md) (main) |
| `004` | done | [Entity build-and-init smoke test](../items/004-entity-smoke-test/ITEM.md) (main) |
| `005` | planned | [Golden damage-table test](../items/005-golden-damage-test/ITEM.md) (main) |
| `006` | idea | [Headless boot-and-run per level (stage A)](../items/006-headless-boot-run/ITEM.md) (main) |
| `008` | active | [Dev launch options and level staging](../items/008-dev-launch-options/ITEM.md) (main) |
| `040` | idea | [Atlas checks and one-command repack (keep TexturePacker)](../items/040-atlas-checks/ITEM.md) (main) |
| `055` | planned | [Record and replay for contract checks](../items/055-record-and-replay/ITEM.md) (main) |
| `090` | planned | [Branch name in the game window title](../items/090-branch-window-title/ITEM.md) (main) |
| `091` | planned | [Check that a test room loads with no visible game window](../items/091-room-load-check/ITEM.md) (main) |
| `009` | planned | [Allocation and GC baseline per branch](../items/009-gc-baseline/ITEM.md) |
| `010` | idea | [One simulation clock: fixed tick for gameplay, separate present phase](../items/010-fixed-tick/ITEM.md) |
| `011` | done | [One seedable, interceptable random source with named streams](../items/011-game-random/ITEM.md) |
| `161` | done | [A type-aware check for direct random calls](../items/161-random-call-lint/ITEM.md) (main) |
| `164` | idea | [Move the text-scan guards to detekt](../items/164-guards-to-detekt/ITEM.md) (main) |
| `100` | idea | [CI result before a merge](../items/100-ci-before-merge/ITEM.md) (main) |
| `101` | idea | [Typed Tiled fields and checks for camera sections and rooms](../items/101-map-object-schema/ITEM.md) |
| `105` | idea | [Fail a test task that runs 0 tests](../items/105-empty-test-run-check/ITEM.md) (main) |
| `106` | idea | [Make a new line in the known-failures list visible](../items/106-known-failures-growth-check/ITEM.md) (main) |
| `107` | idea | [Tighter floor on the entity class count in the smoke test](../items/107-tighter-entity-class-floor/ITEM.md) (main) |
| `142` | idea | [Remove the reflection from `FriendlyFireTest` and `EntityExistenceTest`](../items/142-tests-no-reflection/ITEM.md) (main) |
| `168` | idea | [`allowRotation` off in the TexturePacker projects](../items/168-texture-packer-no-rotation/ITEM.md) (main) |
| `174` | idea | [Move the D-1000 frames out of `Bosses3` so that it packs with `Basic`](../items/174-bosses3-atlas-split/ITEM.md) (main) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-01: `O6-R1` names `core:smokeTest`, because CI runs it since `004a`. The owner approved the change.

## Open questions
