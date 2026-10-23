# Contract: mega-refactor-029c-boss-players

The gameplay that this branch must not change, and how to check it. This was written ahead of time
on `master`, before the branch existed.

- Status: approved
- Approved: 2026-10-23

This branch changes how bosses, `AbstractBoss` and `AbstractEnemy` reach the player, moves
`shooting`, `canBeDamaged` and the damage listeners into `PlayerStateComponent`, and renames the boss
damage listener. With one player every helper gives Megaman, so bosses must start, attack, react to
Megaman being hurt, and end as before, and every enemy must take damage, drop items and give the
one-up as before.

## Automated checks

- [ ] `./gradlew engine:test` passes: the 470 tests on `master`, plus the new tests.
- [ ] `./gradlew core:test` passes: the 107 tests on `master`, plus the new tests.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The compiler warnings do not grow (114 on `master`).
- [ ] The contact rule selection test passes, and `contact-rule-selection.txt` is unchanged.
- [ ] No boss or base class reads the global player, except the profile reads:
      `grep -rnE "(^|[^\w.])megaman\b|game\.megaman|getMegaman\(\)|\bis Megaman\b|as Megaman" core/src/main/kotlin/com/megaman/maverick/game/entities/bosses core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractBoss.kt core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractEnemy.kt`
      (leave out `package` and `import` lines) finds only comments, log text, method names, the
      commented-out old bosses, and the profile reads (`AbstractEnemy` drop odds and one-up,
      `hasEnhancement` in `AbstractEnemy` and `AbstractBoss`, `hasHeartTank` in `ReactorMonkey`), each
      with a `TODO` that names `029e`.
- [ ] `Megaman::class` only in the `PreciousWoman` damage-class sets.
- [ ] `IMegamanDamageListener` and `onMegamanDamaged` no longer exist; `IPlayerDamageListener` has
      `onPlayerDamaged(damager, player: MegaPlayer)` (grep).
- [ ] The four boss intro landing checks use `players.all`, each with a `TODO` that names `047`.
- [ ] `AbstractBoss` adds and removes itself as a damage listener at the same points as before
      (`onSpawn`, `onDestroy`), with a `TODO` for players who join during a fight (`047`).
- [ ] `ReactorMan_OLD`, `MechaDragon_OLD` and `SphinxMiniBoss` are commented out in full with a `TODO`
      at the top, and their `BossesFactory` entries are commented out; nothing else names them
      (grep).
- [ ] `Megaman.shooting` and `Megaman.canBeDamaged` read and write `PlayerStateComponent`; its
      `reset()` does nothing (review and tests).
- [ ] A review shows only the player reference changed in each moved expression, and the intent
      rule followed (target, `any`, `all`, `forEach`, `viewOf`).

## Manual playthrough

Run each check on the baseline (before the code changes) and again at the end, from the worktree.
Use `--room` to go straight to a boss room where you know its name.

| Level | What to check |
|-------|---------------|
| `REFACTOR_TEST_029`, `enemy_aim` | `Met` raises its shield when Megaman shoots toward it, and lowers it when he stops; shots damage enemies as before. |
| `REFACTOR_TEST_029`, `pickups` | Enemies drop items as before (health bulbs more often at low health). |
| `PRECIOUS_WOMAN` | Precious Woman laughs when she hurts Megaman while she stands. Her shield gems shatter as before. |
| `TIMBER_WOMAN` | Timber Woman's jump spin happens when Megaman is near and not invincible; the ground pound stun as before. |
| `MOON_MAN` | Moon Man's gravity change flips Megaman (not while he is dead); the fight starts and ends as before. |
| `REACTOR_MAN` | Reactor Man is hurt only by his reflected shots as before; Reactor Monkey (if in the stage) drops its heart tank only when Megaman does not have it. |
| `WILY_STAGE_1` | Elec Devil and the refight bosses start as before (Elec Devil waits for its intro timer). |
| `WILY_STAGE_2` | Guts Tank waits until Megaman lands before the fight; laughs when it or its fist hits Megaman; the fist takes damage only from Megaman's shots. |
| `WILY_STAGE_3` | The final Wily boss: its intro waits until Megaman lands; it laughs while Megaman is damaged; at its defeat Megaman cannot be damaged, faces Wily, and the controller turns off. |
| `INTRO_STAGE` | The big robot boss launches the hand that Megaman does not stand on. |

## Behaviors to check

- The game boots, and a level starts with no exception.
- An enemy killed with a one-up drop gives a life as before.

## Intended changes

None.
