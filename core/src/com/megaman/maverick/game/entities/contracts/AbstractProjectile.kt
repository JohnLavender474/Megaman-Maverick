package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.extensions.overlaps
import com.engine.common.objects.Properties
import com.engine.components.IGameComponent
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullableOnUncontained
import com.engine.cullables.CullablesComponent
import com.engine.cullables.ICullable
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.BodyComponent
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.events.EventType

abstract class AbstractProjectile(game: MegamanMaverickGame) : MegaGameEntity(game), IOwnable, IDamager, IBodyEntity,
    ISpritesEntity, IAudioEntity, ICullableEntity {

    override var owner: IGameEntity? = null

    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null
    protected var movementScalar = 1f

    override fun init() {
        super<MegaGameEntity>.init()
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getCullOnOutOfGameCam())
        else removeCullOnOutOfGameCam()

        val cullOnEvents = spawnProps.getOrDefault(ConstKeys.CULL_EVENTS, true, Boolean::class)
        if (cullOnEvents) putCullable(ConstKeys.CULL_EVENTS, getCullOnEventCullable())
        else removeCullOnEventCullable()

        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }

    open fun defineProjectileComponents(): Array<IGameComponent> {
        val components = Array<IGameComponent>()
        components.add(AudioComponent(this))
        components.add(
            CullablesComponent(
                this,
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
            containerSupplier = { game.viewports.get(ConstKeys.GAME).camera },
            containable = { it.overlaps(body) }
        )

    override fun canDamage(damageable: IDamageable) = damageable != owner

    open fun explodeAndDie(vararg params: Any?) {}

    open fun hitBody(bodyFixture: IFixture) {}

    open fun hitBlock(blockFixture: IFixture) {}

    open fun hitShield(shieldFixture: IFixture) {}

    open fun hitWater(waterFixture: IFixture) {}

    open fun hitProjectile(projectileFixture: IFixture) {}

    protected abstract fun defineBodyComponent(): BodyComponent

    protected abstract fun defineSpritesComponent(): SpritesComponent
}