# Scope: mega-refactor-134b-interpolation-audit

What this branch changes and why. This is Claude's record. The owner approves the brief, not this file.

- Status: approved
- Approved: 2026-11-09

## Goal

Fix the known render interpolation hitches that `134a` left: a body placed on another body outside the world
steps, a snap inside a step below the guard, the shape draw with the stepped camera, and the `I` key during a pause.
Gameplay positions, collisions and the gameplay camera do not change.

## Item

`134` (`O9`, `O5`). From the `134` `ITEM.md`, section "`134b`":

- Bodies placed outside the world steps against a parent: the `GutsTank` fist (seen by the owner) and tank block,
  the `BigAssMaverickRobot` hand and orbs, the `PRECIOUS_GUARD` gems (`PreciousGemCluster`). `ElecDevilBodyPieces`
  and `RotationAnchor` move their children inside the steps.
- Sprite entities with no body that place a sprite on another body (offset 0 today).
- The shape renderer draws with the stepped camera (`--debugShapes`, the `WhiteArrowPool` outline).
- The `I` dev key during a pause changes the sprite offsets at once, but the camera offset only at resume.
- Explicit snaps at teleports and respawns (`047c` names the co-op respawn).

## Current state

Scan on `master` at `0957c567e` by 3 read-only subagents and Claude. The full inventories, with `file:line` for each
case, are in `temp/mega-refactor-134b-interpolation-audit/` (`audit-bosses-special.md`, `audit-entities.md`,
`audit-teleports-draws.md`). The summary:

The draw offset of a body is `(alpha - 1) * lastStepMove` (`WorldSystem.getDrawOffset`, `WorldSystem.kt:164-171`).
`SpritesSystem` sets the offset of the entity's own body on each sprite (`SpritesSystem.kt:45-52`).

### A. A child placed on its parent outside the steps (parent moves inside the steps)

The child has no step move, so it gets offset 0 while its parent gets an interpolated offset. The gap changes each
frame. A child placed in the parent's body `preProcess` hook acts the same (the child's `beginStep` runs after it,
because a child joins the world after its parent).

| Child | Parent | Where | Rigid |
|---|---|---|---|
| `GutsTankFist` | `GutsTank` | `GutsTankFist.kt:186`, updatable | yes |
| `ReactorManProjectile` (held) | `ReactorMan` | `ReactorMan.kt:413-417`, updatable | yes, until the throw |
| `PreciousGem` (shield) | `PreciousWoman` | `PreciousWoman.kt:523-535`, updatable | orbit on top |
| `BigAssMaverickRobotHand` (`FOLLOW`) | `BigAssMaverickRobot` | `BigAssMaverickRobotHand.kt:275-278` | yes |
| `BigAssMaverickRobotOrb` | hand | `BigAssMaverickRobotHand.kt:257-262` | no (orb `i` moves `i/n`) |
| `BigAssMaverickRobotBody` (no body) | `BigAssMaverickRobot` | `BigAssMaverickRobot.kt:455` | yes |
| `FireMetFlame` | `FireMet` | `FireMet.kt:134`, updatable | yes |
| `PetitDevilChild` | `PetitDevil` | `PetitDevil.kt:304-309` | spin on top |
| `PreciousGem` | `PreciousGemCluster` | `PreciousGemCluster.kt:128`, updatable | orbit on top |
| `PreciousGemCluster` | the player | `MegamanWeaponsHandler.kt:384-389`, lerp to the focus | lerp |
| `Electrocutie` children | `Electrocutie` | `Electrocutie.kt:213-219`, parent `preProcess` | yes |
| `CeilingCrusher` block | `CeilingCrusher` | `CeilingCrusher.kt:208`, parent `preProcess` | yes |
| `Spike` on a parent (for example a `RocketPlatform`) | parent | `Spike.kt:258-264`, own `preProcess` (1 step late) | yes |
| `MegamanXSabreSlash` (body and arc sprite) | `MegamanX` | `MegamanXSabreSlash.kt:177-193` | yes; the size changes per frame |

Not visible: `FreezableEntityHandler.kt:86` (the shield has no sprite), `SpiderWeb` (the player is stuck), parents
that do not move (`MoonEyeStone`, `FireballBar`, the `ElecDevil` parts).

### B. Blocks a player rides that move outside the steps or by a catch-up velocity

