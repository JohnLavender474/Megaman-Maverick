package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullableOnUncontained
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.events.EventType

const val PROJECTILE_DEFAULT_CULL_TIME = 0.5f

interface IProjectileEntity : IMegaGameEntity, IBodyEntity, IAudioEntity, ICullableEntity, IOwnable, IDamager {

    override fun getEntityType() = EntityType.PROJECTILE

    fun defineProjectileComponents(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME): Array<IGameComponent> {
        val components = Array<IGameComponent>()
        components.add(AudioComponent())
        components.add(
            CullablesComponent(
                objectMapOf(
                    ConstKeys.CULL_EVENTS to getCullOnEventCullable(),
                    ConstKeys.CULL_OUT_OF_BOUNDS to getCullOnOutOfGameCam(outOfBoundsCullTime)
                )
            )
        )
        return components
    }

    fun removeCullOnEventCullable() = removeCullable(ConstKeys.CULL_EVENTS)

    fun getCullOnEventCullable(): CullableOnEvent {
        val cullEvents = objectSetOf<Any>(
            EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
        )
        return CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
    }

    fun removeCullOnOutOfGameCam() = removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

    fun getCullOnOutOfGameCam(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME) =
        CullableOnUncontained<Camera>(
            containerSupplier = { game.getGameCamera() },
            containable = { it.overlaps(body) },
            timeToCull = outOfBoundsCullTime
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