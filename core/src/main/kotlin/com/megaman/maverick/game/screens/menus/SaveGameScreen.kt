package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.menus.IMenuButton
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
        private const val MAIN_MENU = "MAIN MENU"
        private const val TEXT_ROW_START = 9f
        private const val TEXT_X = 5f
        private const val ARROW_OFFSET_X = -0.5f
    }

    private val fontHandles = OrderedMap<String, MegaFontHandle>()
    private val arrows = OrderedMap<String, BlinkingArrow>()
    private val backgroundSprite = GameSprite()

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init()")

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val backgroundRegion = uiAtlas.findRegion("menu_screen_bkg")
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        var row = TEXT_ROW_START
        gdxArrayOf(SAVE, CONTINUE, MAIN_MENU).forEach { text ->
            val fontHandle = MegaFontHandle(
                text = text,
                positionX = TEXT_X * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false,
                centerY = false,
            )
            fontHandles.put(text, fontHandle)

            val arrowCenter = Vector2(
                TEXT_X + ARROW_OFFSET_X,
                row - ConstVals.ARROW_CENTER_ROW_DECREMENT
            ).scl(ConstVals.PPM.toFloat())
            arrows.put(text, BlinkingArrow(game.assMan, arrowCenter))

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        buttons.put(SAVE, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MAIN_MENU
                Direction.DOWN -> CONTINUE
                else -> getCurrentButtonKey()
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
                else -> getCurrentButtonKey()
            }

            override fun onSelect(delta: Float): Boolean {
                when {
                    game.state.isLevelDefeated(LevelDefinition.INTRO_STAGE) ->
                        game.setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name)
                    else -> {
                        game.setCurrentLevel(LevelDefinition.INTRO_STAGE)
                        game.startLevel()
                    }
                }

                return true
            }
        })

        buttons.put(MAIN_MENU, object : IMenuButton {

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> CONTINUE
                Direction.DOWN -> SAVE
                else -> getCurrentButtonKey()
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

        val arrow = arrows[buttonKey]
        arrow.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        backgroundSprite.draw(batch)
        fontHandles.values().forEach { it.draw(batch) }
        arrow.draw(batch)

        batch.end()
    }
}
