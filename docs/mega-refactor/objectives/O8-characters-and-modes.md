# `O8`: More playable characters and modes

- Status: active

## Problem

- `P1`: Megaman is the only playable character. Proto Man (`060`) and X (`081`) cannot be added with no large copy of
  player code while the player logic stays tied to `Megaman`.
- `P2`: Weapon data is in `when` blocks in at least 8 files (`024`), so a character cannot have its own loadout with no
  change to each file.
- `P3`: X needs features that the main game does not have, for example a dash button (`069`) and slopes (`068`).

## Goal

More than one playable character, each with its own abilities and physics, and modes such as extra levels, Spec Ops
missions and PvP that use them.

## Done means

- Proto Man is playable from the shared player parts.
- X is playable in the extra levels.

## Rules

### `O8-R1`: No copy of `Megaman.kt`

- Rule: a new character extends `MegaPlayer` and uses the shared player components and functions, not a copy of
  `Megaman.kt`. `MegaPlayer` holds only what every player class shares; character moves, body values and sprites stay
  in the character class. (Changed by the owner on 2026-11-10; before, the rule also ruled out a player base class.)
- Problem: `P1`
- Check: review the new character class.
- Applies: `linked items`
- Active: `now`

### `O8-R2`: Capabilities, not character checks

- Rule: no logic checks for a specific character where a capability fits.
- Problem: `P1`
- Check: search for `is Megaman` and casts to `Megaman` in gameplay code.
- Applies: `always-on`
- Active: `now`

### `O8-R3`: Name the modes

- Rule: a character item names the modes that it changes.
- Problem: `P3`
- Check: read `ITEM.md` and `SCOPE.md`.
- Applies: `linked items`
- Active: `now`

### `O8-R4`: One definition for each weapon

