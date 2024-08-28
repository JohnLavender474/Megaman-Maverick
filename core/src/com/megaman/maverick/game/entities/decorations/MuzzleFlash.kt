package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType

class MuzzleFlash(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "MuzzleFlash"
        private const val CULL_TIME = 0.15f
        private var region: TextureRegion? = null
    }

    private val cullTimer = Timer(CULL_TIME)

    override fun getEntityType() = EntityType.DECORATION

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        firstSprite!!.setCenter(spawn)
        cullTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) kill()
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.add { game.eventsMan.addListener(cullOnEvents) }
        runnablesOnDestroy.add { game.eventsMan.removeListener(cullOnEvents) }

        val cullOutOfBounds =
            getGameCameraCullingLogic(game.getGameCamera(), { firstSprite!!.boundingRectangle.toGameRectangle() })

        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS to cullOnEvents,
                ConstKeys.CULL_OUT_OF_BOUNDS to cullOutOfBounds
            )
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(0.75f * ConstVals.PPM)
        return SpritesComponent(sprite)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.05f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}