package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.megaman.maverick.game.MegamanMaverickGame

class LoadPasswordScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, MAIN_MENU) {

    companion object {
        const val TAG = "LoadPasswordScreen"
        const val MAIN_MENU = "MAIN MENU"
        const val LOAD = "LOAD"
    }

    private val fontHandles = Array<BitmapFontHandle>()
}