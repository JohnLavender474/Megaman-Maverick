package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class InfernoChainPlatform(game: MegamanMaverickGame) : FeetRiseSinkBlock(game), ICullableEntity, ISpritesEntity {

    companion object {
        const val TAG = "InfernoChainPlatform"

        private const val PLATFORM = "platform"
        private const val CHAIN = "chain"

        private const val CHAIN_COUNT = "chain_count"
        private const val DEFAULT_CHAIN_COUNT = 25

        private const val BLOCK_WIDTH = 2f
        private const val BLOCK_HEIGHT = 2f

        private const val CHAIN_WIDTH = BLOCK_WIDTH
        private const val CHAIN_HEIGHT = BLOCK_HEIGHT

        private const val MIN = 12f
        private const val FALL_SPEED = -2f
        private const val RISE_SPEED = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var spawnRoom: String

    private val spawnPropsCopy = Properties()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf(PLATFORM, CHAIN).forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        spawnPropsCopy.putAll(spawnProps)

        val bottomCenter =
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM).setBottomCenterToPoint(bottomCenter)
        spawnPropsCopy.put(ConstKeys.BOUNDS, bounds)

        spawnPropsCopy.put(ConstKeys.FALL, FALL_SPEED)
        spawnPropsCopy.put(ConstKeys.RISE, RISE_SPEED)
        spawnPropsCopy.put(ConstKeys.MIN, MIN)

        spawnPropsCopy.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        spawnPropsCopy.put("${ConstKeys.FEET}_${ConstKeys.SOUND}", true)

        super.onSpawn(spawnPropsCopy)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        val chainCount = spawnProps.getOrDefault(CHAIN_COUNT, DEFAULT_CHAIN_COUNT, Int::class)
        defineDrawables(chainCount)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        spawnPropsCopy.clear()

        sprites.clear()
    }

    override fun shouldMoveDown() = super.shouldMoveDown() && !body.isSensing(BodySense.FEET_ON_GROUND)

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val feetBounds = GameRectangle().setHeight(0.1f * ConstVals.PPM)
        val feetFixture = Fixture(body, FixtureType.FEET, feetBounds)
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.FEET) {
            val width = 0.75f * body.getWidth()
            feetBounds.setWidth(width)

            feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        }

        return BodyComponentCreator.amend(this, component)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG, "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    private fun defineDrawables(chainCount: Int) {
        val platform = GameSprite(regions[PLATFORM])
        sprites.put(PLATFORM, platform)

        putSpritePreProcess(PLATFORM) { _, sprite -> platform.setBounds(body.getBounds()) }

        for (i in 0 until chainCount) {
            val key = "${CHAIN}_$i"

            val chain = GameSprite(regions[CHAIN], DrawingPriority(DrawingSection.PLAYGROUND, 15))
            chain.setSize(CHAIN_WIDTH * ConstVals.PPM, CHAIN_HEIGHT * ConstVals.PPM)

            sprites.put(key, chain)

            putSpritePreProcess(key) { _, sprite ->
                val x = body.getX()
                val y = body.getMaxY() + i * CHAIN_HEIGHT * ConstVals.PPM
                sprite.setPosition(x, y)
            }
        }
    }
}
