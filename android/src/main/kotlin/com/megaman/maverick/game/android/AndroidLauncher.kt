package com.megaman.maverick.game.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.mega.game.engine.common.GameLogLevel
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.MegamanMaverickGameParams
import com.megaman.maverick.game.StartScreenOption

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configuration = AndroidApplicationConfiguration()
        configuration.useImmersiveMode = false

        val params = MegamanMaverickGameParams()
        params.logLevel = GameLogLevel.OFF
        params.debugFPS = false
        params.startScreen = StartScreenOption.SIMPLE

        initialize(MegamanMaverickGame(params), configuration)
    }
}
