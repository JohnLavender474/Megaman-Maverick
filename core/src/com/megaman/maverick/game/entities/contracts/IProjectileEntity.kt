package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.extensions.overlaps
import com.engine.components.IGameComponent
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullableOnUncontained
import com.engine.cullables.CullablesComponent
import com.engine.cullables.ICullable
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.world.BodyComponent
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile.Companion.DEFAULT_PROJECTILE_CULL_TIME
import com.megaman.maverick.game.events.EventType

interface IProjectileEntity : IMegaGameEntity, IBodyEntity, ICullableEntity, IOwnable, IDamager {

    override fun getEntityType() = EntityType.PROJECTILE

    fun defineProjectileComponents(): Array<IGameComponent> {
        val components = Array<IGameComponent>()
        components.add(AudioComponent())
        components.add(
            CullablesComponent(
                objectMapOf(
                    ConstKeys.CULL_EVENTS to getCullOnEventCullable(),
                    ConstKeys.CULL_OUT_OF_BOUNDS to getCullOnOutOfGameCam()
                )
            )
        )
        return components
    }

    fun removeCullOnEventCullable() = removeCullable(ConstKeys.CULL_EVENTS)

    fun getCullOnEventCullable(): ICullable {
        val cullEvents = objectSetOf<Any>(
            EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
        )
        return CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
    }

    fun removeCullOnOutOfGameCam() = removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

    fun getCullOnOutOfGameCam() =
        CullableOnUncontained<Camera>(
            containerSupplier = { game.getGameCamera() },
            containable = { it.overlaps(body) },
            timeToCull = DEFAULT_PROJECTILE_CULL_TIME
        )

    override fun canDamage(damageable: IDamageable) = damageable != owner

    fun explodeAndDie(vararg params: Any?) {}

    fun hitBody(bodyFixture: IFixture) {}

    fun hitBlock(blockFixture: IFixture) {}

    fun hitShield(shieldFixture: IFixture) {}

    fun hitWater(waterFixture: IFixture) {}

    fun hitSand(sandFixture: IFixture) {}

    fun hitProjectile(projectileFixture: IFixture) {}

    fun defineBodyComponent(): BodyComponent

    fun defineSpritesComponent(): SpritesComponent
}