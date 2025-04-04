package com.mega.game.engine.common.objects

data class GamePair<K, V>(var first: K, var second: V) {

    companion object {
        fun <K, V> of(first: K, second: V) = GamePair(first, second)
    }

    fun set(first: K, second: V): GamePair<K, V> {
        this.first = first
        this.second = second
        return this
    }
}

infix fun <A, B> A.pairTo(that: B): GamePair<A, B> = GamePair(this, that)
