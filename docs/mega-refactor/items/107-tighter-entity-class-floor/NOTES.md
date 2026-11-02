# Notes: 107 Tighter floor on the entity class count in the smoke test

Found on 2026-11-01 by the final review of `mega-refactor-004a-entity-smoke-test`, on `master` at `b8afd2632`.

The floor is more than 400, and the scan finds 432. Up to 31 classes can drop out, for example if a package moves. A
floor equal to the count would need an edit for each new class.
