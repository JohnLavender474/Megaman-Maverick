package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType


class ExplosionOrb(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "ExplosionOrb"
        private const val OOB_CULL_TIME = 0.5f
        private var textureRegion: TextureRegion? = null
    }

    private lateinit var trajectory: Vector2

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (textureRegion == null) textureRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "ExplosionOrbs")

        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
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
        val cullOOB = getGameCameraCullingLogic(game.getGameCamera(), { firstSprite.boundingRectangle }, OOB_CULL_TIME)
        val cullEvents = getStandardEventCullingLogic(this, objectSetOf(EventType.PLAYER_SPAWN))
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOOB,
                ConstKeys.CULL_EVENTS pairTo cullEvents
            )
        )
    }
}
