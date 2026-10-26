package com.maverick.game.engine.controller

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem


class ControllerSystem : GameSystem(ControllerComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(ControllerComponent::class)!!
                val input = component.input ?: return@forEach
                val frame = input.read()

                val buttons = component.buttons
                val actuators = component.actuators
                for (i in buttons.indices) {
                    val actuator = actuators[i]
                    when (frame.getStatus(buttons[i])) {
                        ButtonStatus.JUST_PRESSED -> actuator.onJustPressed()
                        ButtonStatus.PRESSED -> actuator.onPressContinued(delta)
                        ButtonStatus.JUST_RELEASED -> actuator.onJustReleased()
                        ButtonStatus.RELEASED -> actuator.onReleaseContinued(delta)
                    }
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing controller for entity: $entity", e)
            }
        }
    }
}
