package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.setToDefaultPosition

// TODO: disable fps settings until issues regarding physics tied to fps are fixed
class CameraSettingsScreen(
    game: MegamanMaverickGame,
    var backAction: () -> Boolean = {
        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        true
    }
) : MegaMenuScreen(game, CameraSettingsButton.BACK.name), Initializable {

    companion object {
        const val TAG = "CameraSettingsScreen"
        private const val FONT_START_ROW = 11f
        private const val ARROW_START_ROW = 10.5f
        private const val KEY_X = 4f
        private const val VALUE_X = 9f
    }

    private enum class CameraSettingsButton { BACK, /* FPS, VSYNC, */ LERP, LERP_VAL }

    private val buttonValues = objectMapOf(
        /*
        CameraSettingsButton.FPS to { Gdx.graphics.framesPerSecond.toString() },
        CameraSettingsButton.VSYNC to { game.doUseVsync().toString() },
         */
        CameraSettingsButton.LERP to { game.doLerpGameCamera().toString() },
        CameraSettingsButton.LERP_VAL to { game.getLerpValueForGameCamera() }
    )
    private val buttonFontHandles = OrderedMap<MegaFontHandle, () -> Boolean>()
    private lateinit var blinkingArrow: BlinkingArrow
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        blinkingArrow = BlinkingArrow(game.assMan, Vector2(2.5f * ConstVals.PPM, FONT_START_ROW * ConstVals.PPM))

        for (i in 0 until CameraSettingsButton.values().size) {
            val button = CameraSettingsButton.values()[i]
            val row = FONT_START_ROW - (i + 1)

            val buttonFontHandle = MegaFontHandle(
                { button.name },
                positionX = KEY_X * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false
            )
            buttonFontHandles.put(buttonFontHandle) {
                if (button == CameraSettingsButton.LERP_VAL) game.doLerpGameCamera() else true
            }

            if (buttonValues.containsKey(button)) {
                val buttonValueHandle = MegaFontHandle(
                    { buttonValues.get(button).invoke() },
                    positionX = VALUE_X * ConstVals.PPM,
                    positionY = row * ConstVals.PPM,
                    centerX = false
                )
                buttonFontHandles.put(buttonValueHandle) {
                    if (button == CameraSettingsButton.LERP_VAL) game.doLerpGameCamera() else true
                }
            }

            buttons.put(button.name, object : IMenuButton {
                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> {
                        var index = i - 1
                        if (index < 0) index = CameraSettingsButton.values().size - 1
                        CameraSettingsButton.values()[index].name
                    }

                    Direction.DOWN -> {
                        val index = (i + 1) % CameraSettingsButton.values().size
                        CameraSettingsButton.values()[index].name
                    }

                    Direction.LEFT -> {
                        when (button) {
                            /*
                            CameraSettingsButton.FPS -> {
                                shiftFpsLeft()
                                currentButtonKey
                            }

                            CameraSettingsButton.VSYNC -> {
                                shiftUseVsync()
                                currentButtonKey
                            }
                             */

                            CameraSettingsButton.LERP -> {
                                shiftLerp()
                                currentButtonKey
                            }

                            CameraSettingsButton.LERP_VAL -> {
                                shiftLerpValueLeft()
                                currentButtonKey
                            }

                            else -> currentButtonKey
                        }
                    }

                    Direction.RIGHT -> {
                        when (button) {
                            /*
                            CameraSettingsButton.FPS -> {
                                shiftFpsRight()
                                currentButtonKey
                            }

                            CameraSettingsButton.VSYNC -> {
                                shiftUseVsync()
                                currentButtonKey
                            }
                             */

                            CameraSettingsButton.LERP -> {
                                shiftLerp()
                                currentButtonKey
                            }

                            CameraSettingsButton.LERP_VAL -> {
                                shiftLerpValueRight()
                                currentButtonKey
                            }

                            else -> currentButtonKey
                        }
                    }
                }

                override fun onSelect(delta: Float) = when (button) {
                    CameraSettingsButton.BACK -> {
                        backAction.invoke()
                        true
                    }

                    /*
                    CameraSettingsButton.FPS -> {
                        shiftFpsRight()
                        false
                    }

                    CameraSettingsButton.VSYNC -> {
                        shiftUseVsync()
                        false
                    }
                     */

                    CameraSettingsButton.LERP -> {
                        shiftLerp()
                        false
                    }

                    CameraSettingsButton.LERP_VAL -> {
                        shiftLerpValueRight()
                        false
                    }
                }
            })
        }
    }

    override fun show() {
        if (!initialized) init()
        super.show()
        game.getUiCamera().setToDefaultPosition()
    }

    override fun onAnySelection() =
        game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)

        blinkingArrow.centerX = 2.5f * ConstVals.PPM
        blinkingArrow.centerY =
            (ARROW_START_ROW - CameraSettingsButton.valueOf(currentButtonKey!!).ordinal) * ConstVals.PPM
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        blinkingArrow.draw(game.batch)
        buttonFontHandles.forEach { if (it.value()) it.key.draw(game.batch) }
        game.batch.end()
    }

    private fun shiftFpsLeft() {
        val oldFPS = game.getTargetFPS()
        val newFPS = when (oldFPS) {
            ConstVals.VERY_HIGH_FPS -> ConstVals.HIGH_FPS
            ConstVals.HIGH_FPS -> ConstVals.MID_FPS
            ConstVals.MID_FPS -> ConstVals.LOW_FPS
            ConstVals.LOW_FPS -> ConstVals.VERY_HIGH_FPS
            else -> throw IllegalStateException("Invalid FPS value: $oldFPS")
        }
        game.setTargetFPS(newFPS)
    }

    private fun shiftFpsRight() {
        val oldFPS = game.getTargetFPS()
        val newFPS = when (oldFPS) {
            ConstVals.VERY_HIGH_FPS -> ConstVals.LOW_FPS
            ConstVals.HIGH_FPS -> ConstVals.VERY_HIGH_FPS
            ConstVals.MID_FPS -> ConstVals.HIGH_FPS
            ConstVals.LOW_FPS -> ConstVals.MID_FPS
            else -> throw IllegalStateException("Invalid FPS value: $oldFPS")
        }
        game.setTargetFPS(newFPS)
    }

    private fun shiftUseVsync() {
        val currentlyUsing = game.doUseVsync()
        game.setUseVsync(!currentlyUsing)
    }

    private fun shiftLerp() {
        val doLerp = game.doLerpGameCamera()
        game.setDoLerpGameCamera(!doLerp)
    }

    private fun shiftLerpValueLeft() {
        val oldKey = game.getLerpValueForGameCamera()
        val newKey = when (oldKey) {
            ConstKeys.FAST -> ConstKeys.MEDIUM
            ConstKeys.MEDIUM -> ConstKeys.SLOW
            ConstKeys.SLOW -> ConstKeys.FAST
            else -> throw IllegalStateException("Illegal lerp setting: $oldKey")
        }
        game.setLerpValueForGameCamera(newKey)
    }

    private fun shiftLerpValueRight() {
        val oldKey = game.getLerpValueForGameCamera()
        val newKey = when (oldKey) {
            ConstKeys.FAST -> ConstKeys.SLOW
            ConstKeys.MEDIUM -> ConstKeys.FAST
            ConstKeys.SLOW -> ConstKeys.MEDIUM
            else -> throw IllegalStateException("Illegal lerp setting: $oldKey")
        }
        game.setLerpValueForGameCamera(newKey)
        currentButtonKey
    }
}