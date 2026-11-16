# Plan: mega-refactor-060m-protoman-moves

How to do the work in `SCOPE.md`. It needs no approval. When the plan changes, write the reason in the process log.

## Steps

### 1. Art round (Fable 5.2 agent, runs beside steps 2-5)

- Changes: drafts of `wallslide`, `wallslide_shoot`, `sabre_wall` (+ arc), `wallslide_guard` (+ pull-out),
  `airdash`, `airdash_shoot`, `airdash_guard` (+ pull-out), `swim`, `swim_shoot` in
  `temp/mega-refactor-060m-protoman-moves/art/`, 48x48 frames in the `Protoman_v2` style, with a contact sheet.
- Check: the owner picks per strip (one round, D4).
- Done: yes

### 2. Megaman tests first

- Changes: tests of Megaman's swim and of the `A` task rules (ground, air, water, wall slide end, air dash end, climb
  end, spawn), on the old code.
- Check: green on the old code; a mutation per rule fails.
- Done: yes

### 3. The swim and the `A` task move into `players/`

- Changes: `players/PlayerSwim.kt` (`SwimParams`, `playerSwimBehavior`); the `A` task as one `players/` part (D1);
  Megaman calls both, rule for rule; `players/README.md`.
- Check: the step 2 tests stay green; `core:test`.
- Done: yes

### 4. Test rooms

- Changes: `RefactorTest060.tmx` rooms `wall` (a shaft with walls on both sides) and `airdash` (gaps that need a
  dash), with painted tiles; built by a `level-builder` subagent.
- Check: read back with `level-analyzer`; each room has a player spawn.
- Done: yes

### 5. Proto Man: the 3 moves, the `A` task, the camera turn

- Changes: `WALL_SLIDING` (with the wall jump), `AIR_DASHING`, `SWIMMING` through the shared code; his values in
  `ProtomanValues` (Megaman's numbers); the `A` task part; the camera turn on a gravity change. The art install
  (step 7) ran first, so the poses use the new strips at once.
- Check: new `ProtomanTest` cases; mutations.
- Done: yes

### 6. Attacks and the guard in the moves (D2, D3)

- Changes: `B` shots with offsets (from the step 1 muzzle points); `UP`+`B`: the wall slash (`SabreState.WALL`, a
  `ProtomanSabreMoves` wall move), the air slash in a dash and in water; the guard: wall guard, shielded air dash, air
  guard in water.
- Check: new `ProtomanTest` cases; mutations.
- Done: yes

### 7. Art install

- Changes: the picked strips into `assets/sprites/frames/Protoman_v2/`; the atlas repacked with the `060c` packer;
  the animation keys; the muzzle points and shot offsets.
- Check: `core:smokeTest`; each region byte-equal to its frame file.
- Done: yes

### 8. Docs and review before the play test

- Changes: `players/README.md`, `NOTES.md`, process log; review by subagents (branch diff; each manual check).
- Check: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar --rerun` green.
- Done: yes
