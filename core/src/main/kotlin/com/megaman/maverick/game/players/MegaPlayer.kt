package com.megaman.maverick.game.players

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.PlayerView
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class MegaPlayer(override val slot: PlayerSlot, override val entity: IGameEntity) : PlayerView {

    private val bodyComponent = entity.require(BodyComponent::class)
    private val behaviorsComponent = entity.require(BehaviorsComponent::class)
    private val state = entity.require(PlayerStateComponent::class)
    private val directional = entity as? IDirectional ?: throw IllegalArgumentException("$entity is not IDirectional")
    private val faceable = entity as? IFaceable ?: throw IllegalArgumentException("$entity is not IFaceable")
    private val damageable = entity as? IDamageable ?: throw IllegalArgumentException("$entity is not IDamageable")
    private val megaGameEntity =
        entity as? MegaGameEntity ?: throw IllegalArgumentException("$entity is not a MegaGameEntity")

    val body: Body
        get() = bodyComponent.body

    var direction: Direction
        get() = directional.direction
        set(value) {
            directional.direction = value
        }

    var facing: Facing
        get() = faceable.facing
        set(value) {
            faceable.facing = value
        }

    val invincible: Boolean
        get() = damageable.invincible

    val shooting: Boolean
        get() = state.shooting

    var canBeDamaged: Boolean
        get() = state.canBeDamaged
        set(value) {
            state.canBeDamaged = value
        }

    val dead: Boolean
        get() = megaGameEntity.dead

    val ready: Boolean
        get() = state.ready

    val damaged: Boolean
        get() = state.damaged

    val feetFixture: Fixture
        get() = body.getProperty(ConstKeys.FEET) as Fixture

    val leftSideFixture: Fixture
        get() = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", Fixture::class)!!

    val rightSideFixture: Fixture
        get() = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", Fixture::class)!!

    val damageableFixture: Fixture
        get() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

    fun isBehaviorActive(key: Any) = behaviorsComponent.isBehaviorActive(key)

    fun addDamageListener(listener: IPlayerDamageListener) = state.damageListeners.add(listener)

    fun removeDamageListener(listener: IPlayerDamageListener) = state.damageListeners.remove(listener)
}
