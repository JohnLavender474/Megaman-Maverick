# Plan: mega-refactor-161a-random-call-lint

How to do the work in `SCOPE.md`. No new API (build config, a fixture and docs only).

## Steps

### 1. Research and spike

- Changes: find a detekt version that reads Kotlin `2.3.20`; apply it with only `ForbiddenMethodCall` on.
- Check: `detektMain` passes on the 3 modules; a planted file in `core/src/main` fails it with a finding on each
  forbidden line and none on the allowed lines.
- Done: yes (2026-11-16)

### 2. Fixture check

- Changes: move the planted file to `core/src/detektFixture/`, add `detektRandomRuleFixture` and `verifyRandomRule`
  to `core/build.gradle`, write `expected-findings.txt`.
- Check: `core:verifyRandomRule` passes; with one config entry removed it fails and names the missing finding.
- Done: yes (2026-11-16)

### 3. Wire into `check` and CI; delete the text scan

- Changes: `check` depends on `detektMain` and `verifyRandomRule`; `ci.yml` runs them and uploads the detekt
  reports; delete `RandomSourceGuardTest`.
- Check: the full automated checks of `CONTRACT.md` pass.
- Done: yes (2026-11-16)

### 4. Docs

- Changes: `engine/.../common/random/README.md` ("Rules for a change"), repo `CLAUDE.md` ("Build, run, test"),
  branch docs, brief.
- Check: no doc names `RandomSourceGuardTest` as a current check (outside history).
- Done: yes (2026-11-16)

### 5. Review before the play test

- Changes: fixes from the review.
- Check: automated checks pass again; findings in the process log.
- Done: yes (2026-11-16); findings and fixes in the process log
