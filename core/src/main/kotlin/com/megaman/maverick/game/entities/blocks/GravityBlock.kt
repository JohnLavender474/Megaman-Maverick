package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class GravityBlock(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAudioEntity, IDirectional {

    companion object {
        const val TAG = "GravityBlock"

        const val MOON_CRATE = "MoonCrate"

        private const val GRAVITY = 0.25f
        private const val GROUND_GRAVITY = 0.01f

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private var innerBlock: Block? = null
    private lateinit var spawnRoom: String
    private lateinit var regionKey: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf(MOON_CRATE).forEach { regions.put(it, atlas.findRegion(it)) }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds =
            GameObjectPools.fetch(GameRectangle::class)
                .setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
                .setCenter(center)
        body.set(bounds)

        innerBlock = MegaEntityFactory.fetch(Block::class)!!
        innerBlock!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.BOUNDS pairTo bounds,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.BLOCK_FILTERS pairTo { entity: MegaGameEntity, block: MegaGameEntity ->
                    blockFilter(entity, block)
                },
            )
        )

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        regionKey = spawnProps.getOrDefault(ConstKeys.REGION, MOON_CRATE, String::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        innerBlock?.destroy()
        innerBlock = null
    }

    private fun blockFilter(entity: MegaGameEntity, block: MegaGameEntity) =
        entity == this && block == this.innerBlock

    private fun defineUpdatablesComponent() = UpdatablesComponent({ direction = megaman.direction })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS), cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@cull room != spawnRoom
                }
            )
        )
    )

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.receiveFrictionX = false
        body.physics.receiveFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val feetFixture = Fixture(
            body,
            FixtureType.FEET,
            GameRectangle().setSize(BODY_WIDTH * 0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ ->
            requestToPlaySound(SoundAsset.POUND_SOUND, false)
        }
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.BLOCK) {
            innerBlock!!.let {
                it.body.direction = direction
                it.body.set(body)
            }
        }
        body.preProcess.put(ConstKeys.FEET_BLOCKS) {
            val feetBlocks = body.getFeetBlocks()
            val filteredFeetBlocks = feetBlocks.removeIf { it == innerBlock }
            body.setFeetBlocks(filteredFeetBlocks)
        }
        body.preProcess.put(ConstKeys.GRAVITY) {
            val gravity = GameObjectPools.fetch(Vector2::class)

            val value = when {
                body.isSensing(BodySense.FEET_ON_GROUND) && !body.getFeetBlocks().isEmpty -> GROUND_GRAVITY
                else -> GRAVITY
            }

            when (direction) {
                Direction.UP -> gravity.set(0f, -value)
                Direction.DOWN -> gravity.set(0f, value)
                Direction.LEFT -> gravity.set(value, 0f)
                Direction.RIGHT -> gravity.set(-value, 0f)
            }.scl(ConstVals.PPM.toFloat())

            body.physics.gravity.set(gravity)
        }
        body.preProcess.put(ConstKeys.MOVEMENT) {
            when {
                direction.isVertical() -> body.physics.velocity.x = 0f
                else -> body.physics.velocity.y = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1)))
        .updatable { _, sprite ->
            sprite.setRegion(regions[regionKey])
            sprite.setBounds(body.getBounds())
        }
        .build()

    override fun getTag() = TAG

    override fun getType() = EntityType.BLOCK
}
