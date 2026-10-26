package com.megaman.maverick.game.players

import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.PlayerView
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.megaman.maverick.game.screens.levels.camera.IFocusable

class MegaPlayer(
    override val slot: PlayerSlot,
    override val entity: IGameEntity,
    private val inputSource: InputSource
) : PlayerView {

    private val bodyComponent = entity.require(BodyComponent::class)
    private val behaviorsComponent = entity.require(BehaviorsComponent::class)
    private val state = entity.require(PlayerStateComponent::class)
    private val directional = entity as? IDirectional ?: throw IllegalArgumentException("$entity is not IDirectional")
    private val faceable = entity as? IFaceable ?: throw IllegalArgumentException("$entity is not IFaceable")
    private val damageable = entity as? IDamageable ?: throw IllegalArgumentException("$entity is not IDamageable")
    private val health = entity as? IHealthEntity ?: throw IllegalArgumentException("$entity is not IHealthEntity")
    private val gravityDeviceSwitch = entity.getProperty(PlayerKeys.GRAVITY_DEVICE_SWITCH)
        ?: throw IllegalArgumentException("$entity has no gravity device switch receiver")
    private val lightReceiver = entity.getProperty(PlayerKeys.LIGHT)
        ?: throw IllegalArgumentException("$entity has no light receiver")
    private val megaGameEntity =
        entity as? MegaGameEntity ?: throw IllegalArgumentException("$entity is not a MegaGameEntity")

    val focus = entity as? IFocusable ?: throw IllegalArgumentException("$entity is not IFocusable")

    init {
        entity.require(ControllerComponent::class).input = inputSource
        // TODO: unregistering a player does not remove this view or its input source from its entity; players who
        //  drop out are a co-op rule for item 047
        entity.putProperty(PlayerKeys.VIEW, this)
    }

    val input: InputFrame
        get() = inputSource.read()

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

    var ready: Boolean
        get() = state.ready
        set(value) {
            state.ready = value
        }

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

    val headFixture: Fixture
        get() = body.getProperty(ConstKeys.HEAD, Fixture::class)!!

    val feetGravityFixture: Fixture
        get() = body.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as Fixture

    val teleporting: Boolean
        get() = state.teleporting

    val light: PlayerLight
        get() = lightReceiver()

    val healthRatio: Float
        get() = health.getHealthRatio()

    val currentHealth: Int
        get() = health.getCurrentHealth()

    val maxHealth: Int
        get() = health.getMaxHealth()

    fun translateHealth(value: Int) = health.translateHealth(value)

    fun depleteHealth() = health.depleteHealth()

    fun spawn(props: Properties) = megaGameEntity.spawn(props)

    fun onGravityDeviceSwitch() = gravityDeviceSwitch()

    fun isBehaviorActive(key: Any) = behaviorsComponent.isBehaviorActive(key)

    fun isAnyBehaviorActive(vararg keys: Any) = behaviorsComponent.isAnyBehaviorActive(*keys)

    fun setAllBehaviorsAllowed(allowed: Boolean) = behaviorsComponent.setAllBehaviorsAllowed(allowed)

    fun allowMovement(allowed: Boolean) {
        state.movementAllowed = allowed
    }

    fun addDamageListener(listener: IPlayerDamageListener) = state.damageListeners.add(listener)

    fun removeDamageListener(listener: IPlayerDamageListener) = state.damageListeners.remove(listener)
}
