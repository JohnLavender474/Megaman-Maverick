package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
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
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.ElecBall
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.max
import kotlin.math.min

class ElecDevil(game: MegamanMaverickGame) : AbstractBoss(game), IStateable<ElecDevilState> {

    companion object {
        const val TAG = "ElecDevil"

        internal const val INIT_DUR = 1f
        internal const val APPEAR_DUR = 0.5f
        internal const val STAND_DUR = 0.5f
        internal const val CHARGE_DUR = 1f
        internal const val HAND_DUR = 1.5f

        internal const val EYE_SHOT_DELAY_NORMAL = 0.5f
        internal const val EYE_SHOT_DELAY_HARD = 0.3f

        internal const val MAX_LAUNCH_DELAY_NORMAL = 0.6f
        internal const val MIN_LAUNCH_DELAY_NORMAL = 0.45f
        internal const val MAX_LAUNCH_DELAY_HARD = 0.5f
        internal const val MIN_LAUNCH_DELAY_HARD = 0.375f

        internal const val MAX_RANDOM_LAUNCH_DELAY_NORMAL = 0.7f
        internal const val MIN_RANDOM_LAUNCH_DELAY_NORMAL = 0.55f
        internal const val MAX_RANDOM_LAUNCH_DELAY_HARD = 0.6f
        internal const val MIN_RANDOM_LAUNCH_DELAY_HARD = 0.475f

        internal const val MIN_HEALTH_FOR_LAUNCH_OFFSET = 5f

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

    // LAUNCH, APPEAR, STAND, CHARGE, HAND, TURN_TO_PIECES
    private val loop = Loop(
        ElecDevilState.LAUNCH,
        ElecDevilState.APPEAR,
        ElecDevilState.STAND,
        ElecDevilState.CHARGE,
        ElecDevilState.STAND,
        ElecDevilState.HAND,
        ElecDevilState.STAND,
        ElecDevilState.TURN_TO_PIECES
    )
    private lateinit var stateTimers: OrderedMap<ElecDevilState, Timer>

    private val initTimer = Timer(INIT_DUR)

    private val launchQueue = Queue<ElecDevilLaunchQueueElement>()
    private val launchedPieces = Array<ElecDevilBodyPiece>()
    private val launchDelayTimer = Timer()
    private var launches = 0

    private lateinit var lightSourceKeys: ObjectSet<Int>

    private var startX = 0f

    private val reusableIntMap = OrderedMap<Int, OrderedSet<Int>>()

    private lateinit var eye: IGameShape2D
    private lateinit var hand: IGameShape2D

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        damageOverrides.put(Axe::class, dmgNeg(2))
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

        stateTimers = orderedMapOf(
            ElecDevilState.APPEAR pairTo Timer(APPEAR_DUR),
            ElecDevilState.STAND pairTo Timer(STAND_DUR),
            ElecDevilState.CHARGE pairTo Timer(CHARGE_DUR).setRunOnFinished { shootFromEye() },
            ElecDevilState.HAND pairTo Timer(HAND_DUR).also { timer ->
                val delay = if (game.state.getDifficultyMode() == DifficultyMode.HARD)
                    EYE_SHOT_DELAY_HARD else EYE_SHOT_DELAY_NORMAL

                val shots = HAND_DUR.div(delay).toInt()

                for (i in 1..shots) {
                    val time = i * delay
                    val runnable = TimeMarkedRunnable(time) { shootFromEye() }
                    timer.addRunnable(runnable)
                }
            },
            ElecDevilState.TURN_TO_PIECES pairTo Timer(TURN_TO_PIECES_DUR)
        )

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

        launches = 0

        initTimer.reset()
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun onReady() {
        super.onReady()

        setBothBodiesInactive()
        setBodyPiecesActive(true)

        loadLaunchQueue(true)
        resetLaunchDelay()

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

    override fun spawnDefeatExplosion() {
        val position = Position.entries.random()

        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.POSITION pairTo spawn.add(
                    (position.x - 1) * ConstVals.PPM.toFloat(),
                    (position.y - 1) * ConstVals.PPM.toFloat()
                ),
                ConstKeys.DAMAGER pairTo false
            )
        )
    }

