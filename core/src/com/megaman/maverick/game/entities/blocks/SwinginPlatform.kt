package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.events.EventType

class SwinginPlatform(game: MegamanMaverickGame) : Block(game), IParentEntity, ISpritesEntity, IMotionEntity,
    IDrawableShapesEntity, IEventListener {

    companion object {
        const val TAG = "SwinginPlatform"
        private const val LENGTH = 2.5f
        private const val PENDULUM_GRAVITY = 3f
        private const val TIME_TO_SPAWN_ENEMY = 0.25f
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN
    )
    override var children = Array<GameEntity>()

    private lateinit var pendulum: Pendulum
    private val timeToSpawnEnemyTimer = Timer(TIME_TO_SPAWN_ENEMY)
    private var target: Vector2? = null
    private var enemyToSpawn: String? = null

    override fun init() {
        super.init()
        addComponent(MotionComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        clearMotionDefinitions()
        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        body.setCenter(bounds.getCenter())
        setPendulum(bounds)

        body.preProcess.put(ConstKeys.TARGET) { delta ->
            target?.let { target ->
                val velocity = target.cpy().sub(body.getBottomCenterPoint()).scl(1f / delta)
                body.physics.velocity = velocity
            }
        }

        val regionKey = spawnProps.get(ConstKeys.REGION, String::class)!!
        val region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, regionKey)
        firstSprite!!.setRegion(region)

        // spawn the enemy onto the platform after the timer has finished so that the platform position has
        // been properly updated
        enemyToSpawn =
            if (spawnProps.containsKey(ConstKeys.ENEMY_SPAWN)) spawnProps.get(ConstKeys.ENEMY_SPAWN, String::class)!!
            else null

        game.eventsMan.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { it.destroy() }
        children.clear()
        enemyToSpawn = null
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event = $event")
        timeToSpawnEnemyTimer.reset()
    }

    private fun setPendulum(bounds: GameRectangle) {
        pendulum = Pendulum(
            LENGTH * ConstVals.PPM, PENDULUM_GRAVITY * ConstVals.PPM, bounds.getCenter(), 1 / 60f
        )
        putMotionDefinition(
            ConstKeys.PENDULUM, MotionComponent.MotionDefinition(motion = pendulum, function = { value, _ ->
                target = value
            })
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

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (enemyToSpawn != null) {
            timeToSpawnEnemyTimer.update(it)
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

        val iter = children.iterator()
        while (iter.hasNext()) {
            val child = iter.next()
            if (!(child as MegaGameEntity).spawned) iter.remove()
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM, 1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

}