`GutsTank` block (`GutsTank.kt:365-367`), `GravityBlock` (`:179-183`), `SphinxBall` top block (`:150`),
`SteamRoller` and `PushableBlock` (the `Block` `CUSTOM_PRE_PROCESS` hook, `Block.kt:226-232`), `DynamicBlock` (no
map). Catch-up velocity `velocity = (target - position) / fixedStep`, with a target that changes once per frame:
`SigmaRatClaw.kt:309-314`, `BigAssMaverickRobotHand.kt:384-390`, `RotationAnchor.kt:118-124`,
`SwingingPlatform.kt:111-116`. The whole frame move happens in the first step. These blocks have no sprite of their
own (the parent draws them), so the draw of the block does not change; the open question is the rider's carry, which
is gameplay.

### C. Bodies that move once per frame outside the steps (a new finding)

A body moved by a behavior or an updatable (`setCenter`, `setX`, `translate` by `v * delta`) has a step move of 0, so
it is drawn at the frame time, while the camera and the step-moved bodies are drawn up to 1 step earlier
(`(1 - alpha)` steps). The gap is `alpha * step * v` and changes each frame: up to `v / 150 s` at `MEDIUM`. Example:
`D1000` (`D1000.kt:582-584`; the `preProcess` puts the body on `origin` that the updatable moves). Before `134a` these
bodies were even and the camera was not. The count of such movers is not known: 485 body position writes in 349
files on 2026-11-06, most of them snaps or attachments.

### D. Snaps

- `PipePortal.pullEntityIn` (`PipePortal.kt:133-151`) runs inside a step (`TeleporterRule`, the contact phase). The
  snap to the pipe mouth is below 1 tile, so the guard misses it: the player is drawn back toward the old spot for 1
  frame. The camera offset is 0 in that frame (the same call starts the smooth follow).
- Fine: `CapsuleTeleporter`, `PortalHopper` and the `PipePortal` exit (events, velocity 0, sprite hidden);
  `GravitySwitcharoo` (only the direction); respawn (`Body.reset()` at destroy, and the camera holds on the reset
  frame); the co-op warp (the `PlayersFocus` ease gives offset 0); ladder, crouch and slide snaps (behaviors).
- Minor: bodies that resize about their center in `preProcess` (`RodentMan.kt:544`, `IcicleTelly.kt:210`,
  `Gate.kt:303`); a vertical room transition in the air (`RoomTransitionCarries.kt:11-21`, older than `134a`).

### E. Draws outside the sprites system

- `BeamInView` and `EndLevelEventHandler` place the beam on a player that does not move: offset 0 is right.
- Trails (`MegamanTrailSpriteV2`, `KoopaShellTrailSprite`, `StarknerTrailSprite`) copy the position once: fine.
  `MegamanTrailSpriteV2` copies the stepped position, up to 1 step ahead of the drawn player (minor).
- Fonts: the only `FontsComponent` (`Togglee`) is empty.
- Shapes: `MegaLevelScreen.draw(renderer)` (`:953-955`, `TODO`) draws with the camera at its stepped position, so the
  `WhiteArrowPool` outline and the debug sections move against the level by up to 1 step while the camera follows.

### F. The `I` key during a pause

During a pause only `SpritesSystem` runs (`LevelStateHandler.kt:13-19`), and `getDrawOffset` reads the flag live, so
the sprite offsets change at once. `CameraDrawOffset.offset` is written only in `cameraController.update`, which does
not run while paused (`MegaLevelScreen.kt:848-853`).

## In scope

Approved 2026-11-09 with the owner's answers (see "Decisions").

1. **Draw link (engine, on the draw side).** `SpritesComponent.drawOffsetBody: (() -> IBody?)?`. When it is set and
   gives a body, `SpritesSystem` uses that body's draw offset for the entity's sprites; otherwise it uses the
   entity's own body (as today). The child sets the function once in `init()`; the function gives `null` while the
   child is detached, so a detach needs no extra call. The link is in the child's own sprites component, which its
   `init()` builds: a parent must not write it after `spawn()`, because `spawn()` only queues the child and a new
   pooled child has no components until the next engine update (review finding, 2026-11-09). Where the parent knows
   the anchor (the gems), the child has a plain field that the parent sets. The `CeilingCrusher` block has no sprite,
   so it needs no link. It does not follow chains: a child names the body at the end
   of the chain (a gem names the player, not the cluster). First caller: `GutsTankFist` (the tank body while
   `ATTACHED`). Other callers: the rigid cases of table A (`ReactorManProjectile` while held,
   `BigAssMaverickRobotHand` in `FOLLOW`, `BigAssMaverickRobotBody` (no body), `FireMetFlame`, `Electrocutie`
   children, `Spike` on a parent, `MegamanXSabreSlash` (the X body)), and the orbit and lerp
   cases (`PreciousGem` to `PreciousWoman` or to the player of its cluster, the cluster to its player,
   `PetitDevilChild`), where the parent's offset removes the step part of the gap and the own motion stays as it is.
