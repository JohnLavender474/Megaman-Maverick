package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint

class HeartTank(game: MegamanMaverickGame) : AbstractItem(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "HeartTank"
        private const val BODY_SIZE = 1f
        private var region: TextureRegion? = null
    }

    lateinit var heartTank: MegaHeartTank
        private set

    private val spawnOnContact = Array<RectangleMapObject>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ITEMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val value = spawnProps.get(ConstKeys.VALUE)!!
        this.heartTank = when (value) {
            is MegaHeartTank -> value
            is String -> MegaHeartTank.valueOf(value.uppercase())
            else -> throw IllegalArgumentException("Invalid value: $value")
        }
        return !megaman.hasHeartTank(heartTank)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        if (!this::heartTank.isInitialized) throw IllegalStateException("Heart tank value is not initialized")
        body.setSize(BODY_SIZE * ConstVals.PPM)
        super.onSpawn(spawnProps)
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWN) && value is RectangleMapObject) spawnOnContact.add(value)
        }
    }

    override fun contactWithPlayer(megaman: Megaman) {
        destroy()
        spawnOnContact.forEach {
            val props = it.properties.toProps()

            val entityType = EntityType.valueOf(props.get(ConstKeys.ENTITY_TYPE, String::class)!!.uppercase())
            val key = entityType.getFullyQualifiedName(it.name)

            val entity = MegaEntityFactory.fetch(key, MegaGameEntity::class)!!
            entity.spawn(props)
        }
        game.eventsMan.submitEvent(Event(EventType.ATTAIN_HEART_TANK, props(ConstKeys.VALUE pairTo heartTank)))
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
