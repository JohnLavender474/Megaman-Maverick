package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.time.Timer
import com.engine.controller.ControllerUtils
import com.engine.controller.buttons.Buttons
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.PreferenceFiles
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.getControllerPreferences
import com.megaman.maverick.game.controllers.getKeyboardPreferences
import com.megaman.maverick.game.controllers.resetToDefaults
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.setToDefaultPosition

class ControllerSettingsScreen(
    game: MegamanMaverickGame, private val buttons: Buttons, var isKeyboardSettings: Boolean
) : AbstractMenuScreen(game, BACK) {

    companion object {
        const val TAG = "ControllerSettingsScreen"
        private const val BACK = "BACK TO MAIN MENU"
        private const val RESET_TO_DEFAULTS = "RESET TO DEFAULTS"
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
            buttons.values().forEach { if (it.keyboardCode == keycode) it.keyboardCode = button.keyboardCode }
            button.keyboardCode = keycode

            // save the keyboard codes to preferences
            val keyboardPreferences = ControllerUtils.getKeyboardPreferences()
            buttons.forEach {
                val keyboardCode = it.value.keyboardCode
                keyboardPreferences.putInteger((it.key as ControllerButton).name, keyboardCode)
            }
            keyboardPreferences.flush()

            Gdx.input.inputProcessor = null
            selectedButton = null

            delayOnChangeTimer.reset()
            return true
        }
    }
    private val buttonListener = object : ControllerAdapter() {
        override fun buttonDown(controller: Controller, buttonIndex: Int): Boolean {
            if (selectedButton == null) return true

            val button = buttons.get(selectedButton!!)
            GameLogger.debug(
                TAG, "Setting [$selectedButton] controller code from [${button.controllerCode}] to [$buttonIndex]"
            )

            // if any buttons match the controller code, then switch them
            buttons.values().forEach { if (it.controllerCode == buttonIndex) it.controllerCode = button.controllerCode }
            button.controllerCode = buttonIndex

            // save the controller codes to preferences
            val controllerPreferences = ControllerUtils.getControllerPreferences(controller)
            buttons.forEach {
                val controllerCode = it.value.controllerCode ?: return@forEach
                controllerPreferences.putInteger((it.key as ControllerButton).name, controllerCode)
            }
            controllerPreferences.flush()

            controller.removeListener(this)
            selectedButton = null

            delayOnChangeTimer.reset()
            return super.buttonUp(controller, buttonIndex)
        }
    }
    private val controller: Controller?
        get() = ControllerUtils.getController()
    private val hintFontHandle: BitmapFontHandle
    private val fontHandles = Array<BitmapFontHandle>()
    private val blinkingArrow: BlinkingArrow
    private var selectedButton: ControllerButton? = null

    init {
        hintFontHandle = BitmapFontHandle(
            {
                "Press any ${if (isKeyboardSettings) "keyboard key" else "controller button"} to set \na new code for" +
                        " the button: \n$selectedButton"
            },
            getDefaultFontSize(),
            Vector2(
                ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
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
        fontHandles.add(backFontHandle)

        menuButtons.put(BACK, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> ControllerButton.B.name
                Direction.DOWN -> RESET_TO_DEFAULTS
                else -> null
            }
        })

        row -= 1f
        val resetToDefaultsFontHandle = BitmapFontHandle(
            RESET_TO_DEFAULTS,
            getDefaultFontSize(),
            Vector2(3f * ConstVals.PPM, row * ConstVals.PPM),
            centerX = false,
            centerY = false,
            fontSource = "Megaman10Font.ttf"
        )
        fontHandles.add(resetToDefaultsFontHandle)

        menuButtons.put(RESET_TO_DEFAULTS, object : IMenuButton {
            override fun onSelect(delta: Float): Boolean {
                ControllerUtils.resetToDefaults(buttons, isKeyboardSettings)
                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> BACK
                Direction.DOWN -> ControllerButton.START.name
                else -> null
            }
        })

        ControllerButton.values().forEach { controllerButton ->
            row -= 1f
            val codeHintSupplier = {
                val button = buttons.get(controllerButton)
                val code = if (isKeyboardSettings) button.keyboardCode else button.controllerCode
                "${controllerButton.name}: $code"
            }
            val buttonFontHandle = BitmapFontHandle(
                { codeHintSupplier() },
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
                    if (isKeyboardSettings) Gdx.input.inputProcessor = keyboardListener
                    else {
                        if (controller == null) return false
                        controller!!.addListener(buttonListener)
                    }
                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> {
                        val index = controllerButton.ordinal - 1
                        if (index < 0) RESET_TO_DEFAULTS else ControllerButton.values()[index].name
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

    override fun onAnySelection() {
        super.onAnySelection()
        castGame.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
    }

    override fun onAnyMovement() {
        super.onAnyMovement()
        castGame.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)
    }

    override fun render(delta: Float) {
        if (!isKeyboardSettings && controller == null) {
            GameLogger.error(TAG, "No controller found")
            castGame.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
            return
        }

        super.render(delta)

        delayOnChangeTimer.update(delta)
        if (delayOnChangeTimer.isJustFinished()) undoSelection()

        val arrowY =
            when (currentButtonKey) {
                BACK -> 10.6f
                RESET_TO_DEFAULTS -> 9.6f
                else -> 9.6f - (ControllerButton.valueOf(currentButtonKey).ordinal + 1)
            }
        blinkingArrow.center = Vector2(2.5f * ConstVals.PPM, arrowY * ConstVals.PPM)
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = castGame.getUiCamera().combined
        game.batch.begin()

        if (selectedButton != null) hintFontHandle.draw(game.batch)
        else {
            blinkingArrow.draw(game.batch)
            fontHandles.forEach { it.draw(game.batch) }
        }
        game.batch.end()
    }
}