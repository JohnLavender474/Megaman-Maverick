# 047: Co-op with multiple controllers

- Status: active
- Type: feature
- Objectives: `O2`
- Depends on: `029`, `031`
- Related: `007`, `008`, `013`, `030`, `038`, `051`, `094`, `095`, `102`, `108`, `109`, `110`, `118`, `119`, `120`
- Branches: `047a` (merged), `047b` (merged), `047g` (merged), `047h` (merged), `047j` (merged), `047c` (active)

## Goal

Support more than one controller and a co-op mode. The target content is now the dedicated modes `108` and `109`, not the main campaign (owner, 2026-11-01).

## Owner's input

From the first request (summary): all controller and behavior logic assumes one player. Add support for more than one controller and a co-op mode.

Item `029` research (2026-10-23, verbatim):

> I'd say just 2 players, but ideally the design should be able to scale up to 4 (or even more) players.

> Save states should probably be player-count agnostic, anyways. Let's say that there's co-op for the main campaign (intro stage + 8 robot masters + wily stages). Let's say that on day 1, 2 players are playing through the game. Then, on day 2, only one player wants to play. On day 2, the player can pick up where the 2-player gameplay from the previous day left off. In this case, save states are agnostic to player count; one player is registered by default on bootup, and a 2nd (or nth) player can register or de-register after the fact. (This might confirm the following design decision: camera follows only the primary player, and the primary player is treated as "the source of truth", and anything that the primary player earns is shared with the other players, for the sake of simplicity. You can fold this parenthetical into an existing or new item's notes.)

> Having it behind an interface might not be sufficient if nth players (other than the primary player) can drop in and out of the game on the fly.

## Decisions

- H (2026-10-22): lives are a shared pool or per player, by game mode. Use an interface (for example `LivesPolicy` with `SharedPool` and `PerPlayer`) so the rule is easy to swap. There will be a maximum number of players.
- H (2026-10-22): split co-op into sequential items, easiest first: (1) shared screen (M–L: camera rule, off-screen rule, room transitions and respawn with 2 players); (2) split screen (L–XL: two cameras and viewports; spawn, cull and checkpoint rules must handle 2 cameras); (3) online (XL: lockstep on top of `010`, `011` and `031`, after `055`).

