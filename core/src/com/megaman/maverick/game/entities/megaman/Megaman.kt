package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
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
import com.engine.entities.GameEntity
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
import com.megaman.maverick.game.entities.bosses.Bospider
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.enemies.*
import com.megaman.maverick.game.entities.explosions.CaveRockExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SnowballExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.hazards.Bolt
import com.megaman.maverick.game.entities.hazards.ElectrocutieChild
import com.megaman.maverick.game.entities.hazards.LaserBeamer
import com.megaman.maverick.game.entities.hazards.SpikeBall
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.projectiles.SniperJoeShield
import com.megaman.maverick.game.entities.utils.setStandardOnPortalHopperContinueProp
import com.megaman.maverick.game.entities.utils.standardOnPortalHopperEnd
import com.megaman.maverick.game.entities.utils.standardOnPortalHopperStart
import com.megaman.maverick.game.entities.utils.stopSoundNow
import com.megaman.maverick.game.events.EventType
import kotlin.reflect.KClass

class Megaman(game: MegamanMaverickGame) : GameEntity(game), IMegaUpgradable, IEventListener, IFaceable, IDamageable,
    IDirectionRotatable, IBodyEntity, IHealthEntity, ISpriteEntity, IBehaviorsEntity, IPointsEntity, IAudioEntity,
    IBoundsSupplier {

    companion object {
        const val TAG = "Megaman"
        const val MEGAMAN_EVENT_LISTENER_TAG = "MegamanEventListener"
    }

    val damaged: Boolean
        get() = !damageTimer.isFinished()

    override val invincible: Boolean
        get() = damaged || !damageRecoveryTimer.isFinished() || !canBeDamaged

    var canBeDamaged = true
    var canMove = true

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
        GachappanBall::class to dmgNeg(3),
        Imorm::class to dmgNeg(3),
        SpikeBall::class to dmgNeg(8),
        Peat::class to dmgNeg(2),
        BulbBlaster::class to dmgNeg(2),
        Bospider::class to dmgNeg(5),
        BabySpider::class to dmgNeg(2),
        GutsTankFist::class to dmgNeg(3),
        PurpleBlast::class to dmgNeg(3),
        HeliMet::class to dmgNeg(3)
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
        EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS, EventType.GATE_INIT_OPENING
    )

    override val upgradeHandler = MegamanUpgradeHandler(this)

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
            firstSprite!!.hidden = !field
        }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            GameLogger.debug(TAG, "directionRotation: value = $value")
            body.cardinalRotation = value
            when (value) {
                Direction.UP, Direction.RIGHT -> { // jump
                    jumpVel = MegamanValues.JUMP_VEL
                    wallJumpVel = MegamanValues.WALL_JUMP_VEL
                    waterJumpVel = MegamanValues.WATER_JUMP_VEL
                    waterWallJumpVel = MegamanValues.WATER_WALL_JUMP_VEL
                    cartJumpVel = MegamanValues.CART_JUMP_VEL

                    // gravity
                    gravity = MegamanValues.GRAVITY
                    groundGravity = MegamanValues.GROUND_GRAVITY
                    iceGravity = MegamanValues.ICE_GRAVITY
                    waterGravity = MegamanValues.WATER_GRAVITY
                    waterIceGravity = MegamanValues.WATER_ICE_GRAVITY

                    // swim
                    swimVel = MegamanValues.SWIM_VEL_Y
                }

                Direction.DOWN, Direction.LEFT -> { // jump
                    jumpVel = -MegamanValues.JUMP_VEL
                    wallJumpVel = -MegamanValues.WALL_JUMP_VEL
                    waterJumpVel = -MegamanValues.WATER_JUMP_VEL
                    waterWallJumpVel = -MegamanValues.WATER_WALL_JUMP_VEL
                    cartJumpVel = -MegamanValues.CART_JUMP_VEL

                    // gravity
                    gravity = -MegamanValues.GRAVITY
                    groundGravity = -MegamanValues.GROUND_GRAVITY
                    iceGravity = -MegamanValues.ICE_GRAVITY
                    waterGravity = -MegamanValues.WATER_GRAVITY
                    waterIceGravity = -MegamanValues.WATER_ICE_GRAVITY

                    // swim
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

    internal var jumpVel = 0f
    internal var wallJumpVel = 0f
    internal var waterJumpVel = 0f
    internal var cartJumpVel = 0f
    internal var waterWallJumpVel = 0f
    internal var gravity = 0f
    internal var groundGravity = 0f
    internal var iceGravity = 0f
    internal var waterGravity = 0f
    internal var waterIceGravity = 0f
    internal var swimVel = 0f

    override fun init() {
        addComponent(AudioComponent(this))
        addComponent(defineUpdatablesComponent())
        addComponent(definePointsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        weaponHandler.putWeapon(MegamanWeapon.BUSTER)
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        super.spawn(spawnProps)
        setHealth(getMaxHealth())
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
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) forceQuitBehavior(BehaviorType.AIR_DASHING)
            teleporting = true
        })
        setStandardOnPortalHopperContinueProp(this)
        putProperty(ConstKeys.ON_PORTAL_HOPPER_END, {
            standardOnPortalHopperEnd(this)
            aButtonTask = AButtonTask.AIR_DASH
            teleporting = false
        })

        teleporting = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super<GameEntity>.onDestroy()
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
                game.gameEngine.spawn(
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
        }
    }

    override fun canBeDamagedBy(damager: IDamager) =
        !invincible && dmgNegotations.containsKey(damager::class) && (damager is AbstractEnemy || damager is IHazard || (damager is IProjectileEntity && damager.owner != this))

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (canMove && !isBehaviorActive(BehaviorType.RIDING_CART) && !noDmgBounce.contains(damager::class) && damager is IGameEntity && damager.hasComponent(
                BodyComponent::class
            )
        ) {
            val enemyBody = damager.getComponent(BodyComponent::class)!!.body
            body.physics.velocity.x =
                (if (enemyBody.x > body.x) -MegamanValues.DMG_X else MegamanValues.DMG_X) * ConstVals.PPM
            body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
        }
        val damage = dmgNegotations.get(damager::class).get(damager)
        damageTimer.reset()
        addHealth(-damage)
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)
        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
        return true
    }

    override fun getPosition() = when (directionRotation) {
        Direction.UP -> body.getBottomCenterPoint()
        Direction.DOWN -> body.getTopCenterPoint()
        Direction.LEFT -> body.getCenterRightPoint()
        Direction.RIGHT -> body.getCenterLeftPoint()
    }

    override fun getBounds() = body

    override fun getTag() = TAG
}
