package com.megaman.maverick.game.world.body

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.objects.PhasedPropertyKey
import com.maverick.game.engine.common.objects.PropertyKey
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.VelocityAlteration

object Receivers {

    val VELOCITY_ALTERATION =
        PropertyKey<(alterableBodyFixture: IFixture, delta: Float, state: ProcessState) -> VelocityAlteration>(
            ConstKeys.VELOCITY_ALTERATION
        )

    val FILTER = PropertyKey<(other: IFixture) -> Boolean>(ConstKeys.FILTER)

    val RUNNABLE = PropertyKey<() -> Unit>(ConstKeys.RUNNABLE)

    val CONSUMER = PropertyKey<(state: ProcessState, other: IFixture) -> Unit>(ConstKeys.CONSUMER)

    val FORCE_ALTERATION =
        PhasedPropertyKey<(alteration: VelocityAlteration) -> Unit>("${ConstKeys.FORCE}_${ConstKeys.VELOCITY_ALTERATION}")

    val HIT_BY_WATER = PropertyKey<(water: IWater) -> Unit>(ConstKeys.HIT_WATER)

    val HIT_BY_EXPLOSION = PropertyKey<(explosion: IBodyEntity) -> Unit>(ConstKeys.HIT_BY_EXPLOSION)

    val HIT_BY_BODY = PropertyKey<(body: IBodyEntity, state: ProcessState) -> Unit>(ConstKeys.HIT_BY_BODY)

    val HIT_BY_SIDE = PropertyKey<(side: IFixture, state: ProcessState) -> Unit>(ConstKeys.HIT_BY_SIDE)

    val HIT_BY_BLOCK = PhasedPropertyKey<(block: Block, delta: Float) -> Unit>(ConstKeys.HIT_BY_BLOCK)

    val HIT_BY_PLAYER = PropertyKey<(player: Megaman) -> Unit>(ConstKeys.HIT_BY_PLAYER)

    val HIT_BY_PROJECTILE = PropertyKey<(projectile: IProjectileEntity) -> Unit>(ConstKeys.HIT_BY_PROJECTILE)

    val HIT_BY_FEET = PhasedPropertyKey<(feet: IFixture, delta: Float) -> Unit>(ConstKeys.HIT_BY_FEET)

    val HIT_BY_DAMAGEABLE =
        PropertyKey<(damageable: IDamageable, state: ProcessState) -> Unit>(ConstKeys.HIT_BY_DAMAGEABLE)

    val STICK_TO_BLOCK = PropertyKey<(state: ProcessState, blockFixture: IFixture) -> Boolean>(ConstKeys.STICK_TO_BLOCK)

    val HIT_BY_SHIELD = PropertyKey<(state: ProcessState, shieldFixture: IFixture) -> Unit>(ConstKeys.HIT_BY_SHIELD)

    val FEET_BLOCK_CONTACT = PropertyKey<(state: ProcessState, block: Block) -> Unit>("feet_block_contact")

    val WATER_CONTACT = PropertyKey<(state: ProcessState, wasInWater: Boolean) -> Unit>("water_contact")

    val ICE_FRICTION_FILTER = PropertyKey<() -> Boolean>("ice_friction_filter")

    val SIDE_ICE_FRICTION = PropertyKey<() -> Boolean>("side_ice_friction")

    val NO_PROJECTILE_COLLISION_EXCEPTION =
        PropertyKey<(projectile: IProjectileEntity, fixture: IFixture) -> Boolean>(
            "${FixtureLabel.NO_PROJECTILE_COLLISION}_${ConstKeys.EXCEPTION}"
        )
}

fun <R : Any> IFixture.setReceiver(key: PropertyKey<R>, receiver: R): IFixture {
    putProperty(key, receiver)
    return this
}

fun <R : Any> IFixture.setReceiver(key: PhasedPropertyKey<R>, state: ProcessState, receiver: R) =
    setReceiver(key[state], receiver)

fun <R : Any> IFixture.receiver(key: PropertyKey<R>) = getProperty(key)

fun <R : Any> IFixture.receiver(key: PhasedPropertyKey<R>, state: ProcessState) = getProperty(key[state])
