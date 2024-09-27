package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.setToDefaultPosition

class SimpleSelectLevelScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, BETA), Initializable {

    companion object {
        const val TAG = "SimpleSelectLevelScreen"
        const val BETA = "BETA"
        const val ALPHA = "ALPHA"
        const val TEST = "TEST"
        private val LEVELS = objectMapOf(
            BETA pairTo gdxArrayOf(
                Level.INFERNO_MAN,
                Level.MOON_MAN,
                Level.DESERT_MAN,
                Level.REACTOR_MAN,
                Level.GLACIER_MAN
            ),
            ALPHA pairTo gdxArrayOf(
                Level.TIMBER_WOMAN,
                Level.MAGNET_MAN,
                Level.WILY_STAGE_1,
                Level.WILY_STAGE_2,
                Level.WILY_STAGE_3
            ),
            TEST pairTo gdxArrayOf(
                Level.TEST1,
                Level.TEST2
            )
        )
        private const val SECTION_Y = 12f
        private val SECTION_X_ARRAY = gdxArrayOf(2f, 6f, 10f)
        private const val LEVEL_X = 8f
        private const val LEVEL_Y = 4f
    }

    private val sections = Array<MegaFontHandle>()
    private val levelArrows = Array<BlinkingArrow>()
    private lateinit var sectionArrow: BlinkingArrow
    private lateinit var level: MegaFontHandle
    private var initialized = false
    private var index = 0

    override fun init() {
        if (initialized) return
        initialized = true

        index = 0

        buttons.put(BETA, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.LEFT -> TEST
                Direction.RIGHT -> ALPHA
                else -> currentButtonKey
            }

            override fun onSelect(delta: Float): Boolean {
                game.startLevelScreen(LEVELS[BETA][index])
                return true
            }
        })
        buttons.put(ALPHA, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.LEFT -> BETA
                Direction.RIGHT -> TEST
                else -> currentButtonKey
            }

            override fun onSelect(delta: Float): Boolean {
                game.startLevelScreen(LEVELS[ALPHA][index])
                return true
            }
        })
        buttons.put(TEST, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.LEFT -> ALPHA
                Direction.RIGHT -> BETA
                else -> currentButtonKey
            }

            override fun onSelect(delta: Float): Boolean {
                game.startLevelScreen(LEVELS[TEST][index])
                return true
            }
        })

        sections.add(
            MegaFontHandle(
                { BETA },
                positionX = SECTION_X_ARRAY[0] * ConstVals.PPM,
                positionY = SECTION_Y * ConstVals.PPM
            )
        )
        sections.add(
            MegaFontHandle(
                { ALPHA },
                positionX = SECTION_X_ARRAY[1] * ConstVals.PPM,
                positionY = SECTION_Y * ConstVals.PPM
            )
        )
        sections.add(
            MegaFontHandle(
                { TEST },
                positionX = SECTION_X_ARRAY[2] * ConstVals.PPM,
                positionY = SECTION_Y * ConstVals.PPM
            )
        )

        sectionArrow = BlinkingArrow(game.assMan)

        levelArrows.add(
            BlinkingArrow(
                game.assMan,
                Vector2(LEVEL_X * ConstVals.PPM, (LEVEL_Y - 1.5f) * ConstVals.PPM),
                270f
            )
        )
        levelArrows.add(
            BlinkingArrow(
                game.assMan,
                Vector2(LEVEL_X * ConstVals.PPM, (LEVEL_Y + 0.5f) * ConstVals.PPM),
                90f
            )
        )

        level = MegaFontHandle(
            { LEVELS[currentButtonKey][index].name },
            positionX = LEVEL_X * ConstVals.PPM,
            positionY = LEVEL_Y * ConstVals.PPM
        )
    }

    override fun onAnyMovement(direction: Direction) {
        game.audioMan.playSound(SoundAsset.BLOOPITY_SOUND, false)
        when (direction) {
            Direction.UP -> {
                index--
                if (index < 0) index = LEVELS[currentButtonKey].size - 1
            }

            Direction.DOWN -> {
                index++
                if (index >= LEVELS[currentButtonKey].size) index = 0
            }

            Direction.LEFT -> index = 0
            Direction.RIGHT -> index = 0
        }
    }

    override fun onAnySelection() = game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

    override fun show() {
        if (!initialized) init()
        super.show()
        game.getUiCamera().setToDefaultPosition()
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setCurrentScreen(ScreenEnum.SIMPLE_INIT_GAME_SCREEN.name)
        super.render(delta)

        sectionArrow.centerX = when (currentButtonKey) {
            BETA -> sections[0].font.position.x - 1.5f * ConstVals.PPM.toFloat()
            ALPHA -> sections[1].font.position.x - 1.75f * ConstVals.PPM.toFloat()
            TEST -> sections[2].font.position.x - 1.5f * ConstVals.PPM.toFloat()
            else -> throw IllegalStateException("Invalid current button key: $currentButtonKey")
        }
        sectionArrow.centerY = (SECTION_Y - 0.45f) * ConstVals.PPM
        sectionArrow.update(delta)
        levelArrows.forEach { it.update(delta) }

        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()
        sections.forEach { it.draw(batch) }
        levelArrows.forEach { it.draw(batch) }
        sectionArrow.draw(batch)
        level.draw(batch)
        batch.end()
    }
}