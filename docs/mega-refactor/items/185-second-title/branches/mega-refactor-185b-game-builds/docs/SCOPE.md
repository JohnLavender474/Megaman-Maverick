# Scope: mega-refactor-185b-game-builds

- Status: approved (merged 2026-11-23)
- Approved: 2026-11-23 (owner request: builds for each game into `game-builds/<game>/`,
  `game-builds/` git-ignored, at least one build of each game). Small branch: no brief, no
  `PLAN.md`.

## Goal

`build-game.sh` builds a platform bundle (packr, bundled JRE, zip) of either game. Output in
`game-builds/<game>/`. At least one build of each game.

## Item

Item `185`, the build part of step 1 (owner decision of 2026-11-23 in `ITEM.md`), and the folded
bug `build-game.sh:66` (`unzip` of `jdk.zip` on every platform).

## Current state

- `build-game.sh <platform> <executable-name>`: fixed jar folder `lwjgl3/build/libs/`, fixed main
  class `com.megaman.maverick.game.lwjgl3.DesktopLauncher`, output `game-builds/<name>-<platform>/`
  and a zip next to it. The jar must be built by hand first.
- `build-game-{linux,mac,windows}.sh` read `projectVersion` and pass `Megaman-Maverick-<version>`.
- `.gitignore:174-180` ignores only some patterns in `game-builds/`. `game-builds/packr-all-4.0.0.jar`
  is tracked. `game-builds/` holds 60G of old Maverick builds.

## In scope

1. `build-game.sh <game> <platform>`: a table per game (Gradle jar task, jar path, main class,
   executable name, version key). It runs the jar task, then packr, then the zip. Output
   `game-builds/<game>/<name>-<version>-<platform>/` and `.zip`. The JREs stay in
   `game-builds/jdk/<platform>/` (shared).
2. Wrappers named like the run scripts: `build-mega-man-maverick-{linux,mac,windows}.sh` and
   `build-mega-man-x-lost-memories-{linux,mac,windows}.sh`. The old `build-game-*.sh` go.
3. Folded bug: the stray `unzip` after the platform `case`.
4. `.gitignore`: `game-builds/` ignored, except the tracked `packr-all-4.0.0.jar`.
5. Docs: root `CLAUDE.md`, `README.md`, the Lost Memories `README.md`; the `TODO` in
   `games/mega-man-x-lost-memories/desktop/build.gradle`.
6. One `linux64` build of each game (the owner's machine is Linux; the JREs of all 3 platforms
   are present).

## Out of scope

- `construo` blocks (not used by the build scripts).
- An assets folder per game: both bundles ship the root `assets/` (item open question 2).
- Moving the old builds in `game-builds/` (left in place).

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R5` | no effect | no new code API |

## Steps

1. `build-game.sh` rewrite and the 6 wrappers; remove the old wrappers.
2. `.gitignore`.
3. Docs and the `TODO` line.
4. Builds: `./build-mega-man-maverick-linux.sh`, `./build-mega-man-x-lost-memories-linux.sh`.

# Contract

## Automated checks

- [ ] `bash -n` on each build script.
- [ ] `./build-mega-man-maverick-linux.sh` and `./build-mega-man-x-lost-memories-linux.sh` write
  their folder and zip in `game-builds/<game>/`.
- [ ] `git status` shows no file of `game-builds/`.

## Manual playthrough

1. Start `game-builds/mega-man-x-lost-memories/Mega-Man-X-Lost-Memories-0.1.0-linux64/Mega-Man-X-Lost-Memories`:
   the splash shows; START opens the X6 intro.
2. Start `game-builds/mega-man-maverick/Megaman-Maverick-1.0.1-linux64/Megaman-Maverick`: Maverick
   starts as before.

## Intended changes

- Build scripts per game; output folder per game; `game-builds/` ignored.
