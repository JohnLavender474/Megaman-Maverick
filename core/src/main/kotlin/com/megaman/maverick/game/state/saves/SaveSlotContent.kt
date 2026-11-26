package com.megaman.maverick.game.state.saves

sealed interface SaveSlotContent {

    data object Empty : SaveSlotContent

    class Used(val save: SaveFile) : SaveSlotContent

    class NewerVersion(val version: Long) : SaveSlotContent

    class Unreadable(val reason: String) : SaveSlotContent
}
