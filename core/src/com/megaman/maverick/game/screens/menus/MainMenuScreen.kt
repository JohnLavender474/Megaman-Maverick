package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.time.Timer
import com.engine.controller.ControllerUtils
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ConstVals.UI_ARROW_BLINK_DUR
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.ScreenSlide
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getDefaultCameraPosition
import com.megaman.maverick.game.utils.setToDefaultPosition

class MainMenuScreen(game: MegamanMaverickGame) : AbstractMenuScreen(game, MainScreenButton.GAME_START.text) {

    enum class MainScreenButton(val text: String) {
        GAME_START("GAME START"),
        PASSWORD("PASSWORD"),
        SETTINGS("SETTINGS"),
        CREDITS("CREDITS"),
        EXIT("EXIT")
    }

    enum class MainScreenSettingsButton(val text: String) {
        BACK("BACK"),
        MUSIC_VOLUME("MUSIC VOLUME"),
        EFFECTS_VOLUME("SFX VOLUME"),
        KEYBOARD_SETTINGS("KEYBOARD SETTINGS"),
        CONTROLLER_SETTINGS("CONTROLLER SETTINGS")
    }

    companion object {
        const val TAG = "MainScreen"
        private const val SETTINGS_TRANS_DUR = 0.5f
        private val SETTINGS_TRAJ = Vector3(15f * ConstVals.PPM, 0f, 0f)
    }

    override val eventKeyMask = objectSetOf<Any>()
    override val menuButtons = objectMapOf<String, IMenuButton>()

    private val pose = Sprite()
    private val title = Sprite()
    private val subtitle = Sprite()

    private var screenSlide =
        ScreenSlide(
            castGame.getUiCamera(),
            SETTINGS_TRAJ,
            getDefaultCameraPosition(),
            getDefaultCameraPosition().add(SETTINGS_TRAJ),
            SETTINGS_TRANS_DUR,
            true
        )

    private val fontHandles = Array<BitmapFontHandle>()
    private val settingsArrows = Array<Sprite>()

    private val settingsArrowBlinkTimer = Timer(UI_ARROW_BLINK_DUR)
    private val blinkArrows = ObjectMap<String, BlinkingArrow>()

    private var settingsArrowBlink = false
    private var doNotPlayPing = false

