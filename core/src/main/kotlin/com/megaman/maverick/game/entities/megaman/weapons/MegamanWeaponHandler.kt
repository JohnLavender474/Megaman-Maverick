package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.GROUND_SLIDE_SPRITE_OFFSET_Y
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MegaWeaponEntry(
    cooldownDur: Float,
    var chargeable: () -> Boolean = { true },
    var canFireWeapon: () -> Boolean = { true },
    var normalCost: () -> Int = { 0 },
    var halfChargedCost: () -> Int = { 0 },
    var fullyChargedCost: () -> Int = { 0 },
    var cooldownTimer: Timer = Timer(cooldownDur).setToEnd(),
    var spawned: Array<AbstractProjectile> = Array(),
    var ammo: Int = MegamanValues.MAX_WEAPON_AMMO
) : Updatable {

    override fun update(delta: Float) {
        cooldownTimer.update(delta)
        spawned.removeAll { it.dead }
    }
}

class MegamanWeaponHandler(private val megaman: Megaman /*, private val weaponSpawns: OrderedMap<String, IntPair> */) :
    Updatable, Resettable {

    companion object {
        const val TAG = "MegamanWeaponHandler"
        private const val MEGA_BUSTER_BULLET_VEL = 10f
    }

    private val engine = megaman.game.engine
    private val weapons = ObjectMap<MegamanWeapon, MegaWeaponEntry>()
    private val out = Vector2()

    override fun reset() = weapons.values().forEach { it.cooldownTimer.setToEnd() }

    override fun update(delta: Float) {
        weapons[megaman.currentWeapon]?.update(delta)
    }

    fun getSpawnPosition(out: Vector2): Vector2 {
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

        out.set(megaman.body.getCenter())

        val xOffset = megaman.facing.value * when {
            megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> 1f
            megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) -> 0.75f
            megaman.isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0.5f
            megaman.isBehaviorActive(BehaviorType.RIDING_CART) ->
                if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) 1.5f else 1.25f

            !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> 1f
            megaman.slipSliding -> 1f
            megaman.running -> 1.5f
            else -> 1.25f
        }

        var yOffset = when {
            megaman.isBehaviorActive(BehaviorType.AIR_DASHING) -> -0.4f
            megaman.isBehaviorActive(BehaviorType.WALL_SLIDING) ->
                if (megaman.direction == Direction.LEFT) 0.3f else 0.35f
            megaman.isBehaviorActive(BehaviorType.JETPACKING) -> 0.2f
            megaman.isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0.25f
            megaman.isBehaviorActive(BehaviorType.CLIMBING) -> 0.25f
            megaman.isBehaviorActive(BehaviorType.RIDING_CART) ->
                if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) 0.6f else 0.3f

            !megaman.body.isSensing(BodySense.FEET_ON_GROUND) -> when (megaman.direction) {
                Direction.UP -> 0.05f
                else -> 0.2f
            }

            else -> when (megaman.direction) {
                Direction.UP -> 0.1f
                else -> 0f
            }
        }

        if (megaman.direction.isVertical()) {
            out.x += xOffset * ConstVals.PPM
            out.y += (if (megaman.direction == Direction.DOWN) -yOffset else yOffset) * ConstVals.PPM

            if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
                var groundSlideOffset = GROUND_SLIDE_SPRITE_OFFSET_Y * ConstVals.PPM
                out.y += if (megaman.direction == Direction.UP) -groundSlideOffset else groundSlideOffset
            }
        } else {
            out.x += (if (megaman.direction == Direction.LEFT) -yOffset - 0.1f else yOffset + 0.1f) * ConstVals.PPM
            out.y += xOffset * ConstVals.PPM
        }

        return out
    }

    fun getSpawned(weapon: MegamanWeapon) = weapons[weapon]?.spawned

    fun putWeapon(weapon: MegamanWeapon): MegaWeaponEntry? = weapons.put(weapon, getWeaponEntry(weapon))

    fun hasWeapon(weapon: MegamanWeapon) = weapons.containsKey(weapon)

    fun canFireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
        if (!megaman.ready || !hasWeapon(weapon)) return false

        val e = weapons[weapon]
        if (!e.cooldownTimer.isFinished() || !e.canFireWeapon()) return false

        val cost = when {
            e.chargeable() -> (when {
                weapon === MegamanWeapon.BUSTER -> 0
                else -> when (stat) {
                    MegaChargeStatus.FULLY_CHARGED -> e.fullyChargedCost()
                    MegaChargeStatus.HALF_CHARGED -> e.halfChargedCost()
                    MegaChargeStatus.NOT_CHARGED -> e.normalCost()
                }
            })

            else -> e.normalCost()
        }

        return cost <= e.ammo
    }

    fun isChargeable(weapon: MegamanWeapon) = hasWeapon(weapon) && weapons[weapon].chargeable()

    fun translateAmmo(weapon: MegamanWeapon, delta: Int) {
        if (!hasWeapon(weapon)) return

        val weaponEntry = weapons[weapon]
        weaponEntry.ammo += delta

        when {
            weaponEntry.ammo >= MegamanValues.MAX_WEAPON_AMMO -> weaponEntry.ammo = MegamanValues.MAX_WEAPON_AMMO
            weaponEntry.ammo < 0 -> weaponEntry.ammo = 0
        }
    }

    fun setAllToMaxAmmo() = weapons.keys().forEach { setToMaxAmmo(it) }

    fun setToMaxAmmo(weapon: MegamanWeapon) {
        weapons[weapon]?.ammo = MegamanValues.MAX_WEAPON_AMMO
    }

    fun depleteAmmo(weapon: MegamanWeapon) {
        weapons[weapon]?.ammo = 0
    }

    fun getAmmo(weapon: MegamanWeapon) = if (!hasWeapon(weapon)) 0
    else if (weapon == MegamanWeapon.BUSTER) Int.MAX_VALUE else weapons[weapon].ammo

    fun isDepleted(weapon: MegamanWeapon) = getAmmo(weapon) == 0

    fun fireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
        var _stat: MegaChargeStatus = stat

        if (!canFireWeapon(weapon, _stat)) return false
        if (!isChargeable(weapon)) _stat = MegaChargeStatus.NOT_CHARGED

        val weaponEntry = weapons[weapon]

        val cost = when {
            weapon.equalsAny(MegamanWeapon.BUSTER, MegamanWeapon.RUSH_JETPACK) -> 0
            else -> when (_stat) {
                MegaChargeStatus.FULLY_CHARGED -> weaponEntry.fullyChargedCost()
                MegaChargeStatus.HALF_CHARGED -> weaponEntry.halfChargedCost()
                MegaChargeStatus.NOT_CHARGED -> weaponEntry.normalCost()
            }
        }

        if (cost > getAmmo(weapon)) return false

        val projectile = when (weapon) {
            MegamanWeapon.BUSTER, MegamanWeapon.RUSH_JETPACK -> fireMegaBuster(_stat)
            // TODO: MegamanWeapon.FLAME_TOSS -> fireFlameToss(_stat)
        }

        weaponEntry.spawned.add(projectile)
        weaponEntry.cooldownTimer.reset()

        translateAmmo(weapon, -cost)

        return true
    }

    private fun getWeaponEntry(weapon: MegamanWeapon) = when (weapon) {
        MegamanWeapon.BUSTER -> MegaWeaponEntry(.01f)
        MegamanWeapon.RUSH_JETPACK -> MegaWeaponEntry(cooldownDur = 0.1f, chargeable = { false })
        /* TODO:
        MegamanWeapon.FLAME_TOSS -> {
            val e = MegaWeaponEntry(.5f)
            e.normalCost = { 3 }
            e.halfChargedCost = { 5 }
            e.fullyChargedCost = { 7 }
            e.chargeable = { !megaman.body.isSensing(BodySense.IN_WATER) }
            e.canFireWeapon = { !megaman.body.isSensing(BodySense.IN_WATER) && e.spawned.size == 0 }
            e
        }
         */
    }

    private fun fireMegaBuster(stat: MegaChargeStatus): AbstractProjectile {
        val trajectory = GameObjectPools.fetch(Vector2::class)
        when {
            megaman.direction.isVertical() -> trajectory.x = MEGA_BUSTER_BULLET_VEL * megaman.facing.value
            else -> trajectory.y = MEGA_BUSTER_BULLET_VEL * megaman.facing.value
        }
        trajectory.scl(ConstVals.PPM.toFloat())

        if (megaman.applyMovementScalarToBullet) trajectory.scl(megaman.movementScalar)

        val props = props()
        props.put(ConstKeys.OWNER, megaman)
        props.put(ConstKeys.TRAJECTORY, trajectory)
        props.put(ConstKeys.DIRECTION, megaman.direction)

        val shot = when (stat) {
            MegaChargeStatus.NOT_CHARGED -> MegaEntityFactory.fetch(Bullet::class)

            MegaChargeStatus.HALF_CHARGED, MegaChargeStatus.FULLY_CHARGED -> {
                props.put(ConstKeys.BOOLEAN, stat == MegaChargeStatus.FULLY_CHARGED)
                MegaEntityFactory.fetch(ChargedShot::class)
            }
        } ?: throw IllegalStateException("MegaBusterShot is null")

        if (stat == MegaChargeStatus.NOT_CHARGED)
            megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, false)
        else {
            megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, false)
            megaman.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        }

        val s = getSpawnPosition(out)
        props.put(ConstKeys.POSITION, s)
        engine.spawn(shot, props)

        return shot
    }

    private fun fireFlameToss(stat: MegaChargeStatus): AbstractProjectile {
        val props = props()
        props.put(ConstKeys.OWNER, megaman)

        val spawn = getSpawnPosition(out)
        props.put(ConstKeys.POSITION, spawn)

        val fireball = when (stat) {
            MegaChargeStatus.NOT_CHARGED, MegaChargeStatus.HALF_CHARGED, MegaChargeStatus.FULLY_CHARGED -> {
                props.put(
                    ConstKeys.TRAJECTORY, Vector2(
                        MegamanValues.FLAME_TOSS_X_VEL * megaman.facing.value, MegamanValues.FLAME_TOSS_Y_VEL
                    ).scl(ConstVals.PPM.toFloat())
                )
                props.put(ConstKeys.GRAVITY, Vector2(0f, MegamanValues.FLAME_TOSS_GRAVITY))
                props.put(Fireball.BURST_ON_HIT_BODY, true)
                props.put(Fireball.BURST_ON_DAMAGE_INFLICTED, true)
                EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL) as Fireball
            }
        }
        engine.spawn(fireball, props)

        megaman.requestToPlaySound(SoundAsset.CRASH_BOMBER_SOUND, false)

        return fireball
    }
}
