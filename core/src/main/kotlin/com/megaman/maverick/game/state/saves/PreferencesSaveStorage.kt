package com.megaman.maverick.game.state.saves

import com.badlogic.gdx.Preferences

class PreferencesSaveStorage(private val prefs: Preferences) : SaveStorage {

    override fun read(name: String): String? = if (prefs.contains(name)) prefs.getString(name) else null

    override fun write(name: String, text: String) {
        prefs.putString(name, text)
        prefs.flush()
    }
}
