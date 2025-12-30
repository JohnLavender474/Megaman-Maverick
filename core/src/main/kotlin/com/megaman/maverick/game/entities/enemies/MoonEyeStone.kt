package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.common.utils.OrbitUtils
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class MoonEyeStone(game: MegamanMaverickGame) : AbstractEnemy(game), IFreezableEntity, IAnimatedEntity,
    IDrawableShapesEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "MoonEyeStone"

        private const val AWAKEN_RADIUS = 8f
        private const val AWAKEN_DUR = 0.3f

        private const val MAX_ASTEROIDS = 4
        private const val ASTEROID_RADIUS = 1.5f
        private const val ASTEROID_ROTATION_SPEED = 0.25f

        private const val THROW_DELAY = 1f
        private const val THROW_DUR = 1f
        private const val THROW_TIME = 0.25f
        private const val THROW_SPEED = 8f

        private const val MIN_RELEASE_SPEED = 3f
        private const val MAX_RELEASE_SPEED = 6f

        private val animDefs = orderedMapOf(
            "frozen" pairTo AnimationDef(),
            "sleep" pairTo AnimationDef(),
            "awaken" pairTo AnimationDef(3, 1, 0.1f, false),
            "seek" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.1f), true),
            "throw" pairTo AnimationDef(2, 1, 0.1f, true),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MoonEyeStoneState { SLEEP, AWAKEN, SEEK }

    private data class AsteroidOrbit(var angle: Float, var distance: Float)

    override lateinit var facing: Facing
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private val loop = Loop(MoonEyeStoneState.entries.toGdxArray())
    private val currentState: MoonEyeStoneState
        get() = loop.getCurrent()

    private val awakenArea =
        GameCircle().setRadius(AWAKEN_RADIUS * ConstVals.PPM).also { it.drawingColor = Color.GRAY }
    private val awakenTimer = Timer(AWAKEN_DUR)

    private val asteroids = OrderedMap<Asteroid, AsteroidOrbit>()
    private val asteroidArea =
        GameCircle().setRadius(ASTEROID_RADIUS * ConstVals.PPM).also { it.drawingColor = Color.ORANGE }

    private val throwDelay = Timer(THROW_DELAY)
    private val throwTimer = Timer(THROW_DUR)
        .addRunnable(TimeMarkedRunnable(THROW_TIME) { throwAsteroids() })
    private val throwing: Boolean
        get() = !throwTimer.isFinished()

    private val reusableAssSet = OrderedSet<Asteroid>()
    private val reusableBodySet = MutableOrderedSet<IBody>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { awakenArea }
        addDebugShapeSupplier { asteroidArea }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        loop.reset()

        awakenArea.setCenter(body.getCenter())
        awakenTimer.reset()

        throwDelay.reset()
        throwTimer.setToEnd()

        reusableBodySet.clear()

        FacingUtils.setFacingOf(this)

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        reusableBodySet.clear()

        asteroids.keys().forEach { asteroid ->
            asteroid.owner = null

            val speed = UtilMethods.getRandom(MIN_RELEASE_SPEED, MAX_RELEASE_SPEED)
            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(0f, speed * ConstVals.PPM)
                .setAngleDeg(UtilMethods.getRandom(0f, 359f))
            asteroid.impulse.set(impulse)
        }

        asteroids.clear()

        frozen = false
    }

    override fun canBeDamagedBy(damager: IDamager) = super.canBeDamagedBy(damager) && damager !is Asteroid

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()
        explode()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            freezeHandler.update(delta)

            if (frozen) {
                body.physics.velocity.setZero()
                return@update
            }

            direction = megaman.body.direction

            if (game.isCameraRotating()) return@update

            if (currentState != MoonEyeStoneState.SEEK || !throwing) processAsteroids(delta)

            when (currentState) {
                MoonEyeStoneState.SLEEP -> {
                    body.physics.velocity.setZero()
                    awakenArea.setCenter(body.getCenter())
                    if (megaman.body.getBounds().overlaps(awakenArea)) nextState()
                }
                MoonEyeStoneState.AWAKEN -> {
                    body.physics.velocity.setZero()
                    awakenTimer.update(delta)
                    if (awakenTimer.isFinished()) nextState()
                }
                MoonEyeStoneState.SEEK -> if (throwing) {
                    body.physics.velocity.setZero()
                    throwTimer.update(delta)
                    if (throwTimer.isFinished()) throwDelay.reset()
                } else {
                    FacingUtils.setFacingOf(this)
                    if (!asteroids.isEmpty) throwDelay.update(delta)
                    if (throwDelay.isFinished() && (asteroids.size > MAX_ASTEROIDS || anyAsteroidCanHitMegaman()))
                        throwTimer.reset()
                }
            }
        }
    }

    private fun anyAsteroidCanHitMegaman(): Boolean {
        val blocks = MegaGameEntities.getOfType(EntityType.BLOCK)
        return asteroids.keys().any { asteroid ->
            val line = GameObjectPools.fetch(GameLine::class)
                .set(asteroid.body.getCenter(), megaman.body.getCenter())
            return@any blocks.none { block -> block is IBodyEntity && block.body.getBounds().overlaps(line) }
        }
    }

    private fun processAsteroids(delta: Float) {
        asteroidArea.setCenter(body.getCenter())

        val assSet = MegaGameEntities.getOfTag<Asteroid>(Asteroid.TAG, reusableAssSet)
        assSet.forEach { asteroid ->
            if (!asteroids.containsKey(asteroid) && asteroidArea.overlaps(asteroid.body.getBounds())) {
                if (asteroids.isEmpty) throwDelay.reset()

                asteroid.owner = this
                asteroid.impulse.setZero()

                val angle = asteroid.body
                    .getBounds()
                    .getCenter()
                    .sub(body.getBounds().getCenter())
                    .nor()
                    .angleDeg()
                val distance = body.getBounds().getCenter().dst(asteroid.body.getBounds().getCenter())
                asteroids.put(asteroid, AsteroidOrbit(angle, distance))
            }
        }
        assSet.clear()

        val iter = asteroids.iterator()
        while (iter.hasNext) {
            val entry = iter.next()

            val asteroid = entry.key
            if (asteroid.dead) {
                iter.remove()
                continue
            }

            val def = entry.value
            def.angle += ASTEROID_ROTATION_SPEED * 360f * delta

            val position = OrbitUtils.calculateOrbitalPosition(
                def.angle,
                def.distance,
                body.getBounds().getCenter(),
                GameObjectPools.fetch(Vector2::class)
            )
            asteroid.body.setCenter(position)
        }
    }

    private fun throwAsteroids() {
        val iter = asteroids.keys().iterator()
        while (iter.hasNext) {
            val asteroid = iter.next()

            val impulse = megaman.body.getBounds().getCenter()
                .sub(asteroid.body.getBounds().getCenter())
                .nor()
                .scl(THROW_SPEED * ConstVals.PPM)
            asteroid.impulse.set(impulse)

            iter.remove()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture ->
                when (fixture.getType()) {
                    FixtureType.DAMAGEABLE -> fixture.setActive(currentState == MoonEyeStoneState.SEEK)
                    FixtureType.SHIELD -> fixture.setActive(currentState != MoonEyeStoneState.SEEK)
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    if (frozen) return@setKeySupplier "frozen"

                    when (currentState) {
                        MoonEyeStoneState.SEEK -> if (throwing) "throw" else "seek"
                        else -> currentState.name.lowercase()
                    }
                }
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

    private fun nextState() {
        GameLogger.debug(TAG, "nextState()")
        val previous = currentState
        val current = loop.next()
        onChangeState(current, previous)
    }

    private fun onChangeState(current: MoonEyeStoneState, previous: MoonEyeStoneState) =
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
}
