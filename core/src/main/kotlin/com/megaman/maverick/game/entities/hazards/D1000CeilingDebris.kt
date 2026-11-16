package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.DustPuff
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class D1000DebrisDrop {
    var key = ""
    var startX = 0f
    val velocity = Vector2()
    var spin = 0f
    var delay = 0f
    var dropped = false

    val radius: Float
        get() = D1000CeilingDebris.radius(key)
}

class D1000CeilingDebris(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IDamagerEntity, IHazard, IOwnable<IGameEntity> {

    companion object {
        const val TAG = "D1000CeilingDebris"
        const val DAMAGE = 1
        const val GRAVITY = 28f * ConstVals.PPM
        const val MAX_START_SPEED = 2f * ConstVals.PPM
        const val MAX_DRIFT_SPEED = 1.5f * ConstVals.PPM
        const val MIN_SPIN = 360f
        const val MAX_SPIN = 1080f
        const val MIN_PIECES = 2
        const val MAX_PIECES = 3
        const val MIN_GAP = 0.05f
        const val MAX_GAP = 0.15f
        const val MAX_LAST_DELAY = (MAX_PIECES - 1) * MAX_GAP
        val BIG_KEYS = gdxArrayOf("chunk4", "chunk5", "chunk6")
        val SMALL_KEYS = gdxArrayOf("chunk1", "chunk2", "chunk3")
        val SIZES: OrderedMap<String, Vector2> = orderedMapOf(
            "chunk1" pairTo Vector2(6f, 5f),
            "chunk2" pairTo Vector2(9f, 6f),
            "chunk3" pairTo Vector2(11f, 8f),
            "chunk4" pairTo Vector2(13f, 9f),
            "chunk5" pairTo Vector2(16f, 10f),
            "chunk6" pairTo Vector2(18f, 13f)
        )
        val MAX_RADIUS = SIZES.values().maxOf { radius(it) }

        private val regions = ObjectMap<String, TextureRegion>()

        fun size(key: String): Vector2 = SIZES[key] ?: throw IllegalArgumentException("$TAG: no chunk $key")

        fun radius(key: String) = radius(size(key))

        private fun radius(size: Vector2) = max(size.x, size.y) / 2f

        fun fallTime(drop: Float, startSpeed: Float) =
            (-startSpeed + sqrt(startSpeed * startSpeed + 2f * GRAVITY * max(drop, 0f))) / GRAVITY

        fun landingBand(out: GameRectangle, lane: Float, spread: Float, minLanding: Float, maxLanding: Float): GameRectangle {
            val left = min(lane - spread, (lane - spread).coerceIn(minLanding, maxLanding))
            val right = max(lane + spread, (lane + spread).coerceIn(minLanding, maxLanding))
            return out.set(left - MAX_RADIUS, 0f, right - left + 2f * MAX_RADIUS, 0f)
        }

        fun roll(
            out: Array<D1000DebrisDrop>,
            lane: Float,
            spread: Float,
            minLanding: Float,
            maxLanding: Float,
            hangY: Float,
            floorY: Float,
            random: GameRandom
        ) {
            val count = random.nextInt(MIN_PIECES, MAX_PIECES)
            var delay = 0f
            for (i in 0 until count) {
                val drop = if (i < out.size) out[i] else D1000DebrisDrop().also { out.add(it) }
                val big = i == 0
                drop.key = if (big) BIG_KEYS[random.nextInt(0, BIG_KEYS.size - 1)] else SMALL_KEYS[random.nextInt(0, SMALL_KEYS.size - 1)]
                drop.startX = if (big) lane else lane + spread * (2f * random.nextFloat(0f, 1f) - 1f)
                val landing = (lane + spread * (2f * random.nextFloat(0f, 1f) - 1f)).coerceIn(minLanding, maxLanding)
                val startSpeed = MAX_START_SPEED * random.nextFloat(0f, 1f)
                val time = fallTime(hangY - size(drop.key).y / 2f - floorY, startSpeed)
                val drift = ((landing - drop.startX) / time).coerceIn(-MAX_DRIFT_SPEED, MAX_DRIFT_SPEED)
                drop.velocity.set(drift, -startSpeed)
                drop.spin = (MIN_SPIN + (MAX_SPIN - MIN_SPIN) * random.nextFloat(0f, 1f)) * (if (random.nextBoolean()) 1f else -1f)
                drop.delay = delay
                drop.dropped = false
                delay += MIN_GAP + (MAX_GAP - MIN_GAP) * random.nextFloat(0f, 1f)
            }
            out.truncate(count)
        }
    }

    override var owner: IGameEntity? = null

    var key = ""
        private set
    var spin = 0f
        private set
    var rotation = 0f
        private set
    private var floorY = 0f
    private val damagerShape = GameRectangle()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
            SIZES.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        key = spawnProps.get(ConstKeys.KEY, String::class)!!
        val size = size(key)
        body.setSize(size.x, size.y)
        damagerShape.setSize(size.x, size.y)
        body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.gravity.set(0f, -GRAVITY)
        spin = spawnProps.get(ConstKeys.SPIN, Float::class)!!
        rotation = 0f
        floorY = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getY()
    }

    override fun onDestroy() {
        super.onDestroy()
        owner = null
    }

    override fun getDamageToMegaman() = DAMAGE

    override fun onDamageInflictedTo(damageable: IDamageableEntity) = breakUp()

    private fun update(delta: Float) {
        rotation += spin * delta
        if (body.getY() <= floorY) {
            body.setY(floorY)
            breakUp()
        }
    }

    private fun breakUp() {
        destroy()
        val dust = MegaEntityFactory.fetch(DustPuff::class)!!
        dust.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(MAX_RADIUS * 2f)
        damagerShape.setSize(MAX_RADIUS * 2f)
        body.addFixture(Fixture(body, FixtureType.DAMAGER, damagerShape))
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setRegion(regions[key])
            val size = size(key)
            sprite.setSize(size.x, size.y)
            val center = body.getCenter()
            sprite.setCenter(center.x, center.y)
            sprite.setOriginCenter()
            sprite.rotation = rotation
            sprite.setFlip(body.physics.velocity.x < 0f, false)
        }
        return component
    }

    override fun getType() = EntityType.HAZARD
}
