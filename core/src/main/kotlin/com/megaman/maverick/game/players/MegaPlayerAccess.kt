package com.megaman.maverick.game.players

import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.screens.levels.camera.IFocusable

private fun MegaPlayer.missing(part: String): Nothing =
    throw IllegalStateException("Player in $slot has no $part: $entity")

val MegaPlayer.body: Body
    get() = entity.require(BodyComponent::class).body

val MegaPlayer.hasBody: Boolean
    get() = entity.hasComponent(BodyComponent::class)

val MegaPlayer.direction: Direction
    get() = (entity as? IDirectional ?: missing("direction")).direction

val MegaPlayer.facing: Facing
    get() = (entity as? IFaceable ?: missing("facing")).facing

val MegaPlayer.focus: IFocusable
    get() = entity as? IFocusable ?: missing("camera focus")

val MegaPlayer.dead: Boolean
    get() = (entity as? MegaGameEntity ?: missing("game entity")).dead

val MegaPlayer.invincible: Boolean
    get() = (entity as? IDamageable)?.invincible ?: true

val MegaPlayer.damaged: Boolean
    get() = (entity as? AbstractHealthEntity)?.damaged == true

private val MegaPlayer.health: IHealthEntity
    get() = entity as? IHealthEntity ?: missing("health")

val MegaPlayer.healthRatio: Float
    get() = health.getHealthRatio()

val MegaPlayer.currentHealth: Int
    get() = health.getCurrentHealth()

val MegaPlayer.maxHealth: Int
    get() = health.getMaxHealth()

fun MegaPlayer.translateHealth(value: Int) = health.translateHealth(value)

fun MegaPlayer.depleteHealth() = health.depleteHealth()

val MegaPlayer.lightOrNull: PlayerLight?
    get() = entity.getComponent(PlayerLightComponent::class)?.light?.invoke()

fun MegaPlayer.onGravityDeviceSwitch() {
    entity.getComponent(GravityDeviceComponent::class)?.onSwitch?.invoke()
}

fun MegaPlayer.isBehaviorActive(key: Any) =
    entity.getComponent(BehaviorsComponent::class)?.isBehaviorActive(key) == true

fun MegaPlayer.isAnyBehaviorActive(vararg keys: Any) =
    entity.getComponent(BehaviorsComponent::class)?.isAnyBehaviorActive(*keys) == true

val MegaPlayer.feetFixture: Fixture
    get() = body.getProperty(ConstKeys.FEET) as Fixture

val MegaPlayer.leftSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", Fixture::class)!!

val MegaPlayer.rightSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", Fixture::class)!!

val MegaPlayer.damageableFixture: Fixture
    get() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

val MegaPlayer.headFixture: Fixture
    get() = body.getProperty(ConstKeys.HEAD, Fixture::class)!!

val MegaPlayer.feetGravityFixture: Fixture
    get() = body.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as Fixture
