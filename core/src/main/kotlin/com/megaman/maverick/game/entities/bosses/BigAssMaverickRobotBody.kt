package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IRectangle
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.getSize

class BigAssMaverickRobotBody(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IEventListener {

    companion object {
        const val TAG = "BigAssMaverickRobotBody"
        private const val BODY = "Body"
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN)

    internal val bounds = GameRectangle()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureRegion(
            TextureAsset.BOSSES_3.source,
            "${BigAssMaverickRobot.TAG}/$BODY/${ConstKeys.BODY}"
        )
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        services.events.addListener(this)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, IRectangle::class)!!)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        services.events.removeListener(this)
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.PLAYER_SPAWN) destroy()
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!, DrawingPriority(DrawingSection.BACKGROUND, 1)))
        .preProcess { _, sprite ->
            sprite.setSize(bounds.getSize())
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(bounds.getPositionPoint(position), position)
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
