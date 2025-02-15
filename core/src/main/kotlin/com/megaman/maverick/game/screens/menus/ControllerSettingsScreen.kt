package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.getControllerPreferences
import com.megaman.maverick.game.controllers.getKeyboardPreferences
import com.megaman.maverick.game.controllers.resetToDefaults
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class ControllerSettingsScreen(
    game: MegamanMaverickGame,
    private val controllerButtons: ControllerButtons,
    var isKeyboardSettings: Boolean,
    var backAction: () -> Boolean = backAction@{
        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        return@backAction true
    }
) : MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "ControllerSettingsScreen"
        private const val BACK = "BACK TO MAIN MENU"
        private const val RESET_TO_DEFAULTS = "RESET TO DEFAULTS"
        private const val DELAY_ON_CHANGE = 0.25f
    }

    private val delayOnChangeTimer = Timer(DELAY_ON_CHANGE)
    private val controller: Controller?
        get() = ControllerUtils.getController()
    private val fontHandles = Array<MegaFontHandle>()

    private lateinit var keyboardListener: InputAdapter
    private lateinit var buttonListener: ControllerAdapter
    private lateinit var hintFontHandle: MegaFontHandle
    private lateinit var blinkingArrow: BlinkingArrow

    private var selectedMegaButton: MegaControllerButton? = null
    private var oldInputProcessor: InputProcessor? = null
    private var initialized = false

    private var actionOnNextUpdate: (() -> Unit)? = null

    override fun init() {
        if (initialized) return
        initialized = true

        keyboardListener = object : InputAdapter() {

            override fun keyDown(keycode: Int): Boolean {
                if (selectedMegaButton == null) return true

                val button = controllerButtons.get(selectedMegaButton!!) as ControllerButton
                GameLogger.debug(
                    TAG,
                    "Setting [$selectedMegaButton] keycode from [${button.keyboardCode}] to [$keycode]"
                )

                // if any buttons match the keycode, then switch them
                controllerButtons.values().forEach {
                    it as ControllerButton
                    if (it.keyboardCode == keycode) it.keyboardCode = button.keyboardCode
                }
                button.keyboardCode = keycode

                // save the keyboard codes to preferences
                val keyboardPreferences = getKeyboardPreferences()
                controllerButtons.forEach {
                    val keyboardCode = (it.value as ControllerButton).keyboardCode
                    keyboardPreferences.putInteger((it.key as MegaControllerButton).name, keyboardCode)
                }
                keyboardPreferences.flush()

                Gdx.input.inputProcessor = oldInputProcessor
                selectedMegaButton = null

                delayOnChangeTimer.reset()

                return true
            }
        }

        buttonListener = object : ControllerAdapter() {

            override fun buttonDown(controller: Controller, buttonIndex: Int): Boolean {
                if (selectedMegaButton == null) return true

                val button = controllerButtons.get(selectedMegaButton!!) as ControllerButton
                GameLogger.debug(
                    TAG,
                    "Setting [$selectedMegaButton] controller code from [${button.controllerCode}] to [$buttonIndex]"
                )

                // if any buttons match the controller code, then switch them
                controllerButtons.values().forEach {
                    it as ControllerButton
                    if (it.controllerCode == buttonIndex) it.controllerCode = button.controllerCode
                }
                button.controllerCode = buttonIndex

                // save the controller codes to preferences
                val controllerPreferences = getControllerPreferences(controller)
                controllerButtons.forEach {
                    val button = it.value as ControllerButton
                    val controllerCode = button.controllerCode ?: return@forEach
                    controllerPreferences.putInteger((it.key as MegaControllerButton).name, controllerCode)
                }
                controllerPreferences.flush()

                controller.removeListener(this)
                selectedMegaButton = null

                delayOnChangeTimer.reset()

                return true
            }
        }

        hintFontHandle = MegaFontHandle(
            {
                "Press any ${if (isKeyboardSettings) "keyboard key" else "controller button"}\n" +
                    "to set a new code for\nthe button: $selectedMegaButton"
            },
            positionX =
                ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
            centerX = true,
            centerY = true,
        )

        var row = 10.75f
        blinkingArrow = BlinkingArrow(game.assMan, Vector2(2.5f * ConstVals.PPM, row * ConstVals.PPM))

        val backFontHandle = MegaFontHandle(
            { BACK }, positionX = 3f * ConstVals.PPM, positionY = row * ConstVals.PPM, centerX = false, centerY = false
        )
        fontHandles.add(backFontHandle)

        buttons.put(BACK, object : IMenuButton {

            override fun onSelect(delta: Float) = backAction.invoke()

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MegaControllerButton.B.name
                Direction.DOWN -> RESET_TO_DEFAULTS
                else -> null
            }
        })

        row -= 1f
        val resetToDefaultsFontHandle = MegaFontHandle(
            { RESET_TO_DEFAULTS },
            positionX = 3f * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false,
            centerY = false,
        )
        fontHandles.add(resetToDefaultsFontHandle)

        buttons.put(RESET_TO_DEFAULTS, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                ControllerUtils.resetToDefaults(controllerButtons, isKeyboardSettings)
                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> BACK
                Direction.DOWN -> MegaControllerButton.START.name
                else -> null
            }
        })

        MegaControllerButton.entries.forEach { b ->
            row -= 1f
            val codeHintSupplier = {
                val button = controllerButtons.get(b) as ControllerButton
                val code = if (isKeyboardSettings) button.keyboardCode else button.controllerCode
                "${b.name}: $code"
            }
            val buttonFontHandle = MegaFontHandle(
                { codeHintSupplier() },
                positionX = 3f * ConstVals.PPM,
                positionY = row * ConstVals.PPM,
                centerX = false,
                centerY = false
            )
            fontHandles.add(buttonFontHandle)

            buttons.put(b.name, object : IMenuButton {

                override fun onSelect(delta: Float): Boolean {
                    selectedMegaButton = b

                    if (isKeyboardSettings) {
                        oldInputProcessor = Gdx.input.inputProcessor
                        Gdx.input.inputProcessor = keyboardListener
                    } else {
                        if (controller == null) return false
                        actionOnNextUpdate = { controller!!.addListener(buttonListener) }
                    }

                    return true
                }

                override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                    Direction.UP -> {
                        val index = b.ordinal - 1
                        if (index < 0) RESET_TO_DEFAULTS else MegaControllerButton.entries[index].name
                    }

                    Direction.DOWN -> {
                        val index = b.ordinal + 1
                        if (index >= MegaControllerButton.entries.size) BACK else MegaControllerButton.entries[index].name
                    }

                    else -> null
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
        if (!isKeyboardSettings && controller == null) {
            GameLogger.error(TAG, "No controller found")
            game.audioMan.playSound(SoundAsset.ERROR_SOUND, false)
            backAction.invoke()
            return
        }

        if (actionOnNextUpdate != null) {
            actionOnNextUpdate!!.invoke()
            actionOnNextUpdate = null
        }

        super.render(delta)

        delayOnChangeTimer.update(delta)
        if (delayOnChangeTimer.isJustFinished()) undoSelection()

        val arrowY = when (currentButtonKey) {
            BACK -> 10.6f
            RESET_TO_DEFAULTS -> 9.6f
            else -> 9.6f - (MegaControllerButton.valueOf(currentButtonKey!!).ordinal + 1)
        }
        blinkingArrow.centerX = 2.5f * ConstVals.PPM
        blinkingArrow.centerY = arrowY * ConstVals.PPM
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        if (selectedMegaButton != null) hintFontHandle.draw(game.batch)
        else {
            blinkingArrow.draw(game.batch)
            fontHandles.forEach { it.draw(game.batch) }
        }
        game.batch.end()
    }
}
