package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.IGameEntity

class TagsComponent(vararg tags: String) : IGameComponent {

    private val tags = Array<String>(tags.size)

    init {
        if (tags.isEmpty()) throw IllegalArgumentException("A TagsComponent needs at least 1 tag")
        tags.forEach { add(it) }
    }

    fun add(tag: String) {
        if (!tags.contains(tag, false)) tags.add(tag)
    }

    val size: Int
        get() = tags.size

    operator fun get(index: Int): String = tags[index]

    fun contains(tag: String, ignoreCase: Boolean = false): Boolean {
        for (i in 0 until tags.size) if (tags[i].equals(tag, ignoreCase)) return true
        return false
    }

    override fun toString(): String = tags.toString(",")
}

val IGameEntity.tags: TagsComponent
    get() = require(TagsComponent::class)

fun IGameEntity.hasTag(tag: String, ignoreCase: Boolean = false) = tags.contains(tag, ignoreCase)

fun IGameEntity.hasAnyTag(tags: ObjectSet<String>): Boolean {
    val own = this.tags
    for (i in 0 until own.size) if (tags.contains(own[i])) return true
    return false
}
