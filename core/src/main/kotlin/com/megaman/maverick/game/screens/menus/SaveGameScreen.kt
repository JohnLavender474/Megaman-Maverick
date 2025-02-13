package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class SaveGameScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, SAVE), Initializable {

    companion object {
        const val TAG = "SaveScreen"
        private const val SAVE = "SAVE"
        private const val CONTINUE = "CONTINUE"
        private const val MAIN_MENU = "MAIN MENU"
    }

    private val fontHandles = Array<MegaFontHandle>()
    private lateinit var arrow: BlinkingArrow

    private var initialized = false

    private val out = Vector2()

    override fun init() {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init()")

        val saveFont = MegaFontHandle(
            text = SAVE,
            positionX = ConstVals.PPM.toFloat(),
            positionY = 3f * ConstVals.PPM,
            centerX = false,
            centerY = false,
        )
        fontHandles.add(saveFont)

        val continueFont = MegaFontHandle(
            text = CONTINUE,
            positionX = ConstVals.PPM.toFloat(),
            positionY = 2f * ConstVals.PPM,
            centerX = false,
            centerY = false
        )
        fontHandles.add(continueFont)

        val mainMenuFont = MegaFontHandle(
            text = MAIN_MENU,
            positionX = ConstVals.PPM.toFloat(),
            positionY = ConstVals.PPM.toFloat(),
            centerX = false,
            centerY = false
        )
        fontHandles.add(mainMenuFont)

        arrow = BlinkingArrow(game.assMan)

        buttons.put(SAVE, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MAIN_MENU
                Direction.DOWN -> CONTINUE
                else -> null
            }

            override fun onSelect(delta: Float): Boolean {
                game.saveState()
                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }
        })

        buttons.put(CONTINUE, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> SAVE
                Direction.DOWN -> MAIN_MENU
                else -> null
            }

            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name)
                return true
            }
        })

        buttons.put(MAIN_MENU, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> CONTINUE
                Direction.DOWN -> SAVE
                else -> null
            }

            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }
        })
    }

    override fun show() {
        if (!initialized) init()
        GameLogger.debug(TAG, "show()")
        super.show()
        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.MM2_PASSWORD_SCREEN_MUSIC)
    }

    override fun render(delta: Float) {
        super.render(delta)

        val font = if (currentButtonKey == CONTINUE) fontHandles[0] else fontHandles[1]
        val arrowPosition = font.getPosition(out)
        arrowPosition.x -= ConstVals.PPM.toFloat()
        arrow.x = arrowPosition.x
        arrow.y = arrowPosition.y
        arrow.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()
        fontHandles.forEach { it.draw(batch) }
        arrow.draw(batch)
        batch.end()
    }
}
