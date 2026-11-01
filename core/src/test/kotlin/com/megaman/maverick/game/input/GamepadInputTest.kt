package com.megaman.maverick.game.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerMapping
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.controllers.ControllerUtils
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.players.FakePreferencesFiles
import com.megaman.maverick.game.players.PlayerSettings
import com.megaman.maverick.game.players.PlayerSettingsStore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

private const val SLOT_0_FILE = "Megaman Maverick - Player Settings - Slot 0"

class GamepadInputTest : DescribeSpec({

    lateinit var files: FakePreferencesFiles
    lateinit var store: PlayerSettingsStore

    fun PlayerSettings.codeOf(button: MegaControllerButton) = padCodes[button.index]

    fun saveTable(padName: String, firstCode: Int) {
        val table = PlayerSettings()
        MegaControllerButton.entries.forEach { table.padCodes[it.index] = firstCode + it.ordinal }
        store.saveGamepad(padName, table)
    }

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

    fun mockControllerWithMapping(padName: String = "TestController"): Controller {
        val mapping = testMapping()

        val controller = mockk<Controller>(relaxed = true)
        every { controller.mapping } returns mapping
        every { controller.name } returns padName
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
        files = FakePreferencesFiles()
        files.install()
        store = PlayerSettingsStore(PlayerSlot(0))
        mockkObject(ControllerUtils)
        mockkStatic("com.megaman.maverick.game.controllers.ControllerMappingExtensionsKt")
    }

    afterEach { unmockkAll() }

    describe("on first run with controller connected") {
        it("loads default controller mapping when 'loadPrefsOnConnect' is false") {
            val controller = mockControllerWithMapping()

            connect(controller)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            gamepad.held()

            settings.codeOf(MegaControllerButton.UP) shouldBe 10
            settings.codeOf(MegaControllerButton.DOWN) shouldBe 11
            settings.codeOf(MegaControllerButton.LEFT) shouldBe 12
            settings.codeOf(MegaControllerButton.RIGHT) shouldBe 13
            settings.codeOf(MegaControllerButton.A) shouldBe 21
            settings.codeOf(MegaControllerButton.B) shouldBe 22
            settings.codeOf(MegaControllerButton.START) shouldBe 23
            settings.codeOf(MegaControllerButton.SELECT) shouldBe 20
        }

        it("loads preferred codes when 'loadPrefsOnConnect' is true and prefs are present") {
            val controller = mockControllerWithMapping()
            connect(controller)

            saveTable("TestController", 100)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            gamepad.held()

            MegaControllerButton.entries.forEach { btn ->
                settings.codeOf(btn) shouldBe (100 + btn.ordinal)
            }
        }

        it("falls back to default mapping when prefs lookup returns null") {
            val controller = mockControllerWithMapping()

            connect(controller)

            saveTable("OtherController", 100)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            gamepad.held()

            // matches the default-mapping case
            settings.codeOf(MegaControllerButton.UP) shouldBe 10
            settings.codeOf(MegaControllerButton.A) shouldBe 21
            settings.codeOf(MegaControllerButton.SELECT) shouldBe 20
        }

        it("loads the mapping, not code 0, when 'loadPrefsOnConnect' is true and the prefs file has no keys") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            gamepad.held()

            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }
    }

    describe("on disconnect after being connected") {
        it("clears every code back to NO_CODE") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            // connect
            gamepad.held()
            settings.codeOf(MegaControllerButton.UP) shouldBe 10

            // disconnect
            connect()

            gamepad.held()

            MegaControllerButton.entries.forEach { btn ->
                settings.codeOf(btn) shouldBe PlayerSettings.NO_CODE
            }
        }
    }

    describe("when controller stays connected across runs") {
        it("does not re-apply the mapping on subsequent runs") {
            val controller = mockControllerWithMapping()

            connect(controller)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            gamepad.held()

            // simulate a user remap between frames
            settings.padCodes[MegaControllerButton.UP.index] = 999

            gamepad.held()

            settings.codeOf(MegaControllerButton.UP) shouldBe 999
        }

        it("keeps the codes while the primary pad is the same object over many ticks") {
            val controller = mockControllerWithMapping()
            connect(controller)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            repeat(10) { tick ->
                gamepad.held()
                settings.codeOf(MegaControllerButton.UP) shouldBe (if (tick == 0) 10 else 900 + tick - 1)
                settings.padCodes[MegaControllerButton.UP.index] = 900 + tick
            }
        }
    }

    describe("held") {
        it("sets the bit of each button whose gamepad code is down, while connected") {
            val controller = mockControllerWithMapping()
            connect(controller)
            every { controller.getButton(21) } returns true
            every { controller.getButton(13) } returns true

            GamepadInput(PlayerSettings(), store).held() shouldBe
                ((1L shl MegaControllerButton.A.index) or (1L shl MegaControllerButton.RIGHT.index))
        }

        it("gives no bits while disconnected") {
            connect()

            val settings = PlayerSettings()
            settings.padCodes[MegaControllerButton.A.index] = 21

            GamepadInput(settings, store).held() shouldBe 0L
        }
    }

    describe("when controller stays disconnected") {
        it("leaves the codes untouched") {
            connect()

            val settings = PlayerSettings()
            settings.padCodes[MegaControllerButton.UP.index] = 7
            val gamepad = GamepadInput(settings, store)

            gamepad.held()

            settings.codeOf(MegaControllerButton.UP) shouldBe 7
        }
    }

    describe("with two pads") {
        it("sets the bit of a bound button that is down on the second pad") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            connect(padA, padB)
            every { padB.getButton(21) } returns true

            GamepadInput(PlayerSettings(), store).held() shouldBe bit(MegaControllerButton.A)
        }

        it("ORs the buttons of both pads into one mask, with one bit for a button down on both") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()
            connect(padA, padB)
            every { padA.getButton(21) } returns true
            every { padA.getButton(22) } returns true
            every { padB.getButton(13) } returns true
            every { padB.getButton(22) } returns true

            GamepadInput(PlayerSettings(), store).held() shouldBe
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

            GamepadInput(PlayerSettings(), store).held() shouldBe bit(MegaControllerButton.A)
        }

        it("reloads the codes from the prefs of the remaining pad when the first pad goes, with 'loadPrefsOnConnect'") {
            val padA = mockControllerWithMapping("PadA")
            val padB = mockControllerWithMapping("PadB")
            saveTable("PadA", 100)
            saveTable("PadB", 200)

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            connect(padA, padB)
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> settings.codeOf(btn) shouldBe (100 + btn.ordinal) }

            connect(padB)
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> settings.codeOf(btn) shouldBe (200 + btn.ordinal) }
        }

        it("reloads the codes from the mapping when the first pad goes, without 'loadPrefsOnConnect'") {
            val padA = mockControllerWithMapping()
            val padB = mockControllerWithMapping()

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            connect(padA, padB)
            gamepad.held()
            settings.padCodes[MegaControllerButton.UP.index] = 999
            settings.padCodes[MegaControllerButton.A.index] = 998

            connect(padB)
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }
    }

    describe("left stick") {
        it("gives LEFT and RIGHT on the X axis from 0.5") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val gamepad = GamepadInput(PlayerSettings(), store)

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
            val gamepad = GamepadInput(PlayerSettings(), store)

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

            GamepadInput(PlayerSettings(), store).held() shouldBe bit(MegaControllerButton.RIGHT, MegaControllerButton.UP)
        }

        it("gives one RIGHT bit for the stick and the bound d-pad RIGHT together") {
            val pad = mockControllerWithMapping()
            connect(pad)
            every { pad.getAxis(0) } returns 0.5f
            every { pad.getButton(code(MegaControllerButton.RIGHT)) } returns true

            GamepadInput(PlayerSettings(), store).held() shouldBe bit(MegaControllerButton.RIGHT)
        }

        it("gives the stick bits for a pad with no bound codes") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)
            gamepad.held()
            settings.padCodes.fill(PlayerSettings.NO_CODE)
            every { pad.getAxis(0) } returns -0.5f
            every { pad.getAxis(1) } returns 0.5f

            gamepad.held() shouldBe bit(MegaControllerButton.LEFT, MegaControllerButton.DOWN)
        }
    }

    describe("triggers") {
        it("holds a button bound to a trigger code when that trigger axis is at 0.5, not at 0.49") {
            val pad = mockControllerWithMapping()
            connect(pad)
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)
            gamepad.held()
            settings.padCodes[MegaControllerButton.A.index] = GamepadInput.TRIGGER_LEFT_CODE
            settings.padCodes[MegaControllerButton.B.index] = GamepadInput.TRIGGER_RIGHT_CODE

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
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)
            gamepad.held()
            settings.padCodes[MegaControllerButton.A.index] = GamepadInput.TRIGGER_LEFT_CODE
            settings.padCodes[MegaControllerButton.B.index] = GamepadInput.TRIGGER_RIGHT_CODE

            gamepad.held()

            verify(exactly = 0) { pad.getButton(GamepadInput.TRIGGER_LEFT_CODE) }
            verify(exactly = 0) { pad.getButton(GamepadInput.TRIGGER_RIGHT_CODE) }
        }
    }

    describe("on reconnect") {
        it("clears the codes when the list becomes empty and loads them again for a new pad object") {
            val pad = mockControllerWithMapping()
            val padAgain = mockControllerWithMapping()

            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            connect(pad)
            gamepad.held()

            connect()
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> settings.codeOf(btn) shouldBe PlayerSettings.NO_CODE }

            connect(padAgain)
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }
    }

    describe("replug") {
        it("gives the mapping, then the saved codes after the flag, and the mapping for a name with no table") {
            saveTable("TestController", 100)
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            connect(mockControllerWithMapping())
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }

            gamepad.loadPrefsOnConnect = true
            repeat(2) {
                connect(mockControllerWithMapping())
                gamepad.held()
                MegaControllerButton.entries.forEach { btn -> settings.codeOf(btn) shouldBe (100 + btn.ordinal) }
            }

            connect(mockControllerWithMapping("NoTable"))
            gamepad.held()
            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }

        it("gives the mapping for a new pad object after the gamepad reset, with the flag") {
            saveTable("TestController", 100)
            store.clearGamepad("TestController")
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            connect(mockControllerWithMapping())
            gamepad.held()

            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }

        it("gives the mapping for a new pad object after clear(), with the flag") {
            saveTable("TestController", 100)
            store.clear()
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            connect(mockControllerWithMapping())
            gamepad.held()

            defaultMappingByButton.forEach { (btn, code) -> settings.codeOf(btn) shouldBe code }
        }

        it("starts with 'loadPrefsOnConnect' false") {
            GamepadInput(PlayerSettings(), store).loadPrefsOnConnect shouldBe false
        }

        it("gives the saved code of each saved key and the mapping of each missing key") {
            saveTable("TestController", 100)
            files.file(SLOT_0_FILE).remove("GAMEPAD.TestController.A")
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            connect(mockControllerWithMapping())
            gamepad.held()

            settings.codeOf(MegaControllerButton.A) shouldBe code(MegaControllerButton.A)
            settings.codeOf(MegaControllerButton.B) shouldBe (100 + MegaControllerButton.B.ordinal)
        }

        it("keeps a saved code 0") {
            files.file(SLOT_0_FILE).put("GAMEPAD.TestController.A", "0")
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store).apply { loadPrefsOnConnect = true }

            connect(mockControllerWithMapping())
            gamepad.held()

            settings.codeOf(MegaControllerButton.A) shouldBe 0
        }

        it("reads the axis, not 'getButton', for a saved trigger code") {
            files.file(SLOT_0_FILE).put("GAMEPAD.TestController.A", GamepadInput.TRIGGER_LEFT_CODE.toString())
            val pad = mockControllerWithMapping()
            every { pad.getAxis(GamepadInput.TRIGGER_LEFT_AXIS) } returns 1f
            val gamepad = GamepadInput(PlayerSettings(), store).apply { loadPrefsOnConnect = true }

            connect(pad)

            gamepad.held() shouldBe bit(MegaControllerButton.A)
            verify(exactly = 0) { pad.getButton(GamepadInput.TRIGGER_LEFT_CODE) }
        }

        it("has every code as NO_CODE with no pad, and a NO_CODE never reaches 'getButton'") {
            val settings = PlayerSettings()
            val gamepad = GamepadInput(settings, store)

            connect()
            gamepad.held()
            MegaControllerButton.entries.forEach { btn -> settings.codeOf(btn) shouldBe PlayerSettings.NO_CODE }

            val pad = mockControllerWithMapping()
            connect(pad)
            gamepad.held()
            settings.padCodes.fill(PlayerSettings.NO_CODE)
            gamepad.held()

            verify(exactly = 0) { pad.getButton(PlayerSettings.NO_CODE) }
        }
    }
})
