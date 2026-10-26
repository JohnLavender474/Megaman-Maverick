package com.maverick.game.engine.controller

import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.controller.buttons.IButtonActuator
import com.maverick.game.engine.input.InputButton
import com.maverick.game.engine.input.InputSource

class ControllerComponent(vararg actuators: GamePair<out InputButton, out IButtonActuator>) : IGameComponent {

    var input: InputSource? = null

    internal val buttons: Array<InputButton>
    internal val actuators: Array<IButtonActuator>

    init {
        val sorted = actuators.sortedBy { it.first.index }
        buttons = Array(sorted.size) { sorted[it].first }
        this.actuators = Array(sorted.size) { sorted[it].second }
    }
}
