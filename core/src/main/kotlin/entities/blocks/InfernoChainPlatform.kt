package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.getBounds

class InfernoChainPlatform(game: MegamanMaverickGame) : FeetRiseSinkBlock(game), ISpritesEntity {

    companion object {
        const val TAG = "InfernoChainPlatform"

        private const val PLATFORM = "platform"
        private const val CHAIN = "chain"

        private const val CHAIN_COUNT = "chain_count"
        private const val DEFAULT_CHAIN_COUNT = 15

        private const val BLOCK_WIDTH = 2f
        private const val BLOCK_HEIGHT = 2f

        private const val CHAIN_WIDTH = BLOCK_WIDTH
        private const val CHAIN_HEIGHT = BLOCK_HEIGHT

        private const val DEFAULT_FALL_SPEED = -2f
        private const val DEFAULT_RISE_SPEED = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var spawnRoom: String

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf(PLATFORM, CHAIN).forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        val bottomCenter =
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM)
            .setBottomCenterToPoint(bottomCenter)
        spawnProps.put(ConstKeys.BOUNDS, bounds)

        spawnProps.putIfAbsent(ConstKeys.FALL, DEFAULT_FALL_SPEED)
        spawnProps.putIfAbsent(ConstKeys.RISE, DEFAULT_RISE_SPEED)

        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        spawnProps.put("${ConstKeys.FEET}_${ConstKeys.SOUND}", true)

        super.onSpawn(spawnProps)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        val chainCount = spawnProps.getOrDefault(CHAIN_COUNT, DEFAULT_CHAIN_COUNT, Int::class)
        defineDrawables(chainCount)

        putCullable(ConstKeys.CULL_EVENTS, defineCullOnEventsCullable())
    }

    private fun defineCullOnEventsCullable() = getStandardEventCullingLogic(
        this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
            val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
            val cull = room != spawnRoom
            GameLogger.debug(
                TAG,
                "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
            )
            cull
        }
    )


    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
    }

    private fun defineDrawables(chainCount: Int) {
        val platform = GameSprite(regions[PLATFORM])
        sprites.put(PLATFORM, platform)

        putUpdateFunction(PLATFORM) { _, sprite -> platform.setBounds(body.getBounds()) }

        for (i in 0 until chainCount) {
            val key = "${CHAIN}_$i"

            val chain = GameSprite(regions[CHAIN], DrawingPriority(DrawingSection.BACKGROUND, 1))
            chain.setSize(CHAIN_WIDTH * ConstVals.PPM, CHAIN_HEIGHT * ConstVals.PPM)

            sprites.put(key, chain)

            putUpdateFunction(key) { _, sprite ->
                val x = body.getX()
                val y = body.getMaxY() + i * CHAIN_HEIGHT * ConstVals.PPM
                sprite.setPosition(x, y)
            }
        }
    }
}
