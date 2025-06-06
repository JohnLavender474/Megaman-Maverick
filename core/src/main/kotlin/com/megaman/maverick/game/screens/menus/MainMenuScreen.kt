package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.events.Event
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.ScreenSlide
import com.megaman.maverick.game.utils.extensions.getDefaultCameraPosition
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition
import com.megaman.maverick.game.utils.interfaces.IShapeDebuggable

class MainMenuScreen(game: MegamanMaverickGame) : MegaMenuScreen(game, MainScreenButton.START_NEW_GAME.text),
    Initializable, IShapeDebuggable {

    private enum class MainScreenButton(val text: String) {
        START_NEW_GAME("START NEW GAME"),
        LOAD_SAVE_FILE("LOAD SAVE FILE"),
        SETTINGS("SETTINGS"),
        CREDITS("CREDITS"),
        EXIT("EXIT")
    }

    private enum class MainScreenSettingsButton(val text: String) {
        BACK("BACK"),
        MUSIC_VOLUME("MUSIC VOLUME"),
        EFFECTS_VOLUME("SFX VOLUME"),
        PIXEL_PERFECT("PIXEL PERFECT"),
        KEYBOARD_SETTINGS("KEYBOARD SETTINGS"),
        CONTROLLER_SETTINGS("CONTROLLER SETTINGS")
    }

    companion object {
        const val TAG = "MainMenuScreen"
        private const val DEBUG_SHAPES = false
        private const val MAIN_MENU_TEXT_START_ROW = 6f
        private const val SETTINGS_TEXT_START_ROW = 11f
        private const val SETTINGS_TRANS_DUR = 0.5f
        private val SETTINGS_TRAJ = Vector3(15f * ConstVals.PPM, 0f, 0f)
        private val MAIN_MENU_MUSIC = MusicAsset.MMX3_INTRO_STAGE_MUSIC
    }

    private lateinit var background: GameSprite

    private var screenSlide = ScreenSlide(
        game.getUiCamera(),
        getDefaultCameraPosition(false),
        getDefaultCameraPosition(false).add(SETTINGS_TRAJ),
        SETTINGS_TRANS_DUR,
        true
    )

    private val fontHandles = Array<MegaFontHandle>()
    private val settingsArrows = Array<GameSprite>()

    private val settingsArrowBlinkTimer = Timer(ConstVals.UI_ARROW_BLINK_DUR)
    private val blinkArrows = ObjectMap<String, BlinkingArrow>()

    private var settingsArrowBlink = false
    private var doNotPlayPing = false

    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        var row = MAIN_MENU_TEXT_START_ROW

        MainScreenButton.entries.forEach {
            val fontHandle = MegaFontHandle(
                it.text,
                positionX = 2f * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
            fontHandles.add(fontHandle)

            val arrowCenter =
                Vector2(1.5f * ConstVals.PPM, (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM)
            blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        row = SETTINGS_TEXT_START_ROW

        MainScreenSettingsButton.entries.forEach {
            val textSupplier: () -> String = when (it) {
                MainScreenSettingsButton.PIXEL_PERFECT -> {
                    { "${it.text}: ${game.isPixelPerfect().toString().uppercase()}" }
                }
                else -> {
                    { it.text }
                }
            }

            val fontHandle = MegaFontHandle(
                textSupplier = textSupplier,
                positionX = 17f * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
            fontHandles.add(fontHandle)

            val arrowCenter =
                Vector2(16.5f * ConstVals.PPM, (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM)
            blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))

            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        fontHandles.add(
            MegaFontHandle(
                MegamanMaverickGame.VERSION,
                positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                positionY = 8.5f * ConstVals.PPM,
                centerX = true,
                centerY = true
            )
        )

        fontHandles.add(
            MegaFontHandle(
                "OLDLAVYGENES 20XX",
                positionX = 5f * ConstVals.PPM,
                positionY = 0.5f * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
        )

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

            val blinkArrow = GameSprite(arrowRegion)
            blinkArrow.setBounds(
                (if (i % 2 == 0) 24f else 26f) * ConstVals.PPM,
                y * ConstVals.PPM,
                ConstVals.PPM / 2f,
                ConstVals.PPM / 2f
            )
            blinkArrow.setFlip(i % 2 == 0, false)

            settingsArrows.add(blinkArrow)
        }

        val atlas = game.assMan.getTextureAtlas(TextureAsset.UI_2.source)
        background = GameSprite(atlas.findRegion("TitleScreenBackgroundv3"))
        background.setSize(ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        background.setCenter(ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f)

        buttons.put(
            MainScreenButton.START_NEW_GAME.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    game.state.reset()
                    game.setCurrentLevel(LevelDefinition.INTRO_STAGE)
                    game.startLevel()
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenButton.EXIT.text
                    Direction.DOWN -> MainScreenButton.LOAD_SAVE_FILE.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenButton.LOAD_SAVE_FILE.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    if (game.hasSavedState() && game.loadSavedState()) {
                        GameLogger.debug(TAG, "Loaded saved state")
                        when {
                            game.state.isLevelDefeated(LevelDefinition.INTRO_STAGE) ->
                                game.setCurrentScreen(ScreenEnum.LEVEL_SELECT_SCREEN.name)
                            else -> {
                                game.setCurrentLevel(LevelDefinition.INTRO_STAGE)
                                game.startLevel()
                            }
                        }
                        game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

                        return true
                    } else {
                        GameLogger.error(TAG, "Failed to load saved state")
                        game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                        doNotPlayPing = true
                        return false
                    }
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenButton.START_NEW_GAME.text
                    Direction.DOWN -> MainScreenButton.SETTINGS.text
                    else -> buttonKey
                }
            }
        )

        buttons.put(
            MainScreenButton.SETTINGS.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    screenSlide.init()
                    buttonKey = MainScreenSettingsButton.BACK.text
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenButton.LOAD_SAVE_FILE.text
                    Direction.DOWN -> MainScreenButton.CREDITS.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenButton.CREDITS.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    game.setCurrentScreen(ScreenEnum.CREDITS_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenButton.SETTINGS.text
                    Direction.DOWN -> MainScreenButton.EXIT.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenButton.EXIT.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    Gdx.app.exit()
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenButton.CREDITS.text
                    Direction.DOWN -> MainScreenButton.START_NEW_GAME.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenSettingsButton.BACK.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    screenSlide.init()
                    buttonKey = MainScreenButton.SETTINGS.text
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenSettingsButton.CONTROLLER_SETTINGS.text
                    Direction.DOWN -> MainScreenSettingsButton.MUSIC_VOLUME.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenSettingsButton.MUSIC_VOLUME.text,
            object : IMenuButton {

                override fun onSelect(delta: Float) = false

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.LEFT -> {
                        var volume = game.audioMan.musicVolume
                        volume = if (volume <= 0f) 1f else volume - 0.1f
                        game.audioMan.musicVolume = volume
                        buttonKey
                    }

                    Direction.RIGHT -> {
                        var volume = game.audioMan.musicVolume
                        volume = if (volume >= 1f) 0f else volume + 0.1f
                        game.audioMan.musicVolume = volume
                        buttonKey
                    }

                    Direction.UP -> MainScreenSettingsButton.BACK.text
                    Direction.DOWN -> MainScreenSettingsButton.EFFECTS_VOLUME.text
                }
            })

        buttons.put(
            MainScreenSettingsButton.EFFECTS_VOLUME.text,
            object : IMenuButton {

                override fun onSelect(delta: Float) = false

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.LEFT -> {
                        var volume = game.audioMan.soundVolume
                        volume = if (volume <= 0f) 1f else volume - 0.1f
                        game.audioMan.soundVolume = volume
                        buttonKey
                    }

                    Direction.RIGHT -> {
                        var volume = game.audioMan.soundVolume
                        volume = if (volume >= 1f) 0f else volume + 0.1f
                        game.audioMan.soundVolume = volume
                        buttonKey
                    }

                    Direction.UP -> MainScreenSettingsButton.MUSIC_VOLUME.text
                    Direction.DOWN -> MainScreenSettingsButton.PIXEL_PERFECT.text
                }
            })

        buttons.put(
            MainScreenSettingsButton.PIXEL_PERFECT.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    onNavigate(Direction.RIGHT, delta)
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenSettingsButton.EFFECTS_VOLUME.text
                    Direction.DOWN -> MainScreenSettingsButton.KEYBOARD_SETTINGS.text
                    Direction.LEFT, Direction.RIGHT -> {
                        val pixelPerfect = game.isPixelPerfect()

                        game.eventsMan.submitEvent(
                            Event(
                                EventType.TOGGLE_PIXEL_PERFECT,
                                props(ConstKeys.VALUE pairTo !pixelPerfect)
                            )
                        )

                        buttonKey
                    }
                }
            }
        )

        buttons.put(
            MainScreenSettingsButton.KEYBOARD_SETTINGS.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    game.setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenSettingsButton.PIXEL_PERFECT.text
                    Direction.DOWN -> MainScreenSettingsButton.CONTROLLER_SETTINGS.text
                    else -> buttonKey
                }
            })

        buttons.put(
            MainScreenSettingsButton.CONTROLLER_SETTINGS.text,
            object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    if (!ControllerUtils.isControllerConnected()) {
                        GameLogger.debug(TAG, "no controller connected")
                        game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                        doNotPlayPing = true
                        return false
                    }
                    game.setCurrentScreen(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> MainScreenSettingsButton.KEYBOARD_SETTINGS.text
                    Direction.DOWN -> MainScreenSettingsButton.BACK.text
                    else -> buttonKey
                }
            })
    }

    override fun show() {
        if (!initialized) init()
        super.show()

        screenSlide.reset()

        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MAIN_MENU_MUSIC, true)

        GameLogger.debug(TAG, "current button key: $buttonKey")
        GameLogger.debug(TAG, "blinking arrows keys: ${blinkArrows.keys().toGdxArray()}")
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            screenSlide.update(delta)
            if (screenSlide.justFinished) screenSlide.reverse()

            blinkArrows.get(buttonKey).update(delta)

            settingsArrowBlinkTimer.update(delta)
            if (settingsArrowBlinkTimer.isFinished()) {
                settingsArrowBlink = !settingsArrowBlink
                settingsArrowBlinkTimer.reset()
            }
        }
    }

    override fun draw(drawer: Batch) {
        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = game.getUiCamera().combined

        drawer.begin()

        background.draw(drawer)
        fontHandles.forEach { it.draw(drawer) }
        blinkArrows.get(buttonKey).draw(drawer)

        if (settingsArrowBlink) settingsArrows.forEach { it.draw(drawer) }

        drawer.end()
    }

    override fun draw(renderer: ShapeRenderer) {
        if (DEBUG_SHAPES) {
            val renderer = game.shapeRenderer
            renderer.projectionMatrix = game.getUiCamera().combined
            renderer.begin()
            blinkArrows.values().forEach { it.draw(renderer) }
            renderer.end()
        }
    }

    override fun getNavigationDirection() = if (screenSlide.finished) super.getNavigationDirection() else null

    override fun selectionRequested() = screenSlide.finished && super.selectionRequested()

    override fun onAnyMovement(direction: Direction) {
        GameLogger.debug(TAG, "Current button: $buttonKey")
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }

    override fun onAnySelection() {
        val allow = screenSlide.finished
        if (allow) {
            if (doNotPlayPing) doNotPlayPing = false
            else game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND)
        }
    }
}
