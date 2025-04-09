package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IStateable
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator

class ElecDevil(game: MegamanMaverickGame) : AbstractBoss(game), IStateable<ElecDevilState> {

    companion object {
        const val TAG = "ElecDevil"

        internal const val INIT_DUR = 0.5f

        internal const val APPEAR_DUR = 0.5f

        internal const val STAND_DUR = 1f

        internal const val CHARGE_DUR = 1f

        internal const val HAND_DUR = 1.5f
        internal const val HAND_SHOT_DELAY = 0.5f

        internal const val MIN_LAUNCH_DELAY = 0.25f
        internal const val MAX_LAUNCH_DELAY = 0.75f

        internal const val TURN_TO_PIECES_DUR = 1f

        private const val FROM_LEFT = "${ConstKeys.FROM}_${ConstKeys.LEFT}"
    }

    private data class ElecDevilLaunchQueueElement(
        val start: IntPair,
        val target: IntPair,
        val startPosition: Vector2,
        val targetPosition: Vector2
    )

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
        ElecDevilState.TURN_TO_PIECES pairTo Timer(TURN_TO_PIECES_DUR).setRunOnFirstupdate { destroyBodyEye() }
    )
    private val initTimer = Timer(INIT_DUR)

    private val launchDelayTimer = Timer(MAX_LAUNCH_DELAY)
    private val launchedPieces = Array<ElecDevilBodyPiece>()
    private val launchQueue = Queue<ElecDevilLaunchQueueElement>()
    private var launches = 0

    private lateinit var lightSourceKeys: ObjectSet<Int>

    private var startX = 0f

    private val reusableIntMap = OrderedMap<Int, OrderedSet<Int>>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
    }

    // On spawn, both bodies should be inactive, all body pieces should be inactive, and body pieces should be queued
    // to spawn from off-screen via the "start" object property.
    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC.name)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val room = spawnProps.get(ConstKeys.ROOM, RectangleMapObject::class)!!.rectangle
        body.set(room)

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        lightSourceKeys = spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}", String::class)!!
            .replace("\\s+", "")
            .split(",")
            .filter { it.isNotBlank() }
            .map { it.toInt() }
            .toObjectSet()

        val leftPos = spawnProps.get(
            ConstKeys.LEFT, RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)

        val leftBody = MegaEntityFactory.fetch(ElecDevilBody::class)!!
        leftBody.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo leftPos,
                ConstKeys.FACING pairTo Facing.RIGHT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.leftBody = leftBody

        val leftBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        leftBodyPieces.spawn(
            props(
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.OWNER pairTo leftBody,
                ConstKeys.FACING pairTo Facing.RIGHT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
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
                ConstKeys.FACING pairTo Facing.LEFT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.rightBody = rightBody

        val rightBodyPieces = MegaEntityFactory.fetch(ElecDevilBodyPieces::class)!!
        rightBodyPieces.spawn(
            props(
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.OWNER pairTo rightBody,
                ConstKeys.FACING pairTo Facing.LEFT,
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )
        this.rightBodyPieces = rightBodyPieces

        fromLeft = spawnProps.get(FROM_LEFT, Boolean::class)!!

        startX = spawnProps.get(
            "${ConstKeys.START}_${ConstKeys.X}", RectangleMapObject::class
        )!!.rectangle.getCenter().x

        launchDelayTimer.resetDuration(MAX_LAUNCH_DELAY)
        launches = 0

        initTimer.reset()
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun onReady() {
        super.onReady()

        setBothBodiesInactive()
        setBodyPiecesActive(true)

        loadLaunchQueue(true)
        launches++

        GameLogger.debug(
            TAG,
            "onReady():\n" +
                "\tleftBody=${leftBody},\n" +
                "\trightBody=${rightBody},\n" +
                "\tleftBodyPieces=${leftBodyPieces},\n" +
                "\trightBodyPieces=${rightBodyPieces},\n" +
                "\tlaunchPositionQueue=" + if (launchQueue.isEmpty) "[]" else "\n\t\t${launchQueue.toString("\n\t\t")}"
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        launchQueue.clear()

        launchedPieces.forEach { it.destroy() }
        launchedPieces.clear()

        reusableIntMap.clear()

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
            game.setDebugText("body active: ${isBodyActive()}")

            initTimer.update(delta)

            if (!ready) return@add

            val launchedPieceIter = launchedPieces.iterator()
            while (launchedPieceIter.hasNext()) {
                val launchedPiece = launchedPieceIter.next()
                if (launchedPiece.dead) launchedPieceIter.remove()
            }

            when (getCurrentState()) {
                // During the "launch" state, both bodies should be inactive (hidden) and all body pieces should be
                // active. The "start" body pieces entity should have its pieces hidden one by one as they're launched,
                // while the "target" entity should have its pieces shown as each projectile hits its target.
                ElecDevilState.LAUNCH -> {
                    if (launchQueue.isEmpty && launchedPieces.isEmpty) {
                        fromLeft = !fromLeft

                        val old = getCurrentState()
                        val next = loop.next()
                        GameLogger.debug(TAG, "update(): old=$old, next=$next")

                        setOneBodyActive()
                        setBodyPiecesActive(false)

                        val launchDelay = UtilMethods.interpolate(MIN_LAUNCH_DELAY, MAX_LAUNCH_DELAY, getHealthRatio())
                        launchDelayTimer.resetDuration(launchDelay)

                        return@add
                    }

                    launchDelayTimer.update(delta)
                    if (launchDelayTimer.isFinished() && !launchQueue.isEmpty) {
                        val (start, target, startPosition, targetPosition) = launchQueue.removeFirst()

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
                            "update(): startBodyPieces.setStateOfPiece: start=$start, state=false"
                        )

                        val (startRow, startColumn) = start
                        startBodyPieces.setStateOfPiece(startRow, startColumn, false)

                        val onEnd: () -> Unit = {
                            GameLogger.debug(
                                TAG,
                                "update(): targetBodyPieces.setStateOfPiece: target=$target, state=true"
                            )

                            val (targetRow, targetColumn) = target
                            targetBodyPieces.setStateOfPiece(targetRow, targetColumn, true)
                        }

                        val bodyPiece = MegaEntityFactory.fetch(ElecDevilBodyPiece::class)!!
                        bodyPiece.spawn(
                            props(
                                ConstKeys.END pairTo onEnd,
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.START pairTo startPosition,
                                ConstKeys.TARGET pairTo targetPosition,
                                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
                            )
                        )

                        launchedPieces.add(bodyPiece)

                        val launchDelay = UtilMethods.interpolate(MIN_LAUNCH_DELAY, MAX_LAUNCH_DELAY, getHealthRatio())
                        launchDelayTimer.resetDuration(launchDelay)
                    }
                }

                // For all other states, update the state's corresponding timer. If the timer is finished and the next
                // state is "launch", then set up the initial "active" values for the "start" and "target" entities.
                else -> {
                    val timer = stateTimers[getCurrentState()]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        val old = getCurrentState()
                        val next = loop.next()
                        GameLogger.debug(TAG, "update(): old=$old, next=$next")

                        if (next == ElecDevilState.LAUNCH) {
                            loadLaunchQueue(false)
                            launches++

                            setBothBodiesInactive()
                            setBodyPiecesActive(true)

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
                                    "startBodyPieces=${startBodyPieces.facing}, " +
                                    "targetBodyPieces=${targetBodyPieces.facing}"
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

    private fun loadLaunchQueue(firstLaunch: Boolean) {
        val startBody: ElecDevilBody
        val targetBody: ElecDevilBody

        if (fromLeft) {
            startBody = leftBody!!
            targetBody = rightBody!!
        } else {
            startBody = rightBody!!
            targetBody = leftBody!!
        }

        val randomTargetRows = launches != 0 && launches % 3 == 0

        val columnRowMap = when {
            randomTargetRows -> ElecDevilConstants.fillMapWithColumnsAsKeys(reusableIntMap)
            else -> null
        }

        ElecDevilConstants.fillPieceQueue(fromLeft).forEach { (row, column) ->
            var xOffset = ElecDevilConstants.PIECE_WIDTH * ConstVals.PPM / 2f
            val yOffset = ElecDevilConstants.PIECE_HEIGHT * ConstVals.PPM / 2f

            val start = row pairTo column
            val startPosition = startBody.getPositionOf(row, column)
            when {
                firstLaunch -> startPosition.add(0f, yOffset).setX(startX)
                else -> startPosition.add(xOffset, yOffset)
            }

            val target = when {
                randomTargetRows -> {
                    val targetColumn = columnRowMap!![column]
                    val targetRow = targetColumn.random()

                    targetColumn.remove(targetRow)
                    if (targetColumn.isEmpty) columnRowMap.remove(column)

                    targetRow pairTo column
                }

                else -> row pairTo column
            }

            val (targetRow, targetColumn) = target
            val targetPosition = targetBody
                .getPositionOf(targetRow, targetColumn)
                .add(xOffset * targetBody.facing.value, yOffset)

            launchQueue.addLast(
                ElecDevilLaunchQueueElement(
                    start,
                    target,
                    startPosition,
                    targetPosition
                )
            )
        }

        if (columnRowMap != null && !columnRowMap.isEmpty) {
            GameLogger.error(TAG, "loadLaunchQueue(): column-row map should be empty but is not: $columnRowMap")
            columnRowMap.clear()
        }

        GameLogger.debug(
            TAG,
            "loadLaunchQueue():\n" +
                "\tstartBody=${startBody.facing},\n" +
                "\ttargetBody=${targetBody.facing},\n" +
                "\tlaunchPositionQueue=\n\t\t${launchQueue.toString("\n\t\t")}"
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

        GameLogger.debug(
            TAG,
            "setOneBodyActive(): activeBody=${activeBody.facing}, inactiveBody=${inactiveBody.facing}"
        )
    }

    private fun setBothBodiesInactive() {
        GameLogger.debug(TAG, "setBothBodiesInactive()")
        leftBody!!.on = false
        rightBody!!.on = false
    }

    private fun setBodyPiecesActive(active: Boolean) {
        GameLogger.debug(TAG, "setBodyPiecesActive(): active=$active")
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

    private fun isBodyActive() = leftBody!!.on || rightBody!!.on
}
