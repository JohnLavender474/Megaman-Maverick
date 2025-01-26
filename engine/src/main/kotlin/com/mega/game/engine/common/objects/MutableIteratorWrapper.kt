package com.mega.game.engine.common.objects

open class MutableIteratorWrapper<A, B>(val iterator: MutableIterator<A>, val converter: (A) -> B) : MutableIterator<B> {

    override fun hasNext() = iterator.hasNext()

    override fun next(): B {
        val value = iterator.next()
        return converter.invoke(value)
    }

    override fun remove() = iterator.remove()
}
