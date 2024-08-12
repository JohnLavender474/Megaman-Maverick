package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.enums.Size
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IBoundsSupplier
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.*
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTankFist
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
import com.megaman.maverick.game.entities.megaman.extensions.stopCharging
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.projectiles.SniperJoeShield
import com.megaman.maverick.game.entities.special.Togglee
import com.megaman.maverick.game.entities.utils.setStandardOnPortalHopperContinueProp
import com.megaman.maverick.game.entities.utils.standardOnPortalHopperEnd
import com.megaman.maverick.game.entities.utils.standardOnPortalHopperStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.StunType
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensingAny
import kotlin.reflect.KClass

class Megaman(game: MegamanMaverickGame) : MegaGameEntity(game), IMegaUpgradable, IEventListener, IFaceable, IDamageable,
    IDirectionRotatable, IBodyEntity, IHealthEntity, ISpritesEntity, IBehaviorsEntity, IPointsEntity, IAudioEntity,
    IAnimatedEntity, IScalableGravityEntity, IBoundsSupplier {

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
        Bullet::class to dmgNeg(2),
        ChargedShot::class to dmgNeg(4),
        Bat::class to dmgNeg(2),
        Met::class to dmgNeg(2),
        DragonFly::class to dmgNeg(3),
        FloatingCan::class to dmgNeg(2),
        FlyBoy::class to dmgNeg(3),
        GapingFish::class to dmgNeg(2),
        SpringHead::class to dmgNeg(3),
        SuctionRoller::class to dmgNeg(2),
        MagFly::class to dmgNeg(3),
        Explosion::class to dmgNeg(2),
        JoeBall::class to dmgNeg(3),
        Snowball::class to dmgNeg(3),
        SnowballExplosion::class to dmgNeg(1),
        SwinginJoe::class to dmgNeg(2),
        SniperJoe::class to dmgNeg(3),
        ShieldAttacker::class to dmgNeg(4),
        Penguin::class to dmgNeg(3),
        Hanabiran::class to dmgNeg(3),
        Petal::class to dmgNeg(3),
        CaveRock::class to dmgNeg(3),
        CaveRocker::class to dmgNeg(3),
        CaveRockExplosion::class to dmgNeg(2),
        Elecn::class to dmgNeg(3),
        ElectricBall::class to dmgNeg(3),
        Ratton::class to dmgNeg(2),
        PicketJoe::class to dmgNeg(3),
        Picket::class to dmgNeg(3),
        LaserBeamer::class to dmgNeg(3),
        CartinJoe::class to dmgNeg(3),
        Bolt::class to dmgNeg(3),
        ElectrocutieChild::class to dmgNeg(3),
        Togglee::class to dmgNeg(3),
        Eyee::class to dmgNeg(3),
        Adamski::class to dmgNeg(3),
        UpNDown::class to dmgNeg(3),
        BigJumpingJoe::class to dmgNeg(6),
        SniperJoeShield::class to dmgNeg(2),
        SuicideBummer::class to dmgNeg(3),
        Gachappan::class to dmgNeg(5),
        ExplodingBall::class to dmgNeg(3),
        Imorm::class to dmgNeg(3),
        SpikeBall::class to dmgNeg(8),
        Peat::class to dmgNeg(2),
        BulbBlaster::class to dmgNeg(2),
        Bospider::class to dmgNeg(5),
        BabySpider::class to dmgNeg(2),
        GutsTankFist::class to dmgNeg(3),
        PurpleBlast::class to dmgNeg(3),
        HeliMet::class to dmgNeg(3),
        SigmaRat::class to dmgNeg(3),
        SigmaRatElectricBall::class to dmgNeg(3),
        SigmaRatElectricBallExplosion::class to dmgNeg(2),
        SigmaRatClaw::class to dmgNeg(2),
        Fireball::class to dmgNeg(3),
        BoulderProjectile::class to dmgNeg {
            it as BoulderProjectile
            when (it.size) {
                Size.LARGE -> 4
                Size.MEDIUM -> 2
                Size.SMALL -> 1
            }
        },
        PetitDevil::class to dmgNeg(3),
        PetitDevilChild::class to dmgNeg(2),
        Shotman::class to dmgNeg(2),
        Snowhead::class to dmgNeg(2),
        SnowheadThrower::class to dmgNeg(3),
        Spiky::class to dmgNeg(4),
        PenguinMiniBoss::class to dmgNeg(3),
        BabyPenguin::class to dmgNeg(2),
        UFOBomb::class to dmgNeg(3),
        UFOBombBot::class to dmgNeg(2),
        RollingBot::class to dmgNeg(3),
        RollingBotShot::class to dmgNeg(3),
        AcidGoop::class to dmgNeg(3),
        ToxicBarrelBot::class to dmgNeg(3),
        ToxicGoopShot::class to dmgNeg(3),
        ToxicGoopSplash::class to dmgNeg(3),
        ReactorMonkeyBall::class to dmgNeg(3),
        ReactorMonkeyMiniBoss::class to dmgNeg(3),
        SmokePuff::class to dmgNeg(2),
        TubeBeam::class to dmgNeg(5),
        ReactMan::class to dmgNeg(3),
        ReactManProjectile::class to dmgNeg(3),
        FlameThrower::class to dmgNeg(6),
        Popoheli::class to dmgNeg(3),
        BouncingAngryFlameBall::class to dmgNeg(3),
        LavaDrop::class to dmgNeg(6),
        PopupCanon::class to dmgNeg(3),
        Asteroid::class to dmgNeg(3),
        AsteroidExplosion::class to dmgNeg(3),
        MoonHeadMiniBoss::class to dmgNeg(3),
        BunbyRedRocket::class to dmgNeg(3),
        BunbyTank::class to dmgNeg(3),
        FireMet::class to dmgNeg(3),
        FireMetFlame::class to dmgNeg(3),
        Robbit::class to dmgNeg(3),
        Pipi::class to dmgNeg(3),
        PipiEgg::class to dmgNeg(3),
        Copipi::class to dmgNeg(3)
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
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER
    )

    override val upgradeHandler = MegamanUpgradeHandler(game.state, this)

    val weaponHandler = MegamanWeaponHandler(this)

    private val canChargeCurrentWeapon: Boolean
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

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            GameLogger.debug(TAG, "directionRotation: value = $value")

            forceQuitBehavior(BehaviorType.JETPACKING)

            body.cardinalRotation = value
            when (value) {
                Direction.UP, Direction.RIGHT, null -> {
                    jumpVel = MegamanValues.JUMP_VEL
                    wallJumpVel = MegamanValues.WALL_JUMP_VEL
                    cartJumpVel = MegamanValues.CART_JUMP_VEL

                    gravity = MegamanValues.GRAVITY
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
            GameLogger.debug(TAG, "facing: value = $value")
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
    internal var groundGravity = 0f
    internal var iceGravity = 0f
    internal var waterGravity = 0f
    internal var waterIceGravity = 0f
    internal var swimVel = 0f

    internal var applyMovementScalarToBullet = false

    override fun init() {
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

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.spawn(spawnProps)

        setHealth(getMaxHealth())
        weaponHandler.setAllToMaxAmmo()

        game.eventsMan.addListener(this)

        val bounds = properties.get(ConstKeys.BOUNDS) as GameRectangle
        body.positionOnPoint(bounds.getBottomCenterPoint(), Position.BOTTOM_CENTER)

        facing = Facing.RIGHT
        aButtonTask = AButtonTask.JUMP
        currentWeapon = MegamanWeapon.BUSTER
        directionRotation = Direction.UP
        running = false
        damageFlash = false

        damageTimer.setToEnd()
        damageRecoveryTimer.setToEnd()
        damageFlashTimer.reset()

        canMove = true
        canBeDamaged = true

        shootAnimTimer.reset()
        groundSlideTimer.reset()
        wallJumpTimer.reset()
        chargingTimer.reset()
        airDashTimer.reset()

        putProperty(ConstKeys.ON_PORTAL_HOPPER_START, {
            standardOnPortalHopperStart(this)
            stopCharging()
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) forceQuitBehavior(BehaviorType.AIR_DASHING)
            teleporting = true
        })
        setStandardOnPortalHopperContinueProp(this)
        putProperty(ConstKeys.ON_PORTAL_HOPPER_END, {
            standardOnPortalHopperEnd(this)
            stopCharging()
            aButtonTask = AButtonTask.AIR_DASH
            teleporting = false
        })

        teleporting = false
        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super<MegaGameEntity>.onDestroy()
        body.physics.velocity.setZero()

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
            explosionOrb?.let { orb ->
                game.engine.spawn(
                    orb, props(ConstKeys.TRAJECTORY to trajectory, ConstKeys.POSITION to body.getCenter())
                )
            }
        }
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                val position = event.properties.get(ConstKeys.POSITION) as Vector2
                GameLogger.debug(
                    MEGAMAN_EVENT_LISTENER_TAG, "BEGIN/CONTINUE ROOM TRANS: position = $position"
                )
                body.setCenter(position)
                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "GATE_INIT_OPENING")
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
                                BodySense.FEET_ON_GROUND, BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                                BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                            )
                        ) return

                        val stunOriginX = event.getProperty(ConstKeys.X, Float::class)!!
                        stunBounce(stunOriginX)
                        val stunDuration = event.getProperty(ConstKeys.DURATION, Float::class)!!
                        stunTimer.resetDuration(stunDuration)
                    }
                }
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && dmgNegotations.containsKey(damager::class) &&
                (damager is AbstractEnemy || damager is IHazard ||
                        (damager is AbstractProjectile && damager.owner != this))

    fun stunBounce(bounceOriginX: Float) {
        body.physics.velocity.x =
            (if (bounceOriginX > body.x) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
        body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
    }

    fun setToNextWeapon() {
        val index = currentWeapon.ordinal
        val nextIndex = (index + 1) % MegamanWeapon.values().size
        currentWeapon = MegamanWeapon.values()[nextIndex]
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (canMove && !isBehaviorActive(BehaviorType.RIDING_CART) &&
            !noDmgBounce.contains(damager::class) &&
            damager is IGameEntity &&
            damager.hasComponent(BodyComponent::class)
        ) {
            val enemyBody = damager.getComponent(BodyComponent::class)!!.body
            stunBounce(enemyBody.x)
        }
        val damage = dmgNegotations.get(damager::class).get(damager)
        translateHealth(-damage)
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)
        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        damageTimer.reset()
        return true
    }

    override fun getPosition() = when (directionRotation!!) {
        Direction.UP -> body.getBottomCenterPoint()
        Direction.DOWN -> body.getTopCenterPoint()
        Direction.LEFT -> body.getCenterRightPoint()
        Direction.RIGHT -> body.getCenterLeftPoint()
    }

    override fun getBounds() = body

    override fun getTag() = TAG
}
