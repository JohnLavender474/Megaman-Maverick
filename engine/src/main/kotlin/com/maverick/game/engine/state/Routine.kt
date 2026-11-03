package com.maverick.game.engine.state

import com.badlogic.gdx.utils.Array

class Routine<C>(private val steps: Array<Step<C>>) {

    val finished: Boolean
        get() = index >= steps.size

    private var index = 0
    private var started = false

    fun update(context: C) {
        while (!finished) {
            val step = steps[index]

            if (!started) {
                step.begin(context)
                started = true
            }

            if (!step.isFinished(context)) return

            step.end(context, false)
            started = false
            index++
        }
    }

    fun cancel(context: C) {
        if (started) {
            steps[index].end(context, true)
            started = false
        }

        index = steps.size
    }
}
