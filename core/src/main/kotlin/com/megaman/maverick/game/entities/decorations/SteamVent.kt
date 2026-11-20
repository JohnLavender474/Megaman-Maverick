package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setBounds
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic

class SteamVent(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity,
    ICullableEntity {

    companion object {
        const val TAG = "SteamVent"
        private const val PRIORITY = -10
        private const val FRAMES = 8
        private const val FRAME_DURATION = 0.1f
        private const val MIN_WAIT = 1f
        private const val MAX_WAIT = 3f
        private var region: TextureRegion? = null
    }

    private val bounds = GameRectangle()
    private val wait = Timer(MAX_WAIT)
    private lateinit var puff: Animation

    override fun init(vararg params: Any) {
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        defaultSprite.setBounds(bounds)
        puff.setToEnd()
        wait.resetDuration(nextWait())
    }

    private fun nextWait() = services.random.nextFloat(MIN_WAIT, MAX_WAIT)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (puff.isFinished()) {
            wait.update(delta)
            if (wait.isFinished()) {
                puff.reset()
                wait.resetDuration(nextWait())
            }
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(services.gameCamera, { bounds }))
    )

    private fun defineSpritesComponent() =
        SpritesComponent(GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, PRIORITY)))

    private fun defineAnimationsComponent(): AnimationsComponent {
        puff = Animation(region!!, 1, FRAMES, FRAME_DURATION, false)
        return AnimationsComponent(this, Animator(puff))
    }

    override fun getType() = EntityType.DECORATION
}
