package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.MegamanMaverickGame

class LoadPasswordScreen(game: MegamanMaverickGame) : AbstractMenuScreen(game, MAIN_MENU) {

    companion object {
        const val TAG = "LoadPasswordScreen"
        const val MAIN_MENU = "MAIN MENU"
        const val LOAD = "LOAD"
    }

    override val menuButtons = ObjectMap<String, IMenuButton>()
    override val eventKeyMask = ObjectSet<Any>()

    override fun init() {
        // TODO: blinking arrow should disappear when cursor is on the password table
    }
}