package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.Nutt
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class NuttGlider(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable,
    IFreezableEntity {

    companion object {
        const val TAG = "NuttGlider"

        private const val HAS_NUTT = "has_nutt"
        private const val DEFAULT_HAS_NUTT = true

        private const val NO_NUTT_SUFFIX = ""
        private const val NUTT_SUFFIX = "_nutt"

        private const val CULL_TIME = 2f

        private const val STAND_DUR = 0.75f
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

        private const val FROZEN_GRAVITY = -0.25f
        private const val FROZEN_GROUND_GRAV = -0.01f

        private val ANIM_DEFS = objectMapOf<String, AnimationDef>(
            "glide" pairTo AnimationDef(rows = 2, duration = 0.1f),
            "glide_nutt" pairTo AnimationDef(rows = 2, duration = 0.1f),
            "jump" pairTo AnimationDef(rows = 2, duration = 0.1f, loop = false),
            "jump_nutt" pairTo AnimationDef(rows = 2, duration = 0.1f, loop = false),
            "stand" pairTo AnimationDef(),
            "stand_nutt" pairTo AnimationDef(),
            "frozen" pairTo AnimationDef(),
            "frozen_nutt" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class NuttGliderState { GLIDE, JUMP, STAND }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = frozenHandler.isFrozen()
        set(value) {
            frozenHandler.setFrozen(value)
        }
    private val frozenHandler = FreezableEntityHandler(this)

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
            AnimationUtils.loadRegions(TAG, atlas, ANIM_DEFS.keys(), regions)
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

        FacingUtils.setFacingOf(this)

        hasNutt = spawnProps.getOrDefault(HAS_NUTT, DEFAULT_HAS_NUTT, Boolean::class)

        timers.values().forEach { it.reset() }

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            if (!frozen) FacingUtils.setFacingOf(this)

            body.physics.gravityOn = frozen || state != NuttGliderState.GLIDE

            frozenHandler.update(delta)
            if (frozen) {
                body.physics.velocity.x = 0f
                return@update
            }

            when (state) {
                NuttGliderState.GLIDE -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        state = NuttGliderState.STAND
                        return@update
                    }

                    if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                    ) {
                        state = NuttGliderState.STAND
                        return@update
                    }

                    if (hasNutt && shouldDropNutt()) {
                        dropNutt()
                        hasNutt = false
                    }

                    body.physics.velocity.let { velocity ->
                        val impulseX = GLIDE_IMPULSE_X * ConstVals.PPM * delta * facing.value
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

                    if (timer.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)) {
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
        GameLogger.debug(TAG, "dropNutt()")

        val spawn = body.getCenter()
            .add(NUTT_DROP_OFFSET_X * ConstVals.PPM * facing.value, NUTT_DROP_OFFSET_Y * ConstVals.PPM)

        val nutt = MegaEntityFactory.fetch(Nutt::class)!!
        nutt.spawn(props(ConstKeys.POSITION pairTo spawn))
    }

    private fun jump() {
        GameLogger.debug(TAG, "jump()")

        body.physics.velocity.set(
            JUMP_IMPULSE_X * facing.value * ConstVals.PPM,
            JUMP_IMPULSE_Y * ConstVals.PPM
        )
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

            when {
                frozen || state == NuttGliderState.GLIDE -> {
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

            val gravity = when {
                frozen -> if (body.isSensing(BodySense.FEET_ON_GROUND)) FROZEN_GROUND_GRAV else FROZEN_GRAVITY
                else -> if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            }
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (frozen) body.physics.velocity.x = 0f

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            val frictionX = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_FRICTION_X else DEFAULT_FRICTION_X
            body.physics.defaultFrictionOnSelf.x = frictionX
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.BODY)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1)))
        .preProcess { _, sprite ->
            sprite.setSize(3f * ConstVals.PPM)

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            if (frozen || state == NuttGliderState.GLIDE) sprite.translateY(-0.5f * ConstVals.PPM)

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    val prefix = if (frozen) "frozen" else when (state) {
                        NuttGliderState.STAND -> when {
                            body.isSensing(BodySense.FEET_ON_GROUND) -> "stand"
                            else -> "jump"
                        }
                        else -> state.name.lowercase()
                    }

                    val suffix = if (hasNutt) NUTT_SUFFIX else NO_NUTT_SUFFIX

                    return@setKeySupplier "${prefix}${suffix}"
                }
                .setOnChangeKeyListener { _, oldKey, nextKey ->
                    GameLogger.debug(TAG, "onChangeKey(): oldKey=$oldKey, nextKey=$nextKey")
                }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(ANIM_DEFS, animations, regions)
                }
                .build()
        )
        .build()
}
