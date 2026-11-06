# Notes: 081 Playable X for the extra levels, with `CharacterDef`, ported from `mmx-x6`

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-29: first scan (`master` at `685cdfe78`, `mmx-x6` at `393e376`)

### What already exists in the items

- `069`: the dash button, the `LevelType.X_LEVEL` select-button override, the NES-pad layout. Input only.
  (2026-11-01: `069` removed the override, `X_LEVEL` and the NES-pad layout.)
- `060`: Proto Man and `CharacterDef`. The nearest model for a second character.
- `029` `NOTES.md`, "What a second character needs first": values; body def; sprites, animations and sounds;
  loadout (`024`); an ability-set contract (`BehaviorType` keys, `setBehaviorAllowed`, `aButtonTask`); damage
  reaction; Mega-named APIs and `is Megaman` checks; factories that read `Megaman` fields (`048`). `061` removed
  `MegaPlayer` after that list was written, so item 7 of it must be checked again.
- `024`: `WeaponDef` and a loadout per character.
- `051`: X6-style camera bounds for the extra levels.
- `068`: slopes. They work for any body with a `FEET` fixture, so they do not need X.

### `mmx-x6` player (facts only; no code is copied)

- `game/player/`: `Player.java` (1,621 lines), `PlayerConst.java` (32), `BusterShot.java` (119),
  `BusterPoints.java` (73), `ChargeFlash.java` (56), `SabreSlash.java` (96), `PlayerStep.java` (6).
- States in `Player.java` (one `update...` method each): beam-in, stand, walk, dash, dash end, crouch, land, jump,
  fall, wall grab, wall slide, wall kick, falcon dash, ladder, climb, rope, bar, sabre, hurt, death. Charge is a
  separate update.
- `PlayerConst.java` values: `WALK_START`, `WALK`, `JUMP`, `FALL_MAX`, `FALL_GRAVITY`, `JUMP_GRAVITY`, `DASH_START`,
  `DASH_MIN`, `DASH_DECEL`, `DASH_TIME`, `WALL_SLIDE`, `WALL_KICK_UP`, `LADDER_FIRST`, `LADDER`, `BAR_MOVE`,
  `ROPE_MOVE`, `HANG_DASH`, `FALCON_DASH_TIME`, `MAX_HP`, the box offsets and half sizes, `FEET`.

## From `084h`: X has 2 charges (2026-11-01)

X has a buster charge and a special-weapon charge (`mmx-demake-tools/docs/players/attacks.md:31-32,100`), and an entity holds 1 component per class. So `WeaponChargeComponent` as it is cannot hold both. `081` decides the form: 2 timers in the part, or another form.

## `1LMX` sprite sheet (2026-11-01, `master` at `ed04d966c`)

The artist's sheet `1LMX.png` is cut into 30 strips with 110 frames in 48x48 cells. The feet are on `y=47`, as in
`Megaman_v3`. The work is on `master`: `utils/sprite_slicer/` (the tool and `sheets/MegamanX.yaml`),
`assets/sprites/frames/MegamanX/`, and `MegamanX.png` and `MegamanX.txt` in `assets/sprites/sprite_sheets/`. There is
no `TextureAsset` entry yet, because no code reads the atlas.

For the code that adds X:

- **`wallslide_shoot` is mirrored:** X's arm extends left, and Megaman's extends right (`x=7-36` against `x=12-38`).
  The code must flip this strip for X, or X shoots into the wall. `walljump_shoot` has its arm on the right.
- **The burst frames sit high:** frame 2 of `damaged` and of `air_damaged` is a burst. On the sheet, the burst is
  centered on the body and extends 5 pixels below the feet. In a cell with the feet on `y=47` there is no room below
  the feet, so the burst is about 6 pixels higher than on the sheet. See open question 5.
- **`spawn_out` rises 5 pixels at frame 1:** a glow goes 5 pixels below the feet in frames 1 to 4, and a cell has no
  room below `y=47`. Frame 0 stays on `y=47` to join `victory`, so the feet are on `y=42` in frames 1 to 4. `offsets`
  in `MegamanX.yaml` set the sideways position of frame 0 and put the beam in frames 5 and 6 on its sheet lines. The
  owner chose the rise at frame 1 (2026-11-01).
- **Names follow `Megaman_v3` where a name exists:** `run_trans`, `climb_finish`, `wallslide`. `run` has 8 frames
  (Megaman has 4), so X needs its own `AnimationDef` values. `r13_unknown` is a pose with no name yet.
