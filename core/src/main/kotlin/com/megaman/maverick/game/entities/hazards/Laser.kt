package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.toInt
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.ILaserEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.special.DarknessV2
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getWorldPoints
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*

class Laser(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IHazard, IDamager,
    ILaserEntity, IActivatable {

    companion object {
        const val TAG = "Laser"

        private const val DEFAULT_MAX_LENGTH = 20f

        private const val LIGHT_SOURCE_OFFSET = 0.5f
        private const val LIGHT_SOURCE_RADIUS = 1
        private const val LIGHT_SOURCE_RADIANCE = 1.5f

        private const val LASER_SPRITE_SIZE = 2f / ConstVals.PPM

        private var region: TextureRegion? = null
    }

    override var on = true

    private val line = GameLine()

    private lateinit var laserFixture: Fixture
    private lateinit var damagerFixture: Fixture

    private val contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
        val origin = getOrigin()
        val d1 = p1.dst2(origin)
        val d2 = p2.dst2(origin)
        d1.compareTo(d2)
    }

    private val blocksToIgnore = ObjectSet<Int>()
    private val lightSourceKeys = ObjectSet<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region =
            game.assMan.getTextureRegion(TextureAsset.COLORS.source, "${ConstKeys.BRIGHT}_${ConstKeys.RED}")
        super.init()
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

        val origin = spawnProps.getOrDefault(ConstKeys.ORIGIN, Vector2.Zero, Vector2::class)
        line.setOrigin(origin)

        body.set(line.getBoundingRectangle())

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

        on = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        sprites.clear()
        contacts.clear()
        blocksToIgnore.clear()
        lightSourceKeys.clear()

        laserFixture.setShape(GameLine())
        damagerFixture.setShape(GameLine())
    }

    fun getOrigin(reclaim: Boolean = true): Vector2 =
        GameObjectPools.fetch(Vector2::class, reclaim).set(line.originX, line.originY)

    fun set(line: GameLine) = this.line.set(line)

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        laserFixture = Fixture(body, FixtureType.LASER, GameLine())
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
        laserFixture.attachedToBody = false
        body.addFixture(laserFixture)

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameLine())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture -> fixture.setActive(on) }

            if (on) {
                body.set(line.getBoundingRectangle())
                laserFixture.setShape(line)
            }

            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            if (on) {
                val damager = damagerFixture.rawShape as GameLine

                val origin = getOrigin()
                damager.setFirstLocalPoint(origin)

                val end = when {
                    contacts.isEmpty() || contacts.peek().dst(origin) > line.getLength() -> {
                        val (_, endPoint) = line.getWorldPoints()
                        endPoint
                    }
                    else -> contacts.peek()
                }
                damager.setSecondLocalPoint(end)
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (shouldSendLightSourceEvents()) sendLightSourceEvents()
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

            val sprite = GameSprite(region!!)
            sprite.setSize(LASER_SPRITE_SIZE * ConstVals.PPM)
            sprites.put(key, sprite)

            putSpriteUpdateFunction(key) updateFunc@{ _, _ ->
                val laser = damagerFixture.getShape() as GameLine

                if (i * LASER_SPRITE_SIZE * ConstVals.PPM > laser.getLength()) {
                    sprite.hidden = true
                    return@updateFunc
                }

                sprite.hidden = !on

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

    override fun isLaserIgnoring(block: Block) = blocksToIgnore.contains(block.mapObjectId)

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
