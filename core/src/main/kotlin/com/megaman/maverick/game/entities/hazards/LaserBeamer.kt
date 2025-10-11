package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getFirstLocalPoint
import com.megaman.maverick.game.utils.extensions.getOrigin
import com.megaman.maverick.game.utils.extensions.getSecondLocalPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class LaserBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity,
    ICullableEntity, IAudioEntity, IEventListener {

    companion object {
        const val TAG = "LaserBeamer"

        private const val DEFAULT_SPEED = 2f
        private const val DEFAULT_MAX_RADIUS = 20f

        private const val SWITCH_TIME = 1f

        private const val MIN_DEGREES = 200f
        private const val MAX_DEGREES = 340f
        private const val INIT_DEGREES = 270f

        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_DONE_DYIN)

    private var laser: Laser? = null
    private lateinit var spawnRoom: String
    private val switchTimer = Timer(SWITCH_TIME)
    private lateinit var rotatingLine: RotatingLine

    private var clockwise = false
    private var beaming = false
    private var speed = 0f

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(MotionComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
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
        rotatingLine.line.drawingColor = Color.ORANGE

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        switchTimer.reset()

        beaming = false

        laser = MegaEntityFactory.fetch(Laser::class)
        val laserProps = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.ORIGIN pairTo GameObjectPools.fetch(Vector2::class)
                .set(rotatingLine.line.originX, rotatingLine.line.originY),
            "${ConstKeys.FIRST}_${ConstKeys.POINT}" pairTo rotatingLine.line.getFirstLocalPoint(),
            "${ConstKeys.SECOND}_${ConstKeys.POINT}" pairTo rotatingLine.line.getSecondLocalPoint(),
        )
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) laserProps.put(key, value)
        }
        laser!!.spawn(laserProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        laser?.destroy()
        laser = null
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.PLAYER_DONE_DYIN) beaming = false
    }

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

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        debugShapes.add { rotatingLine.line }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM)
        )
        shieldFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setPosition(rotatingLine.getOrigin(), Position.BOTTOM_CENTER)
            sprite.translateY(-0.1f * ConstVals.PPM)
        }
        .build()

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        if (!beaming) {
            if (megaman.spawned && megaman.ready && overlapsGameCamera()) {
                beaming = true
                laser?.on = true
                requestToPlaySound(SoundAsset.LASER_BEAM_SOUND, false)
            } else {
                laser?.on = false
                return@update
            }
        }

        laser?.set(rotatingLine.line)

        switchTimer.update(delta)
        if (!switchTimer.isFinished()) return@update

        if (switchTimer.isJustFinished()) {
            clockwise = !clockwise

            var speed = this.speed * ConstVals.PPM
            if (clockwise) speed *= -1f

            rotatingLine.speed = speed
        }

        rotatingLine.update(delta)

        if (clockwise && rotatingLine.degrees <= MIN_DEGREES) {
            rotatingLine.degrees = MIN_DEGREES
            switchTimer.reset()
        } else if (!clockwise && rotatingLine.degrees >= MAX_DEGREES) {
            rotatingLine.degrees = MAX_DEGREES
            switchTimer.reset()
        }
    })

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
