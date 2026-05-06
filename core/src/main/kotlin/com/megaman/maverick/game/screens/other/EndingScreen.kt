package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

class EndingScreen(game: MegamanMaverickGame) : AbstractStoryScreen(game) {

    companion object {
        const val TAG = "EndingScreen"
    }

    override val music = MusicAsset.MMX3_ZERO_THEME_MUSIC
    override val slides: Array<Array<String>> = gdxArrayOf(
        gdxArrayOf("DR. WILY HAS BEEN DEFEATED!"),
        gdxArrayOf(
            "THANKS TO MEGA MAN'S BRAVERY,",
            "THE WORLD IS SAFE ONCE MORE."
        ),
        gdxArrayOf(
            "DR. WILY KNELT BEFORE MEGA MAN,",
            "BEGGING FOR MERCY AS ALWAYS."
        ),
        gdxArrayOf(
            "MEGA MAN COULD ONLY SIGH.",
            "WHEN WILL DR. WILY EVER",
            "CHANGE HIS WAYS?"
        ),
        gdxArrayOf(
            "AS LONG AS THERE IS EVIL,",
            "MEGA MAN STANDS EVER READY"
        ),
        gdxArrayOf(
            "EVEN AS A MAVERICK",
            "IN THE EYES OF THE WORLD..."
        )
    )

    override fun onCompletion() = game.setCurrentScreen(ScreenEnum.CREDITS_SCREEN.name)
}
