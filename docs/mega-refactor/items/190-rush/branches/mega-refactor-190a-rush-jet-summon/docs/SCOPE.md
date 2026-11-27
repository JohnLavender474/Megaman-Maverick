# Scope: mega-refactor-190a-rush-jet-summon

- Status: draft (the owner gave the flow and asked for a first pass with a draft PR; the decisions below were made
  without the owner and are asked in the PR)
- Approved: no

## Goal

Rush Jet goes through Rush. With Rush Jet equipped, `SHOOT` beams Rush down beside Mega Man; Rush lands, sits and
waits; on contact the game pauses while the two join; then the jet works as today. Unequipping beams Rush out, and no
Rush ability can call him again until his beam up ends. Rush uses temporary "dog" sprites until the owner's sheet
comes in (`190b`).

## Item

`190` (`ITEMS.md`). This branch takes "The Rush flow" of `ITEM.md`, all five steps, for Rush Jet only:

1. Mega Man equips a Rush ability (first: Rush Jet).
2. Mega Man presses `SHOOT`: Rush beams down beside Mega Man, on the side he faces.
3. If Rush lands on a platform, he sits and waits for contact with Mega Man.
4. On contact, an animation plays in which the two join into one (for Rush Jet: Rush forms into the jet that sits on
   Mega Man's back). The game pauses while the animation plays.
5. Mega Man can use the ability as long as it stays equipped. When Mega Man unequips it, Rush beams out, and no Rush
   ability can beam Rush down again until his beam up ends.

## Current state

See the item `NOTES.md` ("Rush Jet today", "Pause while an animation plays"). In short:

- `MegamanWeapons.RUSH_JET` turns on the jetpack and the jet air dash while it is the current weapon
  (`JetpackBehavior.kt:22`, `AirDashBehavior.kt:24-50`, `MegamanJetpackHandler.kt:26`).
- The jet look comes from the loadout look of the current weapon (`MegamanLoadout.kt:105`, `Megaman.kt` animation key
  supplier, `MegamanTrailSpriteV2.kt:60`).
- `SHOOT` calls `Megaman.shoot()` (`Megaman.kt:589`), which fires the current weapon; Rush Jet fires the buster
  (`MegamanWeaponFire.kt:169`).
- A pause that keeps the sprites drawn is `LevelStateHandler.turnOffAllSystemsButSprites()`; only the level screen
  holds it.

## In scope

- **`WeaponDef.summonsRush`** (default `false`; `true` for `RUSH_JET`): the weapon is a Rush ability. Its moves
  (`enablesJetpack`, `airDash`) and its look work only while Rush is joined; until then Mega Man moves and looks as
  with the buster.
- **`MegamanRushHandler`** (`entities/megaman/handlers/`): one per Megaman. Phases `ABSENT`, `ARRIVING`, `JOINING`,
  `JOINED`, `LEAVING`. `summon`, `dismiss`, `joined`, `reset`. First callers: `Megaman.shoot()`, `RUSH_JET.onDeselect`,
  the gated moves, `Megaman.onPlayerDestroy()`.
- **`Rush`** entity (`entities/special/Rush.kt`, `EntityType.SPECIAL`, from `MegaEntityFactory`): states
  `BEAMING_DOWN`, `LANDING`, `SITTING`, `JOINING`, `BEAMING_OUT`, `BEAMING_UP`.
  - Beam down: from the top of the view, 1.5 tiles in front of Mega Man, at a fixed beam speed with no gravity. Blocks
    are ignored while Rush's bottom is more than 2 tiles above Mega Man's top, so that Rush passes a ceiling over Mega
    Man but lands on a platform near his height.
  - Land: on `FEET_ON_GROUND`, a short land animation, then he sits (gravity on).
  - Fail: if Rush leaves the view (a pit, Mega Man walks away, a room transition) he is gone at once, and Mega Man can
    call him again.
  - Join: while Rush sits, his body overlaps Mega Man's body and Mega Man can act (in play, not damaged, not
    teleporting): Rush starts the join.
  - Beam out: dematerialize, then a beam up at the beam speed; gone when he leaves the view.
- **The join pause:** `LevelFreezeRoutine` (`levels/`, a one-method interface: `update(delta): Boolean`, `true` when
  done) and the event `EventType.START_FREEZE_ROUTINE`. The level screen turns off all systems but the sprites, updates
  the routine each frame (not while paused), and turns the systems on when it is done. Rush is the routine: he drives
  his own animation and moves from his seat onto Mega Man's back over `JOIN_DUR`. Then Rush is destroyed and the
  handler is `JOINED`. `TODO` at the code: item `012`.
- **`Megaman.shoot()`**: with a Rush ability and Rush `ABSENT`, `SHOOT` calls Rush (no shot, no shoot pose). In any
  other phase it fires the weapon as today (the buster for Rush Jet).
- **Unequip:** `RUSH_JET.onDeselect` dismisses Rush: a waiting Rush beams out where he sits; a joined Rush is spawned
  on Mega Man's back and beams out. While Rush is `LEAVING`, no Rush ability can call him.
- **Temporary art:** a "dog" placeholder atlas `Rush.txt` (new `TextureAsset.RUSH`, frames in
  `assets/sprites/frames/Rush/`, a `.tps` project with `Basic`), made by a script in `temp/`. No Fable agent: these are
  programmer placeholders that `190b` replaces with the owner's sheet.
