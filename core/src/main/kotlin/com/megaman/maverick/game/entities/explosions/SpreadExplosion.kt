package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class SpreadExplosion(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDirectional, IOwnable<IGameEntity>, IDamager, IHazard {

    companion object {
        const val TAG = "SpreadExplosion"
        private const val DURATION = 0.3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class SpreadExplosionColor { GREEN, DEFAULT }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var owner: IGameEntity? = null

    private lateinit var color: SpreadExplosionColor

    private val size = Vector2()
    private var offset = 0f

    private val timer = Timer(
        DURATION, gdxArrayOf(
            TimeMarkedRunnable(0.05f) {
                size.set(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
                offset = 0.5f
            },
            TimeMarkedRunnable(0.1f) {
                size.set(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
                offset = 1f
            },
            TimeMarkedRunnable(0.15f) {
                size.set(2f * ConstVals.PPM, 3f * ConstVals.PPM)
                offset = 1.5f
            },
            TimeMarkedRunnable(0.2f) {
                size.set(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
                offset = 2f
            },
            TimeMarkedRunnable(0.25f) {
                size.set(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
                offset = 2f
            },
        )
    )

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.EXPLOSIONS_1.source)
            SpreadExplosionColor.entries.forEach { color ->
                val key = color.name.lowercase()
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        color = spawnProps.getOrDefault(ConstKeys.COLOR, SpreadExplosionColor.DEFAULT, SpreadExplosionColor::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        when (direction) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }

        size.set(0.25f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        offset = 0f

        timer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ConstVals.PPM.toFloat()))
        damagerFixture1.attachedToBody = false
        body.addFixture(damagerFixture1)
        debugShapes.add { damagerFixture1 }

        val explosionFixture1 = Fixture(body, FixtureType.EXPLOSION, GameRectangle())
        explosionFixture1.attachedToBody = false
        body.addFixture(explosionFixture1)

        val damagerFixture2 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ConstVals.PPM.toFloat()))
        damagerFixture2.attachedToBody = false
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2 }

        val explosionFixture2 = Fixture(body, FixtureType.EXPLOSION, GameRectangle())
        explosionFixture2.attachedToBody = false
        body.addFixture(explosionFixture2)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture {
                val fixture = it as Fixture

                if (!fixture.getType().equalsAny(FixtureType.DAMAGER, FixtureType.EXPLOSION)) return@forEachFixture

                (fixture.rawShape as GameRectangle).let { bounds ->
                    if (direction.isVertical()) bounds.setSize(size.x, size.y) else bounds.setSize(size.y, size.x)

                    val position = DirectionPositionMapper.getInvertedPosition(direction)
                    bounds.positionOnPoint(body.getPositionPoint(position), position)

                    val translation = GameObjectPools.fetch(Vector2::class)
                    when (direction) {
                        Direction.UP, Direction.DOWN -> when (fixture) {
                            damagerFixture1, explosionFixture1 ->
                                translation.set(offset, if (direction == Direction.UP) -0.1f else 0.1f)

                            else -> translation.set(-offset, if (direction == Direction.UP) 0.1f else -0.1f)
                        }

                        Direction.LEFT, Direction.RIGHT -> when (fixture) {
                            damagerFixture1, explosionFixture1 ->
                                translation.set(if (direction == Direction.LEFT) 0.1f else -0.1f, offset)

                            else -> translation.set(if (direction == Direction.LEFT) -0.1f else 0.1f, -offset)
                        }
                    }
                    bounds.translate(translation.scl(ConstVals.PPM.toFloat()))
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(6f * ConstVals.PPM, 3f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { color.name.lowercase() }
                .applyToAnimations { animations ->
                    SpreadExplosionColor.entries.forEach { color ->
                        val key = color.name.lowercase()
                        val animation = Animation(regions[key], 3, 2, 0.05f, false)
                        animations.put(key, animation)
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.EXPLOSION

    override fun getTag() = TAG
}
