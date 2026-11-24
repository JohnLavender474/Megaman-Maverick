# Git and processes

Read this file before you commit, merge, run Gradle or stop a process. It applies to every task.

- **Stage by path.** Never use `git add -A` or `git add .`. The owner keeps local edits during a play test (for
  example a muted run script), and other sessions change files. Run `git status --short` before each commit. Before a
  squash merge, run `git diff --stat <base> <branch>` with no path filter, and confirm the list of files.
- **The main checkout is shared.** All sessions that work on `master` use the same index there, and a plain
  `git commit` takes what another session staged. Before a merge or a commit in the main checkout, run
  `git status --short`. If a change is staged or modified that is not yours, commit it to `master` in its own
  commit first (by its paths), and then continue (owner, 2026-11-23). Stop and ask the owner only when the change
  conflicts with your merge or commit: it touches the same files, it does not compile, or it looks like half-done
  work of another session. Commit with explicit paths where you can, and check `git show --stat HEAD` after the
  commit.
- **Commit process docs without asking** (owner, 2026-11-19). A docs-only change on `master` that the mega-refactor
  process makes (an item decision or open question, research in a `NOTES.md`, a branch start, a new item) is
  committed at once. Do not ask "should I commit?". The rules above still apply.
- **Do not wait for other sessions; tell the owner** (owner, 2026-11-23, after `183b`: a wait loop on another
  session's Gradle run matched its own shell and hung for over 20 minutes). Never write a loop, a background wait or
  a poll that waits for another session's process (Gradle, a test JVM, a shell) to end. When you cannot run the build
  or the tests now (another session runs Gradle in the same checkout, memory is too low, or a run fails for a reason
  outside your change, for example `NoClassDefFoundError` from a parallel build), stop and tell the owner: which
  check, why it cannot run, and what is still open. The owner keeps track and says when to retry.
- **Select a process by its PID.** Several sessions run Gradle at once on this machine. List the candidates with
  the PID, the age (`etimes`), the working folder (`readlink /proc/<pid>/cwd`) and the parent, keep only those whose
  folder is your worktree, and act on those PIDs. Never use `pkill -f <name>`, and never take `pgrep ... | head -1`
  as yours. (A `pkill -f` on 2026-11-11 stopped the test JVMs of other sessions.) A `pgrep -f` pattern also matches
  the shell whose command line holds it. A Gradle daemon's folder is `~/.gradle/daemon/<version>`, not the worktree.
- **Mutation checks: few, batched runs** (owner, 2026-11-16). A mutation check shows that a new test fails when its
  rule breaks. Each run recompiles `core`, so the cost is the number of Gradle runs, not the number of tests:
  - Scope: one mutation per rule (or per group of asserts on one rule), only for new or changed rules. Rules that a
    move keeps "rule for rule" are covered by the tests written on the old code; a move needs no new mutations.
  - Batch: put 3 to 6 independent mutations in one run. Each mutation breaks a different rule, and the failing case
    names show which mutation each failure belongs to. Split a batch only when 2 mutations make the same case fail.
  - Run only the target specs: `--tests 'full.package.ClassName'` (a `*Name` pattern finds nothing with Kotest).
  - Keep the compiler warm for the loop: run it with the daemon and the Kotlin compiler in the daemon, and a short
    idle time, so the daemon stops by itself after the loop:
    `./gradlew --daemon -Dorg.gradle.daemon.idletimeout=120000 -Pkotlin.compiler.execution.strategy=in-process ...`.
    Do not use `./gradlew --stop`: it stops the daemons of every session.
  - Restore the code exactly after each run (`git diff` of the mutated file is empty, or equal to the uncommitted
    change it had before), and write the mutations and the failing cases in the branch process log.
