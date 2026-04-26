package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.events.Event
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.ScreenEnum

class GameSettingsPanel(
    private val game: MegamanMaverickGame,
    private val onBack: () -> Unit,
    private val onError: () -> Unit = {},
    private val isInLevelScreen: Boolean = false
) : Initializable, Updatable, IDrawable<Batch>, Resettable {

    companion object {
        const val BACK_KEY = "BACK"
        const val MUSIC_VOLUME_KEY = "MUSIC VOLUME"
        const val EFFECTS_VOLUME_KEY = "SFX VOLUME"
        const val PIXEL_PERFECT_KEY = "PIXEL PERFECT"
        const val VSYNC_KEY = "VSYNC"
        const val PERFORMANCE_KEY = "PERFORMANCE"
        const val KEYBOARD_SETTINGS_KEY = "KEYBOARD SETTINGS"
        const val CONTROLLER_SETTINGS_KEY = "CONTROLLER SETTINGS"

        val ALL_KEYS = listOf(
            BACK_KEY, MUSIC_VOLUME_KEY, EFFECTS_VOLUME_KEY, PIXEL_PERFECT_KEY,
            VSYNC_KEY, PERFORMANCE_KEY, KEYBOARD_SETTINGS_KEY, CONTROLLER_SETTINGS_KEY
        )

        val SLIDE_TRAJ = Vector3(15f * ConstVals.PPM, 0f, 0f)
        const val SLIDE_DUR = 0.5f

        private const val START_ROW = 11f
        private const val TEXT_X = 17f
        private const val ARROW_X = 16.5f
    }

    var currentKey = BACK_KEY
        private set

    val buttons = ObjectMap<String, IMenuButton>()

    private val fontHandles = Array<MegaFontHandle>()
    private val blinkArrows = ObjectMap<String, BlinkingArrow>()
    private val settingsArrows = Array<GameSprite>()
    private val settingsArrowBlinkTimer = Timer(ConstVals.UI_ARROW_BLINK_DUR)
    private var settingsArrowBlink = false

    override fun reset() {
        currentKey = BACK_KEY
    }

    override fun init(vararg params: Any) {
        var row = START_ROW

        ALL_KEYS.forEach { key ->
            val textSupplier: () -> String = when (key) {
                PIXEL_PERFECT_KEY -> {
                    { "$PIXEL_PERFECT_KEY: ${game.isPixelPerfect().toString().uppercase()}" }
                }

                VSYNC_KEY -> {
                    { "$VSYNC_KEY: ${game.isVsync().toString().uppercase()}" }
                }

                PERFORMANCE_KEY -> {
                    { "$PERFORMANCE_KEY: ${game.getPerformance().name.replace("_", " ")}" }
                }

                else -> {
                    { key }
                }
            }

            fontHandles.add(
                MegaFontHandle(
                    textSupplier = textSupplier,
                    positionX = TEXT_X * ConstVals.PPM,
                    positionY = row * ConstVals.PPM,
                    centerX = false,
                    centerY = false
                )
            )
            blinkArrows.put(
                key, BlinkingArrow(
                    game.assMan,
                    Vector2(ARROW_X * ConstVals.PPM, (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM)
                )
            )

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        fontHandles.add(
            MegaFontHandle(
                { (game.audioMan.musicVolume * 10f).toInt().toString() },
                positionX = 25.2f * ConstVals.PPM,
                positionY = 10.45f * ConstVals.PPM,
            )
        )
        fontHandles.add(
            MegaFontHandle(
                { (game.audioMan.soundVolume * 10f).toInt().toString() },
                positionX = 25.2f * ConstVals.PPM,
                positionY = 9.625f * ConstVals.PPM
            )
        )

        val arrowRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, ConstKeys.ARROW)
        var y = 9.8f
        for (i in 0 until 4) {
            if (i != 0 && i % 2 == 0) y -= 0.85f
            val sprite = GameSprite(arrowRegion)
            sprite.setBounds(
                (if (i % 2 == 0) 24f else 26f) * ConstVals.PPM,
                y * ConstVals.PPM,
                ConstVals.PPM / 2f,
                ConstVals.PPM / 2f
            )
            sprite.setFlip(i % 2 == 0, false)
            settingsArrows.add(sprite)
        }

        defineButtons()
    }

    private fun defineButtons() {
        buttons.put(BACK_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                onBack()
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = CONTROLLER_SETTINGS_KEY; CONTROLLER_SETTINGS_KEY
                }

                Direction.DOWN -> {
                    currentKey = MUSIC_VOLUME_KEY; MUSIC_VOLUME_KEY
                }

                else -> currentKey
            }
        })

        buttons.put(MUSIC_VOLUME_KEY, object : IMenuButton {

            override fun onSelect(delta: Float) = false

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.LEFT -> {
                    var v = game.audioMan.musicVolume
                    v = if (v <= 0f) 1f else v - 0.1f
                    game.audioMan.musicVolume = v
                    currentKey
                }

                Direction.RIGHT -> {
                    var v = game.audioMan.musicVolume
                    v = if (v >= 1f) 0f else v + 0.1f
                    game.audioMan.musicVolume = v
                    currentKey
                }

                Direction.UP -> {
                    currentKey = BACK_KEY; BACK_KEY
                }

                Direction.DOWN -> {
                    currentKey = EFFECTS_VOLUME_KEY; EFFECTS_VOLUME_KEY
                }
            }
        })

        buttons.put(EFFECTS_VOLUME_KEY, object : IMenuButton {

            override fun onSelect(delta: Float) = false

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.LEFT -> {
                    var v = game.audioMan.soundVolume
                    v = if (v <= 0f) 1f else v - 0.1f
                    game.audioMan.soundVolume = v
                    currentKey
                }

                Direction.RIGHT -> {
                    var v = game.audioMan.soundVolume
                    v = if (v >= 1f) 0f else v + 0.1f
                    game.audioMan.soundVolume = v
                    currentKey
                }

                Direction.UP -> {
                    currentKey = MUSIC_VOLUME_KEY; MUSIC_VOLUME_KEY
                }

                Direction.DOWN -> {
                    currentKey = PIXEL_PERFECT_KEY; PIXEL_PERFECT_KEY
                }
            }
        })

        buttons.put(PIXEL_PERFECT_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                onNavigate(Direction.RIGHT, delta)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = EFFECTS_VOLUME_KEY; EFFECTS_VOLUME_KEY
                }

                Direction.DOWN -> {
                    currentKey = VSYNC_KEY; VSYNC_KEY
                }

                Direction.LEFT, Direction.RIGHT -> {
                    game.eventsMan.submitEvent(
                        Event(
                            EventType.TOGGLE_PIXEL_PERFECT,
                            props(ConstKeys.VALUE pairTo !game.isPixelPerfect())
                        )
                    )
                    currentKey
                }
            }
        })

        buttons.put(VSYNC_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                game.setVsync(!game.isVsync())
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = PIXEL_PERFECT_KEY; PIXEL_PERFECT_KEY
                }

                Direction.DOWN -> {
                    currentKey = PERFORMANCE_KEY; PERFORMANCE_KEY
                }

                Direction.LEFT, Direction.RIGHT -> {
                    game.setVsync(!game.isVsync())
                    currentKey
                }
            }
        })

        buttons.put(PERFORMANCE_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                onNavigate(Direction.RIGHT, delta)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = VSYNC_KEY; VSYNC_KEY
                }

                Direction.DOWN -> {
                    currentKey = KEYBOARD_SETTINGS_KEY; KEYBOARD_SETTINGS_KEY
                }

                Direction.LEFT -> {
                    val entries = MegamanMaverickGame.Performance.entries
                    val idx = entries.indexOf(game.getPerformance())
                    game.setPerformance(if (idx <= 0) entries.last() else entries[idx - 1])
                    currentKey
                }

                Direction.RIGHT -> {
                    val entries = MegamanMaverickGame.Performance.entries
                    val idx = entries.indexOf(game.getPerformance())
                    game.setPerformance(if (idx >= entries.lastIndex) entries.first() else entries[idx + 1])
                    currentKey
                }
            }
        })

        buttons.put(KEYBOARD_SETTINGS_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                if (isInLevelScreen) {
                    val backAction: () -> Boolean = backAction@{
                        game.startLevel()
                        return@backAction true
                    }
                    game.setCurrentScreen(
                        ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name,
                        props("${ConstKeys.BACK}_${ConstKeys.ACTION}" pairTo backAction)
                    )
                } else game.setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)

                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = PERFORMANCE_KEY; PERFORMANCE_KEY
                }

                Direction.DOWN -> {
                    currentKey = CONTROLLER_SETTINGS_KEY; CONTROLLER_SETTINGS_KEY
                }

                else -> currentKey
            }
        })

        buttons.put(CONTROLLER_SETTINGS_KEY, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                if (!ControllerUtils.isControllerConnected()) {
                    game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                    onError()
                    return false
                }

                if (isInLevelScreen) {
                    val backAction: () -> Boolean = backAction@{
                        game.startLevel()
                        return@backAction true
                    }
                    game.setCurrentScreen(
                        ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name,
                        props("${ConstKeys.BACK}_${ConstKeys.ACTION}" pairTo backAction)
                    )
                } else game.setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)

                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> {
                    currentKey = KEYBOARD_SETTINGS_KEY; KEYBOARD_SETTINGS_KEY
                }

                Direction.DOWN -> {
                    currentKey = BACK_KEY; BACK_KEY
                }

                else -> currentKey
            }
        })
    }

    override fun update(delta: Float) {
        blinkArrows.get(currentKey)?.update(delta)
        settingsArrowBlinkTimer.update(delta)
        if (settingsArrowBlinkTimer.isFinished()) {
            settingsArrowBlink = !settingsArrowBlink
            settingsArrowBlinkTimer.reset()
        }
    }

    override fun draw(drawer: Batch) {
        fontHandles.forEach { it.draw(drawer) }
        blinkArrows.get(currentKey)?.draw(drawer)
        if (settingsArrowBlink) settingsArrows.forEach { it.draw(drawer) }
    }

    fun draw(renderer: ShapeRenderer) {
        blinkArrows.values().forEach { it.draw(renderer) }
    }
}
