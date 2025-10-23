package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
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
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.entities.projectiles.Rock
import com.megaman.maverick.game.entities.projectiles.Rock.RockSize
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class DrillHead(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DrillHead"

        private const val IDLE_DUR = 0.5f
        private const val HARD_IDLE_DUR = 0.25f

        private const val DRILL_DUR = 2f

        private const val FLY_SPEED = 8f
        private const val HOVER_SPEED = 5f
        private const val HARD_HOVER_SPEED = 8f

        private const val DRILL_DEBRIS_OBJS = 10
        private const val DRILL_EXPLOSIONS = 5

        private const val DRILL_ROCK_MIN_X_IMPULSE = -8f
        private const val DRILL_ROCK_MAX_X_IMPULSE = 8f
        private const val DRILL_ROCK_MIN_Y_IMPULSE = -12f
        private const val DRILL_ROCK_MAX_Y_IMPULSE = -4f

        // Avoid spawning debris if the FPS dips below this guard
        private const val FPS_GUARD = 50

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(),
            "fly" pairTo AnimationDef(2, 1, 0.05f, true),
            "hover" pairTo AnimationDef(2, 1, 0.1f, true),
            "drill" pairTo AnimationDef(3, 1, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DrillHeadState {
        HOVER, IDLE, FLY, DRILL,
    }

    private enum class DrillSpawnType {
        ROCK, PRECIOUS_SHARD,
    }

    override lateinit var facing: Facing

    private val stateLoop = Loop(DrillHeadState.entries.toGdxArray())
    private val currentState: DrillHeadState
        get() = stateLoop.getCurrent()
    private lateinit var stateTimers: OrderedMap<DrillHeadState, Timer>

    private lateinit var drillSpawnType: DrillSpawnType

    private val currentHoverSpot = Vector2()
    private val hoverSpots = Array<Vector2>()
    private val tempVec2Set = OrderedSet<Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        if (spawnProps.containsKey("${ConstKeys.CULL}_${ConstKeys.BOUNDS}")) {
            val cullBounds = spawnProps.get(
                "${ConstKeys.CULL}_${ConstKeys.BOUNDS}",
                RectangleMapObject::class
            )!!.rectangle.toGameRectangle(false)

            spawnProps.put(
                "${ConstKeys.CULL}_${ConstKeys.BOUNDS}_${ConstKeys.SUPPLIER}",
                { cullBounds }
            )
        }

        super.onSpawn(spawnProps)

        stateTimers = orderedMapOf(
            DrillHeadState.IDLE pairTo Timer(if (game.state.hardMode) HARD_IDLE_DUR else IDLE_DUR),
            DrillHeadState.DRILL pairTo Timer(DRILL_DUR).also { drillTimer ->
                val debrisRunnable: () -> Unit = {
                    if (Gdx.graphics.framesPerSecond >= FPS_GUARD) spawnDrillDebris()
                }
                val drillDebrisDelay = DRILL_DUR / DRILL_DEBRIS_OBJS
                for (i in 1..DRILL_DEBRIS_OBJS) drillTimer.addRunnable(
                    TimeMarkedRunnable(i * drillDebrisDelay, debrisRunnable)
                )

                val explosionRunnable: () -> Unit = {
                    val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
                    explosion.spawn(
                        props(
                            ConstKeys.OWNER pairTo this,
                            ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER)
                        )
                    )
                }
                val drillExplosionDelay = DRILL_DUR / DRILL_EXPLOSIONS
                for (i in 0 until DRILL_EXPLOSIONS) drillTimer.addRunnable(
                    TimeMarkedRunnable(i * drillExplosionDelay, explosionRunnable)
                )
            }
        )

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        stateLoop.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.HOVER)) {
                val hoverSpot = (value as RectangleMapObject).rectangle.getCenter(false)
                hoverSpots.add(hoverSpot)
            }
        }

        setNextPosition()

        drillSpawnType = when {
            spawnProps.containsKey("${ConstKeys.DRILL}_${ConstKeys.SPAWN}_${ConstKeys.TYPE}") ->
                DrillSpawnType.valueOf(
                    spawnProps
                        .get("${ConstKeys.DRILL}_${ConstKeys.SPAWN}_${ConstKeys.TYPE}", String::class)!!
                        .uppercase()
                )
            else -> DrillSpawnType.ROCK
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        hoverSpots.forEach { GameObjectPools.free(it) }
        hoverSpots.clear()

        currentHoverSpot.setZero()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val timer = stateTimers[currentState]
            if (timer != null) {
                timer.update(delta)
                if (timer.isFinished()) {
                    nextState()
                    timer.reset()
                }
            }

            when (currentState) {
                DrillHeadState.IDLE -> {
                    body.physics.velocity.setZero()
                    FacingUtils.setFacingOf(this)
                }
                DrillHeadState.FLY -> {
                    body.physics.velocity.set(0f, FLY_SPEED * ConstVals.PPM)
                    if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                        nextState()
                }
                DrillHeadState.DRILL -> body.physics.velocity.setZero()
                DrillHeadState.HOVER -> {
                    val hoverSpeed = if (game.state.hardMode) HARD_HOVER_SPEED else HOVER_SPEED
                    val velocity = GameObjectPools.fetch(Vector2::class)
                        .set(currentHoverSpot)
                        .sub(body.getCenter())
                        .nor()
                        .scl(hoverSpeed * ConstVals.PPM)
                    body.physics.velocity.set(velocity)

                    if (body.getCenter().epsilonEquals(currentHoverSpot, 0.1f * ConstVals.PPM))
                        nextState()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        damageableFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.75f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = 0.375f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGER
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun nextState() {
        val next = stateLoop.next()
        if (next == DrillHeadState.HOVER) setNextPosition()
    }

    private fun setNextPosition() {
        tempVec2Set.addAll(hoverSpots)
        tempVec2Set.remove(currentHoverSpot)

        val nextHoverSpot = tempVec2Set.random()
        currentHoverSpot.set(nextHoverSpot)

        tempVec2Set.clear()
    }

    private fun spawnDrillDebris() {
        val big = UtilMethods.getRandomBool()

        val position = body.getPositionPoint(Position.TOP_CENTER)
            .sub(0f, 0.2f * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .setX(UtilMethods.getRandom(DRILL_ROCK_MIN_X_IMPULSE, DRILL_ROCK_MAX_X_IMPULSE))
            .setY(UtilMethods.getRandom(DRILL_ROCK_MIN_Y_IMPULSE, DRILL_ROCK_MAX_Y_IMPULSE))
            .scl(ConstVals.PPM.toFloat())

        GameLogger.debug(
            TAG,
            "spawnDrillDebris(): " +
                "big=$big, rockRosition=$position, rockImpulse=$impulse, body.center=${body.getCenter()}"
        )

        val size = if (big) RockSize.BIG else RockSize.SMALL

        when (drillSpawnType) {
            DrillSpawnType.ROCK -> {
                val rock = MegaEntityFactory.fetch(Rock::class)!!
                rock.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.SIZE pairTo size,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.POSITION pairTo position
                    )
                )
            }
            DrillSpawnType.PRECIOUS_SHARD -> {
                val size = PreciousShardSize.entries.random()
                val color = PreciousShardColor.entries.random()

                val preciousShard = MegaEntityFactory.fetch(PreciousShard::class)!!
                preciousShard.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.SIZE pairTo size,
                        ConstKeys.COLOR pairTo color,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.POSITION pairTo position
                    )
                )

                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.DINK_SOUND, false)
            }
        }
    }
}
