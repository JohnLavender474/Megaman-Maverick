# Contract: mega-refactor-029b-enemy-players

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-23

This branch changes how every enemy reaches the player. With one player, `targetPlayer`,
`players.any` and `players.viewOf` all give Megaman, so every enemy must aim, face, chase, trigger,
react to contact and react to Megaman's shots as before, also while Megaman is dead or beaming in.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 472 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 102 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test passes, and `contact-rule-selection.txt` is unchanged.
- [ ] No enemy reads the global player:
      `grep -rnE "\bmegaman\b|game\.megaman|\bis Megaman\b|as Megaman" core/src/main/kotlin/com/megaman/maverick/game/entities/enemies core/src/main/kotlin/com/megaman/maverick/game/utils/misc/FacingUtils.kt`
      finds only comments, log text and names (for example `directlyChaseMegaman`); `Megaman::class`
      only in the two damage-class sets (`SwinginJoe`, `PreciousTron`).
- [ ] `MegamanValues` appears in `entities/enemies/` only at the 6 stomp-bounce sites, each with a
      `TODO` comment that names `029g`.
- [ ] `targetFor` has a `TODO` comment that says it always gives slot 0 and names the targeting design.
- [ ] No `TargetPolicy` or `TargetingComponent` exists (grep).
- [ ] `PlayerStateComponent.reset()` does nothing, and `Megaman.ready` reads and writes the component
      (review and test).
- [ ] A review of each batch shows only the player reference changed in each expression, and the
      intent rule followed: overlap with the player's body uses `players.any`; contact and owner
      identity uses `players.viewOf`; everything else uses `targetPlayer`.

## Manual playthrough

Run each check on the baseline (before the code changes) and again at the end. Use
`./run-game-alpha.sh --level <LEVEL> --room <ROOM>` from the worktree. The room names can change in
the plan.

| Level and room | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `enemy_aim` | `Met` and `SniperJoe` turn to face Megaman on both sides, and aim and fire at him. |
| `REFACTOR_TEST_029`, `enemy_drop` | `Darspider` drops when Megaman is under it; `SuicideBummer` reacts when he is in range, not before. |
| `REFACTOR_TEST_029`, `enemy_chase` | `Bat` wakes and chases Megaman, and returns after it hits him. |
| `REFACTOR_TEST_029`, `enemy_magfly` | `MagFly` pulls Megaman up, and does not pull him while he climbs the ladder or while he is damaged. |
| `REFACTOR_TEST_029`, `enemy_contact` | The `Matasaburo` and `Coldier` winds push Megaman. (`GapingFish` was replaced: it crashes on spawn on the baseline too; bug candidate.) |
| `REFACTOR_TEST_029`, `enemy_owner` | `Cokeyro` shakes when Megaman's shot hits it. |
| `REFACTOR_TEST_029`, `death` | While Megaman dies and respawns, the enemies in the rooms above do not crash and act as before. |
| `SMB3_1` | Stomp a `Goomba`, a `RedKoopa` and a `GreenKoopa`: Megaman bounces as before. Kick a shell; a shell hits Megaman from the side. Stomp a `BulletBill`. |
| `GLACIER_MAN` (sanity pass of a real stage) | Play the first rooms: `Matasaburo`, `Coldier`, `GapingFish`, `SniperJoe` act as before. |

## Behaviors to check

- The game boots, and a level starts with no exception.
- Enemies that spawn while Megaman beams in (not ready) act as before.

## Intended changes

None.
