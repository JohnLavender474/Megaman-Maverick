# Contract: mega-refactor-014c-contact-rules-table

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-22

This branch changes how a contact finds its rule, not what the rule does. Two things must stay the
same: (1) for every contact, the same rule runs, in the same phase, with the fixtures in the same
role order; (2) each rule body does the same thing. The selection test proves (1) for every
possible pair. A verbatim move, checked line by line, gives (2). The play test then covers every
kind of contact once.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 436 tests on `master`, plus the new table and
      `FixtureTypeSet` tests.
- [ ] `./gradlew core:test` passes: the 77 tests on `master`, plus the selection test and the core
      table tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (115 on `master`).
- [ ] **The selection test passes against the new table, with the reference file unchanged since
      stage 0:** `git diff <stage-0 commit> -- <reference file>` is empty. The reference file was
      made from the old chain.
- [ ] Each rule class matches its old branch: `git diff --color-moved` and a reading of each class
      show only the mechanical changes listed in `SCOPE.md` (no role destructuring, no `contact`,
      no `printDebugLog`, no `out`/`set1`/`set2`, `tempVec2Set` local to the laser rules, `game`
      through the `FEET`-`SAND` constructor).
- [ ] The 6 type helpers are gone from `Contact`, and nothing calls them:
      `grep -rnE "fixturesMatch|fixtureSetsMatch|oneFixtureMatches|getFixturesIfOneMatches|getFixturesInOrder|getFixtureSetsInOrder" engine core`
      finds nothing.
- [ ] `MegaContactListener.kt` is deleted, and every rule in it is registered in
      `MegaContactRules.kt` (the selection test checks this).
- [ ] The dispatch path does not allocate: a review of `ContactRuleTable.dispatch` and
      `ContactRuleDispatcher` (no `Pair`, no lambda created per call, no boxing).

## Manual playthrough

Every contact now goes through the table, so the play test touches each kind of contact once. The
bodies did not change, so a short visit to each is enough.

| Level | What to check |
|-------|---------------|
| `TEST_1` | The widest set: land on blocks, walk on ice (slide) and quicksand (sink, splash), swim in water and polygon water (splash on entry and exit), climb a ladder, bounce on `SpringBouncer`, ride the cart, enter the gravity-change area, stand in the `TubeBeamerV2` laser and watch it stop at blocks, shoot enemies and blocks, take damage, kill enemies, collect an item. |
| `GLACIER_MAN` | Snow (slower), ice blocks, water. Slide into an ice wall (wall-slide friction). |
| `DESERT_MAN` | `CartV2`, quicksand, a boss gate. |
| `INFERNO_MAN` | The rising lava (death fixture): touching it kills. |
| Any level with spikes (for example `RODENT_MAN`) | Spikes kill. |
| One boss fight (for example the `DESERT_MAN` boss) | Boss damage, Megaman damage, the gate opening into the boss room. |

Use `./run-game-alpha.sh --level <LEVEL>` from the worktree; add `--room <name>` to jump to a room.

## Behaviors to check

- Megaman: the land sound plays once per landing; jump works after landing; air dash and swim
  start as before after leaving the ground or entering water; wall contact stops movement.
- Heart tanks and items are collected on contact.
- Buster shots hit blocks, enemies and shields as before; shots disappear in sand and water as
  before.
- The contact debug log (printed only when its tag is in `--logTags` and the debug filter accepts
  the contact) may change its text. It is a debug tool, not gameplay.

## Intended changes

None in gameplay. The contact debug log changes its text: it prints the rule ID instead of the old
messages. This fixes `docs/issues/2026-10-22-wrong-contact-log-texts.md`.