- 2026-10-23: the target is 2 players; the design must scale to 4 or more.
- 2026-10-23: one player is registered at boot. More players can join and leave during play. Saves do not depend on the player count.
- 2026-10-25 (from `031`): the co-op input flows belong to `047`, not `031`: the join step ("press `START`" claims a
  device), one device per player (two players never share a keyboard or a gamepad), the pause and "press `START` on
  any free device" prompt when a player's gamepad is unplugged, and reconnect. `selectButtonAction` and bindings per
  slot for slots 2 to 4. Profiles (a named copy of a slot's settings) come during or after co-op. See item `031`
  `ITEM.md` "Decisions" and `NOTES.md`.
- 2026-11-01: for the first pass, each player is a copy of `Megaman`. A new character for a co-op slot waits for `060`.
- 2026-11-01 (from `081`): one exception to the decision above. X can play in slot 1 behind a dev flag, for his development in `081`, before `060`. A character choice for co-op players still waits.
- 2026-11-01: for now, the death of slot 0 resets the level for all players. The behavior must become configurable later, with these 3 options: slot 0 death resets the level; the level does not reset until all slots die; players respawn at once until all lives are gone, with no level reset (co-op only). Only the first option is built now. The other two are `TODO`s.
- 2026-11-01: for now, friendly fire is off, and shots pass through teammates.
- 2026-11-01: for now, player 2 has a debug tint as a marker.
- 2026-11-01: for the first pass, a player spawn object can have an object property that points to the spawn of player 2, in place of a left-or-right flag. With no property, player 2 spawns 1 tile to the right of the player 1 spawn, relative to the gravity direction. The details are in the `047a` `SCOPE.md`.

- 2026-11-01: each `047` branch merges directly into `master`, with no parent branch. Each branch stays behind `--players 2`, so each one is safe to ship alone.
- 2026-11-01: Spec Ops (`108`) and PvP (`109`) are the crux of `O2`. Co-op in the main campaign is optional, and it comes after them (`110`). This changes the campaign target in the owner's 2026-10-23 words. The 2026-10-23 save decision applies only to `110`.
- 2026-11-01: gameplay rules such as friendly fire are a suite of toggles, and each toggle has a value for single player, co-op and PvP. `047b` builds a small rules object now, and `102` extends it later.
- 2026-11-01: for now, only CLI flags choose the rules. `--mode coop|pvp` chooses them, `--players 2` defaults to co-op, and either mode with 1 player is a launch error.
- 2026-11-01: for the first pass, an enemy chooses the nearest live player on each read. The owner expects this rule to need much more refinement later.
- 2026-11-01: health and weapon energy go to the player who collects them. Lives, currency, heart tanks and health tanks stay shared.
- 2026-11-02: a player entity is slot-owned, not pooled: one entity per slot, created and released by the slot's owner, not by `destroy`. The old reason ("one singleton") is out of date, but a pool does not fit, because it treats `destroy` as release and a slot keeps its player through death. The real need is "the entity in a slot can change" (`047f`, `081`). See `NOTES.md`, "Can `Megaman` be pooled?". Only 2 sites bind one instance for the session: `MegamanMaverickGame.kt:454` and `MegaLevelScreen.kt:337`.
- 2026-11-02: a released player object stays for a rejoin, unregistered. Slot 1 is built at a load point or a lobby, not in play. Slot 0 cannot leave until "What happens when the lead player leaves?" closes. A leave does not fire `PLAYER_DESTROYED` as a death.
- 2026-11-02 (from `119`): `047c` owns any listener filter that `119` does not finish.
- 2026-11-06: the shared screen uses a group camera with edge walls. The camera follows the players as a group inside the 16x14 room view (the view does not zoom). The left and right edges of the view block each player, so no player leaves the view sideways and cull and spawn keep one camera. This replaces the 2026-10-23 lean to "the camera follows the lead player", which had the campaign as its target.
- 2026-11-06: room bounds come before the shared view. At a room edge, the single-player rule applies (a transition, a wall, or a pit death at the bottom); the view edge is only a wall inside the room.
- 2026-11-06: for now, every room exit uses "carry": any one player triggers the transition, and the other players are carried along (they warp next to that player at the end of the pan). This applies to side, ladder, jump-up and fall-down exits. Other rules come later (see "Open questions", "Rooms").
- 2026-11-06: tall rooms with 2 players: the co-op default becomes `bottom=WARP` (`047j`); top stays `NONE`. Found at the `047h` play test in `coop_shaft`: one player at the top ledge and one at the shaft floor, 28 tiles apart, so the midpoint camera showed neither, and the floor under the low player culled out of view, so that player died. With `bottom=WARP`, the keeper camera keeps one player in view, and a player who drops below the view is held and warps to the partner. Chosen over waiting for `128` and over a level design rule alone. `128` stays the real fix for out-of-view culling.
- 2026-11-06: per-player transition path (`047i`, after `047h` merges). Found at the `047h` play test: slot 1 jumped into the exit wall while slot 0 stood on the floor, and slot 1 "teleported" to slot 0, because the carry moves every player to one point that starts at slot 0's center. When every live player is at the exit, each player takes only the transition-axis coordinate of that point and keeps its own other coordinate (its own path). When not every player is at the exit (a gate, which any one player triggers; later vertical exits), the shared point stays, so no player lands inside a solid; phases 3 and 4 decide more. This revises "warp next to that player" below for gathered exits. With 1 player, nothing changes. The spread of the players after a transition stays a separate backlog candidate.
- 2026-11-06: side exits with 2 players (phase 2, `047h`): "gather at the exit". A side room exit is a wall for every player. It opens only when every live player touches it from inside; then slot 0 crosses, the transition runs, and the other players are carried. Chosen over "only slot 1 waits" and "any player leads". Found at the play test of the `coop_*` route: slot 1 ran into the next room alone.
- 2026-11-06: build the shared screen in phases. Each phase adds one mechanic and gets play tested before the next: (1) one room with no exits, group camera and edge walls; (2) side exits; (3) vertical exits (ladders, jumps, falls); (4) gates; (5) camera sections (`051`); (6) death and respawn. The first co-op maps use only side and ladder exits, until phase 3 tests jump and fall exits.

## Branch split

Proposed 2026-11-01. Not approved. Each later branch gets its own scope, which can still change this list. This list was written for the main stages. The `047b` scan reviewed it against `108` and `109`: the `047b` row fits both modes. The later rows are not reviewed yet.

| Branch | Goal |
|---|---|
| `047a` `second-slot` | With `--players 2`, a second `Megaman` plays in a level on its own device |
| `047b` `player-rules` | Session rules, friendly fire, targeting, `ChargedShot` and pickups |
| `047c` | Player 2 death and respawn near the lead (shared screen phase 6). Also fixes `2026-11-02-weapon-spawned-sets-survive-death.md` and `2026-11-02-throw-axe-no-live-check.md` |
| `047d` | HUD and pause |
| `047e` | Bosses and gauntlets, as present-player steps in `118` plans |
| `047f` | Join and leave during play, device claim, unplug prompt, settings for slot 1. Adds the first production callers of `Players.unregister` and `GameState.removeListener` |
| `047g` `shared-camera` | Shared screen phase 1: a group camera with edge walls in one room with no exits (2026-11-06). Phases 2 to 5 (side exits, vertical exits, gates, camera sections) get their own branches after the phase 1 play test. It starts before `047c` to `047f`, which keep their letters because other docs and code `TODO`s name them |
| `047h` `side-exits` | Shared screen phase 2: side room exits with 2 players, "gather at the exit" (2026-11-06) |
| `047j` | The co-op default `bottom=WARP` for tall rooms (2026-11-06): `SessionRules` for `COOP` (and `PVP`?), with checks in `coop_shaft` and `group_tall` |
| `047i` | Per-player transition path at a gathered exit (2026-11-06), after `047h`. Changes `playerOnRoomTransition` (`PlayerLifecycle.kt`) and the event data from `LevelRoomHandler`, with 1-player control checks |

`038` comes before the end of phase 1 if a death must not reset the room for the other player.

`047e` depends on the first branch of `112`: a boss that keeps one target through an attack, and parts that share a target, need its targeting policy.

## Objectives audit (2026-11-01)

- Decision (owner, 2026-11-01): this item holds phase 1 of `O2`: two players on one machine, on one shared screen. Split screen is `094`, and netplay is `095`.
- `MegaPlayers.targetFor` ignores the seeker (`MegaPlayers.kt:34`). Input is built for slot 0 only. Lives are one shared count (`GameState.kt:39`).
- `Players.unregister` has only test callers. Leave during play is a possible first caller.

## Open questions

- Camera: closed 2026-11-06 (group camera with edge walls, see "Decisions").
- Tall rooms (taller than the view): `047g` (phase 1) gives a rule for each view edge (`NONE`, `WALL`, `BOUNCE`, `WARP`, `KILL`) through `--viewEdges`, and the owner played all of them (2026-11-06). `bottom=WARP` becomes the co-op default (`047j`, decision 2026-11-06). Open: the final top default, and whether side `WARP` (experimental) stays.
- Phase 6 (death and respawn, from the `047h` backlog): when slot 0 is held by a side `WARP` rule or is dead, the exit walls drop, and slot 1 can walk into the next room with no transition, because the room scan follows only slot 0. Same as before `047h`.
- Phase 5 (camera sections, from the `047g` backlog): with 2 players in a room with a `051` zone or scroll, the zone and the scroll read the group midpoint as their focus point, and `CameraEdgeBlocks` kills only slot 0 at a kill edge. The view edge rules are off in such a room until then.
- The lead player as the source of truth, with earnings shared by all players: confirm. This is one profile for the session. The lead slot is a session policy, not a helper in the players query API (`029` decision).
- What happens when the lead player leaves? Does the next slot become the lead?
- Respawn with drop-in and drop-out: is a respawn routine behind an interface (`038`) enough?
- The other shared-screen rules: see `029` `NOTES.md`, "Co-op gaps and ideas".
- Boss intro landing checks (`GutsTank`, `ElecDevil`, `WilyFinalBoss`) use `players.all { landed }` after `029c`. A dead player or one who waits to respawn never lands, so the boss would never start. Rule needed: skip players who are not present? (`TODO` comments at the four sites.)

### Effect of the dedicated modes

- The owner's decision of 2026-11-01 puts multiplayer in dedicated modes (`108`, `109`). Co-op in the main stages moves to the optional `110`.
- Questions marked **(mode)** below, and the lead player, shared earnings and save-related questions above, wait for `110` if `108` and `109` do not need them. Content made for co-op can avoid these cases by design, for example with one-screen rooms. Each question stays here until a mode shows that it does or does not apply.
- `102` asks if the co-op rules (`LivesPolicy`, the death options, friendly fire) belong to a mode, and if a lobby replaces the join flow.

### Camera and off-screen

- Off-screen rule, if the camera follows the lead: a player outside the view has no floor. Of the 2827 blocks in the live stages, 2270 spawn by camera and cull after 1 s out of view (`Block.kt:75`, `getGameCameraCullingLogic`). Enemies are absent there too, and `InfernoDeath` is off out of view (`InfernoDeath.kt:34`). Options: hold the player at the view edge (as `CameraEdgeBlock` does), warp the player to the lead, damage the player, or use a group camera. **(mode)**
- With a block at the view edge, the moving view can pin a follower against a wall. Is a pinned follower warped, or crushed? Today a crush needs a `crush_surface` (`051`). **(mode)**
- Who is the camera focus when the lead dies and the level does not reset (death options 2 and 3)? `cameraFollow.setFocus` runs once (`MegaLevelScreen.kt:336`), and `cameraHold` stops the camera when slot 0 is not in play (`:302`).
- Scroll sections: the kill at a camera edge tests only slot 0 (`MegaLevelScreen.kt:332`). Does it apply to each player? Does a scroll wait for all players to be ready, or only the lead (`:320`)? **(mode)**
- Smooth follow requests come from any player (`Megaman.kt:137`, `PipePortal.kt:126,148`). See the `051` question about whose focus the request changes.

### Rooms

- Exit rules after "carry" (decision 2026-11-06): consider more rules and when each applies. Candidates: "wait" (the exit is a wall until all players touch it; hard for jump-up exits, broken for fall-down exits, because the first player to fall must stand on air), "lead only" (only slot 0 triggers), and a hybrid (wait for side and ladder exits, carry for jump and fall exits). Questions: is the rule per mode, per map or per exit (a Tiled property)? How does a player see which rule an exit uses? How is "carry" griefing handled (one player pulls the other out of a fight, or back down a shaft)?
- Who triggers a room transition? Today two paths exist. The room scan tests only the focus (`CameraRoomBounds.kt:91,138`). `GateRule` lets any player open a `Gate`, and the `Gate` then moves the camera (`Gate.kt:265`). Options: the lead, any player, all players at the edge. **(mode)**
- What happens to the other players in a transition? Each `Megaman` sets its center to the one transition point (`Megaman.kt:386`), so all players stack. Inference: "wait behind" does not work, because the transition destroys the enemies (`LevelRoomHandler.kt:72`), and 465 blocks spawn by room. Options: all stack, all keep their offset, warp at the end. **(mode)**
- Teleports move one player: `PipePortal` (4 in live stages) and `CapsuleTeleporter` (2). If the lead goes, do the others follow? If another player goes, does the off-screen rule apply at once? **(mode)**

### Gravity

- Is gravity one value for the world, or per player? `WorldGravity.set` writes all players (`WorldGravity.kt:12`). So one player at any of the 30 `GravitySwitcharoo` objects, or at a `CapsuleTeleporter` (`CapsuleTeleporter.kt:210`), turns the other player too, and stops their movement during the turn (`Megaman.kt:148-155`). See also `058`. **(mode)**
- `MoonMan` changes gravity only while its target is alive (`MoonMan.kt:526`). With more players, does a dead target stop the change?

### Death, lives and respawn

- Death options 2 and 3 are `TODO`s, but each needs more rules before it is built. Does the music stop at the first death (`MegaLevelScreen.kt:574`)? Does each death remove a life? What is the game-over test (`livesExhausted`, `:597`)? What resets when all players are dead?
- A slot 1 respawn near the lead (`047c`): what does it cost? Today a slot 1 death is free, and it removes no life. Where does it land if the lead is in the air or over a pit? Options: the lead's last safe ground, the current checkpoint, a drop on the lead.
- Are checkpoints level-wide or per player? `PlayerCheckpointManager` holds one current spawn. With one shared camera, a checkpoint activates for all (`036`).
- Do `GauntletMaster` waves reset on any player's death, or only when all are dead (`GauntletMaster.kt:168`)? Today any `PLAYER_JUST_DIED` resets them. The 16 objects are in the extra levels, so this question stays under the `102` direction.
- `SpiderWeb` and `PreciousGem` react to any `PLAYER_JUST_DIED`. Must only the death of the stuck or the owning player count? `SpiderWeb` also keeps one set of stuck webs for all players (`SpiderWeb.kt:75`, see `096`).

### Rewards

- Whose state sets the drop odds: the killer, the lowest player, or the shared pool? `AbstractEnemy.kt:99` reads the target's lives and health.
- Who gets a one-up from floating points (`AbstractEnemy.kt:330`): the killer's slot or the pool? `029` `NOTES.md` asks this too, and `LivesPolicy` can answer it.
- Does a health or weapon fill still stop the world for all players? `PlayerStatsHandler.kt:317` turns off all systems but sprites. `029` `NOTES.md` calls this "acceptable", but with 2 players it stops play more often. See `012`.

### Enemies and bosses

- How often does an enemy choose its target? `047b` plans the nearest present player. If the choice runs each frame, a target can change on each frame between 2 players. Options: each frame, sticky for a time, at set points of the enemy's behavior. What does `targetFor` give when no player is present (`MegaPlayers.kt:38`)?
- Do enemies and bosses scale with the player count? Nothing reads the count now. If a boss scales its health, what happens when a player joins or leaves in the fight? **(mode)**
- What happens to a player outside a boss room when the `Gate` closes, and to a player who joins in a fight? `AbstractBoss` destroys itself on any `PLAYER_SPAWN` (`AbstractBoss.kt:167`), so a late spawn must not use that event (`038`). **(mode)**

### Shared objects

- What does the second player do at a one-rider object? `CartV2` (3 in live stages) carries one player, `Cart` allows one at a time (`Cart.kt:83`), and `Lift` reacts only to its target (`Lift.kt:119`). Options: one cart for each player, the other player rides along, the other player warps to the end. **(mode)**

### HUD, pause and level flow

- HUD layout: bars side by side, one corner for each player, or a compact co-op bar? It must scale to 4. `PlayerStatsHandler` draws one set for slot 0.
- Who can pause, who drives the menu, and are the weapon and health tank choices per player? Today all of these are slot 0 (`isPauseRequested` input, `MegaPlayers.menuInput`, `LevelPauseScreen.kt:562,578`).
- Is the beam one effect or one for each player? `047a` keeps one beam on slot 0. `levelState.megamanBeam` holds one value, and `DarknessV4.kt:557` lights one beam center.

### Join and leave

- When and where can a player join: only in a safe state, or also in a boss fight, a scroll or a transition? Where do they appear? With which health and weapon energy? Ammo is on the entity (`MegamanWeaponsHandler`), and capacity is fixed at startup. See also `102`, "Menu and start".
- When a player leaves, what happens to their shots, their `Cart` and their locks? `MegaPlayers.register` has a `TODO`: unregister does not unbind the input.

## Sources

- `NOTES.md` in this folder.
