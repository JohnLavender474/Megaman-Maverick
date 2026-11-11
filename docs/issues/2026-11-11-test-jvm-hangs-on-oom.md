---
status: closed
verified: yes
area: build
gameplay-change: no
found-by: mega-refactor-060c-protoman-buster (2026-11-11)
related-items: []
---

# A test JVM that runs out of heap hangs, and its Gradle daemon stays busy for days

## Where

- `core/build.gradle` (`test { maxHeapSize = '1g' }`), `engine/build.gradle`, and the root `build.gradle`: no `Test`
  task sets an action for an `OutOfMemoryError` or a timeout.

## What happens

A `Gradle Test Executor` can stay alive with a full heap at about 1% CPU, not making progress. The build never ends, so its Gradle daemon stays busy and never reaches its idle timeout. When the `gradlew` client
stops (a Claude Bash tool call times out, or the session ends), only the client goes. The daemon and the test JVM stay.

Found on 2026-11-11: 8 such test JVMs and their 6 daemons ran on the owner's machine, 4 from
`mega-refactor-081f-x6-intro-fixes` (about 44 hours old, about 1.18 GB each, heap full) and 4 from the deleted
worktree `fix-quicksand-two-players` (about 9 days old, about 0.6 GB each). Together with the daemons they held about
7 GB, and swap was 10 of 19 GB full. All ignored `SIGTERM`; `SIGKILL` stopped them. After that, free memory went
from 7 to 15 GB and swap use from 10 to 1 GB.

The heap of a `081f` JVM held 157,352 retained `io.mockk.Invocation` objects, each with a captured stack trace (about
960 MB of stack-trace arrays). That is a separate problem (mocks that outlive their tests record every call); this
issue is about the hang.

## What should happen

A test JVM that runs out of heap exits at once, and the build fails with a clear message. A test task that hangs for
another reason stops after a time limit.

## How to reproduce

Not reproduced. A test that fills the heap and keeps it full (a static list of 1 MB arrays) does not hang the JVM:
Kotest catches the `OutOfMemoryError` in the test thread, the next tests fail too, and the build ends normally
(105 s, no test JVM left). Inference, not measured: the stuck JVMs had an OOM in one of Gradle's own worker threads
(for example the one that reports results to the daemon), which then died and left the JVM waiting.

## Short contract

Branch: `fix/test-jvm-oom-hang`, off `master`, in `.worktrees/fix/test-jvm-oom-hang`.

Intended changes:

- In the root `build.gradle`, `configure(subprojects)`: every `Test` task (`test`, `smokeTest`, `allocationTest`)
  gets `jvmArgs '-XX:+ExitOnOutOfMemoryError'` and `timeout = Duration.ofMinutes(20)`. The full `core:test` takes
  about 2 minutes today.

Changed files: `build.gradle`.

Not changed:

- The test heap sizes (`core` keeps `maxHeapSize = '1g'`), the test code, CI, gameplay.

Checks:

- With a temporary test that allocates without limit (not committed): `./gradlew core:test` fails within a minute,
  and no `Gradle Test Executor` of the worktree is left running after the build. (Before the change, the same test
  did not hang either; see "How to reproduce".)
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

## Notes

- Fix: `fix/test-jvm-oom-hang`: `-XX:+ExitOnOutOfMemoryError` and a task `timeout` on every `Test` task.
- A `SessionEnd` hook in the owner's Claude settings stops the Gradle processes of a session's worktree, as a safety
  net for other hang causes.

## Resolution

Fixed in `fix/test-jvm-oom-hang`, squash commit `b777da0db` (2026-11-11): every `Test` task runs with
`-XX:+ExitOnOutOfMemoryError` and a 20-minute `timeout`. With the fill-the-heap test, the test JVM ends at the first
OOM ("Terminating due to java.lang.OutOfMemoryError") and the build fails in 8 s with exit value 3; no test JVM is left.
The flag also ends a JVM whose OOM hits a Gradle worker thread; the timeout ends any other hang. The heap pressure in
`core:test` (mocks that keep their recorded calls) is not fixed here.
