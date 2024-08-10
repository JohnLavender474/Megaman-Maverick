package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType


class ExplosionOrb(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "ExplosionOrb"
        private var textureRegion: TextureRegion? = null
    }

    private lateinit var trajectory: Vector2

    override fun init() {
        if (textureRegion == null) textureRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "ExplosionOrbs")

        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        firstSprite!!.setCenter(spawn.x, spawn.y)
        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2))
        sprite.setSize(3f * ConstVals.PPM)
        return SpritesComponent(sprite)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(textureRegion!!, 1, 2, 0.075f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        firstSprite!!.translate(
            trajectory.x * ConstVals.PPM * it, trajectory.y * ConstVals.PPM * it
        )
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = CullablesComponent()

        cullable.put(
            ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(
                game.getGameCamera(), { (firstSprite as Sprite).boundingRectangle }, 0.5f
            )
        )

        val cullOnEvent = CullableOnEvent({ it.key == EventType.PLAYER_SPAWN }, objectSetOf(EventType.PLAYER_SPAWN))
        cullable.put(ConstKeys.CULL_EVENTS, cullOnEvent)

        runnablesOnSpawn.add { game.eventsMan.addListener(cullOnEvent) }
        runnablesOnDestroy.add { game.eventsMan.removeListener(cullOnEvent) }

        return cullable
    }
}
