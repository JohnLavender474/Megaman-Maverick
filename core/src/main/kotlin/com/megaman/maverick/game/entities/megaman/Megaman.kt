package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Speed
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.*
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.clearLocks
import com.maverick.game.engine.locks.isLocked
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.UnderWaterBubble
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.megaman.handlers.MegamanJetpackHandler
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.megaman.sprites.MegamanTrailSpriteV2
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportContinueProp
import com.megaman.maverick.game.entities.utils.standardOnTeleportEnd
import com.megaman.maverick.game.entities.utils.standardOnTeleportStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelRewards
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.locks.behaviorsLockHook
import com.megaman.maverick.game.players.GravityDeviceComponent
import com.megaman.maverick.game.players.PlayerLight
import com.megaman.maverick.game.players.PlayerLightComponent
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.screens.levels.camera.IFocusable
import com.megaman.maverick.game.state.IGameStateListener
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getContactWater
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

class Megaman(game: MegamanMaverickGame) : AbstractHealthEntity(game), IBodyEntity, ISpritesEntity, IBehaviorsEntity,
    IPointsEntity, IAudioEntity, IAnimatedEntity, IScalableGravityEntity, IFreezableEntity, IGameStateListener,
    IEventListener, IFaceable, IDirectional, IFocusable, IDamagerEntity, IGateTrigger, ICartRider {

    companion object {
        const val TAG = "Megaman"
        const val MEGAMAN_EVENT_LISTENER_TAG = "MegamanEventListener"

        private val explosionOrbTrajectories = gdxArrayOf(
            Vector2(-EXPLOSION_ORB_SPEED, 0f),
            Vector2(-EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(0f, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, 0f),
            Vector2(EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED),
            Vector2(0f, -EXPLOSION_ORB_SPEED),
            Vector2(-EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED)
        )

        private const val UNDER_WATER_BUBBLE_DELAY = 2f

        private const val DEATH_X_OFFSET = 1.5f
        private const val DEATH_Y_OFFSET = 1.5f

        private const val TRAIL_SPRITE_DELAY = 0.1f
    }

    override val damageNegotiator = MegamanDamageNegotiator(this)

    val stunned: Boolean
        get() = !stunTimer.isFinished()

    override val invincible: Boolean
        get() = damaged || !damageRecoveryTimer.isFinished() || isLocked(MegaLockGroup.DAMAGE) || !existenceState.liveAfterUpdate || !inPlay
    override val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()

    override var frozen = false
    private var hitsToUnfreeze = 0
    internal var frozenPushTimer = Timer(MegamanValues.FROZEN_PUSH_DUR)

    val canMove: Boolean
        get() = !isLocked(MegaLockGroup.MOVEMENT) && !stunned && !damaged && !frozen

    internal val stunTimer = Timer(MegamanValues.STUN_DUR)
    internal val damageRecoveryTimer = Timer(MegamanValues.DAMAGE_RECOVERY_TIME).setToEnd()
    internal val damageFlashTimer = Timer(MegamanValues.DAMAGE_FLASH_DURATION)
    internal var recoveryFlash = false

    private val noDmgBounce = objectSetOf<Any>(SpringHead::class)

    internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME).setToEnd()

    internal val chargingTimer = Timer(
        MegamanValues.TIME_TO_FULLY_CHARGED,
        TimeMarkedRunnable(MegamanValues.TIME_TO_HALFWAY_CHARGED) {
            requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false)
        }
    ).setToEnd()

    internal var runTime = 0f
    internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()

    internal val postActionMomentumTimer = Timer(MegamanValues.POST_ACTION_MOMENTUM_DUR).setToEnd()
    internal var pendingPostActionMomentumKill = false

    internal var airDashConsumed = false

    internal val groundSlideToCrouchDelay = Timer(MegamanValues.GROUND_SLIDE_TO_CROUCH_DELAY)
    internal val buttonToCrouchDelay = Timer(MegamanValues.BUTTON_TO_CROUCH_DELAY)

    private val trailSpriteTimer = Timer(TRAIL_SPRITE_DELAY)
    private val underWaterBubbleTimer = Timer(UNDER_WATER_BUBBLE_DELAY)

    override val eventKeyMask = objectSetOf<Any>(
        EventType.BEGIN_ROOM_TRANS,
        EventType.CONTINUE_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER,
        EventType.END_GAME_CAM_ROTATION
    )

    internal lateinit var controllerComponent: ControllerComponent
    internal lateinit var weaponsHandler: MegamanWeaponsHandler
    internal lateinit var jetpackHandler: MegamanJetpackHandler

    val canChargeCurrentWeapon: Boolean
        get() = weaponsHandler.isChargeable(currentWeapon)
    val chargeStatus: MegaChargeStatus
        get() = when {
            fullyCharged -> MegaChargeStatus.FULLY_CHARGED
            charging -> MegaChargeStatus.HALF_CHARGED
            else -> MegaChargeStatus.NOT_CHARGED
        }
    val charging: Boolean
        get() = canChargeCurrentWeapon && chargingTimer.time >= MegamanValues.TIME_TO_HALFWAY_CHARGED
    val halfCharged: Boolean
        get() = chargeStatus == MegaChargeStatus.HALF_CHARGED
    val fullyCharged: Boolean
        get() = canChargeCurrentWeapon && chargingTimer.isFinished()
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

                game.setFocusSnappedAway(true)

                val camDir = if (value.isVertical()) value else value.getOpposite()
                if (game.getGameCamera().direction != camDir) {
                    game.eventsMan.submitEvent(
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
                    wallSlideGravity = MegamanValues.WALL_SLIDE_GRAVITY
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
                    wallSlideGravity = -MegamanValues.WALL_SLIDE_GRAVITY
                    groundGravity = -MegamanValues.GROUND_GRAVITY
                    iceGravity = -MegamanValues.ICE_GRAVITY
                    waterGravity = -MegamanValues.WATER_GRAVITY
                    waterIceGravity = -MegamanValues.WATER_ICE_GRAVITY

                    swimVel = -MegamanValues.SWIM_VEL_Y
                }
            }
        }

    override var facing: Facing
        get() = getProperty(MegamanProps.FACING) as Facing
        set(value) {
            putProperty(MegamanProps.FACING, value)
        }

    var aButtonTask: AButtonTask
        get() = getProperty(MegamanProps.A_BUTTON_TASK) as AButtonTask
        set(value) {
            putProperty(MegamanProps.A_BUTTON_TASK, value)
        }

    var currentWeapon: MegamanWeapon
        get() = getProperty(MegamanProps.WEAPON, MegamanWeapon::class)!!
        set(value) {
            val previous = getProperty(MegamanProps.WEAPON, MegamanWeapon::class)
            weaponsHandler.onChangeWeapon(value, previous)

            putProperty(MegamanProps.WEAPON, value)
        }

    var running: Boolean
        get() = inPlay && getProperty(ConstKeys.RUNNING, Boolean::class)!!
        set(value) {
            putProperty(ConstKeys.RUNNING, value)
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
            forEachAnimator { _, _, animator ->
                animator as Animator
                animator.updateScalar = value
            }
        }

    override var gravityScalar = 1f

    internal var jumpVel = 0f
    internal var wallJumpVel = 0f
    internal var fallGravity = 0f
    internal var jumpGravity = 0f
    internal var wallSlideGravity = 0f
    internal var groundGravity = 0f
    internal var iceGravity = 0f
    internal var waterGravity = 0f
    internal var waterIceGravity = 0f
    internal var swimVel = 0f

    internal var canMakeLandSound = false
    internal var applyMovementScalarToBullet = false

    internal val roomTransPauseTimer = Timer(ConstVals.ROOM_TRANS_DELAY_DURATION)
    internal val spawningTimer = Timer(MegamanValues.SPAWNING_DUR)

    internal val wallSlideNotAllowedTimer = Timer(MegamanValues.WALLSLIDE_NOT_ALLOWED_DELAY)


    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()

        addComponent(PlayPhaseComponent())
        addComponent(TeleportableComponent())
        addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(this)))
        addComponent(defineBodyComponent())
        addComponent(defineBehaviorsComponent())
        controllerComponent = defineControllerComponent()
        addComponent(controllerComponent)
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())

        /*
        val weaponSpawns = OrderedMap<String, IntPair>()

        val weaponSpawnMagicColors = objectSetOf(MegamanValues.WEAPON_SPAWN_MAGIC_COLOR)
        val magicColorsMap = OrderedMap<IntPair, Color>()
        val regionProcessor = object : MegaRegionProcessor {

            override fun process(
                region: TextureRegion,
                defKey: String,
                fullKey: String,
                rows: Int,
                columns: Int,
                durations: Array<Float>,
                loop: Boolean,
                index: Int
            ): TextureRegion {
                GameLogger.debug(TAG, "init(): regionProcessor: defKey=$defKey")

                // TODO: val magical = MagicPixels.get(frame, weaponSpawnMagicColors, magicColorsMap)
                val file = Gdx.files.internal("sprites/frames/Megaman_v2/${defKey}.png")
                val r = TextureRegion(Texture(file)).splitAndFlatten(rows, columns, Array())[index]
                val magical = MagicPixels.get(Pixmap(file), weaponSpawnMagicColors, magicColorsMap)

                val region = when {
                    magical -> {
                        GameLogger.debug(
                            TAG,
                            "init(): regionProcessor: magical region found: map=$magicColorsMap, index=$index"
                        )

                        val data = r.texture.textureData // TODO: frame.texture.textureData
                        if (!data.isPrepared) data.prepare()

                        val pixmap = data.consumePixmap()

                        // there should only be at most one magic color pixel per frame, so we'll just pick the first one
                        magicColorsMap.keys().forEach { position ->
                            val key = MegamanAnimations.splitFullKey(fullKey)[0]
                            weaponSpawns.put(key, position)

                            val (x, y) = position
                            pixmap.drawPixel(x, y, Color.rgba8888(0f, 0f, 0f, 0f))
                        }

                        TextureRegion(Texture(pixmap))
                    }

                    else -> frame
                }

                magicColorsMap.clear()

                return region
            }
        }
         */
        val animations = MegamanAnimations(game /*, regionProcessor */).get()
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

        game.eventsMan.addListener(this)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

        body.physics.velocity.setZero()
        body.physics.collisionOn = true
        body.physics.gravityOn = true
        body.forEachFixture { fixture -> fixture.setActive(true) }

        runTime = 0f

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
        game.gravity.set(
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())
        )

        aButtonTask = AButtonTask.JUMP

        setHealth(getMaxHealth())

        weaponsHandler.setAllToMaxAmmo()
        currentWeapon = MegamanWeapon.MEGA_BUSTER

        running = false

        clearLocks(MegaLockGroup.MOVEMENT)
        require(TeleportableComponent::class).teleporter = null
        clearLocks(MegaLockGroup.DAMAGE)
        recoveryFlash = false
        canMakeLandSound = false

        frozen = false
        hitsToUnfreeze = 0
        frozenPushTimer.setToEnd()

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)

        stunTimer.setToEnd()
        damageTimer.setToEnd()
        damageFlashTimer.reset()
        damageRecoveryTimer.setToEnd()

        shootAnimTimer.reset()
        wallJumpTimer.reset()
        chargingTimer.reset()
        spawningTimer.reset()

        roomTransPauseTimer.setToEnd()

        wallSlideNotAllowedTimer.reset()
        postActionMomentumTimer.setToEnd()
        pendingPostActionMomentumKill = false
        airDashConsumed = false

        putProperty(ConstKeys.ON_TELEPORT_START, { _: ITeleporterEntity ->
            stopCharging()
            standardOnTeleportStart(this)
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) resetBehavior(BehaviorType.AIR_DASHING)
            lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
        })
        setStandardOnTeleportContinueProp(this)
        putProperty(ConstKeys.ON_TELEPORT_END, {
            stopCharging()
            standardOnTeleportEnd(this)
            aButtonTask = AButtonTask.AIR_DASH
            unlock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
            game.setFocusSnappedAway(false)
        })

        removeProperty(ConstKeys.FOCUS)
        game.setFocusSnappedAway(false)

        game.eventsMan.submitEvent(Event(EventType.PLAYER_SPAWN, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        body.removeProperty(ConstKeys.VELOCITY)

        val eventsMan = game.eventsMan
        eventsMan.removeListener(this)
        eventsMan.submitEvent(Event(EventType.PLAYER_JUST_DIED, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))

        stopSoundNow(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        explosionOrbTrajectories.forEach { trajectory ->
            val orb = MegaEntityFactory.fetch(ExplosionOrb::class)!!
            orb.spawn(
                props(
                    ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat()),
                    ConstKeys.POSITION pairTo body.getCenter()
                )
            )
        }

        if (game.getCurrentLevel().type == LevelType.MARIO_LEVEL)
            playSoundNow(SoundAsset.SMB3_PLAYER_DOWN_SOUND, false)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                if (event.key == EventType.BEGIN_ROOM_TRANS) roomTransPauseTimer.reset()

                val position = event.getProperty(ConstKeys.POSITION, Vector2::class)!!
                if (event.key == EventType.BEGIN_ROOM_TRANS) GameLogger.debug(
                    MEGAMAN_EVENT_LISTENER_TAG, "BEGIN ROOM TRANS: position=$position"
                )
                body.setCenter(position)

                if (event.key == EventType.BEGIN_ROOM_TRANS) when {
                    isBehaviorActive(BehaviorType.CLIMBING) || !bodyOverGround -> body.physics.velocity.setZero()
                    else -> when (direction) {
                        Direction.UP, Direction.DOWN -> {
                            val velY = body.physics.velocity.y
                            body.putProperty("${ConstKeys.VELOCITY}_${ConstKeys.Y}", velY)
                        }
                        else -> {
                            val velX = body.physics.velocity.x
                            body.putProperty("${ConstKeys.VELOCITY}_${ConstKeys.X}", velX)
                        }
                    }
                }

                body.physics.gravityOn = false

                stopMomentum(true)

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.END_ROOM_TRANS -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "endRoomTrans()")

                when {
                    isBehaviorActive(BehaviorType.CLIMBING) || !bodyOverGround -> body.physics.velocity.setZero()
                    else -> when (direction) {
                        Direction.UP, Direction.DOWN -> {
                            val velY = body.removeProperty("${ConstKeys.VELOCITY}_${ConstKeys.Y}", Float::class)!!
                            body.physics.velocity.y = velY
                        }
                        else -> {
                            val velX = body.removeProperty("${ConstKeys.VELOCITY}_${ConstKeys.X}", Float::class)!!
                            body.physics.velocity.x = velX
                        }
                    }
                }

                body.physics.gravityOn = !isBehaviorActive(BehaviorType.CLIMBING)

                stopMomentum(true)
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "GATE_INIT_OPENING")

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.STUN_PLAYER -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "STUN_PLAYER: called")

                val targets = event.getProperty(PlayerEventKeys.SLOTS)
                if (targets == null || !targets.contains(playerSlot, false)) {
                    GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "STUN_PLAYER: Megaman is not a target: $targets")
                    return
                }

                if (stunned) {
                    GameLogger.debug(
                        MEGAMAN_EVENT_LISTENER_TAG,
                        "STUN_PLAYER: do not stun Megaman because he is already stunned: " +
                            "stunTimer.getRatio()=${stunTimer.getRatio()}"
                    )
                    return
                }

                val stunType = event.getProperty(ConstKeys.TYPE, StunType::class)!!
                if (stunType == StunType.STUN_BOUNCE_IF_ON_SURFACE &&
                    !body.isSensing(BodySense.FEET_ON_GROUND) &&
                    !isBehaviorActive(BehaviorType.WALL_SLIDING)
                ) {
                    GameLogger.debug(
                        MEGAMAN_EVENT_LISTENER_TAG,
                        "STUN_PLAYER: do not stun because stun type is $stunType and Megaman is not on a surface"
                    )
                    return
                }

                // TODO: This assumes that Megaman's body is rotated UP.
                //    Refactor this to support directionally dynamic bouncing.
                body.physics.velocity.let {
                    it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * movementScalar * -facing.value
                    it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
                }

                stunTimer.reset()
            }

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

        if (!noDmgBounce.contains(damager::class) && damager is IBodyEntity) {
            val bounds = damager.body.getBounds()

            when (direction) {
                Direction.UP -> {
                    body.physics.velocity.x =
                        (if (bounds.getX() > body.getX()) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                    body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
                }
                Direction.DOWN -> {
                    body.physics.velocity.x =
                        (if (bounds.getX() > body.getX()) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                    body.physics.velocity.y = -MegamanValues.DMG_Y * ConstVals.PPM
                }
                Direction.LEFT -> {
                    body.physics.velocity.x = -MegamanValues.DMG_Y * ConstVals.PPM
                    body.physics.velocity.y =
                        (if (bounds.getY() > body.getY()) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                }
                Direction.RIGHT -> {
                    body.physics.velocity.x = MegamanValues.DMG_Y * ConstVals.PPM
                    body.physics.velocity.y =
                        (if (bounds.getY() > body.getY()) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                }
            }
        }

        if (!frozen && damager is IFreezerEntity) {
            frozen = true
            hitsToUnfreeze = 0
            frozenPushTimer.setToEnd()
        }

        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)

        players.onPlayerDamaged(this.asPlayer!!, damager)

        return true
    }

    override fun editDamageFrom(damager: IDamagerEntity, baseDamage: Int): Int {
        var damage = when {
            game.state.containsEnhancement(MegaEnhancement.DAMAGE_INCREASE) -> MegaEnhancement.scaleDamage(
                baseDamage, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR
            )

            else -> baseDamage
        }

        if (game.state.getDifficultyMode() == DifficultyMode.HARD) damage += 1

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
            if (body.getX() < -DEATH_X_OFFSET * ConstVals.PPM || body.getY() < -DEATH_Y_OFFSET * ConstVals.PPM ||
                body.getMaxX() > (game.getTiledMapLoadResult().map.properties.get(ConstKeys.WIDTH) as Int + DEATH_X_OFFSET) * ConstVals.PPM ||
                body.getMaxY() > (game.getTiledMapLoadResult().map.properties.get(ConstKeys.HEIGHT) as Int + DEATH_Y_OFFSET) * ConstVals.PPM
            ) {
                GameLogger.error(TAG, "Megaman is below game bounds, kill him")
                destroy()
            }

            if (!weaponsHandler.isChargeable(currentWeapon)) stopCharging()
            weaponsHandler.update(delta)

            jetpackHandler.update(delta)

            if (body.isSensing(BodySense.FEET_ON_GROUND)) stunTimer.update(delta)
            if (damageTimer.isJustFinished()) damageRecoveryTimer.reset()
            if (stunned || damaged) chargingTimer.reset()

            if (damageTimer.isFinished() && !damageRecoveryTimer.isFinished()) {
                damageRecoveryTimer.update(delta)
                damageFlashTimer.update(delta)
                if (damageFlashTimer.isFinished()) {
                    damageFlashTimer.reset()
                    recoveryFlash = !recoveryFlash
                }
            }
            if (damageRecoveryTimer.isJustFinished()) recoveryFlash = false

            wallJumpTimer.update(delta)
            shootAnimTimer.update(delta)
            roomTransPauseTimer.update(delta)

            if (bodyOverGround) postActionMomentumTimer.update(delta)
            else if (FacingUtils.isFacingBlock(this)) stopMomentum(!running)

            if (bodyOverGround &&
                pendingPostActionMomentumKill &&
                !body.isSensing(BodySense.FEET_ON_ICE)
            ) killPostActionMomentum()

            if (!running && bodyOverGround && !postActionMomentumTimer.isFinished() &&
                !isBehaviorActive(BehaviorType.GROUND_SLIDING) &&
                !body.isSensing(BodySense.FEET_ON_ICE)
            ) {
                val decay = kotlin.math.exp(
                    -MegamanValues.POST_ACTION_BURST_RESISTANCE * delta
                )
                when (direction) {
                    Direction.UP, Direction.DOWN -> body.physics.velocity.x *= decay
                    else -> body.physics.velocity.y *= decay
                }
            }

            if (
                body.isSensing(BodySense.IN_WATER) &&
                !body.isSensing(BodySense.FORCE_APPLIED) &&
                body.getContactWater().any { water ->
                    water.body.getBounds().contains(
                        this.headFixture.getShape().getCenter()
                    )
                }
            ) {
                underWaterBubbleTimer.update(delta)
                if (underWaterBubbleTimer.isFinished()) {
                    spawnBubble()
                    underWaterBubbleTimer.reset()
                }
            }

            trailSpriteTimer.update(delta)
            if (trailSpriteTimer.isFinished()) {
                val trailSpriteSpawned = tryToSpawnTrailSprite()
                if (trailSpriteSpawned) trailSpriteTimer.reset()
            }

            if (inPlay) spawningTimer.update(delta)

            frozenPushTimer.update(delta)
            if (frozen) {
                if (frozenPushTimer.isFinished() &&
                    MegamanValues.BUTTONS_TO_UNFREEZE.any { input.isJustReleased(it) }
                ) {
                    hitsToUnfreeze++
                    frozenPushTimer.reset()
                    requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
                }

                if (hitsToUnfreeze >= MegamanValues.HITS_TO_UNFREEZE) {
                    frozen = false
                    hitsToUnfreeze = 0
                    frozenPushTimer.setToEnd()

                    for (i in 0 until 5) {
                        val shard = MegaEntityFactory.fetch(IceShard::class)!!
                        shard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
                    }
                }
            }

            wallSlideNotAllowedTimer.update(delta)

            if (bodyOverGround) airDashConsumed = false

            if (!bodyOverGround && aButtonTask == AButtonTask.JUMP &&
                !isAnyBehaviorActive(
                    BehaviorType.WALL_SLIDING,
                    BehaviorType.CLIMBING,
                    BehaviorType.JUMPING
                )
            ) aButtonTask = when {
                body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                !airDashConsumed -> AButtonTask.AIR_DASH
                else -> AButtonTask.JUMP
            }

            if (bodyOverGround && aButtonTask == AButtonTask.AIR_DASH &&
                !body.isSensing(BodySense.IN_WATER) &&
                !isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.AIR_DASHING)
            ) aButtonTask = AButtonTask.JUMP
        }
    }

    private fun tryToSpawnTrailSprite(): Boolean {
        val animKey: String

        when {
            isBehaviorActive(BehaviorType.GROUND_SLIDING) -> {
                var key = "groundslide"
                if (shooting) {
                    if (currentWeapon == MegamanWeapon.NEEDLE_SPIN) return false
                    else if (currentWeapon != MegamanWeapon.RODENT_CLAWS) key += "_shoot"
                }
                animKey = key
            }

            isBehaviorActive(BehaviorType.AIR_DASHING) -> {
                var key = "airdash"
                if (shooting) {
                    if (currentWeapon == MegamanWeapon.NEEDLE_SPIN) return false
                    else if (currentWeapon != MegamanWeapon.RODENT_CLAWS) key += "_shoot"
                }
                animKey = key
            }

            else -> return false
        }

        val trailSprite = MegaEntityFactory.fetch(MegamanTrailSpriteV2::class)!!
        return trailSprite.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.KEY pairTo animKey))
    }

    private fun spawnBubble() {
        val offsetY = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.05f else 0.1f
        val offsetX = 0.2f * facing.value

        val spawn = body.getCenter().add(offsetX * ConstVals.PPM, offsetY * ConstVals.PPM)

        val bubble = MegaEntityFactory.fetch(UnderWaterBubble::class)!!
        bubble.spawn(
            props(
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.SPEED pairTo Speed.SLOW,
                ConstKeys.POSITION pairTo spawn
            )
        )
    }

    override fun getFocusBounds() = body.getBounds()

    override fun getFocusPosition(): Vector2 {
        if (hasProperty(ConstKeys.FOCUS)) return getProperty(ConstKeys.FOCUS, Vector2::class)!!

        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val focus = body.getBounds().getPositionPoint(position)
        when (direction) {
            Direction.UP -> focus.y += MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.DOWN -> focus.y -= MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.LEFT -> focus.x -= MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
            Direction.RIGHT -> focus.x += MEGAMAN_BODY_HEIGHT * ConstVals.PPM / 2f
        }
        return focus
    }

    override fun onAddLevelDefeated(level: LevelDefinition) {
        game.putProperty(ConstKeys.WEAPONS_ATTAINED, LevelRewards.weaponsFor(level))
    }

    override fun onAddHeartTank(heartTank: MegaHeartTank) = setMaxHealthFromHeartTanks()

    override fun onRemoveHeartTank(heartTank: MegaHeartTank) = setMaxHealthFromHeartTanks()

    // TODO: the max health is a stored copy that is set when a heart tank changes; a max that is read through from
    //  the game state on each access needs a change to the engine Points (backlog candidate "max health read through
    //  from the profile", item 029)
    private fun setMaxHealthFromHeartTanks() {
        val heartTanks = MegaHeartTank.entries.count { game.state.containsHeartTank(it) }
        setMaxHealth(MegamanValues.START_HEALTH + heartTanks * MegaHeartTank.HEALTH_BUMP)
    }

    override fun onAddCurrency(value: Int) {
        if (game.state.getCurrency() < game.state.getMaxCurrency())
            requestToPlaySound(SoundAsset.CURRENCY_PICKUP_SOUND, false)
    }

    fun translateAmmo(ammo: Int, weapon: MegamanWeapon = currentWeapon): Int {
        val remaining = weaponsHandler.translateAmmo(weapon, ammo)
        GameLogger.debug(TAG, "translateAmmo(): weapon=$weapon, ammo=$ammo, remaining=$remaining")
        return remaining
    }

    fun stopCharging() {
        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        chargingTimer.reset()
    }

    fun shoot(): Boolean {
        val shot = weaponsHandler.fireWeapon(currentWeapon, chargeStatus)
        if (shot) shootAnimTimer.reset()
        return shot
    }

    fun shouldPreserveMomentum(checkTimer: Boolean): Boolean {
        if (body.isSensing(BodySense.FEET_ON_SNOW)) return false
        if (body.isSensing(BodySense.FEET_ON_ICE)) return true
        return !checkTimer || !postActionMomentumTimer.isFinished()
    }

    fun preserveMomentum() {
        postActionMomentumTimer.reset()
    }

    fun stopMomentum(stopMegaman: Boolean) {
        postActionMomentumTimer.setToEnd()
        if (stopMegaman) when (direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
            else -> body.physics.velocity.y = 0f
        }
    }

    fun killPostActionMomentum() {
        val maxRun = getMaxRunSpeed()
        when (direction) {
            Direction.UP, Direction.DOWN ->
                body.physics.velocity.x = body.physics.velocity.x.coerceIn(-maxRun, maxRun)
            else ->
                body.physics.velocity.y = body.physics.velocity.y.coerceIn(-maxRun, maxRun)
        }
        pendingPostActionMomentumKill = false
    }

    override fun getType() = EntityType.MEGAMAN

    override fun getTag() = TAG
}
