package com.megaman.maverick.game.entities.megaman.sprites

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.drawables.sprites.containsRegion
import com.mega.game.engine.drawables.sprites.splitAndFlatten
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.entities.megaman.sprites.MegamanAnimationDefs.CHARGING_FRAME_DUR
import java.util.function.Supplier

class MegamanAnimations(
    private val game: MegamanMaverickGame,
    private val regionProcessor: MegaRegionProcessor? = null
) : Initializable, Supplier<OrderedMap<String, IAnimation>> {

    companion object {
        const val TAG = "MegamanAnimations"

        fun buildFullKey(regionKey: String, weapon: MegamanWeapon) =
            "${regionKey}_${weapon.name.lowercase().replace("_", "")}"

        fun splitFullKey(definitionKey: String) = definitionKey.split("_")
    }

    private val animations = OrderedMap<String, IAnimation>()

    private var initialized = false

    override fun init() {
        MegamanWeapon.entries.forEach { weapon ->
            val assetSource = when (weapon) {
                MegamanWeapon.MEGA_BUSTER -> TextureAsset.MEGAMAN_BUSTER.source
                MegamanWeapon.ICE_CUBE -> TextureAsset.MEGAMAN_ICE_CUBE.source
                MegamanWeapon.MAGMA_WAVE -> TextureAsset.MEGAMAN_MAGMA_WAVE.source
                MegamanWeapon.MOON_SCYTHE -> TextureAsset.MEGAMAN_MOON_SCYTHE.source
                MegamanWeapon.PRECIOUS_GUARD -> TextureAsset.MEGAMAN_PRECIOUS_GUARD.source
                MegamanWeapon.RUSH_JETPACK -> TextureAsset.MEGAMAN_RUSH_JETPACK.source
            }

            val atlas = game.assMan.getTextureAtlas(assetSource)

            MegamanAnimationDefs.getKeys().forEach { defKey ->
                if (!atlas.containsRegion(defKey)) return@forEach

                val fullKey = buildFullKey(defKey, weapon)
                val animation = buildAnimation(defKey, atlas, fullKey)
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

    private fun buildAnimation(defKey: String, atlas: TextureAtlas, fullKey: String) = when (defKey) {
        "stand" -> buildStandAnimation(atlas, false)
        "stand_shoot" -> buildStandAnimation(atlas, true)
        else -> {
            val region = atlas.findRegion(defKey)
            var (rows, columns, durations, loop) = MegamanAnimationDefs.get(defKey)
            val regions = region.splitAndFlatten(rows, columns, Array())

            // I'm too lazy to go in and manually add the "uncharged" region to each charging animation, so I'm doing
            // programatically here. Not the smartest move, but... well, yeah, just not the smartest move lol. This
            // is SUPER hacky, but... it's kinda fun to be stupid and hacky sometimes. Anyways, if this doesn't work,
            // then it's easy enough (though time-consuming as fuck) to go through each animation and manually add an
            // additional frame.
            if (defKey.contains("charge_half") || defKey.contains("charge_full")) {
                val unchargedKey = defKey.split("_")
                    .filter { keyPart -> !keyPart.equalsAny("charge", "half", "full") }
                    .joinToString("_")

                GameLogger.debug(
                    TAG,
                    "buildAnimation(): building charge animation: " +
                        "defKey=$defKey, unchargedKey=$unchargedKey, regions=${regions.size}, durations=${durations.size}"
                )

                if (atlas.containsRegion(unchargedKey)) {
                    val unchargedRegion = atlas.findRegion(unchargedKey)

                    val (uRows, uColumns, _, _) = MegamanAnimationDefs.get(unchargedKey)
                    val unchargedRegions = unchargedRegion.splitAndFlatten(uRows, uColumns, Array())

                    regions.add(unchargedRegions[0]) // only add the first region

                    durations = Array(durations) // create copy of array so that original isn't modified
                    durations.add(CHARGING_FRAME_DUR)

                    GameLogger.debug(
                        TAG, "buildAnimation(): built charge animation: " +
                            "regions=${regions.size}, durations=${durations.size}"
                    )
                } else GameLogger.error(TAG, "buildAnimation(): no region for uncharged key: $unchargedKey")
            }

            if (regionProcessor != null) for (i in 0 until regions.size)
                regions[i] = regionProcessor.process(regions[i], defKey, fullKey, rows, columns, durations, loop, i)

            Animation(regions, durations)
        }
    }

    private fun buildStandAnimation(atlas: TextureAtlas, shoot: Boolean): IAnimation {
        val runTransKey = if (shoot) "run_trans_shoot" else "run_trans"
        val region2 = atlas.findRegion(runTransKey)
        val runTransDef = MegamanAnimationDefs.get(runTransKey)
        val runTransRegs = region2.splitAndFlatten(runTransDef.rows, runTransDef.cols, Array())
        val runTransAnim = Animation(runTransRegs, runTransDef.durations, false)

        val standKey = if (shoot) "stand_shoot" else "stand"
        val region1 = atlas.findRegion(standKey)
        val standDef = MegamanAnimationDefs.get(standKey)
        val standRegs = region1.splitAndFlatten(standDef.rows, standDef.cols, Array())
        val standAnim = Animation(standRegs, standDef.durations, true)

        return object : IAnimation {

            override fun update(delta: Float) = when {
                !runTransAnim.isFinished() -> runTransAnim.update(delta)
                else -> standAnim.update(delta)
            }

            override fun reset() {
                runTransAnim.reset()
                standAnim.reset()
            }

            override fun getCurrentRegion() = when {
                !runTransAnim.isFinished() -> runTransAnim.getCurrentRegion()
                else -> standAnim.getCurrentRegion()
            }

            override fun isFinished() = false

            override fun getDuration() = runTransAnim.getDuration() + standAnim.getDuration()

            override fun setFrameDuration(frameDuration: Float) {
                throw Error("Not yet implemented")
            }

            override fun setFrameDuration(index: Int, frameDuration: Float) {
                throw Error("Not yet implemented")
            }

            override fun isLooping() = true

            override fun setLooping(loop: Boolean) {
                throw Error("Not yet implemented")
            }

            override fun reversed(): IAnimation {
                throw Error("Not yet implemented")
            }

            override fun slice(start: Int, end: Int): IAnimation {
                throw Error("Not yet implemented")
            }

            override fun setIndex(index: Int) {
                throw Error("Not yet implemented")
            }

            override fun getIndex() = when {
                !runTransAnim.isFinished() -> runTransAnim.getIndex()
                else -> standAnim.getIndex()
            }

            override fun setCurrentTime(time: Float) {
                throw Error("Not yet implemented")
            }

            override fun getCurrentTime() = when {
                !runTransAnim.isFinished() -> runTransAnim.getCurrentTime()
                else -> standAnim.getCurrentTime()
            }

            override fun copy(): IAnimation {
                throw Error("Not yet implemented")
            }
        }
    }
}

interface MegaRegionProcessor {

    fun process(
        region: TextureRegion,
        defKey: String,
        fullKey: String,
        rows: Int,
        columns: Int,
        durations: Array<Float>,
        loop: Boolean,
        index: Int
    ): TextureRegion
}
