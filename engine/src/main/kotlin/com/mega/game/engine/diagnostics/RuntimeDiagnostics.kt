package com.mega.game.engine.diagnostics

import com.badlogic.gdx.utils.Disposable
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Collects per-frame timing diagnostics and writes them to a file asynchronously.
 *
 * Usage:
 *   - Call [beginFrame] at the start of each render frame.
 *   - Call [beginEntry] / [endEntry] around any named work unit (nestable).
 *   - Call [endFrame] at the end of each render frame — this serializes the current
 *     frame's data to the output file and clears state for the next frame.
 *
 * Entries form a tree: any [beginEntry] called while another entry is open becomes
 * a child of that entry. Depth is unlimited.
 *
 * All file I/O is offloaded to a single background thread so the render thread is
 * not blocked.
 *
 * This class assumes that all diagnostics are run synchronously and sequentially.
 * This class is NOT thread safe!
 *
 * @param filePath path to the output file (relative to the working directory)
 */
class RuntimeDiagnostics(filePath: String) : Disposable {

    private class TimingNode(val name: String, val startNs: Long = System.nanoTime()) {
        var endNs: Long = -1L
        val children: MutableList<TimingNode> = mutableListOf()
        val durationMs: Double get() = (endNs - startNs) / 1_000_000.0
    }

    private val writer = BufferedWriter(FileWriter(File(filePath), false))
    private val executor = Executors.newSingleThreadExecutor()

    // Stack of in-progress entries on the current frame's render thread.
    private val stack = ArrayDeque<TimingNode>()

    // Completed top-level entries accumulated during the current frame.
    private val frameRoots = mutableListOf<TimingNode>()

    private var frameStartNs = 0L
    private var frameNumber = 0L
    private var disposed = false

    /** Mark the start of a new frame. Must be paired with [endFrame]. */
    fun beginFrame() {
        frameStartNs = System.nanoTime()
    }

    /**
     * Mark the end of the current frame. Serializes the frame's timing tree to the
     * output file (asynchronously) and resets all state for the next frame.
     */
    fun endFrame() {
        val frameDurationMs = (System.nanoTime() - frameStartNs) / 1_000_000.0

        val sb = StringBuilder()
        sb.appendLine("=== Frame #${frameNumber++} (${frameDurationMs.fmt()}ms) ===")
        frameRoots.forEach { appendNode(sb, it, 1) }

        // Capture the formatted string before clearing mutable state.
        val text = sb.toString()
        executor.submit {
            writer.write(text)
            writer.flush()
        }

        stack.clear()
        frameRoots.clear()
    }

    /**
     * Begin timing a named work unit. Nests inside any currently open entry.
     * Must be paired with [endEntry].
     */
    fun beginEntry(name: String) {
        stack.addLast(TimingNode(name))
    }

    /**
     * End the most recently opened entry. Its measured duration and any children
     * are attached to its parent entry (or promoted to a top-level root if there
     * is no parent).
     */
    fun endEntry() {
        if (stack.isEmpty()) return
        val node = stack.removeLast()
        node.endNs = System.nanoTime()
        if (stack.isEmpty()) frameRoots.add(node) else stack.last().children.add(node)
    }

    private fun appendNode(sb: StringBuilder, node: TimingNode, depth: Int) {
        val indent = "  ".repeat(depth)
        sb.appendLine("${indent}${node.name}: ${node.durationMs.fmt()}ms")
        node.children.forEach { appendNode(sb, it, depth + 1) }
    }

    private fun Double.fmt() = String.format("%.3f", this)

    override fun dispose() {
        if (disposed) return
        disposed = true
        executor.awaitTermination(2_000, TimeUnit.MILLISECONDS)
        executor.shutdown()
        writer.close()
    }
}
