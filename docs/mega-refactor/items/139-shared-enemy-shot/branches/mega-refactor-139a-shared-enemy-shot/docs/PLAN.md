# Plan: mega-refactor-139a-shared-enemy-shot

How to do the work in `SCOPE.md`. Write this file after the owner approves the brief. It needs
no approval. When the plan changes, write the reason in the process log.

Owner answers (2026-11-13): `D1` A (`EnemyShot`), `D2` A (one tag entry reads `kind.damage`), `D3` A (the 4 junk
kinds, `RollingBotShot`, `FirePellet`).

Gradle: `JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew <tasks> --no-daemon -q`, only with 4 GiB or more
available (`free -g`). Kotest filter: `-Pkotest_filter_specs='<pattern>'`.

## Steps

### 1. Rename `JunkShot` to `EnemyShot`

- Changes: `git mv` the file; class `EnemyShot`, `TAG = "EnemyShot"`; `Type` becomes `Kind`, `type` becomes `kind`.
  The atlas regions keep `JunkShot/<key>` (the kind names its region). Rename the uses in `D1000Core`, `Junkroid`,
  `HoverGunner`, `D1000TrapGuard`, `PlayerDamageNegotiator` (one entry, `kind.damage`), the 11 test files and
  `entity-tags.txt`. Remove the `TODO` that points to item `139`. No behavior change.
- Check: `core:test` with the `D1000*` and `X6IntroEnemies*` specs passes; no `JunkShot` symbol is left in
  `core/src` (`grep`), only region names and the frame-file path in `D1000EyeGlowTest`.
- Done: yes

### 2. Make the kind general, with the 4 junk kinds only

- Changes: the kind gets the fields of `SCOPE.md` "New API and first caller": art (`Ticks` def or `Grid` animation,
  each with its atlas and region), body width and height in tiles, sprite turn, draw priority, `damageable` (the
  `DAMAGEABLE` fixture is active), `shootable`, `diesOnHit`, `diesOnShield`, death effect, death sound. `onSpawn`
  sets every per-kind value (body and fixture size, active fixtures, gravity, draw priority); the sprite
  pre-process sets size, flip and rotation each frame. The 4 junk kinds keep their values.
- Check: compiles; step 1 specs pass.
- Done: yes

### 3. `EnemyShotTest` for the junk kinds

- Changes: new `core/src/test/.../entities/projectiles/EnemyShotTest.kt` with a real `WorldSystem` and the game
  contact rules. A test player (`TestBodyPlayer` with a `DAMAGEABLE` fixture) takes the damage through
  `PlayerDamageNegotiator`. Cases: damage 2, 1, 1, 3; body size; a player `Bullet` destroys the core, Junkroid and
  Hover Gunner kinds and not the Junkroid hard kind; a junk kind dies on a hit and on a block, not on a shield.
- Check: the spec passes. Break each covered rule once (a damage value, `shootable`, `diesOnHit`, `diesOnShield`),
  see the spec fail, restore.
- Done: yes

### 4. Move `RollingBotShot` into a `ROLLING_BOT` kind

- Changes: kind `ROLLING_BOT` (`PROJECTILES_1` region `RollingBotShot`, grid 1x2 at 0.1 s, sprite 1x1, body
  0.85x0.45, flip when moving left, `PLAYGROUND` 1, damage 3, dies on hit and shield, `ChargedShotExplosion` turned
  to the path for 0.1 s, `ENEMY_DAMAGE_SOUND`). `RollingBot.shoot` passes `TYPE` and a `TRAJECTORY` of 8 tiles per
  second in its facing. Delete `RollingBotShot.kt`, its `ProjectilesFactory` key and pool, its `entity-tags.txt` line.
  The old `BODY` fixture is not moved (see `NOTES.md`, decision on the `BODY` fixture): a player `Bullet` explodes on
  the `BODY` of an `IDamageableEntity` that refuses it, and `EnemyShot` is one.
- Check: tests for the kind: damage 3, body size, dies on a hit, a shield and a block, death effect and sound, a
  player `Bullet` passes through it. Break the pass-through (add an active `BODY` fixture), see the test fail,
  restore.
- Done: yes

### 5. Move `FirePellet` into a `FIRE_PELLET` kind

- Changes: kind `FIRE_PELLET` (`PROJECTILES_1` region `FirePellet`, one frame, sprite 1x1, body 0.5, turns with its
  path, `FOREGROUND` 0, damage 3, does not die on a hit, dies on a shield, `Disintegration`, `ENEMY_DAMAGE_SOUND`).
  `DemonMet.fire` passes `TYPE` and `TRAJECTORY` (the old `IMPULSE`) and still no owner. Delete `FirePellet.kt`, its
  `ProjectilesFactory` key and pool, its `entity-tags.txt` line.
- Check: tests: damage 3, survives a hit on a player, dies on a shield and a block, has no owner, sprite turned to the
  path. Break `diesOnHit` for the kind, see the test fail, restore.
- Done: yes

### 6. Pooled reuse test

- Changes: `EnemyShotTest` spawns one instance as one kind, destroys it, spawns it as another kind, and checks the
  active fixtures, body and fixture size, gravity, draw priority and sprite rotation of the second kind.
- Check: passes; break one per-spawn reset (for example the draw priority), see it fail, restore.
- Done: yes

### 7. Full automated checks

- Changes: none.
- Check: `engine:test core:test core:smokeTest lwjgl3:jar` once; write the counts in the process log.
- Done: yes

### 8. Docs

- Changes: `docs/NOTES.md` (decisions of this branch), `process-log.md` (status and a dated entry). `CONTRACT.md`
  automated checks ticked.
- Check: the docs name each changed file and the decision on the `BODY` fixture.
- Done: yes
