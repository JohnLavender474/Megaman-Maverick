package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.files.FileHandle
import com.maverick.game.engine.common.GameLogger

class FileSaveStorage(private val folder: FileHandle) : SaveStorage {

    companion object {
        const val TAG = "FileSaveStorage"
        const val TEMP_SUFFIX = ".tmp"
        const val BACKUP_SUFFIX = ".bak"
        private const val CHARSET = "UTF-8"
    }

    override fun read(name: String): String? {
        val file = folder.child(name)
        if (file.exists()) return file.readString(CHARSET)

        val backup = folder.child(name + BACKUP_SUFFIX)
        if (!backup.exists()) return null

        GameLogger.error(TAG, "read(): $name is missing, so its backup ${backup.name()} is read")
        return backup.readString(CHARSET)
    }

    override fun write(name: String, text: String) {
        folder.mkdirs()
        val file = folder.child(name)
        val temp = folder.child(name + TEMP_SUFFIX)
        val backup = folder.child(name + BACKUP_SUFFIX)
        temp.writeString(text, false, CHARSET)

        if (file.exists()) {
            backup.delete()
            file.moveTo(backup)
        }

        temp.moveTo(file)
    }
}
