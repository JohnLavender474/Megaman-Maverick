package com.mega.game.engine.controller

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.controller.buttons.IButtonActuator

class ControllerComponent(val actuators: ObjectMap<Any, () -> IButtonActuator?>) : IGameComponent {

    constructor(vararg _actuators: GamePair<Any, () -> IButtonActuator?>) : this(ObjectMap<Any, () -> IButtonActuator?>().apply {
        _actuators.forEach { put(it.first, it.second) }
    })

    fun putActuator(name: Any, actuator: () -> IButtonActuator?) {
        actuators.put(name, actuator)
    }

    fun putActuator(name: Any, actuator: IButtonActuator) {
        actuators.put(name) { actuator }
    }

    fun removeActuator(name: Any) {
        actuators.remove(name)
    }
}

