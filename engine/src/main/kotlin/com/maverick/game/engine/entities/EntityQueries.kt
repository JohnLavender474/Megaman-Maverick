package com.maverick.game.engine.entities

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent

private fun IGameEntity.missing(part: String): Nothing = throw IllegalStateException("$this has no $part")

val IGameEntity.body: Body
    get() = require(BodyComponent::class).body

val IGameEntity.hasBody: Boolean
    get() = hasComponent(BodyComponent::class)

val IGameEntity.facing: Facing
    get() = (this as? IFaceable ?: missing("facing")).facing

val IGameEntity.direction: Direction
    get() = (this as? IDirectional ?: missing("direction")).direction

fun IGameEntity.isBehaviorActive(key: Any) = getComponent(BehaviorsComponent::class)?.isBehaviorActive(key) == true

fun IGameEntity.isAnyBehaviorActive(vararg keys: Any) =
    getComponent(BehaviorsComponent::class)?.isAnyBehaviorActive(*keys) == true

val IGameEntity.input: InputFrame
    get() {
        val source = getComponent(ControllerComponent::class)?.input ?: return InputFrame.RELEASED
        return source.read()
    }
