package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class DecorLaserTube(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "DecorLaserTube"
        private const val CAP_HEIGHT = 0.5f
        private const val MID_HEIGHT = 1f
        private const val PRIORITY = -10
        private const val FRAMES = 4
        private const val FRAME_DURATION = 0.1f
        private const val TOP_CAP = "top_cap"
        private const val BOTTOM_CAP = "bottom_cap"
        private const val MID = "mid"
        private var capRegion: TextureRegion? = null
        private var midRegion: TextureRegion? = null
    }

    private val bounds = GameRectangle()
    private val midSprites = Array<GameSprite>()
    private val midAnimations = Array<Animation>()

    override fun init(vararg params: Any) {
        val atlas = TextureAsset.DECORATIONS_1.source
        if (capRegion == null) capRegion = services.assets.getTextureRegion(atlas, "$TAG/cap")
        if (midRegion == null) midRegion = services.assets.getTextureRegion(atlas, "$TAG/mid")
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        val mids = ((bounds.getHeight() - 2f * CAP_HEIGHT * ConstVals.PPM) / (MID_HEIGHT * ConstVals.PPM)).toInt()
        check(mids >= 1) {
            "$TAG $mapObjectId: the map object is ${bounds.getHeight()} px high; it needs at least " +
                "${(2f * CAP_HEIGHT + MID_HEIGHT) * ConstVals.PPM} px (2 caps and 1 glass segment)"
        }
        while (midAnimations.size < mids) addMid(midAnimations.size)
        val phase = (mapObjectId ?: 0) % FRAMES
        val cap = CAP_HEIGHT * ConstVals.PPM
        val mid = MID_HEIGHT * ConstVals.PPM
        sprites[TOP_CAP]!!.setBounds(bounds.getX(), bounds.getMaxY() - cap, bounds.getWidth(), cap)
        sprites[BOTTOM_CAP]!!.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), cap)
        for (i in 0 until midAnimations.size) {
            val sprite = midSprites[i]
            sprite.hidden = i >= mids
            sprite.setBounds(bounds.getX(), bounds.getY() + cap + i * mid, bounds.getWidth(), mid)
            midAnimations[i].setIndex(phase)
        }
    }

    private fun addMid(index: Int) {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, PRIORITY))
        val key = "${MID}_$index"
        sprites.put(key, sprite)
        midSprites.add(sprite)
        val animation = Animation(midRegion!!, 1, FRAMES, FRAME_DURATION, true)
        midAnimations.add(animation)
        getComponent(AnimationsComponent::class)!!.putAnimator(key, sprite, Animator(animation))
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(services.gameCamera, { bounds }))
    )

    private fun defineSpritesComponent(): SpritesComponent {
        val component = SpritesComponent()
        val top = GameSprite(capRegion!!, DrawingPriority(DrawingSection.PLAYGROUND, PRIORITY))
        val bottom = GameSprite(capRegion!!, DrawingPriority(DrawingSection.PLAYGROUND, PRIORITY))
        bottom.setFlip(false, true)
        component.putSprite(TOP_CAP, top)
        component.putSprite(BOTTOM_CAP, bottom)
        return component
    }

    override fun getType() = EntityType.DECORATION
}
