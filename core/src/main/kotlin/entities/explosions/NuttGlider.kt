package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class NuttGlider(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "NuttGlider"

        private const val HAS_NUTT = "has_nutt"
        private const val DEFAULT_HAS_NUTT = false

        private const val NO_NUTT_SUFFIX = ""
        private const val NUTT_SUFFIX = "_nutt"

        private const val STAND_DUR = 1f
        private const val GLIDE_DUR = 1f

        private const val JUMP_IMPULSE_X = 5f
        private const val JUMP_IMPULSE_Y = 16f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val DEFAULT_FRICTION_X = 1.05f
        private const val GROUND_FRICTION_X = 5f

        private const val GLIDE_VEL_MAX_X = 7.5f
        private const val GLIDE_IMPULSE_X = 15f
        private const val GLIDE_VEL_Y = -2.5f

        private val ANIM_DEFS = objectMapOf<NuttGliderState, AnimationDef>(
            NuttGliderState.GLIDE pairTo AnimationDef(rows = 2, duration = 0.1f),
            NuttGliderState.JUMP pairTo AnimationDef(rows = 2, duration = 0.1f, loop = false),
            NuttGliderState.STAND pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class NuttGliderState { GLIDE, JUMP, STAND }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private val timers = objectMapOf(
        NuttGliderState.STAND pairTo Timer(STAND_DUR),
        NuttGliderState.GLIDE pairTo Timer(GLIDE_DUR)
    )
    private lateinit var state: NuttGliderState
    private var hasNutt = true

    override fun init() {
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

                    body.physics.velocity.let { velocity ->
                        var impulseX = GLIDE_IMPULSE_X * ConstVals.PPM * delta
                        if (megaman().body.getCenter().x < body.getCenter().x) impulseX *= -1f
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
                    val timer = timers[state]
                    timer.update(delta)

                    if (timer.isFinished()) {
                        jump()
                        state = NuttGliderState.JUMP
                        timer.reset()
                    }
                }
            }
        }
    }

    private fun jump() = body.physics.velocity.add(
        JUMP_IMPULSE_X * facing.value * ConstVals.PPM,
        JUMP_IMPULSE_Y * ConstVals.PPM
    )

    private fun updateFacing() {
        facing = when {
            megaman().body.getCenter().x < body.getCenter().x -> Facing.LEFT
            else -> Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionY = false
        body.setHeight(0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = 0.375f * ConstVals.PPM
        body.addFixture(headFixture)
        headFixture.rawShape.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val width = when (state) {
                NuttGliderState.GLIDE -> 1.5f
                else -> 1f
            }
            body.setWidth(width * ConstVals.PPM)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

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
        .sprite(TAG, GameSprite())
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setSize(2.5f * ConstVals.PPM)
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { "${state.name.lowercase()}${if (hasNutt) NUTT_SUFFIX else NO_NUTT_SUFFIX}" }
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
