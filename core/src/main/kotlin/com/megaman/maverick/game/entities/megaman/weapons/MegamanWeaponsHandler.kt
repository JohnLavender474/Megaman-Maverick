package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.common.utils.OrbitUtils
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.GEM_COLORS
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEMS_ANGLES
import com.megaman.maverick.game.entities.bosses.PreciousWoman.Companion.SHIELD_GEM_START_OFFSET
import com.megaman.maverick.game.entities.bosses.PreciousWoman.ShieldGemDef
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.GROUND_SLIDE_SPRITE_OFFSET_Y
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.min

data class MegaWeaponHandler(
    var cooldown: Timer,
    var ammo: Int = MegamanValues.MAX_WEAPON_AMMO,
    var normalCost: (MegaWeaponHandler) -> Int = { 0 },
    var halfChargedCost: (MegaWeaponHandler) -> Int = { 0 },
    var fullyChargedCost: (MegaWeaponHandler) -> Int = { 0 },
    var chargeable: (MegaWeaponHandler) -> Boolean = { true },
    var updateFunction: ((MegaGameEntity, MegaChargeStatus, Float) -> Unit)? = null,
    var canFireWeapon: (MegaWeaponHandler, MegaChargeStatus) -> Boolean = { _, _ -> true },
) : Updatable {

    companion object {
        const val TAG = "MegaWeaponHandler"
    }

    private val spawned = OrderedMap<MegaChargeStatus, OrderedSet<MegaGameEntity>>()

    init {
        MegaChargeStatus.entries.forEach { spawned.put(it, OrderedSet()) }
    }

    fun getSpawnedCount(vararg stats: MegaChargeStatus) = getSpawnedCount(stats.asIterable())

    fun getSpawnedCount(stats: Iterable<MegaChargeStatus>): Int {
        var count = 0
        for (stat in stats) {
            val statCount = spawned[stat]?.size
            count += statCount ?: 0
        }
        return count
    }

    fun cullAllDead() = spawned.values().forEach { set ->
        val iter = set.iterator()
        while (iter.hasNext) {
            val entity = iter.next()
            if (entity.dead) {
                iter.remove()
                GameLogger.debug(TAG, "cullAllDead(): culled entity=$entity")
            }
        }
    }

    fun clearAllSpawned() = spawned.values().forEach { set -> set.clear() }

    fun addSpawned(stat: MegaChargeStatus, entity: MegaGameEntity) {
        spawned[stat].add(entity)
        GameLogger.debug(TAG, "addSpawned(): stat=$stat, entity=$entity, spawned=$spawned")
    }

    override fun update(delta: Float) {
        cullAllDead()
        cooldown.update(delta)
        if (updateFunction != null) spawned.forEach { entry ->
            val chargeStatus = entry.key
            val entities = entry.value
            entities.forEach { entity -> updateFunction?.invoke(entity, chargeStatus, delta) }
        }
    }

    fun getSpawned() = spawned

    override fun toString() = "MegaWeaponHandler{" +
        "cooldown=${UtilMethods.roundFloat(cooldown.getRatio() * 100, 2)}%," +
        "ammo=$ammo"
}

