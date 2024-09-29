package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.screens.menus.AbstractMenuScreen
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButtons

abstract class MegaMenuScreen(
    protected val game: MegamanMaverickGame,
    firstButtonKey: String,
    buttons: ObjectMap<String, IMenuButton> = ObjectMap()
) : AbstractMenuScreen(buttons, { game.paused }, firstButtonKey) {

    override fun getNavigationDirection() =
        if (game.controllerPoller.isJustPressed(MegaControllerButtons.UP)) Direction.UP
        else if (game.controllerPoller.isJustPressed(MegaControllerButtons.DOWN))
            Direction.DOWN
        else if (game.controllerPoller.isJustPressed(MegaControllerButtons.LEFT))
            Direction.LEFT
        else if (game.controllerPoller.isJustPressed(MegaControllerButtons.RIGHT))
            Direction.RIGHT
        else null

    override fun selectionRequested() = game.controllerPoller.isAnyJustPressed(
        gdxArrayOf(
            MegaControllerButtons.START,
            MegaControllerButtons.A
        )
    )

    protected open fun undoSelection() {
        selectionMade = false
    }

    override fun pause() {
        super.pause()
        game.audioMan.pauseAllSound()
        game.audioMan.pauseMusic()
    }

    override fun resume() {
        super.resume()
        game.audioMan.resumeAllSound()
        game.audioMan.playMusic()
    }

    override fun reset() {
        super.reset()
        game.audioMan.stopMusic()
    }
}
