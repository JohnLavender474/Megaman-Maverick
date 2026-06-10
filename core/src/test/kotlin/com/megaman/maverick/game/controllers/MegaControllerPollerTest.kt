package com.megaman.maverick.game.controllers

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerMapping
import com.mega.game.engine.controller.ControllerUtils
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.controller.buttons.ControllerButtons
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class MegaControllerPollerTest : DescribeSpec({

    fun buildButtons(): ControllerButtons {
        val buttons = ControllerButtons()
        MegaControllerButton.entries.forEach {
            // start with no controller code, disabled so super.run() skips isPressed()
            buttons.put(
                it,
                ControllerButton(
                    it.defaultKeyboardKey,
                    controllerCode = null,
                    enabled = false
                )
            )
        }
        return buttons
    }

    fun ControllerButtons.codeOf(button: MegaControllerButton): Int? =
        (get(button) as ControllerButton).controllerCode

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

    fun mockControllerWithMapping(): Controller {
        val mapping = mockk<ControllerMapping>(relaxed = true)

        val controller = mockk<Controller>(relaxed = true)
        every { controller.mapping } returns mapping
        every { controller.name } returns "TestController"

        defaultMappingByButton.forEach { (btn, code) ->
            every { mapping.getMapping(btn) } returns code
        }

        return controller
    }

    beforeEach {
        mockkObject(ControllerUtils)
        mockkStatic("com.megaman.maverick.game.controllers.ControllerUtilsExtensionsKt")
        mockkStatic("com.megaman.maverick.game.controllers.ControllerMappingExtensionsKt")
    }

    afterEach { unmockkAll() }

    describe("on first run with controller connected") {
        it("loads default controller mapping when 'loadPrefsOnConnect' is false") {
            val controller = mockControllerWithMapping()

            every { ControllerUtils.isControllerConnected() } returns true
            every { ControllerUtils.getController() } returns controller

            val buttons = buildButtons()
            val poller = MegaControllerPoller(buttons).apply { on = false }

            poller.run()

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
            every { ControllerUtils.isControllerConnected() } returns true
            every { ControllerUtils.getController() } returns controller

            MegaControllerButton.entries.forEach { btn ->
                every { getPreferredControllerCode(controller, btn) } returns 100 + btn.ordinal
            }

            val buttons = buildButtons()
            val poller = MegaControllerPoller(buttons).apply {
                on = false
                loadPrefsOnConnect = true
            }

            poller.run()

            MegaControllerButton.entries.forEach { btn ->
                buttons.codeOf(btn) shouldBe (100 + btn.ordinal)
            }
        }

        it("falls back to default mapping when prefs lookup returns null") {
            val controller = mockControllerWithMapping()

            every { ControllerUtils.isControllerConnected() } returns true
            every { ControllerUtils.getController() } returns controller

            MegaControllerButton.entries.forEach { btn ->
                every { getPreferredControllerCode(controller, btn) } returns null
            }

            val buttons = buildButtons()
            val poller = MegaControllerPoller(buttons).apply {
                on = false
                loadPrefsOnConnect = true
            }

            poller.run()

            // matches the default-mapping case
            buttons.codeOf(MegaControllerButton.UP) shouldBe 10
            buttons.codeOf(MegaControllerButton.A) shouldBe 21
            buttons.codeOf(MegaControllerButton.SELECT) shouldBe 20
        }
    }

    describe("on disconnect after being connected") {
        it("clears every 'controllerCode' back to null") {
            val controller = mockControllerWithMapping()
            every { ControllerUtils.isControllerConnected() } returns true
            every { ControllerUtils.getController() } returns controller

            val buttons = buildButtons()
            val poller = MegaControllerPoller(buttons).apply { on = false }

            // connect
            poller.run()
            buttons.codeOf(MegaControllerButton.UP) shouldBe 10

            // disconnect
            every { ControllerUtils.isControllerConnected() } returns false
            every { ControllerUtils.getController() } returns null

            poller.run()

            MegaControllerButton.entries.forEach { btn ->
                buttons.codeOf(btn) shouldBe null
            }
        }
    }

    describe("when controller stays connected across runs") {
        it("does not re-apply the mapping on subsequent runs") {
            val controller = mockControllerWithMapping()

            every { ControllerUtils.isControllerConnected() } returns true
            every { ControllerUtils.getController() } returns controller

            val buttons = buildButtons()
            val poller = MegaControllerPoller(buttons).apply { on = false }

            poller.run()

            // simulate a user remap between frames
            (buttons.get(MegaControllerButton.UP) as ControllerButton).controllerCode = 999

            poller.run()

            buttons.codeOf(MegaControllerButton.UP) shouldBe 999
        }
    }

    describe("when controller stays disconnected") {
        it("leaves 'controllerCodes' untouched") {
            every { ControllerUtils.isControllerConnected() } returns false
            every { ControllerUtils.getController() } returns null

            val buttons = buildButtons()
            (buttons.get(MegaControllerButton.UP) as ControllerButton).controllerCode = 7
            val poller = MegaControllerPoller(buttons).apply { on = false }

            poller.run()

            buttons.codeOf(MegaControllerButton.UP) shouldBe 7
        }
    }
})
