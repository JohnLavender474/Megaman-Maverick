package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.locks.clearLocks
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.RoomTransitionEventKeys
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.locks.behaviorsLockHook
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

abstract class MegaPlayer(services: MegaServices) : AbstractHealthEntity(services), IPlayer, IBodyEntity, IFaceable,
    IDirectional, IEventListener, IAudioEntity {

    private val logTag: String
        get() = this::class.simpleName!!

    final override val damageNegotiator = PlayerDamageNegotiator(this)

    // TODO: shared player values come from MegamanValues until a character needs other values (item 060)
    internal val damageRecovery =
        DamageRecoveryComponent(MegamanValues.DAMAGE_RECOVERY_TIME, MegamanValues.DAMAGE_FLASH_DURATION)

    final override val invincible: Boolean
        get() {
            val recovery = require(DamageRecoveryComponent::class)
            val damageLocks = require(LocksComponent::class).getLockGroup(MegaLockGroup.DAMAGE)
            val playPhase = require(PlayPhaseComponent::class)
            return damaged || recovery.recovering || damageLocks.isLocked() || !existenceState.liveAfterUpdate ||
                playPhase.phase != PlayPhase.IN_PLAY
        }
    final override val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()

    final override val eventKeyMask = objectSetOf<Any>(
        EventType.BEGIN_ROOM_TRANS,
        EventType.CONTINUE_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER,
        EventType.END_GAME_CAM_ROTATION
    )

    final override lateinit var facing: Facing

    abstract val character: PlayerCharacter

    abstract val shooting: Boolean

    protected abstract val cameraFocusHeight: Float

    internal abstract fun defineBodyComponent(): BodyComponent

    internal abstract fun defineMomentumComponent(): PostActionMomentumComponent

    internal abstract fun defineKnockbackComponent(): KnockbackComponent

    internal abstract fun defineStunComponent(): StunComponent

    internal abstract fun defineBehaviorsComponent(): BehaviorsComponent

    internal abstract fun defineControllerComponent(): ControllerComponent

    internal abstract fun defineSpritesComponent(): SpritesComponent

    internal abstract fun defineAnimationsComponent(): AnimationsComponent

    protected open fun teleportableComponent() = TeleportableComponent()

    protected open fun onPlayerSpawn(spawnProps: Properties) {}

    protected open fun onPlayerDestroy() {}

    protected open fun onPlayerEvent(event: Event) {}

    protected open fun afterPlayerDamage(damager: IDamagerEntity) {}

    override fun init(vararg params: Any) {
        GameLogger.debug(logTag, "init()")
        super.init()

        addComponent(PlayPhaseComponent())
        addComponent(teleportableComponent())
        addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(this)))
        addComponent(defineBodyComponent())
        addComponent(defineCameraSubjectComponent())
        addComponent(definePlayerProgressComponent())
        addComponent(RunComponent())
        addComponent(defineMomentumComponent())
        addComponent(
            RoomTransitionCarryComponent(
                ConstVals.ROOM_TRANS_DELAY_DURATION,
                carries = { !isBehaviorActive(BehaviorType.CLIMBING) && bodyOverGround }
            )
        )
        addComponent(defineKnockbackComponent())
        addComponent(defineFreezeComponent())
        addComponent(defineStunComponent())
        addComponent(damageRecovery)
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(WeaponWindupComponent())
        addComponent(AudioComponent())
        addComponent(defineAnimationsComponent())
    }

    final override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(logTag, "onSpawn(): spawnProps=$spawnProps")
        setMaxHealthFromHeartTanks()
        super.onSpawn(spawnProps)

        playerSpawnStart(spawnProps)

        damageTimer.setToEnd()

        onPlayerSpawn(spawnProps)

        // TODO: a behavior that ends in the component reset pass can write momentum after the momentum component reset, so clear it
        //  here too; a reset order for components would remove this line (see the 084c docs/NOTES.md)
        require(PostActionMomentumComponent::class).reset()

        playerSpawnEnd()
    }

    final override fun onDestroy() {
        GameLogger.debug(logTag, "onDestroy()")
        super.onDestroy()

        playerOnDestroy()

        onPlayerDestroy()
    }

    override fun onHealthDepleted() {
        GameLogger.debug(logTag, "onHealthDepleted()")
        super.onHealthDepleted()

        ExplosionOrb.spawnRing(body.getCenter())
    }

    final override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS, EventType.END_ROOM_TRANS ->
                playerOnRoomTransition(event)

            EventType.STUN_PLAYER -> onStunPlayerEvent(event)

            EventType.END_GAME_CAM_ROTATION -> {
                unlock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)

                body.physics.gravityOn = true
            }
        }

        onPlayerEvent(event)
    }

    final override fun canBeDamagedBy(damager: IDamagerEntity) =
        existenceState.liveAfterUpdate && super.canBeDamagedBy(damager) && playerAllowsDamageFrom(damager, services.rules)

    final override fun getDamageDuration(damager: IDamagerEntity) = MegamanValues.DAMAGE_DURATION

    final override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val oldHealth = getCurrentHealth()

        if (!super.takeDamageFrom(damager)) return false

        val healthDiff = oldHealth - getCurrentHealth()
        GameLogger.debug(logTag, "takeDamageFrom(): healthDiff=$healthDiff, damager=$damager")

        playerAfterDamage(damager)

        afterPlayerDamage(damager)

        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)

        return true
    }

    final override fun editDamageFrom(damager: IDamagerEntity, baseDamage: Int) = playerEditDamage(baseDamage)

    final override fun definePointsComponent(): PointsComponent {
        val component = super.definePointsComponent()

        val points = component.getPoints(ConstKeys.HEALTH)
        points.max = MegamanValues.START_HEALTH

        return component
    }

    internal fun defineCameraSubjectComponent() = CameraSubjectComponent(
        bounds = { body.getBounds() },
        position = {
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            val focus = body.getBounds().getPositionPoint(position)
            val offset = cameraFocusHeight * ConstVals.PPM / 2f
            when (direction) {
                Direction.UP -> focus.y += offset
                Direction.DOWN -> focus.y -= offset
                Direction.LEFT -> focus.x -= offset
                Direction.RIGHT -> focus.x += offset
            }
            focus
        }
    )

    // TODO: X shares Megaman's progress (heart tanks, health tanks, lives) for now; a profile for each character is
    //  in items 047 and 059
    internal fun definePlayerProgressComponent() = PlayerProgressComponent(
        onHeartTanksChanged = { setMaxHealthFromHeartTanks() },
        onCurrencyAdded = {
            if (services.gameState.getCurrency() < services.gameState.getMaxCurrency())
                requestToPlaySound(SoundAsset.CURRENCY_PICKUP_SOUND, false)
        }
    )

    // TODO: the max health is a stored copy that is set when a heart tank changes; a max that is read through from
    //  the game state on each access needs a change to the engine Points (backlog candidate "max health read through
    //  from the profile", item 029)
    private fun setMaxHealthFromHeartTanks() {
        val heartTanks = MegaHeartTank.entries.count { services.gameState.containsHeartTank(it) }
        setMaxHealth(MegamanValues.START_HEALTH + heartTanks * MegaHeartTank.HEALTH_BUMP)
    }

    internal fun defineFreezeComponent() = FreezeComponent(
        MashThawRule(
            MegamanValues.HITS_TO_UNFREEZE,
            MegamanValues.FROZEN_PUSH_DUR,
            mashed = { MegamanValues.BUTTONS_TO_UNFREEZE.any { input.isJustReleased(it) } },
            onMash = { requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false) }
        ),
        onThaw = { IceShard.spawn5(body.getCenter()) }
    )

    private fun playerSpawnStart(spawnProps: Properties) {
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

        setHealth(getMaxHealth())

        clearLocks(MegaLockGroup.MOVEMENT)
        require(TeleportableComponent::class).teleporter = null
        clearLocks(MegaLockGroup.DAMAGE)
    }

    private fun playerSpawnEnd() {
        services.cameraController.stopSmoothFollow()
    }

    // TODO: this event fires on each destroy of a player, also when it is not a death (item 119)
    private fun playerOnDestroy() {
        val events = services.events
        events.removeListener(this)
        events.submitEvent(Event(EventType.PLAYER_JUST_DIED, props(PlayerEventKeys.SLOT pairTo slot)))
    }

    private fun playerOnRoomTransition(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                val position = event.getProperty(ConstKeys.POSITION, Vector2::class)!!
                if (event.key == EventType.BEGIN_ROOM_TRANS)
                    GameLogger.debug(logTag, "playerOnRoomTransition(): begin: position=$position")
                // TODO: a vertical axis-only pan sets only y; phase 3 of the shared screen adds its caller (item 047)
                val axisOnly = event.getProperty(RoomTransitionEventKeys.AXIS_ONLY)!!
                if (axisOnly) body.setCenter(position.x, body.getCenter().y) else body.setCenter(position)

                if (event.key == EventType.BEGIN_ROOM_TRANS) beginRoomTransitionCarry()

                body.physics.gravityOn = false

                stopMomentum(true)
            }

            EventType.END_ROOM_TRANS -> {
                GameLogger.debug(logTag, "playerOnRoomTransition(): end")

                endRoomTransitionCarry()

                body.physics.gravityOn = !isBehaviorActive(BehaviorType.CLIMBING)

                stopMomentum(true)
            }

            else -> throw IllegalArgumentException("${event.key} is not a room transition event")
        }
    }

    private fun playerAfterDamage(damager: IDamagerEntity) {
        knockBackFrom(damager)
        freezeFrom(damager)
        players.onPlayerDamaged(this, damager)
    }

    // TODO: the difficulty and the enhancement change the damage here; item 023 moves these rules
    private fun playerEditDamage(baseDamage: Int): Int {
        var damage = when {
            services.gameState.containsEnhancement(MegaEnhancement.DAMAGE_INCREASE) -> MegaEnhancement.scaleDamage(
                baseDamage, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR
            )
            else -> baseDamage
        }
        if (services.gameState.getDifficultyMode() == DifficultyMode.HARD) damage += 1
        return damage
    }

    final override fun getType() = EntityType.MEGAMAN
}
