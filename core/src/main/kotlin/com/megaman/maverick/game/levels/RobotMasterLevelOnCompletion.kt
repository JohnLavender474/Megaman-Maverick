package com.megaman.maverick.game.levels

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.screens.ScreenEnum

object RobotMasterLevelOnCompletion : (MegamanMaverickGame) -> ScreenEnum {

    override fun invoke(game: MegamanMaverickGame) =
        if (game.weaponsAttained != null) ScreenEnum.GET_WEAPONS_SCREEN
        else ScreenEnum.SAVE_GAME_SCREEN
}
