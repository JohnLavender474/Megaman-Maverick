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
    private val regionProcessor: MegaRegionProcessor? = null
) : Initializable, Supplier<OrderedMap<String, IAnimation>> {

    companion object {
        const val TAG = "MegamanAnimations"

        fun buildFullKey(regionKey: String, weapon: MegamanWeapon) =
            "${regionKey}_${weapon.name.lowercase().replace("_", "")}"
    }

    private val animations = OrderedMap<String, IAnimation>()

    private var initialized = false

    override fun init() {
        MegamanWeapon.entries.forEach { weapon ->
            val assetSource = when (weapon) {
                MegamanWeapon.MEGA_BUSTER -> TextureAsset.MEGAMAN_BUSTER.source
                MegamanWeapon.ICE_CUBE -> TextureAsset.MEGAMAN_BUSTER.source // TODO: TextureAsset.MEGAMAN_ICE_CUBE.source
                MegamanWeapon.FIRE_BALL -> TextureAsset.MEGAMAN_FIRE_BALL.source
                MegamanWeapon.MOON_SCYTHE -> TextureAsset.MEGAMAN_MOON_SCYTHE.source
                MegamanWeapon.RUSH_JETPACK -> TextureAsset.MEGAMAN_RUSH_JETPACK.source
            }

            val atlas = game.assMan.getTextureAtlas(assetSource)

            MegamanAnimationDefs.getKeys().forEach { defKey ->
                if (!atlas.containsRegion(defKey)) return@forEach

                val region = atlas.findRegion(defKey)
                val def = MegamanAnimationDefs.get(defKey)
                val regions = region.splitAndFlatten(def.rows, def.cols, Array())

                val fullKey = buildFullKey(defKey, weapon)

                if (regionProcessor != null) for (i in 0 until regions.size)
                    regions[i] = regionProcessor.process(regions[i], fullKey, defKey, def, i)

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

interface MegaRegionProcessor {

    fun process(region: TextureRegion, fullKey: String, defKey: String, def: AnimationDef, index: Int): TextureRegion
}
