package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
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
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

class D1000BombMarker(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "D1000BombMarker"
    }

    val position = Vector2()
    var rotation = 0f

    private val animations = ObjectMap<String, IAnimation>()

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.MARKER, animations)
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent(this, Animator(animations["marker"])))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        position.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val def = D1000Frames.MARKER["marker"]
            TickFrames.place(sprite, def, position.x, position.y, false)
            sprite.setOrigin(def.originX * TickFrames.SCALE, (def.height - def.originY) * TickFrames.SCALE)
            sprite.rotation = rotation
        }
        return component
    }

    override fun getType() = EntityType.DECORATION
}
