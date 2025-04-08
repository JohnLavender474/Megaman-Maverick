package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.setX
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IStateable
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.forEachCell
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator

class ElecDevil(game: MegamanMaverickGame) : AbstractBoss(game), IStateable<ElecDevilState> {

    companion object {
        const val TAG = "ElecDevil"

        internal const val APPEAR_DUR = 0.5f
        internal const val STAND_DUR = 1f
        internal const val CHARGE_DUR = 1f
        internal const val HAND_DUR = 1.5f
        internal const val HAND_SHOT_DELAY = 0.5f
        internal const val LAUNCH_DELAY = 0.5f
        internal const val TURN_TO_PIECES_DUR = 1f

        private const val FROM_LEFT = "${ConstKeys.FROM}_${ConstKeys.LEFT}"
    }

    private data class ElecDevilPieceQueueElement(val position: IntPair, val start: Vector2, val target: Vector2)

    private var leftBody: ElecDevilBody? = null
    private var leftBodyPieces: ElecDevilBodyPieces? = null

    private var rightBody: ElecDevilBody? = null
    private var rightBodyPieces: ElecDevilBodyPieces? = null

    // if true, then the left body should be active, and pieces should come from the left, and vice versa if false
    private var fromLeft = true

    private val loop = Loop(ElecDevilState.entries.toGdxArray())
    private val stateTimers = orderedMapOf(
        ElecDevilState.APPEAR pairTo Timer(APPEAR_DUR),
        ElecDevilState.STAND pairTo Timer(STAND_DUR).setRunOnFirstupdate { spawnBodyEye() },
        ElecDevilState.CHARGE pairTo Timer(CHARGE_DUR).setRunOnFinished { shootFromBodyEye() },
        ElecDevilState.HAND pairTo Timer(HAND_DUR).also { timer ->
            val shots = HAND_DUR.div(HAND_SHOT_DELAY).toInt()
            for (i in 1..shots) {
                val time = i * HAND_SHOT_DELAY
                val runnable = TimeMarkedRunnable(time) { shootFromHand() }
                timer.addRunnable(runnable)
            }
        },
        ElecDevilState.TURN_TO_PIECES pairTo Timer(TURN_TO_PIECES_DUR)
            .setRunOnFirstupdate { destroyBodyEye() }
            .setRunOnFinished { loadLaunchQueue() }
    )

    private val launchPositionQueue = Queue<ElecDevilPieceQueueElement>()
    private val launchDelay = Timer(LAUNCH_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
    }

    // On spawn, both bodies should be inactive, all body pieces should be inactive, and body pieces should be queued
    // to spawn from off-screen via the "start" object property.
    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val room = spawnProps.get(ConstKeys.ROOM, RectangleMapObject::class)!!.rectangle
        body.set(room)

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        val leftPos = spawnProps.get(
            ConstKeys.LEFT, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)

