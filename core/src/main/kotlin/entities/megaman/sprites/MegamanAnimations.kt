package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
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
        MegamanWeapon.entries.forEach { weapon ->
            val assetSource = when (weapon) {
                MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_V2_BUSTER.source
                MegamanWeapon.RUSH_JETPACK -> TextureAsset.MEGAMAN_RUSH_JETPACK.source
                // TODO: MegamanWeapon.FLAME_TOSS -> "" // TODO: TextureAsset.MEGAMAN_FLAME_TOSS.source
            }

            val atlas = game.assMan.getTextureAtlas(assetSource)
            MegamanAnimationDefs.getKeys().forEach { defKey ->
                if (!atlas.containsRegion(defKey)) return@forEach

                val def = MegamanAnimationDefs.get(defKey)
                val key = "${defKey}_${weapon.name}"
                animations.put(key, Animation(atlas.findRegion(defKey), def.rows, def.cols, def.durations))

                GameLogger.debug(TAG, "init(): put animation \'$defKey\' with key \'$key\'")
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
