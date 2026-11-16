package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.setToDirection
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.utils.OrbitUtils
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.GEM_COLORS
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEMS_ANGLES
import com.megaman.maverick.game.entities.bosses.PreciousWoman.ShieldGemDef
import com.megaman.maverick.game.entities.bosses.RodentMan.Companion.SLASH_WAVE_SPEED
import com.megaman.maverick.game.entities.decorations.SlashDissipation
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.weapons.WeaponState
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.isSensing

private const val TAG = MegamanWeaponsHandler.TAG

private fun computeShootingDown(megaman: Megaman): Boolean {
    val services = megaman.services
    val shootingDown = (megaman.isBehaviorActive(BehaviorType.JUMPING) || !megaman.bodyOverGround) &&
        !megaman.isAnyBehaviorActive(BehaviorType.AIR_DASHING, BehaviorType.WALL_SLIDING) &&
        megaman.input.isPressed(MegaControllerButton.DOWN)

    megaman.putProperty("${ConstKeys.SHOOT}_${ConstKeys.DOWN}", shootingDown)

    if (shootingDown) {
        GameLogger.debug(
            "${Megaman.TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
            "computeShootingDown(): shooting down"
        )

        if (megaman.hasProperty("${ConstKeys.SHOOT}_${ConstKeys.DOWN}_${ConstKeys.ID}")) {
            val id = megaman.removeProperty("${ConstKeys.SHOOT}_${ConstKeys.DOWN}_${ConstKeys.ID}", String::class)!!
            services.updatables.remove(id)

            GameLogger.debug(
                "${Megaman.TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
                "shootMegaBuster(): removed shoot down id runnable: $id"
            )
        }

        val id = MegaUtilMethods.delayRun(services, MegamanValues.SHOOT_ANIM_TIME) {
            GameLogger.debug(
                "${Megaman.TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
                "shootMegaBuster(): set shoot down to false"
            )
            megaman.putProperty("${ConstKeys.SHOOT}_${ConstKeys.DOWN}", false)
        }

        megaman.putProperty("${ConstKeys.SHOOT}_${ConstKeys.DOWN}_${ConstKeys.ID}", id)

        GameLogger.debug(
            "${Megaman.TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
            "shootMegaBuster(): put shoot down run delay: $id"
        )
    }

    return shootingDown
}

private fun shootMegaBuster(megaman: Megaman, stat: MegaChargeStatus) {
    GameLogger.debug(TAG, "fireMegaBuster(): stat=$stat")

    val shootingDown = computeShootingDown(megaman)
    if (shootingDown && stat != MegaChargeStatus.NOT_CHARGED) {
        GameLogger.debug(
            "${Megaman.TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
            "shootMegaBuster(): shooting down"
        )
        val velocity = GameObjectPools.fetch(Vector2::class)
            .setToDirection(megaman.direction)
            .scl(MegamanValues.SHOOT_DOWN_IMPULSE_Y_CHARGED * ConstVals.PPM.toFloat())
        megaman.body.physics.velocity.set(velocity)
    }

    val trajectory = GameObjectPools.fetch(Vector2::class)

    if (shootingDown) when (megaman.direction) {
        Direction.UP -> trajectory.y = -MegamanValues.BULLET_VEL
        Direction.DOWN -> trajectory.y = MegamanValues.BULLET_VEL
        Direction.LEFT -> trajectory.x = MegamanValues.BULLET_VEL
        Direction.RIGHT -> trajectory.x = -MegamanValues.BULLET_VEL
    } else when (megaman.direction) {
        Direction.UP -> trajectory.x = MegamanValues.BULLET_VEL * megaman.facing.value
        Direction.DOWN -> trajectory.x = MegamanValues.BULLET_VEL * -megaman.facing.value
        Direction.LEFT -> trajectory.y = MegamanValues.BULLET_VEL * megaman.facing.value
        Direction.RIGHT -> trajectory.y = MegamanValues.BULLET_VEL * -megaman.facing.value
    }

    trajectory.scl(ConstVals.PPM.toFloat())

    if (megaman.applyMovementScalarToBullet) trajectory.scl(megaman.movementScalar)

    val position = if (shootingDown)
        GameObjectPools.fetch(Vector2::class)
            .add(0.25f * megaman.facing.value * ConstVals.PPM, -0.75f * ConstVals.PPM)
            .rotateDeg(megaman.direction.rotation)
            .add(megaman.body.getCenter())
    else megaman.weaponsHandler.getSpawnPosition()

    val direction = if (shootingDown) megaman.direction.getRotatedClockwise() else megaman.direction

    val props = props(
        ConstKeys.OWNER pairTo megaman,
        ConstKeys.POSITION pairTo position,
        ConstKeys.DIRECTION pairTo direction,
        ConstKeys.TRAJECTORY pairTo trajectory,
    )

    val busterState = megaman.weaponsHandler.stateOf(MegamanWeapons.MEGA_BUSTER)

    when (stat) {
        MegaChargeStatus.NOT_CHARGED -> {
            megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, loop = false)

            val bullet = MegaEntityFactory.fetch(Bullet::class)!!
            bullet.spawn(props)
            busterState.addSpawned(stat, bullet)
        }

        MegaChargeStatus.HALF_CHARGED, MegaChargeStatus.FULLY_CHARGED -> {
            megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, loop = false)
            megaman.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            props.put(ConstKeys.BOOLEAN, stat == MegaChargeStatus.FULLY_CHARGED)
            props.put("${ConstKeys.SHOOT}_${ConstKeys.DOWN}", shootingDown)

            val chargedShot = MegaEntityFactory.fetch(ChargedShot::class)!!
            chargedShot.spawn(props)
            busterState.addSpawned(stat, chargedShot)
        }
    }
}

