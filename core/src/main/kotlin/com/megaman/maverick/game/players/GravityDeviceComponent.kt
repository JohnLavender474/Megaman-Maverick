package com.megaman.maverick.game.players

import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity

class GravityDeviceComponent(val onSwitch: () -> Unit) : IGameComponent

fun IGameEntity.onGravityDeviceSwitch() {
    getComponent(GravityDeviceComponent::class)?.onSwitch?.invoke()
}
