package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.getCenter

class WoodPlank(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "WoodPlank"
        private const val WIDTH = 3
        private const val HEIGHT = 1
        private val animDefs = orderedMapOf(
            "still" pairTo AnimationDef(),
            "fire" pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineSpritesComponent())
        // addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val copyProps = spawnProps.copy()

        copyProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(WIDTH * ConstVals.PPM.toFloat(), HEIGHT * ConstVals.PPM.toFloat())
            .setCenter(center)
        copyProps.put(ConstKeys.BOUNDS, bounds)

        super.onSpawn(spawnProps)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(WIDTH * ConstVals.PPM.toFloat(), HEIGHT * ConstVals.PPM.toFloat()) }
        )
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    /*
    private fun defineAnimationsComponent(): AnimationsComponent {
        // TODO: add more animations
        val keySupplier: () -> String? = { "flash" }
        val animations = objectMapOf<String, IAnimation>(
            "flash" pairTo Animation(regions["flash"], 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
     */
}
