package com.mega.game.engine.drawables.sorting

import com.mega.game.engine.drawables.IDrawable

interface IComparableDrawable<T> : IDrawable<T>, Comparable<IComparableDrawable<T>> {

    val priority: DrawingPriority

    override fun compareTo(other: IComparableDrawable<T>) = priority.compareTo(other.priority)
}
