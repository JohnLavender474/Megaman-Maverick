package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputFrameBuilder
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.input.SystemButton
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private fun bit(button: MegaControllerButton) = 1L shl button.index

private fun playerBuilder() = InputFrameBuilder(MegaControllerButton.entries)

private fun justPressed(button: MegaControllerButton) = playerBuilder().build(bit(button))

private fun justReleased(button: MegaControllerButton) = playerBuilder().run {
    build(bit(button))
    build(0L)
}

private fun requested(
    playerFrame: InputFrame = InputFrame.RELEASED,
    paused: Boolean = false,
    alive: Boolean = true,
    inPlay: Boolean = true,
    locked: Boolean = false,
    backStatus: ButtonStatus = ButtonStatus.RELEASED,
    selectAction: SelectButtonAction = SelectButtonAction.NEXT_WEAPON
) = isPauseRequested(
    paused = paused,
    alive = alive,
    inPlay = inPlay,
    locked = locked,
    playerFrame = playerFrame,
    backStatus = backStatus,
    selectAction = selectAction
)

private fun escPressed() = InputFrameBuilder(SystemButton.entries).run {
    build(1L shl SystemButton.BACK.index).getStatus(SystemButton.BACK)
}

private fun escHeld() = InputFrameBuilder(SystemButton.entries).run {
    build(1L shl SystemButton.BACK.index)
    build(1L shl SystemButton.BACK.index).getStatus(SystemButton.BACK)
}

private fun drain(queue: Queue<() -> Unit>) {
    while (!queue.isEmpty) queue.removeLast().invoke()
}

class LevelPauseRulesTest : DescribeSpec({

    describe("isPauseRequested") {
        it("is true for a real START release with no lock") {
            requested(justReleased(MegaControllerButton.START)) shouldBe true
        }

        it("is true for a press of SELECT only with the START action, and false for a release of SELECT") {
            val pressed = justPressed(MegaControllerButton.SELECT)

            requested(pressed, selectAction = SelectButtonAction.START) shouldBe true
            requested(pressed, selectAction = SelectButtonAction.NEXT_WEAPON) shouldBe false
            requested(pressed, selectAction = SelectButtonAction.AIR_DASH) shouldBe false

            val released = justReleased(MegaControllerButton.SELECT)

            requested(released, selectAction = SelectButtonAction.START) shouldBe false
        }

        it("is false when paused, not alive or not in play") {
            val release = justReleased(MegaControllerButton.START)

            requested(release, paused = true) shouldBe false
            requested(release, alive = false) shouldBe false
            requested(release, inPlay = false) shouldBe false

            requested(backStatus = escPressed(), paused = true) shouldBe false
            requested(backStatus = escPressed(), alive = false) shouldBe false
            requested(backStatus = escPressed(), inPlay = false) shouldBe false
        }

        it("is false for the START release that the first locked build makes") {
            val builder = playerBuilder()
            builder.build(bit(MegaControllerButton.START))
            builder.build(bit(MegaControllerButton.START))

            builder.locked = true
            val frame = builder.build(bit(MegaControllerButton.START))

            frame.getStatus(MegaControllerButton.START) shouldBe ButtonStatus.JUST_RELEASED
            requested(frame, locked = true) shouldBe false
        }

        it("is true for a press of Esc with no lock, and false while Esc stays held") {
            requested(backStatus = escPressed()) shouldBe true
            requested(backStatus = escHeld()) shouldBe false
        }

        it("is false for a press of Esc with the lock on") {
            requested(backStatus = escPressed(), locked = true) shouldBe false
        }

        it("is false for SELECT with the START action and the lock on") {
            requested(
                justPressed(MegaControllerButton.SELECT),
                locked = true,
                selectAction = SelectButtonAction.START
            ) shouldBe false
        }

        it("is false for a START held across the end of a lock, and true for its real release") {
            val builder = playerBuilder()
            val held = bit(MegaControllerButton.START)
            builder.build(held)
            builder.locked = true
            builder.build(held)
            builder.build(held)
            builder.locked = false

            val pressed = builder.build(held)
            pressed.getStatus(MegaControllerButton.START) shouldBe ButtonStatus.JUST_PRESSED
            requested(pressed) shouldBe false

            val stillPressed = builder.build(held)
            stillPressed.getStatus(MegaControllerButton.START) shouldBe ButtonStatus.PRESSED
            requested(stillPressed) shouldBe false

            requested(builder.build(0L)) shouldBe true
        }
    }

    describe("queuePause") {
        it("drops the pause when a lock starts after the request, in the order of render()") {
            val builder = playerBuilder()
            val queue = Queue<() -> Unit>()
            var paused = false

            builder.build(bit(MegaControllerButton.START))
            builder.build(bit(MegaControllerButton.START))

            drain(queue)
            val frame = builder.build(0L)
            frame.getStatus(MegaControllerButton.START) shouldBe ButtonStatus.JUST_RELEASED
            requested(frame, locked = builder.locked) shouldBe true
            queuePause(queue, { builder.locked }, { paused = true })
            builder.locked = true

            drain(queue)
            builder.build(0L)

            paused shouldBe false
        }

        it("runs the pause once when no lock starts") {
            val builder = playerBuilder()
            val queue = Queue<() -> Unit>()
            var pauses = 0

            builder.build(bit(MegaControllerButton.START))
            builder.build(bit(MegaControllerButton.START))

            drain(queue)
            val frame = builder.build(0L)
            requested(frame, locked = builder.locked) shouldBe true
            queuePause(queue, { builder.locked }, { pauses++ })

            drain(queue)
            builder.build(0L)
            drain(queue)

            pauses shouldBe 1
        }
    }
})
