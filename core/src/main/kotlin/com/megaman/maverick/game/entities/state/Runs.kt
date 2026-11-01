package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity

var IGameEntity.running: Boolean
    get() = inPlay && getComponent(RunComponent::class)?.running == true
    set(value) {
        require(RunComponent::class).running = value
    }
