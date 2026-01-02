package com.mega.game.engine.animations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.interfaces.ICopyable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

interface IAnimation : ICopyable<IAnimation>, Updatable, Resettable {

    fun getCurrentRegion(): TextureRegion?

    fun isFinished(): Boolean

    fun getDuration(): Float

    fun setFrameDuration(frameDuration: Float)

    fun setFrameDuration(index: Int, frameDuration: Float)

    fun isLooping(): Boolean

    fun setLooping(loop: Boolean)

    fun reversed(): IAnimation

    fun slice(start: Int, end: Int): IAnimation

    fun setIndex(index: Int)

    fun getIndex(): Int

    fun setCurrentTime(time: Float)

    fun getCurrentTime(): Float

    fun setToEnd()
}
