package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.world.body.getBounds

fun IBodyEntity.knockBackFrom(damager: IDamagerEntity) {
    val knockback = getComponent(KnockbackComponent::class) ?: return
    if (knockback.exempt.contains(damager::class) || damager !is IBodyEntity) return

    val bounds = damager.body.getBounds()

    when (body.direction) {
        Direction.UP -> {
            body.physics.velocity.x = if (bounds.getX() > body.getX()) -knockback.x else knockback.x
            body.physics.velocity.y = knockback.y
        }
        Direction.DOWN -> {
            body.physics.velocity.x = if (bounds.getX() > body.getX()) -knockback.x else knockback.x
            body.physics.velocity.y = -knockback.y
        }
        Direction.LEFT -> {
            body.physics.velocity.x = -knockback.y
            body.physics.velocity.y = if (bounds.getY() > body.getY()) -knockback.x else knockback.x
        }
        Direction.RIGHT -> {
            body.physics.velocity.x = knockback.y
            body.physics.velocity.y = if (bounds.getY() > body.getY()) -knockback.x else knockback.x
        }
    }
}
