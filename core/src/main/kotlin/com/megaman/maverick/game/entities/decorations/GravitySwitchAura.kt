package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import kotlin.math.max

class GravitySwitchAura(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "GravitySwitchAura"

        private const val INIT_SIZE = 2f
        private const val SIZE_DELTA_SCALE = 2f
        private const val SCALE_TIMES = 10f
        private const val DELTA_DUR = 0.1f

        private var region: TextureRegion? = null
    }

    private val deltaTimer = Timer(DELTA_DUR)
    private var deltaIndex = 0

    private val center = Vector2()
    private var size = INIT_SIZE

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        this.center.set(center)

        size = INIT_SIZE

        deltaTimer.reset()
        deltaIndex = 0
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        deltaTimer.update(delta)
        if (deltaTimer.isFinished()) {
            deltaIndex++
            deltaTimer.reset()
            size *= SIZE_DELTA_SCALE
        }
        if (deltaIndex >= SCALE_TIMES) {
            destroy()
            return@update
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite())
        .updatable { _, sprite ->
            sprite.setSize(size * ConstVals.PPM)
            sprite.setCenter(center)
            val alpha = max(0f, 1f - ((1f / SCALE_TIMES) * deltaIndex))
            sprite.setAlpha(alpha)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 3, 1, 0.1f, true)))
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
