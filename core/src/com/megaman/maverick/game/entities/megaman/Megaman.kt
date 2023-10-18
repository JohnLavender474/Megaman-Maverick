package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.engine.IGame2D
import com.engine.audio.AudioComponent
import com.engine.common.enums.Facing
import com.engine.common.interfaces.Faceable
import com.engine.common.objects.Properties
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.entities.GameEntity
import com.engine.entities.contracts.*
import com.engine.world.Body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.*

class Megaman(game: IGame2D) :
    GameEntity(game),
    Faceable,
    IBodyEntity,
    ISpriteEntity,
    IBehaviorsEntity,
    IPointsEntity,
    IDamageableEntity,
    IAudioEntity {

  // Megaman's timers
  internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME)
  internal val chargingTimer =
      Timer(
          MegamanValues.TIME_TO_FULLY_CHARGED,
          TimeMarkedRunnable(MegamanValues.TIME_TO_HALFWAY_CHARGED) {
            getComponent(AudioComponent::class)!!.requestToPlaySound(
                SoundAsset.MEGA_BUSTER_CHARGING_SOUND.source, true)
          })
  internal val damageFlashTimer = Timer(MegamanValues.DAMAGE_FLASH_DURATION)
  internal val airDashTimer = Timer(MegamanValues.MAX_AIR_DASH_TIME)
  internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()
  internal val groundSlideTimer = Timer(MegamanValues.MAX_GROUND_SLIDE_TIME)

  // the handler for Megaman's weapons
  val weaponHandler = MegamanWeaponHandler(this)

  // the charge status for the current weapon
  val chargeStatus: MegaChargeStatus
    get() =
        if (fullyCharged) MegaChargeStatus.FULLY_CHARGED
        else if (charging) MegaChargeStatus.HALF_CHARGED else MegaChargeStatus.NOT_CHARGED

  // if Megaman's current weapon is charging
  val charging: Boolean
    get() = !chargingTimer.isFinished()

  // if Megaman's current weapon is fully charged
  val fullyCharged: Boolean
    get() = charging && chargingTimer.time >= MegamanValues.TIME_TO_HALFWAY_CHARGED

  // if Megaman is shooting or in the shooting animation
  val shooting: Boolean
    get() = !shootAnimTimer.isFinished()

  // the amount of ammo for the current weapon
  val ammo: Int
    get() =
        if (currentWeapon === MegamanWeapon.BUSTER) Int.MAX_VALUE
        else weaponHandler.getAmmo(currentWeapon)

  // if Megaman should flash due to damage
  var damageFlash = false

  // if Megaman is upside down
  var upsideDown: Boolean
    get() = getProperty(MegamanProps.UPSIDE_DOWN) == true
    set(value) {
      putProperty(MegamanProps.UPSIDE_DOWN, value)
      if (upsideDown) {
        // jump
        jumpVel = -MegamanValues.JUMP_VEL
        wallJumpVel = -MegamanValues.WALL_JUMP_VEL
        waterJumpVel = -MegamanValues.WATER_JUMP_VEL
        waterWallJumpVel = -MegamanValues.WATER_WALL_JUMP_VEL

        // gravity
        gravity = -MegamanValues.GRAVITY
        groundGravity = -MegamanValues.GROUND_GRAVITY
        iceGravity = -MegamanValues.ICE_GRAVITY
        waterGravity = -MegamanValues.WATER_GRAVITY
        waterIceGravity = -MegamanValues.WATER_ICE_GRAVITY

        // swim
        swimVelY = -MegamanValues.SWIM_VEL_Y
      } else {
        // jump
        jumpVel = MegamanValues.JUMP_VEL
        wallJumpVel = MegamanValues.WALL_JUMP_VEL
        waterJumpVel = MegamanValues.WATER_JUMP_VEL
        waterWallJumpVel = MegamanValues.WATER_WALL_JUMP_VEL

        // gravity
        gravity = MegamanValues.GRAVITY
        groundGravity = MegamanValues.GROUND_GRAVITY
        iceGravity = MegamanValues.ICE_GRAVITY
        waterGravity = MegamanValues.WATER_GRAVITY
        waterIceGravity = MegamanValues.WATER_ICE_GRAVITY

        // swim
        swimVelY = MegamanValues.SWIM_VEL_Y
      }
    }

  // If Megaman is facing left or right
  override var facing: Facing
    get() = getProperty(MegamanProps.FACING) as Facing
    set(value) {
      putProperty(MegamanProps.FACING, value)
    }

  // Megaman's A button task
  var aButtonTask: AButtonTask
    get() = getProperty(MegamanProps.A_BUTTON_TASK) as AButtonTask
    set(value) {
      putProperty(MegamanProps.A_BUTTON_TASK, value)
    }

  // Megaman's current currentWeapon
  var currentWeapon: MegamanWeapon
    get() = getProperty(MegamanProps.WEAPON) as MegamanWeapon
    set(value) {
      putProperty(MegamanProps.WEAPON, value)
    }

  // If Megaman is running
  var running: Boolean
    get() = getProperty(ConstKeys.RUNNING) as Boolean
    set(value) {
      putProperty(ConstKeys.RUNNING, value)
    }

  internal var jumpVel = 0f
  internal var wallJumpVel = 0f
  internal var waterJumpVel = 0f
  internal var waterWallJumpVel = 0f
  internal var gravity = 0f
  internal var groundGravity = 0f
  internal var iceGravity = 0f
  internal var waterGravity = 0f
  internal var waterIceGravity = 0f
  internal var swimVelY = 0f

  /**
   * Initializes Megaman's components and properties. Called by the super [spawn] method if
   * [initialized] is false and this [init] has been called.
   */
  override fun init() {
    addComponent(defineUpdatablesComponent())
    addComponent(definePointsComponent())
    addComponent(defineDamageableComponent())
    addComponent(defineBodyComponent())
    addComponent(defineBehaviorsComponent())
    addComponent(defineControllerComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
  }

  /**
   * Spawns Megaman. The super method [spawn] calls [init] if [initialized] is false.
   *
   * @param spawnProps the [Properties] to use to spawn Megaman.
   */
  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    // set Megaman's position
    val position = properties.get(ConstKeys.POSITION, Vector2::class)!!
    body.setPosition(position)

    // initialize Megaman's props
    facing = Facing.RIGHT
    aButtonTask = AButtonTask.JUMP
    currentWeapon = MegamanWeapon.BUSTER
    upsideDown = false
    running = false
  }

  /**
   * Destroys Megaman by setting the [Body]'s velocity to zero and calling the super method which
   * sets [dead] to true, resets all the components, and calls anything contained in
   * [runnablesOnDestroy].
   */
  override fun onDestroy() {
    super<GameEntity>.onDestroy()
    body.physics.velocity.setZero()
  }
}
