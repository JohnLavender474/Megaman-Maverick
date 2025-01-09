package com.megaman.maverick.game.entities.hazards

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getPosition
import kotlin.math.ceil
import kotlin.math.floor

class RisingLavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), IEventListener {

    companion object {
        const val TAG = "RisingLavaRiver"

        private const val DEFAULT_RISE_SPEED = 1.5f
        private const val DEFAULT_FALL_SPEED = 6f

        private const val STOP_DELAY = 0.5f

        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.003125f

        private const val RISE_SHAKE_DELAY = 3f
        private const val FALL_SHAKE_DELAY = 1f

        private const val RISE_SHAKE_DUR = 1f
        private const val FALL_SHAKE_DUR = 0.5f

        private const val RISE_SHAKE_INTERVAL = 0.1f
        private const val FALL_SHAKE_INTERVAL = 0.1f
    }

    private enum class RisingLavaRiverState { DORMANT, RISING, STOPPED, FALLING }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_READY,
        EventType.PLAYER_DONE_DYIN,
        EventType.BEGIN_ROOM_TRANS,
        EventType.END_ROOM_TRANS
    )

    private val lavaRivers = Matrix<LavaRiver>()
    private val startBounds = GameRectangle()

    // the lava rises only for a single room; for all other rooms the lava should be lowered and dormant
    private lateinit var riseRoom: String

    private lateinit var state: RisingLavaRiverState

    private val shakeDelay = Timer()
    private val stopDelay = Timer(STOP_DELAY)

    private var riseSpeed = 0f
    private var fallSpeed = 0f
    private var left = false

    override fun init() {
        GameLogger.debug(TAG, "init()")

        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        game.eventsMan.addListener(this)

        super.onSpawn(spawnProps)

        state = RisingLavaRiverState.DORMANT

        startBounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        riseRoom = spawnProps.get("${ConstKeys.RISE}_${ConstKeys.ROOM}", String::class)!!

        riseSpeed = spawnProps.getOrDefault("${ConstKeys.RISE}_${ConstKeys.SPEED}", DEFAULT_RISE_SPEED, Float::class)
        fallSpeed = spawnProps.getOrDefault("${ConstKeys.FALL}_${ConstKeys.SPEED}", DEFAULT_FALL_SPEED, Float::class)

        spawnLava()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        game.eventsMan.removeListener(this)

        lavaRivers.forEach { it.destroy() }
        lavaRivers.clear()
    }

    override fun onEvent(event: Event) {
        val key = event.key

        GameLogger.debug(TAG, "onEvent(): event.key=$key")

        when (key) {
            EventType.PLAYER_READY -> {
                val room = game.getCurrentRoom()!!.name
                if (room == riseRoom) setLavaToRising()
            }

            EventType.PLAYER_DONE_DYIN -> setLavaToDormant()

            EventType.BEGIN_ROOM_TRANS -> if (state == RisingLavaRiverState.RISING) {
                val room = game.getCurrentRoom()!!.name
                if (room != riseRoom) {
                    setLavaToStopped()
                    stopDelay.reset()
                }
            }

            EventType.END_ROOM_TRANS -> {
                val room = game.getCurrentRoom()!!.name
                if (room == riseRoom) setLavaToRising()
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            RisingLavaRiverState.RISING -> {
                shakeDelay.update(delta)

                if (shakeDelay.isFinished()) {
                    val position = lavaRivers[0, 0]!!.body.getPosition()

                    GameLogger.debug(TAG, "update(): shake on rise: position=$position")

                    game.eventsMan.submitEvent(
                        Event(
                            EventType.SHAKE_CAM, props(
                                ConstKeys.INTERVAL pairTo RISE_SHAKE_INTERVAL,
                                ConstKeys.DURATION pairTo RISE_SHAKE_DUR,
                                ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                                ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                            )
                        )
                    )

                    shakeDelay.reset()
                }
            }

            RisingLavaRiverState.STOPPED -> {
                stopDelay.update(delta)

                if (stopDelay.isFinished()) {
                    GameLogger.debug(TAG, "update(): stop delay finished")

                    setLavaToFalling()
                }
            }

            RisingLavaRiverState.FALLING -> {
                val maxLavaY = lavaRivers[0, lavaRivers.rows - 1]?.body?.getMaxY()
                val camY = game.getGameCamera().toGameRectangle().getY()
                if (maxLavaY != null && maxLavaY < camY) {
                    GameLogger.debug(TAG, "update(): falling: maxLavaY=$maxLavaY, camY=$camY")
                    setLavaToDormant()
                    return@UpdatablesComponent
                }

                shakeDelay.update(delta)

                if (shakeDelay.isFinished()) {
                    game.eventsMan.submitEvent(
                        Event(
                            EventType.SHAKE_CAM, props(
                                ConstKeys.INTERVAL pairTo FALL_SHAKE_INTERVAL,
                                ConstKeys.DURATION pairTo FALL_SHAKE_DUR,
                                ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                                ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                            )
                        )
                    )

                    shakeDelay.reset()
                }
            }

            else -> {}
        }
    })

    private fun setLavaToRising() {
        GameLogger.debug(TAG, "setLavaToRising()")

        state = RisingLavaRiverState.RISING
        shakeDelay.resetDuration(RISE_SHAKE_DELAY)

        val startX = startBounds.getX()
        val startY = startBounds.getY()

        lavaRivers.forEach { column, row, lava ->
            val x = startX + (column * 2f) * ConstVals.PPM
            val y = startY + row * ConstVals.PPM

            lava?.let {
                it.active = true
                it.hidden = false
                it.body.setPosition(x, y)
                it.body.physics.velocity.set(0f, riseSpeed * ConstVals.PPM)
            }
        }
    }

    private fun setLavaToStopped() {
        GameLogger.debug(TAG, "setLavaToStopped()")

        state = RisingLavaRiverState.STOPPED
        stopDelay.reset()

        lavaRivers.forEach { column, row, lava -> lava?.body?.physics?.velocity?.setZero() }
    }

    private fun setLavaToFalling() {
        GameLogger.debug(TAG, "setLavaToFalling()")

        state = RisingLavaRiverState.FALLING
        shakeDelay.resetDuration(FALL_SHAKE_DELAY)

        lavaRivers.forEach { column, row, lava -> lava?.body?.physics?.velocity?.set(0f, -fallSpeed * ConstVals.PPM) }
    }

    private fun setLavaToDormant() {
        GameLogger.debug(TAG, "setLavaToDormant()")

        state = RisingLavaRiverState.DORMANT

        val startX = startBounds.getX()
        val startY = startBounds.getY()

        lavaRivers.forEach { column, row, lava ->
            val x = startX + (column * 2f) * ConstVals.PPM
            val y = startY + row * ConstVals.PPM

            lava?.let {
                it.hidden = true
                it.active = false
                it.body.setPosition(x, y)
                it.body.physics.velocity.setZero()
            }
        }
    }

    private fun spawnLava() {
        val rows = (startBounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (startBounds.getWidth() / ConstVals.PPM).toInt()

        lavaRivers.clear()
        lavaRivers.rows = rows
        lavaRivers.columns = ceil(columns / 2f).toInt()

        val startX = startBounds.getX()
        val startY = startBounds.getY()

        GameLogger.debug(TAG, "spawnLava(): rows=$rows, columns=$columns, startX=$startX, startY=$startY")

        var row = 0
        while (row < rows) {
            val top = row == rows - 1

            var column = 0
            while (column < columns) {
                val x = startX + column * ConstVals.PPM
                val y = startY + row * ConstVals.PPM

                val bounds = GameObjectPools.fetch(GameRectangle::class)
                    .set(x, y, 2f * ConstVals.PPM, ConstVals.PPM.toFloat())

                val type = when {
                    top -> LavaRiver.TOP
                    else -> LavaRiver.INNER
                }

                val lavaRiver = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.LAVA_RIVER)!! as LavaRiver
                lavaRiver.spawn(
                    props(
                        ConstKeys.LEFT pairTo left,
                        ConstKeys.TYPE pairTo type,
                        ConstKeys.HIDDEN pairTo true,
                        ConstKeys.ACTIVE pairTo false,
                        ConstKeys.BOUNDS pairTo bounds,
                        "${ConstKeys.OWN}_${ConstKeys.CULL}" pairTo false
                    )
                )

                lavaRivers[floor(column / 2f).toInt(), row] = lavaRiver

                column += 2
            }

            row++
        }
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
