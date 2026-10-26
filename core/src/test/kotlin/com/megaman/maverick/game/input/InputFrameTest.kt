package com.megaman.maverick.game.input

import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.buttons.ControllerButtons
import com.maverick.game.engine.controller.buttons.IControllerButton
import com.maverick.game.engine.controller.polling.ControllerPoller
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.controllers.MegaControllerButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private class FakeButton : IControllerButton {
    var pressed = false
    var enabled = true

    override fun isPressed() = pressed

    override fun isEnabled() = enabled
}

private class FixedPoller(private val statuses: Map<MegaControllerButton, ButtonStatus>) : IControllerPoller {
    override var on = true

    override fun getStatus(key: Any) = statuses[key]

    override fun run() {}
}

class InputFrameTest : DescribeSpec({

    val buttons = MegaControllerButton.entries

    fun assertSameAnswers(frame: InputFrame, poller: IControllerPoller, context: String) {
        buttons.forEach { button ->
            val where = "$context, $button"
            (frame.getStatus(button) to where) shouldBe ((poller.getStatus(button) ?: ButtonStatus.RELEASED) to where)
            (frame.isPressed(button) to where) shouldBe (poller.isPressed(button) to where)
            (frame.isJustPressed(button) to where) shouldBe (poller.isJustPressed(button) to where)
            (frame.isJustReleased(button) to where) shouldBe (poller.isJustReleased(button) to where)
        }
    }

    describe("InputFrame") {
        it("reads back each status of each button, with the other buttons set to another status") {
            ButtonStatus.entries.forEach { status ->
                ButtonStatus.entries.filter { it != status }.forEach { other ->
                    buttons.forEach { button ->
                        val statuses = buttons.associateWith { if (it == button) status else other }
                        val frame = InputFrame.of(FixedPoller(statuses))

                        buttons.forEach { frame.getStatus(it) shouldBe statuses[it] }
                    }
                }
            }
        }

        it("gives the poller's answer for each status") {
            ButtonStatus.entries.forEach { status ->
                val poller = FixedPoller(buttons.associateWith { status })
                assertSameAnswers(InputFrame.of(poller), poller, "status $status")
            }
        }

        it("reads a missing status as RELEASED") {
            val frame = InputFrame.of(FixedPoller(emptyMap()))

            buttons.forEach { frame.getStatus(it) shouldBe ButtonStatus.RELEASED }
        }

        it("gives false for every query in the default frame") {
            val frame = InputFrame.RELEASED

            buttons.forEach {
                frame.getStatus(it) shouldBe ButtonStatus.RELEASED
                frame.isPressed(it) shouldBe false
                frame.isJustPressed(it) shouldBe false
                frame.isJustReleased(it) shouldBe false
            }
        }

        it("tells PRESSED from JUST_PRESSED, as JetpackBehavior needs") {
            val frame = InputFrame.of(
                FixedPoller(
                    mapOf(
                        MegaControllerButton.UP to ButtonStatus.PRESSED,
                        MegaControllerButton.A to ButtonStatus.JUST_PRESSED
                    )
                )
            )

            frame.getStatus(MegaControllerButton.UP) shouldBe ButtonStatus.PRESSED
            frame.getStatus(MegaControllerButton.A) shouldBe ButtonStatus.JUST_PRESSED
        }
    }

    describe("InputFrame built from a ControllerPoller") {
        lateinit var fakes: Map<MegaControllerButton, FakeButton>
        lateinit var poller: ControllerPoller

        beforeEach {
            fakes = buttons.associateWith { FakeButton() }
            val controllerButtons = ControllerButtons()
            buttons.forEach { controllerButtons.put(it, fakes[it]) }
            poller = ControllerPoller(controllerButtons)
        }

        fun tick(context: String) {
            poller.run()
            assertSameAnswers(InputFrame.of(poller), poller, context)
        }

        it("gives the poller's answers on every tick of seeded random scripts") {
            listOf(1L, 2L, 3L, 31L, 2026L).forEach { seed ->
                val random = Random(seed)
                repeat(2_000) { t ->
                    buttons.forEach { fakes[it]!!.pressed = random.nextInt(3) == 0 }
                    if (random.nextInt(20) == 0) poller.on = !poller.on
                    if (random.nextInt(50) == 0) {
                        val fake = fakes[buttons[random.nextInt(buttons.size)]]!!
                        fake.enabled = !fake.enabled
                    }
                    tick("seed $seed, tick $t")
                }
            }
        }

        it("hold, then lock: JUST_RELEASED once, then RELEASED") {
            val a = fakes[MegaControllerButton.A]!!
            a.pressed = true
            tick("press")
            tick("hold")
            poller.on = false
            tick("lock 1")
            InputFrame.of(poller).getStatus(MegaControllerButton.A) shouldBe ButtonStatus.JUST_RELEASED
            tick("lock 2")
            InputFrame.of(poller).getStatus(MegaControllerButton.A) shouldBe ButtonStatus.RELEASED
        }

        it("unlock while held: a new JUST_PRESSED") {
            val a = fakes[MegaControllerButton.A]!!
            a.pressed = true
            poller.on = false
            tick("locked")
            poller.on = true
            tick("unlock")
            InputFrame.of(poller).getStatus(MegaControllerButton.A) shouldBe ButtonStatus.JUST_PRESSED
        }

        it("press and lock in the same tick: RELEASED") {
            tick("start")
            fakes[MegaControllerButton.B]!!.pressed = true
            poller.on = false
            tick("press and lock")
            InputFrame.of(poller).getStatus(MegaControllerButton.B) shouldBe ButtonStatus.RELEASED
        }

        it("a disabled button flips between JUST_RELEASED and RELEASED") {
            tick("start")
            fakes[MegaControllerButton.SELECT]!!.enabled = false
            tick("disabled 1")
            InputFrame.of(poller).getStatus(MegaControllerButton.SELECT) shouldBe ButtonStatus.JUST_RELEASED
            tick("disabled 2")
            InputFrame.of(poller).getStatus(MegaControllerButton.SELECT) shouldBe ButtonStatus.RELEASED
            tick("disabled 3")
            InputFrame.of(poller).getStatus(MegaControllerButton.SELECT) shouldBe ButtonStatus.JUST_RELEASED
        }

        it("gives every query false before the first poll") {
            assertSameAnswers(InputFrame.of(poller), poller, "before the first poll")
        }
    }

    describe("PlayerInputs") {
        it("starts with the default frame in every slot and stores a frame per slot") {
            val inputs = PlayerInputs(4)
            (0 until 4).forEach { inputs[PlayerSlot(it)] shouldBe InputFrame.RELEASED }

            val frame = InputFrame.of(FixedPoller(mapOf(MegaControllerButton.START to ButtonStatus.JUST_PRESSED)))
            inputs[PlayerSlot(2)] = frame

            inputs[PlayerSlot(2)] shouldBe frame
            inputs[PlayerSlot(0)] shouldBe InputFrame.RELEASED
        }
    }
})
