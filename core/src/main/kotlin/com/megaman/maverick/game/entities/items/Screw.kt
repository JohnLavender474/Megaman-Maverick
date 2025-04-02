package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType

class Screw(game: MegamanMaverickGame) : AbstractEnergyItem(game) {

    companion object {
        const val TAG = "Screw"
        private val animDefs = orderedMapOf(
            "large" pairTo AnimationDef(2, 2, 0.15f, true),
            "small" pairTo AnimationDef(2, 2, 0.15f, true)
        )
        private val regions = ObjectMap<String, ObjectMap<String, TextureRegion>>()
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (!regions.containsKey(getTag())) {
            val map = ObjectMap<String, TextureRegion>()
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)
            animDefs.keys().forEach { key -> map.put(key, atlas.findRegion("${getTag()}/$key")) }
            regions.put(getTag(), map)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun contactWithPlayer(megaman: Megaman) {
        GameLogger.debug(TAG, "contactWithPlayer()")
        destroy()
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_CURRENCY, props(ConstKeys.VALUE pairTo if (large) LARGE_AMOUNT else SMALL_AMOUNT)
            )
        )
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { "${getTag()}/${if (large) "large" else "small"}" }

        val animations = ObjectMap<String, IAnimation>()
        animDefs.forEach { entry ->
            val key = entry.key
            val fullKey = "${getTag()}/$key"
            try {
                val region = regions[getTag()][key]
                val (rows, columns, durations, loop) = entry.value

                animations.put(fullKey, Animation(region, rows, columns, durations, loop))
            } catch (e: Exception) {
                throw Exception(
                    "Failed to create animation for key=$key, tag=${getTag()}, fullKey=$fullKey, regions=${regions}", e
                )
            }
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
