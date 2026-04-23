package com.mega.game.engine.diagnostics

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

class RuntimeDiagnosticsTest : DescribeSpec({

    lateinit var tempFile: File
    lateinit var diagnostics: RuntimeDiagnostics

    beforeEach {
        tempFile = File.createTempFile("test-diagnostics", ".txt")
        tempFile.deleteOnExit()
        diagnostics = RuntimeDiagnostics(tempFile.absolutePath)
    }

    // dispose() is idempotent, so calling it here is safe even if a test already called it
    afterEach {
        diagnostics.dispose()
        tempFile.delete()
    }

    // Flush pending async writes and return the full file content.
    fun flush(): String {
        diagnostics.dispose()
        return tempFile.readText()
    }

    describe("file creation") {

        it("creates the output file at the given path on construction") {
            // then
            tempFile.exists() shouldBe true
        }
    }

    describe("frame headers") {

        it("writes a frame header containing the frame number and total time") {
            // when
            diagnostics.beginFrame()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "=== Frame #0 ("
            output shouldContain "ms) ==="
        }

        it("increments the frame number for each call to endFrame") {
            // when
            diagnostics.beginFrame(); diagnostics.endFrame()
            diagnostics.beginFrame(); diagnostics.endFrame()
            diagnostics.beginFrame(); diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "=== Frame #0 ("
            output shouldContain "=== Frame #1 ("
            output shouldContain "=== Frame #2 ("
        }

        it("records a non-negative total frame time") {
            // when
            diagnostics.beginFrame()
            Thread.sleep(1)
            diagnostics.endFrame()

            // then — extract the ms value from "=== Frame #0 (X.XXXms) ==="
            val output = flush()
            val ms = output
                .substringAfter("=== Frame #0 (")
                .substringBefore("ms) ===")
                .toDouble()
            (ms >= 0.0) shouldBe true
        }

        it("writes an empty frame with no entries when no entries are added") {
            // when
            diagnostics.beginFrame()
            diagnostics.endFrame()

            // then — header present, no indented entry lines
            val output = flush()
            output shouldContain "=== Frame #0 ("
            output shouldNotContain "  " // no indented entries
        }
    }

    describe("root-level entries") {

        it("writes a root entry with the correct name and indentation") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("WorldSystem")
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "  WorldSystem:"
        }

        it("writes multiple root entries in the order they were added") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("SystemA"); diagnostics.endEntry()
            diagnostics.beginEntry("SystemB"); diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "  SystemA:"
            output shouldContain "  SystemB:"
            // order: SystemA appears before SystemB
            (output.indexOf("  SystemA:") < output.indexOf("  SystemB:")) shouldBe true
        }

        it("records a non-negative duration for a root entry") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("WorldSystem")
            Thread.sleep(1)
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            val ms = output
                .lines()
                .first { it.trim().startsWith("WorldSystem:") }
                .trim()
                .removePrefix("WorldSystem:")
                .trim()
                .removeSuffix("ms")
                .toDouble()
            (ms >= 0.0) shouldBe true
        }
    }

    describe("entry nesting") {

        it("writes a child entry indented one level deeper than its parent") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("WorldSystem")
            diagnostics.beginEntry("buildBodyArray")
            diagnostics.endEntry()
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "  WorldSystem:"     // depth 1 — two spaces
            output shouldContain "    buildBodyArray:" // depth 2 — four spaces
        }

        it("writes a grandchild entry indented two levels deeper than the root") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("WorldSystem")
            diagnostics.beginEntry("cycle[1]")
            diagnostics.beginEntry("preProcess")
            diagnostics.endEntry()
            diagnostics.endEntry()
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "  WorldSystem:"
            output shouldContain "    cycle[1]:"
            output shouldContain "      preProcess:"
        }

        it("writes multiple sibling children under a single parent") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("WorldSystem")
            diagnostics.beginEntry("buildBodyArray");     diagnostics.endEntry()
            diagnostics.beginEntry("cycle[1]");           diagnostics.endEntry()
            diagnostics.beginEntry("updateWorldContainer"); diagnostics.endEntry()
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            output shouldContain "    buildBodyArray:"
            output shouldContain "    cycle[1]:"
            output shouldContain "    updateWorldContainer:"
        }

        it("correctly closes back to root level after a nested sequence ends") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("ParentA")
            diagnostics.beginEntry("Child")
            diagnostics.endEntry() // close Child
            diagnostics.endEntry() // close ParentA
            diagnostics.beginEntry("ParentB")
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then — ParentB should be a root entry, not a child of ParentA
            val output = flush()
            output shouldContain "  ParentA:"
            output shouldContain "  ParentB:"
            // ParentB must be at root indent (two spaces), not child indent (four)
            output shouldNotContain "    ParentB:"
        }
    }

    describe("endEntry on empty stack") {

        it("is a no-op and does not throw") {
            // when
            diagnostics.beginFrame()
            diagnostics.endEntry() // stack is empty — should not throw
            diagnostics.endFrame()

            // then — frame header still written normally
            flush() shouldContain "=== Frame #0 ("
        }

        it("does not add a spurious root entry") {
            // when
            diagnostics.beginFrame()
            diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            // Only the header line; no indented entry lines
            output.lines().filter { it.startsWith("  ") } shouldBe emptyList()
        }
    }

    describe("frame isolation") {

        it("does not carry root entries from one frame into the next") {
            // when
            diagnostics.beginFrame()
            diagnostics.beginEntry("SystemA"); diagnostics.endEntry()
            diagnostics.endFrame()

            diagnostics.beginFrame()
            diagnostics.beginEntry("SystemB"); diagnostics.endEntry()
            diagnostics.endFrame()

            // then
            val output = flush()
            val frame0Block = output
                .substringAfter("=== Frame #0 (")
                .substringBefore("=== Frame #1 (")
            val frame1Block = output
                .substringAfter("=== Frame #1 (")

            frame0Block shouldContain "SystemA"
            frame0Block shouldNotContain "SystemB"
            frame1Block shouldContain "SystemB"
            frame1Block shouldNotContain "SystemA"
        }

        it("discards open entries left on the stack when endFrame is called") {
            // when — intentionally omit endEntry for "Unclosed"
            diagnostics.beginFrame()
            diagnostics.beginEntry("Unclosed")
            // no endEntry
            diagnostics.endFrame()

            // then — unclosed entry was never promoted to frameRoots, so it is absent
            val output = flush()
            output shouldNotContain "Unclosed"
        }
    }

    describe("dispose") {

        it("a second call to dispose is a no-op and does not throw") {
            // when / then
            diagnostics.dispose()
            diagnostics.dispose() // must not throw
        }

        it("flushes pending writes before shutting down so output is readable") {
            // when
            repeat(5) {
                diagnostics.beginFrame()
                diagnostics.beginEntry("WorldSystem"); diagnostics.endEntry()
                diagnostics.endFrame()
            }

            // then — dispose waits for the executor; all 5 frames must be present
            val output = flush()
            (0..4).forEach { i -> output shouldContain "=== Frame #$i (" }
        }
    }
})
