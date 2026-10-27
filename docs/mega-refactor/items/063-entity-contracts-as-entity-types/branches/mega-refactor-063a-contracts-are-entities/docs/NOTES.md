# Notes: mega-refactor-063a-contracts-are-entities

## Decisions

The owner decisions for this branch are in the item `ITEM.md` (2026-10-27) and copied in `SCOPE.md`.

## Overlap with other branches

- `fix/starkner-alive-cleanup` changes `Starkner.kt` and removes its `destroy()` override. This branch does not edit
  `Starkner.kt`. Either merge order works; after both, `Starkner` overrides `onDestroyQueued`/`onDestroy`, not
  `destroy()`.
