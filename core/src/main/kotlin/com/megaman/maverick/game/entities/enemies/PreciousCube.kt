package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class PreciousCube(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PreciousCube"

        private const val INIT_DUR = 1f
        private const val FLOAT_DUR = 1f
        private const val BLINK_IN_DUR = 0.4f
        private const val BLINK_OUT_DUR = 0.4f

        private const val INIT_IMPULSE = -8f
        private const val FLOAT_SPEED = 1.5f

        private const val INIT_FRICTION_Y = 1.025f

        private const val BLINK_IN_POS_MAX_RETRIES = 10
        private const val BLINK_IN_MIN_ANGLE_DIFF = MathUtils.PI / 6f
        private const val BLINK_IN_MIN_DIST = 2f

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "float" pairTo AnimationDef(2, 4, 0.1f, true),
            "blink_in" pairTo AnimationDef(2, 2, 0.1f, false),
            "blink_out" pairTo AnimationDef(2, 2, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class PreciousCubeColor { BLUE, GREEN, PINK, PURPLE }

    private enum class PreciousCubeState { INIT, FLOAT, BLINK_OUT, BLINK_IN }

    override lateinit var facing: Facing

    private lateinit var state: PreciousCubeState
    private lateinit var color: PreciousCubeColor

    private val timers = orderedMapOf(
        PreciousCubeState.INIT pairTo Timer(INIT_DUR),
        PreciousCubeState.FLOAT pairTo Timer(FLOAT_DUR),
        PreciousCubeState.BLINK_IN pairTo Timer(BLINK_IN_DUR),
        PreciousCubeState.BLINK_OUT pairTo Timer(BLINK_OUT_DUR)
    )

    private val reusablePosArray = Array<Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { state ->
                PreciousCubeColor.entries.forEach { color ->
                    val key = "${state}_${color.name.lowercase()}"
                    val region = atlas.findRegion("$TAG/$key") ?: throw IllegalStateException("Region is null: $key")
                    regions.put(key, region)
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        body.physics.velocity.y = INIT_IMPULSE * ConstVals.PPM

        state = PreciousCubeState.INIT
        color = spawnProps.get(ConstKeys.COLOR, PreciousCubeColor::class)!!

        timers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val timer = timers[state]
            timer.update(delta)
            if (timer.isFinished()) {
                state = getNextState()
                onNewState(state)
                timer.reset()
            }

            when (state) {
                PreciousCubeState.FLOAT -> {
                    if (!body.getBounds().overlaps(megaman.body.getBounds())) {
                        val velocity =
                            megaman.body.getCenter().sub(body.getCenter()).nor().scl(FLOAT_SPEED * ConstVals.PPM)
                        body.physics.velocity.set(velocity)

                        FacingUtils.setFacingOf(this)
                    }
                }
                PreciousCubeState.BLINK_OUT, PreciousCubeState.BLINK_IN -> body.physics.velocity.setZero()
                else -> {}
            }
        }
    }

    private fun getNextState() = when (state) {
        PreciousCubeState.INIT, PreciousCubeState.FLOAT -> PreciousCubeState.BLINK_OUT
        PreciousCubeState.BLINK_OUT -> PreciousCubeState.BLINK_IN
        PreciousCubeState.BLINK_IN -> PreciousCubeState.FLOAT
    }

    private fun onNewState(state: PreciousCubeState) {
        if (state == PreciousCubeState.BLINK_IN) {
            val currentAngle = megaman.body.getCenter().sub(body.getCenter()).nor().angleRad()

            val positions = reusablePosArray
            (0 until BLINK_IN_POS_MAX_RETRIES).forEach {
                var radius = megaman.body.getCenter().dst(body.getCenter())
                if (radius < BLINK_IN_MIN_DIST * ConstVals.PPM) radius = BLINK_IN_MIN_DIST * ConstVals.PPM

                var angle = MathUtils.random() * MathUtils.PI * 2f
                if (abs(angle - currentAngle) < BLINK_IN_MIN_ANGLE_DIFF) {
                    val random = UtilMethods.getRandomBool()
                    if (random) angle -= BLINK_IN_MIN_ANGLE_DIFF else angle += BLINK_IN_MIN_ANGLE_DIFF
                }

                val x = MathUtils.cos(angle) * radius
                val y = MathUtils.sin(angle) * radius

                val position = megaman.body.getCenter().add(x, y)

                positions.add(position)
            }
            positions.shuffle()

            val camBounds = game.getGameCamera().getRotatedBounds()
            var blinkInPos = positions.firstOrNull { position -> camBounds.contains(position) }

            if (blinkInPos == null) {
                positions.sort { pos1, pos2 ->
                    pos1.dst2(megaman.body.getCenter())
                        .minus(pos2.dst2(megaman.body.getCenter()))
                        .toInt()
                }
                blinkInPos = positions[0]
            }

            body.setCenter(blinkInPos)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.defaultFrictionOnSelf.y = INIT_FRICTION_Y
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val applyFriction = state == PreciousCubeState.INIT
            body.physics.applyFrictionX = applyFriction
            body.physics.applyFrictionY = applyFriction

            val damager = !state.equalsAny(PreciousCubeState.BLINK_IN, PreciousCubeState.BLINK_OUT)
            body.fixtures[FixtureType.DAMAGER].first().setActive(damager)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    "${
                        when (state) {
                            PreciousCubeState.INIT, PreciousCubeState.FLOAT -> "float"
                            else -> state.name.lowercase()
                        }
                    }_${color.name.lowercase()}"
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        PreciousCubeColor.entries.forEach { color ->
                            val key = "${entry.key}_${color.name.lowercase()}"
                            val (rows, columns, durations, loop) = entry.value
                            val region = regions[key]
                            if (region == null) throw IllegalStateException("Region is null: $key")
                            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                        }
                    }
                }
                .build()
        )
        .build()
}
