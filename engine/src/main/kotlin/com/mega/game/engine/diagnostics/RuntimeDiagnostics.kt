package com.mega.game.engine.diagnostics

import com.badlogic.gdx.utils.Disposable
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Collects per-frame timing diagnostics and writes them to a file asynchronously.
 *
 * ## Usage
 *
 *   - Call [beginFrame] at the start of each render frame.
 *   - Call [beginEntry] / [endEntry] around any named work unit (nestable).
 *   - Call [endFrame] at the end of each render frame — this serializes the current
 *     frame's data to the output file and clears state for the next frame.
 *   - Call [dispose] once when shutting down, so pending frames reach the file.
 *
 * Entries form a tree: any [beginEntry] called while another entry is open becomes
 * a child of that entry. Depth is unlimited.
 *
 * ## How it fits together
 *
 * Two threads and a mailbox between them:
 *
 * ```
 * RENDER THREAD (the game)       WRITER THREAD (background)
 * ────────────────────────       ──────────────────────────
 * beginFrame()
 *   beginEntry("WorldSystem")    ┐
 *   endEntry()                   ├─ record into `nodes`
 *   …                            ┘
 * endFrame() ── build text ──▶ [ queue: 256 slots ] ──▶ writer.write(text)
 *                               (drop frame if full)    flush once the queue drains
 * ```
 *
 * The render thread never touches the file. It measures, turns the finished frame
 * into one string, drops that string in the mailbox, and moves on. The writer thread
 * only takes strings out of the mailbox and puts them on disk. The bounded [queue] is
 * the entire connection between the two.
 *
 * ## What is held in memory
 *
 * Four structures, all reused for the lifetime of the object:
 *
 *   - [nodes] — the [TimingNode] pool *and* the current frame's entries at once.
 *     [nodeCount] marks the split: `nodes[0 until nodeCount]` belongs to this frame,
 *     anything past that is a spare waiting to be handed out again.
 *   - [openIndices] / [openCount] — a stack of the entries that are currently open
 *     (started but not yet ended), innermost last.
 *   - [builder] — one [StringBuilder], cleared and refilled every frame.
 *   - [writer] — a buffered writer touched only by the writer thread, until shutdown.
 *
 * ## Life of a frame
 *
 * [beginFrame] just stamps a start time. [beginEntry] takes the next spare node,
 * records its name and its depth (which is simply how many entries are already open),
 * and pushes its index onto the open stack; the start time is stamped last so none of
 * that bookkeeping is charged to the entry. [endEntry] pops the innermost open index
 * and stamps the end time — an unbalanced call on an empty stack is ignored rather
 * than throwing.
 *
 * No tree is built along the way and nothing is appended to: a node's parent is
 * implied by its depth and its position in [nodes].
 *
 * ## Serializing a frame
 *
 * [endFrame] writes a header and then walks the frame's nodes in order. Because
 * entries always close innermost-first, the order they were opened in is already the
 * order a depth-first walk of the equivalent tree would print, so one forward loop
 * produces correctly nested output:
 *
 * ```
 * === Frame #41 (16.703ms) ===
 *   WorldSystem: 4.512ms
 *     cycle[1]: 1.998ms
 *   audioMan: 0.204ms
 * ```
 *
 * Indentation is `depth + 1` copies of two spaces. Entries left open when the frame
 * ended are skipped along with everything nested inside them — their duration was
 * never measured — which matches the older tree-based implementation, where an
 * unclosed parent took its children down with it.
 *
 * The finished string is handed to the queue with a non-blocking offer. If all slots
 * are full the frame is dropped and counted rather than stalling the game. Resetting
 * [nodeCount] and [openCount] to zero then frees every node at once; nothing is
 * deallocated, and the same instances are handed out again next frame.
 *
 * ## The writer thread
 *
 * It polls for work without waiting. When the mailbox comes up empty it has caught
 * up, so it flushes and then blocks until more arrives. While frames keep coming they
 * are written back to back into the buffer and not flushed at all. The result is
 * batched syscalls under load and nothing left unflushed while idle.
 *
 * If writing throws — a full disk, an IO error — the loop exits and the thread ends
 * quietly. The game does not hang: the mailbox simply fills up, and from then on every
 * frame is dropped and counted.
 *
 * ## Shutdown
 *
 * [dispose] puts a sentinel in the queue behind everything already pending, so queued
 * frames are written first, joins the writer thread, and only then writes the dropped
 * frame tally and closes the file — no race for the writer. It is idempotent, and the
 * writer thread is a daemon so a crashing game is never held open by it.
 *
 * ## Why it is written this way
 *
 * The render-thread cost is kept low by pooling timing nodes across frames, reusing a
 * single [StringBuilder], and formatting durations by hand — `String.format` is roughly
 * twenty times more expensive per value and is locale-sensitive, which would emit a
 * decimal comma in some locales and break downstream parsing. The bounded queue means
 * a stalled or failed writer can never balloon memory; dropped frames show up as gaps
 * in the frame numbering and are tallied at the end of the file on [dispose].
 *
 * This class assumes that all diagnostics are run synchronously and sequentially.
 * This class is NOT thread safe!
 *
 * @param filePath path to the output file (relative to the working directory)
 */