2. **Restart of the step move (engine, physics state).** `IBody.restartStep()`: the step move starts again from the
   current position, so a teleport does not count as motion; outside a step the last step move becomes 0. It changes
   only the step state that `134a` put on the body (`beginStep`, `endStep`, `getStepMove`); only `getDrawOffset`
   reads that state. First caller: `PipePortal.pullEntityIn`. The guard of 1 tile stays.
4. **Shapes.** `MegaLevelScreen.draw(renderer)` moves the game camera by the camera draw offset and puts it back, as
   the batch draw does. Per-body offsets for the debug shapes per D2.
5. **`I` key during a pause.** The key does nothing while the game is paused (dev key only).
6. **Test map.** New rooms in `RefactorTest134.tmx` for the cases that a small room can show (`FireMet`,
   `PetitDevil`, `Electrocutie`, a `Spike` on a `RocketPlatform`, a `PipePortal` pair).
7. The `TODO`s at `GutsTankFist`, `GutsTank`, `SpritesSystem`, `WorldSystem` and `MegaLevelScreen` go or change.

## Out of scope

- Table B (riders and the catch-up velocity): the carry of a rider is gameplay. Per D3.
- Table C (bodies that move once per frame): it needs a second offset rule (a body at the frame time lags 1 full
  step, not `1 - alpha`), and a way to tell such a body from a child of a step-moved parent. Per D3.
- The orbs of `BigAssMaverickRobotHand` (`i/n` of the hand move): the hand offset is wrong for them, and 0 is wrong
  too; a scaled offset needs its own API. The robot moves only in its entrance fall.
- Bodies that resize about their center; the room transition in the air; the trail start position.

## Affected areas and risks

- Engine: `SpritesComponent.kt`, `SpritesSystem.kt`, `Body.kt`, `IBody.kt` (and the tests). One field read per drawn
  entity for the common case.
- Core: the entity files in table A, `PipePortal.kt`, `MegaLevelScreen.kt`, `MegamanMaverickGame.kt` (the key).
- Risk: a link that gives a body after a detach (a thrown projectile, a released gem, a fist that launches) draws
  the child with the wrong offset. Each link function gives `null` in the detached states.
- Risk: the draw link must not reach gameplay (`O2-R7`): only `SpritesSystem` reads it.

## Overlap with other branches

- `047c` (co-op respawn, planning): shares `MegaLevelScreen.kt` and the respawn. The respawn needs no snap (`Body.reset()`
  and the camera hold cover it); `047c` takes in `master` before its code.
- `081f` (X6 intro fixes, active): changes `MegamanXSabreSlash.kt` and `D1000.kt`. The sabre link is a small change
  in the same file; read `081f` again before the merge.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | A field on `SpritesComponent`, a function on `Body`. |
| `O1-R3` No new fragile order | helps | The link removes the dependency on the order of parent and child in the world. |
| `O1-R4` Typed state, not new property keys | no effect | Typed fields; no new `ConstKeys`. |
| `O1-R5` Code only with a production caller | no effect | Each API has its first caller in this branch (see "In scope"). |
| `O2-R3` One fixed tick in all play | waiting | Draw only. |
| `O2-R5` No logic assumes one player | no effect | The cluster links to its own owner, not to slot 0. |
| `O2-R6` No logic assumes one camera | waiting | The shape draw uses the one game camera offset, as the batch draw. |
| `O2-R7` Gameplay does not depend on rendering | no effect | Only `SpritesSystem` reads the link; only `getDrawOffset` reads the step state. The review checks it. |
| `O5-R1` No per-frame allocation | no effect | Fields only. |
| `O5-R2` Measure hot paths | no effect | `SpritesSystem` measured with `--diagnostics` before and after. |
| `O6-R2` Named checks | helps | Engine tests for the link, the chain, the cycle and the snap; play checks per case. |
| `O9-R1` NES-style pad | no effect | |

## Decisions (owner, 2026-11-09, at the brief)

- D1: the link lives on the sprites component, not on the body (not the recommended option). The owner's reason:
  draw logic does not belong on the body; the body is the physics and world representation. The rule is now in
  `docs/mega-refactor/CLAUDE.md` ("Keep concerns separate", `master` `c49d23d09`). So the snap of the brief
  (`snapDraw()`) is named in physics terms: `restartStep()`.
- D2: shapes get the camera draw offset only.
- D3: tables B and C become a new item at the merge (`docs/TODO.md`).

## Open questions

None.
