package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.*
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.*
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
import com.mega.game.engine.drawables.sprites.setBounds
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
import com.megaman.maverick.game.utils.extensions.*
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

        private const val BEAM_DELAY = 1.5f
        private const val BEAM_DUR = 1f

        private const val SPAWN_EXPLOSION_DELAY = 0.2f

        private const val BEAM_WIDTH = 0.125f
        private const val BEAM_HEIGHT = 1f
        private const val BEAM_GROWTH_RATE = 16f
        private const val BEAM_REGION_KEY = "TubeBeam_short"
        private const val BEAM_DELAY_FLASH_RATIO = 0.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var direction: Direction

    private val rawLine = GameLine()
    private val maxLine = GameLine()
    private val actualLine = GameLine()

    private val contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
        val (origin, _) = rawLine.getWorldPoints()
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
        if (regions.isEmpty) {
            regions.put(
                BEAM_REGION_KEY,
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, BEAM_REGION_KEY)
            )
            gdxArrayOf(ConstKeys.BLACK, ConstKeys.WHITE, ConstKeys.PINK).forEach { color ->
                regions.put(color, game.assMan.getTextureRegion(TextureAsset.COLORS.source, color))
            }
        }
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

        rawLine.setFirstLocalPoint(spawn)
        val endPoint = spawn.add(MAX_LENGTH * ConstVals.PPM, direction)
        rawLine.setSecondLocalPoint(endPoint)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                ignoreIds.add(id)
            }
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        beamDelay.reset()
        beamTimer.setToEnd()
        spawnExplosionDelay.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        ignoreIds.clear()

        rawLine.reset()
        maxLine.reset()
        actualLine.reset()
    }

    override fun isLaserIgnoring(block: Block) = ignoreIds.contains(block.mapObjectId)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when {
            beaming -> {
                beamTimer.update(delta)
                if (beamTimer.isFinished()) beamDelay.reset()

                when {
                    actualLine.getLength() < maxLine.getLength() -> {
                        val nextEndPoint = actualLine.getSecondLocalPoint()
                            .add(BEAM_GROWTH_RATE * delta * ConstVals.PPM, direction)
                        actualLine.setSecondLocalPoint(nextEndPoint)
                    }

                    else -> {
                        actualLine.set(maxLine)

                        spawnExplosionDelay.update(delta)
                        if (spawnExplosionDelay.isFinished()) {
                            val spawn = actualLine.getWorldPoints().second

                            val explosion = MegaEntityFactory.fetch(TubeBeamExplosion::class)!!
                            explosion.spawn(
                                props(
                                    ConstKeys.OWNER pairTo this,
                                    ConstKeys.POSITION pairTo spawn
                                )
                            )

                            spawnExplosionDelay.reset()
                        }
                    }
                }
            }

            else -> {
                beamDelay.update(delta)
                if (beamDelay.isFinished()) {
                    val start = rawLine.getFirstLocalPoint()
                    actualLine.setLocalPoints(start, start)

                    beamTimer.reset()
                    spawnExplosionDelay.setToEnd()

                    if (game.getGameCamera().overlaps(maxLine.getBoundingRectangle()))
                        requestToPlaySound(SoundAsset.BURST_SOUND, false)
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val laserFixture = Fixture(body, FixtureType.LASER, rawLine)
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

        val updateDamager = {
            damagerFixture.setActive(beaming)

            val damagerBounds = damagerFixture.rawShape as GameRectangle
            when {
                beaming -> {
                    damagerBounds.setSize(actualLine.getLength(), BEAM_HEIGHT * ConstVals.PPM)
                    val origin = actualLine.getCenter()
                    damagerBounds.setCenter(origin)
                    damagerBounds.rotate(direction.rotation + 90f, origin.x, origin.y)
                }

                else -> damagerBounds.set(-100f * ConstVals.PPM, -100f * ConstVals.PPM, 0f, 0f)
            }
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            contacts.clear()
            updateDamager()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val (lineStart, lineEnd) = rawLine.getWorldPoints()
            maxLine.setFirstLocalPoint(lineStart)
            actualLine.setFirstLocalPoint(lineStart)
            val end = if (contacts.isEmpty()) lineEnd else contacts.peek()
            maxLine.setSecondLocalPoint(end)

            updateDamager()
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
            val key = "${ConstKeys.BEAM}_$i"

            val sprite = GameSprite()
            sprite.setSize(BEAM_WIDTH * ConstVals.PPM, BEAM_HEIGHT * ConstVals.PPM)
            sprites.put(key, sprite)

            updaters.put(key) { _, _ ->
                sprite.setOriginCenter()
                sprite.rotation = direction.rotation + 90f

                val center = rawLine.getWorldPoints().first
                    .add(i * BEAM_WIDTH * ConstVals.PPM.toFloat(), direction)
                    .add(BEAM_WIDTH * ConstVals.PPM / 2f, direction)
                sprite.setCenter(center)

                sprite.hidden = !beaming || !actualLine.contains(center)
            }

            val animation = Animation(regions[BEAM_REGION_KEY], 2, 2, 0.05f, true)
            val animator = Animator(animation)
            animators.put(key, animator)
        }

        val beamer = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
        sprites.put(ConstKeys.BEAMER, beamer)
        updaters.put(ConstKeys.BEAMER) { _, _ -> beamer.setBounds(body.getBounds()) }

        val beamerAnims = objectMapOf<String, IAnimation>(
            ConstKeys.BLACK pairTo Animation(regions[ConstKeys.BLACK]),
            ConstKeys.PRIOR pairTo Animation(gdxArrayOf(regions[ConstKeys.PINK], regions[ConstKeys.WHITE]), 0.1f, true),
            ConstKeys.BEAM pairTo Animation(gdxArrayOf(regions[ConstKeys.PINK], regions[ConstKeys.WHITE]), 0.05f, true)
        )
        val beamerKeySupplier: (String?) -> String? = key@{
            return@key when {
                beaming -> ConstKeys.BEAM
                beamDelay.getRatio() >= BEAM_DELAY_FLASH_RATIO -> ConstKeys.PRIOR
                else -> ConstKeys.BLACK
            }
        }
        val beamerAnimator = Animator(beamerKeySupplier, beamerAnims)
        animators.put(ConstKeys.BEAMER, beamerAnimator)

        addComponent(SpritesComponent(sprites, updaters))
        addComponent(AnimationsComponent(animators, sprites))
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
