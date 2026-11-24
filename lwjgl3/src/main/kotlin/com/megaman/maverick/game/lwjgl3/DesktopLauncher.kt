package com.megaman.maverick.game.lwjgl3

import com.megaman.maverick.game.desktop.DesktopGame
import com.megaman.maverick.game.desktop.DesktopRunner
import com.megaman.maverick.game.titles.MaverickTitle

object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        DesktopRunner.run(args, DesktopGame("Megaman Maverick", "Megaman.png", MaverickTitle))
    }
}