        val leftBody = MegaEntityFactory.fetch(ElecDevilBody::class)!!
        leftBody.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo leftPos,
                ConstKeys.FACING pairTo Facing.RIGHT
            )
        )
        this.leftBody = leftBody

        val leftBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        leftBodyPieces.spawn(props(ConstKeys.OWNER pairTo leftBody, ConstKeys.ACTIVE pairTo false))
        this.leftBodyPieces = leftBodyPieces

        val rightPos = spawnProps.get(
            ConstKeys.RIGHT, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)

        val rightBody = MegaEntityFactory.fetch(ElecDevilBody::class)!!
        rightBody.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo rightPos,
                ConstKeys.FACING pairTo Facing.LEFT
            )
        )
        this.rightBody = rightBody

        val rightBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        rightBodyPieces.spawn(props(ConstKeys.OWNER pairTo rightBody, ConstKeys.ACTIVE pairTo false))
        this.rightBodyPieces = rightBodyPieces

        fromLeft = spawnProps.get(FROM_LEFT, Boolean::class)!!

        val startX = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter().x
        ElecDevilConstants.fillPieceQueue(fromLeft).forEach { (row, column) ->
            val start = leftBody.getPositionOf(row, column).setX(startX)
            val target = rightBody.getPositionOf(row, column)
            launchPositionQueue.addLast(ElecDevilPieceQueueElement(row pairTo column, start, target))
        }

        launchDelay.reset()

        GameLogger.debug(
            TAG,
            "onSpawn(): " +
                "leftBody=${leftBody.hashCode()}, " +
                "rightBody=${rightBody.hashCode()}, " +
                "leftBodyPieces=${leftBodyPieces.hashCode()}, " +
                "rightBodyPieces=${rightBodyPieces.hashCode()}, " +
                "spawnProps=$spawnProps"
        )
    }

    override fun isReady(delta: Float) = true

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        launchPositionQueue.clear()

        leftBody?.destroy()
        leftBody = null

        leftBodyPieces?.destroy()
        leftBodyPieces = null

        rightBody?.destroy()
        rightBody = null

        rightBodyPieces?.destroy()
        rightBodyPieces = null
    }

    override fun getCurrentState() = loop.getCurrent()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (getCurrentState()) {
                // During the "launch" state, both bodies should be inactive (hidden) and all body pieces should be
                // active. The "start" body pieces entity should have its pieces hidden one by one as they're launched,
                // while the "target" entity should have its pieces shown as each projectile hits its target.
                ElecDevilState.LAUNCH -> {
                    setBothBodiesInactive()
                    setBodyPiecesActive(true)

                    if (launchPositionQueue.isEmpty) {
                        fromLeft = !fromLeft

                        val next = loop.next()
                        GameLogger.debug(TAG, "update(): next=$next")

                        return@add
                    }

                    launchDelay.update(delta)
                    if (launchDelay.isFinished()) {
                        val (position, start, target) = launchPositionQueue.removeFirst()
                        val (row, column) = position

                        val startBodyPieces: ElecDevilBodyPieces
                        val targetBodyPieces: ElecDevilBodyPieces

                        if (fromLeft) {
                            startBodyPieces = leftBodyPieces!!
                            targetBodyPieces = rightBodyPieces!!
                        } else {
                            startBodyPieces = rightBodyPieces!!
                            targetBodyPieces = leftBodyPieces!!
                        }

                        startBodyPieces.setStateOfPiece(row, column, false)

                        val onEnd: () -> Unit = { targetBodyPieces.setStateOfPiece(row, column, true) }

                        val bodyPiece = MegaEntityFactory.fetch(ElecDevilBodyPiece::class)!!
                        bodyPiece.spawn(
                            props(
                                ConstKeys.END pairTo onEnd,
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.START pairTo start,
                                ConstKeys.TARGET pairTo target
                            )
                        )
                    }
                }

                // For all other states, update the state's corresponding timer. If the timer is finished and the next
                // state is "launch", then set up the initial "active" values for the "start" and "target" entities.
                else -> {
                    setOneBodyActive()
                    setBodyPiecesActive(false)

                    val timer = stateTimers[getCurrentState()]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        val next = loop.next()
                        GameLogger.debug(TAG, "update(): next=$next")

                        if (next == ElecDevilState.LAUNCH) {
                            val startBodyPieces: ElecDevilBodyPieces
                            val targetBodyPieces: ElecDevilBodyPieces

                            if (fromLeft) {
                                startBodyPieces = leftBodyPieces!!
                                targetBodyPieces = rightBodyPieces!!
                            } else {
                                startBodyPieces = rightBodyPieces!!
                                targetBodyPieces = leftBodyPieces!!
                            }

                            GameLogger.debug(
                                TAG,
                                "update(): set body pieces active states: " +
                                    "startBodyPieces=${startBodyPieces.hashCode()}, " +
                                    "targetBodyPieces=${targetBodyPieces.hashCode()}"
                            )

                            startBodyPieces.setStateOfAllPieces(true)
                            targetBodyPieces.setStateOfAllPieces(false)
                        }

                        timer.reset()
                    }
                }
            }
        }
    }

    // No need for this entity to have a body. Contacts will be made from the "body" and "pieces" entities.
    override fun defineBodyComponent() = BodyComponentCreator.create(this, Body(BodyType.ABSTRACT))

    // No sprites for this entity. Sprites will be shown from the "body" and "pieces" entities.
    override fun defineSpritesComponent() = SpritesComponent()

    private fun loadLaunchQueue() {
        val startBody: ElecDevilBody
        val targetBody: ElecDevilBody

        if (fromLeft) {
            startBody = leftBody!!
            targetBody = rightBody!!
        } else {
            startBody = rightBody!!
            targetBody = leftBody!!
        }

        forEachCell { row, column ->
            val start = startBody.getPositionOf(row, column)
            val target = targetBody.getPositionOf(row, column)
            launchPositionQueue.addLast(ElecDevilPieceQueueElement(row pairTo column, start, target))
        }

        GameLogger.debug(
            TAG,
            "loadLaunchQueue(): " +
                "startBody=${startBody.hashCode()}, " +
                "targetBody=${targetBody.hashCode()}, " +
                "launchPositionQueue=$launchPositionQueue"
        )
    }

    private fun setOneBodyActive() {
        val activeBody: ElecDevilBody
        val inactiveBody: ElecDevilBody

        if (fromLeft) {
            activeBody = leftBody!!
            inactiveBody = rightBody!!
        } else {
            activeBody = rightBody!!
            inactiveBody = leftBody!!
        }

        activeBody.on = true
        inactiveBody.on = false
    }

    private fun setBothBodiesInactive() {
        leftBody!!.on = false
        rightBody!!.on = false
    }

    private fun setBodyPiecesActive(active: Boolean) {
        leftBodyPieces!!.on = active
        rightBodyPieces!!.on = active
    }

    private fun spawnBodyEye() {
        GameLogger.debug(TAG, "spawnBodyEye()")
        // TODO
    }

    private fun shootFromBodyEye() {
        GameLogger.debug(TAG, "shootFromEye()")
        // TODO
    }

    private fun destroyBodyEye() {
        GameLogger.debug(TAG, "destroyBodyEye()")
        // TODO
    }

    private fun shootFromHand() {
        GameLogger.debug(TAG, "shootFromHand()")
        // TODO
    }
}