    override fun spawnExplosionOrbs(spawn: Vector2) {
        GameLogger.debug(TAG, "spawnExplosionOrbs()")
        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }
        super.spawnExplosionOrbs(spawn)
    }

    override fun disintegrate(disintegrationProps: Properties?) {
        GameLogger.debug(TAG, "disintegrate()")
        val spawn = when {
            leftBody!!.on -> leftBody!!.body.getCenter()
            rightBody!!.on -> rightBody!!.body.getCenter()
            leftBodyPieces!!.on -> leftBodyPieces!!.body.getCenter()
            else -> rightBodyPieces!!.body.getCenter()
        }
        super.disintegrate(props(ConstKeys.POSITION pairTo spawn))
    }

    override fun getCurrentState() = loop.getCurrent()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) {
                if (megaman.body.isSensing(BodySense.FEET_ON_GROUND)) initTimer.update(delta)
                return@add
            }

            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

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

                        requestToPlaySound(SoundAsset.ELECTRIC_2_SOUND, false)

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
                        startBodyPieces.setStateOfPiece(startRow, startColumn, ElecDevilBodyPieceState.INACTIVE)

                        val (targetRow, targetColumn) = target
                        targetBodyPieces.setStateOfPiece(targetRow, targetColumn, ElecDevilBodyPieceState.STANDBY)

                        val onEnd: () -> Unit = {
                            GameLogger.debug(
                                TAG,
                                "update(): targetBodyPieces.setStateOfPiece: target=$target, state=true"
                            )

                            targetBodyPieces.setStateOfPiece(targetRow, targetColumn, ElecDevilBodyPieceState.ACTIVE)
                        }

                        val speed = UtilMethods.interpolate(
                            ElecDevilConstants.PIECE_MAX_SPEED,
                            ElecDevilConstants.PIECE_MIN_SPEED,
                            getHealthRatio()
                        ) * ConstVals.PPM

                        val bodyPiece = MegaEntityFactory.fetch(ElecDevilBodyPiece::class)!!
                        bodyPiece.spawn(
                            props(
                                ConstKeys.END pairTo onEnd,
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.SPEED pairTo speed,
                                ConstKeys.START pairTo startPosition,
                                ConstKeys.TARGET pairTo targetPosition,
                                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
                            )
                        )

                        launchedPieces.add(bodyPiece)

                        resetLaunchDelay()

                        requestToPlaySound(SoundAsset.ELECTRIC_1_SOUND, false)
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
                            resetLaunchDelay()

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

                            startBodyPieces.setStateOfAllPieces(ElecDevilBodyPieceState.ACTIVE)
                            targetBodyPieces.setStateOfAllPieces(ElecDevilBodyPieceState.INACTIVE)
                        }

                        timer.reset()
                    }
                }
            }
        }
    }

    // No need for this entity to have a body. Contacts will be made from the "body" and "pieces" entities.
    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        eye = GameCircle().setRadius(0.5f * ConstVals.PPM)
        val eyeFixture = Fixture(body, FixtureType.DAMAGEABLE, eye)
        eyeFixture.attachedToBody = false
        body.addFixture(eyeFixture)
        debugShapes.add { eyeFixture }

        body.preProcess.put(ConstKeys.DAMAGEABLE) {
            val active = getCurrentState().equalsAny(ElecDevilState.STAND, ElecDevilState.CHARGE, ElecDevilState.HAND)
            eyeFixture.setActive(active)

            eyeFixture.drawingColor = if (active) Color.PURPLE else Color.GRAY

            val center = when {
                leftBody!!.on -> leftBody!!.body.getCenter().add(-0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
                else -> rightBody!!.body.getCenter().add(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
            }
            eye.setCenter(center)
        }

        hand = GameRectangle().setSize(1.5f * ConstVals.PPM)
        debugShapes.add { hand }

        val handShieldFixture = Fixture(body, FixtureType.SHIELD, hand)
        handShieldFixture.attachedToBody = false
        body.addFixture(handShieldFixture)

        val handDamagerFixture = Fixture(body, FixtureType.DAMAGER, hand)
        handDamagerFixture.attachedToBody = false
        body.addFixture(handDamagerFixture)

        body.preProcess.put(ConstKeys.HAND) {
            val active = getCurrentState() == ElecDevilState.HAND
            hand.drawingColor = if (active) Color.YELLOW else Color.GRAY
            handShieldFixture.setActive(active)
            handDamagerFixture.setActive(active)

            val center = when {
                leftBody!!.on -> leftBody!!.body.getCenter().add(2.5f * ConstVals.PPM, 1.25f * ConstVals.PPM)
                else -> rightBody!!.body.getCenter().add(-2.5f * ConstVals.PPM, 1.25f * ConstVals.PPM)
            }
            hand.setCenter(center)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    // No sprites for this entity. Sprites will be shown from the "body" and "pieces" entities.
    override fun defineSpritesComponent() = SpritesComponent()

    private fun resetLaunchDelay() {
        val numerator = max(0f, getCurrentHealth() - MIN_HEALTH_FOR_LAUNCH_OFFSET)
        val ratio = min(1f, numerator / ConstVals.MAX_HEALTH)

        val hard = game.state.getDifficultyMode() == DifficultyMode.HARD

        val min: Float
        val max: Float
        if (shouldRandomizeLaunchTargets()) {
            min = if (hard) MIN_RANDOM_LAUNCH_DELAY_HARD else MIN_RANDOM_LAUNCH_DELAY_NORMAL
            max = if (hard) MAX_RANDOM_LAUNCH_DELAY_HARD else MAX_RANDOM_LAUNCH_DELAY_NORMAL
        } else {
            min = if (hard) MIN_LAUNCH_DELAY_HARD else MIN_LAUNCH_DELAY_NORMAL
            max = if (hard) MAX_LAUNCH_DELAY_HARD else MAX_LAUNCH_DELAY_NORMAL
        }

        val duration = UtilMethods.interpolate(min, max, ratio)
        launchDelayTimer.resetDuration(duration)

        // game.setDebugText(UtilMethods.roundFloat(duration, 2).toString())
    }

    private fun shouldRandomizeLaunchTargets() = launches % 3 == 0

    private fun loadLaunchQueue(firstLaunch: Boolean) {
        launches++

        val startBody: ElecDevilBody
        val targetBody: ElecDevilBody

        if (fromLeft) {
            startBody = leftBody!!
            targetBody = rightBody!!
        } else {
            startBody = rightBody!!
            targetBody = leftBody!!
        }

        val randomTargetRows = shouldRandomizeLaunchTargets()

        val columnRowMap = when {
            randomTargetRows -> ElecDevilConstants.fillMapWithColumnsAsKeys(reusableIntMap)
            else -> null
        }

        ElecDevilConstants.getPieceQueue().forEach { (row, column) ->
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
            val targetPosition = targetBody.getPositionOf(targetRow, targetColumn).add(xOffset, yOffset)

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

    private fun shootFromEye() {
        GameLogger.debug(TAG, "shootFromEye()")

        val target = megaman.body.getCenter()
        val targetXDelta = UtilMethods.getRandom(-0.5f, 0.5f) * ConstVals.PPM
        val targetYDelta = UtilMethods.getRandom(-0.5f, 0.5f) * ConstVals.PPM
        target.add(targetXDelta, targetYDelta)

        val elecBall = MegaEntityFactory.fetch(ElecBall::class)!!
        elecBall.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.TARGET pairTo target,
                ConstKeys.POSITION pairTo eye.getCenter(),
                "${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}" pairTo lightSourceKeys
            )
        )

        requestToPlaySound(SoundAsset.VOLT_SOUND, false)
    }

    internal fun isDamageBlinking() = damageBlink

    override fun getTag() = TAG
}
