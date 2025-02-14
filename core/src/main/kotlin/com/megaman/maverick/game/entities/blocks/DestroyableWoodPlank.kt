package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.body.getCenter

class DestroyableWoodPlank(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "DestroyableWoodPlank"
        private const val WIDTH = 3
        private const val HEIGHT = 1
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put("plank", atlas.findRegion("WoodPlank"))
            regions.put("flash", atlas.findRegion("$TAG/flash"))
        }
        super.init()
        addComponent(defineSpritesComponent())
        // addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(regions["plank"])
        sprite.setSize(WIDTH * ConstVals.PPM.toFloat(), HEIGHT * ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }

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
