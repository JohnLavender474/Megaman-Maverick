package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.megaman.components.defineDamageRecoveryComponent
import com.megaman.maverick.game.entities.megaman.components.defineKnockbackComponent
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.locks.behaviorsLockHook
import com.megaman.maverick.game.players.*
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MegamanX(services: MegaServices) : AbstractHealthEntity(services), IBodyEntity, ISpritesEntity, IAudioEntity,
    IEventListener, IFaceable, IDirectional, IDamagerEntity {

    companion object {
        const val TAG = "MegamanX"
    }

    override val damageNegotiator = PlayerDamageNegotiator(this)
    internal val damageRecovery = defineDamageRecoveryComponent()

    override val invincible: Boolean
        get() = playerInvincible
    override val damageTimer = Timer(MegamanValues.DAMAGE_DURATION).setToEnd()

    override val eventKeyMask = objectSetOf<Any>(
        EventType.BEGIN_ROOM_TRANS,
        EventType.CONTINUE_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.STUN_PLAYER,
        EventType.END_GAME_CAM_ROTATION
    )

    override lateinit var facing: Facing

    // TODO: X does not turn the camera when the world gravity changes; the X6 levels have no gravity change, and the
    //  camera turn moves to WorldGravity with item 058
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    internal val buster = MegamanXBuster(this)

    internal var airDashUsed = false
    internal val wallGrabTimer = Timer(MegamanXValues.WALL_GRAB_TIME)
    internal val kickLockTimer = Timer(MegamanXValues.WALL_KICK_LOCK_TIME).setToEnd()
    internal val pushOffTimer = Timer(MegamanXValues.PUSH_OFF_TIME).setToEnd()
    internal val wallDelayTimer = Timer(MegamanXValues.WALL_DELAY_AFTER_JUMP).setToEnd()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()

        addComponent(PlayPhaseComponent())
        // TODO: a teleporter does not stop X's charge, lock damage or stop the smooth follow, as it does for Megaman;
        //  no X level has a teleporter yet (item 081)
        addComponent(TeleportableComponent())
        addComponent(LocksComponent(MegaLockGroup.entries.size, behaviorsLockHook(this)))
        addComponent(defineBodyComponent())
        addComponent(defineFocusComponent())
        addComponent(defineProgressComponent())
        addComponent(RunComponent())
        addComponent(defineWeaponChargeComponent())
        addComponent(PostActionMomentumComponent(0f, 0f, { 0f }, { bodyOverGround }, { false }, { false }))
        addComponent(RoomTransitionCarryComponent(ConstVals.ROOM_TRANS_DELAY_DURATION, carries = { bodyOverGround }))
        addComponent(defineKnockbackComponent())
        addComponent(defineFreezeComponent())
        addComponent(defineStunComponent())
        addComponent(damageRecovery)
        addComponent(defineBehaviorsComponent())
        addComponent(defineControllerComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        setMaxHealthFromHeartTanks()
        super.onSpawn(spawnProps)

        playerSpawnStart(spawnProps)

        damageTimer.setToEnd()
        buster.reset()
        airDashUsed = false
        kickLockTimer.setToEnd()
        pushOffTimer.setToEnd()
        wallDelayTimer.setToEnd()
        require(PostActionMomentumComponent::class).reset()

        playerSpawnEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        playerOnDestroy()

        services.audio.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        ExplosionOrb.spawnRing(body.getCenter())
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
                playerOnRoomTransition(event)
                stopCharging()
            }

            EventType.END_ROOM_TRANS -> playerOnRoomTransition(event)

            EventType.GATE_INIT_OPENING -> stopCharging()

            EventType.STUN_PLAYER -> onStunPlayerEvent(event)

            EventType.END_GAME_CAM_ROTATION -> {
                unlock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
                body.physics.gravityOn = true
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        existenceState.liveAfterUpdate && super.canBeDamagedBy(damager) && playerAllowsDamageFrom(damager)

    override fun getDamageDuration(damager: IDamagerEntity) = MegamanValues.DAMAGE_DURATION

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!super.takeDamageFrom(damager)) return false

        playerAfterDamage(damager)

        stopCharging()
        requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND, false)

        return true
    }

    override fun editDamageFrom(damager: IDamagerEntity, baseDamage: Int) = playerEditDamage(baseDamage)

    override fun definePointsComponent(): PointsComponent {
        val component = super.definePointsComponent()
        component.getPoints(ConstKeys.HEALTH).max = MegamanValues.START_HEALTH
        return component
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            killPlayerIfOutsideLevel()

            require(StunComponent::class).update(delta, body.isSensing(BodySense.FEET_ON_GROUND))
            if (stunned || damaged) stopCharging()

            damageRecovery.update(delta, damaged, damageTimer.isJustFinished())

            require(RoomTransitionCarryComponent::class).update(delta)
            require(FreezeComponent::class).update(delta)

            buster.update(delta)

            kickLockTimer.update(delta)
            pushOffTimer.update(delta)
            wallDelayTimer.update(delta)
            if (bodyOverGround) airDashUsed = false
        }
    }

    // TODO: X shares Megaman's progress (heart tanks, health tanks, lives) for now; a profile for each character is
    //  in items 047 and 059
    private fun defineProgressComponent() = PlayerProgressComponent(
        onLevelDefeated = {},
        onHeartTanksChanged = { setMaxHealthFromHeartTanks() },
        onCurrencyAdded = {
            if (services.gameState.getCurrency() < services.gameState.getMaxCurrency())
                requestToPlaySound(SoundAsset.CURRENCY_PICKUP_SOUND, false)
        }
    )

    private fun setMaxHealthFromHeartTanks() {
        val heartTanks = MegaHeartTank.entries.count { services.gameState.containsHeartTank(it) }
        setMaxHealth(MegamanValues.START_HEALTH + heartTanks * MegaHeartTank.HEALTH_BUMP)
    }

    internal fun upSign() = when (direction) {
        Direction.UP, Direction.RIGHT -> 1f
        Direction.DOWN, Direction.LEFT -> -1f
    }

    override fun getType() = EntityType.MEGAMAN

    override fun getTag() = TAG
}
