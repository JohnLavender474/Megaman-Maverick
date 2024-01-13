package com.megaman.maverick.game.screens.levels.events

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.screens.ScreenEnum

class EndLevelEventHandler(private val game: MegamanMaverickGame) {

  fun endLevelSuccessfully() {
    game.setCurrentScreen(ScreenEnum.SIMPLE_END_LEVEL_SUCCESSFULLY.name)
  }
}
