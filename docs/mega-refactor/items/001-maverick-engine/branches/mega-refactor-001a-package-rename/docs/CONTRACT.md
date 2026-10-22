# Contract: mega-refactor-001a-package-rename

The gameplay that this branch must not change, and how to check it. Do the checks before the
branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-10-22

This branch changes only package names. So the main proof is that the build and the tests give
the same result as the baseline in `NOTES.md`. The play test is short: it checks the paths that
use reflection or load classes by name at runtime.

## Automated checks

Run with a full JDK 17 (see `NOTES.md`):

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

- [x] The build succeeds.
- [x] `engine:test`: 50 spec files, 427 tests, 0 failures, 0 errors, 0 skipped (same as the
  baseline).
- [x] `core:test`: 4 spec files, 49 tests, 0 failures, 0 errors, 0 skipped (same as the
  baseline).
- [x] `lwjgl3:jar` builds `Megaman-Maverick-1.0.1.jar`.
- [x] No new Kotlin compiler warnings: the count of `w:` lines is 133 or less, and no warning
  names a package or an unresolved reference.

## Source checks

- [x] `git grep -n 'com\.mega\.game\.engine' -- '*.kt' '*.java' '*.gradle'` finds nothing.
- [x] `git grep -n 'com/mega/game/engine' -- engine core lwjgl3` finds nothing.
- [x] The folders `engine/src/main/kotlin/com/mega/` and `engine/src/test/kotlin/com/mega/` no
  longer exist.
- [x] The engine files show as renames, not as deletes and adds. (The move is its own commit,
  `2f8ba1cd3`, with 304 renames at 100% similarity.)
- [x] The rename commit changes no line other than `package` and `import` lines, the 3 KDoc
  links and the 2 fully qualified names in `CullableOnUncontainedTest.kt`. Check with
  `git show <sha> | grep '^[-+]' | grep -v '^[-+][-+]' | grep -v -E '^[-+](package|import) '`.

## Manual playthrough

Result (2026-10-22, owner): pass. Played part of the intro stage with `./run-game-debug.sh` at a
stable 60-61 FPS; start, pause and resume work.

Run the game with `./run-game-debug.sh` (with a JDK 17 `JAVA_HOME` if needed).

| Level | What to check |
|-------|---------------|
| Boot | The game starts with no exception, and the main menu shows. |
| `TEST_1` (or `./run-game-test.sh`) | The level loads. Enemies, blocks and hazards spawn from the map (this uses `Class.forName` on `core` class names). |
| One robot master level (for example Moon Man) | Play the first rooms: a room transition, shoot and kill an enemy, take damage, die once and respawn at a checkpoint. |

## Behaviors to check

- [ ] Pause and resume in a level.
- [ ] The level select screen opens and starts a level.
- [ ] Save and load from the main menu still work (the save data does not use engine package
  names).

## Intended changes

None.
