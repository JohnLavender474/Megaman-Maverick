package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Initializable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.controllers.ControllerUtils.getController
import com.maverick.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.*
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.input.GamepadInput
import com.megaman.maverick.game.players.KeyCapture
import com.megaman.maverick.game.players.PlayerSettings
import com.megaman.maverick.game.players.PlayerSettingsStore
import com.megaman.maverick.game.players.keyLabel
import com.megaman.maverick.game.players.padLabel
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition

class ControllerSettingsScreen(
    game: MegamanMaverickGame,
    private val settings: PlayerSettings,
    private val store: PlayerSettingsStore,
    var isKeyboardSettings: Boolean,
    var backAction: () -> Boolean = backAction@{
        game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
        return@backAction true
    },
) : MegaMenuScreen(game, BACK), Initializable {

    companion object {
        const val TAG = "ControllerSettingsScreen"
        private const val BACK = "BACK"
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
    private var captureRefused = false
    private var capturePad: Controller? = null

    override fun init(vararg params: Any) {
        if (initialized) return
        initialized = true

        keyboardListener = object : InputAdapter() {

            override fun keyDown(keycode: Int): Boolean {
                if (selectedMegaButton == null) return true

                val capture = settings.captureKey(selectedMegaButton!!, keycode)
                GameLogger.debug(TAG, "Capture of [$selectedMegaButton] with keycode [$keycode]: $capture")

                if (capture == KeyCapture.REFUSE) {
                    game.audio.playSound(SoundAsset.ERROR_SOUND, false)
                    captureRefused = true
                    return true
                }

                // save the keyboard codes to preferences
                if (capture == KeyCapture.ACCEPT) try {
                    store.saveKeyboard(settings)
                } catch (e: Exception) {
                    GameLogger.error(TAG, "Failed to save controller preferences.", e)
                }

                endCapture()
                delayOnChangeTimer.reset()

                return true
            }
        }

        buttonListener = object : ControllerAdapter() {

            override fun buttonDown(controller: Controller, buttonIndex: Int) = setControllerCode(controller, buttonIndex)

            override fun axisMoved(controller: Controller, axisIndex: Int, value: Float): Boolean {
                val code = when (axisIndex) {
                    GamepadInput.TRIGGER_LEFT_AXIS -> GamepadInput.TRIGGER_LEFT_CODE
                    GamepadInput.TRIGGER_RIGHT_AXIS -> GamepadInput.TRIGGER_RIGHT_CODE
                    else -> return false
                }
                if (value < GamepadInput.TRIGGER_THRESHOLD) return false
                return setControllerCode(controller, code)
            }

            private fun setControllerCode(controller: Controller, code: Int): Boolean {
                if (selectedMegaButton == null) {
                    GameLogger.error(TAG, "Selected button is null")
                    return true
                }

                val codes = settings.padCodes
                val index = selectedMegaButton!!.index
                val oldCode = codes[index]
                GameLogger.debug(TAG, "Setting [$selectedMegaButton] controller code from [$oldCode] to [$code]")

                // if any buttons match the controller code, then switch them
                for (i in codes.indices) if (codes[i] == code) codes[i] = oldCode
                codes[index] = code

                // save the controller codes to preferences
                try {
                    store.saveGamepad(controller.name, settings)
                } catch (e: Exception) {
                    GameLogger.error(TAG, "Failed to save keyboard preferences.", e)
                }

                endCapture()
                delayOnChangeTimer.reset()

                return true
            }
        }

        hintFontHandle = MegaFontHandle(
            {
                "Press any ${if (isKeyboardSettings) "keyboard key" else "controller\nbutton or trigger"}\n" +
                    "to set a new code for\nthe button: $selectedMegaButton" +
                    if (captureRefused) "\nRefused: reserved key" else ""
            },
            positionX =
                ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f,
            positionY = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f,
            centerX = true,
            centerY = true,
        )

        var row = 12.75f
        blinkingArrow = BlinkingArrow(game.assets, Vector2(2.5f * ConstVals.PPM, row * ConstVals.PPM))

        val backFontHandle = MegaFontHandle(
            { BACK }, positionX = 3f * ConstVals.PPM, positionY = row * ConstVals.PPM, centerX = false, centerY = false
        )
        fontHandles.add(backFontHandle)

        buttons.put(BACK, object : IMenuButton {

            override fun onSelect(delta: Float) = backAction.invoke()

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> SELECT_ACTION
                Direction.DOWN -> LOAD_SAVED_SETTINGS
                else -> BACK
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
                    store.loadKeyboard(settings)
                } else {
                    if (controller == null) {
                        GameLogger.error(TAG, "Controller is null, cannot load saved settings")
                        return false
                    }

                    store.loadGamepad(controller!!.name, settings)

                    game.gamepadInput.loadPrefsOnConnect = true
                }

                store.loadSelectButtonAction()?.let { settings.selectButtonAction = it }

                game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)

                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> BACK
                Direction.DOWN -> RESET_TO_DEFAULTS
                else -> LOAD_SAVED_SETTINGS
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
                val pad = controller
                if (isKeyboardSettings) MegaControllerButton.entries.forEach {
                    settings.keyCodes[it.index] = it.defaultKeyboardKey
                } else if (pad != null) MegaControllerButton.entries.forEach {
                    settings.padCodes[it.index] = pad.mapping.getMapping(it)
                }

                try {
                    if (isKeyboardSettings) store.clearKeyboard() else if (pad != null) store.clearGamepad(pad.name)
                } catch (e: Exception) {
                    GameLogger.error(TAG, "Failed to clear the saved settings.", e)
                }

                settings.selectButtonAction = SelectButtonAction.NEXT_WEAPON
                game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = when (direction) {
                Direction.UP -> LOAD_SAVED_SETTINGS
                Direction.DOWN -> MegaControllerButton.entries[0].name
                else -> RESET_TO_DEFAULTS
            }
        }
        )

        MegaControllerButton.entries.forEach { b ->
            row -= 1f

            val codeHintSupplier = {
                val label = when {
                    isKeyboardSettings -> keyLabel(settings.keyCodes[b.index])
                    else -> padLabel(settings.padCodes[b.index])
                }
                "${b.name}: $label"
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
                    captureRefused = false

                    if (isKeyboardSettings) {
                        oldInputProcessor = Gdx.input.inputProcessor
                        Gdx.input.inputProcessor = keyboardListener
                    } else {
                        if (controller == null) return false
                        actionOnNextUpdate = {
                            capturePad = controller
                            capturePad!!.addListener(buttonListener)
                        }
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

                    else -> b.name
                }
            })
        }

        row -= 1f

        val selectActionTextSupplier = {
            "${SELECT_ACTION}: ${settings.selectButtonAction.text}"
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
                    settings.selectButtonAction = settings.selectButtonAction.previous()
                    store.saveSelectButtonAction(settings)
                    SELECT_ACTION
                }

                Direction.RIGHT -> {
                    settings.selectButtonAction = settings.selectButtonAction.next()
                    store.saveSelectButtonAction(settings)
                    SELECT_ACTION
                }
            }
        })
    }

    override fun show(props: Properties) {
        val overPause = props.containsKey("${ConstKeys.BACK}_${ConstKeys.ACTION}")
        if (overPause) backAction = props.get("${ConstKeys.BACK}_${ConstKeys.ACTION}") as () -> Boolean
        show()
        if (!overPause) game.audio.stopMusic()
    }

    override fun show() {
        GameLogger.debug(TAG, "show()")
        if (!initialized) init()
        super.show()
        game.getUiCamera().setToDefaultPosition()
    }

    override fun onBack(delta: Float) {
        selectionMade = buttons[BACK].onSelect(delta)
        if (selectionMade) onAnySelection()
    }

    override fun onAnySelection() {
        GameLogger.debug(TAG, "onAnySelection()")
        game.audio.playSound(SoundAsset.SELECT_PING_SOUND, false)
    }

    override fun onAnyMovement(direction: Direction) {
        GameLogger.debug(TAG, "onAnyMovement(): direction=$direction")
        game.audio.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)
    }

    override fun render(delta: Float) {
        if (!isKeyboardSettings && controller == null) {
            GameLogger.error(TAG, "No controller found")
            game.audio.playSound(SoundAsset.ERROR_SOUND, false)
            backAction.invoke()
            return
        }

        if (capturePad != null && capturePad !== controller) {
            endCapture()
            game.audio.playSound(SoundAsset.ERROR_SOUND, false)
            delayOnChangeTimer.reset()
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

    private fun endCapture() {
        capturePad?.removeListener(buttonListener)
        capturePad = null
        if (Gdx.input.inputProcessor === keyboardListener) Gdx.input.inputProcessor = oldInputProcessor
        selectedMegaButton = null
        actionOnNextUpdate = null
        captureRefused = false
    }

    override fun reset() {
        super.reset()
        endCapture()
        backAction = backAction@{
            game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
            return@backAction true
        }
    }
}
