# Plan: mega-refactor-159a-intro-boss-art

## Steps

### 1. Cracks in `Head/defeated`

- Changes: the crack drawing (`temp/159a/draft.py`, step 2 of the drafts) written into the frame file and the atlas
  region in place.
- Check: a script compares the atlas with `master` (only the region changes); `core:smokeTest`, `lwjgl3:jar`.
- Done: yes (the Gradle checks run before the merge)
