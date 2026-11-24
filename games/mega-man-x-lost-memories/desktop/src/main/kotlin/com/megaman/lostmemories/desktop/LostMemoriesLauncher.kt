package com.megaman.lostmemories.desktop

import com.megaman.lostmemories.LostMemoriesTitle
import com.megaman.maverick.game.desktop.DesktopGame
import com.megaman.maverick.game.desktop.DesktopRunner

object LostMemoriesLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        // TODO: Lost Memories has no window icon of its own yet; it uses Maverick's (item 185)
        DesktopRunner.run(args, DesktopGame("Mega Man X: Lost Memories", "Megaman.png", LostMemoriesTitle))
    }
}
