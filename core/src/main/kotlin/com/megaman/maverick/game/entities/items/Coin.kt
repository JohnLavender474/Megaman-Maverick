package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.getCenter

class Coin(game: MegamanMaverickGame): AbstractItem(game), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "Coin"
        private var region: TextureRegion? = null
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_ITEMS.source)
            region = atlas.findRegion(TAG.lowercase())
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        body.setSize(ConstVals.PPM.toFloat())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.GRAVITY, 0f)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put("${ConstKeys.CENTER}_${ConstKeys.POSITION}", true)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun contactWithPlayer(megaman: Megaman) {
        val coins = game.getOrDefaultProperty(ConstKeys.COINS, 0, Int::class)
        game.putProperty(ConstKeys.COINS, coins + 1)

        playSoundNow(SoundAsset.SMB3_COIN_SOUND, false)

        destroy()
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(1f * ConstVals.PPM) })
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 2, 0.1f, true)))
        .build()
}
