package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.*

class BigAssMaverickRobotHand(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IDamager,
    IHazard, IOwnable {

    companion object {
        const val TAG = "BigAssMaverickRobot/Hand"

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private const val BLOCK_WIDTH = 1.5f
        private const val BLOCK_HEIGHT = 0.25f
        private val BLOCK_POSITION = Position.TOP_CENTER

        private const val SHIELD_WIDTH = 2f
        private const val SHIELD_HEIGHT = 1f

        private const val SPRITE_SIZE = 3f

        private const val GRAVITY = -0.15f

        private const val LAUNCH_DELAY = 1f

        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 4f

        private const val ARM_ORBS = 4

        private const val BLINK_DELAY = 0.05f

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "still" pairTo AnimationDef(),
            "rocket" pairTo AnimationDef(2, 2, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class BigAssMaverickRobotHandState { ROTATE, LAUNCH, RETURN }

    override var owner: IGameEntity? = null

    lateinit var state: BigAssMaverickRobotHandState
        private set

    internal var defeated = false

    private var block: Block? = null
    private val blockTarget: Vector2
        get() = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.5f * ConstVals.PPM)

    private lateinit var rotatingLine: RotatingLine
    private lateinit var rotateSpeedSupplier: () -> Float

    private val launchTarget = Vector2()
    private val launchDelay = Timer(LAUNCH_DELAY)
    private lateinit var launchSpeedSupplier: () -> Float

    private val returnDelay = Timer(RETURN_DELAY)
    private val returnTarget = Vector2()

    private val armOrbs = Array<BigAssMaverickRobotOrb>()
    private val armOrigin = Vector2()

    private val blinkDelay = Timer(BLINK_DELAY)
    private var blink = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, BigAssMaverickRobot::class)!!

        rotateSpeedSupplier =
            spawnProps.get("${ConstKeys.ROTATION}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}") as () -> Float

        launchSpeedSupplier =
            spawnProps.get("${ConstKeys.LAUNCH}_${ConstKeys.SPEED}_${ConstKeys.SUPPLIER}") as () -> Float

        val origin = spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!
        val radius = spawnProps.get(ConstKeys.RADIUS, Float::class)!!
        val speed = rotateSpeedSupplier.invoke()
        rotatingLine = RotatingLine(origin.cpy(), radius, speed)

        state = BigAssMaverickRobotHandState.ROTATE

        launchDelay.reset()
        returnDelay.reset()

        blinkDelay.reset()
        blink = false

        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(
            props(
                ConstKeys.BLOCK_FILTERS pairTo TAG,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                ConstKeys.WIDTH pairTo BLOCK_WIDTH * ConstVals.PPM,
                ConstKeys.HEIGHT pairTo BLOCK_HEIGHT * ConstVals.PPM,
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY)
            )
        )
        this.block = block

        armOrigin.set(spawnProps.get("${ConstKeys.ARM}_${ConstKeys.ORIGIN}", Vector2::class))
        (0 until ARM_ORBS).forEach { it ->
            val armOrb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
            armOrb.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.TRAJECTORY pairTo Vector2.Zero,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    ConstKeys.CAN_BE_HIT pairTo false,
                    ConstKeys.ACTIVE pairTo false,
                    ConstKeys.PRIORITY pairTo -1,
                    ConstKeys.SECTION pairTo DrawingSection.PLAYGROUND
                )
            )
            armOrbs.add(armOrb)
        }

        defeated = false
    }

    override fun onBossDefeated(boss: AbstractBoss) {
        GameLogger.debug(TAG, "onBossDefeated(): boss=$boss")

        defeated = true

        block?.destroy()
        block = null
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        armOrbs.forEach { if (!it.dead) it.destroy() }
        armOrbs.clear()

        block?.destroy()
        block = null
    }

    internal fun launch() {
        state = BigAssMaverickRobotHandState.LAUNCH

        launchTarget.set(megaman.body.getCenter())
        launchDelay.reset()

        returnTarget.set(body.getCenter())
    }

    internal fun isBeingStoodUpon() = megaman.feetFixture.getShape().overlaps(block!!.body.getBounds())

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!defeated) for (i in 0 until armOrbs.size) {
            val scalar = i.toFloat() / armOrbs.size.toFloat()
            val center =
                UtilMethods.interpolate(armOrigin, body.getCenter(), scalar, GameObjectPools.fetch(Vector2::class))
            armOrbs[i].body.setCenter(center)
        }

        if (defeated) {
            blinkDelay.update(delta)
            if (blinkDelay.isFinished()) {
                blink = !blink
                blinkDelay.reset()
            }

            return@UpdatablesComponent
        }

        when (state) {
            BigAssMaverickRobotHandState.ROTATE -> {
                rotatingLine.speed = rotateSpeedSupplier.invoke()
                rotatingLine.update(delta)
                val center = rotatingLine.getMotionValue()!!
                body.setCenter(center.x, center.y)
            }

            BigAssMaverickRobotHandState.LAUNCH -> {
                launchDelay.update(delta)
                if (!launchDelay.isFinished()) {
                    body.physics.velocity.setZero()
                    return@UpdatablesComponent
                }

                if (launchDelay.isJustFinished()) block!!.body.let { blockBody ->
                    blockBody.physics.collisionOn = false
                    blockBody.forEachFixture { fixture -> fixture.setActive(false) }
                }

                val launchSpeed = launchSpeedSupplier.invoke()
                body.physics.velocity.set(launchTarget).sub(body.getCenter()).nor().scl(launchSpeed)

                if (body.getCenter().epsilonEquals(launchTarget, 0.1f * ConstVals.PPM)) {
                    state = BigAssMaverickRobotHandState.RETURN

                    returnDelay.reset()
                }
            }

            BigAssMaverickRobotHandState.RETURN -> {
                returnDelay.update(delta)
                if (!returnDelay.isFinished()) {
                    body.physics.velocity.setZero()
                    return@UpdatablesComponent
                }

                body.physics.velocity.set(returnTarget).sub(body.getCenter()).nor().scl(RETURN_SPEED * ConstVals.PPM)

                if (body.getCenter().epsilonEquals(returnTarget, 0.1f * ConstVals.PPM)) {
                    state = BigAssMaverickRobotHandState.ROTATE

                    block!!.body.let { blockBody ->
                        blockBody.physics.collisionOn = true
                        blockBody.forEachFixture { fixture -> fixture.setActive(true) }
                    }
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(SHIELD_WIDTH * ConstVals.PPM, SHIELD_HEIGHT * ConstVals.PPM)

        )
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = (-body.getHeight() / 2f) + 0.5f * ConstVals.PPM.toFloat()
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (defeated && body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.setZero()
            body.physics.gravityOn = defeated && !body.isSensing(BodySense.FEET_ON_GROUND)

            block?.let { block ->
                block.body.physics.velocity
                    .set(blockTarget)
                    .sub(block.body.getPositionPoint(BLOCK_POSITION))
                    .scl(1f / ConstVals.FIXED_TIME_STEP)
            }

            damagerFixture.setActive(state != BigAssMaverickRobotHandState.ROTATE)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
                .also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val position = Position.TOP_CENTER
            val point = when (block) {
                null -> body.getPositionPoint(position)
                else -> block!!.body.getPositionPoint(position)
            }
            sprite.setPosition(point, position)

            sprite.translateY(0.25f * ConstVals.PPM)

            sprite.hidden = defeated && blink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (state == BigAssMaverickRobotHandState.ROTATE || defeated) "still" else "rocket" }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
