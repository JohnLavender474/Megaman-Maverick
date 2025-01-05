package com.mega.game.engine.cullables

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.components.IGameComponent

class CullablesComponent(val cullables: ObjectMap<String, ICullable> = ObjectMap()) : IGameComponent {

    fun put(key: String, cullable: ICullable) {
        cullables.put(key, cullable)
    }

    fun remove(key: String) {
        cullables.remove(key)
    }

    override fun reset() = cullables.values().forEach { it.reset() }
}
