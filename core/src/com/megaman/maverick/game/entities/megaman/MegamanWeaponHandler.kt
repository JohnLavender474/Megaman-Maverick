package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.interfaces.Resettable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.time.Timer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing

class MegaWeaponEntry(
    cooldownDur: Float,
    var chargeable: () -> Boolean = { true },
    var canFireWeapon: () -> Boolean = { true },
    var normalCost: () -> Int = { 0 },
    var halfChargedCost: () -> Int = { 0 },
    var fullyChargedCost: () -> Int = { 0 },
    var cooldownTimer: Timer = Timer(cooldownDur).setToEnd(),
    var spawned: Array<IProjectileEntity> = Array(),
    var ammo: Int = MegamanValues.MAX_WEAPON_AMMO
) : Updatable {

    override fun update(delta: Float) {
        cooldownTimer.update(delta)
        spawned.removeAll { it.dead }
    }
}

class MegamanWeaponHandler(private val megaman: Megaman) : Updatable, Resettable {

    companion object {
        private const val MEGA_BUSTER_BULLET_VEL = 10f
    }

    private val engine = megaman.game.engine
    private val weapons = ObjectMap<MegamanWeapon, MegaWeaponEntry>()
    private val spawnCenter: Vector2
        get() {
            val spawnCenter = Vector2(megaman.body.getCenter())

            if (megaman.isDirectionRotatedVertically()) {
                spawnCenter.x += ConstVals.PPM * megaman.facing.value * if (megaman.isBehaviorActive(BehaviorType.RIDING_CART)) 1.5f else 0.85f

                var yOffset: Float = ConstVals.PPM / 16f
                if (megaman.isBehaviorActive(BehaviorType.RIDING_CART)) yOffset += .35f * ConstVals.PPM
                else if (megaman.isAnyBehaviorActive(
                        BehaviorType.CLIMBING, BehaviorType.WALL_SLIDING
                    )
                ) yOffset += .15f * ConstVals.PPM
                else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) yOffset -= .05f * ConstVals.PPM
                else yOffset += .25f * ConstVals.PPM

                spawnCenter.y += if (megaman.isDirectionRotatedDown()) -yOffset else yOffset
            } else {
                var xOffset = ConstVals.PPM / 16f
                xOffset += if (megaman.isBehaviorActive(BehaviorType.RIDING_CART)) .25f * ConstVals.PPM
                else if (megaman.isAnyBehaviorActive(
                        BehaviorType.CLIMBING, BehaviorType.WALL_SLIDING
                    )
                ) .15f * ConstVals.PPM
                else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) -.05f * ConstVals.PPM
                else .05f * ConstVals.PPM
                spawnCenter.x += if (megaman.isDirectionRotatedLeft()) -xOffset else xOffset

