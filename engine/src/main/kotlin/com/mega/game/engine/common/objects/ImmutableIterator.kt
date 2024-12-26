package com.mega.game.engine.common.objects


class ImmutableIterator<E>(private val iterator: Iterator<E>) : Iterator<E> {

    override fun hasNext() = iterator.hasNext()

    override fun next() = iterator.next()
}
