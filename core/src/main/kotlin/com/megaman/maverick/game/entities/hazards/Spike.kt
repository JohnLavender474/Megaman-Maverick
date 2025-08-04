package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.*
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class Spike(game: MegamanMaverickGame) : MegaGameEntity(game), IChildEntity, IBodyEntity, ISpritesEntity,
    IAnimatedEntity, ICullableEntity, IDirectional {

    companion object {
        const val TAG = "Spike"
        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f
        private var atlas: TextureAtlas? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var parent: IGameEntity? = null

    private val offset = Vector2()

    private var block: Block? = null

    private var allowInstantDeath = true
    private var collisionOn = true

    private var spriteWidth: Float? = null
    private var spriteHeight: Float? = null
    private var animation: Animation? = null
    private lateinit var region: TextureRegion

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        super.init()
        addComponent(CullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        direction = megaman.direction

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        if (!gravityOn) parent?.let { if (it is IBodyEntity) offset.set(body.getCenter().sub(it.body.getCenter())) }

        spriteWidth = spawnProps.get(ConstKeys.SPRITE_WIDTH, Float::class)
        spriteHeight = spawnProps.get(ConstKeys.SPRITE_HEIGHT, Float::class)

        val regionKey = spawnProps.get(ConstKeys.REGION, String::class)!!
        region = atlas!!.findRegion(regionKey)

        animation = if (spawnProps.isProperty(ConstKeys.ANIMATION, true)) {
            val animRows = spawnProps.get("${ConstKeys.ANIMATION}_${ConstKeys.ROWS}", Int::class)!!
            val animCols = spawnProps.get("${ConstKeys.ANIMATION}_${ConstKeys.COLUMNS}", Int::class)!!
            val animDuration = spawnProps.get("${ConstKeys.ANIMATION}_${ConstKeys.DURATION}", Float::class)!!
            Animation(region, animRows, animCols, animDuration)
        } else null

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        when {
            cullOutOfBounds -> putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))
            else -> removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)
        }

        when {
            !cullOutOfBounds && spawnProps.containsKey(ConstKeys.CULL_EVENTS) -> {
                val cullEvents: ObjectSet<Any> =
                    spawnProps.get(ConstKeys.CULL_EVENTS, String::class)!!
                        .split(",")
                        .map { EventType.valueOf(it.uppercase()) }
                        .toObjectSet()
                val cullOnEvents = when {
                    cullEvents.isEmpty -> getStandardEventCullingLogic(this)
                    else -> getStandardEventCullingLogic(this, cullEvents)
                }
                putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
            }
            else -> removeCullable(ConstKeys.CULL_EVENTS)
        }

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val blockProps = spawnProps.copy()
        blockProps.putAll(ConstKeys.BLOCK_FILTERS pairTo TAG, ConstKeys.DRAW pairTo false)
        block!!.spawn(blockProps)

        allowInstantDeath = spawnProps.getOrDefault(ConstKeys.INSTANT, true, Boolean::class)
        collisionOn = spawnProps.getOrDefault("${ConstKeys.COLLIDE}_${ConstKeys.ON}", true, Boolean::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        block?.destroy()
        block = null

        parent = null

        offset.setZero()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        deathFixture.putProperty(ConstKeys.INSTANT, false)
        body.addFixture(deathFixture)
        debugShapes.add { deathFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.collisionOn = collisionOn

            feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            (deathFixture.rawShape as GameRectangle).set(body)

            val instant = if (allowInstantDeath) !body.isSensing(BodySense.FEET_ON_GROUND) else false
            deathFixture.putProperty(ConstKeys.INSTANT, instant)

            block!!.body.setCenter(body.getCenter())
            block!!.body.physics.collisionOn = body.isSensing(BodySense.FEET_ON_GROUND)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> gravityVec.set(0f, -gravity)
                Direction.DOWN -> gravityVec.set(0f, gravity)
                Direction.LEFT -> gravityVec.set(gravity, 0f)
                Direction.RIGHT -> gravityVec.set(-gravity, 0f)
            }.scl(ConstVals.PPM.toFloat())
            body.physics.gravity.set(gravityVec)

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
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { delta, _ ->
            sprite.setRegion(animation?.let {
                it.update(delta)
                it.getCurrentRegion()
            } ?: region)

            val width = if (spriteWidth != null) spriteWidth!! * ConstVals.PPM else body.getWidth()
            val height = if (spriteHeight != null) spriteHeight!! * ConstVals.PPM else body.getHeight()
            sprite.setSize(width, height)

            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return component
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
