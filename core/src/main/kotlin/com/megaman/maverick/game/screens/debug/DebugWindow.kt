package com.megaman.maverick.game.screens.debug

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea

class DebugWindow : Disposable {

    companion object {
        const val TAG = "DebugWindow"
        private const val MAX_LOGS = 200
    }

    private val frame = JFrame("Debug Window")
    private val logs = Queue<String>()
    private val textArea: JTextArea

    init {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)
        frame.minimumSize = Dimension(400, 300)
        frame.setSize(800, 600)

        textArea = JTextArea()
        textArea.lineWrap = true
        textArea.isEditable = false
        val scrollPane = JScrollPane(textArea)
        frame.add(scrollPane, BorderLayout.CENTER)

        frame.isVisible = true
    }

    fun log(message: String?) {
        logs.addLast(message)
        if (logs.size > MAX_LOGS) logs.removeFirst()

        frame.title = "$TAG logs=${logs.size}"

        updateDisplay()

        textArea.setCaretPosition(textArea.document.length)
    }

    private fun updateDisplay() {
        val s = StringBuilder()
        logs.forEach { log -> s.append(log).append("\n") }
        textArea.text = s.toString()
    }

    override fun dispose() = frame.dispose()
}