class RuntimeDiagnostics(filePath: String) : Disposable {

    companion object {
        const val TAG = "RuntimeDiagnostics"
        private const val QUEUE_CAPACITY = 256
        private const val WRITER_BUFFER_SIZE = 1 shl 16
        private const val SHUTDOWN_TIMEOUT_MS = 2_000L
        private const val INITIAL_NODE_CAPACITY = 128
        private const val INITIAL_BUILDER_CAPACITY = 8192

        // identity-compared sentinel telling the writer thread to finish up
        private val POISON_PILL = String("".toCharArray())
    }

    /**
     * One timed entry. Nodes are pooled and reused frame to frame, so instances are
     * mutable and are only valid for the frame in which they were obtained.
     *
     * The frame's nodes are held in one flat list in the order they were opened,
     * with [depth] recording nesting. Because entries obey stack discipline,
     * open-order is identical to the order a depth-first walk of the equivalent
     * tree would produce, so the serialized output is unchanged by the flattening.
     */
    private class TimingNode {
        var name: String = ""
        var depth: Int = 0
        var startNs: Long = 0L
        var endNs: Long = -1L
    }

    private val writer = BufferedWriter(FileWriter(File(filePath), false), WRITER_BUFFER_SIZE)
    private val queue = ArrayBlockingQueue<String>(QUEUE_CAPACITY)

    // Pooled nodes. Indices [0, nodeCount) are the current frame's entries, in open order.
    private val nodes = ArrayList<TimingNode>(INITIAL_NODE_CAPACITY)
    private var nodeCount = 0

    // Indices into [nodes] of the entries that are currently open, innermost last.
    private var openIndices = IntArray(32)
    // How many entries are currently open — "open" as in an open parenthesis: begun but
    // not yet ended. Rises and falls during the frame, and is also the depth of the next
    // entry to be opened, since being inside N open entries means being N levels deep.
    private var openCount = 0

    private val builder = StringBuilder(INITIAL_BUILDER_CAPACITY)

    private var frameStartNs = 0L
    private var frameNumber = 0L
    private var droppedFrames = 0
    private var disposed = false

    private val writerThread = Thread({ writeLoop() }, "$TAG-writer").apply {
        isDaemon = true
        start()
    }

    /** Mark the start of a new frame. Must be paired with [endFrame]. */
    fun beginFrame() {
        frameStartNs = System.nanoTime()
    }

