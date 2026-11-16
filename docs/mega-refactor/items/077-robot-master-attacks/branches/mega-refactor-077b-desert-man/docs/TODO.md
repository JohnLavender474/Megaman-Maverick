# TODO: mega-refactor-077b-desert-man

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `DesertManTuning.punchExtendSpeed` (24 / 28 / 28 / 32 tiles/s) never changes the punch: each row is faster than
  the 30 tiles/s peak cap allows (about 19.1 tiles/s average with `sineOut`), so the extend time is always the cap
  time. Remove the field or lower the cap in a later tuning pass. (Found by the `077b` test pass.)

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `DesertMan.onSpawn` cleans the `light_source_keys` value with `String.replace("\\s+", "")`, a literal replace and not
  a regex, so a value with spaces such as `"1, 2"` throws `NumberFormatException`. The live maps use `"1"`. Also on
  `master` before this branch. (Found by the `077b` test pass.) Already fixed in the active branch `fix/small-bugs-3`
  (`533b7ae1d`, the same line); no new issue file needed.
