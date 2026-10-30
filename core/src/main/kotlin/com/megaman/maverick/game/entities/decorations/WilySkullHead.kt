package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.Explosion

class WilySkullHead(services: MegaServices) : MegaGameEntity(services), IAudioEntity, ISpritesEntity {

    companion object {
        const val TAG = "WilySkullHead"
        private const val FALL_SPEED = 16f
        private const val SPRITE_WIDTH = 16f
        private const val SPRITE_HEIGHT = 16f
        private const val TTL = 1f
        private const val EXPLOSION_DELAY = 0.25f
        private const val BLINK_DELAY = 0.1f
        private var region: TextureRegion? = null
    }

    private val current = Vector2()
    private val target = Vector2()

    private val ttl = Timer(TTL)
    private var targetReached = false

    private var blink = false
    private val blinkDelay = Timer(BLINK_DELAY)
    private val explosionDelay = Timer(EXPLOSION_DELAY)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null)
            region = services.assets.getTextureRegion(TextureAsset.WILY_FINAL_BOSS.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        current.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)

        target.set(spawnProps.get(ConstKeys.TARGET, Vector2::class)!!)
        targetReached = false

        ttl.reset()
        explosionDelay.reset()

        blink = false
        blinkDelay.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        blinkDelay.update(delta)
        if (blinkDelay.isFinished()) {
            blink = !blink
            blinkDelay.reset()
        }

        explosionDelay.update(delta)
        if (explosionDelay.isFinished()) {
            val position = current.cpy().add(
                UtilMethods.getRandom(-1f, 1f) * ConstVals.PPM,
                UtilMethods.getRandom(-1f, 1f) * ConstVals.PPM,
            )
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.DAMAGER pairTo false,
                    ConstKeys.POSITION pairTo position
                )
            )
            requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
            explosionDelay.reset()
        }

        if (!targetReached) {
            val movement = target.cpy().sub(current).nor().scl(FALL_SPEED * ConstVals.PPM * delta)
            current.add(movement)

            if (current.y <= target.y) {
                current.set(target)
                targetReached = true
            }
        } else {
            ttl.update(delta)
            if (ttl.isFinished()) destroy()
        }
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND))
                .also { it.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(current)
            sprite.hidden = blink
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
