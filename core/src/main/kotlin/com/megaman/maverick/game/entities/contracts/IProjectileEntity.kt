package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.cullables.CullableOnEvent
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
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType

const val PROJECTILE_DEFAULT_CULL_TIME = 0.05f

interface IProjectileEntity : IMegaGameEntity, IBodyEntity, IAudioEntity, ICullableEntity, IOwnable, IDamager, ISizable {

    override fun getType() = EntityType.PROJECTILE

    fun defineProjectileComponents(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME): Array<IGameComponent> {
        val components = Array<IGameComponent>()
        components.add(AudioComponent())
        components.add(
            CullablesComponent(
                objectMapOf(
                    ConstKeys.CULL_EVENTS pairTo getCullOnEventCullable(),
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo getCullOnOutOfGameCam(outOfBoundsCullTime)
                )
            )
        )
        return components
    }

    fun removeCullOnEventCullable() = removeCullable(ConstKeys.CULL_EVENTS)

    fun getCullOnEventCullable(): CullableOnEvent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        return CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
    }

    fun removeCullOnOutOfGameCam() = removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

    fun getCullOnOutOfGameCam(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME) =
        getGameCameraCullingLogic(this, outOfBoundsCullTime)

    override fun canDamage(damageable: IDamageable) = damageable != owner

    fun explodeAndDie(vararg params: Any?) {}

    fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun defineBodyComponent(): BodyComponent

    fun defineSpritesComponent(): SpritesComponent
}
