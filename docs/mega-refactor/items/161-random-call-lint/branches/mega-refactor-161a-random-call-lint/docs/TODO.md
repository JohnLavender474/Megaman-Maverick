# TODO: mega-refactor-161a-random-call-lint

## Tasks

- At the merge (on `master`): update `objectives/O2-multiplayer.md`, `O2-R2` "Check" and "Current state" (the
  detekt rule replaces `RandomSourceGuardTest`), and the `O6` current state if CI gains the tasks. Answer the item
  open question in `ITEM.md` with the measured times from `NOTES.md`.
- At the merge (on `master`): `items/045-pool-ownership/ITEM.md:29` names `RandomSourceGuardTest` as a model;
  point it to the detekt rule (`config/detekt/detekt.yml`) in place of the deleted test.
- Move to a stable detekt `2.0.0` when it is released, and keep the Kotlin version inside close to `kotlinVersion`.

## Backlog candidates

- Use the same detekt setup for other rules that are text searches today, one rule at a time (for example the
  `O2-R1` search for `Gdx.input` and `controllerPoller` in gameplay code, with `ForbiddenMethodCall` or
  `ForbiddenImport`).

## Bug candidates

None.
