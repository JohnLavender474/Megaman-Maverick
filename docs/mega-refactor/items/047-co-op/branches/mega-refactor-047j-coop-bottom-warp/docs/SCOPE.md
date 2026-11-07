# Scope: mega-refactor-047j-coop-bottom-warp

- Status: approved
- Approved: 2026-11-06

## Goal

The co-op default view edge rules get `bottom=WARP`. In a room taller than the view, the group camera then keeps one
player fully in view (the `047g` keeper), and a player who drops fully below the view is held (no fall when its floor
culls) and warps beside the partner. With 1 player, nothing changes.

## Item

Item `047`, "Decisions", 2026-11-06: "tall rooms with 2 players: the co-op default becomes `bottom=WARP` (`047j`); top
stays `NONE`. Found at the `047h` play test in `coop_shaft` [...]. `128` stays the real fix for out-of-view culling."

## Current state

- `GameMode` (`state/SessionRules.kt:17-21`): `SINGLE`, `COOP`, `PVP`, each with `SessionRules(friendlyFire)` and the
  default `ViewEdgeRules()` (`state/ViewEdgeRules.kt`): `left=WALL, right=WALL, top=NONE, bottom=NONE, delay=0,
  damage=0`.
- `--viewEdges` starts from `mode.rules.viewEdges` (`LaunchOptionsResolver.kt:109-119`), so a key that is not given
  keeps the mode's value, and `--viewEdges bottom=NONE` turns the new default off.
- The view edge parts are built only with 2 players (`MegaLevelScreen.init()`), so `SINGLE` never reads the rules.
- A view edge acts only inside the room (`viewEdgeInsideRoom`). In a room one view tall, the bottom view edge is the
  room edge, so the rule does nothing, and a pit kills as today.
- `PlayersFocus` keeps a keeper fully in view on an axis with a `WARP` or `KILL` edge: with `bottom` only, the player
  farther from the bottom (the higher one).
- `findWarpSpot` does not test the spot for solids (item `127`, under review).
- Tests that read the default: `LaunchOptionsResolverTest` "uses the default rules when not given".

## In scope

1. `GameMode.COOP` gets `ViewEdgeRules(bottom = WARP, delay = 1f)`. `ViewEdgeRules()` keeps its own defaults (`NONE`),
   so `PVP` and `SINGLE` do not change (D1).
2. **No free partner.** A held player with no partner that is live and not held is released and dies
   (`depleteHealth()`, which the `DAMAGE` lock does not stop), so the level resets as for any slot 0 death. Today slot
   0 held while slot 1 dies is a soft lock: no warp spot, `DAMAGE` locked, and slot 1 never respawns
   (`CameraViewEdges.kt:242-244`, `MegaLevelScreen.kt:611`). The default makes it reachable in normal play. A `TODO`
   keeps "held with no partner" counted as dead for phase 6 (`047c`). Added at the brief (owner, 2026-11-06).
3. Tests: the co-op default; `--viewEdges` keeps it for keys not given and can set `bottom=NONE`; the PvP default.
4. The `047g` play checks that relied on `bottom=NONE` give it with the flag (the contract).

## Out of scope

- The warp spot test for solids (`127`, D3).
- `top` (stays `NONE`), the side rules, PvP (`109`).
- The cull of things out of view (`128`).

## New API and first callers

None. One value changes, and `CameraViewEdges.updateHolds` gets one more branch.

## Affected areas and risks

- `state/SessionRules.kt`, `LaunchOptionsResolverTest.kt`.
- Risk: the camera in every tall co-op room follows the higher player more than the midpoint. Intended.
- Risk: a pit inside a tall room, above the room bottom: a player who falls fully below the view before the pit's
  `Death` sensor is held and warps in place of dying. The review checks the tall rooms of the real stages.
- Risk: a warp into a solid (`127`) becomes more frequent.
- Accepted (review): a player who falls fully below the view before a spike floor in a tall room warps in place of
  dying, while the partner is far above. The rooms: `docs/mega-refactor/items/047-co-op/branches/mega-refactor-047j-coop-bottom-warp/process-log.md`.

## Overlap with other branches

`081d` (X dash and sabre): no overlap in `SessionRules` or the camera parts.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` Code only with a production caller | no effect | No new code. |
| `O2-R5` No logic assumes one player | helps | A tall room keeps a player in view for both players. |
| `O2-R6` No logic assumes one camera | waiting | As in `047g`. |
| `O6-R2` Named checks | helps | Checks in `coop_shaft` and `group_tall`. |

## Decisions (owner, 2026-11-06)

- D1: co-op only; `PVP` keeps `ViewEdgeRules()`.
- D2: `delay` 1 s; the dev flag `--viewEdges delay=...` is the override (no other setting).
- D3: the warp spot test (`127`) is a separate branch right after this one.
- Added: the "no free partner" fix goes into this branch, because the new default makes the soft lock reachable.

## Open questions

- None.
