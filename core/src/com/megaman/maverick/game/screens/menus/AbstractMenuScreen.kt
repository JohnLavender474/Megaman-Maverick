package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.enums.Direction
import com.engine.screens.BaseScreen
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractMenuScreen(game: MegamanMaverickGame, protected val firstButtonKey: String) :
    BaseScreen(game) {

    protected val castGame: MegamanMaverickGame = game

    protected abstract val menuButtons: ObjectMap<String, IMenuButton>

    var currentButtonKey = firstButtonKey

    protected var selectionMade = false
        private set

    protected open fun onAnyMovement() {
        // do nothing
    }

    protected open fun onAnySelection() = true

    override fun show() {
        selectionMade = false
        currentButtonKey = firstButtonKey
    }

    override fun render(delta: Float) {
        if (selectionMade || game.paused) return

        val menuButton = menuButtons.get(currentButtonKey)
        menuButton?.let {
            val direction =
                if (game.controllerPoller.isJustPressed(ControllerButton.UP)) Direction.UP
                else if (game.controllerPoller.isJustPressed(ControllerButton.DOWN))
                    Direction.DOWN
                else if (game.controllerPoller.isJustPressed(ControllerButton.LEFT))
                    Direction.LEFT
                else if (game.controllerPoller.isJustPressed(ControllerButton.RIGHT))
                    Direction.RIGHT
                else null

            direction?.let { d ->
                onAnyMovement()
                menuButton.onNavigate(d, delta)?.let { currentButtonKey = it }
            }

            if (game.controllerPoller.isJustPressed(ControllerButton.START) ||
                game.controllerPoller.isJustPressed(ControllerButton.A)
            )
                if (onAnySelection()) selectionMade = menuButton.onSelect(delta)
        }
    }

    override fun pause() {
        castGame.audioMan.pauseAllSound()
        castGame.audioMan.pauseMusic()
    }

    override fun resume() {
        castGame.audioMan.resumeAllSound()
        castGame.audioMan.playMusic()
    }

    override fun dispose() = castGame.audioMan.stopMusic()
}
