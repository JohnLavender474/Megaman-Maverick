package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.PreciousBlock
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.enemies.PreciousGemCanon
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGemBomb
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.entities.special.DarknessV2
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.*
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getEntity
import java.util.*

class Laser(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IHazard, IDamager,
    ILaserEntity, IAudioEntity, IActivatable, IOwnable<MegaGameEntity>, IChildEntity {

    companion object {
        const val TAG = "Laser"

        private const val DEFAULT_MAX_LENGTH = 20f

        private const val LIGHT_SOURCE_OFFSET = 0.5f
        private const val LIGHT_SOURCE_RADIUS = 1
        private const val LIGHT_SOURCE_RADIANCE = 1.5f

        private const val LASER_SPRITE_SIZE = 2f / ConstVals.PPM
        private const val LASER_SPRITE_ON_DELAY = 0.025f

        private const val LASER_PRECIOUS_SHARDS_DELAY = 0.15f
        private const val LASER_SHARDS_MIN_IMPULSE_X = -3f
        private const val LASER_SHARDS_MAX_IMPULSE_X = 3f
        private const val LASER_SHARDS_IMPULSE_Y = 6f

        // This is how many times a "laser" can be reflected. A "laser" is composed of
        // one or more Laser instances - but to the player, it appears like just one.
        private const val MAX_REFLECTIONS = 2

        private var region: TextureRegion? = null
    }

    override var on: Boolean
        get() = getOrDefaultProperty(ConstKeys.ON, false, Boolean::class)
        set(value) {
            if (isProperty(ConstKeys.ON, value)) return

            putProperty(ConstKeys.ON, value)
            if (!value) spriteOnDelay.reset()
        }
    // The "owner" is the entity that spawns this laser (or causes it to be spawned), e.g.
    // a laser beamer, a shield, etc.
    override var owner: MegaGameEntity? = null
    // The "parent" is the parent laser. This is null except if this laser is spawned from
    // another laser.
    override var parent: IGameEntity? = null

    private val line = GameLine()

    private lateinit var laserFixture: Fixture
    private lateinit var damagerFixture: Fixture

    private val contacts = PriorityQueue<GamePair<Vector2, IFixture>> { p1, p2 ->
        val origin = getFirstLocalPoint()
        val d1 = p1.first.dst2(origin)
        val d2 = p2.first.dst2(origin)
        d1.compareTo(d2)
    }

    private val obstaclesToIgnore = ObjectSet<Int>()
    private val lightSourceKeys = ObjectSet<Int>()

    // This is the "actual" end point after accounting for any blocks or other entities
    // in the path of this laser's "line".
    private val actualEndPoint = Vector2()

    private var burst: WhiteBurst? = null
    private var reflectingLaser: Laser? = null

    private var hitShieldFixture: IFixture? = null
    private var reflectionIndex = 0

    private val spriteOnDelay = Timer(LASER_SPRITE_ON_DELAY)

    private val laserShardsTimer = Timer(LASER_PRECIOUS_SHARDS_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region =
            game.assMan.getTextureRegion(TextureAsset.COLORS.source, "${ConstKeys.BRIGHT}_${ConstKeys.RED}")
        super.init()
        addComponent(AudioComponent())
        addComponent(SpritesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val maxLength = spawnProps.getOrDefault(ConstKeys.RADIUS, DEFAULT_MAX_LENGTH, Float::class) * ConstVals.PPM
        buildLaserSprites(maxLength)

        val firstPoint =
            spawnProps.getOrDefault("${ConstKeys.FIRST}_${ConstKeys.POINT}", Vector2.Zero, Vector2::class)
        val secondPoint =
            spawnProps.getOrDefault("${ConstKeys.SECOND}_${ConstKeys.POINT}", Vector2.Zero, Vector2::class)
        line.set(firstPoint, secondPoint)

        actualEndPoint.set(secondPoint)

        body.set(line.getBoundingRectangle())

        owner = spawnProps.get(ConstKeys.OWNER, MegaGameEntity::class)
        owner?.let { obstaclesToIgnore.add(it.id) }

        // cast to "Laser" here to enforce strict type for "parent"
        parent = spawnProps.get(ConstKeys.PARENT, Laser::class)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).toProps().get(ConstKeys.ID, Int::class)!!
                obstaclesToIgnore.add(id)
            }
        }

        lightSourceKeys.addAll(
            spawnProps.getOrDefault("${ConstKeys.LIGHT}_${ConstKeys.KEYS}", "", String::class)
                .replace("\\s+", "")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.toInt() }
                .toObjectSet()
        )

        on = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)

        burst = MegaEntityFactory.fetch(WhiteBurst::class)!!
        burst!!.spawn(props(ConstKeys.OWNER pairTo this))

        reflectionIndex = spawnProps.getOrDefault(ConstKeys.INDEX, 0, Int::class)

        if (reflectionIndex < MAX_REFLECTIONS) {
            reflectingLaser = MegaEntityFactory.fetch(Laser::class)!!
            reflectingLaser!!.spawn(
                props(
                    ConstKeys.PARENT pairTo this,
                    ConstKeys.ACTIVE pairTo false,
                    ConstKeys.INDEX pairTo reflectionIndex + 1
                )
            )
        }

        laserShardsTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        owner = null
        parent = null

        sprites.clear()
        contacts.clear()
        lightSourceKeys.clear()
        obstaclesToIgnore.clear()

        hitShieldFixture = null

        line.reset()
        laserFixture.setShape(GameLine())
        damagerFixture.setShape(GameLine())

        burst?.destroy()
        burst = null

        reflectingLaser?.destroy()
        reflectingLaser = null
    }

    private fun hitShield(shieldFixture: IFixture, delta: Float) {
        val shield = shieldFixture.getEntity()
        if (shield !is PreciousBlock) return

        laserShardsTimer.update(delta)
        if (laserShardsTimer.isFinished()) {
            val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo actualEndPoint
                )
            )

            val tempRect = GameObjectPools.fetch(GameRectangle::class)
                .setSize(0.25f * ConstVals.PPM)
                .setCenter(actualEndPoint)

            val direction = when {
                shieldFixture.hasProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}") ->
                    shieldFixture.getProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", Direction::class)!!
                else -> UtilMethods.getOverlapPushDirection(tempRect, shieldFixture.getShape())
            }

            if (direction == null) return

            val shardsPosition = GameObjectPools.fetch(Vector2::class)
                .set(actualEndPoint)
                .add(0.25f * ConstVals.PPM, direction)

            val impulse = GameObjectPools.fetch(Vector2::class)
                .setX(UtilMethods.getRandom(LASER_SHARDS_MIN_IMPULSE_X, LASER_SHARDS_MAX_IMPULSE_X))
                .setY(LASER_SHARDS_IMPULSE_Y)
                .rotateDeg(direction.rotation)
                .scl(ConstVals.PPM.toFloat())

            val size = PreciousShardSize.entries.random()
            val color = PreciousShardColor.entries.random()

            val preciousShard = MegaEntityFactory.fetch(PreciousShard::class)!!
            preciousShard.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.SIZE pairTo size,
                    ConstKeys.COLOR pairTo color,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.POSITION pairTo shardsPosition,
                    "${ConstKeys.COLLIDE}_${ConstKeys.DELAY}" pairTo false,
                )
            )

            if (game.getGameCamera().getRotatedBounds().contains(shardsPosition))
                requestToPlaySound(SoundAsset.DINK_SOUND, false)

            laserShardsTimer.reset()
        }
    }

    fun getFirstLocalPoint() = line.getFirstLocalPoint()

    fun setFirstLocalPoint(p1: Vector2) {
        line.setFirstLocalPoint(p1)
        GameLogger.debug(TAG, "setFirstLocalPoint(): line=$line, owner=$owner, parent=$parent")
    }

    fun setSecondLocalPoint(p2: Vector2) {
        line.setSecondLocalPoint(p2)
        GameLogger.debug(TAG, "setSecondLocalPoint(): line=$line, owner=$owner, parent=$parent")
    }

    fun set(other: GameLine) {
        line.set(other)
        GameLogger.debug(TAG, "set(): line=$line, other=$other, owner=$owner, parent=$parent")
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameLine())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        laserFixture = Fixture(body, FixtureType.LASER, GameLine())
        laserFixture.putProperty(ConstKeys.DAMAGER, damagerFixture)
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
        laserFixture.attachedToBody = false
        body.addFixture(laserFixture)
        laserFixture.drawingColor = Color.ORANGE
        debugShapes.add { laserFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture -> fixture.setActive(on) }

            body.set(line.getBoundingRectangle())
            laserFixture.setShape(line)

            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val damager = damagerFixture.rawShape as GameLine

            if (on) {
                val p1 = getFirstLocalPoint()
                damager.setFirstLocalPoint(p1)

                val p2 = when {
                    contacts.isEmpty() || contacts.peek().first.dst(p1) > line.getLength() -> {
                        val (_, endPoint) = line.getWorldPoints()
                        endPoint
                    }
                    else -> {
                        val (endPoint, fixture) = contacts.poll()
                        if (fixture.getType() == FixtureType.SHIELD) {
                            if (shouldHitShield(fixture)) {
                                GameLogger.debug(TAG, "body.postProcess(): hit shield: $fixture")
                                hitShieldFixture = fixture
                            } else {
                                GameLogger.debug(TAG, "body.postProcess(): ignore shield: $fixture")
                                resetReflectingLaserIfAny()
                            }
                        }
                        endPoint
                    }
                }

                damager.setSecondLocalPoint(p2)
                burst?.body?.setCenter(p2)
                actualEndPoint.set(p2)
            } else {
                damager.setFirstLocalPoint(Vector2.Zero)
                damager.setSecondLocalPoint(Vector2.Zero)

                burst?.body?.setCenter(Vector2.Zero)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun shouldHitShield(shield: IFixture): Boolean {
        val shieldEntity = shield.getEntity()
        if (isLaserIgnoring(shieldEntity)) return false

        // Special case: if the shield is Megaman's axe shield, then the axe shield
        // needs to be facing the origin of the laser. This is to prevent lasers
        // from being reflected when hitting the back of the shield.
        if (shieldEntity is Megaman && shield.isProperty(ConstKeys.AXE, true)) {
            val origin = getFirstLocalPoint()

            if (megaman.isFacing(Facing.LEFT) && origin.x > megaman.body.getCenter().x) return false
            if (megaman.isFacing(Facing.RIGHT) && origin.x < megaman.body.getCenter().x) return false
        }

        return true
    }

    private fun resetReflectingLaserIfAny() {
        reflectingLaser?.let {
            it.on = false
            it.owner = null

            it.setFirstLocalPoint(Vector2.Zero)
            it.setSecondLocalPoint(Vector2.Zero)

            it.obstaclesToIgnore.clear()
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        if (shouldSendLightSourceEvents()) sendLightSourceEvents()

        if (on) spriteOnDelay.update(delta)

        if (hitShieldFixture == null) {
            resetReflectingLaserIfAny()
            laserShardsTimer.setToEnd()
            return@update
        }

        val shieldFixture = hitShieldFixture!!
        hitShieldFixture = null

        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): " +
                "shieldFixture=$shieldFixture, " +
                "reflectDir=${shieldFixture.getProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}")}"
        )

        hitShield(shieldFixture, delta)

        if (reflectingLaser == null) return@update

        val direction = when {
            shieldFixture.hasProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}") ->
                shieldFixture.getProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}", Direction::class)!!
            else -> {
                val tempRect = GameObjectPools.fetch(GameRectangle::class)
                    .setSize(0.25f * ConstVals.PPM)
                    .setCenter(actualEndPoint)

                UtilMethods.getOverlapPushDirection(tempRect, shieldFixture.getShape())
            }
        }

        if (direction == null) {
            resetReflectingLaserIfAny()
            return@update
        }

        GameLogger.debug(TAG, "defineUpdatablesComponent(): direction=$direction")

        val origin = getFirstLocalPoint()
        val xDiff = actualEndPoint.x - origin.x
        val yDiff = actualEndPoint.y - origin.y

        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): this.origin=$origin, this.endPoint=$actualEndPoint, xDiff=$xDiff, yDiff=$yDiff"
        )

        val newFirstPoint = GameObjectPools.fetch(Vector2::class).set(actualEndPoint)

        val newSecondPoint = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP, Direction.DOWN -> {
                newSecondPoint.x = actualEndPoint.x + xDiff
                newSecondPoint.y = origin.y
            }
            Direction.LEFT, Direction.RIGHT -> {
                newSecondPoint.x = origin.x
                newSecondPoint.y = actualEndPoint.y + yDiff
            }
        }
        val reflectionDirection = GameObjectPools.fetch(Vector2::class)
            .set(newSecondPoint)
            .sub(newFirstPoint)
            .nor()
        newSecondPoint.set(newFirstPoint)
            .add(reflectionDirection.scl(DEFAULT_MAX_LENGTH * ConstVals.PPM))

        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): newFirstPoint=$newFirstPoint, newSecondPoint=$newSecondPoint"
        )

        val reflector = shieldFixture.getEntity()

        GameLogger.debug(TAG, "defineUpdatablesComponent(): reflector=$reflector")

        reflectingLaser!!.obstaclesToIgnore.clear()
        reflectingLaser!!.obstaclesToIgnore.add(reflector.id)

        reflectingLaser!!.on = true
        reflectingLaser!!.owner = reflector
        reflectingLaser!!.setFirstLocalPoint(newFirstPoint)
        reflectingLaser!!.setSecondLocalPoint(newSecondPoint)
    })

    private fun shouldSendLightSourceEvents() = MegaGameEntities.getOfTag(DarknessV2.TAG).any {
        (it as DarknessV2).overlaps(damagerFixture.getShape())
    }

    private fun sendLightSourceEvents() {
        val line = damagerFixture.getShape() as GameLine
        val (worldPoint1, worldPoint2) = line.getWorldPoints()

        val parts = line.getLength().div(LIGHT_SOURCE_OFFSET * ConstVals.PPM).toInt()
        for (point in 0..parts) {
            val position = MegaUtilMethods.interpolate(worldPoint1, worldPoint2, point.toFloat() / parts.toFloat())

            LightSourceUtils.sendLightSourceEvent(
                game,
                lightSourceKeys,
                position,
                LIGHT_SOURCE_RADIANCE,
                LIGHT_SOURCE_RADIUS
            )
        }
    }

    private fun buildLaserSprites(maxLength: Float) {
        val count = (maxLength / (LASER_SPRITE_SIZE * ConstVals.PPM)).toInt()

        GameLogger.debug(TAG, "buildLaserSprites(): maxRadius=$maxLength, count=$count")

        for (i in 0 until count) {
            val key = "${ConstKeys.PIECE}_$i"

            val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 10))
            sprite.setSize(LASER_SPRITE_SIZE * ConstVals.PPM)
            sprites.put(key, sprite)

            putSpriteUpdateFunction(key) updateFunc@{ _, _ ->
                val laser = damagerFixture.getShape() as GameLine

                if (i * LASER_SPRITE_SIZE * ConstVals.PPM > laser.getLength()) {
                    sprite.hidden = true
                    return@updateFunc
                }

                sprite.hidden = !on || !spriteOnDelay.isFinished()

                val p1 = GameObjectPools.fetch(Vector2::class)
                val p2 = GameObjectPools.fetch(Vector2::class)
                laser.calculateWorldPoints(p1, p2)

                val distance = i * LASER_SPRITE_SIZE * ConstVals.PPM

                val offset = GameObjectPools.fetch(Vector2::class)
                    .set(p2)
                    .sub(p1)
                    .nor()
                    .scl(distance)

                val center = GameObjectPools.fetch(Vector2::class)
                    .set(p1)
                    .add(offset)

                sprite.setCenter(center)
            }
        }
    }

    override fun isLaserIgnoring(entity: IGameEntity): Boolean {
        if (obstaclesToIgnore.contains((entity as MegaGameEntity).id)) return true

        if (entity.isAny(
                Axe::class,
                PreciousShard::class,
                PreciousGemBomb::class,
                PreciousGemCanon::class,
            )
        ) return true

        return false
    }

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
