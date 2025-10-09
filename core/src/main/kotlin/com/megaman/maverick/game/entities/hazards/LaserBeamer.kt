package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameLine.GameLineRenderingType
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.RotatingLine
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
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.ILaserEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.special.DarknessV2
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.*
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import java.util.*

class LaserBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity, IDrawableShapesEntity,
    ICullableEntity, ILaserEntity, IAudioEntity, IDamager, IEventListener {

    companion object {
        const val TAG = "LaserBeamer"

        private val CONTACT_RADII = floatArrayOf(2f, 5f, 8f)

        private const val DEFAULT_SPEED = 2f
        private const val DEFAULT_MAX_RADIUS = 10f

        private const val CONTACT_TIME = 0.05f
        private const val SWITCH_TIME = 1f

        private const val MIN_DEGREES = 200f
        private const val MAX_DEGREES = 340f
        private const val INIT_DEGREES = 270f

        private const val LIGHT_SOURCE_OFFSET = 0.5f
        private const val LIGHT_SOURCE_RADIUS = 1
        private const val LIGHT_SOURCE_RADIANCE = 1.5f

        private const val LASER_SPRITE_SIZE = 2f / ConstVals.PPM

        private var region: TextureRegion? = null
        private var redRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_DONE_DYIN)

    private val contactTimer = Timer(CONTACT_TIME)
    private val switchTimer = Timer(SWITCH_TIME)

    private val contactGlow = GameCircle().also {
        it.drawingColor = Color.WHITE
        it.drawingShapeType = ShapeType.Filled
    }

    private lateinit var rotatingLine: RotatingLine

    private lateinit var laserFixture: Fixture
    private lateinit var damagerFixture: Fixture

    private lateinit var spawnRoom: String

    private val contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
        val origin = rotatingLine.getOrigin()
        val d1 = p1.dst2(origin)
        val d2 = p2.dst2(origin)
        d1.compareTo(d2)
    }

    private var clockwise = false
    private var contactIndex = 0
    private var beaming = false
    private var speed = 0f

    private val blocksToIgnore = ObjectSet<Int>()
    private val lightSourceKeys = ObjectSet<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        if (redRegion == null) redRegion =
            game.assMan.getTextureRegion(TextureAsset.COLORS.source, "${ConstKeys.BRIGHT}_${ConstKeys.RED}")

        super.init()

        val prodShapeSuppliers: Array<() -> IDrawableShape?> = gdxArrayOf({ contactGlow }, /* { damagerFixture } */)
        addComponent(DrawableShapesComponent(prodShapeSuppliers = prodShapeSuppliers, debug = true))

        addComponent(defineCullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(MotionComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val maxRadius = spawnProps.getOrDefault(ConstKeys.RADIUS, DEFAULT_MAX_RADIUS, Float::class) * ConstVals.PPM
        speed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_SPEED, Float::class)
        rotatingLine = RotatingLine(spawn, maxRadius, speed * ConstVals.PPM, INIT_DEGREES)

        buildLaserSprites(maxRadius)

        contactTimer.reset()
        switchTimer.reset()

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.BLOCK)) {
                val id = (value as RectangleMapObject).toProps().get(ConstKeys.ID, Int::class)!!
                blocksToIgnore.add(id)
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

        beaming = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        contacts.clear()

        blocksToIgnore.clear()

        lightSourceKeys.clear()

        laserFixture.setShape(GameLine())
        damagerFixture.setShape(GameLine())

        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.PLAYER_DONE_DYIN) beaming = false
    }

    override fun isLaserIgnoring(block: Block) = blocksToIgnore.contains(block.mapObjectId)

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_ROOM pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.BEGIN_ROOM_TRANS),
                predicate@{ event -> return@predicate !event.isProperty(ConstKeys.NAME, spawnRoom) }
            )
        )
    )

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY
        addDebugShapeSupplier { body.getBounds() }

        laserFixture = Fixture(body, FixtureType.LASER, GameLine())
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
        laserFixture.attachedToBody = false
        body.addFixture(laserFixture)

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameLine())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM)
        )
        shieldFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        addDebugShapeSupplier { shieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            laserFixture.setShape(rotatingLine.line)
            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val laser = damagerFixture.rawShape as GameLine
            if (beaming) {
                val origin = rotatingLine.getOrigin()
                laser.setFirstLocalPoint(origin)

                val end = when {
                    contacts.isEmpty() || contacts.peek().dst(origin) > rotatingLine.line.getLength() ->
                        rotatingLine.getEndPoint()
                    else -> contacts.peek()
                }
                laser.setSecondLocalPoint(end)

                contactGlow.setCenter(end)

                laser.drawingColor = Color.RED
                laser.drawingShapeType = ShapeType.Filled
                laser.drawingThickness = 0.05f * ConstVals.PPM
                laser.drawingRenderType = GameLineRenderingType.BOTH
            } else {
                laser.set(Vector2.Zero, Vector2.Zero)
                contactGlow.setCenter(Vector2.Zero)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setPosition(rotatingLine.getOrigin(), Position.BOTTOM_CENTER)
            sprite.translateY(-0.1f * ConstVals.PPM)
        }
        .build()

    private fun buildLaserSprites(maxRadius: Float) {
        val iter = sprites.iterator()
        while (iter.hasNext) {
            val next = iter.next()
            if (next.key.toString().contains(ConstKeys.PIECE)) iter.remove()
        }

        val count = (maxRadius / (LASER_SPRITE_SIZE * ConstVals.PPM)).toInt()

        GameLogger.debug(TAG, "buildLaserSprites(): maxRadius=$maxRadius, count=$count")

        for (i in 0 until count) {
            val key = "${ConstKeys.PIECE}_$i"

            val sprite = GameSprite(redRegion!!)
            sprite.setSize(LASER_SPRITE_SIZE * ConstVals.PPM)
            sprites.put(key, sprite)

            putSpriteUpdateFunction(key) updateFunc@{ _, _ ->
                val laser = damagerFixture.getShape() as GameLine

                if (i * LASER_SPRITE_SIZE * ConstVals.PPM > laser.getLength()) {
                    sprite.hidden = true
                    return@updateFunc
                }

                sprite.hidden = false

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

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (!beaming) {
            if (megaman.spawned && megaman.ready && overlapsGameCamera()) {
                beaming = true
                requestToPlaySound(SoundAsset.LASER_BEAM_SOUND, false)
            } else return@UpdatablesComponent
        }

        if (shouldSendLightSourceEvents()) sendLightSourceEvents()

        contactTimer.update(it)
        if (contactTimer.isFinished()) {
            contactIndex++
            contactTimer.reset()
        }
        if (contactIndex > 2) contactIndex = 0
        contactGlow.setRadius(CONTACT_RADII[contactIndex])

        switchTimer.update(it)
        if (!switchTimer.isFinished()) return@UpdatablesComponent

        if (switchTimer.isJustFinished()) {
            clockwise = !clockwise
            var speed = this.speed * ConstVals.PPM
            if (clockwise) speed *= -1f
            rotatingLine.speed = speed
        }

        rotatingLine.update(it)

        if (clockwise && rotatingLine.degrees <= MIN_DEGREES) {
            rotatingLine.degrees = MIN_DEGREES
            switchTimer.reset()
        } else if (!clockwise && rotatingLine.degrees >= MAX_DEGREES) {
            rotatingLine.degrees = MAX_DEGREES
            switchTimer.reset()
        }
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

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
