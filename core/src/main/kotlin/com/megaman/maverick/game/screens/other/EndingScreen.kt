package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

class EndingScreen(game: MegamanMaverickGame) : AbstractStoryScreen(game) {

    override val music = MusicAsset.MMX3_ZERO_THEME_MUSIC
    override val slides: OrderedMap<Array<String>, Float> = OrderedMap<Array<String>, Float>().apply {
        put(gdxArrayOf("DR. WILY HAS BEEN DEFEATED!"), 5f)
        put(gdxArrayOf("THANKS TO MEGA MAN'S BRAVERY,", "THE WORLD IS SAFE ONCE MORE."), 5f)
        put(gdxArrayOf("DR. WILY KNELT BEFORE MEGA MAN,", "BEGGING FOR MERCY AS ALWAYS."), 5f)
        put(gdxArrayOf("MEGA MAN COULD ONLY SIGH.", "WHEN WILL DR. WILY EVER", "CHANGE HIS WAYS?"), 5f)
        put(gdxArrayOf("AS LONG AS THERE IS EVIL,", "MEGA MAN STANDS EVER READY"), 5f)
        put(gdxArrayOf("EVEN AS A MAVERICK", "IN THE EYES OF THE WORLD..."), 5f)
    }

    override fun onCompletion() = game.setCurrentScreen(ScreenEnum.CREDITS_SCREEN.name)
}
