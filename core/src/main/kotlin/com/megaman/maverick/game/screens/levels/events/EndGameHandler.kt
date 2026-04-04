package com.megaman.maverick.game.screens.levels.events

import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.megaman.maverick.game.MegamanMaverickGame

class EndGameHandler(private val game: MegamanMaverickGame): Initializable, Updatable, Resettable {

    val finished: Boolean
        get() = true

    override fun init(vararg params: Any) {
        // TODO
    }

    override fun update(delta: Float) {
        // TODO
    }

    override fun reset() {
        // TODO
    }
}
