# Scope: mega-refactor-077f-glacier-man-art

What this branch changes and why, and its contract. This is Claude's record. The owner approves the brief.

- Status: approved
- Approved: 2026-11-19 (owner, brief answers: D1 Fable yes, D2 frame list as listed)

## Goal

Draw the new Glacier Man art that the `077g` fight needs, before the fight code (owner, 2026-11-19: "art first").
The art goes into the atlases. No code reads it until `077g`.

## Item

`077`. The fight design is the draft `SCOPE.md` of `mega-refactor-077g-glacier-man` (the owner input of the planning
conversation, 2026-11-19). The parts that need art:

- Ice cubes: dedicated animations for the chunk (lob) and the aim (straight throw).
- Icicle Rain: Glacier Man pounds the ground with his fist; icicles form on the ceiling, shake and drop (owner,
  2026-11-19, round 1 review: in place of the big ice chunk).
- Death: freeze and shatter.

## Current state (`master` `4cba41ae4`)

- Glacier Man frames: `assets/sprites/frames/Bosses1/GlacierMan/` (21 strips; frame 64 × 48 px, drawn at 2 × as
  4 × 3 tiles). Atlas `Bosses1` (`TextureAsset.BOSSES_1`). Contact sheet of all frames:
  `temp/077f-glacier-man-art/current-frames.png` (main checkout).
- Style: white helmet and chest, two blues, black outline; he rides an ice board. The original art is the Glacier Man
  asset pack by Balloon Cart (README `CREDITS`).
- Ice cube: `SmallIceCube` (`Projectiles1`, regions `SmallIceCube/1`, `/2`).
- Icicle: `FallingIcicle` (`Projectiles2`, `still` 24 × 16, `shake` 2 frames, `shatter`; no "form" frames).
- Shards: `IceShard` entity, `Explosions1/IcicleShards.png` (16 × 80).
- Unused today: `brake_flash`, `fall_flash`.

## In scope: the frame list

Frame size and scale as the current strips. The art agent may change a frame count where the motion needs it, and
writes the final list in `docs/NOTES.md`.

| Strip | Atlas | Frames | Content |
|---|---|---|---|
| `GlacierMan/chunk` | `Bosses1` | 5 | Overhand lob of a small ice cube: gather (cube forms between the hands), raise over the head, throw forward-up, follow-through, recover |
| `GlacierMan/aim` | `Bosses1` | 4 | Straight throw: arm points forward, cube forms in the palm with a glint, snap throw, recover |
| `GlacierMan/aim_up` | `Bosses1` | 4 | As `aim`, arm at about 45° up |
| `GlacierMan/pound` | `Bosses1` | about 5 | Icicle Rain start, side view: he kneels, raises a fist and slams it on the floor in front of his board, with an impact burst |
| `GlacierMan/freeze` | `Bosses1` | 5 | Death 1: ice grows from the board up over him to a full block |
| `GlacierMan/frozen_crack` | `Bosses1` | 3 | Death 2: cracks spread over the block, with a flash on the last frame |
| `GlacierMan/frozen_shatter` | `Bosses1` | 3 | Death 3: the block bursts; the code adds `IceShard` pieces |
| `FallingIcicle/form` | `Projectiles2` | 4 | The icicle grows from a small nub to the `still` frame, same 24 × 16 cell |

## Out of scope

- Code: no class reads the new regions in this branch (`077g` does).
- Changes to the existing Glacier Man frames, `SmallIceCube`, `Snowhead`, `IceShard`.
- Sounds.

## Affected areas and risks

- Atlases `Bosses1` and `Projectiles2` are repacked. Risk: a repack changes an old region. Check by script that each
  old region keeps its name and pixels (`assets.md`).
- A page overflow with `Basic`: move the new frames to a new atlas (`assets.md`), with a new `TextureAsset` entry in
  `077g`, not here.
- The art must match the Balloon Cart style (palette, outline, proportions).

## Overlap with other branches

- `mega-refactor-078d-reactor-man-art` (active, uncommitted work): repacks `Decorations1`, `Environs1` and
  `Specials1`, not `Bosses1` or `Projectiles2`. Both branches edit the README `CREDITS`: a simple merge conflict.
- `mega-refactor-077d-reactor-man` (planned): new laser and emitter art; to check its atlases at its start.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| Always-on rules | no effect | No code change |
| `O4` (depth of bosses) | helps | Art for the richer fight |

## Credits (README `CREDITS`, before the merge)

```markdown
- Glacier Man
  - Original by [Balloon Cart](https://balloon-cart.itch.io/glacier-man-asset-pack)
  - New frames (throws, ground pound, forming icicles, freeze-and-shatter death) by Claude Fable 5.2, an AI model by Anthropic, directed by the developer
```

# Contract

## Automated checks

- [x] `./gradlew core:smokeTest` passes.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] A script check: each old region of `Bosses1` and `Projectiles2` keeps its name and its pixels.
- [x] Each new region in the frame list is in the atlas, with the frame count in `docs/NOTES.md`.

## Manual checks

No code reads the new art, so the owner reviews it outside the game:

- An animated GIF of each new strip at game scale, next to the existing frames (`temp/mega-refactor-077f-glacier-man-art/`).
- A mock-up GIF of each sequence: chunk throw with the cube in flight; pound → icicles form,
  shake, drop; freeze → crack → shatter.

## Intended changes

None in play. New regions in 2 atlases.
