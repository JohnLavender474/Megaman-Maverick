package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandom
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.utils.getGameCamera
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class AsteroidsSpawner(game: MegamanMaverickGame): GameEntity(game), IParentEntity, ICullableEntity {

    companion object {
        const val TAG = "AsteroidsSpawner"
        private const val MIN_SPAWN_DELAY = 0.1f
        private const val MAX_SPAWN_DELAY = 0.5f
        private const val MIN_ANGLE = 240f
        private const val MAX_ANGLE = 300f
        private const val MIN_SPEED = 2f
        private const val MAX_SPEED = 5f
        private const val MAX_CHILDREN = 5
    }

    override var children = Array<IGameEntity>()

    private lateinit var bounds: GameRectangle
    private lateinit var spawnTimer: Timer

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        spawnTimer = Timer()
        resetSpawnTimer()
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
        game.engine.spawn(asteroid, props(
            ConstKeys.POSITION to Vector2(posX, posY),
            ConstKeys.IMPULSE to impulse
        ))
        children.add(asteroid)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        val iter = children.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            if (next.dead) iter.remove()
        }

        if (children.size >= MAX_CHILDREN) return@UpdatablesComponent

        spawnTimer.update(delta)
        if (spawnTimer.isFinished()) {
            spawnAsteroid()
            resetSpawnTimer()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(getGameCamera(), { bounds })
        return CullablesComponent(this, objectMapOf(
            ConstKeys.CULL_OUT_OF_BOUNDS to cullOutOfBounds
        ))
    }
}