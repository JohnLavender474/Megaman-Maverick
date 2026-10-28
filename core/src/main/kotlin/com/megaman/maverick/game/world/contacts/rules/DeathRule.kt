package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.items.HeartTank
import com.megaman.maverick.game.world.body.*

object DeathRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = kill(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = kill(a, b)

    private fun kill(deathFixture: IFixture, otherFixture: IFixture) {
        val entity = otherFixture.getEntity()
        val canDie = entity.getOrDefaultProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, true, Boolean::class)
        if (!canDie) return

        val deathListener = otherFixture.getOrDefaultProperty(ConstKeys.DEATH_LISTENER, true, Boolean::class)
        if (!deathListener) return

        val instant = deathFixture.isProperty(ConstKeys.INSTANT, true)
        when {
            entity is IDamageableEntity && (instant || !entity.invincible) -> otherFixture.depleteHealth()
            entity is IProjectileEntity -> entity.explodeAndDie()
            deathShouldDestroy(entity) -> entity.destroy()
        }
    }

    private fun deathShouldDestroy(entity: IGameEntity) = entity is HeartTank
}
