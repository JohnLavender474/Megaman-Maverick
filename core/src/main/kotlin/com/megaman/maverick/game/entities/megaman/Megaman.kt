package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.megaman.contracts.IMegamanDamageListener
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.megaman.sprites.amendKey
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportContinueProp
import com.megaman.maverick.game.entities.utils.standardOnTeleportEnd
import com.megaman.maverick.game.entities.utils.standardOnTeleportStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.camera.IFocusable
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

class Megaman(game: MegamanMaverickGame) : AbstractHealthEntity(game), IMegaUpgradable, IEventListener, IFaceable,
    IDirectional, IBodyEntity, ISpritesEntity, IBehaviorsEntity, IPointsEntity, IAudioEntity, IAnimatedEntity,
    IScalableGravityEntity, IFocusable {

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

    val damaged: Boolean
        get() = !damageTimer.isFinished()
    val stunned: Boolean
        get() = !stunTimer.isFinished()

    override val damageNegotiator = MegamanDamageNegotiator(this)

    override val invincible: Boolean
        get() = damaged || !damageRecoveryTimer.isFinished() || !canBeDamaged || dead || !ready
    public override val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()

    var canBeDamaged = true
    var canMove = true
        get() = field && !stunned && !damaged

    internal val stunTimer = Timer(MegamanValues.STUN_DUR)
    internal val damageRecoveryTimer = Timer(MegamanValues.DAMAGE_RECOVERY_TIME).setToEnd()
    internal val damageFlashTimer = Timer(MegamanValues.DAMAGE_FLASH_DURATION)

    private val noDmgBounce = objectSetOf<Any>(SpringHead::class)

    internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME).setToEnd()
    internal val chargingTimer = Timer(
        MegamanValues.TIME_TO_FULLY_CHARGED,
        TimeMarkedRunnable(MegamanValues.TIME_TO_HALFWAY_CHARGED) {
            requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false)
        }
    ).setToEnd()
    internal val airDashTimer = Timer(MegamanValues.MAX_AIR_DASH_TIME)
    internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()
    internal val groundSlideTimer = Timer(MegamanValues.MAX_GROUND_SLIDE_TIME)

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

    override val upgradeHandler = MegamanUpgradeHandler(game.state, this)

    lateinit var weaponsHandler: MegamanWeaponsHandler
    // val weaponHandler = MegamanWeaponHandler(this)

    val canChargeCurrentWeapon: Boolean
        get() = weaponsHandler.isChargeable(currentWeapon)
    val chargeStatus: MegaChargeStatus
        get() = if (fullyCharged) MegaChargeStatus.FULLY_CHARGED
        else if (charging) MegaChargeStatus.HALF_CHARGED else MegaChargeStatus.NOT_CHARGED
    val charging: Boolean
        get() = canChargeCurrentWeapon && chargingTimer.time >= MegamanValues.TIME_TO_HALFWAY_CHARGED
    val halfCharged: Boolean
        get() = chargeStatus == MegaChargeStatus.HALF_CHARGED
    val fullyCharged: Boolean
        get() = canChargeCurrentWeapon && chargingTimer.isFinished()
    val shooting: Boolean
        get() = !shootAnimTimer.isFinished()
    val ammo: Int
        get() = if (currentWeapon == MegamanWeapon.BUSTER) Int.MAX_VALUE
        else weaponsHandler.getAmmo(currentWeapon)

    var ready = false
    var maverick = false
    var recoveryFlash = false

    override var direction: Direction
        get() = body.direction
        set(value) {
            GameLogger.debug(TAG, "direction-set(): value=$value")

            if (value != body.direction) {
                GameLogger.debug(TAG, "direction-set(): value not same as field")

                body.direction = value

                val camDir = if (value.isVertical()) value else value.getOpposite()
                if (game.getGameCamera().direction != camDir) {
                    game.eventsMan.submitEvent(
                        Event(
                            EventType.START_GAME_CAM_ROTATION,
                            props(ConstKeys.DIRECTION pairTo camDir)
                        )
                    )

                    canMove = false
                    body.physics.gravityOn = false
                    body.physics.velocity.setZero()

                    resetBehavior(BehaviorType.JETPACKING)
                }
            } else GameLogger.debug(TAG, "direction-set(): value same as field")

            when (value) {
                Direction.UP, Direction.RIGHT -> {
                    jumpVel = MegamanValues.JUMP_VEL
                    wallJumpVel = MegamanValues.WALL_JUMP_VEL
                    cartJumpVel = MegamanValues.CART_JUMP_VEL

                    gravity = MegamanValues.GRAVITY
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
                    cartJumpVel = -MegamanValues.CART_JUMP_VEL

                    gravity = -MegamanValues.GRAVITY
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
        get() = ready && getProperty(ConstKeys.RUNNING) as Boolean
        set(value) {
            putProperty(ConstKeys.RUNNING, value)
        }

    var teleporting = false
        private set

    var movementScalar = 1f
        set(value) {
            field = value
            forEachAnimator { key, sprite, animator ->
                animator as Animator
                animator.updateScalar = value
            }
        }

    val slipSliding: Boolean
        get() = body.isSensing(BodySense.FEET_ON_GROUND) && abs(
            if (direction.isVertical()) body.physics.velocity.x else body.physics.velocity.y
        ) >= MegamanValues.SLIP_SLIDE_VEL_THRESHOLD * ConstVals.PPM

    override var gravityScalar = 1f

    internal var jumpVel = 0f
    internal var wallJumpVel = 0f
    internal var cartJumpVel = 0f
    internal var gravity = 0f
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

    private var neverSpawnedBefore = true

    private val damageListeners = OrderedSet<IMegamanDamageListener>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        super.init()

        addComponent(defineBodyComponent())
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())

        /*
        val weaponSpawns = OrderedMap<String, IntPair>()

        val weaponSpawnMagicColors = objectSetOf(MegamanValues.WEAPON_SPAWN_MAGIC_COLOR)
        val magicColorsMap = OrderedMap<IntPair, Color>()
        val regionProcessor: (TextureRegion, String, String, AnimationDef, Int) -> TextureRegion =
            regionProcessor@{ frame, fullKey, defKey, def, index ->
                GameLogger.debug(TAG, "init(): regionProcessor: defKey=$defKey")

                // TODO: val magical = MagicPixels.get(frame, weaponSpawnMagicColors, magicColorsMap)
                val file = Gdx.files.internal("sprites/frames/Megaman_v2/${defKey}.png")
                val r = TextureRegion(Texture(file)).splitAndFlatten(def.rows, def.cols, Array())[index]
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

                return@regionProcessor region
            }
         */

        val animations = MegamanAnimations(game /* regionProcessor */).get()
        addComponent(defineAnimationsComponent(animations))

        // GameLogger.debug(TAG, "init(): weaponSpawns=$weaponSpawns")

        weaponsHandler = MegamanWeaponsHandler(this /* , weaponSpawns */)
        weaponsHandler.putWeapon(MegamanWeapon.BUSTER)

        aButtonTask = AButtonTask.JUMP
        currentWeapon = MegamanWeapon.BUSTER
    }

    override fun onSpawn(spawnProps: Properties) {
        // GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

        body.physics.velocity.setZero()
        body.physics.collisionOn = true
        body.physics.gravityOn = true
        body.forEachFixture { fixture -> fixture.setActive(true) }

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        aButtonTask = AButtonTask.JUMP
        currentWeapon = MegamanWeapon.BUSTER

        setHealth(getMaxHealth())
        weaponsHandler.setAllToMaxAmmo()

        running = false
        recoveryFlash = false
        canMove = true
        canBeDamaged = true
        teleporting = false
        canMakeLandSound = false

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)

        damageTimer.setToEnd()
        damageRecoveryTimer.setToEnd()
        damageFlashTimer.reset()
        shootAnimTimer.reset()
        groundSlideTimer.reset()
        wallJumpTimer.reset()
        chargingTimer.reset()
        airDashTimer.reset()
        spawningTimer.reset()
        roomTransPauseTimer.setToEnd()
        stunTimer.setToEnd()

        putProperty(ConstKeys.ON_TELEPORT_START, {
            stopCharging()
            standardOnTeleportStart(this)
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) resetBehavior(BehaviorType.AIR_DASHING)
            teleporting = true
            canBeDamaged = false
        })
        setStandardOnTeleportContinueProp(this)
        putProperty(ConstKeys.ON_TELEPORT_END, {
            stopCharging()
            standardOnTeleportEnd(this)
            aButtonTask = AButtonTask.AIR_DASH
            teleporting = false
            canBeDamaged = true
        })

        neverSpawnedBefore = false
    }

    override fun onDestroy() {
        // GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        body.removeProperty(ConstKeys.VELOCITY)

        val eventsMan = game.eventsMan
        eventsMan.removeListener(this)
        eventsMan.submitEvent(Event(EventType.PLAYER_JUST_DIED))

        stopSoundNow(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)

        if (getCurrentHealth() <= 0) explosionOrbTrajectories.forEach { trajectory ->
            val explosionOrb = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION_ORB)
            explosionOrb?.spawn(
                props(
                    ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat()),
                    ConstKeys.POSITION pairTo body.getCenter()
                )
            )
        }
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                if (event.key == EventType.BEGIN_ROOM_TRANS) roomTransPauseTimer.reset()

                val position = event.properties.get(ConstKeys.POSITION, Vector2::class)!!

                if (event.key == EventType.BEGIN_ROOM_TRANS) GameLogger.debug(
                    MEGAMAN_EVENT_LISTENER_TAG, "BEGIN ROOM TRANS: position=$position"
                )

                body.setCenter(position)
                body.physics.gravityOn = false

                if (event.key == EventType.BEGIN_ROOM_TRANS && !body.hasProperty(ConstKeys.VELOCITY))
                    body.putProperty(ConstKeys.VELOCITY, body.physics.velocity.cpy())

                body.physics.velocity.setZero()

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.END_ROOM_TRANS -> {
                val setVel = event.getOrDefaultProperty(ConstKeys.VELOCITY, true, Boolean::class)

                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "endRoomTrans(): setVel=$setVel")

                when {
                    setVel && !isAnyBehaviorActive(
                        BehaviorType.CLIMBING,
                        BehaviorType.JETPACKING,
                        BehaviorType.RIDING_CART,
                        BehaviorType.SWIMMING
                    ) -> {
                        val velocity = body.getProperty(ConstKeys.VELOCITY, Vector2::class)
                        velocity?.let { body.physics.velocity.set(it) }
                    }

                    else -> body.physics.velocity.setZero()
                }

                body.physics.gravityOn = !isBehaviorActive(BehaviorType.CLIMBING)
                body.removeProperty(ConstKeys.VELOCITY)
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "GATE_INIT_OPENING")

                body.physics.gravityOn = false
                if (!body.hasProperty(ConstKeys.VELOCITY)) body.putProperty(
                    ConstKeys.VELOCITY,
                    body.physics.velocity.cpy()
                )
                body.physics.velocity.setZero()

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.STUN_PLAYER -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "STUN_PLAYER: called")

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
                megaman.body.physics.velocity.let {
                    it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * movementScalar * -facing.value
                    it.y = MegamanValues.STUM_IMPULSE_Y * ConstVals.PPM
                }

                stunTimer.reset()
            }

            EventType.END_GAME_CAM_ROTATION -> {
                canMove = true
                body.physics.gravityOn = true
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamager) = when {
        !super.canBeDamagedBy(damager) || dead -> false
        damager is IProjectileEntity -> damager.owner != this
        else -> true
    }

    fun addDamageListener(damageListener: IMegamanDamageListener) = damageListeners.add(damageListener)

    fun removeDamageListener(damageListener: IMegamanDamageListener) = damageListeners.remove(damageListener)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val oldHealth = getCurrentHealth()

        if (!super.takeDamageFrom(damager)) return false

        val healthDiff = oldHealth - getCurrentHealth()
        GameLogger.debug(TAG, "takeDamageFrom(): healthDiff=$healthDiff, damager=$damager")

        if (!isBehaviorActive(BehaviorType.RIDING_CART) && !noDmgBounce.contains(damager::class) && damager is IBodyEntity) {
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

        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)

        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)

        damageListeners.forEach { it.onMegamanDamaged(damager, this) }

        return true
    }

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when {
        has(MegaEnhancement.DAMAGE_INCREASE) ->
            MegaEnhancement.scaleDamage(baseDamage, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR)

        else -> baseDamage
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
                body.getMaxX() > (game.getTiledMapLoadResult().map.properties.get("width") as Int + DEATH_X_OFFSET) * ConstVals.PPM ||
                body.getMaxY() > (game.getTiledMapLoadResult().map.properties.get("height") as Int + DEATH_Y_OFFSET) * ConstVals.PPM
            ) {
                GameLogger.error(TAG, "Megaman is below game bounds, killing him")

                destroy()
            }

            if (!weaponsHandler.isChargeable(currentWeapon)) stopCharging()
            weaponsHandler.update(delta)

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

            shootAnimTimer.update(delta)
            wallJumpTimer.update(delta)
            roomTransPauseTimer.update(delta)

            if (body.isSensing(BodySense.IN_WATER)) {
                underWaterBubbleTimer.update(delta)
                if (underWaterBubbleTimer.isFinished()) {
                    spawnBubbles()
                    underWaterBubbleTimer.reset()
                }
            }

            trailSpriteTimer.update(delta)
            if (trailSpriteTimer.isFinished()) {
                val spawnTrailSprite = when {
                    isBehaviorActive(BehaviorType.GROUND_SLIDING) -> spawnTrailSprite("groundslide")
                    isBehaviorActive(BehaviorType.AIR_DASHING) -> spawnTrailSprite("airdash")
                    // isBehaviorActive(BehaviorType.JUMPING) -> spawnTrailSprite("jump")
                    else -> false
                }
                if (spawnTrailSprite) trailSpriteTimer.reset()
            }

            if (ready) spawningTimer.update(delta)
        }
    }

    private fun spawnTrailSprite(animKey: String? = null): Boolean {
        val key = if (animKey != null) amendKey(animKey) else null
        val trailSprite = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MEGAMAN_TRAIL_SPRITE_V2)!!
        return trailSprite.spawn(props(ConstKeys.KEY pairTo key))
    }

    private fun spawnBubbles() {
        val bubbles = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.UNDER_WATER_BUBBLE)!!
        val offsetY = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.05f else 0.1f
        val offsetX = 0.2f * facing.value
        val spawn = body.getCenter().add(offsetX * ConstVals.PPM, offsetY * ConstVals.PPM)
        bubbles.spawn(props(ConstKeys.POSITION pairTo spawn))
    }

    override fun getFocusBounds() = body.getBounds()

    override fun getFocusPosition(): Vector2 {
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

    override fun getType() = EntityType.MEGAMAN
}
