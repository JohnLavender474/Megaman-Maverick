package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.getCenter

class Coin(game: MegamanMaverickGame) : AbstractItem(game), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "Coin"
        private var region: TextureRegion? = null
    }

    private var doContactWithPlayer = true

    private var doCullAfterTime = false
    private var cullTimer = Timer()

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
        addComponent(defineUpdatablesComponent())
        body.setSize(ConstVals.PPM.toFloat())
    }

    override fun onSpawn(spawnProps: Properties) {
        if (!spawnProps.containsKey(ConstKeys.GRAVITY)) spawnProps.put(ConstKeys.GRAVITY, 0f)

        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put("${ConstKeys.CENTER}_${ConstKeys.POSITION}", true)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse)

        doContactWithPlayer = spawnProps.getOrDefault("${ConstKeys.PLAYER}_${ConstKeys.CONTACT}", true, Boolean::class)

        if (spawnProps.containsKey(ConstKeys.CULL_TIME)) {
            doCullAfterTime = true
            cullTimer.resetDuration(spawnProps.get(ConstKeys.CULL_TIME, Float::class)!!)
        } else doCullAfterTime = false
    }

    override fun contactWithPlayer(megaman: Megaman) {
        if (!doContactWithPlayer) return

        val coins = game.getOrDefaultProperty(ConstKeys.COINS, 0, Int::class)
        game.putProperty(ConstKeys.COINS, coins + 1)

        playSoundNow(SoundAsset.SMB3_COIN_SOUND, false)

        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (doCullAfterTime) {
            cullTimer.update(delta)
            if (cullTimer.isFinished()) destroy()
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(1f * ConstVals.PPM) })
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 2, 0.1f, true)))
        .build()
}
