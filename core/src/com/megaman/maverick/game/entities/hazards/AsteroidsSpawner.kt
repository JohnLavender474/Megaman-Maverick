package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType

class AsteroidsSpawner(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, ICullableEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "AsteroidsSpawner"
        private const val MIN_SPAWN_DELAY = 0.5f
        private const val MAX_SPAWN_DELAY = 1f
        private const val MIN_ANGLE = 240f
        private const val MAX_ANGLE = 300f
        private const val MIN_SPEED = 1.5f
        private const val MAX_SPEED = 4f
        private const val MAX_CHILDREN = 5
    }

    override var children = Array<GameEntity>()

    private lateinit var bounds: GameRectangle
    private lateinit var spawnTimer: Timer

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ bounds }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(
            ConstKeys.CULL_OUT_OF_BOUNDS,
            getGameCameraCullingLogic(getGameCamera(), { bounds })
        ) else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        spawnTimer = Timer()
        resetSpawnTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        children.clear()
    }

    private fun resetSpawnTimer() {
        val newDuration = getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnTimer.resetDuration(newDuration)
    }

    private fun spawnAsteroid() {
        val angle = getRandom(MIN_ANGLE, MAX_ANGLE)
        val speed = getRandom(MIN_SPEED, MAX_SPEED)
        val impulse = Vector2(speed, 0f).rotateDeg(angle).scl(ConstVals.PPM.toFloat())
        val posX = getRandom(bounds.x, bounds.getMaxX())
        val posY = bounds.getMaxY()
        val asteroid = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ASTEROID)!!
        asteroid.spawn(
            props(
                ConstKeys.POSITION to Vector2(posX, posY),
                ConstKeys.IMPULSE to impulse
            )
        )
        children.add(asteroid)

        GameLogger.debug(TAG, "Spawned asteroid. Size of children: ${children.size}")
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        children.removeAll { !(it as MegaGameEntity).spawned }
        if (children.size >= MAX_CHILDREN) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            spawnAsteroid()
            resetSpawnTimer()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEventsSet = objectSetOf<Any>(
            EventType.PLAYER_SPAWN,
            EventType.BEGIN_ROOM_TRANS,
            EventType.GATE_INIT_OPENING
        )
        val cullEvents = CullableOnEvent({ cullEventsSet.contains(it) }, cullEventsSet)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS to cullEvents))
    }
}