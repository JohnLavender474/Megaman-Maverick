package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType
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
        private const val SCALAR_DELTA = 0.5f
        private const val RING_COUNT = 10
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN)
    override var children = Array<GameEntity>()

    private lateinit var pendulum: Pendulum
    private val timeToSpawnEnemyTimer = Timer(TIME_TO_SPAWN_ENEMY)
    private var target: Vector2? = null
    private var enemyToSpawn: String? = null
    private var scalar = MIN_SCALAR
    private var didOverlapMegaman = false
    private var doesOverlapMegaman = false

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

        body.preProcess.put(ConstKeys.TARGET) { delta ->
            target?.let { target ->
                val velocity = target.cpy().sub(body.getBottomCenterPoint()).scl(1f / delta)
                body.physics.velocity = velocity
            }
        }
        body.addBodyLabels(objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        body.fixtures.filter { it.second.getFixtureType() == FixtureType.BLOCK }.map { it.second as Fixture }.forEach {
            it.addFixtureLabels(objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE, FixtureLabel.NO_PROJECTILE_COLLISION))
        }

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
        children.forEach { it.destroy() }
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
            val line = GameLine(pendulum.anchor, pendulum.getMotionValue())
            line.color = Color.DARK_GRAY
            line.shapeType = ShapeRenderer.ShapeType.Line
            line.thickness = ConstVals.PPM / 8f
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 4f)
        circle1.shapeType = ShapeRenderer.ShapeType.Filled
        circle1.color = Color.BROWN
        addDebugShapeSupplier { circle1.setCenter(pendulum.anchor) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.shapeType = ShapeRenderer.ShapeType.Line
        circle2.color = Color.DARK_GRAY
        addDebugShapeSupplier { circle2.setCenter(pendulum.getMotionValue()) }
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
                                ConstKeys.POSITION to body.getTopCenterPoint(), ConstKeys.CULL_OUT_OF_BOUNDS to false
                            )
                        )
                    }

                    "SniperJoe" -> {
                        val sniperJoe = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.SNIPER_JOE)!!
                        children.add(sniperJoe)
                        sniperJoe.spawn(
                            props(
                                ConstKeys.POSITION to body.getTopCenterPoint(), ConstKeys.CULL_OUT_OF_BOUNDS to false
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
        spritesComponent.putUpdateFunction("platform") { _, _sprite ->
            _sprite.setSize(body.getSize())
            _sprite.setCenter(body.getCenter())
        }

        for (i in 0..RING_COUNT) {
            val ringSprite = GameSprite(regions["ring"], DrawingPriority(DrawingSection.FOREGROUND, 2))
            ringSprite.setSize(0.25f * ConstVals.PPM)
            spritesComponent.sprites.put("ring_$i", ringSprite)
            spritesComponent.putUpdateFunction("ring_$i") { _, _sprite ->
                val distance = (i.toFloat() / RING_COUNT.toFloat()) * pendulum.length
                val center = pendulum.getPointFromAnchor(distance)
                _sprite.setPosition(center, Position.BOTTOM_CENTER)
            }
        }

        return spritesComponent
    }

}