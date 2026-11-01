package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.screens.menus.IMenuButton
import com.maverick.game.engine.screens.menus.StandardMenuScreen
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.input.SystemButton

abstract class MegaMenuScreen(
    protected val game: MegamanMaverickGame,
    firstButtonKey: String? = null,
    buttons: ObjectMap<String, IMenuButton> = ObjectMap(),
    var pauseMusicOnPause: Boolean = true,
    var playMusicOnResume: Boolean = true
) : StandardMenuScreen(buttons, firstButtonKey) {

    override fun getNavigationDirection() = game.players.menuInput.let {
        when {
            it.isJustReleased(MegaControllerButton.UP) -> Direction.UP
            it.isJustReleased(MegaControllerButton.DOWN) -> Direction.DOWN
            it.isJustReleased(MegaControllerButton.LEFT) -> Direction.LEFT
            it.isJustReleased(MegaControllerButton.RIGHT) -> Direction.RIGHT
            else -> null
        }
    }

    override fun selectionRequested() = game.players.menuInput.let {
        it.isJustReleased(MegaControllerButton.START) || it.isJustReleased(MegaControllerButton.A)
    }

    override fun backRequested() = game.systemInput.isJustPressed(SystemButton.BACK)

    protected open fun undoSelection() {
        selectionMade = false
    }

    override fun pause() {
        super.pause()
        if (pauseMusicOnPause) game.audio.pauseMusic()
    }

    override fun resume() {
        super.resume()
        if (playMusicOnResume) game.audio.playMusic()
    }
}
