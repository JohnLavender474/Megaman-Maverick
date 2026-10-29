package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class SaveGameScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, SAVE), Initializable {

    companion object {
        const val TAG = "SaveScreen"
        private const val SAVE = "SAVE"
        private const val CONTINUE = "CONTINUE"
        private const val EXTRAS_MENU = "EXTRAS MENU"
        private const val MAIN_MENU = "MAIN MENU"
        private const val BKG_REG_KEY = "menu_screen_bkg_v2"
        private const val TEXT_ROW_START = 9f
        private const val TEXT_X = 5f
        private const val ARROW_OFFSET_X = -0.5f
    }

    private val fontHandles = OrderedMap<String, MegaFontHandle>()
    private val arrows = OrderedMap<String, BlinkingArrow>()
    private val backgroundSprite = GameSprite()

    private val mainlineKeys = gdxArrayOf(SAVE, CONTINUE, MAIN_MENU)
    private val extrasKeys = gdxArrayOf(CONTINUE, EXTRAS_MENU, MAIN_MENU)
    private var activeKeys = mainlineKeys

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init()")

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val backgroundRegion = uiAtlas.findRegion(BKG_REG_KEY)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        // the rows are assigned in show() because the active keys change with the level type
        gdxArrayOf(SAVE, CONTINUE, EXTRAS_MENU, MAIN_MENU).forEach { text ->
            val fontHandle = MegaFontHandle(
                text = text,
                positionX = TEXT_X * ConstVals.PPM,
                positionY = 0f,
                centerX = false,
                centerY = false,
            )
            fontHandles.put(text, fontHandle)
            arrows.put(text, BlinkingArrow(game.assMan))
        }

        buttons.put(SAVE, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(SAVE, direction)

            override fun onSelect(delta: Float): Boolean {
                game.saveState()
                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }
        })

        buttons.put(CONTINUE, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(CONTINUE, direction)

            override fun onSelect(delta: Float): Boolean {
                when {
                    game.levelState.isInExtraLevel -> game.startExtraLevel(game.levelState.currentLevel!!)
                    game.state.isLevelDefeated(LevelDefinition.INTRO_STAGE) ->
                        game.setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name)
                    else -> {
                        game.levelState.currentLevel = LevelDefinition.INTRO_STAGE
                        game.startLevel()
                    }
                }

                return true
            }
        })

        buttons.put(EXTRAS_MENU, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(EXTRAS_MENU, direction)

            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.EXTRAS_SCREEN.name)
                return true
            }
        })

        buttons.put(MAIN_MENU, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = navigate(MAIN_MENU, direction)

            override fun onSelect(delta: Float): Boolean {
                if (game.levelState.isInExtraLevel) game.resetPlayerProgress()
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }
        })
    }

    private fun navigate(key: String, direction: Direction): String? {
        val index = activeKeys.indexOf(key, false)
        return when (direction) {
            Direction.UP -> activeKeys[(index - 1 + activeKeys.size) % activeKeys.size]
            Direction.DOWN -> activeKeys[(index + 1) % activeKeys.size]
            else -> getCurrentButtonKey()
        }
    }

    override fun show() {
        if (!initialized) init()
        GameLogger.debug(TAG, "show()")

        activeKeys = if (game.levelState.isInExtraLevel) extrasKeys else mainlineKeys
        firstButtonKey = activeKeys.first()

        var row = TEXT_ROW_START
        activeKeys.forEach { key ->
            fontHandles[key].positionY = row * ConstVals.PPM

            val arrow = arrows[key]
            arrow.centerX = (TEXT_X + ARROW_OFFSET_X) * ConstVals.PPM
            arrow.centerY = (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        super.show()
        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.VINNYZ_PASSWORD_MUSIC)
    }

    override fun render(delta: Float) {
        super.render(delta)

        val arrow = arrows[buttonKey]
        arrow.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        backgroundSprite.draw(batch)
        activeKeys.forEach { fontHandles[it].draw(batch) }
        arrow.draw(batch)

        batch.end()
    }

    override fun onAnyMovement(direction: Direction) {
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }
}
