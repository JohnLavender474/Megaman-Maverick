package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonActuator
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.entities.resetBehavior
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.*
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.megaman.behaviors.*
import com.megaman.maverick.game.entities.megaman.components.*
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.weapons.AxeThrowWindup
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.BusterCharge
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponWindup
import com.megaman.maverick.game.entities.megaman.handlers.MegamanJetpackHandler
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimations
import com.megaman.maverick.game.entities.megaman.sprites.getAnimationKey
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.entities.utils.standardOnTeleportEnd
import com.megaman.maverick.game.entities.utils.standardOnTeleportStart
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.players.PlayerWeaponsComponent
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.players.*
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class Megaman(services: MegaServices) : MegaPlayer(services), ISpritesEntity, IScalableGravityEntity,
    IDamagerEntity {

    companion object {
        const val TAG = "Megaman"
        const val MEGAMAN_EVENT_LISTENER_TAG = "MegamanEventListener"
    }

    val canMove: Boolean
        get() = playerCanMove


    internal val shootAnimTimer = Timer(MegamanValues.SHOOT_ANIM_TIME).setToEnd()
    internal val wallJumpTimer = Timer(MegamanValues.WALL_JUMP_IMPETUS_TIME).setToEnd()

    internal var airDashDirection: Direction? = null

    var currentAnimKey: String? = null
        internal set

    internal lateinit var weaponsHandler: MegamanWeaponsHandler
    internal lateinit var jetpackHandler: MegamanJetpackHandler

    override val character = PlayerCharacter.MEGAMAN

    override var shooting: Boolean
        get() = !shootAnimTimer.isFinished()
        set(value) {
            if (value) shootAnimTimer.reset() else shootAnimTimer.setToEnd()
        }

    override var direction: Direction
        get() = body.direction
        set(value) {
            GameLogger.debug(TAG, "direction-set(): value=$value")
            turnBodyAndCamera(value, onCameraTurnStart = { resetBehavior(BehaviorType.JETPACKING) })
            playerGravity.applyDirection(value)
            val sign = value.upSign()
            jumpVel = MegamanValues.JUMP_VEL * sign
            wallJumpVel = MegamanValues.WALL_JUMP_VEL * sign
            swimVel = MegamanValues.SWIM_VEL_Y * sign
        }

    private var weapon: WeaponDef<Megaman>? = null
    var currentWeapon: WeaponDef<Megaman>
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

    internal val busterCharge = BusterCharge(
        fullTime = MegamanValues.TIME_TO_FULLY_CHARGED,
        halfTime = MegamanValues.TIME_TO_HALFWAY_CHARGED,
        speedScalar = {
            when {
                services.gameState.containsEnhancement(MegaEnhancement.FASTER_BUSTER_CHARGING) -> MegaEnhancement.FASTER_BUSTER_CHARGING_SCALAR
                else -> 1f
            }
        },
        onHalfCharged = { requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, loop = false) },
        onFullyCharged = {},
        onCleared = { stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND) }
    )
    internal val axeWindup = AxeThrowWindup(MegamanValues.AXE_HOLD_TIME)

    internal var jumpVel = 0f
    internal var wallJumpVel = 0f
    internal val runParams = defineRunParams()
    internal val runBlocked: () -> Boolean = {
        currentWeapon.stopsRunWhileFiring && shooting && bodyOverGround
    }
    internal val onRunBlocked: (Facing) -> Unit = { runFacing ->
        if (currentWeapon.stopsRunWhileFiring && shooting) facing = runFacing
    }

    internal val playerGravity = PlayerGravityComponent(
        PlayerGravityValues(
            ground = MegamanValues.GROUND_GRAVITY,
            jump = MegamanValues.JUMP_GRAVITY,
            fall = MegamanValues.FALL_GRAVITY,
            ice = MegamanValues.ICE_GRAVITY,
            water = MegamanValues.WATER_GRAVITY,
            waterIce = MegamanValues.WATER_ICE_GRAVITY
        )
    )
    internal var swimVel = 0f

    internal var canMakeLandSound = false
    internal var applyMovementScalarToBullet = false

    internal val wallSlideNotAllowedTimer = Timer(MegamanValues.WALLSLIDE_NOT_ALLOWED_DELAY)

    override val cameraFocusHeight = MEGAMAN_BODY_HEIGHT

    override fun init(vararg params: Any) {
        super.init()

        addComponent(SandReactionComponent(SandReaction.SINK))
        addComponent(playerGravity)
        addComponent(defineBubbleEmitterComponent())
        addComponent(defineTrailSpriteComponent())

        weaponsHandler = MegamanWeaponsHandler(this)
        jetpackHandler = MegamanJetpackHandler(this)

        currentWeapon = MegamanWeapons.MEGA_BUSTER
        addComponent(defineWeaponsComponent())

        addComponent(PlayerAButtonComponent())
        addComponent(GravityDeviceComponent { aButtonTaskOnGravitySwitch() })
        addComponent(PlayerLightComponent {
            when {
                charging -> if (fullyCharged) PlayerLight.FULL_CHARGE else PlayerLight.HALF_CHARGE
                isBehaviorActive(BehaviorType.JETPACKING) -> PlayerLight.JETPACK
                else -> PlayerLight.STANDARD
            }
        })
    }

    private fun defineWeaponsComponent() = PlayerWeaponsComponent(
        loadout = MEGAMAN_LOADOUT,
        currentWeaponSupplier = { currentWeapon },
        selectWeapon = { currentWeapon = it },
        ammoOf = { weaponsHandler.getAmmo(it) },
        translateAmmoOf = { weapon, delta -> translateAmmo(delta, weapon) }
    )

    override fun teleportableComponent() = TeleportableComponent(
        onStart = { _: ITeleporterEntity ->
            stopWindup()
            standardOnTeleportStart(this)
            if (isBehaviorActive(BehaviorType.AIR_DASHING)) resetBehavior(BehaviorType.AIR_DASHING)
            lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
        },
        onContinue = { standardOnTeleportStart(this) },
        onEnd = {
            stopWindup()
            standardOnTeleportEnd(this)
            aButtonTask = AButtonTask.AIR_DASH
            unlock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
            services.cameraController.stopSmoothFollow()
        }
    )

    override fun onPlayerSpawn(spawnProps: Properties) {
        resetAButtonTask()

        weaponsHandler.setAllToMaxAmmo()
        currentWeapon = MegamanWeapons.MEGA_BUSTER

        canMakeLandSound = false

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)
        applyMovementScalarToBullet = spawnProps.getOrDefault(ConstKeys.APPLY_SCALAR_TO_CHILDREN, false, Boolean::class)

        shootAnimTimer.reset()
        wallJumpTimer.reset()

        wallSlideNotAllowedTimer.reset()
    }

    override fun onPlayerDestroy() {
        weaponsHandler.reset()

        services.audio.stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun onHealthDepleted() {
        super.onHealthDepleted()

        if (services.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL)
            services.audio.playSound(SoundAsset.SMB3_PLAYER_DOWN_SOUND, false)
    }

    override fun onPlayerEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS ->
                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)

            EventType.GATE_INIT_OPENING -> {
                GameLogger.debug(MEGAMAN_EVENT_LISTENER_TAG, "GATE_INIT_OPENING")

                stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
            }
        }
    }

    override fun afterPlayerDamage(damager: IDamagerEntity) {
        stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            killPlayerIfOutsideLevel()

            stopWindupNotOfCurrentWeapon()
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

    override fun defineBodyComponent(): BodyComponent {
        lateinit var weaponPreStep: () -> Unit
        val body = playerBody(
            this,
            PlayerBodyParams(
                width = MEGAMAN_BODY_WIDTH,
                height = MEGAMAN_BODY_HEIGHT,
                crouchHeight = GROUNDSLIDE_CROUCH_HEIGHT,
                gravity = playerGravity,
                waterGravityScalar = MegamanValues.WATER_GRAVITY_SCALAR,
                clampX = MegamanValues.CLAMP_X,
                clampY = MegamanValues.CLAMP_Y,
                onBounce = { wallSlideNotAllowedTimer.reset() },
                onFeetBounce = { setAirDashTaskOutOfWater() },
                onFeetBlockBegin = { makesSound ->
                    aButtonTask = AButtonTask.JUMP
                    if (makesSound && canMakeLandSound) {
                        requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                        canMakeLandSound = false
                    }
                },
                onFeetBlockContinue = { aButtonTask = AButtonTask.JUMP },
                onFeetBlockEnd = {
                    if (!bodyOverGround) aButtonTask = offGroundAButtonTask()
                    if (!body.hasAnyFeetBlock()) canMakeLandSound = true
                },
                onHeadHitBlock = {
                    if (services.levelState.currentLevel!!.type == LevelType.MARIO_LEVEL) {
                        val shouldMakeBumpSound = when (direction) {
                            Direction.UP -> body.physics.velocity.y >= 0f
                            Direction.DOWN -> body.physics.velocity.y <= 0f
                            Direction.LEFT -> body.physics.velocity.x <= 0f
                            Direction.RIGHT -> body.physics.velocity.x >= 0f
                        }
                        if (shouldMakeBumpSound) requestToPlaySound(SoundAsset.SMB3_BUMP_SOUND, false)
                    }
                },
                sideSticksTo = { blockFixture ->
                    val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
                    owner !is GutsTank
                },
                onWater = { state -> aButtonTaskOnWater(state) },
                onPreStep = { weaponPreStep() }
            )
        )

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getProperty(ConstKeys.DAMAGEABLE) as Fixture }

        val needleSpinDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        body.addFixture(needleSpinDamagerFixture)
        needleSpinDamagerFixture.drawingColor = Color.ORANGE

        val needleSpinShieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        body.addFixture(needleSpinShieldFixture)
        needleSpinShieldFixture.drawingColor = Color.PURPLE

        val axeShieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setHeight(MEGAMAN_BODY_HEIGHT * ConstVals.PPM)
        )
        axeShieldFixture.putProperty(ConstKeys.AXE, true)
        body.addFixture(axeShieldFixture)
        axeShieldFixture.drawingColor = Color.GREEN
        debugShapes.add { if (axeShieldFixture.isActive()) axeShieldFixture else null }

        weaponPreStep = {
            val needleSpinning = currentWeapon.spinFixturesWhileFiring && shooting
            needleSpinDamagerFixture.setActive(needleSpinning)
            needleSpinShieldFixture.setActive(needleSpinning)

            axeShieldFixture.setActive(currentWeapon.axeShield)
            val axeShieldWidth = if (isBehaviorActive(BehaviorType.CLIMBING)) 0.75f else 0.5f
            (axeShieldFixture.rawShape as GameRectangle).setWidth(axeShieldWidth * ConstVals.PPM)
            val axeShieldOffsetX = when {
                isBehaviorActive(BehaviorType.CLIMBING) -> 0f
                slipSliding -> 0.5f
                else -> 0.75f
            }
            axeShieldFixture.offsetFromBodyAttachment.x = axeShieldOffsetX * ConstVals.PPM * facing.value
            val axeShieldReflectDir = when (direction) {
                Direction.UP -> if (isFacing(Facing.LEFT)) Direction.LEFT else Direction.RIGHT
                Direction.DOWN -> if (isFacing(Facing.LEFT)) Direction.RIGHT else Direction.LEFT
                Direction.LEFT -> if (isFacing(Facing.LEFT)) Direction.DOWN else Direction.UP
                Direction.RIGHT -> if (isFacing(Facing.LEFT)) Direction.UP else Direction.DOWN
            }
            axeShieldFixture.putProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", axeShieldReflectDir)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineMomentumComponent() = PostActionMomentumComponent(
        MegamanValues.POST_ACTION_MOMENTUM_DUR,
        MegamanValues.POST_ACTION_BURST_RESISTANCE,
        maxSpeed = { getMaxRunSpeed() },
        overGround = { bodyOverGround },
        facingBlock = { FacingUtils.isFacingBlock(this) },
        groundSliding = { isBehaviorActive(BehaviorType.GROUND_SLIDING) }
    )

    override fun defineKnockbackComponent() = KnockbackComponent(
        MegamanValues.DMG_X * ConstVals.PPM,
        MegamanValues.DMG_Y * ConstVals.PPM,
        objectSetOf(SpringHead::class)
    )

    override fun defineStunComponent() = StunComponent(
        MegamanValues.STUN_DUR,
        onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) || isBehaviorActive(BehaviorType.WALL_SLIDING) },
        onStun = {
            // TODO: This assumes that Megaman's body is rotated UP.
            //    Refactor this to support directionally dynamic bouncing.
            body.physics.velocity.let {
                it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * movementScalar * -facing.value
                it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
            }
        }
    )

    // TODO: the behavior factories check weapons inside their bodies (NEEDLE_SPIN, RUSH_JET, RODENT_CLAWS); these
    //  checks belong to the loadout and move with item 024
    override fun defineBehaviorsComponent(): BehaviorsComponent {
        val behaviorsComponent = BehaviorsComponent()

        behaviorsComponent.addBehavior(BehaviorType.WALL_SLIDING, wallSlideBehavior())
        behaviorsComponent.addBehavior(BehaviorType.SWIMMING, swimBehavior())
        behaviorsComponent.addBehavior(BehaviorType.JUMPING, jumpBehavior())
        behaviorsComponent.addBehavior(BehaviorType.AIR_DASHING, airDashBehavior())
        behaviorsComponent.addBehavior(BehaviorType.GROUND_SLIDING, groundSlideBehavior())
        behaviorsComponent.addBehavior(BehaviorType.CROUCHING, crouchBehavior())
        behaviorsComponent.addBehavior(BehaviorType.CLIMBING, climbBehavior())
        behaviorsComponent.addBehavior(BehaviorType.JETPACKING, jetpackBehavior())

        return behaviorsComponent
    }

    override fun defineControllerComponent(): ControllerComponent {
        val left = playerRunButton(this, Facing.LEFT, runParams, runBlocked, onRunBlocked)
        val right = playerRunButton(this, Facing.RIGHT, runParams, runBlocked, onRunBlocked)

        val attack = ButtonActuator(
            onPressContinued = { delta ->
                val windup = windupOf(currentWeapon)
                if (windup === axeWindup && (services.levelState.roomTransition || services.gameCamera.rotating))
                    return@ButtonActuator

                if (!inPlay || stunned || damaged || teleporting || currentWeapon.stopsChargeOnHold ||
                    !canFireAfterWindupOf(windup)
                ) {
                    stopWindup()
                    return@ButtonActuator
                }

                if (windup == null) return@ButtonActuator
                windups.hold(windup, delta)
            },
            onJustReleased = {
                if (stunned || damaged || services.gameCamera.rotating || teleporting || !inPlay ||
                    !weaponsHandler.canFireWeapon(currentWeapon, chargeStatus) ||
                    services.levelState.roomTransition
                ) {
                    GameLogger.debug(
                        MEGAMAN_CONTROLLER_COMPONENT_TAG,
                        "attack actuator just released, do not shoot: " +
                            "stunned=$stunned, damaged=$damaged, cameraRotating=${services.gameCamera.rotating}, " +
                            "teleporting=$teleporting, inPlay=$inPlay, " +
                            "canFireWeapon=${weaponsHandler.canFireWeapon(currentWeapon, chargeStatus)}"
                    )

                    stopWindup()

                    return@ButtonActuator
                }

                GameLogger.debug(MEGAMAN_CONTROLLER_COMPONENT_TAG, "attack actuator just released, shoot")

                shoot()

                stopWindup()
            }
        )

        val select = ButtonActuator(onJustReleased = {
            when (services.players.settings(slot).selectButtonAction) {
                SelectButtonAction.NEXT_WEAPON -> {
                    if (!inPlay || damaged || teleporting) {
                        GameLogger.debug(
                            MEGAMAN_CONTROLLER_COMPONENT_TAG,
                            "select actuator just released, do nothing: inPlay=$inPlay, damaged=$damaged, teleporting=$teleporting"
                        )
                        return@ButtonActuator
                    }

                    GameLogger.debug(
                        MEGAMAN_CONTROLLER_COMPONENT_TAG,
                        "select actuator just released, attempt to set to next weapon"
                    )

                    setToNextWeapon()
                }

                else -> {}
            }
        })

        return ControllerComponent(
            MegaControllerButton.B pairTo attack,
            MegaControllerButton.LEFT pairTo left,
            MegaControllerButton.RIGHT pairTo right,
            MegaControllerButton.SELECT pairTo select
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val component = SpritesComponent()
        defineMegamanSprite(component)
        defineJetpackFlameSprite(component)
        defineDamagedBurstSprite(component)
        defineDesertTornatoSprite(component)
        return component
    }

    override fun defineAnimationsComponent(): AnimationsComponent {
        val animations = MegamanAnimations(services).get()

        val megamanAnimKeySupplier: (String?) -> String? = supplier@{
            val key = getAnimationKey(currentAnimKey)

            if (key == "jump_shoot_down") GameLogger.debug(
                "${TAG}_${ConstKeys.SHOOT}_${ConstKeys.DOWN}",
                "megamanAnimKeySupplier(): key = jump_shoot_down"
            )

            // if (key != null) game.setDebugText(key) else game.setDebugText("")

            return@supplier when {
                key != null -> {
                    currentAnimKey = key
                    MegamanAnimations.buildFullKey(key, currentWeapon)
                }
                else -> null
            }
        }
        val megamanAnimator = Animator(
            keySupplier = megamanAnimKeySupplier,
            animations = animations,
            onChangeKey = { _, currentKey, nextKey -> onChangeAnimationKey(currentKey, nextKey, animations) },
            shouldUpdateAnimationPredicate = { key, animation, _ -> shouldUpdateAnimation(key, animation) },
            postProcessKey = { _, currentKey, nextKey -> postProcessAnimationKey(currentKey, nextKey) },
            shouldEqualKeysTriggerChange = this::shouldEqualAnimKeysTriggerChange
        )

        val decorationsAtlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_1.source)

        val jetpackFlameRegion = decorationsAtlas.findRegion(JETPACK_FLAME_SPRITE_KEY)
        val jetpackFlameAnimation = Animation(jetpackFlameRegion, 1, 3, 0.1f)
        val jetpackFlameAnimator = Animator(jetpackFlameAnimation)

        val desertTornadoRegion = decorationsAtlas.findRegion(DESERT_TORNADO_SPRITE_KEY)
        val desertTornadoAnimation = Animation(desertTornadoRegion, 2, 1, 0.1f)
        val desertTornadoAnimator = Animator(desertTornadoAnimation)

        val animators = orderedMapOf<Any, IAnimator>(
            MEGAMAN_SPRITE_KEY pairTo megamanAnimator,
            JETPACK_FLAME_SPRITE_KEY pairTo jetpackFlameAnimator,
            DESERT_TORNADO_SPRITE_KEY pairTo desertTornadoAnimator
        )

        return AnimationsComponent(animators, sprites)
    }

    internal fun windupOf(weapon: WeaponDef<Megaman>): WeaponWindup? = when (weapon) {
        MegamanWeapons.MEGA_BUSTER -> busterCharge
        MegamanWeapons.AXE_SWINGER -> axeWindup
        else -> null
    }

    private fun canFireAfterWindupOf(windup: WeaponWindup?) = when {
        windup === axeWindup -> weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.NOT_CHARGED)
        !charging -> weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.HALF_CHARGED)
        else -> weaponsHandler.canFireWeapon(currentWeapon, MegaChargeStatus.FULLY_CHARGED)
    }

    fun translateAmmo(ammo: Int, weapon: WeaponDef<Megaman> = currentWeapon): Int {
        val remaining = weaponsHandler.translateAmmo(weapon, ammo)
        GameLogger.debug(TAG, "translateAmmo(): weapon=$weapon, ammo=$ammo, remaining=$remaining")
        return remaining
    }

    fun shoot(): Boolean {
        val shot = weaponsHandler.fireWeapon(currentWeapon, chargeStatus)
        if (shot) shootAnimTimer.reset()
        return shot
    }
}
