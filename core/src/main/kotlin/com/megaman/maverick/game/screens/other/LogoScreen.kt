package com.megaman.maverick.game.screens.other

import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.MegamanMaverickGame

class LogoScreen(game: MegamanMaverickGame): BaseScreen(), Initializable {

    companion object {
        private const val A_FAN_GAME_BY = "A FAN GAME BY"
        private const val MEGAMAN_IS_TRADEMARKED_BY_CAPCOM = "MEGAMAN IS A TRADEMARK OF CAPCOM."
        private const val THIS_IS_A_NON_COMMERCIAL_GAME = "THIS IS A NON-COMMERCIAL GAME"
        private const val BY_FANS_FOR_FANS = "BY FANS FOR FANS."
    }

    private var initialized = false

    override fun init() {


        initialized = true
    }

    override fun show() {
        if (!initialized) init()
    }
}
