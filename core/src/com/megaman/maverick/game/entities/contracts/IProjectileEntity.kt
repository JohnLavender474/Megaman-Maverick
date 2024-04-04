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
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.events.EventType

interface IProjectileEntity : IOwnable, IDamager, IBodyEntity, ISpriteEntity, IAudioEntity, IGameEntity {

    override fun canDamage(damageable: IDamageable) = damageable != owner && damageable !is IProjectileEntity

    fun explodeAndDie() {}

    fun hitBody(bodyFixture: IFixture) {}

    fun hitBlock(blockFixture: IFixture) {}

    fun hitShield(shieldFixture: IFixture) {}

    fun hitWater(waterFixture: IFixture) {}
}

internal fun IProjectileEntity.defineProjectileComponents(): Array<IGameComponent> {
    val components = Array<IGameComponent>()
    components.add(AudioComponent(this))
    val cullEvents = objectSetOf<Any>(
        EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
    )
    val cullOnEvent = CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
    val cullOnOutOfGameCam =
        CullableOnUncontained<Camera>(containerSupplier = { game.viewports.get(ConstKeys.GAME).camera },
            containable = { it.overlaps(body) })
    components.add(
        CullablesComponent(
            this, objectMapOf(
                ConstKeys.CULL_EVENTS to cullOnEvent, ConstKeys.CULL_OUT_OF_BOUNDS to cullOnOutOfGameCam
            )
        )
    )
    return components
}
