package com.megaman.maverick.game.entities

import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.screens.levels.camera.IFocusable

private fun IGameEntity.missing(part: String): Nothing = throw IllegalStateException("$this has no $part")

val IGameEntity.focus: IFocusable
    get() = this as? IFocusable ?: missing("camera focus")

val IGameEntity.invincible: Boolean
    get() = (this as? IDamageableEntity)?.invincible ?: true

val IGameEntity.damaged: Boolean
    get() = (this as? AbstractHealthEntity)?.damaged == true

private val IGameEntity.health: IHealthEntity
    get() = this as? IHealthEntity ?: missing("health")

val IGameEntity.healthRatio: Float
    get() = health.getHealthRatio()

val IGameEntity.currentHealth: Int
    get() = health.getCurrentHealth()

val IGameEntity.maxHealth: Int
    get() = health.getMaxHealth()

fun IGameEntity.translateHealth(value: Int) = health.translateHealth(value)

fun IGameEntity.depleteHealth() = health.depleteHealth()

// TODO: the shoot animation is Megaman's; it moves to the loadout with item 024
val IGameEntity.shooting: Boolean
    get() = (this as? Megaman)?.shooting == true

val IGameEntity.feetFixture: Fixture
    get() = body.getProperty(ConstKeys.FEET) as Fixture

val IGameEntity.leftSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", Fixture::class)!!

val IGameEntity.rightSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", Fixture::class)!!

val IGameEntity.damageableFixture: Fixture
    get() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

val IGameEntity.headFixture: Fixture
    get() = body.getProperty(ConstKeys.HEAD, Fixture::class)!!

val IGameEntity.feetGravityFixture: Fixture
    get() = body.getProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") as Fixture
