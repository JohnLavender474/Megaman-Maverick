# 189: Villain and dog boss fight (two phases)

- Status: active
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: none
- Related: `118` (fight flow), `077` (boss attacks), `188` (boss upgrade in parallel)
- Branches: `mega-refactor-189a-duo-sprites-moves` (active); `189b`, `189c` prepared

## Goal

A new boss fight, asked for by a fan, against two new original characters: a villain and his dog. In phase 1, the
villain and the dog are two bodies on the field that coordinate their attacks against the player. In phase 2, they
combine into one entity and fight on as one.

## Proposed scope

Not approved. Each branch `SCOPE.md` decides its final scope.

| Branch | Goal | Status |
|---|---|---|
| `mega-refactor-189a-duo-sprites-moves` | Claude Fable 5.2 designs and draws the villain and the dog (frame strips and an atlas); decide the attacks and animations of both phases | planned |
| `mega-refactor-189b-duo-test-fight` | Implement the fight (both bodies, their projectiles, the fight plan) and a test map `REFACTOR_TEST_189` | blocked by `189a` |
| `mega-refactor-189c-duo-in-game` | Put the fight into the main game: where and how it starts, and what happens after the win | blocked by `189b` |

### `189a`: characters, sprites and moves (design and assets, no gameplay code)

- **Original characters.** The villain and the dog are new characters of this game, not characters of an existing
  title. Their names and look are decided in this branch.
- **Art: new sprites designed and drawn by Fable.** A Claude Fable 5.2 art agent designs both characters and the
  combined form, and draws their frames, under the rules of `art.md` (including the Fable confirmation). The frames go
  into fixed-size strips in `assets/sprites/frames/` and a new atlas (or a place in an existing boss atlas;
  `assets.md` decides).
- **Moves.** An Opus agent ideates the attacks of both phases, the coordination between the two bodies, and the
  phase change. The result goes in the branch `NOTES.md` and the decisions of the owner in `SCOPE.md`. The dog's
  moves are not limited to support roles: a run, a pounce, a bite or a shot of its own is possible.
- **Frames.** Fable draws the frames that the chosen moves need, for each body: for the villain a Mega Man-style boss
  set (for example teleport, idle, walk, jump, shoot, hurt, defeat); for the dog its moves, idle and hurt; for the
  combined form the merge, idle, its attacks, hurt and the defeat.
- **Credit** in the root `README.md` `CREDITS`, as `art.md` says for AI art: the new sprites as made by Claude Fable
  5.2.

### `189b`: the fight in a test map

- The villain as the `AbstractBoss` that the fight plan finds, and the dog as a child entity that the villain spawns,
  owns and commands (the pattern of the multi-part bosses, for example `ElecDevil` and its parts). One boss for the
  `FightDirector`, one health bar, one win rule.
- Phase 1: the villain and the dog act from one shared plan, so that their attacks are coordinated, not two
  independent AIs.
- Phase change: the merge animation, with the player's control kept or a short freeze, as `189a` decides.
- Phase 2: one combined entity (the villain in the combined state; the dog despawns), with its own moves, then the
  defeat.
- An entry in `FightPlans.find`, a test map `REFACTOR_TEST_189` and a `LevelDefinition` entry, the smoke test and
  `entity-tags.txt` lines.
- Co-op: in a 2-player session, each body chooses its target with `targetPlayer`; the two bodies may target
  different players (`O2-R5`).

### `189c`: the fight in the main game

- Where the fight happens and how it starts (a room, a trigger, an intro with or without dialogue).
- What happens after the win (a reward, a cutscene, the level end or the next room), and in a replay.
- The save: whether the win is stored (`O9-R2`, after `007`).

## Owner's input

- 2026-11-26: a fan asked for a new boss fight. The owner wants a fight against a villain and his dog: in the first
  phase they coordinate their attacks against Mega Man; in the second phase they combine into one entity.
- 2026-11-26: the split: "Branch A creates the new sprites and decides on the attacks and animations. Branch B
  implements the boss fight as a test level. Branch C sets up the fight in the main game, deciding where and how the
  fight is triggered, and what happens after the fight is over."
- 2026-11-26: the two characters are new and original: one is a villain and the other is a dog.
- 2026-11-26: the owner gave rough draft sheets of the villain (orange) and the dog (purple), with a row of the two
  combined. Fable redraws them fresh, then refines the villain's physique to match Mega Man and Proto Man (buffer
  arms, about the same height). The dog draft is not faithful to the NES and needs a complete redraw.

## Decisions

- 2026-11-26 (Claude, from the owner's split): three branches `189a`, `189b`, `189c`, in this order.
- 2026-11-26 (owner): Claude Fable 5.2 draws new sprites for the fight.
- 2026-11-26 (owner): the fight uses two new original characters, a villain and a dog, designed for this game. No
  existing character or existing sprite sheet is a source or a reference for them.
- 2026-11-26 (owner): merge target: each branch squash-merges directly into `master`.
- 2026-11-26 (owner): the owner's own rough draft sheets are the input of `189a` (frame list, poses, colors, scale,
  merge idea). They lean on Capcom designs, so Fable draws original silhouettes from them, not Bass or Treble.

## Open questions

1. ~~Merge target~~: decided, directly into `master`.
2. **Names and look** (`189a`): the names of the villain and the dog (also the class names and atlas prefixes), and
   the look of each and of the combined form.
3. **Health in phase 1.** One health bar for the pair (the dog can be hit but takes no health of its own, or adds to
   the shared bar), or the villain's bar only with the dog as a hazard that can be stunned? Recommendation: one
   shared bar, so that the fight keeps one boss and one win rule.
4. **Phase change trigger.** At half of the shared health (recommended), or when the dog is knocked out?
5. **Weakness.** Which of the player's weapons is the villain weak to in each phase?
6. **Place in the game** (`189c`): for example a rival fight in a Wily stage, an optional extra level
   (`LevelType.EXTRA_LEVEL`) that opens after a set of bosses, or a mid-stage encounter. `189c` scans and recommends.
7. **Reward** (`189c`): none, a weapon, an item, or only progress.
8. **Music:** which track the fight uses (a `MusicAsset` entry).

## Sources

- `NOTES.md` in this folder.
- `core/.../fights/README.md` and `FightPlans.kt` (how a boss joins the fight flow).