- **No art for the buster charge:** the sheet has no `charge_half` or `charge_full` body versions, and this item's
  scope has charge levels. A recolor by script is possible, because the sheet has 12 colors.
- **No art for** `swim`, `cartin`, `pipe_portal_*`. X needs these only if the extra levels use these features.

## Silent failures for X (2026-11-05, `master` at `7e3a78880`, `x6-intro-extra-level` at `f4221aef5`)

Places where a `MegamanX` class compiles and runs, but plays wrongly. "X6" = the finding affects the X6 intro level.
"Fix" = what the first branch does: proper (a shared function or part, `O8-R1`), workaround (a `TODO`, documented in
the named item), or later.

| # | Finding | Where | X6 | Fix in the first branch |
|---|---|---|---|---|
| 1 | Only Megaman submits `PLAYER_SPAWN`, the level reset (24 listener files) | `Megaman.kt:362-365` | yes | proper: shared spawn function (`038` splits the event later) |
| 2 | Only Megaman submits `PLAYER_JUST_DIED`; with no event, X's death never resets the level | `Megaman.kt:372-374` | yes | proper: same shared function |
| 3 | Only Megaman handles the room-transition events | `Megaman.kt:391-415` | yes | proper: shared handler (`RoomTransitionCarries.kt` exists) |
| 4 | `GATE_INIT_OPENING` stops the charge sound | `Megaman.kt:417-421` | yes | proper: same handler |
| 5 | `STUN_PLAYER` | `Megaman.kt:423` | no | later, or in the shared handler |
| 6 | Gravity reaches only `IDirectional` players; the camera turn and signed values are in Megaman's setter | `WorldGravity.kt:14`, `Megaman.kt:130-194` | no | workaround: X is `IDirectional`, body direction only (`058`, `081`) |
| 7 | World gravity is set from the spawn data only by Megaman | `Megaman.kt:329-331` | yes | proper: shared spawn function |
| 8 | Out-of-bounds kill | `Megaman.kt:486-490` | yes | proper: call `isOutsideLevel` |
| 9 | Focus set once at screen setup | `MegaLevelScreen.kt:323` | yes | proper: set at each level start (decided) |
| 10 | Only Megaman listens to the game state; X's max health ignores heart tanks | `MegamanMaverickGame.kt:446-454`, `PlayerProgressComponent.kt` | yes | proper: X's own listener, max health set at the swap; shared progress `TODO` (`081`, `059`, `047`) |
| 11 | Invincibility rule (locks, not in play, recovery) | `Megaman.kt:87-89` | yes | proper: extract the function (`TODO` exists) |
| 12 | `canMove` | `Megaman.kt:92-94` | low | proper, with row 11 |
| 13 | The behaviors lock hook goes only to Megaman's locks | `Megaman.kt:268` | yes | proper: pass the hook |
| 14 | `editDamageFrom`: damage-increase enhancement and hard-mode +1 | `MegamanDamageNegotiator`, `Megaman.kt:461-473` | yes | proper: shared function |
| 15 | The X6 damage entries are in Megaman's negotiator | `MegamanDamageNegotiator.kt` (+14 lines on the X6 branch) | yes | workaround: the negotiator takes a `MegaGameEntity`, and X uses the same table (`081`, note in `023`) |
| 16 | `players.onPlayerDamaged` is called only by Megaman (`D1000` reads it) | `Megaman.kt:456` | yes | proper: shared `takeDamageFrom` tail |
| 17 | Damage tables use the shot class as the key (`Bullet::class` 20 lines in 14 files, `ChargedShot::class` 15 lines in 13 files, `X6Damage.kt:28-32`) | as listed | yes | workaround: X spawns `Bullet` and `ChargedShot` (`024`) |
| 18 | Stomp bounce in 6 enemies and `SwimBooster` use Megaman's values | `Goomba.kt:182` and others | no | later (`TODO`s exist) |
| 19 | `shooting` is true only for Megaman (`Met` dodges) | `EntityQueries.kt:41-43` | no | later (`024`) |
| 20 | Body fixtures and properties that rules read: `PLAYER` (pickups), `PASS_THROUGH_ONE_WAY`, `FALL_THROUGH_PRESS_UP` (`LadderTop`) | Megaman's `BodyComponent.kt:66-257` | yes | proper: X's body def has them; water and teleporter listeners later |
| 21 | Optional components with silent defaults (sand, light, gravity device, `isBehaviorActive`) | `SandReactionComponent.kt:10` and others | no | later |
| 22 | Teleport hide checks `is ISpritesEntity` | `GameEntityUtils.kt:149,161` | no | proper: X declares it (`048`) |
| 23 | Beam-in and beam-out use Megaman's art | `PlayerSpawnEventHandler.kt:94-101`, `EndLevelEventHandler.kt:90-104` | yes | workaround: visual only (`081`) |
| 24 | The pause rule reads the slot's `selectButtonAction` | `MegaLevelScreen.kt:791`, `LevelPauseRules.kt:20` | if set | later, with a `TODO` (`081`) |
| 25 | `Snowball` explosion mask is `{Megaman::class}` | `Snowball.kt:90-92` | no | later (`030`) |
| 26 | `Megaman` as a damager, and the shield-shatter sets | `StandardDamageNegotiator.kt:50,89,128`, `PreciousWoman.kt`, `PreciousTron.kt:345` | phase 4 | phase 4 (`024`, `023`) |

