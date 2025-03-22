package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Needle.NeedleType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SpikeBot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SpikeBot"

        private const val STAND_DUR = 0.25f

        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.25f

        private const val WALK_DUR = 0.75f
        private const val WALK_SPEED = 4f

        private const val NEEDLES = 3
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 10f
        private const val NEEDLE_Y_OFFSET = 0.1f

        private const val JUMP_IMPULSE = 10f
        private const val LEFT_FOOT = "${ConstKeys.LEFT}_${ConstKeys.FOOT}"
        private const val RIGHT_FOOT = "${ConstKeys.RIGHT}_${ConstKeys.FOOT}"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val NORMAL_MOVEMENT_SCALAR = 1f
        private const val WATER_MOVEMENT_SCALAR = 0.5f

        private val angles = gdxArrayOf(45f, 0f, 315f)
        private val xOffsets = gdxArrayOf(-0.1f, 0f, 0.1f)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class SpikeBotType(val append: String) { DEFAULT(""), SNOW("_snow") }

    private enum class SpikeBotState { STAND, WALK, SHOOT }

    override lateinit var facing: Facing

    lateinit var type: SpikeBotType

    private val loop = Loop(SpikeBotState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        "stand" pairTo Timer(STAND_DUR),
        "shoot" pairTo Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(SHOOT_TIME) { shoot() })),
        "walk" pairTo Timer(WALK_DUR)
    )
    private lateinit var animator: Animator

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("jump", "walk", "shoot", "stand").forEach { key ->
                SpikeBotType.entries.forEach { type ->
                    val amendedKey = "${key}${type.append}"
                    regions.put(amendedKey, atlas.findRegion("${TAG}/${amendedKey}"))
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        timers.values().forEach { it.reset() }
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        type = when {
            spawnProps.containsKey(ConstKeys.TYPE) -> {
                val rawType = spawnProps.get(ConstKeys.TYPE)
                rawType as? SpikeBotType ?: when (rawType) {
                    is String -> SpikeBotType.valueOf(rawType.uppercase())
                    else -> throw IllegalArgumentException("Illegal value for type: $rawType")
                }
            }

            else -> SpikeBotType.DEFAULT
        }
    }

    private fun shoot() {
        for (i in 0 until NEEDLES) {
            val xOffset = xOffsets[i]
            val position =
                body.getPositionPoint(Position.TOP_CENTER).add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)

            val angle = angles[i]
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angle).scl(movementScalar)

            val needleType = when (type) {
                SpikeBotType.DEFAULT -> NeedleType.DEFAULT
                SpikeBotType.SNOW -> NeedleType.ICE
            }

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM,
                    ConstKeys.TYPE pairTo needleType
                )
            )
        }

        val sound = when (type) {
            SpikeBotType.DEFAULT -> SoundAsset.THUMP_SOUND
            SpikeBotType.SNOW -> SoundAsset.ICE_SHARD_1_SOUND
        }
        if (overlapsGameCamera()) requestToPlaySound(sound, false)
    }

    private fun jump() {
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM * movementScalar
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            movementScalar = if (body.isSensing(BodySense.IN_WATER)) WATER_MOVEMENT_SCALAR else NORMAL_MOVEMENT_SCALAR
            animator.updateScalar = movementScalar

            if (!body.isSensing(BodySense.FEET_ON_GROUND) && loop.getCurrent() != SpikeBotState.SHOOT) {
                body.physics.velocity.x = WALK_SPEED * ConstVals.PPM * facing.value * movementScalar
                return@add
            }

            when (loop.getCurrent()) {
                SpikeBotState.STAND, SpikeBotState.SHOOT -> body.physics.velocity.x = 0f
                SpikeBotState.WALK -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND))
                        body.physics.velocity.x = WALK_SPEED * ConstVals.PPM * facing.value * movementScalar

                    when {
                        FacingUtils.isFacingBlock(this) -> swapFacing()

                        isFacing(Facing.LEFT) && !body.isProperty(LEFT_FOOT, true) ->
                            if (megaman.body.getX() < body.getX()) jump() else swapFacing()

                        isFacing(Facing.RIGHT) && !body.isProperty(RIGHT_FOOT, true) ->
                            if (megaman.body.getX() > body.getX()) jump() else swapFacing()
                    }
                }
            }

            val timer = timers[loop.getCurrent().name.lowercase()]
            timer.update(delta)

            if (timer.isFinished()) {
                timer.reset()
                loop.next()
                if (loop.getCurrent() != SpikeBotState.WALK)
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.putProperty(LEFT_FOOT, false)
        body.putProperty(RIGHT_FOOT, false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftSideFixture }

        val rightSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightSideFixture }

        val leftFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFootFixture.setFilter { fixture -> fixture.getType() == FixtureType.BLOCK }
        leftFootFixture.setConsumer { _, fixture -> body.putProperty(LEFT_FOOT, true) }
        leftFootFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -body.getHeight() / 2f)
        body.addFixture(leftFootFixture)
        leftFootFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFootFixture }

        val rightFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFootFixture.setFilter { fixture -> fixture.getType() == FixtureType.BLOCK }
        rightFootFixture.setConsumer { _, fixture -> body.putProperty(RIGHT_FOOT, true) }
        rightFootFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -body.getHeight() / 2f)
        body.addFixture(rightFootFixture)
        rightFootFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFootFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.putProperty(LEFT_FOOT, false)
            body.putProperty(RIGHT_FOOT, false)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM * movementScalar

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0)
                body.physics.velocity.y = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.WATER_LISTENER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            val key = when {
                !body.isSensing(BodySense.FEET_ON_GROUND) -> "jump"
                else -> when (loop.getCurrent()) {
                    SpikeBotState.STAND -> "stand"
                    SpikeBotState.WALK -> "walk"
                    SpikeBotState.SHOOT -> "shoot"
                }
            }
            "${key}${type.append}"
        }
        val animations = ObjectMap<String, IAnimation>()
        gdxArrayOf(
            "jump" pairTo AnimationDef(),
            "stand" pairTo AnimationDef(),
            "walk" pairTo AnimationDef(2, 2, 0.1f, true),
            "shoot" pairTo AnimationDef(1, 5, 0.1f, false)
        ).forEach { (key, def) ->
            SpikeBotType.entries.forEach { type ->
                val amendedKey = "${key}${type.append}"
                val animation = Animation(regions[amendedKey], def.rows, def.cols, def.durations, def.loop)
                animations.put(amendedKey, animation)
            }
        }
        animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
