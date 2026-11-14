# Contract: mega-refactor-159a-intro-boss-art

- Status: approved
- Approved: 2026-11-15 (owner)

## Automated checks

- [ ] `./gradlew core:smokeTest` passes (it reads `Head/defeated` from `Bosses3`).
- [ ] `./gradlew lwjgl3:jar` builds.
- [x] Only the `Head/defeated` region of `Bosses3.png` changes (script against `master`: 170 pixels inside, 0 outside;
  palette mode `P` kept).

## Manual playthrough

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-159a-intro-boss-art`

- `./run-game-alpha.sh --level INTRO_STAGE --room room12 --heartTanks all --healthTanks all`: at the defeat, the
  aghast head shows the cracks, blinks and trembles as before.

## Must not change

- Every other region of `Bosses3`. The death timing, blink and explosions.

## Intended changes

| Change | Before | After |
|---|---|---|
| Death head | the aghast face | the aghast face with cracks |
