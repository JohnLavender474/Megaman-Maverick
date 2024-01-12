package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
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

/**
 * The `MegaWeaponEntry` class represents a weapon entry for a weapon. It contains the weapon's
 * cooldown timer, the projectiles it has spawned, and the amount of ammunition it has left.
 *
 * @param cooldownDur The duration of the cooldown timer for this weapon.
 * @param chargeable A function that returns whether this weapon is chargeable.
 * @param canFireWeapon A function that returns whether this weapon can be fired.
 * @param normalCost A function that returns the normal cost of this weapon.
 * @param halfChargedCost A function that returns the half-charged cost of this weapon.
 * @param fullyChargedCost A function that returns the fully-charged cost of this weapon.
 * @param cooldownTimer The cooldown timer for this weapon.
 * @param spawned The projectiles that this weapon has spawned.
 * @param ammo The amount of ammunition this weapon has left.
 */
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

/**
 * The `MegamanWeaponHandler` class is responsible for managing Megaman's weapons and handling their
 * usage, including ammunition, cooldown, and firing projectiles. It keeps track of the various
 * weapons available to Megaman and facilitates the process of firing them. This class ensures that
 * the ammunition, cooldown, and other restrictions for each weapon are enforced.
 *
 * @param megaman The Megaman character associated with this weapon handler.
 */
class MegamanWeaponHandler(private val megaman: Megaman) : Updatable, Resettable {

  companion object {
    private const val MEGA_BUSTER_BULLET_VEL = 10f
    private val FLAME_TOSS_TRAJECTORY = Vector2(35f, 10f)
  }

  private val gameEngine = megaman.game.gameEngine
  private val weapons = ObjectMap<MegamanWeapon, MegaWeaponEntry>()
  private val spawnCenter: Vector2
    get() {
      val spawnCenter = Vector2(megaman.body.getCenter())

      if (megaman.isDirectionRotatedVertically()) {
        val xOffset: Float = ConstVals.PPM * .85f * megaman.facing.value
        spawnCenter.x += xOffset

        var yOffset: Float = ConstVals.PPM / 16f
        if (megaman.isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.WALL_SLIDING))
            yOffset += .15f * ConstVals.PPM
        else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) yOffset -= .05f * ConstVals.PPM
        else yOffset += .25f * ConstVals.PPM

