package com.megaman.maverick.game.controllers

enum class SelectButtonAction(val text: String) {

    NEXT_WEAPON("NEXT WEAPON"), AIR_DASH("AIR DASH"), START("START");

    fun next(): SelectButtonAction {
        var index = ordinal + 1
        if (index >= SelectButtonAction.entries.size) index = 0
        return SelectButtonAction.entries[index]
    }

    fun previous(): SelectButtonAction {
        var index = ordinal - 1
        if (index < 0) index = SelectButtonAction.entries.size - 1
        return SelectButtonAction.entries[index]
    }
}
