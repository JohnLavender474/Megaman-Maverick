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
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
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
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.projectiles.GroundPebble
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class ScooperPete(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ScooperPete"

        private const val IDLE_DUR = 1f

        private const val DIG_DUR = 0.5f
        private const val DIG_FORWARD_TIME = 0.2f
        private const val DIG_FORWARD_OFFSET = 2f / ConstVals.PPM

        private const val THROW_TIME = 0.3f
        private const val THROW_OFFSET_X = 0.8f
        private const val THROW_OFFSET_Y = 0.1f
        private val THROW_IMPULSES = gdxArrayOf(
            Vector2(20f, 2f),
            Vector2(16f, 4f),
            Vector2(10f, 8f),
            Vector2(4f, 12f),
            Vector2(2f, 16f)
        )
        private const val THROW_IMPULSE_VAR = 2f

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(2, 1, 0.05f, true),
            "dig" pairTo AnimationDef(2, 2, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ProspectorJoeState { IDLE, DIG }

    override lateinit var facing: Facing

    private val loop = Loop(ProspectorJoeState.entries.toGdxArray())
    private val currentState: ProspectorJoeState
        get() = loop.getCurrent()
    private val stateTimers = orderedMapOf(
        ProspectorJoeState.IDLE pairTo Timer(IDLE_DUR),
        ProspectorJoeState.DIG pairTo Timer(DIG_DUR).addRunnable(TimeMarkedRunnable(THROW_TIME) { throwDirt() })
    )
    private val currentStateTimer: Timer
        get() = stateTimers[currentState]

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.POSITION) -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            spawnProps.containsKey(ConstKeys.POSITION_SUPPLIER) ->
                (spawnProps.get(ConstKeys.POSITION_SUPPLIER) as () -> Vector2).invoke()
            else -> spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        }
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (currentState == ProspectorJoeState.IDLE) FacingUtils.setFacingOf(this)

            currentStateTimer.update(delta)
            if (currentStateTimer.isFinished()) {
                loop.next()
                currentStateTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = if (invincible) damageBlink else false

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            val offsetX = when {
                currentState == ProspectorJoeState.DIG && currentStateTimer.time >= DIG_FORWARD_TIME ->
                    DIG_FORWARD_OFFSET
                else -> 0f
            }
            sprite.translateX(offsetX * facing.value * ConstVals.PPM)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
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

    private fun throwDirt() {
        GameLogger.debug(TAG, "throwDirt()")

        val position = body.getCenter()
            .add(THROW_OFFSET_X * facing.value * ConstVals.PPM, THROW_OFFSET_Y * ConstVals.PPM)

        THROW_IMPULSES.forEach {
            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(it.x * facing.value, it.y)
                .add(UtilMethods.getRandom(-THROW_IMPULSE_VAR, THROW_IMPULSE_VAR))
                .scl(ConstVals.PPM.toFloat())

            val dirt = MegaEntityFactory.fetch(GroundPebble::class)!!
            dirt.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.POSITION pairTo position
                )
            )
        }

        requestToPlaySound(SoundAsset.DIG_SOUND, false)
    }
}