Other facts:

- Dead entries, no effect on X: `SwinginJoe.kt:219` and `JoeBall.kt:150-152` (the mask is not read), `DarknessV2.kt:77`,
  `DarknessV4.kt:89`, `SmallIceCube.kt:73`. 12 files import `Megaman` and do not use it. Candidates for `039`.
- Stale in `084` "Silent failures for `060`": `AbstractHealthEntity.kt:75`, `UnderwaterFan.kt:224`,
  `MegaContactFilter.kt:110-116` and `DarknessV4.kt:421` have no effect now.
- A known crash that the X6 level can reach: an enemy drops a `WeaponEnergyBulb` (`AbstractEnemy.kt:104-116`), and its
  pickup reaches the cast at `MegaLevelScreen.kt:612`. Guard it at `:612`, not with a level rule.
- The X6 intro map has: `Block`, `AbstractBlock`, `LadderTop`, `SabreBlock`, `GroundSlope`, `Ladder`, 2 `Gate`,
  `HoverGunner`, `Junkroid`, `MetaDridler`, `MetaWheelF`, the boss `D1000`, `X6Scenery`; 3 rooms, 4 camera zones, no
  scroll sections, 7 `hard_mode_only` objects.

## Camera, rooms and slot 1 (2026-11-05, `master` at `7e3a78880`)

Later on 2026-11-05, the owner chose slot 0 for X (see `ITEM.md`, "Decisions"). The slot-1 limits below no longer
apply to the plan. The facts about the slot-0 rules stay true.

The camera follows only slot 0. All the level rules in `MegaLevelScreen.kt` read slot 0 (`player`, `:165-166`):

- the focus, which is set once in `init()` (`:323`);
- the camera hold (`:288-290`) and the scroll hold (`:307`);
- the edge blocks on or off, and the scroll-edge kill (`:317-321`).

The room scan, the scroll start and the room bounds read only the focus.

What happens to slot 1:

- **Out of view:**
  - Map blocks spawn only when they enter the view. They cull after 1 s out of view (`Block.kt:74-78`,
    `getGameCameraCullingLogic`). Enemies spawn and cull the same way.
  - So slot 1 has no floor out of view, and falls.
  - A plain view edge has no `CameraEdgeBlock`. Only a scroll or zone edge has one.
- **Death:**
  - Slot 1 dies from the out-of-bounds kill (1.5 tiles past the map, `Megaman.kt:487-490`), a `Death`, or a
    `PlayerDeath`.
  - A `Death` with `rooms` reads slot 0's current room. An `InfernoDeath` is active only in view.
  - The scroll-edge kill does not test slot 1.
- **Respawn:**
  - A slot-1 death is ignored (`MegaLevelScreen.kt:571-572`). Slot 1 comes back only when slot 0 dies and the level
    resets.
  - Checkpoints become current from the camera, so slot 1 cannot set one.
- **Room transitions:**
  - Only slot 0 starts one through the room scan. Any player can open a `Gate` (`GateRule.kt:13-19`), and the gate
    then moves the camera.
  - The path starts at slot 0's center (`CameraRoomTransition.kt:54`).
  - Each `Megaman` moves itself onto the path in its `BEGIN_ROOM_TRANS` and `CONTINUE_ROOM_TRANS` handler
    (`Megaman.kt:391-415`), so both players stack on one point.
  - A player class without this handler stays where it was, with no floor, and dies.