- **Test map** `REFACTOR_TEST_190` (`RefactorTest190.tmx`): rooms `flat`, `pit`, `ledge`.
- **Tests** (Kotest): the handler phases and the summon rules, with a fake Rush (see `PLAN.md`).

## Out of scope

- The official Rush sprites and the credit (`190b`), Rush Spring (`190c`).
- Rush in a turned room: Mega Man calls Rush only when he stands upright (`direction == UP`); otherwise `SHOOT` fires
  the buster. `TODO` at the code.
- A call cost, a wait time-out, co-op rules between two Rushes (open questions of the item).
- The pause screen, the HUD and the get-weapon screen (they show the weapon, not Rush).

## Affected areas and risks

- `Megaman.kt`, the Megaman behaviors and handlers, `MegamanLoadout.kt`, `MegamanWeapons.kt`, `WeaponDef.kt`,
  `MegaLevelScreen.kt`, `EventType.kt`, `TextureAsset.kt`, `LevelDefinition.kt`; new `Rush.kt`,
  `MegamanRushHandler.kt`, `LevelFreezeRoutine.kt`; assets and the test map.
- Risk: the look switch (buster look until joined) touches every Megaman animation key. A wrong key shows no sprite.
- Risk: the freeze turns on all systems at its end, as the heart tank does; a freeze during a gate (systems off) would
  turn them on early. `TODO` at the code (item `012`).
- Risk: Rush lands inside a tall wall whose top is above the ignore line. Accepted edge case for the first pass;
  `TODO` at the code.

## Overlap with other branches

See `NOTES.md`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | no effect | The handler is per Megaman; the freeze runner is per level screen. |
| `O1-R2` | no effect | Rush is a plain `MegaGameEntity` with components. |
| `O1-R3` | no effect | The freeze starts through a buffered event, like the other level events. |
| `O1-R4` | helps | Typed fields (`summonsRush`, phases); the spawn props use existing keys only. |
| `O1-R5` | helps | Each new API has its caller in this branch. |
| `O2-R1` | no effect | Rush reads no input. |
| `O2-R2` | no effect | No random. |
| `O2-R4` | no effect | |
| `O2-R5` | helps | Each Megaman has his own handler and Rush; Rush joins only his owner. |
| `O2-R7` | helps | The join ends on a `Timer`, not on the end of the animation. |
| `O2-R9` | no effect | The freeze event carries the routine; no progress. |
| `O5-R1` | no effect | No per-frame allocation; scratch vectors from `GameObjectPools`. |
| `O6-R1` | waiting | Gradle cannot run in the cloud session; CI of the PR is the check. |
| `O6-R2` | helps | Handler tests and the play checks below. |
| `O7-R3` | no effect | |
| `O8-R2` | helps | A def field (`summonsRush`), not a weapon or character check. |
| `O9-R1` | no effect | `SHOOT` only. |
| `O4-R2` | helps | Test map `REFACTOR_TEST_190`. |

## Open questions

The decisions in `NOTES.md` ("Decisions") are asked in the PR.

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes (new `MegamanRushHandlerTest`).
- [ ] `./gradlew core:smokeTest` passes (`Rush` builds and `init()`s with the real atlas; `entity-tags.txt` lists it).
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

In the cloud these run as the CI of the PR (`claude-web.md`, rule 6).

## Manual playthrough

Experimental checks (all with `--weapons RUSH_JET`; select Rush Jet with `SELECT`):

1. `--level REFACTOR_TEST_190 --room flat`: equip Rush Jet: Mega Man looks plain (no jet on his back), `UP`+`A` does
   not fly, the air dash is the normal one. `SHOOT`: no shot; Rush beams down 1.5 tiles in front of Mega Man, lands
   and sits. `SHOOT` again: a buster shot. Walk into Rush: the game pauses, Rush moves onto Mega Man's back, play goes
   on with the jet look; `UP`+`A` flies, the jet air dash works. `SELECT` to the next weapon: Rush beams out from Mega
   Man's back; switch back to Rush Jet at once and `SHOOT`: a buster shot, no Rush until the beam up ends; then
   `SHOOT` calls him again.
2. Same room: call Rush, then `SELECT` away while he sits: he beams out where he sits.
3. `--level REFACTOR_TEST_190 --room pit`: face the pit and `SHOOT`: Rush falls through the pit and is gone; `SHOOT`
   calls him again at once.
4. `--level REFACTOR_TEST_190 --room ledge`: face the ledge and `SHOOT`: Rush lands on top of the ledge, not inside
   it; jump onto it to join.
5. Same room: call Rush, then walk away until he leaves the view: he is gone; `SHOOT` calls him again.
6. Call Rush, then die (any pit): after the respawn, Rush Jet is not current (the buster is) and no Rush is out.

Control checks:

7. `--level MOON_MAN --room room1` (any Moon Man room with a spawn): the buster and the other weapons shoot and move
   as before.

## Behaviors to check

- Pause (`START`) during the join pause: after the resume, the join ends and play goes on.
- Rush Jet with its energy empty: `SHOOT` fires the buster and does not call Rush.

## Intended changes

- Rush Jet's jetpack, jet air dash and jet look need a joined Rush; equipping Rush Jet alone gives the buster look and
  moves.
- `SHOOT` with Rush Jet and no Rush out calls Rush in place of a buster shot.
- New: Rush, the join pause, the beam out.
