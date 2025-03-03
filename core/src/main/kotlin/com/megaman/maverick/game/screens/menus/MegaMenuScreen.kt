package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.screens.menus.IMenuButton
import com.mega.game.engine.screens.menus.StandardMenuScreen
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton

abstract class MegaMenuScreen(
    protected val game: MegamanMaverickGame,
    firstButtonKey: String? = null,
    buttons: ObjectMap<String, IMenuButton> = ObjectMap(),
    var pauseMusicOnPause: Boolean = true,
    var playMusicOnResume: Boolean = true
) : StandardMenuScreen(buttons, firstButtonKey) {

    protected val selectionButtons = gdxArrayOf<Any>(MegaControllerButton.START, MegaControllerButton.A)

    override fun getNavigationDirection() = game.controllerPoller.let {
        when {
            it.isJustReleased(MegaControllerButton.UP) -> Direction.UP
            it.isJustReleased(MegaControllerButton.DOWN) -> Direction.DOWN
            it.isJustReleased(MegaControllerButton.LEFT) -> Direction.LEFT
            it.isJustReleased(MegaControllerButton.RIGHT) -> Direction.RIGHT
            else -> null
        }
    }

    override fun selectionRequested() = game.controllerPoller.isAnyJustReleased(selectionButtons)

    protected open fun undoSelection() {
        selectionMade = false
    }

    override fun pause() {
        super.pause()
        if (pauseMusicOnPause) game.audioMan.pauseMusic()
    }

    override fun resume() {
        super.resume()
        if (playMusicOnResume) game.audioMan.playMusic()
    }
}