        spawnCenter.y += if (megaman.isDirectionRotatedDown()) -yOffset else yOffset
      } else {
        var xOffset = ConstVals.PPM / 16f
        xOffset +=
            if (megaman.isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.WALL_SLIDING))
                .15f * ConstVals.PPM
            else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) -.05f * ConstVals.PPM
            else .05f * ConstVals.PPM
        // else .25f * ConstVals.PPM
        spawnCenter.x += if (megaman.isDirectionRotatedLeft()) -xOffset else xOffset

        val yOffset: Float = ConstVals.PPM * .85f * megaman.facing.value
        spawnCenter.y += yOffset
      }

      return spawnCenter
    }

  /** Resets the weapon handler by resetting the cooldown timers for all weapons. */
  override fun reset() = weapons.values().forEach { it.cooldownTimer.setToEnd() }

  /**
   * Updates the weapon handler by updating the cooldown timers for all weapons.
   *
   * @param delta The time in seconds since the last update.
   */
  override fun update(delta: Float) {
    weapons[megaman.currentWeapon]?.update(delta)
  }

  /**
   * Gets the projectiles spawned by the specified weapon.
   *
   * @param weapon The weapon to get the projectiles for.
   * @return The projectiles spawned by the specified weapon.
   */
  fun getSpawned(weapon: MegamanWeapon) = weapons[weapon]?.spawned

  /**
   * Puts the specified weapon into the weapon handler.
   *
   * @param weapon The weapon to put into the weapon handler.
   * @return The weapon entry for the specified weapon.
   */
  fun putWeapon(weapon: MegamanWeapon): MegaWeaponEntry? =
      weapons.put(weapon, getWeaponEntry(weapon))

  /**
   * Returns if the weapon handler contains the specified weapon.
   *
   * @param weapon The weapon to check for.
   * @return if the weapon handler contains the specified weapon.
   */
  fun hasWeapon(weapon: MegamanWeapon) = weapons.containsKey(weapon)

  /**
   * Returns if the specified weapon can be fired.
   *
   * @param weapon The weapon to check.
   * @param stat The charge status of the weapon.
   * @return if the specified weapon can be fired.
   */
  fun canFireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
    if (!hasWeapon(weapon)) return false

    val e = weapons[weapon]
    if (!e.cooldownTimer.isFinished() || !e.canFireWeapon()) return false

    val cost =
        if (e.chargeable())
            (if (weapon === MegamanWeapon.BUSTER) 0
            else
                when (stat) {
                  MegaChargeStatus.FULLY_CHARGED -> e.fullyChargedCost()
                  MegaChargeStatus.HALF_CHARGED -> e.halfChargedCost()
                  MegaChargeStatus.NOT_CHARGED -> e.normalCost()
                })
        else e.normalCost()

    return cost <= e.ammo
  }

  /**
   * Returns if the specified weapon can be charged.
   *
   * @param weapon The weapon to check.
   * @return if the specified weapon can be charged.
   */
  fun isChargeable(weapon: MegamanWeapon) = hasWeapon(weapon) && weapons[weapon].chargeable()

  /**
   * Translates the ammunition for the specified weapon by the specified amount.
   *
   * @param weapon The weapon to translate the ammunition for.
   * @param delta The amount to translate the ammunition by.
   */
  fun translateAmmo(weapon: MegamanWeapon, delta: Int) {
    if (!hasWeapon(weapon)) return

    val weaponEntry = weapons[weapon]
    weaponEntry.ammo += delta

    if (weaponEntry.ammo >= MegamanValues.MAX_WEAPON_AMMO)
        weaponEntry.ammo = MegamanValues.MAX_WEAPON_AMMO
    else if (weaponEntry.ammo < 0) weaponEntry.ammo = 0
  }

  /**
   * Sets the ammunition for the specified weapon to the max amount.
   *
   * @param weapon The weapon to set the ammunition for.
   */
  fun setToMaxAmmo(weapon: MegamanWeapon) {
    weapons[weapon]?.ammo = MegamanValues.MAX_WEAPON_AMMO
  }

  /**
   * Depletes the ammunition for the specified weapon.
   *
   * @param weapon The weapon to deplete the ammunition for.
   */
  fun depleteAmmo(weapon: MegamanWeapon) {
    weapons[weapon]?.ammo = 0
  }

  /**
   * Returns the amount of ammunition for the specified weapon.
   *
   * @param weapon The weapon to get the ammunition for.
   * @return The amount of ammunition for the specified weapon.
   */
  fun getAmmo(weapon: MegamanWeapon) =
      if (!hasWeapon(weapon)) 0
      else if (weapon == MegamanWeapon.BUSTER) Int.MAX_VALUE else weapons[weapon].ammo

  /**
   * Fires the specified weapon with the specified charge status.
   *
   * @param weapon The weapon to fire.
   * @param stat The charge status of the weapon.
   * @return if the weapon was fired.
   */
  fun fireWeapon(weapon: MegamanWeapon, stat: MegaChargeStatus): Boolean {
    var _stat: MegaChargeStatus = stat
    if (!canFireWeapon(weapon, _stat)) return false
    if (!isChargeable(weapon)) _stat = MegaChargeStatus.NOT_CHARGED

    val weaponEntry = weapons[weapon]
    val cost =
        if (weapon === MegamanWeapon.BUSTER) 0
        else
            when (_stat) {
              MegaChargeStatus.FULLY_CHARGED -> weaponEntry.fullyChargedCost()
              MegaChargeStatus.HALF_CHARGED -> weaponEntry.halfChargedCost()
              MegaChargeStatus.NOT_CHARGED -> weaponEntry.normalCost()
            }
    if (cost > getAmmo(weapon)) return false

    val projectile =
        when (weapon) {
          MegamanWeapon.BUSTER -> fireMegaBuster(_stat)
          MegamanWeapon.FLAME_TOSS -> fireFlameToss(_stat)
        }

    weaponEntry.spawned.add(projectile)
    weaponEntry.cooldownTimer.reset()
    translateAmmo(weapon, -cost)

    return true
  }

  private fun getWeaponEntry(weapon: MegamanWeapon) =
      when (weapon) {
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
    if (megaman.isDirectionRotatedVertically())
        trajectory.x = MEGA_BUSTER_BULLET_VEL * megaman.facing.value
    else trajectory.y = MEGA_BUSTER_BULLET_VEL * megaman.facing.value

    val props = Properties()
    props.put(ConstKeys.OWNER, megaman)
    props.put(ConstKeys.TRAJECTORY, trajectory)
    props.put(
        ConstKeys.DIRECTION,
        if (megaman.isDirectionRotatedVertically()) Direction.UP else Direction.LEFT)

    val megaBusterShot =
        when (stat) {
          MegaChargeStatus.NOT_CHARGED ->
              EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)
          MegaChargeStatus.HALF_CHARGED,
          MegaChargeStatus.FULLY_CHARGED -> {
            props.put(ConstKeys.BOOLEAN, stat == MegaChargeStatus.FULLY_CHARGED)
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CHARGED_SHOT)
          }
        } ?: throw IllegalStateException("MegaBusterShot is null")

    if (stat === MegaChargeStatus.NOT_CHARGED)
        megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND, false)
    else {
      megaman.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND, false)
      megaman.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    val s = spawnCenter

    if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING)) s.y += 0.1f * ConstVals.PPM
    if (megaman.isDirectionRotatedDown())
        if (megaman.isBehaviorActive(BehaviorType.CLIMBING)) s.y -= .45f * ConstVals.PPM
        else s.y -= .05f * ConstVals.PPM

    props.put(ConstKeys.POSITION, s)
    gameEngine.spawn(megaBusterShot, props)

    return megaBusterShot as IProjectileEntity
  }

  private fun fireFlameToss(stat: MegaChargeStatus): IProjectileEntity {
    val props = Properties()
    props.put(ConstKeys.OWNER, megaman)

    val fireball =
        when (stat) {
          MegaChargeStatus.NOT_CHARGED,
          MegaChargeStatus.HALF_CHARGED,
          MegaChargeStatus.FULLY_CHARGED -> {
            props.put(ConstKeys.LEFT, megaman.facing == Facing.LEFT)
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL) as Fireball
          }
        }

    props.put(ConstKeys.POSITION, spawnCenter)
    // TODO: trajectory should be different depending on charge status
    props.put(ConstKeys.TRAJECTORY, FLAME_TOSS_TRAJECTORY)
    gameEngine.spawn(fireball, props)

    megaman.requestToPlaySound(SoundAsset.CRASH_BOMBER_SOUND, false)

    return fireball
  }
}
