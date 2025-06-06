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
import com.mega.game.engine.controller.ControllerUtils.getController
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.controller.buttons.ControllerButtons
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.*
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
        private const val LOAD_SAVED_SETTINGS = "LOAD SAVED SETTINGS"
        private const val RESET_TO_DEFAULTS = "RESET TO DEFAULTS"
        private const val SELECT_ACTION = "SELECT ACTION"
        private const val DELAY_ON_CHANGE = 0.25f
    }

    private val delayOnChangeTimer = Timer(DELAY_ON_CHANGE)
    private val controller: Controller?
        get() = getController()
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
                ControllerUtils.saveSettingsToPrefs(controllerButtons, true)

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
                ControllerUtils.saveSettingsToPrefs(controllerButtons, false)

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

        var row = 12.75f
        blinkingArrow = BlinkingArrow(game.assMan, Vector2(2.5f * ConstVals.PPM, row * ConstVals.PPM))

        val backFontHandle = MegaFontHandle(
            { BACK }, positionX = 3f * ConstVals.PPM, positionY = row * ConstVals.PPM, centerX = false, centerY = false
        )
        fontHandles.add(backFontHandle)

        buttons.put(BACK, object : IMenuButton {

            override fun onSelect(delta: Float) = backAction.invoke()

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> SELECT_ACTION
                Direction.DOWN -> LOAD_SAVED_SETTINGS
                else -> null
            }
        })

        row -= 1f
        val loadSavedSettingsFontHandle = MegaFontHandle(
            LOAD_SAVED_SETTINGS,
            positionX = 3f * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false,
            centerY = false,
        )
        fontHandles.add(loadSavedSettingsFontHandle)

        buttons.put(LOAD_SAVED_SETTINGS, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                if (isKeyboardSettings) {
                    val keyboardPreferences = getKeyboardPreferences()

                    MegaControllerButton.entries.forEach {
                        controllerButtons[it]?.let { controllerButton ->
                            controllerButton as ControllerButton

                            val oldCode = controllerButton.keyboardCode
                            val newCode = keyboardPreferences.getInteger(it.name, oldCode)

                            controllerButton.keyboardCode = newCode

                            GameLogger.debug(TAG, "loadSavedSettings(): keyboard: oldCode=$oldCode, newCode=$newCode")
                        }
                    }
                } else {
                    if (controller == null) {
                        GameLogger.error(TAG, "Controller is null, cannot load saved settings")
                        return false
                    }

                    val controllerPreferences = getControllerPreferences(controller!!)

                    MegaControllerButton.entries.forEach {
                        controllerButtons[it]?.let { controllerButton ->
                            controllerButton as ControllerButton

                            var oldCode = controllerButton.controllerCode
                            if (oldCode == null) oldCode = controller!!.mapping.getMapping(it)
                            val newCode = controllerPreferences.getInteger(it.name, oldCode)

                            controllerButton.keyboardCode = newCode

                            GameLogger.debug(
                                TAG,
                                "loadSavedSettings(): keyboard: oldCode=$oldCode, newCode=$newCode"
                            )
                        }
                    }
                }

                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)

                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> BACK
                Direction.DOWN -> RESET_TO_DEFAULTS
                else -> null
            }
        })

        row -= 1f
        val resetToDefaultsFontHandle = MegaFontHandle(
            RESET_TO_DEFAULTS,
            positionX = 3f * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false,
            centerY = false,
        )
        fontHandles.add(resetToDefaultsFontHandle)

        buttons.put(RESET_TO_DEFAULTS, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                ControllerUtils.resetSettingsToDefaults(controllerButtons, isKeyboardSettings)
                game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> LOAD_SAVED_SETTINGS
                Direction.DOWN -> MegaControllerButton.entries[0].name
                else -> null
            }
        }
        )

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
                        when {
                            index < 0 -> RESET_TO_DEFAULTS
                            else -> MegaControllerButton.entries[index].name
                        }
                    }
                    Direction.DOWN -> {
                        val index = b.ordinal + 1
                        when {
                            index >= MegaControllerButton.entries.size -> SELECT_ACTION
                            else -> MegaControllerButton.entries[index].name
                        }
                    }
                    else -> null
                }
            })
        }

        row -= 1f

        val selectActionTextSupplier = {
            "${SELECT_ACTION}: ${game.selectButtonAction.text}"
        }
        val selectActionFontHandle = MegaFontHandle(
            selectActionTextSupplier,
            positionX = 3f * ConstVals.PPM,
            positionY = row * ConstVals.PPM,
            centerX = false,
            centerY = false,
        )
        fontHandles.addAll(selectActionFontHandle)

        buttons.put(SELECT_ACTION, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                onNavigate(Direction.RIGHT, delta)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> MegaControllerButton.SELECT.name
                Direction.DOWN -> BACK
                Direction.LEFT -> {
                    game.selectButtonAction = game.selectButtonAction.previous()
                    SELECT_ACTION
                }
                Direction.RIGHT -> {
                    game.selectButtonAction = game.selectButtonAction.next()
                    SELECT_ACTION
                }
            }
        })
    }

    override fun show() {
        GameLogger.debug(TAG, "show()")
        if (!initialized) init()
        super.show()
        game.audioMan.stopMusic()
        game.getUiCamera().setToDefaultPosition()
    }

    override fun onAnySelection() {
        GameLogger.debug(TAG, "onAnySelection()")
        game.audioMan.playSound(SoundAsset.SELECT_PING_SOUND, false)
    }

    override fun onAnyMovement(direction: Direction) {
        GameLogger.debug(TAG, "onAnyMovement(): direction=$direction")
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)
    }

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

        val arrowY = when (buttonKey) {
            BACK -> 12.6f
            LOAD_SAVED_SETTINGS -> 11.6f
            RESET_TO_DEFAULTS -> 10.6f
            SELECT_ACTION -> 1.6f
            else -> try {
                10.6f - (MegaControllerButton.valueOf(buttonKey!!).ordinal + 1)
            } catch (e: Exception) {
                throw Exception("Failed to set arrow Y position: buttonKey=$buttonKey", e)
            }
        }
        blinkingArrow.centerX = 2.5f * ConstVals.PPM
        blinkingArrow.centerY = arrowY * ConstVals.PPM
        blinkingArrow.update(delta)

        game.batch.projectionMatrix = game.getUiCamera().combined
        game.batch.begin()
        when {
            selectedMegaButton != null -> hintFontHandle.draw(game.batch)
            else -> {
                blinkingArrow.draw(game.batch)
                fontHandles.forEach { it.draw(game.batch) }
            }
        }
        game.batch.end()
    }
}
