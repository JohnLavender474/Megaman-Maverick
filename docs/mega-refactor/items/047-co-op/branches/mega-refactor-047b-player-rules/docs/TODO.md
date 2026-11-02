# TODO: mega-refactor-047b-player-rules

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Gravity from components. The owner, 2026-11-02: "Ideally, gravity changing is component-driven and doesn't require blind and weird type casts like this". No item covers it. `BodyGravityChangeRule` tests `IScalableGravityEntity` and `IDirectional`, and it tests for a player to call `WorldGravity.set`. Related: `048` owns `IScalableGravityEntity` and the direction from type checks to components, `058` separates the camera rotation from gravity, and the `047` "Gravity" open question asks if gravity is one value or one for each player. Decide at merge: a new item, or a decision in `048`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `GreenKoopaShell.kt:151,188` and `RedKoopaShell.kt:152,189`: the roll direction reads `targetPlayer`, not the player who kicked. With 2 players next to the shell, it can roll toward the kicker. The kicker is known in the stomp path.
- `direction = targetPlayer.direction` at `JetMet.kt:124,171`, `Starkner.kt:300`, `TellySaucer.kt:149`, `MoonEyeStone.kt:218`: the body can flip if 2 players ever have different directions. Gravity is world-wide, so this is unlikely. It is not proved.
- `IPlayers.any` does not test life or `inPlay`. The sites of this branch use `MegaPlayers.anyLive`, but the other `players.any` sites do not. A dead slot 1 keeps its body at its last position, so it can trip a scanner (`Cactus.kt:364`, `FireDispensenator.kt:240,248`). This gives a false positive, not a stuck state.
- `DarknessV4.kt:573`: `while (!lightSourceQueueIter.hasNext())` looks inverted. Not run.