internal fun fireMegaBuster(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) =
    shootMegaBuster(megaman, stat)

internal fun fireRushJet(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) =
    shootMegaBuster(megaman, stat)

internal fun fireFrigidShot(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    GameLogger.debug(TAG, "fireFrigidShot(): stat=$stat")

    val spawn = megaman.weaponsHandler.getSpawnPosition()

    val trajectory = GameObjectPools.fetch(Vector2::class)
        .set(MegamanValues.ICE_CUBE_VEL * ConstVals.PPM * megaman.facing.value, 0f)
        .rotateDeg(megaman.direction.rotation)

    val icecube = MegaEntityFactory.fetch(SmallIceCube::class)!!
    icecube.spawn(
        props(
            ConstKeys.OWNER pairTo megaman,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.GRAVITY_ON pairTo false,
            ConstKeys.FRICTION_X pairTo false,
            ConstKeys.FRICTION_Y pairTo false,
            ConstKeys.HIT_BY_BLOCK pairTo true,
            ConstKeys.TRAJECTORY pairTo trajectory
        )
    )

    megaman.requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, loop = false)
}

internal fun fireInfernalBarrage(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    GameLogger.debug(TAG, "fireInfernalBarrage(): stat=$stat")

    val services = megaman.services

    if (megaman.body.isSensing(BodySense.IN_WATER)) {
        GameLogger.debug(TAG, "fireInfernalBarrage(): in water")

        val smoke = MegaEntityFactory.fetch(SmokePuff::class)!!
        smoke.spawn(
            props(
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.POSITION pairTo megaman.weaponsHandler.getSpawnPosition()
            )
        )

        services.audio.playSound(SoundAsset.WHOOSH_SOUND, loop = false)

        return
    } else if (services.levelState.currentLevel!! == LevelDefinition.MOON_MAN) {
        GameLogger.debug(TAG, "fireInfernalBarrage(): in Moon Man's stage")

        val explosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SCALAR pairTo 3f,
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.POSITION pairTo megaman.weaponsHandler.getSpawnPosition(),
            )
        )
        return
    }

    val spawn = when (megaman.direction) {
        Direction.UP -> megaman.body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(ConstVals.PPM.toFloat() * megaman.facing.value, 0.25f * ConstVals.PPM)
        Direction.DOWN -> megaman.body.getPositionPoint(Position.TOP_CENTER)
            .add(ConstVals.PPM.toFloat() * -megaman.facing.value, -0.25f * ConstVals.PPM)
        Direction.LEFT -> megaman.body.getPositionPoint(Position.CENTER_RIGHT)
            .add(-ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat() * megaman.facing.value)
        Direction.RIGHT -> megaman.body.getPositionPoint(Position.CENTER_LEFT)
            .add(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat() * -megaman.facing.value)
    }

    val trajectory = when (megaman.direction) {
        Direction.UP -> GameObjectPools.fetch(Vector2::class)
            .set(MegamanValues.MAGMA_WAVE_VEL * megaman.facing.value * ConstVals.PPM, 0f)
        Direction.DOWN -> GameObjectPools.fetch(Vector2::class)
            .set(MegamanValues.MAGMA_WAVE_VEL * -megaman.facing.value * ConstVals.PPM, 0f)
        Direction.LEFT -> GameObjectPools.fetch(Vector2::class)
            .set(0f, MegamanValues.MAGMA_WAVE_VEL * megaman.facing.value * ConstVals.PPM)
        Direction.RIGHT -> GameObjectPools.fetch(Vector2::class)
            .set(0f, MegamanValues.MAGMA_WAVE_VEL * -megaman.facing.value * ConstVals.PPM)
    }

    val fireWave = MegaEntityFactory.fetch(MagmaWave::class)!!
    fireWave.spawn(
        props(
            ConstKeys.OWNER pairTo megaman,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.FACING pairTo megaman.facing,
            ConstKeys.DIRECTION pairTo megaman.direction,
        )
    )

    state.addSpawned(stat, fireWave)
    megaman.requestToPlaySound(SoundAsset.CRASH_BOMBER_SOUND, loop = false)
}

