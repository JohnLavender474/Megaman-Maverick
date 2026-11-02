# Notes: 112 Targeting as a policy that each entity owns

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Why `AbstractBoss` alone is not enough (2026-11-01, `master` at `b78708d79`)

From a code review. Nothing here comes from a run.

- `AbstractBoss` has no `init()`. One override that calls `super.init()` and then `addComponent` reaches all 18 subclasses.
- These parts are not `AbstractBoss` subclasses, and they read the target for themselves: `SigmaRatClaw`, `GutsTankFist`, `BigAssMaverickRobotHand`, `WilyCapsuleTentacle`.
- Spawned entities also read for themselves: `RatRobot`, `BabyPenguin`, `Met`, `HeliMet`, `HomingMissile`, `DrWily`.
- Attacks that read the target again in mid-attack: `SigmaRatClaw.kt:154,259`, `GutsTank.kt:393` with `GutsTankFist.kt:172,193`, `WilyFinalBoss.kt:2161-2164,2189,2453`, `WilyCapsuleTentacle.kt:459,471`, `PreciousWoman.kt:969,971`, `TimberWoman.kt:744-757`, `InfernoMan.kt:606,635`.
- A component lookup has no allocation on a repeat lookup, from a read of the sources, not from a profile.
