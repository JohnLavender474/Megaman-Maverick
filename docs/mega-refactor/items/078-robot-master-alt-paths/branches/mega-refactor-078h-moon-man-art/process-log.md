# Process log: mega-refactor-078h-moon-man-art

## Status

- State: active
- Goal: Moon Man art make-over: a new lunar rock set in the existing tile slots and a layered parallax space background.
- Done: first art pass by a Claude Fable 5.2 art agent (tileset `MoonManTiles.png` redrawn in place; 5 background layers, previews and `check.py` in `temp/moon-man-redraw/` of the worktree).
- Next: owner answers open questions 2-4 in `SCOPE.md`; then the map remap, the background wiring and packing, the README credits, and the play test.
- Blocked: none.

## Entries

## 2026-11-25

- Branch cut from `master` (`a5e61cd`) in `.worktrees/mega-refactor-078h-moon-man-art/`. It started as `art-moon-man-redraw` and was renamed to the `078h` convention the same day, once tied to item `078`.
- One Fable 5.2 art agent (owner asked for Fable by name) did the first pass. Result: `check.py` passes (2 px grid, NES palette, 3 colors + backdrop per area, at most 4 palettes on a screen); only `assets/tiled_maps/png/MoonManTiles.png` changed in git. Nothing wired in code yet.
- Docs: item `078` `ITEM.md` gained the Moon Man art entry; `SCOPE.md`, `NOTES.md` and `TODO.md` written for this branch. No `BRIEF.src.html` yet (the owner has not approved a scope).
- Owner, 2026-11-25: the original tileset is by SamuelX4; the original background is by Capcom, with no credit needed because the new background is original. `NOTES.md` and `SCOPE.md` updated.
