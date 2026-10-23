# Contract: mega-refactor-014d-contact-receivers

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-22

This branch moves player-specific code out of the contact rules and the collision handler into
Megaman, and calls it at the same points through receivers, capabilities and body keys. Megaman
must behave the same, and other entities must not start to behave like the player.

## Automated checks

- [ ] `./gradlew engine:test` passes (454 on `master`).
- [ ] `./gradlew core:test` passes: the 79 tests on `master`, plus the new rule and collision tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The selection test still matches `contact-rule-selection.txt`, and the file is unchanged.
- [ ] No player reference is left in the rules except the two `029` sites, and none in the
      collision handler:
      `grep -rnE "\bMegaman\b|game\.megaman" core/src/main/kotlin/com/megaman/maverick/game/world/contacts/rules core/src/main/kotlin/com/megaman/maverick/game/world/collisions`
      shows only `PlayerItemRule.kt` and `ReceiverRules.kt` (`BodyPlayerRule`), each with a `TODO`
      comment that names `029`.
- [ ] `MegaCollisionHandler` does not take the game and does not read the controller.
- [ ] Only Megaman registers the new receivers and body keys and implements `IGateTrigger` and
      `ICartRider` (grep).
- [ ] A review shows each moved block unchanged in Megaman, and each receiver call at the same place
      in its rule as the old `is Megaman` block.

## Manual playthrough

| Level | What to check |
|-------|---------------|
| `TEST_1` | Land on blocks: one land sound per landing, and jump works at once. Walk off a ledge and air dash. Enter water: the fall stops on entry, Megaman swims and falls slower; leave the water: normal gravity and air dash again. Walk on ice into a wall: Megaman does not slide away from the wall. Walk on quicksand: the sand splash appears. Ride the cart. Climb the ladder up through its top and down again. |
| `GLACIER_MAN` | Wall-slide on an ice wall: the slide is slower than on a normal wall, as before. Walk on snow. |
| `INTRO_STAGE` | At the punk-city lamp post: while climbing, press UP to pass up through it. |
| `INFERNO_MAN` | The "collide up only" platforms: Megaman stands on them, and passes through them while climbing. |
| `DESERT_MAN` | The boss gate opens for Megaman. Ride `CartV2`. |

Use `./run-game-alpha.sh --level <LEVEL>` from the worktree; add `--room <name>` to jump to a room.

## Behaviors to check

- Enemies do not open gates and do not ride carts.
- Megaman's swim, air dash and jump tasks switch as before at water and ground changes.

## Intended changes

None.
