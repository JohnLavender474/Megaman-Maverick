package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IBoundsSupplier
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTankFist
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTank
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRat
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRatClaw
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.hazards.*
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.megaman.extensions.clearFeetBlocks
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.special.Togglee
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportContinueProp
import com.megaman.maverick.game.entities.utils.standardOnTeleportEnd
import com.megaman.maverick.game.entities.utils.standardOnTeleportStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensingAny
import kotlin.reflect.KClass

class Megaman(game: MegamanMaverickGame) : MegaGameEntity(game), IMegaUpgradable, IEventListener, IFaceable,
    IDamageable, IDirectionRotatable, IBodyEntity, IHealthEntity, ISpritesEntity, IBehaviorsEntity, IPointsEntity,
    IAudioEntity, IAnimatedEntity, IScalableGravityEntity, IBoundsSupplier {

    companion object {
        const val TAG = "Megaman"
        const val MEGAMAN_EVENT_LISTENER_TAG = "MegamanEventListener"
    }

    val damaged: Boolean
        get() = !damageTimer.isFinished()
    val stunned: Boolean
        get() = !stunTimer.isFinished()

    override val invincible: Boolean
        get() = damaged || !damageRecoveryTimer.isFinished() || !canBeDamaged

    var canBeDamaged = true
    var canMove = true
        get() = field && !stunned && !damaged

    internal val stunTimer = Timer()
    internal val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()
    internal val damageRecoveryTimer = Timer(MegamanValues.DAMAGE_RECOVERY_TIME).setToEnd()
    internal val damageFlashTimer = Timer(MegamanValues.DAMAGE_FLASH_DURATION)

    private val dmgNegotations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg(4),
        Bat::class pairTo dmgNeg(2),
        Met::class pairTo dmgNeg(2),
        DragonFly::class pairTo dmgNeg(3),
        FloatingCan::class pairTo dmgNeg(2),
        FlyBoy::class pairTo dmgNeg(3),
        GapingFish::class pairTo dmgNeg(2),
        SpringHead::class pairTo dmgNeg(3),
        SuctionRoller::class pairTo dmgNeg(2),
        MagFly::class pairTo dmgNeg(3),
        Explosion::class pairTo dmgNeg(2),
        JoeBall::class pairTo dmgNeg(3),
        Snowball::class pairTo dmgNeg(1),
        SnowballExplosion::class pairTo dmgNeg(1),
        SwinginJoe::class pairTo dmgNeg(2),
        SniperJoe::class pairTo dmgNeg(3),
        ShieldAttacker::class pairTo dmgNeg(4),
        Penguin::class pairTo dmgNeg(3),
        Hanabiran::class pairTo dmgNeg(3),
        Petal::class pairTo dmgNeg(3),
        CaveRock::class pairTo dmgNeg(3),
        CaveRocker::class pairTo dmgNeg(3),
        CaveRockExplosion::class pairTo dmgNeg(2),
        Elecn::class pairTo dmgNeg(3),
        ElectricBall::class pairTo dmgNeg(3),
        Ratton::class pairTo dmgNeg(2),
        PicketJoe::class pairTo dmgNeg(3),
        Picket::class pairTo dmgNeg(3),
        LaserBeamer::class pairTo dmgNeg(3),
        CartinJoe::class pairTo dmgNeg(3),
        Bolt::class pairTo dmgNeg(3),
        ElectrocutieChild::class pairTo dmgNeg(3),
        Togglee::class pairTo dmgNeg(3),
        Eyee::class pairTo dmgNeg(3),
        Adamski::class pairTo dmgNeg(3),
        UpNDown::class pairTo dmgNeg(3),
        BigJumpingJoe::class pairTo dmgNeg(6),
        SniperJoeShield::class pairTo dmgNeg(2),
        SuicideBummer::class pairTo dmgNeg(3),
        Gachappan::class pairTo dmgNeg(5),
        ExplodingBall::class pairTo dmgNeg(3),
        Imorm::class pairTo dmgNeg(3),
        SpikeBall::class pairTo dmgNeg(8),
        Peat::class pairTo dmgNeg(2),
        BulbBlaster::class pairTo dmgNeg(2),
        Bospider::class pairTo dmgNeg(5),
        BabySpider::class pairTo dmgNeg(2),
        GutsTank::class pairTo dmgNeg(3),
        GutsTankFist::class pairTo dmgNeg(3),
        PurpleBlast::class pairTo dmgNeg(3),
        HeliMet::class pairTo dmgNeg(3),
        SigmaRat::class pairTo dmgNeg(3),
        SigmaRatElectricBall::class pairTo dmgNeg(3),
        SigmaRatElectricBallExplosion::class pairTo dmgNeg(2),
        SigmaRatClaw::class pairTo dmgNeg(2),
        Fireball::class pairTo dmgNeg(3),
        BoulderProjectile::class pairTo dmgNeg {
            it as BoulderProjectile
            when (it.size) {
                Size.LARGE -> 4
                Size.MEDIUM -> 2
                Size.SMALL -> 1
            }
        },
        PetitDevil::class pairTo dmgNeg(3),
        PetitDevilChild::class pairTo dmgNeg(2),
        Shotman::class pairTo dmgNeg(2),
        Snowhead::class pairTo dmgNeg(2),
        SnowheadThrower::class pairTo dmgNeg(3),
        Spiky::class pairTo dmgNeg(4),
        PenguinMiniBoss::class pairTo dmgNeg(3),
        BabyPenguin::class pairTo dmgNeg(2),
        UFOBomb::class pairTo dmgNeg(3),
        OLD_UFOBombBot::class pairTo dmgNeg(2),
        RollingBot::class pairTo dmgNeg(3),
        RollingBotShot::class pairTo dmgNeg(3),
        AcidGoop::class pairTo dmgNeg(3),
        ToxicBarrelBot::class pairTo dmgNeg(3),
        ToxicGoopShot::class pairTo dmgNeg(3),
        ToxicGoopSplash::class pairTo dmgNeg(3),
        ReactorMonkeyBall::class pairTo dmgNeg(3),
        ReactorMonkeyMiniBoss::class pairTo dmgNeg(3),
        SmokePuff::class pairTo dmgNeg(2),
        TubeBeam::class pairTo dmgNeg(5),
        ReactorMan::class pairTo dmgNeg(3),
        ReactManProjectile::class pairTo dmgNeg(3),
        FlameThrower::class pairTo dmgNeg(6),
        Popoheli::class pairTo dmgNeg(3),
        AngryFlameBall::class pairTo dmgNeg(3),
        LavaDrop::class pairTo dmgNeg(6),
        PopupCanon::class pairTo dmgNeg(3),
        Asteroid::class pairTo dmgNeg(3),
        AsteroidExplosion::class pairTo dmgNeg(3),
        MoonHeadMiniBoss::class pairTo dmgNeg(3),
        BunbyRedRocket::class pairTo dmgNeg(3),
        BunbyTank::class pairTo dmgNeg(3),
        FireMet::class pairTo dmgNeg(3),
        FireMetFlame::class pairTo dmgNeg(3),
        Robbit::class pairTo dmgNeg(3),
        Pipi::class pairTo dmgNeg(3),
        PipiEgg::class pairTo dmgNeg(3),
        Copipi::class pairTo dmgNeg(3),
        MechaDragonMiniBoss::class pairTo dmgNeg(3),
        SpitFireball::class pairTo dmgNeg(3),
        BombPotton::class pairTo dmgNeg(3),
        SmallMissile::class pairTo dmgNeg(3),
        GreenExplosion::class pairTo dmgNeg(3),
        Arigock::class pairTo dmgNeg(3),
        ArigockBall::class pairTo dmgNeg(3),
        TotemPolem::class pairTo dmgNeg(3),
        CactusLauncher::class pairTo dmgNeg(3),
        CactusMissile::class pairTo dmgNeg(3),
        ColtonJoe::class pairTo dmgNeg(3),
        SphinxBall::class pairTo dmgNeg(3),
        SphinxMiniBoss::class pairTo dmgNeg(3),
        SpikeBot::class pairTo dmgNeg(3),
        Needle::class pairTo dmgNeg(3),
        JetpackIceBlaster::class pairTo dmgNeg(3),
        TeardropBlast::class pairTo dmgNeg(3),
        FallingIcicle::class pairTo dmgNeg(3),
        SmallIceCube::class pairTo dmgNeg(1),
        UnderwaterFan::class pairTo dmgNeg(5),
        Tropish::class pairTo dmgNeg(3),
        SeaMine::class pairTo dmgNeg(3),
        Sealion::class pairTo dmgNeg(3),
        SealionBall::class pairTo dmgNeg(3),
        YellowTiggerSquirt::class pairTo dmgNeg(3),
        UFOhNoBot::class pairTo dmgNeg(3),
        TankBot::class pairTo dmgNeg(3),
        Darspider::class pairTo dmgNeg(3),
        WalrusBot::class pairTo dmgNeg(3),
        BigFishNeo::class pairTo dmgNeg(4),
        GlacierMan::class pairTo dmgNeg(4),
        Matasaburo::class pairTo dmgNeg(3),
        CarriCarry::class pairTo dmgNeg(3),
        Cactus::class pairTo dmgNeg(2),
        DesertMan::class pairTo dmgNeg(3),
        FireDispensenator::class pairTo dmgNeg(3),
        FireWall::class pairTo dmgNeg(4),
        DemonMet::class pairTo dmgNeg(3),
        FirePellet::class pairTo dmgNeg(3),
        InfernoMan::class pairTo dmgNeg(3),
        MagmaOrb::class pairTo dmgNeg(4),
        MagmaMeteor::class pairTo dmgNeg(4),
        MagmaExplosion::class pairTo dmgNeg(4),
        MagmaGoop::class pairTo dmgNeg(4),
        MagmaGoopExplosion::class pairTo dmgNeg(4),
        MagmaWave::class pairTo dmgNeg(4),
        MagmaPellet::class pairTo dmgNeg(3),
        MagmaFlame::class pairTo dmgNeg(3)
    )
    private val noDmgBounce = objectSetOf<Any>(SpringHead::class)

    internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME).setToEnd()
    internal val chargingTimer =
        Timer(MegamanValues.TIME_TO_FULLY_CHARGED, TimeMarkedRunnable(MegamanValues.TIME_TO_HALFWAY_CHARGED) {
            requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, false)
        }).setToEnd()
    internal val airDashTimer = Timer(MegamanValues.MAX_AIR_DASH_TIME)
    internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()
    internal val groundSlideTimer = Timer(MegamanValues.MAX_GROUND_SLIDE_TIME)

    override val eventKeyMask = objectSetOf<Any>(
        EventType.BEGIN_ROOM_TRANS,
        EventType.CONTINUE_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER,
        EventType.END_GAME_CAM_ROTATION
    )
    override val upgradeHandler = MegamanUpgradeHandler(game.state, this)

    val weaponHandler = MegamanWeaponHandler(this)
    val canChargeCurrentWeapon: Boolean
        get() = weaponHandler.isChargeable(currentWeapon)
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
        else weaponHandler.getAmmo(currentWeapon)

    var damageFlash = false
    var maverick = false
    var ready = false
        set(value) {
            field = value
            sprites.get("megaman")!!.hidden = !field
        }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            GameLogger.debug(TAG, "directionRotation-set(): value = $value")

            if (value != body.cardinalRotation) {
                GameLogger.debug(TAG, "directionRotation-set(): value is different from field; rotating camera")

                val direction = if (value.isVertical()) value else value.getOpposite()
                game.eventsMan.submitEvent(
                    Event(EventType.SET_GAME_CAM_ROTATION, props(ConstKeys.DIRECTION pairTo direction))
                )

                canMove = false
                body.physics.gravityOn = false
                body.physics.velocity.setZero()
                resetBehavior(BehaviorType.JETPACKING)
            }

            body.cardinalRotation = value

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
        get() = getProperty(MegamanProps.WEAPON) as MegamanWeapon
        set(value) {
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
            animators.forEach {
                val animator = it.second
                if (animator is Animator) animator.updateScalar = value
            }
        }

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
    internal var applyMovementScalarToBullet = false
    internal val roomTransPauseTimer = Timer(ConstVals.ROOM_TRANS_DELAY_DURATION)

    override fun getEntityType() = EntityType.MEGAMAN

    override fun init() {
        GameLogger.debug(TAG, "init")
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        weaponHandler.putWeapon(MegamanWeapon.BUSTER)
        weaponHandler.putWeapon(MegamanWeapon.RUSH_JETPACK)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getBottomCenterPoint(), Position.BOTTOM_CENTER)

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        setHealth(getMaxHealth())
        weaponHandler.setAllToMaxAmmo()

        aButtonTask = AButtonTask.JUMP
        currentWeapon = MegamanWeapon.BUSTER
        running = false
        damageFlash = false
        canMove = true
        canBeDamaged = true
        teleporting = false
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
        roomTransPauseTimer.setToEnd()

        putProperty(ConstKeys.ON_TELEPORT_START, {
            standardOnTeleportStart(this)
            stopCharging()
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) resetBehavior(BehaviorType.AIR_DASHING)
            teleporting = true
            canBeDamaged = false
        })
        setStandardOnTeleportContinueProp(this)
        putProperty(ConstKeys.ON_TELEPORT_END, {
            standardOnTeleportEnd(this)
            stopCharging()
            aButtonTask = AButtonTask.AIR_DASH
            teleporting = false
            canBeDamaged = true
        })

        clearFeetBlocks()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        body.removeProperty(ConstKeys.VELOCITY)
        val eventsMan = game.eventsMan
        eventsMan.removeListener(this)
        eventsMan.submitEvent(Event(EventType.PLAYER_JUST_DIED))
        stopSoundNow(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        if (getCurrentHealth() > 0) return

        val explosionOrbTrajectories = gdxArrayOf(
            Vector2(-EXPLOSION_ORB_SPEED, 0f),
            Vector2(-EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(0f, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, 0f),
            Vector2(EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED),
            Vector2(0f, -EXPLOSION_ORB_SPEED),
            Vector2(-EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED)
        )
        explosionOrbTrajectories.forEach { trajectory ->
            val explosionOrb = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION_ORB)
            explosionOrb?.spawn(
                props(
                    ConstKeys.TRAJECTORY pairTo trajectory.scl(ConstVals.PPM.toFloat()),
                    ConstKeys.POSITION pairTo body.getCenter()
                )
            )
        }
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                if (event.key == EventType.BEGIN_ROOM_TRANS) roomTransPauseTimer.reset()

                val position = event.properties.get(ConstKeys.POSITION) as Vector2
                GameLogger.debug(
                    MEGAMAN_EVENT_LISTENER_TAG, "BEGIN/CONTINUE ROOM TRANS: position = $position"
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
                if (setVel && !isAnyBehaviorActive(
                        BehaviorType.CLIMBING,
                        BehaviorType.JETPACKING,
                        BehaviorType.RIDING_CART,
                        BehaviorType.SWIMMING
                    )
                ) {
                    val velocity = body.getProperty(ConstKeys.VELOCITY, Vector2::class)
                    velocity?.let { body.physics.velocity.set(it) }
                } else body.physics.velocity.setZero()
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
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "STUN_PLAYER_IF_ON_SURFACE")
                when (val stunType = event.getProperty(ConstKeys.TYPE, StunType::class)!!) {
                    StunType.STUN_BOUNCE_IF_ON_SURFACE,
                    StunType.STUN_BOUNCE_ALWAYS -> {
                        if (stunType == StunType.STUN_BOUNCE_IF_ON_SURFACE &&
                            !body.isSensingAny(
                                BodySense.FEET_ON_GROUND,
                                BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                                BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                            )
                        ) return

                        val stunOriginX = event.getProperty(ConstKeys.X, Float::class)!!
                        // TODO: replace with new stunBounds method which accepts rectangle
                        // stunBounce(stunOriginX)
                        val stunDuration = event.getProperty(ConstKeys.DURATION, Float::class)!!
                        stunTimer.resetDuration(stunDuration)
                    }
                }
            }

            EventType.END_GAME_CAM_ROTATION -> {
                canMove = true
                body.physics.gravityOn = true
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && dmgNegotations.containsKey(damager::class) &&
            (damager is AbstractEnemy || damager is IHazard ||
                (damager is IProjectileEntity && damager.owner != this))

    fun setToNextWeapon() {
        val index = currentWeapon.ordinal
        val nextIndex = (index + 1) % MegamanWeapon.entries.size
        currentWeapon = MegamanWeapon.entries.toTypedArray()[nextIndex]
    }

    fun stunBounce(bounds: GameRectangle) =
        when (directionRotation) {
            Direction.UP -> {
                body.physics.velocity.x =
                    (if (bounds.x > body.x) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
            }

            Direction.DOWN -> {
                body.physics.velocity.x =
                    (if (bounds.x > body.x) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
                body.physics.velocity.y = -MegamanValues.DMG_Y * ConstVals.PPM
            }

            Direction.LEFT -> {
                body.physics.velocity.x = -MegamanValues.DMG_Y * ConstVals.PPM
                body.physics.velocity.y =
                    (if (bounds.y > body.y) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
            }

            Direction.RIGHT -> {
                body.physics.velocity.x = MegamanValues.DMG_Y * ConstVals.PPM
                body.physics.velocity.y =
                    (if (bounds.y > body.y) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
            }
        }

    /*
    fun stunBounce(bounceOriginX: Float) {
        body.physics.velocity.x =
            (if (bounceOriginX > body.x) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
        body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
    }
     */

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (canMove && !isBehaviorActive(BehaviorType.RIDING_CART) && !noDmgBounce.contains(damager::class) &&
            damager is GameEntity && damager.hasComponent(BodyComponent::class)
        ) {
            val enemyBody = damager.getComponent(BodyComponent::class)!!.body
            // stunBounce(enemyBody.x)
            stunBounce(enemyBody)
        }
        val damage = dmgNegotations.get(damager::class).get(damager)
        translateHealth(-damage)
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)
        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        damageTimer.reset()
        return true
    }

    override fun getBounds() = body.getBodyBounds()

    override fun getTag() = TAG
}
