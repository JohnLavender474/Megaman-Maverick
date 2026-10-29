package com.megaman.maverick.game.levels

import com.megaman.maverick.game.screens.ScreenEnum

object RobotMasterLevelOnCompletion : (LevelState) -> ScreenEnum {

    override fun invoke(levelState: LevelState) =
        if (levelState.weaponsAttained != null) ScreenEnum.GET_WEAPONS_SCREEN
        else ScreenEnum.SAVE_GAME_SCREEN
}
