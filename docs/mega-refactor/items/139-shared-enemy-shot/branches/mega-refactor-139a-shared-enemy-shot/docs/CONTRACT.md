# Contract: mega-refactor-139a-shared-enemy-shot

The behavior that this branch changes on purpose, the behavior that it must not change, and how to
check them. Write this file during
planning, with `SCOPE.md` and before `BRIEF.src.html`. The owner approves the brief, not this file.
Do the checks before the branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)

This draft assumes the recommended options of `SCOPE.md`: `D1` A (`EnemyShot`), `D2` A (damage in the kind), `D3` A
(`JunkShot` kinds + `RollingBotShot` + `FirePellet`). (Owner: `D3` A, 2026-11-13; the `SubmarineMissile` lines of option B were removed.)

## Intended changes

One cosmetic change in play (from the review before the play test); otherwise only code structure:

- A Rolling Bot shot no longer makes a faint toxic splash when it crosses a `ToxicWaterfall` (`REACTOR_MAN`
  `room4`): the waterfall's consumer reads a `BODY` fixture, and the Rolling Bot kind has none (see `NOTES.md`).

- `JunkShot` becomes `EnemyShot`. `RollingBotShot` and `FirePellet` become kinds of it.
- The damage of the moved shots to a player comes from the kind (3), not from `Size.MEDIUM` (3). Same value.

## Must not change

For each moved kind: the values in `SCOPE.md`, "The 3 closest shots, side by side".

| Shot | Damage to a player | Speed and path | Look | Hit rules | Death |
|---|---|---|---|---|---|
| D-1000 core shot | 2 | straight, aimed | `JunkShot/core` ticks | dies on block and hit; a player shot destroys it | `Disintegration`, no sound |
| Junkroid shot | 1 | lob, gravity -20 | `JunkShot/junkroid` | same; hard mode: a player shot does not destroy it | same |
| Hover Gunner shot | 3 | straight, 8 directions | `JunkShot/hover_gunner` | same as core | same |
| Rolling Bot shot | 3 | straight, 8 tiles/s, horizontal | 1x2 animation, flipped to the left, `PLAYGROUND` 1 | dies on block, hit and shield; player shots pass through it | rotated `ChargedShotExplosion` 0.1 s + `ENEMY_DAMAGE_SOUND` |
| Demon Met pellet | 3 | straight, aimed, 3-way spread | one region, turned to its path | dies on block and shield; does **not** die when it hits a player | `Disintegration` + `ENEMY_DAMAGE_SOUND` |

Also:

- Damage to an enemy from a moved shot stays 0 (no `KClass` table names the shared class).
- The D-1000 trap guard sees the same core shots as before.
- Culling: out of camera and on cull events, as `AbstractProjectile` does now.
- The same for each player character: Megaman, X and Proto Man (one `PlayerDamageNegotiator`).

## Automated checks

- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes. This includes the 11 renamed `D1000*` and `X6IntroEnemiesTest` files.
- [x] `./gradlew core:smokeTest` passes. `entity-tags.txt` lists `projectiles.EnemyShot` and no longer lists
  `JunkShot`, `RollingBotShot` and `FirePellet`. `init()` loads the art of every kind.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] New test `EnemyShotTest` (`core/src/test/.../entities/projectiles/`), through the public flow
  (`testServices()`, spawn, the damage path):
  - [x] each kind's damage through `PlayerDamageNegotiator` equals the table above (2, 1, 1, 3, 3, 3);
  - [x] each kind's body size after spawn equals the old size;
  - [x] a pooled shot spawned as one kind and then as another has only the second kind's fixtures active, body size
    and draw priority;
  - [x] the pellet kind does not die on a hit; the other kinds do;
  - [x] the Rolling Bot and pellet kinds die on a shield; the junk kinds do not;
  - [x] a player's shot destroys the core, Junkroid and Hover Gunner kinds, and not the Junkroid hard kind;
  - [x] a player's `Bullet` that touches a Rolling Bot kind does not explode (its `BODY` fixture on an
    `IDamageableEntity`; see `SCOPE.md` risks).
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Run from the worktree:

- `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-139a-shared-enemy-shot`

A test map is not needed: each moved shot's enemy is in a live room with a player spawn.

Experimental checks (moved shots) first:

- `./run-game-alpha.sh --level REACTOR_MAN --room room1`: `RollingBot` shots fly flat at the same speed, animate,
  face their direction, burst with the hit sound on a wall and on Megaman, and take 3 health. Megaman's buster shots
  pass through them, as before.
- `./run-game-alpha.sh --level INFERNO_MAN --room room4`: `DemonMet` fires 3 pellets in a spread, each turned to its
  path. A pellet goes through Megaman (3 damage) and does not vanish there. It vanishes with the hit sound on a wall.

Control checks (the renamed `JunkShot`):

- `./run-game-alpha.sh --level X6_INTRO --room room1`: `Junkroid` lobs junk in an arc (1 damage); `HoverGunner`
  shots fly straight (3 damage). X's shots destroy both. All vanish in a small disintegration on a wall.
- `./run-game-alpha.sh --level X6_INTRO --room room2 --heartTanks all --healthTanks all`: go right into the
  D-1000 fight. The core shots look the same, do 2 damage, and X's shots destroy them. The trap phase acts as before.

## Behaviors to check

- Hard mode `Junkroid` shots cannot be shot. No launch flag sets hard mode, so `EnemyShotTest` checks it.