class MegamanWeaponsHandler(private val megaman: Megaman /*, private val weaponSpawns: OrderedMap<String, IntPair> */) :
    Updatable, Resettable {

    companion object {
        const val TAG = "MegamanWeaponsHandler"
    }

    private val game: MegamanMaverickGame
        get() = megaman.game

    private val weaponHandlers = ObjectMap<MegamanWeapon, MegaWeaponHandler>()

    override fun reset() {
        weaponHandlers.values().forEach { entry ->
            entry.clearAllSpawned()
            entry.cooldown.setToEnd()
        }

        GameLogger.debug(TAG, "reset()")
    }

    override fun update(delta: Float) = weaponHandlers.values().forEach { entry -> entry.update(delta) }

    fun getSpawnPosition(weapon: MegamanWeapon): Vector2 {
        /*
        val rawKey = (megaman.animators[MEGAMAN_SPRITE_KEY] as Animator).currentKey
        if (rawKey != null) {
            val key = MegamanAnimations.splitFullKey(rawKey)[0]
            if (weaponSpawns.containsKey(key)) {
                GameLogger.debug(TAG, "getSpawnPosition(): has magic pixel weapon spawn for key=$key, rawKey=$rawKey")

                val sprite = megaman.sprites[MEGAMAN_SPRITE_KEY]

                val (rawRegionX, rawRegionY) = weaponSpawns[key]
                GameLogger.debug(TAG, "getSpawnPosition(): rawRegionX=$rawRegionX, rawRegionY=$rawRegionY")

                out.set(
                    sprite.x + (rawRegionX * MEGAMAN_SPRITE_SIZE * ConstVals.PPM),
                    sprite.y + (rawRegionY * MEGAMAN_SPRITE_SIZE * ConstVals.PPM)
                )
                GameLogger.debug(TAG, "getSpawnPosition(): raw out = $out")

                if (megaman.shouldFlipSpriteX()) out.x -= MEGAMAN_SPRITE_SIZE * ConstVals.PPM / 2f
                if (megaman.shouldFlipSpriteY()) out.y -= MEGAMAN_SPRITE_SIZE * ConstVals.PPM / 2f
                GameLogger.debug(TAG, "getSpawnPosition(): out after flip = $out")

                val spriteCenter = sprite.boundingRectangle.getCenter()
                out.rotateAroundOrigin(megaman.direction.rotation, spriteCenter.x, spriteCenter.y)
                GameLogger.debug(TAG, "getSpawnPosition(): rotated out = $out")

                GameLogger.debug(TAG, "getSpawnPosition(): final out = $out")
                return out
            }
        }

        GameLogger.debug(TAG, "getSpawnPosition(): no magic pixel weapon spawn for rawKey=$rawKey")
         */

        val out = GameObjectPools.fetch(Vector2::class).set(megaman.body.getCenter())

        var xOffset = megaman.facing.value * when {
            megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
            megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
            megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.5f
            megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 1f
            !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
            megaman.slipSliding -> 0.75f
            megaman.running -> 1.75f
            else -> 1f
        }

        var yOffset = when {
            megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
            megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.25f
            megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.1f
            megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.15f
            megaman.isBehaviorActive(BehaviorType.CROUCHING) -> 0.125f
            megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.15f
            !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> when (megaman.direction) {
                Direction.UP -> -0.05f
                else -> 0.1f
            }
            else -> 0f
        }

        if (megaman.direction.isVertical()) {
            out.x += xOffset * ConstVals.PPM
            out.y += (if (megaman.direction == Direction.DOWN) -yOffset else yOffset) * ConstVals.PPM

            if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
                var groundSlideOffset = GROUND_SLIDE_SPRITE_OFFSET_Y * ConstVals.PPM
                out.y += if (megaman.direction == Direction.UP) -groundSlideOffset else groundSlideOffset
            }
        } else {
            out.x += (if (megaman.direction == Direction.LEFT) -yOffset else yOffset) * ConstVals.PPM
            if (megaman.direction == Direction.LEFT && !megaman.body.isSensing(BodySense.FEET_ON_GROUND)) out.x -= 0.2f

            if (megaman.isAnyBehaviorActive(
                    BehaviorType.CROUCHING, BehaviorType.GROUND_SLIDING, BehaviorType.WALL_SLIDING
                )
            ) out.x += 0.1f * ConstVals.PPM * if (megaman.direction == Direction.RIGHT) -1f else 1f

            out.y += xOffset * ConstVals.PPM
        }

        return out
    }

    fun putWeapon(weapon: MegamanWeapon) {
        val entry = createWeaponEntry(weapon)
        weaponHandlers.put(weapon, entry)

        GameLogger.debug(TAG, "putWeapon(): weapon=$weapon, entry=$entry, weapons=${weaponHandlers.keys()}")
    }

    fun removeWeapon(weapon: MegamanWeapon) {
        if (megaman.currentWeapon == weapon) megaman.currentWeapon = MegamanWeapon.MEGA_BUSTER
        weaponHandlers.remove(weapon)

        GameLogger.debug(TAG, "removeWeapon(): weapon=$weapon, weapons=${weaponHandlers.keys()}")
    }

    private fun createWeaponEntry(weapon: MegamanWeapon) = when (weapon) {
        MegamanWeapon.MEGA_BUSTER -> MegaWeaponHandler(cooldown = Timer(0.1f))
        MegamanWeapon.RUSH_JETPACK -> MegaWeaponHandler(cooldown = Timer(0.1f), chargeable = { false })
        MegamanWeapon.ICE_CUBE -> MegaWeaponHandler(
            cooldown = Timer(0.25f),
            normalCost = { 3 },
            halfChargedCost = { 5 },
            fullyChargedCost = { 7 },
            chargeable = { _ -> false /* TODO: true */ }
        )

        MegamanWeapon.MAGMA_WAVE -> MegaWeaponHandler(
            cooldown = Timer(0.5f),
            normalCost = { 3 },
            halfChargedCost = { 5 },
            fullyChargedCost = { 7 },
            chargeable = { _ -> false /* TODO: !megaman.body.isSensing(BodySense.IN_WATER) */ },
            canFireWeapon = { _, _ -> !megaman.body.isSensing(BodySense.IN_WATER) }
        )

        MegamanWeapon.MOON_SCYTHE -> MegaWeaponHandler(
            cooldown = Timer(0.1f),
            normalCost = { 6 },
            halfChargedCost = { 12 },
            fullyChargedCost = { 12 },
            chargeable = chargeable@{ it ->
                return@chargeable false
                // TODO:
                /*
                val count = it.getSpawnedCount(MegaChargeStatus.entries)
                return@chargeable count <= MegamanValues.MAX_MOONS_BEFORE_SHOOT_AGAIN
                 */
            },
            canFireWeapon = canFireWeapon@{ it, _ ->
                val count = it.getSpawnedCount(MegaChargeStatus.entries)
                GameLogger.debug(TAG, "MOON_SCYTHE: canFireWeapon(): count=${count}")
                return@canFireWeapon count <= MegamanValues.MAX_MOONS_BEFORE_SHOOT_AGAIN
            }
        )

        MegamanWeapon.PRECIOUS_GUARD -> MegaWeaponHandler(
            cooldown = Timer(0.1f),
            normalCost = { if (it.getSpawnedCount(MegaChargeStatus.NOT_CHARGED) > 0) 0 else 4 },
            chargeable = chargeable@{ it -> return@chargeable false },
            canFireWeapon = canFireWeapon@{ it, _ ->
                if (it.getSpawnedCount(MegaChargeStatus.NOT_CHARGED) > 0) {
                    val cluster = it.getSpawned()
                        .get(MegaChargeStatus.NOT_CHARGED)
                        .first() as PreciousGemCluster
                    return@canFireWeapon cluster.gems.values().none { it.released }
                }
                return@canFireWeapon it.getSpawnedCount(MegaChargeStatus.entries) == 0
            },
            updateFunction = updateFunction@{ cluster, _, delta ->
                if (!cluster.spawned) return@updateFunction
                cluster as PreciousGemCluster
                cluster.origin.lerp(
                    megaman.getFocusPosition(),
                    min(1f, MegamanValues.SHIELD_GEM_LERP * ConstVals.PPM * delta)
                )
                if (cluster.gems.values().none { it.released }) cluster.body.setCenter(cluster.origin)
            }
        )
    }

    fun hasWeapon(weapon: MegamanWeapon) = weaponHandlers.containsKey(weapon)

    fun onChangeWeapon(current: MegamanWeapon, previous: MegamanWeapon?) {
        GameLogger.debug(TAG, "onChangeWeapon(): current=$current, previous=$previous")
        if (previous == MegamanWeapon.PRECIOUS_GUARD) {
            val all = weaponHandlers[MegamanWeapon.PRECIOUS_GUARD]?.getSpawned()
            all?.values()?.forEach { entities ->
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
    }

    fun isChargeable(weapon: MegamanWeapon) = hasWeapon(weapon) && weaponHandlers[weapon].let { it.chargeable(it) }

    fun translateAmmo(weapon: MegamanWeapon, delta: Int) {
        if (!hasWeapon(weapon)) return

        val weaponEntry = weaponHandlers[weapon]
        weaponEntry.ammo += delta

        when {
            weaponEntry.ammo >= MegamanValues.MAX_WEAPON_AMMO -> weaponEntry.ammo = MegamanValues.MAX_WEAPON_AMMO
            weaponEntry.ammo < 0 -> weaponEntry.ammo = 0
        }
    }

    fun setAllToMaxAmmo() = weaponHandlers.keys().forEach { setToMaxAmmo(it) }

    fun setToMaxAmmo(weapon: MegamanWeapon) {
        weaponHandlers[weapon]?.ammo = MegamanValues.MAX_WEAPON_AMMO
    }

    fun getAmmo(weapon: MegamanWeapon) = when {
        !hasWeapon(weapon) -> 0
        weapon == MegamanWeapon.MEGA_BUSTER -> MegamanValues.MAX_WEAPON_AMMO
        else -> weaponHandlers[weapon].ammo
    }

    fun isDepleted(weapon: MegamanWeapon) = getAmmo(weapon) == 0

    fun canFireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
        if (!megaman.ready) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman is not ready")
            return false
        }
        if (!hasWeapon(weapon)) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman doesn't have weapon")
            return false
        }
        if (megaman.frozen) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: megaman is frozen")
            return false
        }

        val handler = weaponHandlers[weapon]
        if (!handler.cooldown.isFinished()) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: cooldown is not finished")
        }
        if (!handler.canFireWeapon(handler, stat)) {
            GameLogger.debug(TAG, "canFireWeapon(): cannot fire $weapon: custom predicate failed")
            return false
        }

        val cost = when {
            handler.chargeable(handler) -> (when {
                weapon === MegamanWeapon.MEGA_BUSTER -> 0
                else -> when (stat) {
                    MegaChargeStatus.FULLY_CHARGED -> handler.fullyChargedCost(handler)
                    MegaChargeStatus.HALF_CHARGED -> handler.halfChargedCost(handler)
                    MegaChargeStatus.NOT_CHARGED -> handler.normalCost(handler)
                }
            })

            else -> handler.normalCost(handler)
        }

        val canFireWeapon = cost <= handler.ammo

        return canFireWeapon
    }

    fun fireWeapon(weapon: MegamanWeapon, statToTry: MegaChargeStatus): Boolean {
        GameLogger.debug(TAG, "fireWeapon(): weapon=$weapon, statToTry=$statToTry")

        var stat = statToTry

        if (!canFireWeapon(weapon, stat)) {
            GameLogger.debug(TAG, "fireWeapon(): cannot fire weapon")
            return false
        }

        if (!isChargeable(weapon)) {
            GameLogger.debug(TAG, "fireWeapon(): weapon not chargeable")
            stat = MegaChargeStatus.NOT_CHARGED
        }

        val handler = weaponHandlers[weapon]

        val cost = when {
            weapon.equalsAny(MegamanWeapon.MEGA_BUSTER, MegamanWeapon.RUSH_JETPACK) -> 0
            else -> when (stat) {
                MegaChargeStatus.FULLY_CHARGED -> handler.fullyChargedCost(handler)
                MegaChargeStatus.HALF_CHARGED -> handler.halfChargedCost(handler)
                MegaChargeStatus.NOT_CHARGED -> handler.normalCost(handler)
            }
        }

        val ammo = getAmmo(weapon)
        if (cost > ammo) {
            GameLogger.debug(TAG, "fireWeapon(): ammo is less than cost: ammo=$ammo, cost=$cost")
            return false
        }

        when (weapon) {
            MegamanWeapon.MEGA_BUSTER,
            MegamanWeapon.RUSH_JETPACK -> shootMegaBuster(stat)
            MegamanWeapon.ICE_CUBE -> shootIceCube(stat)
            MegamanWeapon.MAGMA_WAVE -> shootFireBall(stat)
            MegamanWeapon.MOON_SCYTHE -> shootMoonScythes(stat)
            MegamanWeapon.PRECIOUS_GUARD -> shootPreciousGuard()
        }

        handler.cooldown.reset()

        translateAmmo(weapon, -cost)

        GameLogger.debug(TAG, "fireWeapon(): weapon fired: weaponEntry=$handler")

        return true
    }

    private fun shootMegaBuster(stat: MegaChargeStatus) {
        GameLogger.debug(TAG, "fireMegaBuster(): stat=$stat")

        val trajectory = GameObjectPools.fetch(Vector2::class)
        when {
            megaman.direction.isVertical() -> trajectory.x = MegamanValues.BULLET_VEL * megaman.facing.value
            else -> trajectory.y = MegamanValues.BULLET_VEL * megaman.facing.value
        }
        trajectory.scl(ConstVals.PPM.toFloat())

        if (megaman.applyMovementScalarToBullet) trajectory.scl(megaman.movementScalar)

        val props = props(
            ConstKeys.OWNER pairTo megaman,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo megaman.direction,
            ConstKeys.POSITION pairTo getSpawnPosition(MegamanWeapon.MEGA_BUSTER)
        )

        when (stat) {
            MegaChargeStatus.NOT_CHARGED -> {
                megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, false)

                val bullet = MegaEntityFactory.fetch(Bullet::class)!!
                bullet.spawn(props)

                weaponHandlers[MegamanWeapon.MEGA_BUSTER].addSpawned(stat, bullet)
            }

            MegaChargeStatus.HALF_CHARGED, MegaChargeStatus.FULLY_CHARGED -> {
                megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, false)
                megaman.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)

                props.put(ConstKeys.BOOLEAN, stat == MegaChargeStatus.FULLY_CHARGED)

                val chargedShot = MegaEntityFactory.fetch(ChargedShot::class)!!
                chargedShot.spawn(props)

                weaponHandlers[MegamanWeapon.MEGA_BUSTER].addSpawned(stat, chargedShot)
            }
        }
    }

    private fun shootIceCube(stat: MegaChargeStatus) {
        GameLogger.debug(TAG, "shootIceCube(): stat=$stat")

        val spawn = getSpawnPosition(MegamanWeapon.ICE_CUBE)

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

        megaman.requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun shootFireBall(stat: MegaChargeStatus) {
        GameLogger.debug(TAG, "shootFireBall(): stat=$stat")

        if (game.getCurrentLevel() == LevelDefinition.MOON_MAN) {
            GameLogger.debug(TAG, "shootFireball(): in Moon Man's stage, fire cannot exist in outer space")

            val explosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.SCALAR pairTo 3f,
                    ConstKeys.OWNER pairTo megaman,
                    ConstKeys.POSITION pairTo getSpawnPosition(MegamanWeapon.MEGA_BUSTER),
                )
            )
            return
        }

        // TODO: spawned entity should change based on stat
        when (stat) {
            MegaChargeStatus.NOT_CHARGED,
            MegaChargeStatus.HALF_CHARGED,
            MegaChargeStatus.FULLY_CHARGED -> {
                val spawn = megaman.body.getPositionPoint(Position.BOTTOM_CENTER)
                    .add(ConstVals.PPM.toFloat() * megaman.facing.value, 0f)

                /*
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(MegamanValues.FIRE_BALL_X_VEL * megaman.facing.value, MegamanValues.FIRE_BALL_Y_VEL)
                    .scl(ConstVals.PPM.toFloat())
                 */
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(MegamanValues.MAGMA_WAVE_VEL * megaman.facing.value * ConstVals.PPM, 0f)

                /*
                val gravity = GameObjectPools.fetch(Vector2::class)
                    .set(0f, MegamanValues.FIRE_BALL_GRAVITY * ConstVals.PPM)
                 */

                /*
                val fireball = MegaEntityFactory.fetch(Fireball::class)!!
                fireball.spawn(props)
                 */
                val fireWave = MegaEntityFactory.fetch(MagmaWave::class)!!
                fireWave.spawn(
                    props(
                        ConstKeys.OWNER pairTo megaman,
                        ConstKeys.POSITION pairTo spawn,
                        ConstKeys.TRAJECTORY pairTo trajectory
                    )
                )

                weaponHandlers[MegamanWeapon.MAGMA_WAVE].addSpawned(stat, fireWave)

                megaman.requestToPlaySound(SoundAsset.CRASH_BOMBER_SOUND, false)
            }
        }
    }

    private fun shootMoonScythes(stat: MegaChargeStatus) {
        GameLogger.debug(TAG, "fireMoonScythes(): stat=$stat")

        // TODO: spawned entity should change based on stat
        when (stat) {
            MegaChargeStatus.NOT_CHARGED,
            MegaChargeStatus.HALF_CHARGED,
            MegaChargeStatus.FULLY_CHARGED -> MegamanValues.MOON_SCYTHE_DEG_OFFSETS.forEach { degreeOffset ->
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
                        ConstKeys.POSITION pairTo getSpawnPosition(MegamanWeapon.MOON_SCYTHE),
                        "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo megaman.movementScalar
                    )
                )

                weaponHandlers[MegamanWeapon.MOON_SCYTHE].addSpawned(stat, scythe)

                megaman.requestToPlaySound(SoundAsset.WHIP_SOUND, false)
            }
        }
    }

    private fun shootPreciousGuard() {
        val handler = weaponHandlers[MegamanWeapon.PRECIOUS_GUARD]

        val clusters = handler.getSpawned().get(MegaChargeStatus.NOT_CHARGED)
        if (clusters.size > 0) {
            val cluster = clusters.first() as PreciousGemCluster
            cluster.gems.values().forEach { it.released = true }
            return
        }

        val shieldGems = OrderedMap<PreciousGem, ShieldGemDef>()

        for (i in 0 until SHIELD_GEMS_ANGLES.size) {
            val angle = SHIELD_GEMS_ANGLES[i]

            val spawn = OrbitUtils.calculateOrbitalPosition(
                angle,
                SHIELD_GEM_START_OFFSET * ConstVals.PPM,
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

            shieldGems.put(gem, ShieldGemDef(angle, SHIELD_GEM_START_OFFSET * ConstVals.PPM, false))
        }

        val cluster = MegaEntityFactory.fetch(PreciousGemCluster::class)!!
        cluster.spawn(
            props(
                ConstKeys.OWNER pairTo megaman,
                ConstKeys.POSITION pairTo megaman.body.getCenter(),
                ConstKeys.ORIGIN pairTo megaman.body.getCenter(),
                PreciousGem.TAG pairTo shieldGems,
                "${ConstKeys.MAX}_${ConstKeys.DISTANCE}" pairTo
                    MegamanValues.SHIELD_GEM_MAX_DIST * ConstVals.PPM,
                "${ConstKeys.DISTANCE}_${ConstKeys.DELTA}" pairTo
                    MegamanValues.SHIELD_GEM_DISTANCE_DELTA * ConstVals.PPM
            )
        )

        handler.addSpawned(MegaChargeStatus.NOT_CHARGED, cluster)
    }
}