                val yOffset: Float = ConstVals.PPM * .85f * megaman.facing.value
                spawnCenter.y += yOffset
            }

            return spawnCenter
        }

    override fun reset() = weapons.values().forEach { it.cooldownTimer.setToEnd() }

    override fun update(delta: Float) {
        weapons[megaman.currentWeapon]?.update(delta)
    }

    fun getSpawned(weapon: MegamanWeapon) = weapons[weapon]?.spawned

    fun putWeapon(weapon: MegamanWeapon): MegaWeaponEntry? = weapons.put(weapon, getWeaponEntry(weapon))

    fun hasWeapon(weapon: MegamanWeapon) = weapons.containsKey(weapon)

    fun canFireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
        if (!megaman.ready || !hasWeapon(weapon)) return false

        val e = weapons[weapon]
        if (!e.cooldownTimer.isFinished() || !e.canFireWeapon()) return false

        val cost = if (e.chargeable()) (if (weapon === MegamanWeapon.BUSTER) 0
        else when (stat) {
            MegaChargeStatus.FULLY_CHARGED -> e.fullyChargedCost()
            MegaChargeStatus.HALF_CHARGED -> e.halfChargedCost()
            MegaChargeStatus.NOT_CHARGED -> e.normalCost()
        })
        else e.normalCost()

        return cost <= e.ammo
    }

    fun isChargeable(weapon: MegamanWeapon) = hasWeapon(weapon) && weapons[weapon].chargeable()

    fun translateAmmo(weapon: MegamanWeapon, delta: Int) {
        if (!hasWeapon(weapon)) return

        val weaponEntry = weapons[weapon]
        weaponEntry.ammo += delta

        if (weaponEntry.ammo >= MegamanValues.MAX_WEAPON_AMMO) weaponEntry.ammo = MegamanValues.MAX_WEAPON_AMMO
        else if (weaponEntry.ammo < 0) weaponEntry.ammo = 0
    }

    fun setToMaxAmmo(weapon: MegamanWeapon) {
        weapons[weapon]?.ammo = MegamanValues.MAX_WEAPON_AMMO
    }

    fun depleteAmmo(weapon: MegamanWeapon) {
        weapons[weapon]?.ammo = 0
    }

    fun getAmmo(weapon: MegamanWeapon) = if (!hasWeapon(weapon)) 0
    else if (weapon == MegamanWeapon.BUSTER) Int.MAX_VALUE else weapons[weapon].ammo

    fun fireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
        var _stat: MegaChargeStatus = stat
        if (!canFireWeapon(weapon, _stat)) return false
        if (!isChargeable(weapon)) _stat = MegaChargeStatus.NOT_CHARGED

        val weaponEntry = weapons[weapon]
        val cost = if (weapon === MegamanWeapon.BUSTER) 0
        else when (_stat) {
            MegaChargeStatus.FULLY_CHARGED -> weaponEntry.fullyChargedCost()
            MegaChargeStatus.HALF_CHARGED -> weaponEntry.halfChargedCost()
            MegaChargeStatus.NOT_CHARGED -> weaponEntry.normalCost()
        }
        if (cost > getAmmo(weapon)) return false

        val projectile = when (weapon) {
            MegamanWeapon.BUSTER -> fireMegaBuster(_stat)
            MegamanWeapon.FLAME_TOSS -> fireFlameToss(_stat)
        }

        weaponEntry.spawned.add(projectile)
        weaponEntry.cooldownTimer.reset()
        translateAmmo(weapon, -cost)

        return true
    }

    private fun getWeaponEntry(weapon: MegamanWeapon) = when (weapon) {
        MegamanWeapon.BUSTER -> MegaWeaponEntry(.01f)
        MegamanWeapon.FLAME_TOSS -> {
            val e = MegaWeaponEntry(.5f)
            e.normalCost = { 3 }
            e.halfChargedCost = { 5 }
            e.fullyChargedCost = { 7 }
            e.chargeable = { !megaman.body.isSensing(BodySense.IN_WATER) }
            e.canFireWeapon = { !megaman.body.isSensing(BodySense.IN_WATER) && e.spawned.size == 0 }
            e
        }
    }

    private fun fireMegaBuster(stat: MegaChargeStatus): IProjectileEntity {
        val trajectory = Vector2()
        if (megaman.isDirectionRotatedVertically()) trajectory.x = MEGA_BUSTER_BULLET_VEL * megaman.facing.value
        else trajectory.y = MEGA_BUSTER_BULLET_VEL * megaman.facing.value
        trajectory.scl(ConstVals.PPM.toFloat())

        val props = Properties()
        props.put(ConstKeys.OWNER, megaman)
        props.put(ConstKeys.TRAJECTORY, trajectory)
        props.put(ConstKeys.DIRECTION, megaman.directionRotation)

        val megaBusterShot = when (stat) {
            MegaChargeStatus.NOT_CHARGED -> EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)

            MegaChargeStatus.HALF_CHARGED, MegaChargeStatus.FULLY_CHARGED -> {
                props.put(ConstKeys.BOOLEAN, stat == MegaChargeStatus.FULLY_CHARGED)
                EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CHARGED_SHOT)
            }
        } ?: throw IllegalStateException("MegaBusterShot is null")

        if (stat === MegaChargeStatus.NOT_CHARGED) megaman.requestToPlaySound(
            SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, false
        )
        else {
            megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, false)
            megaman.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        }

        val s = spawnCenter
        if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING)) s.y += 0.1f * ConstVals.PPM
        else if (megaman.isDirectionRotatedDown()) s.y -= .05f * ConstVals.PPM
        else if (!megaman.body.isSensing(BodySense.FEET_ON_GROUND) &&
            !megaman.isAnyBehaviorActive(BehaviorType.WALL_SLIDING, BehaviorType.CLIMBING)
        ) s.y -= .15f * ConstVals.PPM

        props.put(ConstKeys.POSITION, s)
        engine.spawn(megaBusterShot, props)

        return megaBusterShot as IProjectileEntity
    }

    private fun fireFlameToss(stat: MegaChargeStatus): IProjectileEntity {
        val props = Properties()
        props.put(ConstKeys.OWNER, megaman)
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
        props.put(ConstKeys.POSITION, spawnCenter)
        engine.spawn(fireball, props)
        megaman.requestToPlaySound(SoundAsset.CRASH_BOMBER_SOUND, false)
        return fireball
    }
}
