# Process log: mega-refactor-007a-json-save-slots

## Status

- State: active
- Goal: First pass of 5 JSON save slots with a `SaveStorage` interface; no legacy import (owner, 2026-11-26)
- Done: `PLAN.md` steps 1 to 4 (docs, storage and schema, game and UI, tests); not compiled here
- Next: push, draft PR, CI run as the first compile and test check; review before the play test; owner reviews the PR
- Brief: in chat (Claude on the web, `claude-web.md`)
- Blocked: no local Gradle (the proxy blocks the Gradle and Maven hosts)

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-26

- Branch started from the phone. Owner decision: no backwards compatibility, 5 slots in total. Draft PR, no HTML brief.

## 2026-11-26 (implementation agent)

- Scan of `master` at `2ebb704`; `SCOPE.md` (approved by owner decision 2026-11-26), `PLAN.md`, `NOTES.md` and
  `TODO.md` written. Decisions made without the owner are in `NOTES.md`.
- Implementation, in 3 code commits (storage and schema; game, screens and titles; tests).
- Gradle: `./gradlew` not tried (its wrapper downloads from `services.gradle.org`, blocked on 2026-11-25). The system
  Gradle 8.14.3 (`/opt/gradle`) ran once:
  `gradle --no-daemon core:test lost-memories-core:compileKotlin desktop-common:compileKotlin`. It failed at plugin
  resolution in 10 s: the proxy answers 403 for `plugins.gradle.org`, `repo.maven.apache.org`, `oss.sonatype.org` and
  `dl.google.com`. No `kotlinc` is installed. So nothing was compiled and no test ran: `engine:test`, `core:test`,
  `core:smokeTest`, `lwjgl3:jar` and `lost-memories-desktop:jar` are **not run**. The CI run of the PR is the first
  check. The code was reviewed by reading only.
- Mutation checks: not run (no Gradle). To do with the first local or CI-backed run: break the newer-version refusal in
  `SaveSlots.write`, the unknown-name skip in `SaveFileJson.readNames`, the backup move in `FileSaveStorage.write`, and
  the slot bounds `require` in `SaveSlots.fileName`; each must fail a case of `SaveSlotsTest`, `SaveFileJsonTest` or
  `FileSaveStorageTest`.
- `GameStateWeaponsTest` ("weapons are not in the save") now goes through `SaveFileJson` in place of `fromString`.
- Package docs: new `state/saves/README.md`; `titles/README.md` updated for `saveFolderName`. Root `CLAUDE.md` changes
  wait for the merge (`NOTES.md`, "Docs outside the branch folder").

## 2026-11-26 (confirm dialog)

- Owner decision: every save asks for confirmation, also for an empty slot. `SaveSlotScreen` has a YES/NO confirm state in SAVE mode; BACK cancels. Not compiled (no Gradle here); CI is the check.
