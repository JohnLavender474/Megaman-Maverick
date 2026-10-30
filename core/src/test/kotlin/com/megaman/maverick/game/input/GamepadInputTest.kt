package com.megaman.maverick.game.input

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerMapping
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.controllers.ControllerUtils
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.getPreferredControllerCode
import com.maverick.game.engine.controller.buttons.ControllerButton
import com.maverick.game.engine.controller.buttons.ControllerButtons
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class GamepadInputTest : DescribeSpec({

    fun buildButtons(): ControllerButtons {
        val buttons = ControllerButtons()
        MegaControllerButton.entries.forEach {
            buttons.put(it, ControllerButton(it.defaultKeyboardKey))
        }
        return buttons
    }

    fun ControllerButtons.codeOf(button: MegaControllerButton): Int? =
        (get(button)).controllerCode

    val defaultMappingByButton = mapOf(
        MegaControllerButton.UP to 10,
        MegaControllerButton.DOWN to 11,
        MegaControllerButton.LEFT to 12,
        MegaControllerButton.RIGHT to 13,
        MegaControllerButton.A to 21,
        MegaControllerButton.B to 22,
        MegaControllerButton.START to 23,
        MegaControllerButton.SELECT to 20,
    )

    fun code(button: MegaControllerButton) = defaultMappingByButton.getValue(button)

    val undefined = ControllerMapping.UNDEFINED

    // ControllerMapping.axisLeftX and axisLeftY are public final Java fields, not getters, so MockK cannot stub them.
    // This subclass sets them (and the button fields) through the protected constructor.
    fun testMapping(): ControllerMapping = object : ControllerMapping(
        0, 1, undefined, undefined,
        code(MegaControllerButton.SELECT), code(MegaControllerButton.A), undefined, code(MegaControllerButton.B),
        undefined, code(MegaControllerButton.START),
        undefined, undefined, undefined, undefined,
        undefined, undefined,
        code(MegaControllerButton.UP), code(MegaControllerButton.DOWN),
        code(MegaControllerButton.LEFT), code(MegaControllerButton.RIGHT),
    ) {}

    fun mockControllerWithMapping(): Controller {
        val mapping = testMapping()

        val controller = mockk<Controller>(relaxed = true)
        every { controller.mapping } returns mapping
        every { controller.name } returns "TestController"
        every { controller.getAxis(any()) } returns 0f

        return controller
    }

    fun connect(vararg pads: Controller) {
        val list = Array<Controller>()
        pads.forEach { list.add(it) }
        every { ControllerUtils.getControllers() } returns list
    }

    fun bit(vararg buttons: MegaControllerButton) = buttons.fold(0L) { mask, it -> mask or (1L shl it.index) }

    beforeEach {
        mockkObject(ControllerUtils)
        mockkStatic("com.megaman.maverick.game.controllers.ControllerUtilsExtensionsKt")
        mockkStatic("com.megaman.maverick.game.controllers.ControllerMappingExtensionsKt")
    }

    afterEach { unmockkAll() }

    describe("on first run with controller connected") {
        it("loads default controller mapping when 'loadPrefsOnConnect' is false") {
            val controller = mockControllerWithMapping()

            connect(controller)

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            gamepad.held()

            buttons.codeOf(MegaControllerButton.UP) shouldBe 10
            buttons.codeOf(MegaControllerButton.DOWN) shouldBe 11
            buttons.codeOf(MegaControllerButton.LEFT) shouldBe 12
            buttons.codeOf(MegaControllerButton.RIGHT) shouldBe 13
            buttons.codeOf(MegaControllerButton.A) shouldBe 21
            buttons.codeOf(MegaControllerButton.B) shouldBe 22
            buttons.codeOf(MegaControllerButton.START) shouldBe 23
            buttons.codeOf(MegaControllerButton.SELECT) shouldBe 20
        }

        it("loads preferred codes when 'loadPrefsOnConnect' is true and prefs are present") {
            val controller = mockControllerWithMapping()
            connect(controller)

            MegaControllerButton.entries.forEach { btn ->
                every { getPreferredControllerCode(controller, btn) } returns 100 + btn.ordinal
            }

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons).apply { loadPrefsOnConnect = true }

            gamepad.held()

            MegaControllerButton.entries.forEach { btn ->
                buttons.codeOf(btn) shouldBe (100 + btn.ordinal)
            }
        }

        it("falls back to default mapping when prefs lookup returns null") {
            val controller = mockControllerWithMapping()

            connect(controller)

            MegaControllerButton.entries.forEach { btn ->
                every { getPreferredControllerCode(controller, btn) } returns null
            }

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons).apply { loadPrefsOnConnect = true }

            gamepad.held()

            // matches the default-mapping case
            buttons.codeOf(MegaControllerButton.UP) shouldBe 10
            buttons.codeOf(MegaControllerButton.A) shouldBe 21
            buttons.codeOf(MegaControllerButton.SELECT) shouldBe 20
        }

        it("loads the mapping, not code 0, when 'loadPrefsOnConnect' is true and the prefs file has no keys") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val prefs = mockk<Preferences>(relaxed = true) {
                every { contains(any()) } returns false
                every { getInteger(any()) } returns 0
            }
            Gdx.app = mockk<Application>(relaxed = true) {
                every { getPreferences(any()) } returns prefs
            }

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons).apply { loadPrefsOnConnect = true }

            gamepad.held()

            defaultMappingByButton.forEach { (btn, code) -> buttons.codeOf(btn) shouldBe code }
        }
    }

    describe("on disconnect after being connected") {
        it("clears every 'controllerCode' back to null") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            // connect
            gamepad.held()
            buttons.codeOf(MegaControllerButton.UP) shouldBe 10

            // disconnect
            connect()

            gamepad.held()

            MegaControllerButton.entries.forEach { btn ->
                buttons.codeOf(btn) shouldBe null
            }
        }
    }

    describe("when controller stays connected across runs") {
        it("does not re-apply the mapping on subsequent runs") {
            val controller = mockControllerWithMapping()

            connect(controller)

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            gamepad.held()

            // simulate a user remap between frames
            (buttons.get(MegaControllerButton.UP)).controllerCode = 999

            gamepad.held()

            buttons.codeOf(MegaControllerButton.UP) shouldBe 999
        }

        it("keeps the codes while the primary pad is the same object over many ticks") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            repeat(10) { tick ->
                gamepad.held()
                buttons.codeOf(MegaControllerButton.UP) shouldBe (if (tick == 0) 10 else 900 + tick - 1)
                buttons.get(MegaControllerButton.UP).controllerCode = 900 + tick
            }
        }
    }

    describe("held") {
        it("sets the bit of each button whose gamepad code is down, while connected") {
            val controller = mockControllerWithMapping()
            connect(controller)
            every { controller.getButton(21) } returns true
            every { controller.getButton(13) } returns true

            GamepadInput(buildButtons()).held() shouldBe
                ((1L shl MegaControllerButton.A.index) or (1L shl MegaControllerButton.RIGHT.index))
        }

        it("gives no bits while disconnected") {
            connect()

            val buttons = buildButtons()
            buttons.get(MegaControllerButton.A).controllerCode = 21

            GamepadInput(buttons).held() shouldBe 0L
        }
    }

    describe("when controller stays disconnected") {
        it("leaves 'controllerCodes' untouched") {
            connect()

            val buttons = buildButtons()
            (buttons.get(MegaControllerButton.UP)).controllerCode = 7
            val gamepad = GamepadInput(buttons)

            gamepad.held()

            buttons.codeOf(MegaControllerButton.UP) shouldBe 7
        }
    }

    describe("with two pads") {
        it("sets the bit of a bound button that is down on the second pad") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            connect(padA, padB)
            every { padB.getButton(21) } returns true

            GamepadInput(buildButtons()).held() shouldBe bit(MegaControllerButton.A)
        }

        it("ORs the buttons of both pads into one mask, with one bit for a button down on both") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            connect(padA, padB)
            every { padA.getButton(21) } returns true
            every { padA.getButton(22) } returns true
            every { padB.getButton(13) } returns true
            every { padB.getButton(22) } returns true

            GamepadInput(buildButtons()).held() shouldBe
                bit(MegaControllerButton.A, MegaControllerButton.B, MegaControllerButton.RIGHT)
        }

        it("reads every pad of the tick when a pad leaves the list during the read") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            val list = Array<Controller>()
            list.add(padA)
            list.add(padB)
            every { ControllerUtils.getControllers() } returns list
            every { padA.getButton(10) } answers {
                list.removeValue(padA, true)
                false
            }
            every { padB.getButton(21) } returns true

            GamepadInput(buildButtons()).held() shouldBe bit(MegaControllerButton.A)
        }

        it("reloads the codes from the prefs of the remaining pad when the first pad goes, with 'loadPrefsOnConnect'") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            MegaControllerButton.entries.forEach { btn ->
                every { getPreferredControllerCode(padA, btn) } returns 100 + btn.ordinal
                every { getPreferredControllerCode(padB, btn) } returns 200 + btn.ordinal
            }

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons).apply { loadPrefsOnConnect = true }

            connect(padA, padB)
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> buttons.codeOf(btn) shouldBe (100 + btn.ordinal) }

            connect(padB)
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> buttons.codeOf(btn) shouldBe (200 + btn.ordinal) }
        }

        it("reloads the codes from the mapping when the first pad goes, without 'loadPrefsOnConnect'") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            connect(padA, padB)
            gamepad.held()
            buttons.get(MegaControllerButton.UP).controllerCode = 999
            buttons.get(MegaControllerButton.A).controllerCode = 998

            connect(padB)
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> buttons.codeOf(btn) shouldBe code }
        }
    }

    describe("left stick") {
        it("gives LEFT and RIGHT on the X axis from 0.5") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val gamepad = GamepadInput(buildButtons())

            listOf(
                0.49f to 0L,
                0.5f to bit(MegaControllerButton.RIGHT),
                -0.49f to 0L,
                -0.5f to bit(MegaControllerButton.LEFT),
            ).forEach { (x, expected) ->
                every { pad.getAxis(0) } returns x
                gamepad.held() shouldBe expected
            }
        }

        it("gives UP and DOWN on the Y axis from 0.5") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val gamepad = GamepadInput(buildButtons())

            listOf(
                -0.5f to bit(MegaControllerButton.UP),
                0.5f to bit(MegaControllerButton.DOWN),
                0.49f to 0L,
                -0.49f to 0L,
            ).forEach { (y, expected) ->
                every { pad.getAxis(1) } returns y
                gamepad.held() shouldBe expected
            }
        }

        it("gives RIGHT and UP for a diagonal") {
            val pad = mockControllerWithMapping()
            connect(pad)
            every { pad.getAxis(0) } returns 0.5f
            every { pad.getAxis(1) } returns -0.5f

            GamepadInput(buildButtons()).held() shouldBe bit(MegaControllerButton.RIGHT, MegaControllerButton.UP)
        }

        it("gives one RIGHT bit for the stick and the bound d-pad RIGHT together") {
            val pad = mockControllerWithMapping()
            connect(pad)
            every { pad.getAxis(0) } returns 0.5f
            every { pad.getButton(code(MegaControllerButton.RIGHT)) } returns true

            GamepadInput(buildButtons()).held() shouldBe bit(MegaControllerButton.RIGHT)
        }

        it("gives the stick bits for a pad with no bound codes") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)
            gamepad.held()
            buttons.forEach { it.value.controllerCode = null }
            every { pad.getAxis(0) } returns -0.5f
            every { pad.getAxis(1) } returns 0.5f

            gamepad.held() shouldBe bit(MegaControllerButton.LEFT, MegaControllerButton.DOWN)
        }
    }

    describe("triggers") {
        it("holds a button bound to a trigger code when that trigger axis is at 0.5, not at 0.49") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)
            gamepad.held()
            buttons.get(MegaControllerButton.A).controllerCode = GamepadInput.TRIGGER_LEFT_CODE
            buttons.get(MegaControllerButton.B).controllerCode = GamepadInput.TRIGGER_RIGHT_CODE

            every { pad.getAxis(GamepadInput.TRIGGER_LEFT_AXIS) } returns 0.49f
            gamepad.held() shouldBe 0L
            every { pad.getAxis(GamepadInput.TRIGGER_LEFT_AXIS) } returns 0.5f
            gamepad.held() shouldBe bit(MegaControllerButton.A)
            every { pad.getAxis(GamepadInput.TRIGGER_LEFT_AXIS) } returns 0f

            every { pad.getAxis(GamepadInput.TRIGGER_RIGHT_AXIS) } returns 0.49f
            gamepad.held() shouldBe 0L
            every { pad.getAxis(GamepadInput.TRIGGER_RIGHT_AXIS) } returns 0.5f
            gamepad.held() shouldBe bit(MegaControllerButton.B)
        }

        it("never gives a trigger code to 'getButton'") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)
            gamepad.held()
            buttons.get(MegaControllerButton.A).controllerCode = GamepadInput.TRIGGER_LEFT_CODE
            buttons.get(MegaControllerButton.B).controllerCode = GamepadInput.TRIGGER_RIGHT_CODE

            gamepad.held()

            verify(exactly = 0) { pad.getButton(GamepadInput.TRIGGER_LEFT_CODE) }
            verify(exactly = 0) { pad.getButton(GamepadInput.TRIGGER_RIGHT_CODE) }
        }
    }

    describe("on reconnect") {
        it("clears the codes when the list becomes empty and loads them again for a new pad object") {
            val pad = mockControllerWithMapping()
            val padAgain = mockControllerWithMapping()

            val buttons = buildButtons()
            val gamepad = GamepadInput(buttons)

            connect(pad)
            gamepad.held()

            connect()
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> buttons.codeOf(btn) shouldBe null }

            connect(padAgain)
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> buttons.codeOf(btn) shouldBe code }
        }
    }
})
