package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class Snowfall(game: MegamanMaverickGame) : MegaGameEntity(game) {

    companion object {
        const val TAG = "Snowfall"

        private const val MIN_SPAWN_DELAY = 0.1f
        private const val MAX_SPAWN_DELAY = 0.5f

        private const val MIN_FREQUENCY = 0.05f
        private const val MAX_FREQUENCY = 0.15f

        private const val MIN_AMPLITUDE = 0.025f
        private const val MAX_AMPLITUDE = 0.05f

        private const val ABS_DRIFT = 0.1f

        private const val MIN_FALL_SPEED = 2f
        private const val MAX_FALL_SPEED = 3f
    }

    private val bounds = GameRectangle()
    private val spawnDelay = Timer()
    private lateinit var spawnRoom: String
    private var left = false
    private var minY = 0f

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        minY = spawnProps
            .get("${ConstKeys.MIN}_${ConstKeys.Y}", RectangleMapObject::class)!!
            .rectangle.toGameRectangle().getY()
        resetSpawnDelay()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (game.getCurrentRoom()?.name != spawnRoom) return@UpdatablesComponent

        spawnDelay.update(delta)
        if (spawnDelay.isFinished()) {
            spawnSnow()
            resetSpawnDelay()
        }
    })

    private fun spawnSnow() {
        val position = GameObjectPools.fetch(Vector2::class).set(
            UtilMethods.getRandom(bounds.getX(), bounds.getMaxX()),
            bounds.getMaxY()
        )

        val speed = UtilMethods.getRandom(-MIN_FALL_SPEED, -MAX_FALL_SPEED) * ConstVals.PPM

        var drift = UtilMethods.getRandom(0f, ABS_DRIFT) * ConstVals.PPM
        if (left) drift *= -1f

        val snow = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SNOW)!!
        snow.spawn(
            props(
                ConstKeys.POSITION pairTo position,
                ConstKeys.SPEED pairTo speed,
                ConstKeys.DRIFT pairTo drift,
                "${ConstKeys.MIN}_${ConstKeys.Y}" pairTo minY,
                "${ConstKeys.MIN}_${ConstKeys.FREQUENCY}" pairTo MIN_FREQUENCY * ConstVals.PPM,
                "${ConstKeys.MAX}_${ConstKeys.FREQUENCY}" pairTo MAX_FREQUENCY * ConstVals.PPM,
                "${ConstKeys.MIN}_${ConstKeys.AMPLITUDE}" pairTo MIN_AMPLITUDE * ConstVals.PPM,
                "${ConstKeys.MAX}_${ConstKeys.AMPLITUDE}" pairTo MAX_AMPLITUDE * ConstVals.PPM,
            )
        )
    }

    private fun resetSpawnDelay() {
        val random = UtilMethods.getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnDelay.resetDuration(random)
    }

    override fun getEntityType() = EntityType.DECORATION
}
