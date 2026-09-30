package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
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
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

/**
 * Shows a back button and one button for each level of type [LevelType.EXTRA_LEVEL]. When the player selects a level,
 * the level's loadout override replaces the player's progress in memory and the level starts.
 */
class ExtrasScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "ExtrasScreen"
        private const val BACK = "BACK"
        private const val BKG_REG_KEY = "menu_screen_bkg_v2"
        private const val TEXT_ROW_START = 11f
        private const val TEXT_X = 3f
        private const val ARROW_OFFSET_X = -0.5f
    }

    private val keys = Array<String>()
    private val fontHandles = OrderedMap<String, MegaFontHandle>()
    private val arrows = OrderedMap<String, BlinkingArrow>()
    private val backgroundSprite = GameSprite()

    private var initialized = false

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        GameLogger.debug(TAG, "init()")

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val backgroundRegion = uiAtlas.findRegion(BKG_REG_KEY)
        backgroundSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        backgroundSprite.setRegion(backgroundRegion)

        keys.add(BACK)
        val levels = OrderedMap<String, LevelDefinition>()
        LevelDefinition.entries.filter { it.type == LevelType.EXTRA_LEVEL }.forEach { level ->
            val key = level.getFormattedName()
            keys.add(key)
            levels.put(key, level)
        }

        var row = TEXT_ROW_START
        keys.forEach { key ->
            fontHandles.put(
                key, MegaFontHandle(
                    text = key,
                    positionX = TEXT_X * ConstVals.PPM,
                    positionY = row * ConstVals.PPM,
                    centerX = false,
                    centerY = false,
                )
            )

            val arrowCenter = Vector2(
                TEXT_X + ARROW_OFFSET_X,
                row - ConstVals.ARROW_CENTER_ROW_DECREMENT
            ).scl(ConstVals.PPM.toFloat())
            arrows.put(key, BlinkingArrow(game.assMan, arrowCenter))

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        keys.forEachIndexed { index, key ->
            buttons.put(key, object : IMenuButton {

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> keys[(index - 1 + keys.size) % keys.size]
                    Direction.DOWN -> keys[(index + 1) % keys.size]
                    else -> getCurrentButtonKey()
                }

                override fun onSelect(delta: Float): Boolean {
                    game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

                    if (key == BACK) {
                        game.resetPlayerProgress()
                        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                    } else game.startExtraLevel(levels[key])

                    return true
                }
            })
        }
    }

    override fun show() {
        if (!initialized) init()
        GameLogger.debug(TAG, "show()")
        super.show()
        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MusicAsset.VINNYZ_MAIN_MENU_MUSIC, false)
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

    override fun onAnyMovement(direction: Direction) {
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }
}
