package com.megaman.lostmemories.screens

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class LostMemoriesSplashScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    companion object {
        const val TAG = "LostMemoriesSplashScreen"
        private const val MEGA_MAN_X = "MEGA MAN X"
        private const val LOST_MEMORIES = "LOST MEMORIES"
        private const val PRESS_START = "PRESS START"
        private const val PRESS_START_BLINK = 0.25f
    }

    private val fontHandles = OrderedMap<String, MegaFontHandle>()

    private val pressStartBlinkTimer = Timer(PRESS_START_BLINK)
    private var pressStartBlink = true

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        val centerX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
        val centerY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f

        fontHandles.put(
            MEGA_MAN_X,
            MegaFontHandle(
                text = MEGA_MAN_X,
                positionX = centerX,
                positionY = centerY + ConstVals.PPM,
                centerX = true,
                centerY = true
            )
        )
        fontHandles.put(
            LOST_MEMORIES,
            MegaFontHandle(
                text = LOST_MEMORIES,
                positionX = centerX,
                positionY = centerY,
                centerX = true,
                centerY = true
            )
        )
        fontHandles.put(
            PRESS_START,
            MegaFontHandle(
                text = PRESS_START,
                positionX = centerX,
                positionY = centerY - 3 * ConstVals.PPM,
                centerX = true,
                centerY = true
            )
        )
    }

    override fun show() {
        if (!initialized) init()
        super.show()

        game.getUiCamera().setToDefaultPosition()
        game.audio.stopMusic()

        pressStartBlinkTimer.reset()
        pressStartBlink = true
    }

    override fun render(delta: Float) {
        super.render(delta)

        pressStartBlinkTimer.update(delta)
        if (pressStartBlinkTimer.isFinished()) {
            pressStartBlink = !pressStartBlink
            pressStartBlinkTimer.reset()
        }

        if (game.players.menuInput.isJustReleased(MegaControllerButton.START))
            game.startExtraLevel(LevelDefinition.X6_INTRO)
    }

    override fun draw(drawer: Batch) {
        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        fontHandles[MEGA_MAN_X].draw(drawer)
        fontHandles[LOST_MEMORIES].draw(drawer)
        if (pressStartBlink) fontHandles[PRESS_START].draw(drawer)

        batch.end()
    }
}
