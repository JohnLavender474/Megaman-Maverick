# Notes: 181 Entity pools and factories owned by the session

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## Source (2026-11-23, `master` `36048d9c0`)

From the `006` gaps scan (`temp/006-headless-scan/findings.md` in the main checkout, blocker B9; not committed).

- `MegaEntityFactory` is a global `object` (`entities/MegaEntityFactory.kt:22`). Its pools (`:25`) keep the
  entities of the last `init(services)`, and these entities hold that `MegaServices`. `init` does not clear the pools
  (`:34`). 345 `MegaEntityFactory.fetch` calls in 206 files.
- `EntityFactories` (deprecated) is a global `object` (`entities/factories/EntityFactories.kt:12`).
- `GameObjectPools` is a global `object` (`utils/GameObjectPools.kt:20`). `045` owns its scratch rules, not its owner.
- `create()` adds a `GameLogger` log receiver and log tags on each call (`MegamanMaverickGame.kt:311-327`).
- `ToxicLiquidClock` keeps static `time` and `users` that are never reset (time scan of `006`).
- `EntityInitSmokeTest` already fills the factory pools with `testServices()`.
