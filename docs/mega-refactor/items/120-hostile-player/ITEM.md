# 120: A player that turns hostile and comes back

- Status: idea
- Type: feature
- Objectives: `O8`, `O2`
- Depends on: `030`, `047c`, `047e`, `112`, `118`, `119`
- Related: `023`, `024`, `031`, `048`, `058`, `081`, `082`, `102`, `109`, `121`, `122`
- Branches: none yet

## Goal

Let a player in a slot turn against the other player, as a boss or as an enemy, and come back after defeat. Example: P2 is an AI-driven character. A virus takes it over, the player fights it, and the defeat restores it as a playable character. It uses the same moves as when it plays.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. No production caller exists yet, so most APIs here wait for a real fight.

### Two separate facts

- **Who drives the input:** `ControllerComponent.input`. It already exists.
- **Which side the slot is on:** one plain capture value on the slot. Null or unset means a player. Set means "this slot is against the players". "Both" cannot be stored.

A scene changes only the first fact (`082`). A hostile turn changes the second, and also the first if an AI drives.

### Capture

- `PlayerComponent` stays on P2 for the whole fight. Removal breaks input, respawn and 6 `playerSlot!!` sites.
- `Players.capture` and `Players.release` are the only writers. A second capture of one slot throws.
- `isPlayer` reads the value through to the caller, with no per-frame recompute. `playerSlot` does not change. `Players.register` must read the raw slot.
- `forEach`, `any` and `all` filter by default. `forEachSlot` serves the 17 of 20 `forEach` lines that mean "every slot", including the kill loops. This changes nothing until a capture exists, so it goes in the branch that needs it.
- The value does not clear in `reset()`, and `onDestroy` can read it.
- The release is a `Fight` lease (`118`), so that no exit route can forget it. `Fight` is not an entity, so the value is not an `IGameEntity` reference. This combination is a design choice that no research checked, so the first `SCOPE.md` must test it.
- The name is `capture`, not `hold`, because `hold` collides with `held()`, `cameraHold` and "held (stand)" in `082`.

### `DamagePolicy`

The abstraction layer that decides who can hurt whom. `IDamageNegotiator` still says how much.

- `relation(source, target)` gives `SELF`, `ALLY`, `FOE` or `NONE`. `NONE` means "no opinion", so all other logic stays as it is.
- `MegaServices` owns it. It holds `SessionRules`, which stays a read-only `val` and answers one case: player against player.
- It replaces `isFriendlyFire` at `ProjectileRules.kt:29`, the owner walk at `Megaman.kt:435-436` and `PreciousGem.kt:208`, with a gate in `DamageRule`. The `DamageRule` gate is a hot path, so it needs a before and after measure (`O5-R2`).
- It walks raw to the slot owner, because `rootPlayer` goes null for a captured P2.
- Before `030`, it derives a side from the slot owner and from `is AbstractEnemy`. After `030`, it reads `FactionComponent`, and a slot's faction derives from the capture value.

### Fight flow

1. A trigger starts a `Fight` whose `cast` holds P2.
2. `destroy()` and `spawn()` at the takeover end a mid-jump, a charge or a stun, and `onSpawn` gives full health. One guard stops this from counting as a death. The spawn passes `BOUNDS`, `FACING`, `DIRECTION` and the scalars through one props helper, because a missing `DIRECTION` turns the world.
3. Shots whose root owner is P2 are destroyed before the capture, because the owner walk then changes.
4. The capture starts. An intro uses a `DAMAGE` lock with a new reason.
5. Any P2 death ends the fight through `PLAYER_DESTROYED` with `died=true`. This covers damage, crushers, death fixtures and falls. No health route and no death immunity are written.
6. The fight plays the defeat with a stand-in sprite, while P2 is out of the world (option B2 in `NOTES.md`).
7. At the end of the outro, the `restorePlayer` step spawns P2 at one fixed anchor.

### Fixes that come before the first capture

`Megaman.kt:435-436,456`, `ReceiverRules.kt:109`, `Players.kt:16`, `BodyGravityChangeRule.kt:24`, `PortalHopper.kt:206`, `CameraEdgeBlock.kt:59`, `FeetCartRule.kt:34`, `ChargedShot.kt:118`, `AbstractBlock.kt:44`, `IBodyExtensions.kt:22`. The 41 other `asPlayer` lines need a line-by-line check.

### Branch order

1. `DamagePolicy` with the existing behavior.
2. The fixes above that are also wrong today.
3. `forEachSlot`, the capture, the routine and the first fight, together.

### Ordinary enemy form

