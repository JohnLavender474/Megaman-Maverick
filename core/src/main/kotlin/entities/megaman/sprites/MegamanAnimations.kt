package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.containsRegion
import com.mega.game.engine.drawables.sprites.splitAndFlatten
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import java.util.function.Supplier

class MegamanAnimations(
    private val game: MegamanMaverickGame,
    private val regionProcessor: ((region: TextureRegion, fullKey: String, defKey: String, def: AnimationDef, index: Int) -> TextureRegion)? = null
) : Initializable, Supplier<OrderedMap<String, IAnimation>> {

    companion object {
        const val TAG = "MegamanAnimations"

        fun buildFullKey(regionKey: String, weapon: MegamanWeapon) =
            "${regionKey}_${weapon.name.lowercase().replace("_", "")}"

        fun splitFullKey(definitionKey: String) = definitionKey.split("_")
    }

    private var initialized = false
    private val animations = OrderedMap<String, IAnimation>()

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

                val region = atlas.findRegion(defKey)
                val def = MegamanAnimationDefs.get(defKey)
                val regions = region.splitAndFlatten(def.rows, def.cols, Array())

                val fullKey = buildFullKey(defKey, weapon)

                if (regionProcessor != null) for (i in 0 until regions.size)
                    regions[i] = regionProcessor.invoke(regions[i], fullKey, defKey, def, i)

                val animation = Animation(regions, def.durations)
                animations.put(fullKey, animation)

                GameLogger.debug(TAG, "init(): put animation \'$defKey\' with key \'$fullKey\'")
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
