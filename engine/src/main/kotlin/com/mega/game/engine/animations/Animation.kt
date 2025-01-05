package com.mega.game.engine.animations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxFilledArrayOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.drawables.sprites.splitAndFlatten

class Animation : IAnimation {

    companion object {

        fun of(entries: Iterable<GamePair<TextureRegion, Float>>, loop: Boolean): Animation {
            val frames = Array<TextureRegion>()
            val durations = Array<Float>()

            entries.forEach { (frame, duration) ->
                frames.add(frame)
                durations.add(duration)
            }

            return Animation(frames = frames, durations = durations, loop = loop)
        }
    }

    internal val frames: Array<TextureRegion>
    internal val durations: Array<Float>
    internal var currentIndex = 0
        private set
    internal var elapsedTime = 0f
        private set

    private var loop = true
    private var startTime = 0f

    constructor(region: TextureRegion) : this(region, true)

    constructor(region: TextureRegion, loop: Boolean) : this(region, 1, 1, 1f, loop)

    constructor(
        region: TextureRegion, rows: Int, columns: Int, frameDuration: Float, loop: Boolean = true
    ) : this(region, rows, columns, gdxFilledArrayOf(rows * columns, frameDuration), loop)

    constructor(
        region: TextureRegion, rows: Int, columns: Int, durations: Array<Float>, loop: Boolean = true
    ) : this(region.splitAndFlatten(rows, columns, Array()), durations, loop)

    constructor(
        frames: Array<TextureRegion>, duration: Float, loop: Boolean = true
    ): this(frames, gdxFilledArrayOf(frames.size, duration), loop)

    constructor(
        frames: Array<TextureRegion>, durations: Array<Float>, loop: Boolean = true
    ) {
        if (frames.size != durations.size) throw IllegalArgumentException("Frames and durations must be the same size.")
        this.durations = durations
        this.frames = frames
        this.loop = loop
    }

    constructor(animation: Animation, reverse: Boolean = false) {
        this.frames = Array(animation.frames)
        this.durations = Array(animation.durations)
        loop = animation.loop
        if (reverse) {
            this.frames.reverse()
            this.durations.reverse()
        }
    }

    fun setStartTime(startTime: Float) {
        this.startTime = startTime
    }

    fun size() = frames.size

    override fun getCurrentRegion(): TextureRegion = frames[currentIndex]

    override fun isFinished() = !loop && elapsedTime >= getDuration()

    override fun getDuration() = durations.sum()

    override fun setFrameDuration(frameDuration: Float) {
        for (i in 0 until durations.size) durations[i] = frameDuration
    }

    override fun setFrameDuration(index: Int, frameDuration: Float) {
        if (index < 0 || index >= durations.size) throw IllegalArgumentException(
            "The index must be greater than or equal to 0 and less than the size of the animation"
        )
        durations[index] = frameDuration
    }

    override fun isLooping() = loop

    override fun setLooping(loop: Boolean) {
        this.loop = loop
    }

    override fun update(delta: Float) {
        elapsedTime += delta

        // If the animation is finished and not looping, then keep the elapsed time
        // at the duration, and set the current region to the last one (instead of the first)
        val duration = getDuration()
        while (elapsedTime >= duration) {
            if (loop) elapsedTime -= duration
            else {
                elapsedTime = duration
                currentIndex = frames.size - 1
                return
            }
        }

        // If the animation is looping, then find the current region by subtracting the
        // duration of each region from the elapsed time until the elapsed time is less
        // than the duration of the current region
        var currentLoopDuration = elapsedTime
        var tempIndex = 0
        while (tempIndex < frames.size && currentLoopDuration > durations[tempIndex]) {
            currentLoopDuration -= durations[tempIndex]
            tempIndex++
        }
        currentIndex = tempIndex
    }

    override fun reset() {
        elapsedTime = startTime
    }

    override fun copy() = Animation(this)

    override fun reversed() = Animation(this, true)

    override fun slice(start: Int, end: Int): IAnimation {
        if (start < 0 || start >= frames.size) throw IllegalArgumentException("The start index must be greater than or equal to 0 and less than the size of the animation")
        if (end < 0 || end > frames.size) throw IllegalArgumentException("The end index must be greater than or equal to 0 and less than or equal to the size of the animation")
        if (start >= end) throw IllegalArgumentException("The start index must be less than the end index")

        val newAnimation = Animation(this)
        newAnimation.frames.clear()
        newAnimation.durations.clear()
        for (i in start until end) {
            newAnimation.frames.add(frames[i])
            newAnimation.durations.add(durations[i])
        }
        return newAnimation
    }

    override fun setIndex(index: Int) {
        currentIndex = if (index < 0) 0 else if (index >= frames.size) frames.size - 1 else index
        elapsedTime = 0f
        for (i in 0 until currentIndex) elapsedTime += durations[i]
    }

    override fun getIndex() = currentIndex

    override fun setCurrentTime(time: Float) {
        if (time < 0f) throw IllegalArgumentException("Time value cannot be less than zero")
        val duration = getDuration()
        elapsedTime = if (loop) time % duration else time.coerceAtMost(duration)
        if (!loop && elapsedTime == duration) {
            currentIndex = frames.size - 1
            return
        }

        var currentLoopDuration = elapsedTime
        var tempIndex = 0
        while (tempIndex < frames.size && currentLoopDuration > durations[tempIndex]) {
            currentLoopDuration -= durations[tempIndex]
            tempIndex++
        }
        currentIndex = tempIndex
    }

    override fun getCurrentTime() = elapsedTime
}
