package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.screens.ScreenEnum

class OpeningScreen(game: MegamanMaverickGame) : AbstractStoryScreen(game) {

    override val music = MusicAsset.MM5_INTRO_MUSIC
    override val slides: OrderedMap<Array<String>, Float> = OrderedMap<Array<String>, Float>().apply {
        put(gdxArrayOf("IN 20XX, MEGA MAN", "HAD SAVED THE WORLD", "COUNTLESS TIMES."), 3f)
        put(
            gdxArrayOf(
                "IN THAT TIME, DR. LIGHT",
                "HAD BEEN DREAMING",
            ), 2f
        )
        put(
            gdxArrayOf(
                "NEW DESIGNS! NEW PARTS!",
                "A VISION OF WHAT",
                "MEGA COULD YET BECOME."
            ), 3f
        )
        put(gdxArrayOf("BUT THEN..."), 1f)
        put(gdxArrayOf("THE WORLD WAS ONCE AGAIN", "THRUST INTO CHAOS AT THE", "NEFARIOUS HANDS OF DR. WILY!"), 3.5f)
        put(gdxArrayOf("MEGA MAN MUST ONCE AGAIN", "FACE HIS NEMESIS!"), 2f)
        put(gdxArrayOf("HE MUST DO THIS,", "EVEN AS A MAVERICK", "IN THE EYES OF THE WORLD..."), 3f)
    }

    override fun onCompletion() = game.setCurrentScreen(ScreenEnum.MAIN_MENU_SCREEN.name)
}
