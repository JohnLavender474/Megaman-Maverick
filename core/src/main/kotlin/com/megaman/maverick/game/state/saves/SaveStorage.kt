package com.megaman.maverick.game.state.saves

interface SaveStorage {

    fun read(name: String): String?

    fun write(name: String, text: String)
}
