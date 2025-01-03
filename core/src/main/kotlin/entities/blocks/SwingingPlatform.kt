package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Fixture
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools

import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.*
import kotlin.math.PI

class SwingingPlatform(game: MegamanMaverickGame) : Block(game), IParentEntity, ISpritesEntity, IMotionEntity,
    IEventListener {

    companion object {
        const val TAG = "SwingingPlatform"
        private const val LENGTH = 4f
        private const val PENDULUM_GRAVITY = 3f
        private const val TIME_TO_SPAWN_ENEMY = 0.25f
        private const val MAX_SCALAR = 1.25f
        private const val MIN_SCALAR = 0.25f
        private const val RING_COUNT = 10
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN)
    override var children = Array<IGameEntity>()

    private lateinit var pendulum: Pendulum
    private val timeToSpawnEnemyTimer = Timer(TIME_TO_SPAWN_ENEMY)
    private var target: Vector2? = null
    private var enemyToSpawn: String? = null
    private var scalar = MIN_SCALAR
    private var didOverlapMegaman = false
    private var doesOverlapMegaman = false

    private val outFixtures = Array<IFixture>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put("platform", atlas.findRegion("$TAG/other_small_platform"))
            regions.put("ring", atlas.findRegion("$TAG/ring"))
        }
        super.init()
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        scalar = MAX_SCALAR
        setPendulum(bounds)

        body.preProcess.put(ConstKeys.TARGET) {
            target?.let { target ->
                val velocity = GameObjectPools.fetch(Vector2::class)
                velocity.set(target)
                    .sub(body.getPositionPoint(Position.BOTTOM_CENTER))
                    .scl(1f / ConstVals.FIXED_TIME_STEP)
                body.physics.velocity.set(velocity)
            }
        }
        body.addBodyLabels(objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        body.getFixtures(outFixtures, FixtureType.BLOCK).map { it as Fixture }.forEach {
            it.addFixtureLabels(objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE, FixtureLabel.NO_PROJECTILE_COLLISION))
        }
        outFixtures.clear()

        // spawn the enemy onto the platform after the timer has finished so that the platform position has
        // been properly updated
        enemyToSpawn =
            if (spawnProps.containsKey(ConstKeys.ENEMY_SPAWN)) spawnProps.get(ConstKeys.ENEMY_SPAWN, String::class)!!
            else null

        game.eventsMan.addListener(this)

        didOverlapMegaman = false
        doesOverlapMegaman = false
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
        enemyToSpawn = null
        game.eventsMan.removeListener(this)
        clearMotionDefinitions()
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event = $event")
        timeToSpawnEnemyTimer.reset()
    }

    private fun setPendulum(bounds: GameRectangle) {
        pendulum = Pendulum(
            length = LENGTH * ConstVals.PPM,
            gravity = PENDULUM_GRAVITY * ConstVals.PPM,
            anchor = bounds.getCenter().add(0f, LENGTH * ConstVals.PPM),
            targetFPS = 1 / 60f,
            defaultAngle = PI.toFloat() / 1.5f
        )
        putMotionDefinition(
            ConstKeys.PENDULUM,
            MotionComponent.MotionDefinition(motion = pendulum, function = { value, _ -> target = value })
        )

        addDebugShapeSupplier {
            val value = pendulum.getMotionValue()
            if (value == null) return@addDebugShapeSupplier null
            GameLine(pendulum.anchor, value)
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 4f)
        addDebugShapeSupplier { circle1.setCenter(pendulum.anchor) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        addDebugShapeSupplier {
            val value = pendulum.getMotionValue()
            if (value == null) return@addDebugShapeSupplier null
            circle2.setCenter(value)
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (enemyToSpawn != null) {
            timeToSpawnEnemyTimer.update(delta)
            if (timeToSpawnEnemyTimer.isJustFinished()) {
                when (enemyToSpawn) {
                    "PicketJoe" -> {
                        val picketJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.PICKET_JOE)!!
                        children.add(picketJoe)
                        picketJoe.spawn(
                            props(
                                ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                            )
                        )
                    }

                    "SniperJoe" -> {
                        val sniperJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.SNIPER_JOE)!!
                        children.add(sniperJoe)
                        sniperJoe.spawn(
                            props(
                                ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                            )
                        )
                    }
                }
            }
        }

        /*
        didOverlapMegaman = doesOverlapMegaman
        doesOverlapMegaman = body.overlaps(getMegaman().feetFixture.getShape())

        scalar = if (doesOverlapMegaman) min(MAX_SCALAR, scalar + SCALAR_DELTA * delta)
        else max(MIN_SCALAR, scalar - SCALAR_DELTA * delta)
        pendulum.scalar = scalar


        if (doesOverlapMegaman && !didOverlapMegaman) {
            var force = 0.5f * -pendulum.getSwingDirection()
            if (force == 0f) force = 0.5f * if (getMegaman().body.getCenter().x < body.getCenter().x) 1f else -1f
            pendulum.applyForce(force)
        }
        */

        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next() as MegaGameEntity
            if (child.dead) iter.remove()
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val spritesComponent = SpritesComponent()
        val platformSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        platformSprite.setRegion(regions["platform"])
        spritesComponent.sprites.put("platform", platformSprite)
        spritesComponent.putUpdateFunction("platform") { _, sprite ->
            sprite.setSize(body.getSize())
            sprite.setCenter(body.getCenter())
        }

        for (i in 0..RING_COUNT) {
            val ringSprite = GameSprite(regions["ring"], DrawingPriority(DrawingSection.FOREGROUND, 2))
            ringSprite.setSize(0.25f * ConstVals.PPM)
            spritesComponent.sprites.put("ring_$i", ringSprite)
            spritesComponent.putUpdateFunction("ring_$i") { _, sprite ->
                val distance = (i.toFloat() / RING_COUNT.toFloat()) * pendulum.length
                val center = pendulum.getPointFromAnchor(distance)
                sprite.setPosition(center, Position.BOTTOM_CENTER)
            }
        }

        return spritesComponent
    }

}
