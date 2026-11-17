package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.interpolate
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.toVector2
import com.maverick.game.engine.common.time.Timer
import kotlin.math.min

class CameraRoomTransition(
    var distanceOnTransition: Float,
    transDelay: Float,
    transDuration: Float
) : ICameraPart {

    companion object {
        const val TAG = "CameraRoomTransition"
    }

    var onBegin: (() -> Unit)? = null
    var onContinue: ((Float) -> Unit)? = null
    var onEnd: (() -> Unit)? = null

    private val delayTimer = Timer(transDelay)
    private val transTimer = Timer(transDuration)

    private val start = Vector2()
    private val target = Vector2()

    private val focusStart = Vector2()
    private val focusTarget = Vector2()

    private val position = Vector2()

    private var state: ProcessState? = null

    val transitioning: Boolean
        get() = state != null

    var axisOnly = false
        private set

    fun getTransitionInterpolation(out: Vector2): Vector2? =
        if (state == null) null else interpolate(focusStart, focusTarget, transTimer.getRatio(), out)

    fun start(
        camera: Camera,
        next: Rectangle,
        direction: Direction?,
        subjectCenter: Vector2,
        axisOnly: Boolean = false
    ) {
        // TODO: a vertical axis-only pan needs the y branch in the players; phase 3 of the shared screen (item 047)
        if (axisOnly && direction != Direction.LEFT && direction != Direction.RIGHT) throw IllegalArgumentException(
            "An axis-only room transition supports only LEFT or RIGHT; direction=$direction"
        )

        state = ProcessState.BEGIN
        this.axisOnly = axisOnly

        camera.position.toVector2(start)
        target.set(start)

        focusStart.set(subjectCenter)
        focusTarget.set(focusStart)

        when (direction) {
            Direction.LEFT -> {
                target.x = (next.x + next.width) - min(next.width / 2f, camera.viewportWidth / 2f)
                focusTarget.x = (next.x + next.width) - distanceOnTransition
            }
            Direction.RIGHT -> {
                target.x = next.x + min(next.width / 2f, camera.viewportWidth / 2f)
                focusTarget.x = next.x + distanceOnTransition
            }
            Direction.UP -> {
                target.y = next.y + min(next.height / 2f, camera.viewportHeight / 2f)
                focusTarget.y = next.y + distanceOnTransition
            }
            Direction.DOWN -> {
                target.y = (next.y + next.height) - min(next.height / 2f, camera.viewportHeight / 2f)
                focusTarget.y = (next.y + next.height) - distanceOnTransition
            }
            null -> {}
        }
    }

    fun cancel() {
        state = null
        axisOnly = false
    }

    override fun update(camera: Camera, delta: Float): Boolean {
        val current = state ?: return false

        when (current) {
            ProcessState.END -> {
                GameLogger.debug(TAG, "update(): transition target = $target")
                state = null
                axisOnly = false

                delayTimer.reset()
                transTimer.reset()

                start.setZero()
                target.setZero()

                onEnd?.invoke()
            }
            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                if (current == ProcessState.BEGIN) {
                    onBegin?.invoke()
                    GameLogger.debug(TAG, "update(): transition start = $start")
                } else onContinue?.invoke(delta)

                state = ProcessState.CONTINUE

                delayTimer.update(delta)
                if (!delayTimer.isFinished()) return true
                transTimer.update(delta)

                interpolate(start, target, transTimer.getRatio(), position)
                camera.position.x = position.x
                camera.position.y = position.y

                state = if (transTimer.isFinished()) ProcessState.END else ProcessState.CONTINUE
            }
        }

        return true
    }
}