    init {
        var row = 4.75f

        MainScreenButton.values().forEach {
            val fontHandle =
                BitmapFontHandle(
                    it.text,
                    getDefaultFontSize(),
                    Vector2(2f * ConstVals.PPM, row * ConstVals.PPM),
                    centerX = false,
                    centerY = false,
                    fontSource = "Megaman10Font.ttf"
                )
            fontHandles.add(fontHandle)
            val arrowCenter =
                Vector2(1.5f * ConstVals.PPM, (row - 0.25f) * ConstVals.PPM)
            blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))
            row -= 0.025f * ConstVals.PPM
        }

        row = 11f

        MainScreenSettingsButton.values().forEach {
            val fontHandle =
                BitmapFontHandle(
                    it.text,
                    getDefaultFontSize(),
                    Vector2(17f * ConstVals.PPM, row * ConstVals.PPM),
                    centerX = false,
                    centerY = false,
                    fontSource = "Megaman10Font.ttf"
                )
            fontHandles.add(fontHandle)
            val arrowCenter =
                Vector2(16.5f * ConstVals.PPM, (row - 0.25f) * ConstVals.PPM)
            blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))
            row -= ConstVals.PPM * .025f
        }

        fontHandles.add(
            BitmapFontHandle(
                "© OLDLAVYGENES 20XX",
                getDefaultFontSize(),
                Vector2(0.15f * ConstVals.PPM, 0.5f * ConstVals.PPM),
                centerX = false,
                centerY = false,
                fontSource = "Megaman10Font.ttf"
            )
        )

        fontHandles.add(
            BitmapFontHandle(
                { (castGame.audioMan.musicVolume * 10f).toInt().toString() },
                getDefaultFontSize(),
                Vector2(25.2f * ConstVals.PPM, 10.45f * ConstVals.PPM),
                centerX = true,
                centerY = true,
                fontSource = "Megaman10Font.ttf"
            )
        )

        fontHandles.add(
            BitmapFontHandle(
                { (castGame.audioMan.soundVolume * 10f).toInt().toString() },
                getDefaultFontSize(),
                Vector2(25.2f * ConstVals.PPM, 9.625f * ConstVals.PPM),
                centerX = true,
                centerY = true,
                fontSource = "Megaman10Font.ttf"
            )
        )

        val arrowRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, "Arrow")
        var y = 9.8f
        for (i in 0 until 4) {
            if (i != 0 && i % 2 == 0) y -= 0.85f
            val blinkArrow = Sprite(arrowRegion)
            blinkArrow.setBounds(
                (if (i % 2 == 0) 24f else 26f) * ConstVals.PPM,
                y * ConstVals.PPM,
                ConstVals.PPM / 2f,
                ConstVals.PPM / 2f
            )
            blinkArrow.setFlip(i % 2 == 0, false)
            settingsArrows.add(blinkArrow)
        }

        val atlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        title.setRegion(atlas.findRegion("MegamanTitle"))
        title.setBounds(
            ConstVals.PPM.toFloat(), 6.25f * ConstVals.PPM, 13.25f * ConstVals.PPM, 5f * ConstVals.PPM
        )
        subtitle.setRegion(atlas.findRegion("Subtitle8bit"))
        subtitle.setSize(8f * ConstVals.PPM, 8f * ConstVals.PPM)
        subtitle.setCenter(
            ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, (ConstVals.VIEW_HEIGHT - 0.5f) * ConstVals.PPM / 2f
        )
        pose.setRegion(atlas.findRegion("MegamanMaverick"))
        pose.setBounds(8.5f * ConstVals.PPM, -ConstVals.PPM / 12f, 6f * ConstVals.PPM, 6f * ConstVals.PPM)

        menuButtons.put(
            MainScreenButton.GAME_START.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    castGame.setCurrentScreen(ScreenEnum.BOSS_SELECT_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenButton.EXIT.text
                        Direction.DOWN -> MainScreenButton.PASSWORD.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenButton.PASSWORD.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    // TODO: game.setCurrentScreen(ScreenEnum.PASSWORD_SCREEN.name)
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenButton.GAME_START.text
                        Direction.DOWN -> MainScreenButton.SETTINGS.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenButton.SETTINGS.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    screenSlide.init()
                    currentButtonKey = MainScreenSettingsButton.BACK.text
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenButton.PASSWORD.text
                        Direction.DOWN -> MainScreenButton.CREDITS.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenButton.CREDITS.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenButton.SETTINGS.text
                        Direction.DOWN -> MainScreenButton.EXIT.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenButton.EXIT.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    Gdx.app.exit()
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenButton.CREDITS.text
                        Direction.DOWN -> MainScreenButton.GAME_START.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenSettingsButton.BACK.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    screenSlide.init()
                    currentButtonKey = MainScreenButton.SETTINGS.text
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenSettingsButton.EFFECTS_VOLUME.text
                        Direction.DOWN -> MainScreenSettingsButton.MUSIC_VOLUME.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenSettingsButton.MUSIC_VOLUME.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.LEFT -> {
                            var volume = castGame.audioMan.musicVolume
                            volume = if (volume <= 0f) 1f else volume - 0.1f
                            castGame.audioMan.musicVolume = volume
                            null
                        }

                        Direction.RIGHT -> {
                            var volume = castGame.audioMan.musicVolume
                            volume = if (volume >= 1f) 0f else volume + 0.1f
                            castGame.audioMan.musicVolume = volume
                            null
                        }

                        Direction.UP -> MainScreenSettingsButton.BACK.text
                        Direction.DOWN -> MainScreenSettingsButton.EFFECTS_VOLUME.text
                    }
                }
            })

        menuButtons.put(
            MainScreenSettingsButton.EFFECTS_VOLUME.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    return false
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.LEFT -> {
                            var volume = castGame.audioMan.soundVolume
                            volume = if (volume <= 0f) 1f else volume - 0.1f
                            castGame.audioMan.soundVolume = volume
                            null
                        }

                        Direction.RIGHT -> {
                            var volume = castGame.audioMan.soundVolume
                            volume = if (volume >= 1f) 0f else volume + 0.1f
                            castGame.audioMan.soundVolume = volume
                            null
                        }

                        Direction.UP -> MainScreenSettingsButton.MUSIC_VOLUME.text
                        Direction.DOWN -> MainScreenSettingsButton.KEYBOARD_SETTINGS.text
                    }
                }
            })

        menuButtons.put(
            MainScreenSettingsButton.KEYBOARD_SETTINGS.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    castGame.setCurrentScreen(ScreenEnum.KEYBOARD_SETTINGS_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenSettingsButton.EFFECTS_VOLUME.text
                        Direction.DOWN -> MainScreenSettingsButton.CONTROLLER_SETTINGS.text
                        else -> null
                    }
                }
            })

        menuButtons.put(
            MainScreenSettingsButton.CONTROLLER_SETTINGS.text,
            object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    if (!ControllerUtils.isControllerConnected()) {
                        GameLogger.debug(TAG, "No controller connected")
                        game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
                        doNotPlayPing = true
                        return false
                    }
                    game.setCurrentScreen(ScreenEnum.CONTROLLER_SETTINGS_SCREEN.name)
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float): String? {
                    return when (direction) {
                        Direction.UP -> MainScreenSettingsButton.KEYBOARD_SETTINGS.text
                        Direction.DOWN -> MainScreenSettingsButton.BACK.text
                        else -> null
                    }
                }
            })
    }

    override fun show() {
        super.show()
        screenSlide.reset()
        castGame.getUiCamera().setToDefaultPosition()
        castGame.audioMan.playMusic(MusicAsset.MM_OMEGA_TITLE_THEME_MUSIC)
    }

    override fun render(delta: Float) {
        super.render(delta)
        if (!game.paused) {
            screenSlide.update(delta)
            if (screenSlide.justFinished) screenSlide.reverse()

            blinkArrows.get(currentButtonKey).update(delta)

            settingsArrowBlinkTimer.update(delta)
            if (settingsArrowBlinkTimer.isFinished()) {
                settingsArrowBlink = !settingsArrowBlink
                settingsArrowBlinkTimer.reset()
            }
        }

        val batch = game.batch
        batch.projectionMatrix = castGame.getUiCamera().combined
        batch.begin()

        blinkArrows.get(currentButtonKey).draw(batch)
        title.draw(batch)
        pose.draw(batch)
        subtitle.draw(batch)

        fontHandles.forEach { it.draw(batch) }

        if (settingsArrowBlink) settingsArrows.forEach { it.draw(batch) }

        batch.end()
    }

    override fun onAnyMovement() {
        super.onAnyMovement()
        GameLogger.debug(TAG, "Current button: $currentButtonKey")
        castGame.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
    }

    override fun onAnySelection() {
        val allow = screenSlide.finished
        if (allow) {
            if (doNotPlayPing) doNotPlayPing = false
            else castGame.audioMan.playSound(SoundAsset.SELECT_PING_SOUND)
        }
    }
}
