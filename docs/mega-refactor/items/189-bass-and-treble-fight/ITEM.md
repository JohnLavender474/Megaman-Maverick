# 189: Bass and Treble boss fight (two phases)

- Status: planned
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `118` (fight flow), `077` (boss attacks), `188` (boss upgrade in parallel)
- Branches: none yet (3 folders prepared: `189a`, `189b`, `189c`)

## Goal

A new boss fight against Bass and his dog Treble, asked for by a fan. In phase 1, Bass and Treble are two bodies on
the field that coordinate their attacks against the player. In phase 2, they combine into one entity (Treble Boost:
Bass with Treble's wings) and fight on as one.

## Proposed scope

Not approved. Each branch `SCOPE.md` decides its final scope.

| Branch | Goal | Status |
|---|---|---|
| `mega-refactor-189a-bass-sprites-moves` | Claude Fable 5.2 draws new Bass and Treble sprites for the fight (frame strips and an atlas); decide the attacks and animations of both phases | planned |
| `mega-refactor-189b-bass-test-fight` | Implement the fight (`Bass`, `Treble`, their projectiles, the fight plan) and a test map `REFACTOR_TEST_189` | blocked by `189a` |
| `mega-refactor-189c-bass-in-game` | Put the fight into the main game: where and how it starts, and what happens after the win | blocked by `189b` |

### `189a`: sprites and moves (design and assets, no gameplay code)

- **Art: new sprites drawn by Fable.** A Claude Fable 5.2 art agent draws new Bass and Treble sprites for the fight,
  under the rules of `art.md` (including the Fable confirmation). The frames go into fixed-size strips in
  `assets/sprites/frames/` and a new atlas (`BASS`, or a place in an existing boss atlas; `assets.md` decides).
- **Reference sheet.** The owner supplied the Spriters Resource sheet "Mega Man 10: Bass, Treble and Reggae"
  (ripper: Random Talking Bush; copyright Capcom). It is a reference for the look of Bass, Treble and Treble Boost,
  not the source of the game frames. The moves are not limited to the poses on the sheet: where a move needs a
  frame that the sheet does not have (see the table below), Fable draws it.
- **Moves.** An Opus agent ideates the attacks of both phases, the coordination between the two bodies, and the
  phase change, with the table below as a guide to the poses that the reference shows. The result goes in the
  branch `NOTES.md` and the decisions of the owner in `SCOPE.md`.
- **Credit** in the root `README.md` `CREDITS`, as `art.md` says for AI art: the new sprites as made by Claude Fable
  5.2, with the design of Bass and Treble by Capcom; and the reference sheet, its ripper and its source if the
  owner wants it named.

Poses on the reference sheet (2026-11-26, from the owner's upload):

| Body | Frames on the sheet | Missing for a full move set |
|---|---|---|
| Bass | teleport, power up, idle, inch walk, walk, jump, climb, dash, hurt, slip; shoot while idle, walking, jumping, climbing; throw (Rebound Striker) idle, walk, jump, climb; pick up and jump toss (prototype); collapse, recover; weapon palettes | none for a Mega Man-style boss |
| Treble (alone) | teleport, idle, toss, an arrow effect, 2 unused frames | run, jump or pounce, bite, own shot, hurt |
| Treble Boost (combined) | intro (merge), idle, shoot, prototype shoot, revert, wing parts | fly or dive pose, hurt |

The right column lists poses that the reference does not show; Fable draws them if the chosen moves need them. So
Treble's moves are not limited to support roles (teleport, toss); a run, a pounce, a bite or a shot of its own is
possible.

### `189b`: the fight in a test map

- `Bass` as the `AbstractBoss` that the fight plan finds, and `Treble` as a child entity that Bass spawns, owns and
  commands (the pattern of the multi-part bosses, for example `ElecDevil` and its parts). One boss for the
  `FightDirector`, one health bar, one win rule.
- Phase 1: Bass and Treble act from one shared plan, so that their attacks are coordinated, not two independent AIs.
- Phase change: the merge animation (Treble Boost intro), with the player's control kept or a short freeze, as `189a`
  decides.
- Phase 2: one combined entity (Bass in the Treble Boost state; Treble despawns), with its own moves, then the defeat
  (the `revert` and `collapse` frames are candidates).
- An entry in `FightPlans.find`, a test map `REFACTOR_TEST_189` and a `LevelDefinition` entry, the smoke test and
  `entity-tags.txt` lines.
- Co-op: in a 2-player session, each body chooses its target with `targetPlayer`; the two bodies may target
  different players (`O2-R5`).

### `189c`: the fight in the main game

- Where the fight happens and how it starts (a room, a trigger, an intro with or without dialogue).
- What happens after the win (a reward, a cutscene, the level end or the next room), and in a replay.
- The save: whether the win is stored (`O9-R2`, after `007`).

## Owner's input

- 2026-11-26: "A fan of Mega Man Maverick is requesting that we have a fight against Bass. A sprite sheet of Bass
  from MM10 is attached here. The sheet contains not only Bass but also his dog Treble. I think it would be cool to
  have a boss fight where the player must fight against Bass and his dog. In the first phase of the fight, Bass and
  Treble coordinate their attacks against Mega Man. In the second phase, they combine into one entity (depicted in
  the attached sheet)."
- 2026-11-26: the split: "Branch A creates the new sprites and decides on the attacks and animations. Branch B
  implements the boss fight as a test level. Branch C sets up the fight in the main game, deciding where and how the
  fight is triggered, and what happens after the fight is over."

## Decisions

- 2026-11-26 (Claude, from the owner's split): three branches `189a`, `189b`, `189c`, in this order.
- 2026-11-26 (owner, replaces Claude's earlier "frames only from the sheet" decision): Claude Fable 5.2 draws new
  Bass and Treble sprites for the fight; the MM10 sheet is a reference only.

## Open questions

1. **Merge target.** Recommendation: directly into `master`. Each branch is safe to ship alone: `189a` adds frames
   with no caller in play, `189b` adds a test map, and only `189c` puts the fight in front of players.
2. **Health in phase 1.** One health bar for the pair (Treble can be hit but takes no health of its own, or adds to
   the shared bar), or Bass's bar only with Treble as a hazard that can be stunned? Recommendation: one shared bar,
   so that the fight keeps one boss and one win rule.
3. **Phase change trigger.** At half of the shared health (recommended), or when Treble is knocked out?
4. **Weakness.** Which of the player's weapons is Bass weak to in each phase?
5. **Place in the game** (`189c`): for example a rival fight in a Wily stage, an optional extra level
   (`LevelType.EXTRA_LEVEL`) that opens after a set of bosses, or a mid-stage encounter. `189c` scans and recommends.
6. **Reward** (`189c`): none, a weapon, an item, or only progress.
7. **Music:** which track the fight uses (a `MusicAsset` entry).

## Sources

- `NOTES.md` in this folder.
- The sheet: owner upload of 2026-11-26 (a copy in `temp/bass-treble-sheet/`, not committed).
- `core/.../fights/README.md` and `FightPlans.kt` (how a boss joins the fight flow).