- Rule: the data of a weapon is in one definition, and a loadout belongs to a character.
- Problem: `P2`
- Check: count the `when` blocks on `MegamanWeapon`.
- Applies: `linked items`
- Active: after `024`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01, with the `084h` changes (`d796e00ba`). Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O8-R1` | `not checked` | No new character exists: 0 `ProtoMan` and 0 `CharacterDef`. A test entity shows that a player need not be `Megaman` (`SecondPlayerEntity.kt:13`). Since `081a` (2026-11-06), the shared spawn, destroy, room transition, out-of-level kill, invincible, can-move and damage rules are free functions in `players/PlayerLifecycle.kt` and `players/PlayerRules.kt`, and the damage table is `players/PlayerDamageNegotiator.kt`; `Megaman` is their only game caller, and tests run them on a player that is not `Megaman`. Since `081b` (2026-11-06), `MegamanX` is a second player class: it calls the same `players/` functions, `players/playerJumpBehavior(JumpParams)` and `PlayerDamageNegotiator`, and has its own body, run and buster; no copy of `Megaman.kt`. Since `081c`, the wall slide and the air dash are shared too (`players/PlayerWallSlide.kt`, `PlayerAirDash.kt`); 5 of Megaman's 8 behaviors still take the `Megaman` type. Since `081d` (2026-11-07), X's ground dash is its own behavior (`BehaviorType.GROUND_DASHING`, `megamanx/MegamanXMoves.kt`), and the shared jump takes 3 more parameters in `JumpParams`; Megaman's ground slide is not shared. Since `081e` (2026-11-09), X's sabre is its own code (`megamanx/MegamanXSabre.kt` and the `MegamanXSabreSlash` entity); Megaman has no sabre, so nothing is shared or copied. Since `060a` (2026-11-10), Megaman's run, body (fixtures, gravity table, water, ice) and gravity values are shared too (`players/PlayerRun.kt`, `PlayerBody.kt`, `PlayerGravity.kt`), with parameter classes and hooks; `Megaman` is their only caller, and X keeps his own body and run. Since `060j` (2026-11-11), `Megaman` and `MegamanX` extend the abstract `players/MegaPlayer`, which owns the shared lifecycle and components (the new rule text); the moves are plain functions that take `player: MegaPlayer`, and the run's movement is the generic `entities/movement/Run.kt`. Since `060b` (2026-11-11), `Protoman` is a third player class on `MegaPlayer` (stand, step, run, jump, fall, water; test level `REFACTOR_TEST_060` only): he calls `playerRun`, `playerBody` and the shared jump, and copies no `Megaman` code; the sprite hide and flash rule (`players/PlayerSprite.kt`) and `Direction.upSign()` are shared by the 3 classes; 0 new `is Protoman` checks. Since `081g` (2026-11-13), the ladder climb is shared (`players/PlayerClimb.kt`, `ClimbParams`); `Megaman` and `MegamanX` call it, and X's crouch is his own (X6 rules, `megamanx/MegamanXClimbCrouch.kt`); 0 new `is` checks. Since `060c` (2026-11-14), Proto Man is playable with 2 weapon slots (sabre and Proto Buster, `ProtomanWeapon`; since `060l` (2026-11-16) one input as X: `B` the buster, `UP`+`B` the sabre, no slots); since `060m` (2026-11-16) he also wall slides, air dashes and swims through the shared `PlayerWallSlide`, `PlayerAirDash` and the new `players/PlayerSwim.kt`, and Megaman's `A` task (`players/PlayerAButton.kt`, a component) and camera turn (`turnBodyAndCamera`) are shared, a held guard on `DOWN` (a crouch guard on the ground, a `SHIELD` fixture) and crouch attacks; his rules stay in `entities/protoman/`. The sabre, the buster and the charge are shared: `players/PlayerSabre.kt` and the `PlayerSabreSlash` entity (X and Proto Man; X's own sabre classes are deleted), `players/PlayerBuster.kt` (`BusterParams`, X and Proto Man), `players/PlayerCharge.kt` (Megaman and X). Since `060d` (2026-11-15), the ground slide is shared too (`players/PlayerGroundSlide.kt`, `GroundSlideParams`; Megaman and Proto Man), and Proto Man climbs with the shared `PlayerClimb.kt`; his slide and ladder shots, guard breaks and the slide-to-crouch delay stay in `entities/protoman/`; 0 new `is` checks. `CharacterDef` is not made yet. | `060`, `081` |
| `O8-R2` | `partly` | Since `024b` (2026-11-16): 0 `is Megaman` / `as Megaman` / `as? Megaman` lines in code outside `entities/megaman/` (the 5 workarounds of `081b`, the `GetWeaponScreen` cast and `EntityQueries.shooting` go; screens read `IGameEntity.weapons`, and `MegaPlayer.shooting` gives each player's flag). 1 `is MegamanX` line since `081d` (`MegaLevelScreen.kt:804`: `SELECT` never pauses for X; a workaround with a `TODO`). Since `081e`, 4 target files check `is MegamanXSabreSlash` on the damager (`SabreBlock.kt`, `MetaWheelF.kt`, `D1000.kt`, `D1000Core.kt`): the sabre-only gate and the hard-coded damage per target. `081f` (2026-11-12) keeps these checks in the reworked D-1000 (body damage, eye damage and stun, eye dodge); Since `060c` (2026-11-14) the class is the shared `PlayerSabreSlash`, so the 4 checks hold for X and Proto Man with no second class check. This is a check of a damager class, not of a character; the damage per slash kind and per target stays `141`. `060c` added 1 `as? Protoman` line (`PlayerStatsHandler.draw`, his slot icon); `060l` (2026-11-16) removed it with the icons. 13 gameplay `Megaman::class` lines in 9 files, for example `JoeBall.kt:151`. 32 files outside `entities/megaman/` import `Megaman`. | `024`, `030` |
| `O8-R3` | `partly` | `081`, `068` and `069` name the modes. `060` `ITEM.md` does not. | `060` |
| `O8-R4` | `holds` | Since `024b` (2026-11-16): each weapon is one `WeaponDef<in O : MegaPlayer>`; each character has a `CharacterLoadout` (weapons in select order, rewards per level, a `WeaponLook` per weapon); owned weapons are derived per character from the shared completed levels (`GameState.ownsWeapon`). 0 `when` blocks on a weapon outside `entities/megaman/weapons/`. Megaman has 10 weapons; X and Proto Man have empty loadouts until `060h`. | `024` |

## Redo risk

- `029g`, `084`: all 8 files in `entities/megaman/behaviors/` and all 11 in `components/` use the `Megaman` type. `081` plans this change for the behaviors. The state class `entities/state/WeaponChargeComponent.kt` does not use it, but the 5 charge reads and `stopCharging()` in `components/WeaponChargeComponent.kt` do.
- `081a`: a second player class must call the `players/` functions, not copy the lines of `Megaman.onSpawn`, `onDestroy`, `onEvent` and `takeDamageFrom`. The branch `x6-intro-extra-level` adds 7 entries to `MegamanDamageNegotiator`, which is now `players/PlayerDamageNegotiator.kt`; they must move when it takes in `master`.
- `081b`: a third character must not copy `MegamanX` either. The `is Megaman` workarounds (5) grow with each character until `024`. Since `081c`, the jump, the wall slide and the air dash are shared; the other 5 Megaman behaviors (ground slide, crouch, climb, swim, jetpack) still take the `Megaman` type. Since `081d`, X has its own ground dash, so the ground slide stays Megaman's. A third character with a ground dash shares X's code or copies it; `CharacterDef` decides.
- `081e`: the sabre-only gate (`SabreBlock`, `MetaWheelF`) and the sabre damage are `is MegamanXSabreSlash` checks in each target, with one static damage per target for every slash kind. A second character with a sabre (or a sabre weapon from `024`) must not add a second class check in each target; a damage rule by slash kind and by target is `141`. X's values (`MegamanXValues.kt`) are the owner's tuning from the `081e` play test, not the X6 values; a change to them needs the owner.
- `029e`: `GameState.weaponsOwned` must become per character or per loadout (`024`). Done in `024b`.
- `024b` (2026-11-16): a new character's weapons go in its `CharacterLoadout`, with a `WeaponLook` per weapon; a
  weapon that two characters share is a `WeaponDef<MegaPlayer>` and reads only `MegaPlayer` members. A screen reads
  `IGameEntity.weapons`, never the player class. The pause screen keys its regions by weapon id and shows Megaman's
  grid for every character (`TODO`, `060h`).
- `146a` (2026-11-10): a new player class implements the engine marker `IPlayer`, and player code takes `IPlayer`. The
  type means "can be a player"; membership is the `PlayerComponent` (test with `asPlayer` or `isPlayer`, never with
  `is IPlayer`). `060` takes this in.
- `060a` (2026-11-10): a thin `Protoman` (`060b`) calls `playerRun`, `playerRunButton` and `playerBody` with his own
  params; he must not copy `Megaman`'s body or run. The body keeps its fixture order (the contact order);
  `MegamanBodyFixturesTest` and `megaman-body-fixtures.txt` hold Megaman's values. A class with its own move set (X)
  keeps its own body. `PlayerGravityComponent` has no `require` reader yet (`060a` `docs/NOTES.md`).
- `060j` (2026-11-11): `MegaPlayer` holds only what every player class shares (`players/README.md`); a part that one
  character needs goes in that class or in a plain move function it opts in to. The move functions are typed on
  `MegaPlayer`, with `TODO`s for item `121` (enemy clones) to widen them.
- `060c` (2026-11-14): a new sabre or buster user calls `PlayerSabre` (`SabreParams`, its own move list per
  `SabreState`) and `PlayerBuster` (`BusterParams`, with a crouch and a climb offset or `null`), not a copy. A
  class-keyed table reads `PlayerSabreSlash.fullStrength` for half and full values (`060c` `docs/class-entries.md`
  lists the tables); a new player shot class needs a row in each table. Since `060l` Proto Man has no slots and no HUD icon; his special
  weapons come with `060h`. X's `SELECT` is still an
  `is MegamanX` check (`081d`).
- `060b` (2026-11-11): each new Proto Man frame (`060c` to `060e`) needs the v2 restyle (the `Megaman_v3` physique)
  before it joins `Protoman_v2.txt` (`TODO` at `Protoman.defineAnimationsComponent`). His updatables list is a copy
  of the shared order until one frame order fits all classes (`TODO`, item `060`).
- `060d` (2026-11-15): a new slide user calls `playerGroundSlideBehavior` (`GroundSlideParams`) and gives a
  `BusterParams.slideOffset` (or `null`), not a copy. Since `024a`, Megaman's slide block is
  `GroundSlideParams.blocked = { currentWeaponPrevents(BehaviorType.GROUND_SLIDING) }`.
- `024a` (2026-11-15): a new weapon is a `WeaponDef` in `MegamanWeapons.all` (its place is the `SELECT` order); a rule
  that only some weapons have is a def field or hook with a "no change" default, never a check of one weapon in the
  reader (`weapons/README.md`). The id makes the animation, trail, pause and get-weapon region names, so a changed id
  breaks them at run time. `024b` widens the hooks from `Megaman` to the loadout owner.

## Items

| Item | Status | Note |
|------|--------|------|
| `024` | done | [One `WeaponDef` per weapon; loadout per character](../items/024-weapon-defs/ITEM.md) (main) |
| `060` | active | [Proto Man as a playable character](../items/060-proto-man/ITEM.md) (main) |
| `068` | done | [Floor slopes for extra levels](../items/068-slopes/ITEM.md) (main) |
| `123` | idea | [X6 flat step-down off a slope](../items/123-slope-flat-step-down/ITEM.md) |
| `069` | dropped | [A dash button for X](../items/069-dash-button/ITEM.md) (main) |
| `081` | active | [Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`](../items/081-playable-x/ITEM.md) (main) |
| `084` | done | [Split `Megaman` into player components](../items/084-split-megaman/ITEM.md) (main) |
| `029` | done | [`IPlayer` contract, players service and `CharacterDef`](../items/029-player-contract/ITEM.md) |
| `030` | planned | [Faction and root owner instead of `owner == megaman`](../items/030-faction/ITEM.md) |
| `061` | done | [Thin player view; locks and signals as components and functions](../items/061-thin-player/ITEM.md) |
| `102` | idea | [A game mode concept](../items/102-game-modes/ITEM.md) (main) |
| `108` | idea | [Spec Ops co-op missions](../items/108-spec-ops-mode/ITEM.md) |
| `109` | active | [PvP modes](../items/109-pvp-mode/ITEM.md) |
| `113` | idea | [Character select screen](../items/113-character-select/ITEM.md) (main) |
| `120` | idea | [A player that turns hostile and comes back](../items/120-hostile-player/ITEM.md) (main) |
| `121` | idea | [Enemy clones of a playable character](../items/121-playable-character-clones/ITEM.md) |
| `122` | idea | [Human-driven boss](../items/122-human-driven-boss/ITEM.md) (main) |
| `126` | idea | [Movement and damage blocks as lock reasons](../items/126-movement-damage-lock-reasons/ITEM.md) |
| `141` | idea | [The sabre damage by slash kind and by target](../items/141-sabre-damage-rules/ITEM.md) (main) |
| `146` | done | [A marker `IPlayer` interface, so a player has a type](../items/146-player-marker-interface/ITEM.md) |
| `148` | done | [The last player reads take `IPlayer`](../items/148-last-player-reads/ITEM.md) |
| `153` | idea | [A current-weapon icon on every player's HUD, and a HUD that fades when a player is behind it](../items/153-hud-weapon-icon-and-fade/ITEM.md) |
| `172` | idea | [Mega Man picks up and wears a `Met` helmet](../items/172-wear-met-helmet/ITEM.md) |
| `178` | active | [NPCs and AI allies that fight next to the players](../items/178-npc-allies/ITEM.md) (main) |
| `185` | active | [A second game title on the same engine and shared code](../items/185-second-title/ITEM.md) |
| `186` | idea | [A remote bot connector: outside programs drive a player and read the world](../items/186-remote-bot-connector/ITEM.md) (main) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-11-10 (owner, from the `060` `IPlayer` review): an abstract `MegaPlayer` base class owns the shared player wiring and is
  the receiver of the shared `players/` functions. `O8-R1` changes to match. Built in `060j`, before `060b`. Not a
  core `IMegaPlayer` interface (`146a` D1 stays). `MegaPlayer` extends `AbstractHealthEntity`, so `048` must move it.
- 2026-11-01 (owner): X (`081`) comes before Proto Man (`060`), and `081` adds `CharacterDef`. The first X branch plays X in slot 1 behind a dev flag, and it does not wait for `024`.

## Open questions

- The code has no game mode concept, only `DifficultyMode`. Does a mode need its own item?
- How does a player pick a character, and does each co-op slot pick its own? (`113`)
