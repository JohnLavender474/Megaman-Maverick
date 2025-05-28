package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class NuttGlider(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "NuttGlider"

        private const val HAS_NUTT = "has_nutt"
        private const val DEFAULT_HAS_NUTT = true

        private const val NO_NUTT_SUFFIX = ""
        private const val NUTT_SUFFIX = "_nutt"

        private const val CULL_TIME = 2f

        private const val STAND_DUR = 1f
        private const val GLIDE_DUR = 1f

        private const val JUMP_IMPULSE_X = 6f
        private const val JUMP_IMPULSE_Y = 14f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val DEFAULT_FRICTION_X = 1.25f
        private const val GROUND_FRICTION_X = 5f

        private const val GLIDE_VEL_MAX_X = 7.5f
        private const val GLIDE_IMPULSE_X = 15f
        private const val GLIDE_VEL_Y = -2.5f

        private const val NUTT_DROP_OFFSET_X = 0.75f
        private const val NUTT_DROP_OFFSET_Y = -0.25f

        private val ANIM_DEFS = objectMapOf<NuttGliderState, AnimationDef>(
            NuttGliderState.GLIDE pairTo AnimationDef(rows = 2, duration = 0.1f),
            NuttGliderState.JUMP pairTo AnimationDef(rows = 2, duration = 0.1f, loop = false),
            NuttGliderState.STAND pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class NuttGliderState { GLIDE, JUMP, STAND }

    override lateinit var facing: Facing

    private val timers = objectMapOf(
        NuttGliderState.STAND pairTo Timer(STAND_DUR),
        NuttGliderState.GLIDE pairTo Timer(GLIDE_DUR)
    )

    private lateinit var state: NuttGliderState

    private var hasNutt = true

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            NuttGliderState.entries.forEach { state ->
                gdxArrayOf(NO_NUTT_SUFFIX, NUTT_SUFFIX).forEach { suffix ->
                    val key = "${state.name.lowercase()}$suffix"
                    regions.put(key, atlas.findRegion("$TAG/$key"))
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        state = when {
            body.isSensing(BodySense.FEET_ON_GROUND) -> NuttGliderState.STAND
            else -> NuttGliderState.JUMP
        }

        updateFacing()

        hasNutt = spawnProps.getOrDefault(HAS_NUTT, DEFAULT_HAS_NUTT, Boolean::class)

        timers.values().forEach { it.reset() }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            updateFacing()

            body.physics.gravityOn = state != NuttGliderState.GLIDE

            when (state) {
                NuttGliderState.GLIDE -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        state = NuttGliderState.STAND
                        return@add
                    }

                    if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                    ) {
                        state = NuttGliderState.STAND
                        return@add
                    }

                    if (hasNutt && shouldDropNutt()) {
                        dropNutt()
                        hasNutt = false
                    }

                    body.physics.velocity.let { velocity ->
                        var impulseX = GLIDE_IMPULSE_X * ConstVals.PPM * delta * facing.value
                        velocity.x += impulseX

                        velocity.x =
                            velocity.x.coerceIn(-GLIDE_VEL_MAX_X * ConstVals.PPM, GLIDE_VEL_MAX_X * ConstVals.PPM)

                        velocity.y = GLIDE_VEL_Y * ConstVals.PPM
                    }
                }

                NuttGliderState.JUMP -> {
                    when {
                        body.physics.velocity.y < 0f && !body.isSensing(BodySense.FEET_ON_GROUND) ->
                            state = NuttGliderState.GLIDE

                        body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND) ->
                            state = NuttGliderState.STAND
                    }
                }

                NuttGliderState.STAND -> {
                    body.physics.velocity.x = 0f

                    val timer = timers[state]
                    timer.update(delta)

                    if (timer.isFinished()) {
                        jump()
                        timer.reset()
                        state = NuttGliderState.JUMP
                    }
                }
            }
        }
    }

    private fun shouldDropNutt(): Boolean {
        val x = body.getCenter().x + NUTT_DROP_OFFSET_X * ConstVals.PPM * facing.value
        return x >= megaman.body.getX() && x <= megaman.body.getMaxX()
    }

    private fun dropNutt() {
        val nutt = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.Companion.NUTT)!!
        val spawn =
            body.getCenter().add(NUTT_DROP_OFFSET_X * ConstVals.PPM * facing.value, NUTT_DROP_OFFSET_Y * ConstVals.PPM)
        nutt.spawn(props(ConstKeys.POSITION pairTo spawn))
    }

    private fun jump() = body.physics.velocity.set(
        JUMP_IMPULSE_X * facing.value * ConstVals.PPM,
        JUMP_IMPULSE_Y * ConstVals.PPM
    )

    private fun updateFacing() {
        when {
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture = Fixture(body, FixtureType.HEAD, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val outFixtures = Array<IFixture>()
        body.preProcess.put(ConstKeys.DEFAULT) {
            val bodySize = GameObjectPools.fetch(Vector2::class)
            val feetWidth: Float
            val headWidth: Float

            when (state) {
                NuttGliderState.GLIDE -> {
                    bodySize.set(1.75f, 0.75f)
                    feetWidth = 1.5f
                    headWidth = 0.75f
                }

                else -> {
                    bodySize.set(1f, 1f)
                    feetWidth = 1f
                    headWidth = 0.25f
                }
            }

            body.setSize(bodySize.scl(ConstVals.PPM.toFloat()))

            (feetFixture.rawShape as GameRectangle).setWidth(feetWidth * ConstVals.PPM)
            (headFixture.rawShape as GameRectangle).setWidth(headWidth * ConstVals.PPM)

            feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
            leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
            rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

            body.getFixtures(outFixtures, FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE).forEach {
                val bounds = (it as Fixture).rawShape as GameRectangle
                bounds.setSize(bodySize)
            }
            outFixtures.clear()

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            val frictionX = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_FRICTION_X else DEFAULT_FRICTION_X
            body.physics.defaultFrictionOnSelf.x = frictionX
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.Companion.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.BODY)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1)))
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setSize(3f * ConstVals.PPM)
            sprite.setPosition(body.getPositionPoint(position), position)
            if (state == NuttGliderState.GLIDE) sprite.translateY(-0.5f * ConstVals.PPM)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    val prefix = when (state) {
                        NuttGliderState.STAND -> when {
                            body.isSensing(BodySense.FEET_ON_GROUND) -> NuttGliderState.STAND.name.lowercase()
                            else -> NuttGliderState.JUMP.name.lowercase()
                        }

                        else -> state.name.lowercase()
                    }
                    val suffix = if (hasNutt) NUTT_SUFFIX else NO_NUTT_SUFFIX
                    "${prefix}${suffix}"
                }
                .applyToAnimations { animations ->
                    NuttGliderState.entries.forEach { state ->
                        gdxArrayOf(NO_NUTT_SUFFIX, NUTT_SUFFIX).forEach { suffix ->
                            val (rows, columns, durations, loop) = ANIM_DEFS[state]

                            val key = "${state.name.lowercase()}$suffix"
                            val region = regions[key]

                            val animation = Animation(region, rows, columns, durations, loop)
                            animations.put(key, animation)
                        }
                    }
                }
                .build()
        )
        .build()
}
