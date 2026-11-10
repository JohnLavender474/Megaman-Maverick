package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Matrix
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.PrivateSandFloor
import com.megaman.maverick.game.entities.blocks.SharedSandFloor
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.SandReaction
import com.megaman.maverick.game.entities.state.sandReaction
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.defaultDoUpdate
import com.megaman.maverick.game.world.body.getBody
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.setReceiver
import kotlin.math.abs
import kotlin.math.max

class QuickSand(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "QuickSand"
        private const val ATLAS_KEY_SUFFIX = "_v2"
        private const val SINKER_AREA_MARGIN = 1f
        private const val SUBMERGE_DEPTH = 0.5f
        private const val SUBMERGE_MARGIN = 0.1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val privateFloors = ObjectMap<IGameEntity, PrivateSandFloor>()
    private var privateFloorProps: Properties? = null

    private var sharedFloor: SharedSandFloor? = null

    private lateinit var sinkerArea: Fixture

    private val tempSpriteMatrix = Matrix<GameRectangle>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.ENVIRONS_1.source)
            Position.entries.forEach { position ->
                val key = position.name.lowercase()
                val regionKey = "${TAG}${ATLAS_KEY_SUFFIX}/$key"
                val region = atlas.findRegion(regionKey)
                regions.put(key, region)

                GameLogger.debug(TAG, "init(): putting regions: regionKey=$regionKey, key=$key, region=$region")
            }
        }
        super.init()
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).set(bounds) }

        defineDrawables(bounds.splitByCellSize(ConstVals.PPM.toFloat(), tempSpriteMatrix))

        defineFloors(spawnProps, bounds)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        sprites.clear()
        animators.clear()
        clearSpritePreProcess()

        sharedFloor?.destroy()
        sharedFloor = null

        privateFloors.values().forEach { it.destroy() }
        privateFloors.clear()
        privateFloorProps = null

        sinkerArea.setActive(false)
    }

    private fun defineFloors(spawnProps: Properties, bounds: GameRectangle) {
        val floorBounds =
            GameRectangle(bounds.getX(), bounds.getMaxY() - ConstVals.PPM, bounds.getWidth(), ConstVals.PPM.toFloat())

        val floorProps = props(ConstKeys.BOUNDS pairTo floorBounds, ConstKeys.CULL_OUT_OF_BOUNDS pairTo false)
        spawnProps.get(ConstKeys.FIXTURE_LABELS)?.let { floorProps.put(ConstKeys.FIXTURE_LABELS, it) }

        val min = abs(spawnProps.get(ConstKeys.MIN, Float::class)!!)
        floorProps.put(ConstKeys.FALL, spawnProps.get(ConstKeys.FALL, Float::class)!!)
        floorProps.put(ConstKeys.RISE, spawnProps.get(ConstKeys.RISE, Float::class)!!)
        floorProps.put(ConstKeys.MIN, min)
        privateFloorProps = floorProps

        val sharedProps = floorProps.copy()
        sharedProps.put(
            ConstKeys.BLOCK_FILTERS, { entity: IGameEntity, _: IGameEntity -> privateFloors.containsKey(entity) }
        )
        sharedProps.put(ConstKeys.BLOCK_CONTACT_FILTER, { entity: MegaGameEntity -> !privateFloors.containsKey(entity) })
        val floor = MegaEntityFactory.fetch(SharedSandFloor::class)!!
        floor.spawn(sharedProps)
        sharedFloor = floor

        val margin = SINKER_AREA_MARGIN * ConstVals.PPM
        val bottom = floorBounds.getY() - min * ConstVals.PPM - 2f * margin
        (sinkerArea.rawShape as GameRectangle).set(
            bounds.getX() - margin, bottom, bounds.getWidth() + 2f * margin, bounds.getMaxY() + margin - bottom
        )
        sinkerArea.setActive(true)
    }

    private fun spawnPrivateFloor(entity: MegaGameEntity, entityBody: IBody) {
        if (privateFloors.containsKey(entity) || entity.sandReaction == SandReaction.SURFACE ||
            entityBody.type != BodyType.DYNAMIC
        ) return

        val floorProps = privateFloorProps!!.copy()
        floorProps.put(ConstKeys.BLOCK_FILTERS, { other: IGameEntity, _: IGameEntity -> other != entity })
        floorProps.put(ConstKeys.BLOCK_CONTACT_FILTER, { other: MegaGameEntity -> other == entity })
        if (entity.sandReaction == SandReaction.SUBMERGE) {
            val min = floorProps.get(ConstKeys.MIN, Float::class)!!
            floorProps.put(ConstKeys.MIN, max(min, entityBody.getHeight() / ConstVals.PPM + SUBMERGE_DEPTH + SUBMERGE_MARGIN))
        }

        val floor = MegaEntityFactory.fetch(PrivateSandFloor::class)!!
        floor.spawn(floorProps)
        privateFloors.put(entity, floor)
    }

    private fun destroyIfSubmerged(entity: MegaGameEntity, entityBody: IBody) {
        if (entity.sandReaction != SandReaction.SUBMERGE || !entity.existenceState.liveAfterUpdate) return

        val floor = privateFloors[entity] ?: return
        if (!floor.existenceState.liveNow) return

        val restingTop = floor.body.getMaxY() + entityBody.getHeight()
        if (restingTop <= body.getMaxY() - SUBMERGE_DEPTH * ConstVals.PPM) entity.destroy()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()

        val sandFixture = Fixture(body, FixtureType.SAND)
        body.addFixture(sandFixture)
        debugShapes.add { sandFixture }

        // TODO: an enemy's consumer fixture can sense a private floor, because only the floor's own fixture filter
        //  and the player's feet consumer test it (docs/issues/2026-11-01-two-players-stuck-in-quicksand.md)
        sinkerArea = Fixture(body, FixtureType.CONSUMER, GameRectangle(), active = false, attachedToBody = false)
        sinkerArea.setReceiver(Receivers.FILTER) { it.getType() == FixtureType.FEET }
        sinkerArea.setReceiver(Receivers.CONSUMER) { state, other ->
            val entity = other.getEntity()
            when (state) {
                ProcessState.END -> privateFloors.remove(entity)?.destroy()
                else -> {
                    spawnPrivateFloor(entity, other.getBody())
                    destroyIfSubmerged(entity, other.getBody())
                }
            }
        }
        body.addFixture(sinkerArea)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        // TODO: with no private floor, the sensor runs only while the sand overlaps the camera, so an entity that
        //  reaches off-camera sand falls through it, as it did with the culled map block (item 094)
        return BodyComponentCreator.create(
            this, body, doUpdate = { !privateFloors.isEmpty || body.defaultDoUpdate() }
        )
    }

    private fun defineDrawables(cells: Matrix<GameRectangle>) {
        cells.forEach { x, y, bounds ->
            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 20))
            sprite.setSize(ConstVals.PPM.toFloat())

            val key = "${x}_${y}"
            sprites.put(key, sprite)

            putSpritePreProcess(key) { _, _ -> sprite.setCenter(bounds!!.getCenter()) }

            val regionKey = when (x) {
                0 -> when (y) {
                    cells.rows - 1 -> Position.TOP_LEFT
                    0 -> Position.BOTTOM_LEFT
                    else -> Position.CENTER_LEFT
                }
                cells.columns - 1 -> when (y) {
                    cells.rows - 1 -> Position.TOP_RIGHT
                    0 -> Position.BOTTOM_RIGHT
                    else -> Position.CENTER_RIGHT
                }
                else -> when (y) {
                    cells.rows - 1 -> Position.TOP_CENTER
                    0 -> Position.BOTTOM_CENTER
                    else -> Position.CENTER
                }
            }.name.lowercase()

            val animator: Animator
            try {
                val region = regions[regionKey]
                val animation = Animation(region!!, 4, 2, 0.1f, true)
                animator = Animator(animation)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create animator for region $regionKey", e)
            }

            putAnimator(key, sprite, animator)
        }
    }

    override fun getType() = EntityType.SPECIAL
}
