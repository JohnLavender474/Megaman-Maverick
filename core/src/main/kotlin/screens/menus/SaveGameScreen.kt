package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.fonts.BitmapFontHandle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.SpriteMatrix
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.GamePasswords
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class SaveGameScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, SAVE), Initializable {

    companion object {
        const val TAG = "SaveScreen"
        private const val SAVE = "SAVE"
        private const val CONTINUE = "CONTINUE"
        private const val MAIN_MENU = "MAIN MENU"
        private const val FRAMES_X = 2f
        private const val FRAMES_Y = 2f
        private var frameRegion: TextureRegion? = null
        private var dotRegion: TextureRegion? = null
    }

    private val fontHandles = Array<BitmapFontHandle>()
    private lateinit var password: IntArray
    private lateinit var arrow: BlinkingArrow
    private lateinit var frames: SpriteMatrix
    private lateinit var dots: SpriteMatrix
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "Initializing...")

        if (frameRegion == null || dotRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
            frameRegion = atlas.findRegion("WhiteFrameBlackPane")
            dotRegion = atlas.findRegion("RedDot")
        }

        password = GamePasswords.getGamePassword(game.state)

        val saveFont = BitmapFontHandle(
            SAVE,
            getDefaultFontSize(),
            Vector2(1f * ConstVals.PPM, 3f * ConstVals.PPM),
            centerX = true,
            centerY = true,
            hidden = false,
            ConstVals.MEGAMAN_MAVERICK_FONT,
        )
        fontHandles.add(saveFont)

        val continueFont = BitmapFontHandle(
            CONTINUE,
            getDefaultFontSize(),
            Vector2(1f * ConstVals.PPM, 2f * ConstVals.PPM),
            centerX = true,
            centerY = true,
            hidden = false,
            ConstVals.MEGAMAN_MAVERICK_FONT,
        )
        fontHandles.add(continueFont)

        val mainMenuFont = BitmapFontHandle(
            MAIN_MENU,
            getDefaultFontSize(),
            Vector2(3f * ConstVals.PPM, 1f * ConstVals.PPM),
            centerX = true,
            centerY = true,
            hidden = false,
            ConstVals.MEGAMAN_MAVERICK_FONT,
        )
        fontHandles.add(mainMenuFont)

        arrow = BlinkingArrow(game.assMan)

        frames = SpriteMatrix(
            frameRegion!!,
            DrawingPriority(DrawingSection.FOREGROUND, 0),
            ConstVals.PPM.toFloat(),
            ConstVals.PPM.toFloat(),
            6,
            6
        )
        frames.setPosition(FRAMES_X * ConstVals.PPM, FRAMES_Y * ConstVals.PPM)

        dots = SpriteMatrix(
            dotRegion!!,
            DrawingPriority(DrawingSection.FOREGROUND, 0),
            ConstVals.PPM.toFloat(),
            ConstVals.PPM.toFloat(),
            6,
            6
        )
        dots.setPosition(FRAMES_X * ConstVals.PPM, FRAMES_Y * ConstVals.PPM)

        buttons.put(SAVE, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) =
                when (direction) {
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
            override fun onNavigate(direction: Direction, delta: Float) =
                when (direction) {
                    Direction.UP -> SAVE
                    Direction.DOWN -> MAIN_MENU
                    else -> null
                }

            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.BOSS_SELECT_SCREEN.name)
                return true
            }
        })

        buttons.put(MAIN_MENU, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) =
                when (direction) {
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
        GameLogger.debug(TAG, "Showing...")
        super.show()
        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.MM2_PASSWORD_SCREEN_MUSIC)
    }

    override fun render(delta: Float) {
        super.render(delta)

        for (x in 0 until 6) {
            for (y in 0 until 6) {
                val index = x + y * 6
                val dot = dots[x, y]
                dot?.hidden = password[index] == 0
            }
        }

        val arrowPosition = if (currentButtonKey == CONTINUE) fontHandles[0].position else fontHandles[1].position
        arrowPosition.x -= 1f * ConstVals.PPM
        arrow.position = arrowPosition
        arrow.update(delta)

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()
        frames.draw(batch)
        dots.draw(batch)
        fontHandles.forEach { it.draw(batch) }
        arrow.draw(batch)
        batch.end()
    }
}
