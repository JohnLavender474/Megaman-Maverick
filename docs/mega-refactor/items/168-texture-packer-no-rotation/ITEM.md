# 168: `allowRotation` off in the TexturePacker projects

- Status: idea
- Type: tooling
- Objectives: `O6`
- Depends on: none
- Related: `076`
- Branches: none yet

## Goal

The owner rule for atlases is "no rotation" (`docs/ways-of-working/assets.md`). The `.tps` projects in
`assets/sprites/texture_packer/` have `allowRotation` set to `true`. With the `Basic` algorithm no region is rotated
today, but the setting allows it, so a later pack can rotate a region with no warning. Set `allowRotation` to
`false` in all projects.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- `allowRotation` `false` in each `.tps` project.
- Check: a pack of each project gives the same atlas as before (same regions, same pixels).

## Owner's input

None yet. Backlog candidate of `076b` (review before the play test, 2026-11-17).

## Open questions

## Sources

- `NOTES.md` in this folder.
- `076b` `docs/TODO.md`.
