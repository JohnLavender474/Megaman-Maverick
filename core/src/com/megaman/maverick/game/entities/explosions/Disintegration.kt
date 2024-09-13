package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toGameRectangle
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class Disintegration(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, IAudioEntity {

    companion object {
        private const val DURATION = 0.275f
        private var disintegrationRegion: TextureRegion? = null
    }

    private val durationTimer = Timer(DURATION)

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (disintegrationRegion == null) disintegrationRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "Disintegration")
        addComponent(AudioComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        durationTimer.reset()
        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        (firstSprite as GameSprite).setPosition(spawn, Position.CENTER)
        if (GameRectangle()
                .setSize(ConstVals.PPM.toFloat())
                .setCenter(spawn)
                .overlaps(getGameCamera().toGameRectangle() as Rectangle)
        ) requestToPlaySound(
            SoundAsset.THUMP_SOUND,
            false
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        durationTimer.update(it)
        if (durationTimer.isFinished()) destroy()
    })

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        return SpritesComponent(sprite)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(disintegrationRegion!!, 1, 3, 0.005f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
