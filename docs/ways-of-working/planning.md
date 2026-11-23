# Planning and design choices

Read this file before you plan an item or a branch, or choose between a stopgap and the real fix.

- **Get it right the first time; no stopgaps at the start.** When a plan needs a seam (a clock, a renderer, an
  asset path, a split of update and draw), build the real seam that the later items also use. Do not plan a test-only
  fake or a workaround that a later item must replace. When the real seam belongs to another item, make that item
  (or its first step) a prerequisite, and change the order. Do not start with a stopgap. A test double that stands in for
  hardware the test cannot have (for example no GPU) is not a stopgap. It is one only when the real production
  structure it hides is wrong (2026-11-23, item `006` scan: the owner rejected a fake `Graphics` that item `010` would
  replace).
