# Notes: 043 Lighter clone without a history rewrite

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Tooling, saves, settings, logging, profiling and asset hygiene (2026-10-22)

Copied as written from the review section "ITEM 42: Lighter clone".

````text
(1) Current state
- `.git` is 1.6 GB: 1.35 GiB in packs, 199 MiB loose, 1399 objects that can be packed.
- The largest objects are old WAV files in history, for example `Xenoblade_GaurPlains.wav` (89 MB), `Crystal_Mines.wav` (50 MB) and `Cybernetic_Factory.wav` (38 MB). Today `assets/music` has 93 ogg and 16 mp3 files.
- LFS is already set up for `game-builds/` only (`.gitattributes`), with git-lfs 3.0.2.
- Branches:
  - `fix/pool-identity-freeset` and `refactor/matrix-flat-array`: merged (2026-08-31);
  - `old-v1`: merged (2024-09-27, also on origin);
  - `temp`: merged (2025-01-12, also on origin);
  - `overhaul_1`: 9 commits not in master (2024-11-30).

(2) Safe steps (no history rewrite)
- Document a partial clone in the README or CLAUDE.md: `git clone --filter=blob:none <url>` (gets old blobs only when needed), or `--filter=blob:limit=1m`. CI can use `--depth 1` (the default for `actions/checkout`).
- Local cleanup: `git maintenance run` or `git gc`. This is local only.
- LFS for new audio: add `assets/music/** filter=lfs diff=lfs merge=lfs -text` (and `sounds` if you want).
  - Old commits stay as they are. Only new or changed files go to LFS.
  - Existing files can show as changed until someone runs `git add --renormalize`. Do this in its own commit.
  - Check the GitHub LFS storage and bandwidth limits.
  - Every contributor needs git-lfs; CI does not need LFS unless tests read audio.
- Delete the merged local branches, and delete `origin/temp` and `origin/old-v1` if not needed. Tag `overhaul_1` (for example `archive/overhaul_1`) before you delete it. This is only tidying; it saves no space, because master still reaches those objects.

(3) Recommendation
- Do the partial-clone docs, LFS for new audio and the branch tidy now.
- Do not do the following without a separate, explicit decision: `git filter-repo` or BFG to remove old WAVs, or `git lfs migrate import --everything`. Both rewrite every SHA, need a force-push, break other clones, forks and links to commits and PRs, and invalidate `overhaul_1`. Only a rewrite can make `.git` much smaller. That is the trade-off for the owner to decide.

(4) Open questions for the owner
- Is `overhaul_1` still needed?
- Are you willing to do a history rewrite at some point (for example at a release boundary)?
- Is the GitHub LFS quota OK?

(5) Proposed backlog wording
Title: Lighter clone without a history rewrite
- Document partial clone (`--filter=blob:none`) and shallow CI checkout.
- LFS for new audio from now on; renormalize in its own commit.
- Delete merged branches; tag `overhaul_1` before you delete it.
- History rewrite is out of scope; it needs a separate decision.

(6) Dependencies: 38 (retiring music shrinks the working tree, not the history), 1 (CI checkout settings).
````
