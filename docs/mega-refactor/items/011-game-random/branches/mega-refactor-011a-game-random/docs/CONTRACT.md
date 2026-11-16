# Contract: mega-refactor-011a-game-random

- Status: approved
- Approved: 2026-11-14

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] New engine tests: `SeededRandom` gives the same values for the same seed; the inclusive ranges of
  `nextInt(min, maxInclusive)` and `nextFloat(min, max)` hold; `pick` and `shuffle` draw only from the stream and do
  not change the order of the input beyond the shuffle.
- [ ] Guard test (per `D4`): no direct random call (`MathUtils.random*`, `UtilMethods.random`, `java.util.Random`,
  `kotlin.random.Random`, collection `.random()`, `.shuffle()`/`.shuffled()`, `Math.random`) in `core/src/main` or
  `engine/src/main` outside the RNG package.
- [ ] The boss tests that used `UtilMethods.random.setSeed` pass with a fixed seed or a `ScriptedRandom`.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every manual check compiles,
  launches and runs as written. No game launch.

## Manual playthrough

Run from `/home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-011a-game-random`. The rooms
are checked at the review before the play test.

Experimental:

1. `./run-game-alpha.sh --level TEST_1 --randSeed 12345`: the terminal output at start shows `- Rand Seed: 12345`. A run with
   no `--randSeed` shows a different seed each time.
2. `./run-game-alpha.sh --level INFERNO_MAN --room room8 --heartTanks all --healthTanks all`: `MechaDragon` picks its
   attacks and positions with variety, not the same choice twice in a row from one millisecond (`superRandom`).

Control:

3. `./run-game-alpha.sh --level INTRO_STAGE --room room12 --heartTanks all --healthTanks all`: the intro boss picks
   attacks and aims its orbs as before; defeat explosions are spread.
4. `./run-game-alpha.sh --level X6_INTRO --room room2 --heartTanks all --healthTanks all`: the D-1000 fight: orbs,
   core and ceiling debris vary as before.
5. `./run-game-alpha.sh --level WILY_STAGE_3 --room room8 --heartTanks all --healthTanks all`: the final boss's
   attack order varies as before.
6. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: enemies drop items at about the old rate; the rain looks
   as before.
7. `./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons MOON_SCYTHES`: fire the Moon Scythes several times;
   with more than 4 scythes, they fade in the same order as on `master` (a known old fault: the scythes of the new
   shot sort as the oldest, see `TODO.md`).

## Behaviors to check

- No visible change in the spread of any random value (ranges are the same).

## Intended changes

- `superRandom` (used by `MechaDragon`) no longer gives the same index for two calls in one millisecond.
- `OrderedSet.superRandom` no longer moves the picked item to the end of the set.
- The Moon Scythe sort uses the engine spawn order, not the wall clock. The order of live scythes stays the same;
  scythes of one shot now sort by spawn order in place of the `getAllSpawned` order.
- New launch option `--randSeed <long>`; the start log shows the seed (`D3`).
