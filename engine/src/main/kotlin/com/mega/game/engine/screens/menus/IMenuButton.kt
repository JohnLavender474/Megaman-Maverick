package com.mega.game.engine.screens.menus

import com.mega.game.engine.common.enums.Direction


interface IMenuButton {


    fun onSelect(delta: Float): Boolean


    fun onNavigate(direction: Direction, delta: Float): String?
}