- **Camera effects:** any `Megaman` slot can start a camera rotation or a smooth follow (`Megaman.kt:143-152`, `:264`).

What `081a` must do for X in slot 1:

1. Handle the 3 room-transition events as `Megaman` does, through one shared function or component (`O8-R1`).
2. Run the out-of-bounds kill. `084b` moved only the test (`isOutsideLevel`), not the call.
3. Not start a camera rotation or a smooth follow.

Limits that `081a` accepts and writes down:

- X stays within about one view of slot 0.
- A gate that X opens stacks both players at slot 0's position on the cross axis.
- A dead X waits for the level reset.
- Test rooms for X have no scroll sections.

The off-screen rule, kills and holds for each player, respawn near the lead and checkpoints for each player belong to
`047`. Step 5 (X in slot 0) needs a `FocusComponent` on X and a `setFocus` that runs again when slot 0 changes.

## Sabre slash: body and slash overlay (2026-11-05, for phase 4)

The owner chose two sprites for the sabre slash: X's body in the normal 48×48 frame, and the slash as an overlay
sprite in front of it. The owner did not choose one combined frame, or 112×64 frames for every X animation. Reasons:

- X6 does the same: `SabreSlash.java` is its own entity. It follows the player, plays its own animation and draws in
  front of X (`drawPriority() = -1`).
- The slash needs its own attack box for each frame. X6 takes it from the bounds of the slash frame
  (`SabreSlash.attackBox()`).
- `SpritesComponent.kt` tints the slot-1 sprite and sets alpha 0 during the damage blink. A separate slash sprite
  keeps the green slash out of both.
- The body uses the same size and position code as every other X animation.

Strips in `assets/sprites/frames/MegamanX/`:

| Move | Body strip | Slash strip | Slash frame | Body in the slash frame (from top-left) |
|---|---|---|---|---|
| Floor | `sabre_floor.png` (48×432, 9 frames) | `sabre_floor_arc.png` (112×576, 9 frames) | 112×64 | (32, 10) |
| Air | `sabre_air.png` (48×432, 9 frames) | `sabre_air_arc.png` (104×486, 9 frames) | 104×54 | (28, 0) |
| Wall | `sabre_wall.png` (48×384, 8 frames) | `sabre_wall_arc.png` (104×432, 8 frames) | 104×54 | (28, 0) |

- The floor strips are the owner's redraw, with the hilt in X's right hand. The air and wall strips are older, with a
  different slash frame size. Read the offset for each move, not one constant.
- Floor frames 0 and 8 are the `stand` pose with an empty slash frame, added for a showcase GIF. Remove them from both
  strips if the animation must not start and end on `stand`.
- The wall strips face left, as `wallslide` does.
- In floor frames 3 to 6, the body pixels under the slash are transparent. If the slash can be hidden while the body
  pose shows (a cancel, as `endSlash()` in X6 `Player.java`), these pixels must be drawn first.
- On 2026-11-05 the redrawn floor strips are not committed, and the packed atlas (`MegamanX.png`, `MegamanX.txt`)
  still has the old floor strips. Repack before code reads these regions.

Placement (LibGDX places a sprite by its bottom-left corner, with y up):

- Bottom-left offset of the slash from the body, in art px: floor (−32, −6); air and wall (−28, −6).
- At Megaman's sprite size (`MEGAMAN_SPRITE_SIZE = 3` tiles for 48 px, so 1 art px = 2 world px), the floor slash
  sprite is 7×4 tiles and its offset is (−64, −12) world px.
- The body and the slash have the same horizontal center. The vertical center of the slash is +2 art px from the body
  center for the floor, and −3 art px for the air and wall:
  `slash.setCenter(body.centerX, body.centerY + slashCenterDyArtPx * artPx)`.
- Facing left: each sprite mirrors around its own center, so the offset does not change.
- Turned gravity: set the slash origin to the body center, not to its own center. In slash-frame px from the
  bottom-left: (56, 30) for the floor, (52, 30) for the air and wall.
- Place the slash from the body sprite's final position, after `getSpriteXTranslation()` and
  `getSpriteYTranslation()`.
- The slash draws in front of the body.

Timing is not decided. The owner's showcase GIF of the floor slash uses 80 ms per frame. The X6 body animations are
183 (floor), 184 (air) and 187 (wall) in `~/IdeaProjects/mmx-x6/assets/sprites/players/x/animations.json`, at 60 ticks
per second. The slash appears on the frame with flag `0x80`.
