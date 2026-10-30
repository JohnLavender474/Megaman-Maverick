package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.controllers.Controller
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ControllerUtilsExtensionsTest : DescribeSpec({

    val savedCodes = HashMap<String, Int>()

    val controller = mockk<Controller>(relaxed = true) {
        every { name } returns "TestController"
    }

    beforeEach {
        savedCodes.clear()
        val prefs = mockk<Preferences>(relaxed = true) {
            every { contains(any()) } answers { firstArg<String>() in savedCodes }
            every { getInteger(any()) } answers { savedCodes[firstArg<String>()] ?: 0 }
        }
        Gdx.app = mockk<Application>(relaxed = true) {
            every { getPreferences(any()) } returns prefs
        }
    }

    describe("getPreferredControllerCode") {
        it("gives null when the prefs file has no key for the button") {
            getPreferredControllerCode(controller, MegaControllerButton.A) shouldBe null
        }

        it("gives the saved code of the button, also the code 0") {
            savedCodes[MegaControllerButton.A.name] = 7
            savedCodes[MegaControllerButton.B.name] = 0

            getPreferredControllerCode(controller, MegaControllerButton.A) shouldBe 7
            getPreferredControllerCode(controller, MegaControllerButton.B) shouldBe 0
        }
    }
})
