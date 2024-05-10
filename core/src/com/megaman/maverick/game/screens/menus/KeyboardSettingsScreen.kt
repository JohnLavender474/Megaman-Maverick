package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.time.Timer
import com.engine.controller.buttons.Buttons
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.setToDefaultPosition

class KeyboardSettingsScreen(game: MegamanMaverickGame, private val buttons: Buttons) :
    AbstractMenuScreen(game, BACK) {

    companion object {
        const val TAG = "KeyboardSettingsScreen"
        private const val BACK = "BACK TO MAIN MENU"
        private const val DELAY_ON_CHANGE = 0.25f
    }

    override val menuButtons = ObjectMap<String, IMenuButton>()
    override val eventKeyMask = ObjectSet<Any>()

    private val delayOnChangeTimer = Timer(DELAY_ON_CHANGE)
    private val keyboardListener = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (selectedButton == null) return true

            val button = buttons.get(selectedButton!!)
            GameLogger.debug(TAG, "Setting [$selectedButton] keycode from [${button.keyboardCode}] to [$keycode]")

            // if any buttons match the keycode, then switch them
            buttons.values().forEach {
                if (it.keyboardCode == keycode) it.keyboardCode = button.keyboardCode
            }
            button.keyboardCode = keycode

            Gdx.input.inputProcessor = null
            selectedButton = null

            delayOnChangeTimer.reset()
            return true
        }
    }
    private val hintFontHandle: BitmapFontHandle
    private val buttonFontHandles = Array<BitmapFontHandle>()
    private val blinkingArrow: BlinkingArrow
    private var selectedButton: ControllerButton? = null

    init {
        hintFontHandle = BitmapFontHandle(
            { "Press any key to set \na new code for the button: \n$selectedButton" },
            getDefaultFontSize(),
            Vector2(
                ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
            ),
            fontSource = "Megaman10Font.ttf",
            centerX = true,
            centerY = true,
        )

        var row = 10.75f
        blinkingArrow = BlinkingArrow(game.assMan, Vector2(2.5f * ConstVals.PPM, row * ConstVals.PPM))

        val backFontHandle = BitmapFontHandle(
            BACK,
            getDefaultFontSize(),
            Vector2(3f * ConstVals.PPM, row * ConstVals.PPM),
            centerX = false,
            centerY = false,
            fontSource = "Megaman10Font.ttf"
        )
        buttonFontHandles.add(backFontHandle)

        menuButtons.put(BACK, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) =
                when (direction) {
                    Direction.UP -> ControllerButton.B.name
                    Direction.DOWN -> ControllerButton.START.name
                    else -> null
                }
        })

        ControllerButton.values().forEach { controllerButton ->
            row -= 1f
            val buttonFontHandle = BitmapFontHandle(
                { "${controllerButton.name}: ${buttons.get(controllerButton)?.keyboardCode}" },
                getDefaultFontSize(),
                Vector2(3f * ConstVals.PPM, row * ConstVals.PPM),
                centerX = false,
                centerY = false,
                fontSource = "Megaman10Font.ttf"
            )
            buttonFontHandles.add(buttonFontHandle)

            menuButtons.put(controllerButton.name, object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    selectedButton = controllerButton
                    Gdx.input.inputProcessor = keyboardListener
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) =
                    when (direction) {
                        Direction.UP -> {
                            val index = controllerButton.ordinal - 1
                            if (index < 0) BACK else ControllerButton.values()[index].name
                        }

                        Direction.DOWN -> {
                            val index = controllerButton.ordinal + 1
                            if (index >= ControllerButton.values().size) BACK else ControllerButton.values()[index].name
                        }

                        else -> null
                    }
            })
        }
    }

    override fun show() {
        super.show()
        delayOnChangeTimer.setToEnd()
        castGame.getUiCamera().setToDefaultPosition()
    }

    override fun render(delta: Float) {
        super.render(delta)

        delayOnChangeTimer.update(delta)
        if (delayOnChangeTimer.isJustFinished()) undoSelection()

        val arrowY =
            if (currentButtonKey == BACK) 10.6f else 10.6f - (ControllerButton.valueOf(currentButtonKey).ordinal + 1)
        blinkingArrow.center = Vector2(2.5f * ConstVals.PPM, arrowY * ConstVals.PPM)
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = castGame.getUiCamera().combined
        game.batch.begin()
        if (selectedButton != null) hintFontHandle.draw(game.batch)
        else {
            blinkingArrow.draw(game.batch)
            buttonFontHandles.forEach { it.draw(game.batch) }
        }
        game.batch.end()
    }
}