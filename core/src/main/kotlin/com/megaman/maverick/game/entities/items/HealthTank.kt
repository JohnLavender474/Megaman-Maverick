package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class HealthTank(game: MegamanMaverickGame) : AbstractItem(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "HealthTank"
        private const val BODY_WIDTH = 1f
        private const val BODY_HEIGHT = 1.25f
        private const val GRAVITY = 0.15f
        private var region: TextureRegion? = null
    }

    lateinit var healthTank: MegaHealthTank
        private set

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ITEMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        GameLogger.debug(TAG, "canSpawn(): spawnProps=$spawnProps")
        healthTank = MegaHealthTank.valueOf(spawnProps.get(ConstKeys.VALUE, String::class)!!.uppercase())
        return !megaman.hasHealthTank(healthTank)
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.GRAVITY, GRAVITY)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        if (!this::healthTank.isInitialized) throw IllegalStateException("Heart tank value is not initialized")
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        super.onSpawn(spawnProps)
    }

    override fun contactWithPlayer(megaman: Megaman) {
        GameLogger.debug(TAG, "contactWithPlayer()")
        destroy()
        game.eventsMan.submitEvent(Event(EventType.ATTAIN_HEALTH_TANK, props(ConstKeys.VALUE pairTo healthTank)))
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
