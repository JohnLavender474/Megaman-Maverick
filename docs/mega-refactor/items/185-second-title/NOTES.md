# Notes: 185 A second game title on the same engine and shared code

## Research of 2026-11-23 (`master` `7c936e935`)

Read-only research by 6 agents (packages, X closure, reflection coupling, build and assets, roadmap,
web). The main thread checked `EntityType.kt:19`, the content imports of `PlayerDamageNegotiator.kt`
(31) and `MegamanX.kt:49-52`.

### Why not a copy

- The X closure reaches 381 of 904 `core` files (292 when the expansion stops at
  `PlayerDamageNegotiator` and `CharacterLoadout`). A copy takes most of `core` anyway.
- Then every engine, physics, camera and player fix must be made twice. Published studies of
  clone-and-own name this as the main cost (Krüger & Berger, ESEC/FSE 2020).
- Item `001` decision P: one repo, `engine`/`core` split. Decision L: the engine is not published.

### Why not a full split up front

- `O1-R5`: a new API needs a production caller. Practice (rule of three, "frameworks are
  extractions") says the same: let the second game show what is shared.
- 6 worktrees and 14 active items. A tree-wide move conflicts with all of them. Precedent: `001a`
  (about 760 files) ran alone.

### How `core` splits today (file counts)

| Class | Files | Share |
|---|---|---|
| Reusable as-is | 168 | 19% |
| Mixed (genre logic + Maverick imports) | 346 | 38% |
| Maverick content only | 388 | 43% |

Reusable: `utils`, `world`, `spawns`, `events`, `locks`, `input`, `audio`, `animations`,
`behaviors`, `difficulty`, `pathfinding`, `entities/state`, `entities/utils`.
Content: `enemies` (131), `bosses` (65), `projectiles` (75), `megaman` (32), `protoman` (8), menus,
backgrounds.

### Blockers, ordered by risk

| Blocker | Where | Count | Fix |
|---|---|---|---|
| Spawn names built from a package string | `EntityType.kt:19`, `SpawnersLayerBuilder.kt:58-64` | 5 `forName` sites | Name registry per game (`O7-R1`, item `021`) |
| Entity tests scan one package in one module | `EntityInitSmokeTest.kt:19-37`, `EntityConstructorsTest.kt:18-28` | 2 tests, 460-line tags file | Scan root and resource files per module; silent skip otherwise |
| Content in shared player code | `PlayerDamageNegotiator.kt:12-45` | 30 content imports | Damage table that each game supplies |
| Closed character list | `PlayerCharacter.kt:3`, `CharacterLoadout.kt:75-78` | 3 entries | `CharacterDef` (item `081` open question 6) |
| X edges into Megaman/content | `MegamanX.kt:49-52`, `MegamanXBuster.kt:6-8` | 5 content, 2 Megaman files | Move `MegaChargeStatus`, stun values; inject shot classes |
| Base classes test content | `AbstractEnemy.kt:53,55`, `AbstractBoss.kt:35-39`, `StandardDamageNegotiator.kt:11-16` | | Hooks or tables |
| `LevelDefinition` used outside levels | 22 files; 56 lines name a level | 39 entries | Per-game level registry |
| Progression types in `megaman/constants` | `MegaHeartTank`, `MegaEnhancement` | 22 outside users | Move to shared |
| Closed enums | `TextureAsset` 78, `SoundAsset` 87, `MusicAsset` 84, `ScreenEnum` 17, `EventType` 44, `FixtureType` 29 | | Engine side is open (`IAsset`, `Event(key: Any)`, `IFixtureType`); split enums per game |
| Concrete game type | 33 `screens/` files, 13 `drawables` | | Host contract `001b` |
| Save file names | `Constants.kt:5-8`, `GameState.kt` | 2 names, 4 `valueOf` | `GameState` per game; settings file name as parameter |

### Build and assets

- One launcher, one `assets/`, one `appName`: fixed in `build.gradle:86`, `lwjgl3/build.gradle:22,23,53,61,92-125`,
  `DesktopLauncher.kt:25,26,195`, `build-game.sh:88-91`, `build-game-*.sh:5-8`, `ci.yml:30`.
- All 95 atlases, sounds and music queue at startup (`MegamanMaverickGame.kt:928`).
- Mixed atlases: `Enemies1` 77 entity folders, `Enemies2` 54, `Projectiles1` 25. The new game ships
  Maverick sprites unless these are re-packed.
- `MegamanX.txt` is its own atlas. X borrows Mega's buster sounds.
- TMX maps use relative tileset paths (289 `../tsx` refs): shared tilesets must keep their relative place.
- Assets: 272M (music 210M). `generateAssetList` output is read by no code.
- Side finding: `build.gradle:85` `version = '$projectVersion'` (single quotes, no interpolation).

### Options

| Option | Shared fixes | Upfront change | Risk |
|---|---|---|---|
| A. New repo copy | No | None | Drift; every fix twice |
| B. Full lift and shift now | Yes | Tree-wide move | Conflicts with 6 worktrees; wrong abstractions |
| C. New game module on top of `core`, extract per need | Yes | Small: launcher + registry + asset roots | Maverick content on the new game's classpath until extracted |

Recommendation: C. Steps in order:

1. Add `games/<new>/` (game class, launcher, assets root) that depends on `:core` and `:engine`.
2. Spawn-name registry per game (removes the `PACKAGE_PREFIX` break).
3. Per-module scan roots for the smoke and constructor tests.
4. Asset list per game in place of "queue every enum entry".
5. Then extract, one item each, what the new game needs: damage table, `CharacterDef`, progression
   types, level registry, host contract `001b`.
6. When most content is out of `core`, move what is left into `games/mega-man-maverick/` (one
   mechanical commit, no other branch active, like `001a`).

### Gaps

- Closure counts come from a regex resolver, not the compiler; wildcard imports inflate them.
- Content files in blocks/hazards/special were classified by name; 43 is a lower bound.
- Not counted: TMX objects that a registry must map; raw-string asset loads outside the enums.
- No published case of a solo developer comparing the two options for 2 games.
- Owner decision open: is the X game a separate title, or X modes inside Maverick (all of `081`
  assumes inside)?

### Sources (web)

- https://www.cse.chalmers.se/~bergert/paper/2020-fse-reusecosts.pdf
- https://www.kolibrigames.com/blog/the-benefits-of-code-sharing/
- https://docs.gradle.org/current/userguide/composite_builds.html
- https://sandimetz.com/blog/2016/1/20/the-wrong-abstraction
- https://en.wikipedia.org/wiki/Rule_of_three_(computer_programming)
- https://geometrian.com/projects/blog/write_games_not_engines.html
