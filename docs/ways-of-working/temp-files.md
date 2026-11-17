# Temporary files and one-time scripts

Read this file before you write a helper script, a generator or a draft. Where `temp/` is and what goes in it is in
the root `CLAUDE.md` ("Temporary files").

- **A one-time script stays in `temp/`** (owner, 2026-11-17, branch `076a`). A script that makes or converts content
  once (for example a tile or sprite generator, a map remap or repaint, a check of a single change) is a temporary
  file, also when a branch uses it: keep it and its notes in `temp/<task>/`, not in the branch folder or elsewhere in
  git. Commit only its output (the assets, the map). A tool goes in git only when it is meant for reuse, and then it
  goes in `utils/` with a `run.sh` and a `run_tests.sh`, like the other Python tools there.
