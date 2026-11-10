package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.IPlayer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys

class WeaponEnergyBulb(services: MegaServices): AbstractEnergyItem(services) {

    companion object {
        const val TAG = "WeaponEnergyBulb"
        private val animDefs = orderedMapOf(
            "large" pairTo AnimationDef(2, 2, 0.15f, true),
            "small" pairTo AnimationDef(2, 2, 0.15f, true)
        )
        private val regions = ObjectMap<String, ObjectMap<String, TextureRegion>>()
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (!regions.containsKey(TAG)) {
            val map = ObjectMap<String, TextureRegion>()

            val atlas = services.assets.getTextureAtlas(TextureAsset.ITEMS_1.source)
            animDefs.keys().forEach { key -> map.put(key, atlas.findRegion("${TAG}/$key")) }

            regions.put(TAG, map)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun contactWithPlayer(player: IPlayer) {
        GameLogger.debug(TAG, "contactWithPlayer(): player=$player")

        destroy()

        services.events.submitEvent(
            Event(
                EventType.ADD_WEAPON_ENERGY, props(
                    ConstKeys.VALUE pairTo if (large) LARGE_AMOUNT else SMALL_AMOUNT,
                    PlayerEventKeys.SLOT pairTo player.slot
                )
            )
        )
    }


    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { "${TAG}/${if (large) "large" else "small"}" }

        val animations = ObjectMap<String, IAnimation>()
        animDefs.forEach { entry ->
            val key = entry.key
            val fullKey = "${TAG}/$key"

            try {
                val region = regions[TAG][key]
                val (rows, columns, durations, loop) = entry.value

                animations.put(fullKey, Animation(region, rows, columns, durations, loop))
            } catch (e: Exception) {
                throw Exception(
                    "Failed to create animation for key=$key, tag=${TAG}, fullKey=$fullKey, regions=${regions}", e
                )
            }
        }

        val animator = Animator(keySupplier, animations)

        return AnimationsComponent(this, animator)
    }
}