P2 can also turn hostile with no boss flow. Then there is no `BossComponent`, no bar and no defeat flow. A locked room uses a `GauntletMaster`-type entity with `gauntlet_key`. `GauntletMaster` cannot hold P2 today, because it fetches waves by `EntityType.ENEMY` (`:143,:210`). Its kill tracking through `runnablesOnDestroy` works on any entity.

### AI input

Exists: `ControllerComponent.input`, `InputSource`, `InputFrameBuilder`, `PlayerInputs`. Missing: code that computes the held bits, a step that computes them once per tick, a launch way to say "slot 1 is AI", and seeded random numbers (`011`). AI bits must go through `frameBuilders[1].build(held)`, because after defeat only that builder's lock stops input.

## Owner's input

2026-11-02 (verbatim):

> Let's say that there is a 2nd player in the game; it could be an "AI"-driven character, so not played by a real-life person. Then, suddenly, the player becomes hostile and turns into a boss that the player must fight.

> Assuming P2 is also a Mega Man entity, then yes ideally it would be with Mega Man's moves - the same ones present when the player is playing as him. This applies to any other playable entity that becomes an enemy or boss.

> It might be the case that P2's character was "taken over by a virus that makes him into a boss temporarily", and defeating him restores him as a playable character.

> Agreed, anything that would kill the entity when it's a playable character must also kill him/her when he/she is a boss or enemy.

> The setup needs to be able to seamlessly handle the transition from player to boss and vice versa without edge cases. (An entity is either one or the other, not both, at the same time)

> An abstraction layer should handle rules around whether to use `SessionRules` or other rules to control this.

## Decisions

- (2026-11-02, owner) A playable character that turns hostile uses the same moves as when it plays.
- (2026-11-02, owner) `P2` holds no `BossComponent`. A `Fight` (`118`) runs the fight, and `P2` is a member of its `cast`.
- (2026-11-02, owner) The input fact and the side fact stay separate. Scenes go under `082`, with no change to `PlayerComponent`.
- (2026-11-02, owner) No per-frame recompute. The readers read the value directly.
- (2026-11-02, owner) `BossComponent` needs no equal setup, because no code asks "is this a boss?".
- (2026-11-02, owner) `isPlayer` keeps its name. If a rename comes later, `isPlayerSide` fits best, with `030`. `isPlayable` is wrong: a scene player is still a player, and the docs use "playable" in 3 other senses.
- (2026-11-02, owner) `SessionRules` never changes during play. `DamagePolicy` is the abstraction layer.
- (2026-11-02, owner) Anything that kills a playable character also kills it as a boss or enemy. This covers world kills: pits, crushers, death fixtures and level resets. No death immunity goes on `P2`.
- (2026-11-02, owner) Enemy shots do not hurt a hostile `P2`.
- (2026-11-02, owner) A hostile `P2` does not press world devices, such as gates, lifts and floor buttons. It does not collect items, and enemies do not target it. So "not a player" has one meaning.
- (2026-11-02, owner) The takeover and the recovery both go through the engine's destroy and spawn. `Megaman` is slot-owned, so this is the same object.
- (2026-11-02, owner) Restore defaults: a fixed anchor, full health, full ammo and `MEGA_BUSTER`. Shots in flight are destroyed at the turn; after `030` they can keep their spawn side.
- (2026-11-02, owner) The collapse uses a stand-in sprite, not `P2`'s real physics.
- (2026-11-02, owner) A fall out of the level plays the full defeat and counts as a win.
- (2026-11-02, owner) Slot 0 cannot be captured in the first pass. The screen getters, the camera and the death flow assume slot 0.
- (2026-11-02, owner) The first pass avoids enemies in the fight room, and the room is locked. A restore on `BEGIN_ROOM_TRANS` stays as a safety path.
- (2026-11-02, owner) `047c` merges first, so that this item is not the first owner of slot 1 respawn.
- (2026-11-02, owner) A hostile `Megaman` uses a player slot in the first version. A slot-free form is `121`.
- (2026-11-02) AI input stays in scope, because the owner's scenario is AI-driven. The role research recommended a human first; that option stays open for the first test.

## Open questions

- Does the fight end at a room change, or does it pause? The first pass locks the room, so this waits for a chase fight.
- What does an AI session look like at launch? This waits for the AI work. No `GameMode` row is needed now.

## Sources

- `NOTES.md` in this folder: the reports "Hostile AI P2 as a boss", "Player and boss roles" and "Virus recovery".
- `082` `NOTES.md`: the report "Scripted control, player holds, locks and `isPlayer`".
- `119` `NOTES.md`: the death event.
- `121` `NOTES.md`: `P2` as an ordinary enemy.
