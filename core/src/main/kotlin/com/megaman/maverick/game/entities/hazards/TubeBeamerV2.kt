package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.add
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.*
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
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.ILaserEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.TubeBeamExplosion
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.getWorldPoints
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import java.util.*

class TubeBeamerV2(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, IAudioEntity,
    ISpritesEntity, IAnimatedEntity, ILaserEntity, IDirectional, IHazard, IDamager {

    companion object {
        const val TAG = "TubeBeamerV2"

        private const val MAX_LENGTH = 20f
        private const val BODY_SIZE = 1f

        private const val BEAM_DELAY = 2f
        private const val BEAM_DUR = 1f

        private const val SPAWN_EXPLOSION_DELAY = 0.2f

        private const val BEAM_WIDTH = 0.125f
        private const val BEAM_HEIGHT = 1f
        private const val BEAM_REGION_KEY = "TubeBeam_short"

        private var region: TextureRegion? = null
    }

    override lateinit var direction: Direction

    private val line = GameLine()
    private val beam = GameLine()

    private val contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
        val (origin, _) = line.getWorldPoints()
        val d1 = p1.dst2(origin)
        val d2 = p2.dst2(origin)
        d1.compareTo(d2)
    }

    private val ignoreIds = ObjectSet<Int>()

    private lateinit var spawnRoom: String

    private val beamDelay = Timer(BEAM_DELAY)
    private val beamTimer = Timer(BEAM_DUR)
    private val beaming: Boolean
        get() = !beamTimer.isFinished()

    private val spawnExplosionDelay = Timer(SPAWN_EXPLOSION_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, BEAM_REGION_KEY)
        super.init()
        defineDrawableComponents()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        direction = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())

        val position = DirectionPositionMapper.getPosition(direction)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        line.setFirstLocalPoint(spawn)
        val endPoint = spawn.add(MAX_LENGTH * ConstVals.PPM, direction)
        line.setSecondLocalPoint(endPoint)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                ignoreIds.add(id)
            }
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        beamDelay.reset()
        beamTimer.setToEnd()
        spawnExplosionDelay.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        ignoreIds.clear()

        beam.reset()
        line.reset()
    }

    override fun isLaserIgnoring(block: Block) = ignoreIds.contains(block.mapObjectId)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (beaming) {
            beamTimer.update(delta)
            if (beamTimer.isFinished()) beamDelay.reset()

            spawnExplosionDelay.update(delta)
            if (spawnExplosionDelay.isFinished()) {
                val explosion = MegaEntityFactory.fetch(TubeBeamExplosion::class)!!
                explosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.POSITION pairTo beam.getWorldPoints().second
                    )
                )

                spawnExplosionDelay.reset()
            }
        } else {
            beamDelay.update(delta)
            if (beamDelay.isFinished()) {
                beamTimer.reset()
                requestToPlaySound(SoundAsset.BURST_SOUND, false)
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val laserFixture = Fixture(body, FixtureType.LASER, line)
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
        laserFixture.attachedToBody = false
        body.addFixture(laserFixture)
        laserFixture.drawingColor = Color.YELLOW
        debugShapes.add { laserFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val (lineStart, lineEnd) = line.getWorldPoints()
            beam.setFirstLocalPoint(lineStart)
            val end = if (contacts.isEmpty()) lineEnd else contacts.peek()
            beam.setSecondLocalPoint(end)

            damagerFixture.setActive(beaming)

            val damager = damagerFixture.rawShape as GameRectangle
            damager.setSize(beam.getLength(), BEAM_HEIGHT * ConstVals.PPM)
            val origin = beam.getCenter()
            damager.setCenter(origin)
            damager.rotate(direction.rotation + 90f, origin.x, origin.y)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
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

    private fun defineDrawableComponents() {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = OrderedMap<Any, IAnimator>()
        val updaters = ObjectMap<Any, UpdateFunction<GameSprite>>()

        val beamCount = MAX_LENGTH.div(BEAM_WIDTH).toInt()
        for (i in 0 until beamCount) {
            val key = i.toString()

            val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
            sprite.setSize(BEAM_WIDTH * ConstVals.PPM, BEAM_HEIGHT * ConstVals.PPM)
            sprites.put(key, sprite)

            updaters.put(key) { _, _ ->
                sprite.setOriginCenter()
                sprite.rotation = direction.rotation + 90f

                val center = line.getWorldPoints().first
                    .add(i * BEAM_WIDTH * ConstVals.PPM.toFloat(), direction)
                    .add(BEAM_WIDTH * ConstVals.PPM / 2f, direction)
                sprite.setCenter(center)

                sprite.hidden = !beaming || !beam.contains(center)
            }

            val animation = Animation(region!!, 2, 2, 0.05f, true)
            val animator = Animator(animation)
            animators.put(key, animator)
        }

        addComponent(SpritesComponent(sprites, updaters))
        addComponent(AnimationsComponent(animators, sprites))
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
