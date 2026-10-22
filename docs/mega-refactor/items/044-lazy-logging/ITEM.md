# 044: Lazy debug and log strings

- Status: planned
- Type: refactor
- Depends on: none
- Related: `002`, `008`, `009`
- Branches: none yet

## Goal

Stop building about 939 debug log strings that the tag filter throws away.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `inline` lambda overloads for `debug` and `log` (with and without throwable), and a `@PublishedApi` filter check. Inline means no lambda allocation.
- A faster filter: a level array (no boxing), the level checked before the tag, a constant time formatter.
- Change the call sites with IDE `ReplaceWith` (or a script for single-line calls). Deprecate the `String` overloads.
- A CI check against new eager template calls. Measure with the `009` allocation counter.

## Owner's input

Answer to review item 43 (2026-10-22, verbatim):

> yes, I've been afraid of this but don't know how best to implement this

## Open questions

- Change all calls, or only calls with templates?
- Should `--logTags` (`008`) replace the code-level `TAGS_TO_LOG`?

## Sources

- `NOTES.md` in this folder.
