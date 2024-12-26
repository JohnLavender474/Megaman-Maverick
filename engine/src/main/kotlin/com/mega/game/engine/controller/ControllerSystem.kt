package com.mega.game.engine.controller

import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.controller.buttons.ButtonStatus
import com.mega.game.engine.controller.polling.IControllerPoller
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem


class ControllerSystem(private val poller: IControllerPoller) :
    GameSystem(ControllerComponent::class) {

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        entities.forEach {
            val component = it.getComponent(ControllerComponent::class)!!
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
        }
    }
}
