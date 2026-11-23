---
status: open
verified: yes
area: content
gameplay-change: yes
found-by: co-op status research (2026-11-22)
related-items: [047, 102, 108, 112]
---

# No co-op level or co-op test room ends in a boss fight

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/levels/LevelDefinition.kt:256` (`GAUNTLET_1`) and `:270` (`X6_INTRO`): the only live `LevelType.EXTRA_LEVEL` entries.
- `core/src/main/kotlin/com/megaman/maverick/game/levels/LevelDefinition.kt:177` (`REFACTOR_TEST_047`, `RefactorTest047.tmx`): the co-op test map.

## What happens

- `GAUNTLET_1` (`Gauntlet1_v2.tmx`) has no boss.
- `X6_INTRO` sets `characters = setOf(PlayerCharacter.X)`, so `--players 2` is a launch error.
- `RefactorTest047.tmx` has no boss room and no level end. Of the `RefactorTest*.tmx` maps, only `RefactorTest077.tmx` has `event=boss`.

So no level runs the full co-op loop: Extras entry, level play, boss gate, boss fight, defeat, exit back to `EXTRAS_SCREEN`. The boss steps of `047e` and the intro landing rule (`047` `ITEM.md`, "Boss intro landing checks") have no map to test on.

## What should happen

At least one level that 2 players can start from Extras, with a boss room at the end, and a co-op test room with a boss for the `047e` checks.

## How to reproduce

- Open Extras: the list has `GAUNTLET_1` and `X6_INTRO` only.
- `./run-game-test-coop.sh`: `REFACTOR_TEST_047` has no boss room.

## Notes

- Where the level is listed, and whether Extras offers a player or mode choice, are open questions in `102` ("Menu and start") and `108`.
