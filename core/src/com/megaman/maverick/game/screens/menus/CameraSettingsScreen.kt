package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Direction
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

class CameraSettingsScreen(
    game: MegamanMaverickGame,
    var backAction: () -> Boolean = {
        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        true
    }
) : MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "CameraSettingsScreen"
        const val BACK = "BACK"
        const val FPS = "FPS"
        const val LERP = "LERP"
        const val VALUE = "VALUE"
        const val KEY_X = 4f
        const val VALUE_X = 8f
    }

    private lateinit var backFontHandle: MegaFontHandle
    private lateinit var setFpsFontHandle: MegaFontHandle
    private lateinit var fpsFontHandle: MegaFontHandle
    private lateinit var setLerpFontHandle: MegaFontHandle
    private lateinit var doLerpFontHandle: MegaFontHandle
    private lateinit var setValueFontHandle: MegaFontHandle
    private lateinit var valueFontHandle: MegaFontHandle
    private lateinit var blinkingArrow: BlinkingArrow
    private var initialized = false

    override fun init() {
        if (initialized) return
        initialized = true

        var row = 11f
        blinkingArrow = BlinkingArrow(game.assMan, Vector2(2.5f * ConstVals.PPM, row * ConstVals.PPM))

        backFontHandle = MegaFontHandle(
            { BACK },
            positionX = KEY_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        buttons.put(BACK, object : IMenuButton {
            override fun onSelect(delta: Float) = backAction.invoke()

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> VALUE
                Direction.DOWN -> FPS
                else -> currentButtonKey
            }
        })

        row -= 1f
        setFpsFontHandle = MegaFontHandle(
            { FPS },
            positionX = KEY_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        fpsFontHandle = MegaFontHandle(
            { game.getTargetFPS().toString() },
            positionX = VALUE_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        buttons.put(FPS, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> BACK
                Direction.DOWN -> LERP
                Direction.LEFT -> {
                    val oldFPS = game.getTargetFPS()
                    val newFPS = when (oldFPS) {
                        ConstVals.HIGH_FPS -> ConstVals.MID_FPS
                        ConstVals.MID_FPS -> ConstVals.LOW_FPS
                        ConstVals.LOW_FPS -> ConstVals.HIGH_FPS
                        else -> {
                            if (oldFPS > ConstVals.MID_FPS) ConstVals.MID_FPS
                            else if (oldFPS > ConstVals.LOW_FPS) ConstVals.LOW_FPS
                            else ConstVals.HIGH_FPS
                        }
                    }
                    game.setTargetFPS(newFPS)
                    Gdx.graphics.setForegroundFPS(newFPS)
                    currentButtonKey
                }

                Direction.RIGHT -> {
                    val oldFPS = game.getTargetFPS()
                    val newFPS = when (oldFPS) {
                        ConstVals.HIGH_FPS -> ConstVals.LOW_FPS
                        ConstVals.MID_FPS -> ConstVals.HIGH_FPS
                        ConstVals.LOW_FPS -> ConstVals.MID_FPS
                        else -> {
                            if (oldFPS < ConstVals.LOW_FPS) ConstVals.LOW_FPS
                            else if (oldFPS < ConstVals.MID_FPS) ConstVals.MID_FPS
                            else ConstVals.HIGH_FPS
                        }
                    }
                    game.setTargetFPS(newFPS)
                    Gdx.graphics.setForegroundFPS(newFPS)
                    currentButtonKey
                }
            }


            override fun onSelect(delta: Float): Boolean {
                val oldFPS = game.getTargetFPS()
                val newFPS = when (oldFPS) {
                    ConstVals.HIGH_FPS -> ConstVals.LOW_FPS
                    ConstVals.MID_FPS -> ConstVals.HIGH_FPS
                    ConstVals.LOW_FPS -> ConstVals.MID_FPS
                    else -> {
                        if (oldFPS < ConstVals.LOW_FPS) ConstVals.LOW_FPS
                        else if (oldFPS < ConstVals.MID_FPS) ConstVals.MID_FPS
                        else ConstVals.HIGH_FPS
                    }
                }
                game.setTargetFPS(newFPS)
                return false
            }
        })

        row -= 1f
        setLerpFontHandle = MegaFontHandle(
            { LERP },
            positionX = KEY_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        doLerpFontHandle = MegaFontHandle(
            { game.doLerpGameCamera().toString() },
            positionX = VALUE_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        buttons.put(LERP, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> FPS
                Direction.DOWN -> if (game.doLerpGameCamera()) VALUE else BACK
                else -> {
                    val doLerp = game.doLerpGameCamera()
                    game.setLerpGameCamera(!doLerp)
                    currentButtonKey
                }
            }

            override fun onSelect(delta: Float): Boolean {
                val doLerp = game.doLerpGameCamera()
                game.setLerpGameCamera(!doLerp)
                return false
            }
        })

        row -= 1f
        setValueFontHandle =
            MegaFontHandle(
                { VALUE },
                positionX = KEY_X * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false
            )
        valueFontHandle = MegaFontHandle(
            { game.getLerpSettingForGameCamera() },
            positionX = VALUE_X * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false
        )
        buttons.put(VALUE, object : IMenuButton {
            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> LERP
                Direction.DOWN -> BACK
                Direction.LEFT -> {
                    val oldKey = game.getLerpSettingForGameCamera()
                    val newKey = when (oldKey) {
                        ConstKeys.FAST -> ConstKeys.MEDIUM
                        ConstKeys.MEDIUM -> ConstKeys.SLOW
                        ConstKeys.SLOW -> ConstKeys.FAST
                        else -> throw IllegalStateException("Illegal lerp setting: $oldKey")
                    }
                    game.setLerpSettingForGameCamera(newKey)
                    currentButtonKey
                }

                Direction.RIGHT -> {
                    val oldKey = game.getLerpSettingForGameCamera()
                    val newKey = when (oldKey) {
                        ConstKeys.FAST -> ConstKeys.SLOW
                        ConstKeys.MEDIUM -> ConstKeys.FAST
                        ConstKeys.SLOW -> ConstKeys.MEDIUM
                        else -> throw IllegalStateException("Illegal lerp setting: $oldKey")
                    }
                    game.setLerpSettingForGameCamera(newKey)
                    currentButtonKey
                }
            }

            override fun onSelect(delta: Float): Boolean {
                val oldKey = game.getLerpSettingForGameCamera()
                val newKey = when (oldKey) {
                    ConstKeys.FAST -> ConstKeys.SLOW
                    ConstKeys.MEDIUM -> ConstKeys.FAST
                    ConstKeys.SLOW -> ConstKeys.MEDIUM
                    else -> throw IllegalStateException("Illegal lerp setting: $oldKey")
                }
                game.setLerpSettingForGameCamera(newKey)
                return false
            }
        })
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

        val arrowY =
            when (currentButtonKey) {
                BACK -> 10.6f
                FPS -> 9.6f
                LERP -> 8.6f
                VALUE -> 7.6f
                else -> throw IllegalStateException("Illegal current button key: $currentButtonKey")
            }
        blinkingArrow.centerX = 2.5f * ConstVals.PPM
        blinkingArrow.centerY = arrowY * ConstVals.PPM
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        blinkingArrow.draw(game.batch)
        backFontHandle.draw(game.batch)
        setFpsFontHandle.draw(game.batch)
        fpsFontHandle.draw(game.batch)
        setLerpFontHandle.draw(game.batch)
        doLerpFontHandle.draw(game.batch)
        if (game.doLerpGameCamera()) {
            setValueFontHandle.draw(game.batch)
            valueFontHandle.draw(game.batch)
        }
        game.batch.end()
    }
}