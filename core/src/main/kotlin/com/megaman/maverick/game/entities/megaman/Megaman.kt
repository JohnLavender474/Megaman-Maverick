package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.entities.resetBehavior
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.*
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.handlers.MegamanJetpackHandler
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.entities.utils.isOutsideLevel
import com.megaman.maverick.game.entities.utils.standardOnTeleportEnd
import com.megaman.maverick.game.entities.utils.standardOnTeleportStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.locks.behaviorsLockHook
import com.megaman.maverick.game.players.GravityDeviceComponent
import com.megaman.maverick.game.players.PlayerLight
import com.megaman.maverick.game.players.PlayerLightComponent
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.startSmoothFollow
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

class Megaman(services: MegaServices) : AbstractHealthEntity(services), IBodyEntity, ISpritesEntity,
    IAudioEntity, IScalableGravityEntity, IEventListener, IFaceable, IDirectional, IDamagerEntity {

    companion object {
        const val TAG = "Megaman"
        const val MEGAMAN_EVENT_LISTENER_TAG = "MegamanEventListener"

        private const val DEATH_X_OFFSET = 1.5f
        private const val DEATH_Y_OFFSET = 1.5f
    }

    override val damageNegotiator = MegamanDamageNegotiator(this)

    internal val damageRecovery = defineDamageRecoveryComponent()

    // TODO: a second player entity needs this same rule; extract one function when it exists (item 081)
    override val invincible: Boolean
        get() = damaged || damageRecovery.recovering || isLocked(MegaLockGroup.DAMAGE) || !existenceState.liveAfterUpdate || !inPlay
    override val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()

    // TODO: a second player entity needs this same rule; extract one function when it exists (item 081)
    val canMove: Boolean
        get() = !isLocked(MegaLockGroup.MOVEMENT) && !stunned && !damaged && !frozen


    internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME).setToEnd()

    internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()

    internal var airDashConsumed = false
    internal var airDashDirection: Direction? = null

    var currentAnimKey: String? = null
        internal set

    override val eventKeyMask = objectSetOf<Any>(
        EventType.BEGIN_ROOM_TRANS,
        EventType.CONTINUE_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER,
        EventType.END_GAME_CAM_ROTATION
    )

    internal lateinit var weaponsHandler: MegamanWeaponsHandler
    internal lateinit var jetpackHandler: MegamanJetpackHandler

    var shooting: Boolean
        get() = !shootAnimTimer.isFinished()
        set(value) {
            if (value) shootAnimTimer.reset() else shootAnimTimer.setToEnd()
        }
    val ammo: Int
        get() = when (currentWeapon) {
            MegamanWeapon.MEGA_BUSTER -> Int.MAX_VALUE
            else -> weaponsHandler.getAmmo(currentWeapon)
        }

    override var direction: Direction
        get() = body.direction
        set(value) {
            GameLogger.debug(TAG, "direction-set(): value=$value")

            if (value != body.direction) {
                GameLogger.debug(TAG, "direction-set(): value not same as field")

                // TODO: the camera rotation belongs to WorldGravity, not to each player; with more players, each
                //  player's setter would ask for the rotation (see the 029d scope, "Gravity direction")

                body.direction = value

                services.cameraController.startSmoothFollow()

                val camDir = if (value.isVertical()) value else value.getOpposite()
                if (services.gameCamera.direction != camDir) {
                    services.events.submitEvent(
                        Event(
                            EventType.START_GAME_CAM_ROTATION,
                            props(ConstKeys.DIRECTION pairTo camDir)
                        )
                    )

                    lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)

                    // the jetpacking behavior must be reset before the physics values are set below since
                    // ending that behavior turns gravity back on
                    resetBehavior(BehaviorType.JETPACKING)

                    body.physics.gravityOn = false
                    body.physics.velocity.setZero()
                }
            } else GameLogger.debug(TAG, "direction-set(): value same as field")

            when (value) {
                Direction.UP, Direction.RIGHT -> {
                    jumpVel = MegamanValues.JUMP_VEL
                    wallJumpVel = MegamanValues.WALL_JUMP_VEL

                    jumpGravity = MegamanValues.JUMP_GRAVITY
                    fallGravity = MegamanValues.FALL_GRAVITY
                    groundGravity = MegamanValues.GROUND_GRAVITY
                    iceGravity = MegamanValues.ICE_GRAVITY
                    waterGravity = MegamanValues.WATER_GRAVITY
                    waterIceGravity = MegamanValues.WATER_ICE_GRAVITY

                    swimVel = MegamanValues.SWIM_VEL_Y
                }

                Direction.DOWN, Direction.LEFT -> {
                    jumpVel = -MegamanValues.JUMP_VEL
                    wallJumpVel = -MegamanValues.WALL_JUMP_VEL

                    jumpGravity = -MegamanValues.JUMP_GRAVITY
                    fallGravity = -MegamanValues.FALL_GRAVITY
                    groundGravity = -MegamanValues.GROUND_GRAVITY
                    iceGravity = -MegamanValues.ICE_GRAVITY
                    waterGravity = -MegamanValues.WATER_GRAVITY
                    waterIceGravity = -MegamanValues.WATER_ICE_GRAVITY

                    swimVel = -MegamanValues.SWIM_VEL_Y
                }
            }
        }

    override lateinit var facing: Facing

    lateinit var aButtonTask: AButtonTask

    private var weapon: MegamanWeapon? = null
    var currentWeapon: MegamanWeapon
        get() = weapon!!
        set(value) {
            weaponsHandler.onChangeWeapon(value, weapon)
            weapon = value
        }

    val slipSliding: Boolean
        get() = body.isSensing(BodySense.FEET_ON_GROUND) &&
            abs(
                when {
                    direction.isVertical() -> body.physics.velocity.x
                    else -> body.physics.velocity.y
                }
            ) >= MegamanValues.SLIP_SLIDE_THRESHOLD * ConstVals.PPM

    var movementScalar = 1f
        set(value) {
            field = value
            require(AnimationsComponent::class).forEachAnimator { _, _, animator ->
                animator as Animator
                animator.updateScalar = value
            }
        }

    override var gravityScalar = 1f

    internal var jumpVel = 0f
    internal var wallJumpVel = 0f
    internal var fallGravity = 0f
    internal var jumpGravity = 0f
    internal var groundGravity = 0f
    internal var iceGravity = 0f
    internal var waterGravity = 0f
    internal var waterIceGravity = 0f
    internal var swimVel = 0f

    internal var canMakeLandSound = false
    internal var applyMovementScalarToBullet = false

    internal val wallSlideNotAllowedTimer = Timer(MegamanValues.WALLSLIDE_NOT_ALLOWED_DELAY)


    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()

        addComponent(PlayPhaseComponent())
        addComponent(
            TeleportableComponent(
                onStart = { _: ITeleporterEntity ->
                    stopCharging()
                    standardOnTeleportStart(this)
                    if (isBehaviorActive(BehaviorType.AIR_DASHING)) resetBehavior(BehaviorType.AIR_DASHING)
                    lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
                },
                onContinue = { standardOnTeleportStart(this) },
                onEnd = {
                    stopCharging()
                    standardOnTeleportEnd(this)
                    aButtonTask = AButtonTask.AIR_DASH
                    unlock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
                    services.cameraController.stopSmoothFollow()
                }
            )
        )
        addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(this)))
        addComponent(defineBodyComponent())
        addComponent(defineFocusComponent())
        addComponent(definePlayerProgressComponent())
        addComponent(RunComponent())
        addComponent(defineWeaponChargeComponent())
        addComponent(defineMomentumComponent())
        addComponent(defineRoomTransitionCarryComponent())
        addComponent(defineBubbleEmitterComponent())
        addComponent(defineTrailSpriteComponent())
        addComponent(defineKnockbackComponent())
        addComponent(defineFreezeComponent())
        addComponent(defineStunComponent())
        addComponent(damageRecovery)
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())

        val animations = MegamanAnimations(services).get()
        addComponent(defineAnimationsComponent(animations))

        weaponsHandler = MegamanWeaponsHandler(this)

        jetpackHandler = MegamanJetpackHandler(this)

        currentWeapon = MegamanWeapon.MEGA_BUSTER

        aButtonTask = AButtonTask.JUMP

        addComponent(GravityDeviceComponent {
            aButtonTask = when {
                body.isSensing(BodySense.FEET_ON_GROUND) -> AButtonTask.JUMP
                else -> AButtonTask.AIR_DASH
            }
        })

        addComponent(PlayerLightComponent {
            when {
                charging -> if (fullyCharged) PlayerLight.FULL_CHARGE else PlayerLight.HALF_CHARGE
                isBehaviorActive(BehaviorType.JETPACKING) -> PlayerLight.JETPACK
                else -> PlayerLight.STANDARD
            }
        })
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        services.events.addListener(this)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

        body.physics.velocity.setZero()
        body.physics.collisionOn = true
        body.physics.gravityOn = true
        body.forEachFixture { fixture -> fixture.setActive(true) }

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
        services.gravity.set(
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())
        )

        aButtonTask = AButtonTask.JUMP

        setHealth(getMaxHealth())

        weaponsHandler.setAllToMaxAmmo()
        currentWeapon = MegamanWeapon.MEGA_BUSTER

        clearLocks(MegaLockGroup.MOVEMENT)
        require(TeleportableComponent::class).teleporter = null
        clearLocks(MegaLockGroup.DAMAGE)
        canMakeLandSound = false

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)

        damageTimer.setToEnd()

        shootAnimTimer.reset()
        wallJumpTimer.reset()

        wallSlideNotAllowedTimer.reset()
        // TODO: a behavior that ends in the component reset pass can write momentum after the momentum component reset, so clear it
        //  here too; a reset order for components would remove this line (see the 084c docs/NOTES.md)
        require(PostActionMomentumComponent::class).reset()
        airDashConsumed = false

        services.cameraController.stopSmoothFollow()

        // TODO: only slot 0 submits this event, because its listeners use it as a level reset, which must run once; a
        //  later slot has no spawn event until item 038 splits it into a spawn event per player and a level reset
        if (playerSlot!!.index == 0)
            services.events.submitEvent(Event(EventType.PLAYER_SPAWN, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        val eventsMan = services.events
        eventsMan.removeListener(this)
        eventsMan.submitEvent(Event(EventType.PLAYER_JUST_DIED, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))

        services.audio.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        ExplosionOrb.spawnRing(body.getCenter())

        if (services.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL)
            services.audio.playSound(SoundAsset.SMB3_PLAYER_DOWN_SOUND, false)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                val position = event.getProperty(ConstKeys.POSITION, Vector2::class)!!
                if (event.key == EventType.BEGIN_ROOM_TRANS) GameLogger.debug(
                    MEGAMAN_EVENT_LISTENER_TAG, "BEGIN ROOM TRANS: position=$position"
                )
                body.setCenter(position)

                if (event.key == EventType.BEGIN_ROOM_TRANS) beginRoomTransitionCarry()

                body.physics.gravityOn = false

                stopMomentum(true)

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.END_ROOM_TRANS -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "endRoomTrans()")

                endRoomTransitionCarry()

                body.physics.gravityOn = !isBehaviorActive(BehaviorType.CLIMBING)

                stopMomentum(true)
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "GATE_INIT_OPENING")

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.STUN_PLAYER -> onStunPlayerEvent(event)

            EventType.END_GAME_CAM_ROTATION -> {
                unlock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)

                body.physics.gravityOn = true
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamagerEntity): Boolean {
        if (!existenceState.liveAfterUpdate || !super.canBeDamagedBy(damager)) return false
        if (damager is IOwnable<*>) {
            if (damager.owner == this) return false
            if (damager.owner is IOwnable<*> && (damager.owner as IOwnable<*>).owner == this) return false
        }
        return true
    }

    override fun getDamageDuration(damager: IDamagerEntity) = MegamanValues.DAMAGE_DURATION

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val oldHealth = getCurrentHealth()

        if (!super.takeDamageFrom(damager)) return false

        val healthDiff = oldHealth - getCurrentHealth()
        GameLogger.debug(TAG, "takeDamageFrom(): healthDiff=$healthDiff, damager=$damager")

        knockBackFrom(damager)

        freezeFrom(damager)

        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)

        players.onPlayerDamaged(this.asPlayer!!, damager)

        return true
    }

    override fun editDamageFrom(damager: IDamagerEntity, baseDamage: Int): Int {
        var damage = when {
            services.gameState.containsEnhancement(MegaEnhancement.DAMAGE_INCREASE) -> MegaEnhancement.scaleDamage(
                baseDamage, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR
            )

            else -> baseDamage
        }

        if (services.gameState.getDifficultyMode() == DifficultyMode.HARD) damage += 1

        return damage
    }

    override fun definePointsComponent(): PointsComponent {
        val component = super.definePointsComponent()

        val points = component.getPoints(ConstKeys.HEALTH)
        points.max = MegamanValues.START_HEALTH

        return component
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (isOutsideLevel(services.levelState, DEATH_X_OFFSET, DEATH_Y_OFFSET)) {
                GameLogger.error(TAG, "Megaman is below game bounds, kill him")
                destroy()
            }

            if (!weaponsHandler.isChargeable(currentWeapon)) stopCharging()
            weaponsHandler.update(delta)

            jetpackHandler.update(delta)

            require(StunComponent::class).update(delta, body.isSensing(BodySense.FEET_ON_GROUND))
            clearChargeWhenHurt()

            damageRecovery.update(delta, damaged, damageTimer.isJustFinished())

            wallJumpTimer.update(delta)
            shootAnimTimer.update(delta)
            require(RoomTransitionCarryComponent::class).update(delta)

            updatePostActionMomentum(delta, running)

            require(BubbleEmitterComponent::class).update(delta)

            require(TrailSpriteComponent::class).update(delta)

            require(FreezeComponent::class).update(delta)

            wallSlideNotAllowedTimer.update(delta)

            updateAButtonTask()
        }
    }

    fun translateAmmo(ammo: Int, weapon: MegamanWeapon = currentWeapon): Int {
        val remaining = weaponsHandler.translateAmmo(weapon, ammo)
        GameLogger.debug(TAG, "translateAmmo(): weapon=$weapon, ammo=$ammo, remaining=$remaining")
        return remaining
    }

    fun shoot(): Boolean {
        val shot = weaponsHandler.fireWeapon(currentWeapon, chargeStatus)
        if (shot) shootAnimTimer.reset()
        return shot
    }

    override fun getType() = EntityType.MEGAMAN

    override fun getTag() = TAG
}
