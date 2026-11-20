# 109: PvP modes

- Status: idea
- Type: feature
- Objectives: `O2`, `O8`
- Depends on: `047`, `102`, `030`
- Related: `023`, `024`, `094`, `095`, `108`, `111`, `118`, `119`, `120`, `122`
- Branches: none yet

## Goal

Player against player matches, on one machine or on the net: team deathmatch and objective modes. The first target is one large room. This mode and Spec Ops (`108`) are the two named multiplayer modes, and they are the crux of `O2`.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- None yet.

## Owner's input

2026-11-01 (verbatim):

> Let's go ahead and make the "spec ops" and "PvP" modes the crux of the multiplayer objective. These are the only two modes I've outright named and defined for multiplayer.

> It seems like PvP would best be designed in just a single large room, but having the ability to support multiple rooms in PvP is definitely a worthy goal. Let's make it so that first we implement just a single large room and then tackle multi-room support afterward.

See `108` for the owner's reference to Call of Duty.

2026-11-19 (verbatim):

> I want to create a simple test pvp mode and room. See mega-refactor item 109. I'm imagining in my head something like the pvp mode in Super Mario Bros 3: a single room where the 2 players duke it out. In that game, the walls are open on the sides, so if a player runs off the screen to the left, then they appear on the right side of the same floor; basically a mirror. We need to support the same thing in this room. This means that if a player is standway halfway through the left side of the screen, then the other half of their sprite & body appears on the right side of the screen. Let's research to see what it would entail to support this without affecting any other levels in the game.

## Decisions

- 2026-11-01: Spec Ops and PvP are the crux of `O2`, and they are the only two named multiplayer modes.
- 2026-11-01: the first PvP target is one large room. Multi-room support comes after it (`111`).
- 2026-11-19: a first test PvP room is one view wide, with horizontal screen wrap as in the Super Mario Bros. 3 battle mode. A player who straddles an edge has the other half of the sprite and the body on the far side. No other level changes. Research in `NOTES.md`, "Horizontal screen wrap".
- 2026-11-19: shots wrap at the edges of a wrap room, as the players do (TowerFall style, not SMB3).
- 2026-11-19: a wrap counter limits the shots. A shot wraps at most 1 time by default (a weapon can override it). After that it does not wrap, it leaves through the edge, and the camera cull removes it. Players have no limit.
- 2026-11-19: the wrap is generic for any entity with a body or a sprite. A central filter in `core` decides who wraps. Phase 1: players and shots. The physics side and the draw side both ask the filter.
- 2026-11-19: phase 1 has no per-instance override. Phase 2 adds it (for example a wrap flag that an enemy sets to true) with its first caller; until then, a `TODO` at the filter and here.
- 2026-11-19: phase 1 draws a second half only for entities that the filter accepts. A body-less effect at the edge (for example a shot's explosion) shows half. Effects go with phase 2.

## Open questions

- Camera: "large" can mean larger than one view. Then one shared camera cannot show all players, and a follow camera with one lead is not fair (inference). Is the first map limited to one view on a shared screen, or does PvP need one view for each player (`094`) from its start? This decides if `094` goes in "Depends on".
- Do maps use existing rooms, or new maps made as arenas? No live map was made for PvP. How many players does a map support, and does the map say this?
- Player damage: `DamageRule` on `DAMAGER` and `DAMAGEABLE` already allows a hit, because `Megaman.canBeDamagedBy` refuses only the player's own shots (`Megaman.kt:423`). `MegamanDamageNegotiator` then scores a shot by `ISizable` size (2, 3 or 4), not by weapon. `047b` turns this off. Must friendly fire become a rule per team, in place of off? Must each weapon have a PvP value (`023`, `024`)?
- Teams: `030` proposes `Faction { PLAYER, ENEMY, NEUTRAL }`, so all players are one faction. How does PvP express teams: a team ID on the slot, or more factions?
- Score and match rules: `levelState.levelPoints` and `coins` are session values, and `END_LEVEL` is the only level exit that a match can use. Where do kills, team score, the time limit and the win condition live? Do the objective modes need new map objects?
- Respawn: `PlayerCheckpointManager` holds one current spawn, and `PLAYER_JUST_DIED` is a level flow. What are the respawn delay, the spawn point choice and the spawn protection? Do lives exist in a match?
- Enemies and hazards in PvP: can a map hold them? `targetPlayer` and `IHazard.getDamageToMegaman` assume that all players are on one side.
- Netplay: does PvP need rollback? See `095`.
- See `102` for the mode concept, the menu, the start flow and progress.
- Wrap room: where does the flag live? `NOTES.md` recommends a `wrap` property on the `game_rooms` object (inference).
- Wrap room, phase 2 (`TODO`): which enemies, items and effects wrap, and the per-instance override that lets one opt in.
- Wrap room: must ladders, sand, gravity changers and other rules that read the real fixture shape work across the edge, or does a wrap room leave them out?

## Sources

- `NOTES.md` in this folder.
- `docs/mega-refactor/objectives/`
