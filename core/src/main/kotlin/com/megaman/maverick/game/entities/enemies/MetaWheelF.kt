package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

class MetaWheelF(services: MegaServices) : AbstractEnemy(services, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MetaWheelF"

        const val FRAME_COUNT = 8

        private const val SIZE = 1.875f
        private const val CONTACT_SIZE = 1.25f
        private const val HURT_SIZE = 1.4375f
        private const val GRAVITY = -14f
        private const val GROUND_GRAVITY = -0.01f
        private const val FALL_MAX = 12f
        private const val ACCELERATION = 5.3f
        private const val MAX_SPEED = 1.4f
        private const val TURN_WAIT_DUR = 1.5f
        private const val TURN_FLASH_DUR = 0.35f
        private const val FLASH_BLINK = 0.05f
        private const val EXPLOSIONS = 5
        private const val EXPLOSION_SPREAD = 0.5f

        private val DEBRIS = arrayOf("$TAG/debris1", "$TAG/debris2", "$TAG/debris3")

        private val ROLL_ORDER = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
        private val ROLL_TICKS = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4)

        val FRAMES = orderedMapOf(
            "roll_left" pairTo TickFrameDef(36, 34, 18, 17, FRAME_COUNT, ROLL_ORDER, ROLL_TICKS, 0),
            "roll_right" pairTo TickFrameDef(36, 34, 18, 17, FRAME_COUNT, ROLL_ORDER, ROLL_TICKS, 0),
            "turn_flash" pairTo TickFrameDef(36, 34, 18, 17, FRAME_COUNT, ROLL_ORDER, ROLL_TICKS, 0)
        )

        fun picture(key: String, index: Int) = if (key == "roll_right") FRAME_COUNT - 1 - index else index

        fun indexFor(key: String, picture: Int) = if (key == "roll_right") FRAME_COUNT - 1 - picture else picture
    }

    private enum class MetaWheelFState { ROLL, TURN }

    override lateinit var facing: Facing

    override val damageNegotiator: IDamageNegotiator = SelfSizeDamageNegotiator(
        this,
        objectMapOf(
            PlayerSabreSlash::class pairTo dmgNeg {
                it as PlayerSabreSlash
                if (it.fullStrength) 9 else 6
            }
        )
    )

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        super.canBeDamagedBy(damager) && damager is PlayerSabreSlash

    private val animations = ObjectMap<String, IAnimation>()
    private val turnTimer = Timer(TURN_WAIT_DUR + TURN_FLASH_DUR)

    private lateinit var state: MetaWheelFState
    private var wasOnGround = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        TickFrames.animations(atlas, TAG, FRAMES, animations)
        FallingPiece.load(services, DEBRIS)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(GameObjectPools.fetch(Vector2::class))
        body.setCenter(center)
        body.physics.velocity.setZero()
        val direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.LEFT, String::class)
        facing = if (direction == ConstKeys.LEFT) Facing.LEFT else Facing.RIGHT
        state = MetaWheelFState.ROLL
        wasOnGround = false
        turnTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val onGround = body.isSensing(BodySense.FEET_ON_GROUND)
            if (onGround && !wasOnGround) playThud()
            wasOnGround = onGround

            when (state) {
                MetaWheelFState.ROLL -> {
                    if (blockedAhead()) {
                        playThud()
                        body.physics.velocity.x = 0f
                        state = MetaWheelFState.TURN
                        turnTimer.reset()
                        return@add
                    }
                    val speed = body.physics.velocity.x + ACCELERATION * ConstVals.PPM * facing.value * delta
                    val max = MAX_SPEED * ConstVals.PPM
                    body.physics.velocity.x = speed.coerceIn(-max, max)
                }

                MetaWheelFState.TURN -> {
                    body.physics.velocity.x = 0f
                    turnTimer.update(delta)
                    if (turnTimer.isFinished()) {
                        swapFacing()
                        state = MetaWheelFState.ROLL
                    }
                }
            }
        }
    }

    private fun blockedAhead(): Boolean {
        val wall = if (isFacing(Facing.LEFT)) BodySense.SIDE_TOUCHING_BLOCK_LEFT else BodySense.SIDE_TOUCHING_BLOCK_RIGHT
        if (body.isSensing(wall)) return true

        // TODO: in X6 a wheel that lands on another wheel crushes the lower one; here wheels only turn when they
        //  meet (docs/x6-intro/intro-objects-plan.md, Meta Wheel F)
        val ahead = GameObjectPools.fetch(GameRectangle::class).set(body.getBounds())
        ahead.translate(2f * facing.value, 0f)
        return MegaGameEntities.getOfTag(TAG).any { other ->
            other != this && other is MetaWheelF && other.body.getBounds().overlaps(ahead) &&
                (other.body.getCenter().x - body.getCenter().x) * facing.value > 0f
        }
    }

    private fun playThud() {
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
    }

    private fun flashing() = state == MetaWheelFState.TURN && turnTimer.time >= TURN_WAIT_DUR

    override fun disintegrate(disintegrationProps: Properties?) {
        val center = body.getCenter()
        repeat(EXPLOSIONS) {
            val spread = EXPLOSION_SPREAD * ConstVals.PPM
            explode(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.DAMAGER pairTo false,
                    ConstKeys.POSITION pairTo Vector2(
                        center.x + services.random.nextFloat(-spread, spread), center.y + services.random.nextFloat(-spread, spread)
                    )
                )
            )
        }
        FallingPiece.throwDebris(DEBRIS, center, services.random)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(SIZE * ConstVals.PPM)
        body.physics.velocityClamp.set(MAX_SPEED * ConstVals.PPM, FALL_MAX * ConstVals.PPM)
        body.physics.applyFrictionX = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(CONTACT_SIZE * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(HURT_SIZE * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize((SIZE - 0.25f) * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -SIZE * ConstVals.PPM / 2f
        body.addFixture(feetFixture)

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, (SIZE - 0.5f) * ConstVals.PPM)
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -SIZE * ConstVals.PPM / 2f
        body.addFixture(leftFixture)

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, (SIZE - 0.5f) * ConstVals.PPM)
        )
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = SIZE * ConstVals.PPM / 2f
        body.addFixture(rightFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { damagerFixture }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val center = body.getCenter()
            TickFrames.place(sprite, FRAMES["roll_left"], center.x, center.y, false)
            sprite.hidden = damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            val roll = if (isFacing(Facing.LEFT)) "roll_left" else "roll_right"
            if (flashing() && ((turnTimer.time - TURN_WAIT_DUR) / FLASH_BLINK).toInt() % 2 == 0) "turn_flash"
            else roll
        }
        val animator = Animator(
            keySupplier,
            animations,
            onChangeKey = { _, oldKey, newKey ->
                if (oldKey != null && newKey != null) {
                    val picture = picture(oldKey, animations[oldKey].getIndex())
                    animations[newKey].setIndex(indexFor(newKey, picture))
                }
            },
            shouldUpdateAnimationPredicate = { _, _, _ -> state == MetaWheelFState.ROLL }
        )
        return AnimationsComponent(this, animator)
    }
}
