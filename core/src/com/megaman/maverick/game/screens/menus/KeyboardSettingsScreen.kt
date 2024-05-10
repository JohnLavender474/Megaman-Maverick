package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
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
    }

    override val menuButtons = ObjectMap<String, IMenuButton>()
    override val eventKeyMask = ObjectSet<Any>()

    private val keyboardListener = object : InputAdapter() {
        override fun keyUp(keycode: Int): Boolean {
            val button = buttons.get(selectedButton!!)
            GameLogger.debug(TAG, "Setting [$selectedButton] keycode from [${button.keyboardCode}] to [$keycode]")
            button.keyboardCode = keycode
            Gdx.input.inputProcessor = oldInputProcessor
            oldInputProcessor = null
            undoSelection()
            return true
        }
    }
    private val hintFontHandle: BitmapFontHandle = BitmapFontHandle(
        "Press any key to set a new code for the selected button",

    )
    private val fontHandles = Array<BitmapFontHandle>()
    private val blinkingArrow: BlinkingArrow
    private var selectedButton: ControllerButton? = null
    private var oldInputProcessor: InputProcessor? = null

    init {

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
        fontHandles.add(backFontHandle)

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
            fontHandles.add(buttonFontHandle)

            menuButtons.put(controllerButton.name, object : IMenuButton {
                override fun onSelect(delta: Float): Boolean {
                    selectedButton = controllerButton
                    oldInputProcessor = Gdx.input.inputProcessor
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
        castGame.getUiCamera().setToDefaultPosition()
    }

    override fun render(delta: Float) {
        super.render(delta)

        val arrowY =
            if (currentButtonKey == BACK) 10.6f else 10.6f - (ControllerButton.valueOf(currentButtonKey).ordinal + 1)
        blinkingArrow.center = Vector2(2.5f * ConstVals.PPM, arrowY * ConstVals.PPM)
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = castGame.getUiCamera().combined
        game.batch.begin()
        blinkingArrow.draw(game.batch)
        fontHandles.forEach { it.draw(game.batch) }
        game.batch.end()
    }
}