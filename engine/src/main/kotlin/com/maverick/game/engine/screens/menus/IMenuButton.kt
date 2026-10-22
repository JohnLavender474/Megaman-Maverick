package com.maverick.game.engine.screens.menus

import com.maverick.game.engine.common.enums.Direction

interface IMenuButton {

    fun onSelect(delta: Float): Boolean

    fun onNavigate(direction: Direction, delta: Float): String?
}
