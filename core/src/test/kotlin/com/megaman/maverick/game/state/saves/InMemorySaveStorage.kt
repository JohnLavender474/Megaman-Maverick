package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.utils.ObjectMap

class InMemorySaveStorage : SaveStorage {

    val files = ObjectMap<String, String>()

    override fun read(name: String): String? = files.get(name)

    override fun write(name: String, text: String) {
        files.put(name, text)
    }
}
