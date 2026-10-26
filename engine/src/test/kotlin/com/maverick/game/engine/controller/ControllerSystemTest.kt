package com.maverick.game.engine.controller

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.buttons.IButtonActuator
import com.maverick.game.engine.input.InputButton
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

private data class TestButton(override val index: Int) : InputButton

class ControllerSystemTest :
    DescribeSpec({
        describe("ControllerSystem") {
            val button = TestButton(3)

            it("calls only the callback that matches the button's status") {
                ButtonStatus.entries.forEach { status ->
                    val actuator = mockk<IButtonActuator>(relaxed = true)
                    val entity = MockGameEntity()
                    entity.addComponent(ControllerComponent(button pairTo actuator).also {
                        it.input = InputSource { InputFrame.RELEASED.with(button, status) }
                    })
                    val system = ControllerSystem()
                    system.add(entity)

                    system.update(0.1f)

                    when (status) {
                        ButtonStatus.JUST_PRESSED -> verify(exactly = 1) { actuator.onJustPressed() }
                        ButtonStatus.PRESSED -> verify(exactly = 1) { actuator.onPressContinued(0.1f) }
                        ButtonStatus.JUST_RELEASED -> verify(exactly = 1) { actuator.onJustReleased() }
                        ButtonStatus.RELEASED -> verify(exactly = 1) { actuator.onReleaseContinued(0.1f) }
                    }
                    confirmVerified(actuator)
                }
            }

            it("skips an entity with no input source") {
                val actuator = mockk<IButtonActuator>(relaxed = true)
                val entity = MockGameEntity()
                entity.addComponent(ControllerComponent(button pairTo actuator))
                val system = ControllerSystem()
                system.add(entity)

                system.update(0.1f)

                confirmVerified(actuator)
            }

            it("calls the actuators in button index order") {
                val calls = ArrayList<Int>()
                fun recorder(index: Int) = ButtonActuator(onReleaseContinued = { calls.add(index) })
                val entity = MockGameEntity()
                entity.addComponent(
                    ControllerComponent(
                        TestButton(7) pairTo recorder(7),
                        TestButton(2) pairTo recorder(2),
                        TestButton(5) pairTo recorder(5),
                        TestButton(3) pairTo recorder(3)
                    ).also { it.input = InputSource { InputFrame.RELEASED } }
                )
                val system = ControllerSystem()
                system.add(entity)

                system.update(0.1f)

                calls shouldBe listOf(2, 3, 5, 7)
            }

            it("reads the input source once per entity per update") {
                var reads = 0
                val entity = MockGameEntity()
                entity.addComponent(
                    ControllerComponent(
                        TestButton(0) pairTo ButtonActuator(),
                        TestButton(1) pairTo ButtonActuator()
                    ).also {
                        it.input = InputSource {
                            reads++
                            InputFrame.RELEASED
                        }
                    }
                )
                val system = ControllerSystem()
                system.add(entity)

                system.update(0.1f)

                reads shouldBe 1
            }
        }
    })
