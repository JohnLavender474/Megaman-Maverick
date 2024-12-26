package com.mega.game.engine.common.objects


open class ImmutableCollection<E>(private val collection: Collection<E>) : Collection<E> {

    override val size: Int
        get() = collection.size

    override fun contains(element: E) = collection.contains(element)

    override fun containsAll(elements: Collection<E>) = collection.containsAll(elements)

    override fun isEmpty() = collection.isEmpty()

    override fun iterator(): ImmutableIterator<E> = ImmutableIterator(collection.iterator())

    override fun parallelStream() = collection.parallelStream()

    override fun spliterator() = collection.spliterator()

    override fun stream() = collection.stream()

    override fun equals(other: Any?) = collection == other

    override fun hashCode() = collection.hashCode()

    override fun toString() = "ImmutableCollection(collection=$collection)"
}
