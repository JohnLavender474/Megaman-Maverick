package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.containsRegion
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import java.util.function.Supplier

class MegamanAnimations(private val game: MegamanMaverickGame) : Initializable,
    Supplier<OrderedMap<String, IAnimation>> {

    companion object {
        const val TAG = "MegamanAnimations"
    }

    private val animations = OrderedMap<String, IAnimation>()
    private var initialized = false

    override fun init() {
        gdxArrayOf("Megaman" /*"MegamanMaverick"*/).forEach { megamanType ->
            for (weapon in MegamanWeapon.entries) {
                val assetSource = if (megamanType == "Megaman") when (weapon) {
                    MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_BUSTER.source
                    MegamanWeapon.RUSH_JETPACK -> TextureAsset.MEGAMAN_RUSH_JETPACK.source
                    // TODO: MegamanWeapon.FLAME_TOSS -> "" // TODO: TextureAsset.MEGAMAN_FLAME_TOSS.source
                }
                else when (weapon) {
                    MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_MAVERICK_BUSTER.source
                    MegamanWeapon.RUSH_JETPACK -> "" // TODO: TextureAsset.MEGAMAN_MAVERICK_RUSH_JETPACK.source
                    // TODO: MegamanWeapon.FLAME_TOSS -> "" // TODO: create texture atlas
                }
                if (assetSource == "") continue
                val atlas = game.assMan.getTextureAtlas(assetSource)

                MegamanAnimationDefs.getKeys().forEach { key ->
                    if (!atlas.containsRegion(key) || (megamanType == "Megaman" && key.contains("Left"))) return@forEach

                    val def = MegamanAnimationDefs.get(key)

                    var temp = key
                    temp += "_${megamanType}"
                    temp += "_${weapon.name}"

                    GameLogger.debug(TAG, "init(): putting animation \'${key}\' with key \'${temp}\'")

                    animations.put(temp, Animation(atlas.findRegion(key), def.rows, def.cols, def.durations))
                }
            }
        }
    }

    override fun get(): OrderedMap<String, IAnimation> {
        if (!initialized) {
            init()
            initialized = true
        }
        return animations
    }
}
