package com.maverick.game.engine.controller

import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.controller.polling.IControllerPoller
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem


class ControllerSystem(private val poller: IControllerPoller) :
    GameSystem(ControllerComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(ControllerComponent::class)!!
                val actuators = component.actuators

                for (entry in actuators) {
                    val key = entry.key
                    val actuator = entry.value() ?: continue

                    val status = poller.getStatus(key) ?: continue
                    when (status) {
                        ButtonStatus.JUST_PRESSED -> actuator.onJustPressed(poller)
                        ButtonStatus.PRESSED -> actuator.onPressContinued(poller, delta)
                        ButtonStatus.JUST_RELEASED -> actuator.onJustReleased(poller)
                        ButtonStatus.RELEASED -> actuator.onReleaseContinued(poller, delta)
                    }
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing controller for entity: $entity", e)
            }
        }
    }
}
