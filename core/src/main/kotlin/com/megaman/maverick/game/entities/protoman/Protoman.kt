package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.movement.RunParams
import com.megaman.maverick.game.entities.movement.maxRunSpeed
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.players.*
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Protoman(services: MegaServices) : MegaPlayer(services), ISpritesEntity, IScalableGravityEntity {

    companion object {
        const val TAG = "Protoman"
    }

    // TODO: Protoman does not turn the camera when the world gravity changes, as Megaman does; the camera turn moves
    //  to WorldGravity with item 058
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
            playerGravity.applyDirection(value)
        }

    override var gravityScalar = 1f

    override val cameraFocusHeight = ProtomanValues.BODY_HEIGHT

    internal val playerGravity = PlayerGravityComponent(
        PlayerGravityValues(
            ground = ProtomanValues.GROUND_GRAVITY,
            jump = ProtomanValues.JUMP_GRAVITY,
            fall = ProtomanValues.FALL_GRAVITY,
            ice = ProtomanValues.ICE_GRAVITY,
            water = ProtomanValues.WATER_GRAVITY,
            waterIce = ProtomanValues.WATER_ICE_GRAVITY
        )
    )

    internal val runParams = RunParams(
        impulse = ProtomanValues.RUN_IMPULSE,
        iceImpulse = ProtomanValues.ICE_RUN_IMPULSE,
        ramp1Time = ProtomanValues.RUN_1_TIME,
        ramp1Scalar = ProtomanValues.RUN_1_SCALAR,
        ramp2Time = ProtomanValues.RUN_2_TIME,
        ramp2Scalar = ProtomanValues.RUN_2_SCALAR,
        ramp3Scalar = ProtomanValues.RUN_3_SCALAR,
        maxSpeed = { maxRunSpeed(body, ProtomanValues.RUN_MAX_SPEED, ProtomanValues.WATER_RUN_MAX_SPEED_SCALAR) },
        movementScalar = { 1f }
    )

    private var canMakeLandSound = false

    override fun init(vararg params: Any) {
        super.init()
        addComponent(playerGravity)
    }

    override fun onPlayerSpawn(spawnProps: Properties) {
        gravityScalar = 1f
        canMakeLandSound = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        // TODO: share the updatables list with Megaman and X when one frame order fits all classes (item 060)
        updatablesComponent.add { delta ->
            killPlayerIfOutsideLevel()
            require(StunComponent::class).update(delta, body.isSensing(BodySense.FEET_ON_GROUND))
            damageRecovery.update(delta, damaged, damageTimer.isJustFinished())
            require(RoomTransitionCarryComponent::class).update(delta)
            updatePostActionMomentum(delta, running)
            require(FreezeComponent::class).update(delta)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = playerBody(
            this,
            PlayerBodyParams(
                width = ProtomanValues.BODY_WIDTH,
                height = ProtomanValues.BODY_HEIGHT,
                crouchHeight = ProtomanValues.BODY_HEIGHT,
                gravity = playerGravity,
                waterGravityScalar = ProtomanValues.WATER_GRAVITY_SCALAR,
                clampX = ProtomanValues.CLAMP_X,
                clampY = ProtomanValues.CLAMP_Y,
                onBounce = {},
                onFeetBounce = {},
                // TODO: share the land sound with Megaman when a third player class needs it (item 060)
                onFeetBlockBegin = { makesSound ->
                    if (makesSound && canMakeLandSound) {
                        requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                        canMakeLandSound = false
                    }
                },
                onFeetBlockContinue = {},
                onFeetBlockEnd = { if (!body.hasAnyFeetBlock()) canMakeLandSound = true },
                onHeadHitBlock = {},
                // TODO: share the GutsTank side-stick rule with Megaman when a third player class needs it (item 060)
                sideSticksTo = { blockFixture ->
                    val owner = blockFixture.getEntity().getProperty(ConstKeys.OWNER) as IGameEntity?
                    owner !is GutsTank
                },
                // TODO: no swim; A in mid-water does nothing; decided in 060g
                onWater = {},
                onPreStep = {}
            )
        )
        return BodyComponentCreator.create(this, body)
    }

    override fun defineMomentumComponent() = PostActionMomentumComponent(
        ProtomanValues.POST_ACTION_MOMENTUM_DUR,
        ProtomanValues.POST_ACTION_BURST_RESISTANCE,
        maxSpeed = { runParams.maxSpeed() },
        overGround = { bodyOverGround },
        facingBlock = { FacingUtils.isFacingBlock(this) },
        groundSliding = { false }
    )

    // TODO: Megaman's knockback and stun values until Protoman's own damage reaction (item 060e)
    override fun defineKnockbackComponent() = KnockbackComponent(
        MegamanValues.DMG_X * ConstVals.PPM,
        MegamanValues.DMG_Y * ConstVals.PPM,
        objectSetOf(SpringHead::class)
    )

    override fun defineStunComponent() = StunComponent(
        MegamanValues.STUN_DUR,
        onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) },
        onStun = {
            // TODO: the stun bounce assumes that the body points up, as Megaman's does (item 058)
            body.physics.velocity.let {
                it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * -facing.value
                it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
            }
        }
    )

    override fun defineBehaviorsComponent(): BehaviorsComponent {
        val component = BehaviorsComponent()
        component.addBehavior(
            BehaviorType.JUMPING, playerJumpBehavior(
                this,
                JumpParams(
                    canStart = { true },
                    blocked = { false },
                    jumpSpeed = { ProtomanValues.JUMP_VEL * direction.upSign() },
                    wallJumpSpeed = { 0f },
                    wallJumpHorizontal = 0f,
                    wallJumpAllowed = { false },
                    ignoreRelease = { false },
                    startPressed = { input.isJustPressed(MegaControllerButton.A) },
                    // TODO: DOWN + A jumps; the slide on DOWN + A comes with branch 060d
                    downBlocksGroundJump = false,
                    endKeepsFall = false,
                    onJump = { canMakeLandSound = true }
                )
            )
        )
        return component
    }

    override fun defineControllerComponent() = ControllerComponent(
        MegaControllerButton.LEFT pairTo playerRunButton(this, Facing.LEFT, runParams, { false }, {}),
        MegaControllerButton.RIGHT pairTo playerRunButton(this, Facing.RIGHT, runParams, { false }, {})
    )

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(TAG pairTo sprite)
        component.putPreProcess(TAG) { _, player ->
            player.setSize(ProtomanValues.SPRITE_SIZE * ConstVals.PPM)
            player.setFlip(facing == Facing.LEFT, false)
            player.setOriginCenter()
            player.rotation = direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            player.setPosition(body.getPositionPoint(position), position)
            // TODO: the air offset is Megaman's for a body that points up; the other directions wait for item 058
            if (direction == Direction.UP && !bodyOverGround)
                player.translateY(ProtomanValues.AIR_SPRITE_OFFSET_Y * ConstVals.PPM)
            applyHideAndFlash(player)
        }
        return component
    }

    override fun defineAnimationsComponent(): AnimationsComponent {
        // TODO: each new Proto Man frame needs the v2 restyle (Megaman_v3 physique) before it joins the atlas (060)
        val atlas = services.assets.getTextureAtlas(TextureAsset.PROTOMAN.source)
        fun frames(name: String, count: Int): Array<TextureRegion> =
            atlas.findRegion(name)!!.splitAndFlatten(count, 1, Array())

        val standFrames = frames("stand", 2)
        val animations = ObjectMap<String, IAnimation>()
        animations.put(
            "stand", Animation(standFrames, gdxArrayOf(ProtomanValues.STAND_OPEN_TIME, ProtomanValues.STAND_FLICK_TIME))
        )
        animations.put("run_trans", Animation(frames("run_trans", 1), ProtomanValues.STEP_TIME, false))
        animations.put("run", Animation(frames("run", 4), ProtomanValues.RUN_FRAME_TIME))
        animations.put("jump", Animation(frames("jump", 1), 1f))
        // TODO: the stand frame shows while damaged or stunned until Protoman's damage and stun art (item 060e)
        animations.put("damaged", Animation(standFrames[0]))
        val animator = Animator(keySupplier = { current -> animationKey(current, animations) }, animations = animations)
        return AnimationsComponent(orderedMapOf<Any, IAnimator>(TAG pairTo animator), sprites)
    }

    internal fun animationKey(current: String?, animations: ObjectMap<String, IAnimation>): String? = when {
        roomTransitionPaused -> null
        current != null && services.levelState.roomTransition ->
            if (current == "stand" || current == "run_trans") "run" else current
        // TODO: Megaman's blue beam-in plays for Protoman (screens/levels/respawn/BeamDown.kt); his own beam color
        //  and teleport art come with item 060e
        !inPlay -> null
        // TODO: no frozen key; the normal frame shows while frozen until his frozen art (item 060e)
        damaged || stunned -> "damaged"
        isBehaviorActive(BehaviorType.JUMPING) || !bodyOverGround -> "jump"
        running -> "run"
        current == "run_trans" && !animations["run_trans"].isFinished() -> "run_trans"
        current == "run_trans" || current == "stand" -> "stand"
        else -> "run_trans"
    }
}
