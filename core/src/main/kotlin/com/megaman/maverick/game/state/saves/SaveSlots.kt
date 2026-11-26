package com.megaman.maverick.game.state.saves

import com.maverick.game.engine.common.GameLogger

class SaveSlots(private val storage: SaveStorage) {

    companion object {
        const val TAG = "SaveSlots"
        const val SLOT_COUNT = 5
    }

    fun read(slot: Int): SaveSlotContent {
        val text = storage.read(fileName(slot)) ?: return SaveSlotContent.Empty
        return SaveFileJson.read(text)
    }

    fun write(slot: Int, save: SaveFile): Boolean {
        val current = read(slot)
        if (current is SaveSlotContent.NewerVersion) {
            GameLogger.error(TAG, "write(): slot $slot holds version ${current.version}, which is newer than this game")
            return false
        }

        storage.write(fileName(slot), SaveFileJson.write(save))
        return true
    }

    private fun fileName(slot: Int): String {
        require(slot in 1..SLOT_COUNT) { "Save slot $slot does not exist; the slots are 1 to $SLOT_COUNT" }
        return "slot-$slot.json"
    }
}
