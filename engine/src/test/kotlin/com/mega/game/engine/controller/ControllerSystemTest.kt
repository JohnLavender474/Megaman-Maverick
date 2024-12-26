package com.mega.game.engine.controller

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.MockGameEntity
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.mega.game.engine.controller.buttons.IButtonActuator
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.controller.ControllerComponent
import com.mega.game.engine.controller.ControllerSystem
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class ControllerSystemTest :
    DescribeSpec({
        describe("ControllerSystem") {
            it("should call appropriate methods on actuators when processing") {
                val mockControllerPoller = mockk<IControllerPoller>()
                val controllerSystem = ControllerSystem(mockControllerPoller)

                val entity = MockGameEntity()

                val actuator =
                    mockk<IButtonActuator> {
                        every { onJustPressed(any()) } just Runs
                        every { onPressContinued(any(), any()) } just Runs
                        every { onJustReleased(any()) } just Runs
                        every { onReleaseContinued(any(), any()) } just Runs
                    }

                val map = ObjectMap<Any, () -> IButtonActuator?>()
                map.put("ButtonA") { actuator }
                val controllerComponent = ControllerComponent(map)
                entity.addComponent(controllerComponent)

                controllerSystem.add(entity)

                for (buttonStatus in 0..4) {
                    when (buttonStatus) {
                        0 ->
                            every { mockControllerPoller.getStatus("ButtonA") } returns
                                    ButtonStatus.JUST_PRESSED

                        1 -> every { mockControllerPoller.getStatus("ButtonA") } returns ButtonStatus.PRESSED
                        2 ->
                            every { mockControllerPoller.getStatus("ButtonA") } returns
                                    ButtonStatus.JUST_RELEASED

                        3 -> every { mockControllerPoller.getStatus("ButtonA") } returns ButtonStatus.RELEASED
                    }

                    controllerSystem.update(0.1f)

                    when (buttonStatus) {
                        0 -> verify(exactly = 1) { actuator.onJustPressed(mockControllerPoller) }
                        1 -> verify { actuator.onPressContinued(mockControllerPoller, 0.1f) }
                        2 -> verify { actuator.onJustReleased(mockControllerPoller) }
                        3 -> verify { actuator.onReleaseContinued(mockControllerPoller, 0.1f) }
                    }
                }
            }
        }
    })
