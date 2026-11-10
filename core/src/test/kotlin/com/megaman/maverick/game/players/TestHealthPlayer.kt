package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.DamageRecoveryComponent
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.IThawRule
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.locks.MegaLockGroup

class TestHealthPlayer(services: MegaServices) : AbstractHealthEntity(services), IBodyEntity, IFaceable,
    IEventListener, IPlayer {

    override val damageNegotiator = PlayerDamageNegotiator(this)
    override val eventKeyMask = objectSetOf<Any>()
    override var facing = Facing.RIGHT

    val recovery = DamageRecoveryComponent(1f, 0.1f)
    val stun = StunComponent(1f, { true }, {})
    val freeze = FreezeComponent(NeverThaw, {})

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC, width = 32f, height = 48f)))
        addComponent(recovery)
        addComponent(LocksComponent(MegaLockGroup.entries.size))
        addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        addComponent(stun)
        addComponent(freeze)
        addComponent(definePointsComponent())
        damageTimer.setToEnd()
    }

    fun startDamage() = damageTimer.reset()

    override fun onEvent(event: Event) {}

    override fun getType() = EntityType.MEGAMAN
}

object NeverThaw : IThawRule {
    override fun shouldThaw(delta: Float) = false
    override fun reset() {}
}

class TestFreezer(services: MegaServices) : MegaGameEntity(services), IDamagerEntity, IFreezerEntity {
    override fun getType() = EntityType.PROJECTILE
}

class TestOwnedDamager(services: MegaServices, override var owner: IGameEntity?) : MegaGameEntity(services),
    IOwnable<IGameEntity>, IDamagerEntity {

    init {
        init()
    }

    override fun getType() = EntityType.EXPLOSION
}