internal fun fireMoonScythes(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    GameLogger.debug(TAG, "fireMoonScythes(): stat=$stat")
    state as MoonScythesState
    state.maxWaitTime.reset()

    MegamanValues.MOON_SCYTHE_DEG_OFFSETS.forEach { degreeOffset ->
        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(0f, MegamanValues.MOON_SCYTHE_SPEED * ConstVals.PPM)
            .rotateDeg(
                degreeOffset
                    .times(megaman.facing.value)
                    .plus(if (megaman.isFacing(Facing.LEFT)) 90f else 270f)
                    .plus(megaman.direction.rotation)
            )

        val scythe = MegaEntityFactory.fetch(MoonScythe::class)!!
        scythe.spawn(
            props(
                ConstKeys.FADE pairTo false,
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.ROTATION pairTo trajectory.angleDeg(),
                ConstKeys.POSITION pairTo megaman.weaponsHandler.getSpawnPosition(),
                "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo megaman.movementScalar
            )
        )

        state.addSpawned(stat, scythe)
        megaman.requestToPlaySound(SoundAsset.WHIP_SOUND, loop = false)
    }

    state.fadeExtraScythes()
}

internal fun firePreciousGuard(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    state as PreciousGuardState
    state.maxCooldownTimer.reset()

    val clusters = state.spawned.get(MegaChargeStatus.NOT_CHARGED)
    if (clusters.size > 0) {
        for (cluster in clusters) {
            cluster as PreciousGemCluster
            cluster.gems.values().forEach { it.released = true }
        }

        state.clearAllSpawned()
        GameLogger.debug(TAG, "firePreciousGuard(): release gems, clear all spawned")

        return
    }

    val shieldGems = OrderedMap<PreciousGem, ShieldGemDef>()

    for (i in 0 until SHIELD_GEMS_ANGLES.size) {
        val angle = SHIELD_GEMS_ANGLES[i]

        val spawn = OrbitUtils.calculateOrbitalPosition(
            angle,
            2f * ConstVals.PPM,
            megaman.body.getCenter(),
            GameObjectPools.fetch(Vector2::class)
        )

        val color = GEM_COLORS[i % GEM_COLORS.size]

        val gem = MegaEntityFactory.fetch(PreciousGem::class)!!
        gem.spawn(
            props(
                ConstKeys.STATE pairTo 1,
                ConstKeys.COLOR pairTo color,
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
            )
        )

        GameLogger.debug(TAG, "firePreciousGuard(): create gem: hashCode=${gem.hashCode()}")
        shieldGems.put(gem, ShieldGemDef(angle, 2f * ConstVals.PPM, released = false))
    }

    GameLogger.debug(TAG, "firePreciousGuard(): create cluster: shieldGems.size=${shieldGems.size}")

    val cluster = MegaEntityFactory.fetch(PreciousGemCluster::class)!!
    cluster.spawn(
        props(
            ConstKeys.OWNER pairTo megaman,
            ConstKeys.POSITION pairTo megaman.body.getCenter(),
            ConstKeys.ORIGIN pairTo megaman.body.getCenter(),
            PreciousGem.TAG pairTo shieldGems,
            "${ConstKeys.SPIN}_${ConstKeys.SPEED}" pairTo 0.5f,
            "${ConstKeys.MAX}_${ConstKeys.DISTANCE}" pairTo
                MegamanValues.SHIELD_GEM_MAX_DIST * ConstVals.PPM,
            "${ConstKeys.DISTANCE}_${ConstKeys.DELTA}" pairTo
                MegamanValues.SHIELD_GEM_DISTANCE_DELTA * ConstVals.PPM
        )
    )

    state.clearAllSpawned()
    state.addSpawned(MegaChargeStatus.NOT_CHARGED, cluster)
}

