# Notes: mega-refactor-051d-auto-scroll

## Decisions

- 2026-10-31 (owner): `next` is a one-way Tiled object reference, not a name. The kept trailing edge stops a player from going
  back, so a section needs no link to the section before it.

- 2026-10-31 (owner): crushes use marked surfaces. See "Scope changes during the branch" in `SCOPE.md`. If a level needs an edge
  block with an extent that differs from its section, add an optional object reference.

- 2026-10-31 (owner): a y lock on a scroll is a zone that overlaps the section, and it reaches one view width past the far edge of
  the section. The "focus out of view" snap stays: a player above the locked frame gets an ease of half a view, then a jump. A level
  avoids the jump with its design, for example a ceiling over the section. A `no_snap` property waits for a real level.

## Overlap with other branches
