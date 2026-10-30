package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class Life(services: MegaServices) : AbstractItem(services), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "Life"
        private const val BODY_SIZE = 1f
        private var region: TextureRegion? = null
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.ITEMS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        body.setSize(BODY_SIZE * ConstVals.PPM)
        super.onSpawn(spawnProps)
    }

    override fun contactWithPlayer(player: MegaGameEntity) {
        GameLogger.debug(TAG, "contactWithPlayer(): player=$player")
        destroy()
        if (services.gameState.addLife(player.playerSlot!!)) requestToPlaySound(SoundAsset.ONE_UP_SOUND, false)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 1, 0.1f, true))).build()

    override fun getTag() = TAG
}
