package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*

class Spike(game: MegamanMaverickGame) : MegaGameEntity(game), IChildEntity, IBodyEntity, ISpritesEntity,
    ICullableEntity, IDirectionRotatable {

    companion object {
        const val TAG = "Spike"
        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f
        private var atlas: TextureAtlas? = null
    }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }
    override var parent: GameEntity? = null

    private val offset = Vector2()
    private var block: Block? = null
    private lateinit var region: TextureRegion

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        addComponent(CullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        if (!gravityOn) parent?.let { if (it is IBodyEntity) offset.set(body.getCenter().sub(it.body.getCenter())) }

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())

        val regionKey = spawnProps.get(ConstKeys.REGION, String::class)!!
        region = atlas!!.findRegion(regionKey)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))
        else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        if (!cullOutOfBounds && spawnProps.containsKey(ConstKeys.CULL_EVENTS)) {
            val cullEvents: ObjectSet<Any> =
                spawnProps.get(ConstKeys.CULL_EVENTS, String::class)!!
                    .split(",")
                    .map { EventType.valueOf(it.uppercase()) }
                    .toObjectSet()
            val cullOnEvents =
                if (cullEvents.isEmpty) getStandardEventCullingLogic(this)
                else getStandardEventCullingLogic(this, cullEvents)
            putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
        } else removeCullable(ConstKeys.CULL_EVENTS)

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val blockProps = spawnProps.copy()
        blockProps.put(ConstKeys.BLOCK_FILTERS, TAG)
        block!!.spawn(blockProps)
    }

    override fun onDestroy() {
        super.onDestroy()
        block?.destroy()
        block = null
        parent = null
        offset.setZero()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle(body))
        deathFixture.putProperty(ConstKeys.INSTANT, false)
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val instant = !body.isSensing(BodySense.FEET_ON_GROUND)
            deathFixture.putProperty(ConstKeys.INSTANT, instant)

            block!!.body.setCenter(body.getCenter())
            block!!.body.physics.collisionOn = body.isSensing(BodySense.FEET_ON_GROUND)

            val gravityValue = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity = when (directionRotation) {
                Direction.UP -> Vector2(0f, -gravityValue)
                Direction.DOWN -> Vector2(0f, gravityValue)
                Direction.LEFT -> Vector2(gravityValue, 0f)
                Direction.RIGHT -> Vector2(-gravityValue, 0f)
            }.scl(ConstVals.PPM.toFloat())

            parent?.let { p ->
                if (!body.physics.gravityOn && p is IBodyEntity) {
                    val parentCenter = p.body.getCenter()
                    val newCenter = parentCenter.add(offset)
                    body.setCenter(newCenter)
                }
            }
        }

        body.postProcess.put(ConstKeys.DEFAULT) { block!!.body.setCenter(body.getCenter()) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DEATH))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setRegion(region)
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = directionRotation.rotation
        }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
