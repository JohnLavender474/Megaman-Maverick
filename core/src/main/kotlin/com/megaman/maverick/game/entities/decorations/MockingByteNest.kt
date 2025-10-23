package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.MockingByte
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint

class MockingByteNest(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IOwnable<MockingByte> {

    companion object {
        const val TAG = "MockingByteNest"
        private const val DEFAULT_RISE_AMOUNT = 2f
        private var region: TextureRegion? = null
    }

    override var owner: MockingByte? = null

    val position = Vector2()
    val returnPositions = Array<Vector2>()

    var hidden = false
    var riseAmount = 0f

    private lateinit var spawnRoom: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        this.position.set(position)

        owner = spawnProps.get(ConstKeys.OWNER, MockingByte::class)
        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        riseAmount = spawnProps.getOrDefault(ConstKeys.RISE, DEFAULT_RISE_AMOUNT, Float::class)

        hidden = false

        spawnProps.forEach { key, value ->
            if (key.toString().contains("${ConstKeys.RETURN}_${ConstKeys.POSITION}")) {
                val returnPosition = (value as RectangleMapObject).rectangle.getCenter(false)
                returnPositions.add(returnPosition)
            }
        }
        if (returnPositions.isEmpty) throw IllegalStateException(
            "MockingByteNest must always have at least one return position for the MockingByte"
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        owner = null

        returnPositions.forEach { GameObjectPools.free(it) }
        returnPositions.clear()
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.hidden = hidden
        }
        .build()

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_ROOM pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.END_ROOM_TRANS),
                cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@cull spawnRoom != room
                }
            )
        )
    )

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
