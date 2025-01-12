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
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.PicketJoe
import com.megaman.maverick.game.entities.enemies.SniperJoe
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import kotlin.math.PI

class SwingingPlatform(game: MegamanMaverickGame) : Block(game), IParentEntity, ISpritesEntity, IMotionEntity,
    IEventListener {

    companion object {
        const val TAG = "SwingingPlatform"
        private const val BODY_WIDTH = 5f
        private const val BODY_HEIGHT = 1f
        private const val DEFAULT_LENGTH = 4f
        private const val PENDULUM_GRAVITY = 5f
        private const val TIME_TO_SPAWN_ENEMY = 0.25f
        private const val RING_COUNT = 10
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN)
    override var children = Array<IGameEntity>()

    private lateinit var pendulum: Pendulum
    private var target: Vector2? = null

    private val timeToSpawnEnemyTimer = Timer(TIME_TO_SPAWN_ENEMY)
    private var enemyToSpawn: String? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put(ConstKeys.PLATFORM, atlas.findRegion("$TAG/${ConstKeys.PLATFORM}"))
            regions.put(ConstKeys.RING, atlas.findRegion("$TAG/${ConstKeys.RING}"))
        }
        super.init()
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
            .setCenter(center)
        spawnProps.put(ConstKeys.BOUNDS, bounds)

        super.onSpawn(spawnProps)

        setPendulum(bounds)

        // spawn the enemy onto the platform after the timer has finished so that the platform position has
        // been properly updated
        enemyToSpawn = when {
            spawnProps.containsKey(ConstKeys.ENEMY_SPAWN) -> spawnProps.get(ConstKeys.ENEMY_SPAWN, String::class)!!
            else -> null
        }

        body.preProcess.put(ConstKeys.TARGET) {
            target?.let { target ->
                body.physics.velocity.set(target)
                    .sub(body.getPositionPoint(Position.TOP_CENTER))
                    .scl(1f / ConstVals.FIXED_TIME_STEP)
            }
        }

        game.eventsMan.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        clearMotionDefinitions()

        children.forEach { (it as GameEntity).destroy() }
        children.clear()

        enemyToSpawn = null

        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        timeToSpawnEnemyTimer.reset()
    }

    private fun setPendulum(bounds: GameRectangle) {
        pendulum = Pendulum(
            length = DEFAULT_LENGTH * ConstVals.PPM,
            gravity = PENDULUM_GRAVITY * ConstVals.PPM,
            anchor = bounds.getCenter(false).add(0f, DEFAULT_LENGTH * ConstVals.PPM),
            targetFPS = 1 / 60f,
            defaultAngle = PI.toFloat() / 1.5f
        )
        putMotionDefinition(
            ConstKeys.PENDULUM,
            MotionDefinition(motion = pendulum, function = { value, _ -> target = value })
        )

        val line = GameLine()
        addDebugShapeSupplier {
            val value = pendulum.getMotionValue()
            if (value == null) return@addDebugShapeSupplier null
            line.set(pendulum.anchor, value)
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

            if (timeToSpawnEnemyTimer.isJustFinished()) when (enemyToSpawn) {
                PicketJoe.TAG -> {
                    val picketJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.PICKET_JOE)!!
                    picketJoe.spawn(
                        props(
                            ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                            ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                        )
                    )

                    children.add(picketJoe)
                }

                SniperJoe.TAG -> {
                    val sniperJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.SNIPER_JOE)!!
                    sniperJoe.spawn(
                        props(
                            ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                            ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                        )
                    )

                    children.add(sniperJoe)
                }
            }
        }

        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next() as MegaGameEntity
            if (child.dead) iter.remove()
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            ConstKeys.PLATFORM,
            GameSprite(regions[ConstKeys.PLATFORM], DrawingPriority(DrawingSection.PLAYGROUND, 8))
                .also { sprite -> sprite.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM) }
        )
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .also { builder ->
            for (i in 0..RING_COUNT) {
                val ring = GameSprite(regions[ConstKeys.RING], DrawingPriority(DrawingSection.BACKGROUND, 0))
                ring.setSize(0.5f * ConstVals.PPM)

                val key = "${ConstKeys.RING}_$i"
                builder.sprite(key, ring).updatable { _, sprite ->
                    val distance = (i.toFloat() / RING_COUNT.toFloat()) * pendulum.length
                    val center = pendulum.getPointFromAnchor(distance)
                    sprite.setPosition(center, Position.BOTTOM_CENTER)
                }
            }
        }
        .build()
}