internal fun destroyPreciousGuardGems(state: WeaponState) {
    state.spawned.values().forEach { entities ->
        val iter = entities.iterator()
        while (iter.hasNext) {
            val cluster = iter.next() as PreciousGemCluster
            cluster.gems.keys().forEach { it.destroy() }
            cluster.gems.clear()
            cluster.destroy()
            iter.remove()
        }
    }
}

private fun throwAxe(megaman: Megaman, state: WeaponState) {
    GameLogger.debug(TAG, "throwAxe()")
    if (!megaman.existenceState.liveAfterUpdate) return

    val impulse = GameObjectPools.fetch(Vector2::class)
    when (megaman.direction) {
        Direction.UP -> impulse.set(
            MegamanValues.AXE_IMPULSE_X * megaman.facing.value,
            MegamanValues.AXE_IMPULSE_Y
        )

        Direction.DOWN -> impulse.set(
            MegamanValues.AXE_IMPULSE_X * -megaman.facing.value,
            -MegamanValues.AXE_IMPULSE_Y
        )

        Direction.LEFT -> impulse.set(
            -MegamanValues.AXE_IMPULSE_Y,
            MegamanValues.AXE_IMPULSE_X * megaman.facing.value
        )

        Direction.RIGHT -> impulse.set(
            MegamanValues.AXE_IMPULSE_Y,
            MegamanValues.AXE_IMPULSE_X * -megaman.facing.value,
        )
    }
    impulse.scl(ConstVals.PPM.toFloat())

    if (megaman.services.levelState.currentLevel!! == LevelDefinition.MOON_MAN ||
        megaman.body.isSensing(BodySense.IN_WATER)
    ) impulse.scl(MegamanValues.AXE_SLOW_SCALAR)

    val spawn = megaman.body.getCenter()
    when (megaman.direction) {
        Direction.UP -> spawn.add(-0.1f * ConstVals.PPM * megaman.facing.value, 0.75f * ConstVals.PPM)
        Direction.DOWN -> spawn.add(0.1f * ConstVals.PPM * megaman.facing.value, -0.75f * ConstVals.PPM)
        Direction.LEFT -> spawn.add(-0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM * megaman.facing.value)
        Direction.RIGHT -> spawn.add(0.75f * ConstVals.PPM, -0.1f * ConstVals.PPM * megaman.facing.value)
    }

    val axe = MegaEntityFactory.fetch(Axe::class)!!
    axe.spawn(
        props(
            ConstKeys.OWNER pairTo megaman,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.IMPULSE pairTo impulse,
            ConstKeys.DIRECTION pairTo megaman.direction,
            "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}" pairTo megaman.gravityScalar,
            "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo megaman.movementScalar
        )
    )

    state.addSpawned(MegaChargeStatus.NOT_CHARGED, axe)
}

internal fun fireAxeSwinger(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    MegaUtilMethods.delayRun(megaman.services, 0.1f) { throwAxe(megaman, state) }
}

