package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames

// TODO: Wreck draws one still map sprite, as MockSpriteEntity does; merge them (item 138)
class Wreck(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "Wreck"

        val FRAMES = orderedMapOf(
            "wreck_a" pairTo TickFrameDef(48, 36, 20, 12, 1, intArrayOf(0), intArrayOf(1), 0),
            "wreck_b" pairTo TickFrameDef(18, 14, 6, 6, 1, intArrayOf(0), intArrayOf(1), 0)
        )
    }

    private val animations = ObjectMap<String, IAnimation>()
    private val origin = Vector2()
    private lateinit var key: String

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        TickFrames.animations(atlas, TAG, FRAMES, animations)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent(this, Animator({ key }, animations)))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        key = spawnProps.get(ConstKeys.KEY, String::class)!!
        if (!FRAMES.containsKey(key)) throw IllegalArgumentException("Unknown Wreck key $key: use one of ${FRAMES.keys()}")
        spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(origin)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ -> TickFrames.place(sprite, FRAMES[key], origin.x, origin.y, false) }
        return component
    }

    override fun getType() = EntityType.DECORATION
}