    /**
     * Mark the end of the current frame. Serializes the frame's timing tree to the
     * output file (asynchronously) and resets all state for the next frame.
     *
     * If the writer thread has fallen behind, the frame is dropped instead of being
     * queued; the frame number is still consumed so the gap is visible in the output.
     */
    fun endFrame() {
        val frameDurationMs = (System.nanoTime() - frameStartNs) / 1_000_000.0

        builder.setLength(0)
        builder.append("=== Frame #").append(frameNumber++).append(" (")
        appendMillis(builder, frameDurationMs)
        builder.append("ms) ===").append('\n')

        // Entries left open when the frame ended are discarded along with everything
        // nested inside them: their duration was never measured.
        var skipDepth = -1
        for (i in 0 until nodeCount) {
            val node = nodes[i]

            if (skipDepth >= 0) {
                if (node.depth > skipDepth) continue
                skipDepth = -1
            }

            if (node.endNs < 0L) {
                skipDepth = node.depth
                continue
            }

            // `repeat` is inline over an Int, so this indents without allocating the range
            // and iterator that a `(0..depth).forEach` would create for every node
            repeat(node.depth + 1) { builder.append("  ") }
            builder.append(node.name).append(": ")
            appendMillis(builder, (node.endNs - node.startNs) / 1_000_000.0)
            builder.append("ms").append('\n')
        }

        if (!queue.offer(builder.toString())) droppedFrames++

        nodeCount = 0
        openCount = 0
    }

    /**
     * Begin timing a named work unit. Nests inside any currently open entry.
     * Must be paired with [endEntry].
     */
    fun beginEntry(name: String) {
        val node = obtainNode()
        node.name = name
        node.depth = openCount
        node.endNs = -1L

        if (openCount == openIndices.size) openIndices = openIndices.copyOf(openCount * 2)
        openIndices[openCount++] = nodeCount - 1

        // set last so that none of the bookkeeping above is counted against the entry
        node.startNs = System.nanoTime()
    }

    /**
     * End the most recently opened entry. Its measured duration and any children
     * are attached to its parent entry (or promoted to a top-level root if there
     * is no parent).
     */
    fun endEntry() {
        if (openCount == 0) return
        val endNs = System.nanoTime()
        nodes[openIndices[--openCount]].endNs = endNs
    }

    private fun obtainNode(): TimingNode {
        if (nodeCount == nodes.size) nodes.add(TimingNode())
        return nodes[nodeCount++]
    }

    /**
     * Appends [millis] with three decimal places, matching `String.format("%.3f", …)`
     * for the non-negative durations this class produces, but without allocating a
     * `Formatter` or consulting the default locale.
     */
    private fun appendMillis(sb: StringBuilder, millis: Double) {
        var value = millis
        if (value < 0.0) {
            sb.append('-')
            value = -value
        }

        val scaled = (value * 1000.0 + 0.5).toLong()
        sb.append(scaled / 1000L).append('.')

        val fraction = scaled % 1000L
        if (fraction < 100L) sb.append('0')
        if (fraction < 10L) sb.append('0')
        sb.append(fraction)
    }

    private fun writeLoop() {
        try {
            while (true) {
                // Flush only once the queue has drained, rather than once per frame:
                // buffered output is handed to the OS in large batches instead of one
                // write syscall per frame, and nothing sits unflushed while idle.
                var text = queue.poll()
                if (text == null) {
                    writer.flush()
                    text = queue.take()
                }

                if (text === POISON_PILL) break

                writer.write(text)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: Exception) {
            // The writer is dead; the bounded queue means the render thread will drop
            // frames from here on rather than accumulate them.
        } finally {
            try {
                writer.flush()
            } catch (_: Exception) {
            }
        }
    }

    override fun dispose() {
        if (disposed) return
        disposed = true

        try {
            // queued frames are consumed before the pill, so nothing already handed off is lost
            queue.offer(POISON_PILL, SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            writerThread.join(SHUTDOWN_TIMEOUT_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        try {
            if (droppedFrames > 0) {
                writer.write("=== Dropped $droppedFrames frames (writer could not keep up) ===\n")
            }
            writer.close()
        } catch (_: Exception) {
        }
    }
}
