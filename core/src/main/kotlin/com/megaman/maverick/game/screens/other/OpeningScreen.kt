package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

class OpeningScreen(game: MegamanMaverickGame) : AbstractStoryScreen(game) {

    companion object {
        const val TAG = "OpeningScreen"
    }

    override val music = MusicAsset.MM5_INTRO_MUSIC

    override val slides: Array<Array<String>> = gdxArrayOf(
        gdxArrayOf(
            "IN 20XX,",
            "MEGA MAN HAD SAVED THE WORLD",
            "COUNTLESS TIMES."
        ),
        gdxArrayOf(
            "IN THAT TIME, DR. LIGHT",
            "HAD BEEN DREAMING:",
            "NEW DESIGNS, NEW PARTS -",
            "A VISION OF WHAT",
            "MEGA MAN COULD YET BECOME."
        ),
        gdxArrayOf(
            "BUT THEN..."
        ),
        gdxArrayOf(
            "THE WORLD WAS ONCE AGAIN",
            "THRUST INTO CHAOS AT THE",
            "NEFARIOUS HANDS OF DR. WILY!"
        ),
        gdxArrayOf(
            "MEGA MAN MUST ONCE AGAIN",
            "FACE HIS NEMESIS!"
        ),
        gdxArrayOf(
            "HE MUST DO THIS,",
            "EVEN AS A MAVERICK",
            "IN THE EYES OF THE WORLD..."
        )
    )

    override fun onCompletion() = game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
}
