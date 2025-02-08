package com.megaman.maverick.game.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import com.mega.game.engine.common.interfaces.Initializable
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AsyncFileWriter(filePath: String) : Initializable, Disposable {

    companion object {
        private const val START_LOG = "===== SESSION STARTED: %s ====="
    }

    private val executorService = Executors.newSingleThreadExecutor()
    private val fileHandle = Gdx.files.local(filePath)

    override fun init() {
        val startLog = String.format(START_LOG, LocalDateTime.now().toString())
        write(startLog)
    }

    fun write(text: String) {
        try {
            executorService.submit { fileHandle.writeString("$text\n", true) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dispose() {
        executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)
        executorService.shutdown()
    }
}
