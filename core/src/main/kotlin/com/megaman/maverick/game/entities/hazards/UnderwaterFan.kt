package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Speed
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.UnderWaterBubble
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*
import java.util.*
import kotlin.math.max
import kotlin.reflect.KClass

class UnderwaterFan(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IDamager, IHazard, IDirectional {

    companion object {
        const val TAG = "UnderwaterFan"

        private const val FORCE_FIXTURE_WIDTH = 2f
        private const val FORCE_FIXTURE_HEIGHT = 12f
        private const val FORCE_MAX_IMPULSE = 20f
        private const val FORCE_MIN_IMPULSE = 5f
        private const val FORCE_WANE_START_DIST = 5f
        private const val MAX_FORCE_TO_APPLY = 12f

        private const val MIN_BUBBLES_TO_SPAWN = 1
        private const val MAX_BUBBLES_TO_SPAWN = 3

        private const val SPAWN_BUBBLES_MIN_DELAY = 0.1f
        private const val SPAWN_BUBBLE_MAX_DELAY = 0.25f

        private val ENTITIES_TO_BLOW = objectSetOf<KClass<out IBodyEntity>>(Megaman::class)

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class UnderwaterFanColor { GRAY, GREEN }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private lateinit var color: UnderwaterFanColor

    private val forceOrigin = Vector2()
    private val forceBounds = GameRectangle()

    private val spawnBubblesDelay = Timer(SPAWN_BUBBLES_MIN_DELAY)

    private lateinit var spawnRoom: String

    private val positionSorter = PriorityQueue(object : Comparator<Vector2> {
        override fun compare(o1: Vector2, o2: Vector2): Int {
            val dist1 = o1.dst2(forceOrigin)
            val dist2 = o2.dst2(forceOrigin)
            return dist1.compareTo(dist2)
        }
    })

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            UnderwaterFanColor.entries.map { it.name.lowercase() }
                .forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val color = spawnProps.getOrDefault(ConstKeys.COLOR, UnderwaterFanColor.GREEN)
        this.color = when (color) {
            is UnderwaterFanColor -> color
            is String -> UnderwaterFanColor.valueOf(color.uppercase())
            else -> throw IllegalArgumentException("Invalid value for color: $color")
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val position = DirectionPositionMapper.getPosition(direction)
        val spawnBounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(spawnBounds.getPositionPoint(position), position)

        forceOrigin.set(spawnBounds.getPositionPoint(position.opposite()))
        when {
            direction.isVertical() -> forceBounds.setSize(
                FORCE_FIXTURE_WIDTH * ConstVals.PPM, FORCE_FIXTURE_HEIGHT * ConstVals.PPM
            )

            else -> forceBounds.setSize(
                FORCE_FIXTURE_HEIGHT * ConstVals.PPM, FORCE_FIXTURE_WIDTH * ConstVals.PPM
            )
        }
        forceBounds.positionOnPoint(forceOrigin, position.opposite())

        spawnBubblesDelay.reset()
    }

    private fun spawnBubbles() {
        val random = UtilMethods.getRandom(MIN_BUBBLES_TO_SPAWN, MAX_BUBBLES_TO_SPAWN)
        (0 until random).forEach {
            val x = when (direction) {
                Direction.UP, Direction.DOWN -> UtilMethods.getRandom(forceBounds.getX(), forceBounds.getMaxX())
                Direction.LEFT -> forceBounds.getMaxX() - 0.5f * ConstVals.PPM
                Direction.RIGHT -> forceBounds.getX() + 0.5f * ConstVals.PPM
            }
            val y = when (direction) {
                Direction.UP -> body.getBounds().getMaxY()
                Direction.DOWN -> body.getBounds().getY()
                Direction.LEFT, Direction.RIGHT -> UtilMethods.getRandom(forceBounds.getY(), forceBounds.getMaxY())
            }
            val position = GameObjectPools.fetch(Vector2::class).set(x, y)

            val bubble = MegaEntityFactory.fetch(UnderWaterBubble::class)!!
            bubble.spawn(
                props(
                    ConstKeys.SPEED pairTo Speed.FAST,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.DIRECTION pairTo direction,
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        spawnBubblesDelay.update(delta)
        if (spawnBubblesDelay.isFinished()) {
            spawnBubbles()
            val duration = UtilMethods.getRandom(SPAWN_BUBBLES_MIN_DELAY, SPAWN_BUBBLE_MAX_DELAY)
            spawnBubblesDelay.resetDuration(duration)
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.END_ROOM_TRANS),
                predicate@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@predicate room != spawnRoom
                }
            )
        )
    )

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.75f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val blowFixture = Fixture(body, FixtureType.FORCE, forceBounds)
        blowFixture.attachedToBody = false
        blowFixture.setVelocityAlteration { fixture, delta, _ ->
            val entity = fixture.getEntity() as IBodyEntity
            if (!ENTITIES_TO_BLOW.contains(entity::class)) return@setVelocityAlteration VelocityAlteration.addNone()

            val isMax = when (direction) {
                Direction.UP -> entity.body.physics.velocity.y >= MAX_FORCE_TO_APPLY * ConstVals.PPM
                Direction.DOWN -> entity.body.physics.velocity.y <= -MAX_FORCE_TO_APPLY * ConstVals.PPM
                Direction.LEFT -> entity.body.physics.velocity.x <= -MAX_FORCE_TO_APPLY * ConstVals.PPM
                Direction.RIGHT -> entity.body.physics.velocity.x >= MAX_FORCE_TO_APPLY * ConstVals.PPM
            }
            if (isMax) return@setVelocityAlteration VelocityAlteration.addNone()

            positionSorter.clear()
            Position.getCardinalPositions().forEach { cardinal ->
                val position = entity.body.getBounds().getPositionPoint(cardinal)
                positionSorter.add(position)
            }
            val position = positionSorter.peek()
            positionSorter.clear()

            val dist = forceOrigin.dst(position)

            val scalar = max(
                0f, when {
                    dist < FORCE_WANE_START_DIST * ConstVals.PPM -> 1f
                    else -> ((FORCE_FIXTURE_HEIGHT * ConstVals.PPM) - dist)
                        .div((FORCE_FIXTURE_HEIGHT * ConstVals.PPM) - (FORCE_WANE_START_DIST * ConstVals.PPM))
                }
            )

            var force = (FORCE_MIN_IMPULSE + (FORCE_MAX_IMPULSE - FORCE_MIN_IMPULSE))
                .times(scalar * delta * ConstVals.PPM)

            // game.setDebugText("${scalar.toInt()}: ${force.toInt()}")

            return@setVelocityAlteration VelocityAlteration.add(force, direction)
        }
        body.addFixture(blowFixture)
        blowFixture.drawingColor = Color.PINK
        debugShapes.add { blowFixture }

        val shieldFixture1 =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1.875f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        shieldFixture1.offsetFromBodyAttachment.y = 0.25f * ConstVals.PPM
        body.addFixture(shieldFixture1)
        shieldFixture1.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture1 }

        val shieldFixture2 = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.5f * ConstVals.PPM))
        shieldFixture2.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(shieldFixture2)
        shieldFixture2.drawingColor = Color.GREEN
        debugShapes.add { shieldFixture2 }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM, 0.875f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { color.name.lowercase() }
                .applyToAnimations { animations ->
                    UnderwaterFanColor.entries.map { it.name.lowercase() }.forEach {
                        val animation = Animation(regions[it], 2, 2, 0.05f, true)
                        animations.put(it, animation)
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
