package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.interfaces.IActivatable
import com.maverick.game.engine.common.objects.*
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.ShapeUtils
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.IChildEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.PreciousBlock
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.enemies.PreciousGemCanon
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousGemBomb
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.entities.special.DarknessV2
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.OwnedArray
import com.megaman.maverick.game.utils.extensions.*
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getEntity
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class Laser(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IHazard, IDamagerEntity,
    ILaserEntity, IAudioEntity, IActivatable, IOwnable<MegaGameEntity>, IChildEntity {

    companion object {
        const val TAG = "Laser"

        private val WHITESPACE = Regex("\\s+")

        private const val DEFAULT_MAX_LENGTH = 20f

        private const val LIGHT_SOURCE_OFFSET = 0.5f
        private const val LIGHT_SOURCE_RADIUS = 1
        private const val LIGHT_SOURCE_RADIANCE = 1.5f

        private const val LASER_SPRITE_ON_DELAY = 0.025f
        private const val BEAM_FRAME_DUR = 0.05f
        private const val BEAM_FRAME_WIDTH = 16
        private const val BEAM_FRAME_HEIGHT = 9
        private const val GLOW_FRAME_SIZE = 16
        private const val ART_PIXEL = ConstVals.PPM.toFloat() / BEAM_FRAME_WIDTH
        private const val GLOW_SIZE = 2f * GLOW_FRAME_SIZE / ConstVals.PPM
        private const val BEAM_SPRITE_KEY = "beam"
        private const val ORIGIN_SPRITE_KEY = "origin"
        private const val END_SPRITE_KEY = "end"

        private const val LASER_PRECIOUS_SHARDS_DELAY = 0.25f
        private const val LASER_SHARDS_MIN_IMPULSE_X = -3f
        private const val LASER_SHARDS_MAX_IMPULSE_X = 3f
        private const val LASER_SHARDS_IMPULSE_Y = 6f

        // This is how many times a "laser" can be reflected. A "laser" is composed of
        // one or more Laser instances - but to the player, it appears like just one.
        private const val MAX_REFLECTIONS = 2

        // Avoid spawning debris if the FPS dips below this guard
        private const val FPS_GUARD = 50

        private val beamFrames = Array<TextureRegion>()
        private val originFrames = Array<TextureRegion>()
        private val endFrames = Array<TextureRegion>()

        private val beamClock = Timer(BEAM_FRAME_DUR)
        private var beamClockFrameId = -1L
        private var beamFrameTick = 0

        private fun advanceBeamClock(delta: Float) {
            val frameId = Gdx.graphics.frameId
            if (frameId == beamClockFrameId) return
            beamClockFrameId = frameId
            beamClock.update(delta)
            if (!beamClock.isFinished()) return
            beamFrameTick = (beamFrameTick + 1) % (beamFrames.size * originFrames.size * endFrames.size)
            beamClock.reset()
        }

        private fun toArtPixels(value: Float) = (value / ART_PIXEL).roundToInt()
    }

    override var on: Boolean
        get() = getOrDefaultProperty(ConstKeys.ON, false, Boolean::class)
        set(value) {
            if (isProperty(ConstKeys.ON, value)) return

            putProperty(ConstKeys.ON, value)

            if (value) GameLogger.debug(TAG, "turn on: $line")
            else {
                reflectingLaser?.on = false
                spriteOnDelay.reset()
                line.reset()
            }
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

    private val contacts = LaserContacts {
        getFirstLocalPoint()
    }

    private val obstacles = OwnedArray { GameRectangle() }
    private val ignoring = ObjectSet<Int>()
    private val ignoringEntities = ObjectSet<IGameEntity>()

    private var lightSourceKeyString = ""
    private val lightSourceKeys = ObjectSet<Int>()

    // This is the "actual" end point after accounting for any blocks or other entities
    // in the path of this laser's "line".
    private val actualEndPoint = Vector2()

    private var burst: WhiteBurst? = null
    private var reflectingLaser: Laser? = null

    private var hitShieldFixture: IFixture? = null
    private var reflectionIndex = 0

    private val laserShardsTimer = Timer(LASER_PRECIOUS_SHARDS_DELAY)

    private val beamSprite = LaserBeamSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
    private val originSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
    private val endSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 11))
    private val spriteOnDelay = Timer(LASER_SPRITE_ON_DELAY)
    private var spritesHidden = false

    private val hitPoints = Array<Vector2>().apply { repeat(4) { add(Vector2()) } }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (beamFrames.isEmpty) {
            loadFrames(beamFrames, "beam", BEAM_FRAME_WIDTH, BEAM_FRAME_HEIGHT)
            loadFrames(originFrames, "origin", GLOW_FRAME_SIZE, GLOW_FRAME_SIZE)
            loadFrames(endFrames, "end", GLOW_FRAME_SIZE, GLOW_FRAME_SIZE)
        }
        beamSprite.setRegion(beamFrames[0])
        originSprite.setSize(GLOW_SIZE * ConstVals.PPM)
        endSprite.setSize(GLOW_SIZE * ConstVals.PPM)
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
        owner?.let { owner ->
            ignoringEntities.add(owner)
            owner.mapObjectId?.let { ignoring.add(it) }
        }

        // cast to "Laser" here to enforce strict type for "parent"
        parent = spawnProps.get(ConstKeys.PARENT, Laser::class)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).toProps().get(ConstKeys.ID, Int::class)!!
                ignoring.add(id)
            } else if (key.toString().contains(ConstKeys.OBSTACLE)) {
                obstacles.add().set((value as RectangleMapObject).rectangle)
            }
        }

        lightSourceKeyString = spawnProps.getOrDefault("${ConstKeys.LIGHT}_${ConstKeys.KEYS}", "", String::class)
        lightSourceKeys.addAll(
            lightSourceKeyString
                .replace(WHITESPACE, "")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.toInt() }
                .toObjectSet()
        )

        on = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)

        burst = MegaEntityFactory.fetch(WhiteBurst::class)!!
        burst!!.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.HIDDEN pairTo true))

        reflectionIndex = spawnProps.getOrDefault(ConstKeys.INDEX, 0, Int::class)

        laserShardsTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        owner = null
        parent = null

        beamSprite.clearQuads()
        sprites.clear()

        contacts.clear()

        lightSourceKeys.clear()

        obstacles.clear()

        ignoring.clear()
        ignoringEntities.clear()

        hitShieldFixture = null

        line.reset()
        (damagerFixture.rawShape as GameLine).reset()

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

            if (Gdx.graphics.framesPerSecond >= FPS_GUARD) {
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
                    .setX(services.random.nextFloat(LASER_SHARDS_MIN_IMPULSE_X, LASER_SHARDS_MAX_IMPULSE_X))
                    .setY(LASER_SHARDS_IMPULSE_Y)
                    .rotateDeg(direction.rotation)
                    .scl(ConstVals.PPM.toFloat())

                val size = services.random.pick(PreciousShardSize.entries)
                val color = services.random.pick(PreciousShardColor.entries)

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

                if (services.gameCamera.getRotatedBounds().contains(shardsPosition))
                    requestToPlaySound(SoundAsset.DINK_SOUND, false)
            }

            laserShardsTimer.reset()
        }
    }

    fun getFirstLocalPoint() = line.getFirstLocalPoint()

    fun setFirstLocalPoint(p1: Vector2) {
        line.setFirstLocalPoint(p1)
    }

    fun setSecondLocalPoint(p2: Vector2) {
        line.setSecondLocalPoint(p2)
    }

    fun set(other: GameLine) {
        line.set(other)
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
            if (laserFixture.rawShape !== line) laserFixture.setShape(line)

            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val damager = damagerFixture.rawShape as GameLine

            if (on) {
                val p1 = getFirstLocalPoint()
                damager.setFirstLocalPoint(p1)

                obstacles.forEach { obstacle ->
                    val count = ShapeUtils.intersectRectangleAndLine(obstacle, line, hitPoints)
                    for (i in 0 until count) contacts.add(hitPoints[i], null)
                }

                val p2 = when {
                    contacts.isEmpty || contacts.peek().first.dst(p1) > line.getLength() -> {
                        val (_, endPoint) = line.getWorldPoints()
                        endPoint
                    }
                    else -> {
                        val (endPoint, fixture) = contacts.poll()
                        if (fixture?.getType() == FixtureType.SHIELD) {
                            if (shouldHitShield(fixture)) hitShieldFixture = fixture
                            else resetReflectingLaserIfAny()
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

        return BodyComponentCreator.create(this, body, doUpdate = { true })
    }

    private fun shouldHitShield(shield: IFixture): Boolean {
        val shieldEntity = shield.getEntity()
        if (isLaserIgnoring(shieldEntity)) return false

        // Special case: if the shield is Megaman's axe shield, then the axe shield
        // needs to be facing the origin of the laser. This is to prevent lasers
        // from being reflected when hitting the back of the shield.
        val shieldPlayer = shieldEntity.asPlayer
        if (shieldPlayer != null && shield.isProperty(ConstKeys.AXE, true)) {
            val origin = getFirstLocalPoint()

            if (shieldPlayer.facing == Facing.LEFT && origin.x > shieldPlayer.body.getCenter().x) return false
            if (shieldPlayer.facing == Facing.RIGHT && origin.x < shieldPlayer.body.getCenter().x) return false
        }

        return true
    }

    private fun resetReflectingLaserIfAny() {
        reflectingLaser?.let {
            it.on = false
            it.owner = null

            it.setFirstLocalPoint(Vector2.Zero)
            it.setSecondLocalPoint(Vector2.Zero)

            it.ignoring.clear()
            it.ignoringEntities.clear()
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        if (shouldSendLightSourceEvents()) sendLightSourceEvents()

        if (on) {
            spriteOnDelay.update(delta)
            if (spriteOnDelay.isFinished()) {
                spritesHidden = false
                advanceBeamClock(delta)
                renderLaserSprites()
            } else if (!spritesHidden) {
                spritesHidden = true
                hideLaserSprites()
            }
        } else {
            if (!spritesHidden) {
                spritesHidden = true
                hideLaserSprites()
            }
            spriteOnDelay.reset()
            return@update
        }

        if (hitShieldFixture == null) {
            resetReflectingLaserIfAny()
            laserShardsTimer.setToEnd()
            return@update
        }

        val shieldFixture = hitShieldFixture!!
        hitShieldFixture = null

        /*
        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): " +
                "shieldFixture=$shieldFixture, " +
                "reflectDir=${shieldFixture.getProperty("${ConstKeys.REFLECT}_${ConstKeys.DIRECTION}")}"
        )
         */

        hitShield(shieldFixture, delta)

        if (reflectionIndex >= MAX_REFLECTIONS) return@update

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

        // GameLogger.debug(TAG, "defineUpdatablesComponent(): direction=$direction")

        val origin = getFirstLocalPoint()
        val xDiff = actualEndPoint.x - origin.x
        val yDiff = actualEndPoint.y - origin.y

        /*
        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): this.origin=$origin, this.endPoint=$actualEndPoint, xDiff=$xDiff, yDiff=$yDiff"
        )
         */

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

        /*
        GameLogger.debug(
            TAG,
            "defineUpdatablesComponent(): newFirstPoint=$newFirstPoint, newSecondPoint=$newSecondPoint"
        )
         */

        val reflector = shieldFixture.getEntity()

        // GameLogger.debug(TAG, "defineUpdatablesComponent(): reflector=$reflector")

        if (reflectingLaser == null) {
            reflectingLaser = MegaEntityFactory.fetch(Laser::class)!!
            reflectingLaser!!.spawn(
                props(
                    ConstKeys.PARENT pairTo this,
                    ConstKeys.ACTIVE pairTo false,
                    ConstKeys.INDEX pairTo reflectionIndex + 1,
                    "${ConstKeys.LIGHT}_${ConstKeys.KEYS}" pairTo lightSourceKeyString
                )
            )

            obstacles.forEach { obstacle -> reflectingLaser!!.obstacles.add().set(obstacle) }
        }

        reflectingLaser!!.setReflector(reflector, this)
        reflectingLaser!!.setFirstLocalPoint(newFirstPoint)
        reflectingLaser!!.setSecondLocalPoint(newSecondPoint)
    })

    internal fun setReflector(reflector: MegaGameEntity, parent: Laser) {
        ignoring.clear()
        ignoring.addAll(parent.ignoring)
        ignoringEntities.clear()
        ignoringEntities.addAll(parent.ignoringEntities)
        ignoringEntities.add(reflector)
        reflector.mapObjectId?.let { ignoring.add(it) }

        on = true
        owner = reflector
    }

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
                this,
                lightSourceKeys,
                position,
                LIGHT_SOURCE_RADIANCE,
                LIGHT_SOURCE_RADIUS
            )
        }
    }

    private fun loadFrames(out: Array<TextureRegion>, name: String, width: Int, height: Int) {
        val region = services.assets.getTextureRegion(TextureAsset.HAZARDS_1.source, "$TAG/$name")
        region.split(width, height).forEach { row -> out.add(row[0]) }
    }

    private fun buildLaserSprites(maxLength: Float) {
        beamSprite.ensureCapacity(ceil(maxLength / ART_PIXEL).toInt() + 1)
    }

    private fun hideLaserSprites() {
        beamSprite.hidden = true
        originSprite.hidden = true
        endSprite.hidden = true
    }

    private fun renderLaserSprites() {
        sprites.clear()

        val start = GameObjectPools.fetch(Vector2::class)
        val end = GameObjectPools.fetch(Vector2::class)
        (damagerFixture.getShape() as GameLine).calculateWorldPoints(start, end)
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx == 0f && dy == 0f) return

        val direction = when {
            abs(dx) >= abs(dy) -> if (dx > 0f) Direction.RIGHT else Direction.LEFT
            else -> if (dy > 0f) Direction.UP else Direction.DOWN
        }
        renderBeamCells(start, dx, dy, direction)
        beamSprite.hidden = false
        sprites.put(BEAM_SPRITE_KEY, beamSprite)

        if (parent == null) {
            originSprite.setRegion(originFrames[beamFrameTick % originFrames.size])
            setCenterOnArtGrid(originSprite, start)
            originSprite.hidden = false
            sprites.put(ORIGIN_SPRITE_KEY, originSprite)
        }

        endSprite.setRegion(endFrames[beamFrameTick % endFrames.size])
        setCenterOnArtGrid(endSprite, end)
        endSprite.hidden = false
        sprites.put(END_SPRITE_KEY, endSprite)
    }

    private fun renderBeamCells(start: Vector2, dx: Float, dy: Float, direction: Direction) {
        val horizontal = direction == Direction.LEFT || direction == Direction.RIGHT
        val majorStart = if (horizontal) start.x else start.y
        val minorStart = if (horizontal) start.y else start.x
        val majorDelta = if (horizontal) dx else dy
        val slope = (if (horizontal) dy else dx) / majorDelta
        val step = if (majorDelta > 0f) 1 else -1
        val majorOrigin = toArtPixels(majorStart)
        val cellCount = toArtPixels(abs(majorDelta))

        beamSprite.clearQuads()
        var runStart = 0
        var runMinor = 0
        for (cell in 0 until cellCount) {
            val cellCenter = (majorOrigin + step * (cell + 0.5f)) * ART_PIXEL
            val minorCenter = minorStart + (cellCenter - majorStart) * slope
            val minor = toArtPixels(minorCenter - BEAM_FRAME_HEIGHT * ART_PIXEL / 2f)
            if (cell == runStart) {
                runMinor = minor
                continue
            }
            if (minor == runMinor && cell / BEAM_FRAME_WIDTH == runStart / BEAM_FRAME_WIDTH) continue
            addBeamRun(runStart, cell, runMinor, majorOrigin, step, horizontal, direction)
            runStart = cell
            runMinor = minor
        }
        if (cellCount > 0) addBeamRun(runStart, cellCount, runMinor, majorOrigin, step, horizontal, direction)
        beamSprite.applyQuadBounds()
    }

    private fun addBeamRun(
        fromCell: Int,
        toCell: Int,
        minor: Int,
        majorOrigin: Int,
        step: Int,
        horizontal: Boolean,
        direction: Direction
    ) {
        val frame = beamFrames[Math.floorMod(beamFrameTick - fromCell / BEAM_FRAME_WIDTH, beamFrames.size)]
        val texelWidth = (frame.u2 - frame.u) / BEAM_FRAME_WIDTH
        val firstColumn = fromCell % BEAM_FRAME_WIDTH
        val nearU = frame.u + firstColumn * texelWidth
        val farU = frame.u + (firstColumn + toCell - fromCell) * texelWidth

        val majorLow = (if (step > 0) majorOrigin + fromCell else majorOrigin - toCell) * ART_PIXEL
        val majorHigh = majorLow + (toCell - fromCell) * ART_PIXEL
        val minorLow = minor * ART_PIXEL
        val minorHigh = minorLow + BEAM_FRAME_HEIGHT * ART_PIXEL

        if (horizontal) beamSprite.addQuad(
            majorLow, minorLow, majorHigh, minorHigh, nearU, farU, frame.v, frame.v2, direction
        ) else beamSprite.addQuad(
            minorLow, majorLow, minorHigh, majorHigh, nearU, farU, frame.v, frame.v2, direction
        )
    }

    private fun setCenterOnArtGrid(sprite: GameSprite, center: Vector2) {
        sprite.setPosition(
            toArtPixels(center.x - sprite.width / 2f) * ART_PIXEL,
            toArtPixels(center.y - sprite.height / 2f) * ART_PIXEL
        )
    }

    override fun isLaserIgnoring(entity: IGameEntity): Boolean {
        if (ignoringEntities.contains(entity)) return true
        val mapObjectId = entity.mapObjectId
        if (mapObjectId != null && ignoring.contains(mapObjectId)) return true

        if (entity.isAny(
                Axe::class,
                Wanaan::class,
                PreciousGem::class,
                PreciousShard::class,
                PreciousGemBomb::class,
                PreciousGemCanon::class,
            )
        ) return true

        return false
    }

    override fun getType() = EntityType.HAZARD
}
