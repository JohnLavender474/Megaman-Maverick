package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.overlaps
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class Snowfall(game: MegamanMaverickGame) : MegaGameEntity(game) {

    companion object {
        const val TAG = "Snowfall"

        private const val MIN_SPAWN_DELAY = 0.25f
        private const val MAX_SPAWN_DELAY = 0.5f

        private const val MIN_FREQUENCY = 0.05f
        private const val MAX_FREQUENCY = 0.15f

        private const val MIN_AMPLITUDE = 0.025f
        private const val MAX_AMPLITUDE = 0.05f

        private const val ABS_DRIFT = 0.1f

        private const val MIN_FALL_SPEED = 3f
        private const val MAX_FALL_SPEED = 4f

        private const val BACKGROUND_SCALAR = 0.5f
        private const val TRIGGER_BOUNDS_SCALAR = 3f
    }

    private val bounds = GameRectangle()
    private val trigger = GameRectangle()

    private val spawnDelay = Timer()

    private lateinit var spawnRoom: String

    private var background = false
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

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        val triggerWidth = bounds.getWidth() * TRIGGER_BOUNDS_SCALAR
        val triggerHeight = bounds.getHeight() * TRIGGER_BOUNDS_SCALAR
        trigger.setSize(triggerWidth, triggerHeight).setTopCenterToPoint(bounds.getPositionPoint(Position.TOP_CENTER))

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        minY = spawnProps
            .get("${ConstKeys.MIN}_${ConstKeys.Y}", RectangleMapObject::class)!!
            .rectangle.toGameRectangle().getY()
        background = spawnProps.getOrDefault(ConstKeys.BACKGROUND, false, Boolean::class)

        resetSpawnDelay()

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.INIT)) {
                val spawn = (value as RectangleMapObject).rectangle.getCenter()
                spawnSnow(spawn)
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (game.getCurrentRoom()?.name != spawnRoom || !game.getGameCamera().overlaps(trigger))
            return@UpdatablesComponent

        spawnDelay.update(delta)
        if (spawnDelay.isFinished()) {
            spawnSnow()
            resetSpawnDelay()
        }
    })

    private fun spawnSnow(position: Vector2? = null) {
        val spawnPos = position ?: GameObjectPools.fetch(Vector2::class).set(
            UtilMethods.getRandom(bounds.getX(), bounds.getMaxX()),
            bounds.getMaxY()
        )

        var speed = UtilMethods.getRandom(-MIN_FALL_SPEED, -MAX_FALL_SPEED) * ConstVals.PPM
        if (background) speed *= BACKGROUND_SCALAR

        var drift = UtilMethods.getRandom(0f, ABS_DRIFT) * ConstVals.PPM
        if (left) drift *= -1f
        if (background) drift *= BACKGROUND_SCALAR

        var minFreq = MIN_FREQUENCY * ConstVals.PPM
        if (background) minFreq *= BACKGROUND_SCALAR

        var maxFreq = MAX_FREQUENCY * ConstVals.PPM
        if (background) maxFreq *= BACKGROUND_SCALAR

        var minAmpl = MIN_AMPLITUDE * ConstVals.PPM
        if (background) minAmpl *= BACKGROUND_SCALAR

        var maxAmpl = MAX_AMPLITUDE * ConstVals.PPM
        if (background) maxAmpl *= BACKGROUND_SCALAR

        val snow = MegaEntityFactory.fetch(Snow::class)!!
        snow.spawn(
            props(
                ConstKeys.SPEED pairTo speed,
                ConstKeys.DRIFT pairTo drift,
                ConstKeys.POSITION pairTo spawnPos,
                ConstKeys.BACKGROUND pairTo background,
                "${ConstKeys.MIN}_${ConstKeys.Y}" pairTo minY,
                "${ConstKeys.MIN}_${ConstKeys.FREQUENCY}" pairTo minFreq,
                "${ConstKeys.MAX}_${ConstKeys.FREQUENCY}" pairTo maxFreq,
                "${ConstKeys.MIN}_${ConstKeys.AMPLITUDE}" pairTo minAmpl,
                "${ConstKeys.MAX}_${ConstKeys.AMPLITUDE}" pairTo maxAmpl,
            )
        )
    }

    private fun resetSpawnDelay() {
        val random = UtilMethods.getRandom(MIN_SPAWN_DELAY, MAX_SPAWN_DELAY)
        spawnDelay.resetDuration(random)
    }

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
