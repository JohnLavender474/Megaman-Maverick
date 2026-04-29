package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.GameSettingsPanel
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

    companion object {
        const val TAG = "MainMenuScreen"
        private const val DEBUG_SHAPES = false
        private const val MAIN_MENU_TEXT_START_ROW = 6f
        private val MAIN_MENU_MUSIC = MusicAsset.VINNYZ_MAIN_MENU_MUSIC
    }

    private lateinit var background: GameSprite

    private val screenSlide = ScreenSlide(
        game.getUiCamera(),
        getDefaultCameraPosition(false),
        getDefaultCameraPosition(false).add(GameSettingsPanel.SLIDE_TRAJ),
        GameSettingsPanel.SLIDE_DUR,
        true
    )

    private val fontHandles = Array<MegaFontHandle>()
    private val blinkArrows = ObjectMap<String, BlinkingArrow>()

    private var doNotPlayPing = false
    private var initialized = false

    private val settingsPanel = GameSettingsPanel(
        game = game,
        onBack = {
            screenSlide.init()
            buttonKey = MainScreenButton.SETTINGS.text
        },
        onError = { doNotPlayPing = true },
        isInLevelScreen = false
    )

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        var row = MAIN_MENU_TEXT_START_ROW

        MainScreenButton.entries.forEach {
            fontHandles.add(
                MegaFontHandle(
                    it.text,
                    positionX = 2f * ConstVals.PPM,
                    positionY = row * ConstVals.PPM,
                    centerX = false,
                    centerY = false
                )
            )
            blinkArrows.put(
                it.text, BlinkingArrow(
                    game.assMan,
                    Vector2(1.5f * ConstVals.PPM, (row - ConstVals.ARROW_CENTER_ROW_DECREMENT) * ConstVals.PPM)
                )
            )
            row -= ConstVals.TEXT_ROW_DECREMENT * ConstVals.PPM
        }

        fontHandles.add(
            MegaFontHandle(
                MegamanMaverickGame.VERSION,
                positionX = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                positionY = 9f * ConstVals.PPM,
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

        settingsPanel.init()
        settingsPanel.buttons.forEach { buttons.put(it.key, it.value) }

        val atlas = game.assMan.getTextureAtlas(TextureAsset.UI_2.source)
        background = GameSprite(atlas.findRegion("TitleScreenBackgroundv4"))
        // Permanently setting both width and height to the 'view width' size
        background.setSize(ConstVals.VIEW_WIDTH * ConstVals.PPM)
        background.setCenter(ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f)

        buttons.put(MainScreenButton.START_NEW_GAME.text, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                game.state.reset()
                game.setCurrentScreen(ScreenEnum.SELECT_DIFFICULTY_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MainScreenButton.EXIT.text
                Direction.DOWN -> MainScreenButton.LOAD_SAVE_FILE.text
                else -> buttonKey
            }
        })

        buttons.put(MainScreenButton.LOAD_SAVE_FILE.text, object : IMenuButton {
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
                    game.removeProperty(ConstKeys.WEAPONS_ATTAINED)
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
        })

        buttons.put(MainScreenButton.SETTINGS.text, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                screenSlide.init()
                settingsPanel.reset()
                buttonKey = GameSettingsPanel.BACK_KEY
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MainScreenButton.LOAD_SAVE_FILE.text
                Direction.DOWN -> MainScreenButton.CREDITS.text
                else -> buttonKey
            }
        })

        buttons.put(MainScreenButton.CREDITS.text, object : IMenuButton {
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

        buttons.put(MainScreenButton.EXIT.text, object : IMenuButton {
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
    }

    override fun show() {
        if (!initialized) init()
        super.show()

        screenSlide.reset()

        game.getUiCamera().setToDefaultPosition()
        game.audioMan.playMusic(MAIN_MENU_MUSIC, false)

        GameLogger.debug(TAG, "current button key: $buttonKey")
        GameLogger.debug(TAG, "blinking arrows keys: ${blinkArrows.keys().toGdxArray()}")
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            screenSlide.update(delta)
            if (screenSlide.justFinished) screenSlide.reverse()

            if (GameSettingsPanel.ALL_KEYS.contains(buttonKey)) settingsPanel.update(delta)
            else blinkArrows.get(buttonKey)?.update(delta)
        }
    }

    override fun draw(drawer: Batch) {
        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = game.getUiCamera().combined

        drawer.begin()

        background.draw(drawer)
        fontHandles.forEach { it.draw(drawer) }
        blinkArrows.get(buttonKey)?.draw(drawer)

        settingsPanel.draw(drawer)

        drawer.end()
    }

    override fun draw(renderer: ShapeRenderer) {
        if (DEBUG_SHAPES) {
            val renderer = game.shapeRenderer
            renderer.projectionMatrix = game.getUiCamera().combined
            renderer.begin()

            blinkArrows.values().forEach { it.draw(renderer) }
            settingsPanel.draw(renderer)

            renderer.end()
        }
    }

    override fun getNavigationDirection() =
        if (screenSlide.finished) super.getNavigationDirection() else null

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