internal fun fireNeedleSpin(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    megaman.services.audio.playSound(SoundAsset.WHOOSH_SOUND, loop = false)
    GameLogger.debug(TAG, "fireNeedleSpin()")

    val direction = megaman.direction
    val impulseScalar = MegamanValues.NEEDLE_IMPULSE * ConstVals.PPM * megaman.movementScalar
    val gravMag = MegamanValues.NEEDLE_GRAV * ConstVals.PPM * megaman.gravityScalar

    for (i in 0 until MegamanValues.NEEDLE_ANGLES.size) {
        val angle = MegamanValues.NEEDLE_ANGLES[i]
        val xOffset = MegamanValues.NEEDLE_X_OFFSETS[i]

        val position = megaman.body.getCenter()
        when (direction) {
            Direction.UP -> position.add(xOffset * ConstVals.PPM, MegamanValues.NEEDLE_Y_OFFSET * ConstVals.PPM)
            Direction.DOWN -> position.add(xOffset * ConstVals.PPM, -MegamanValues.NEEDLE_Y_OFFSET * ConstVals.PPM)
            Direction.LEFT -> position.add(-MegamanValues.NEEDLE_Y_OFFSET * ConstVals.PPM, xOffset * ConstVals.PPM)
            Direction.RIGHT -> position.add(MegamanValues.NEEDLE_Y_OFFSET * ConstVals.PPM, xOffset * ConstVals.PPM)
        }

        val impulse = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> impulse.set(0f, impulseScalar)
            Direction.DOWN -> impulse.set(0f, -impulseScalar)
            Direction.LEFT -> impulse.set(-impulseScalar, 0f)
            Direction.RIGHT -> impulse.set(impulseScalar, 0f)
        }.rotateDeg(angle)

        val gravity = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> gravity.set(0f, gravMag)
            Direction.DOWN -> gravity.set(0f, -gravMag)
            Direction.LEFT -> gravity.set(-gravMag, 0f)
            Direction.RIGHT -> gravity.set(gravMag, 0f)
        }

        val needle = MegaEntityFactory.fetch(Needle::class)!!
        needle.spawn(
            props(
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.GRAVITY pairTo gravity,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position,
            )
        )

        state.addSpawned(MegaChargeStatus.NOT_CHARGED, needle)
    }

    val vel = megaman.body.physics.velocity

    var impulse =
        ((MegamanValues.NEEDLE_SPIN_MEGAMAN_IMPULSE_Y * ConstVals.PPM) / megaman.movementScalar) / megaman.gravityScalar
    if (megaman.isBehaviorActive(BehaviorType.SWIMMING)) impulse *= MegamanValues.NEEDLE_SPIN_WATER_SCALAR

    when (megaman.direction) {
        Direction.UP -> vel.y = impulse
        Direction.DOWN -> vel.y = -impulse
        Direction.LEFT -> vel.x = -impulse
        Direction.RIGHT -> vel.x = impulse
    }
}

private fun clawsComboBroken(megaman: Megaman) = !megaman.bodyOverGround ||
    megaman.isAnyBehaviorActive(
        BehaviorType.WALL_SLIDING,
        BehaviorType.GROUND_SLIDING,
        BehaviorType.AIR_DASHING,
        BehaviorType.JUMPING,
        BehaviorType.CROUCHING
    )

