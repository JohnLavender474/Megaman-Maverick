package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.getCenter

const val PROJECTILE_DEFAULT_CULL_TIME = 0.05f

interface IProjectileEntity : IMegaGameEntity, IBodyEntity, IAudioEntity, ICullableEntity, IOwnable<IGameEntity>,
    IDamager, ISizable {

    override fun getType() = EntityType.PROJECTILE

    fun defineProjectileComponents(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME): Array<IGameComponent> {
        val components = Array<IGameComponent>()
        components.add(AudioComponent())
        components.add(
            CullablesComponent(
                objectMapOf(
                    ConstKeys.CULL_EVENTS pairTo getCullOnEventsCullable(),
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo getCullOnOutOfGameCam(outOfBoundsCullTime)
                )
            )
        )
        return components
    }

    fun removeCullOnEventCullable() = removeCullable(ConstKeys.CULL_EVENTS)

    fun getCullOnEventsCullable(): CullableOnEvent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        return CullableOnEvent({ cullEvents.contains(it.key) }, cullEvents)
    }

    fun removeCullOnOutOfGameCam() = removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

    fun getCullOnOutOfGameCam(outOfBoundsCullTime: Float = PROJECTILE_DEFAULT_CULL_TIME) =
        getGameCameraCullingLogic(this, outOfBoundsCullTime)

    override fun canDamage(damageable: IDamageable) = damageable != owner

    fun explodeAndDie(vararg params: Any?) {
        // can't access "destroy" since it's defined in the abstract class, so manually call "engine.destroy" instead
        game.engine.destroy(this)

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo body.getCenter()
        )
        explosion.spawn(props)

        requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun hitExplosion(explosionFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {}

    fun defineBodyComponent(): BodyComponent

    fun defineSpritesComponent(): SpritesComponent
}
