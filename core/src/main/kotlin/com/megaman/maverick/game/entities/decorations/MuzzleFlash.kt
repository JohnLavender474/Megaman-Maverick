package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullableOnEvent
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class MuzzleFlash(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "MuzzleFlash"
        private const val CULL_TIME = 0.15f
        private var region: TextureRegion? = null
    }

    private val cullTimer = Timer(CULL_TIME)

    override fun getType() = EntityType.DECORATION

    override fun init(vararg params: Any) {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        defaultSprite.setCenter(spawn)
        cullTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
        runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }

        val cullOutOfBounds =
            getGameCameraCullingLogic(game.getGameCamera(), { defaultSprite.boundingRectangle.toGameRectangle() })

        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS pairTo cullOnEvents,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds
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