internal fun fireRodentClaws(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    state as RodentClawsState
    state.nextSlash(comboBroken = clawsComboBroken(megaman))

    val slashKey = state.slashIndex
    GameLogger.debug(TAG, "fireRodentClaws(): slashIndex=$slashKey")

    val slashAngles = when (slashKey) {
        1 -> gdxArrayOf(
            if (megaman.isFacing(Facing.LEFT)) megaman.services.random.nextFloat(165f, 180f)
            else megaman.services.random.nextFloat(0f, 15f)
        )

        2 -> gdxArrayOf(
            if (megaman.isFacing(Facing.LEFT)) megaman.services.random.nextFloat(150f, 165f)
            else megaman.services.random.nextFloat(30f, 45f)
        )

        else -> when (megaman.facing) {
            Facing.LEFT -> gdxArrayOf(135f, 180f)
            else -> gdxArrayOf(0f, 45f)
        }
    }.map { angle -> angle + megaman.direction.rotation }

    val wallSlideOffset = ConstVals.PPM * if (megaman.isBehaviorActive(BehaviorType.WALL_SLIDING)) 0.5f else 0f

    val slashWaveCenter = GameObjectPools.fetch(Vector2::class)
        .set(megaman.body.getCenter())
        .also {
            when (megaman.direction) {
                Direction.UP -> it.add(0.25f * ConstVals.PPM * megaman.facing.value, wallSlideOffset)
                Direction.DOWN -> it.add(-0.25f * ConstVals.PPM * megaman.facing.value, -wallSlideOffset)
                Direction.LEFT -> it.add(-wallSlideOffset, -0.25f * ConstVals.PPM * megaman.facing.value)
                Direction.RIGHT -> it.add(wallSlideOffset, 0.25f * ConstVals.PPM * megaman.facing.value)
            }
        }

    slashAngles.forEach { slashAngle ->
        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(0f, SLASH_WAVE_SPEED * ConstVals.PPM)
            .setAngleDeg(slashAngle)

        val slashWave = MegaEntityFactory.fetch(SlashWave::class)!!
        slashWave.spawn(
            props(
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.DISSIPATE pairTo true,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.POSITION pairTo slashWaveCenter
            )
        )
    }

    val slashDissipationOffsetX = when (slashKey) {
        1 -> 1f
        2 -> 0.5f
        else -> 0.75f
    }

    val slashDissipationCenter = GameObjectPools.fetch(Vector2::class)
        .set(megaman.body.getCenter())
        .also {
            when (megaman.direction) {
                Direction.UP -> it.add(slashDissipationOffsetX * ConstVals.PPM * megaman.facing.value, wallSlideOffset)
                Direction.DOWN -> it.add(-slashDissipationOffsetX * ConstVals.PPM * megaman.facing.value, -wallSlideOffset)
                Direction.LEFT -> it.add(-wallSlideOffset, slashDissipationOffsetX * ConstVals.PPM * megaman.facing.value)
                Direction.RIGHT -> it.add(wallSlideOffset, slashDissipationOffsetX * ConstVals.PPM * megaman.facing.value)
            }
        }

    val slashDissipation = MegaEntityFactory.fetch(SlashDissipation::class)!!
    slashDissipation.spawn(
        props(
            ConstKeys.FACING pairTo megaman.facing,
            ConstKeys.POSITION pairTo slashDissipationCenter,
            ConstKeys.ROTATION pairTo megaman.direction.rotation
        )
    )

    state.addSpawned(MegaChargeStatus.NOT_CHARGED, slashDissipation)
    megaman.requestToPlaySound(SoundAsset.WHIP_V2_SOUND, loop = false)
}

internal fun fireReactorShot(megaman: Megaman, state: WeaponState, stat: MegaChargeStatus) {
    val shootingDown = computeShootingDown(megaman)

    val trajectory = GameObjectPools.fetch(Vector2::class)

    if (shootingDown) when (megaman.direction) {
        Direction.UP -> trajectory.y = -MegamanValues.BULLET_VEL
        Direction.DOWN -> trajectory.y = MegamanValues.BULLET_VEL
        Direction.LEFT -> trajectory.x = MegamanValues.BULLET_VEL
        Direction.RIGHT -> trajectory.x = -MegamanValues.BULLET_VEL
    } else when (megaman.direction) {
        Direction.UP -> trajectory.x = MegamanValues.BULLET_VEL * megaman.facing.value
        Direction.DOWN -> trajectory.x = MegamanValues.BULLET_VEL * -megaman.facing.value
        Direction.LEFT -> trajectory.y = MegamanValues.BULLET_VEL * megaman.facing.value
        Direction.RIGHT -> trajectory.y = MegamanValues.BULLET_VEL * -megaman.facing.value
    }

    trajectory.scl(ConstVals.PPM.toFloat())

    if (megaman.applyMovementScalarToBullet) trajectory.scl(megaman.movementScalar)

    val position = if (shootingDown)
        GameObjectPools.fetch(Vector2::class)
            .add(0.25f * megaman.facing.value * ConstVals.PPM, -0.75f * ConstVals.PPM)
            .rotateDeg(megaman.direction.rotation)
            .add(megaman.body.getCenter())
    else megaman.weaponsHandler.getSpawnPosition()

    val direction = if (shootingDown) megaman.direction.getRotatedClockwise() else megaman.direction

    val props = props(
        ConstKeys.BIG pairTo true,
        ConstKeys.ACTIVE pairTo true,
        ConstKeys.OWNER pairTo megaman,
        ConstKeys.GRAVITY_ON pairTo true,
        ConstKeys.POSITION pairTo position,
        ConstKeys.DIRECTION pairTo direction,
        ConstKeys.TRAJECTORY pairTo trajectory,
    )

    megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, loop = false)

    val shot = MegaEntityFactory.fetch(ReactorManProjectile::class)!!
    shot.spawn(props)
    state.addSpawned(MegaChargeStatus.NOT_CHARGED, shot)
}
