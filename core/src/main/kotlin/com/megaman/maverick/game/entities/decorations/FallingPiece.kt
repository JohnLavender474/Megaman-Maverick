package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class FallingPiece(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "FallingPiece"

        private const val LIFE = 2f
        private const val BODY_SIZE = 0.25f

        private val SPEEDS_X = floatArrayOf(-5.6f, -3.75f, 2.8f, 4.7f, -6.6f, -4.7f, 3.75f, 5.6f)
        private val SPEEDS_Y = floatArrayOf(6.6f, 8.4f, 11.25f, 5.6f, 7.5f, 9.4f, 10.3f, 4.7f)

        // TODO: make these region lists, and the keys of load and throwDebris, LibGDX Arrays after 060c and 139a merge (137a TODO.md backlog)
        val WOOD_CRATE_REGIONS = Array(10) { "WoodCratePiece/$it" }
        private val BLOCK_REGIONS = Array(BlockPieceColor.entries.size) { BlockPieceColor.entries[it].regionKey }

        private val regions = ObjectMap<String, TextureRegion>()

        fun load(services: MegaServices, keys: Array<String>, asset: TextureAsset = TextureAsset.ENEMIES_2) {
            val atlas = services.assets.getTextureAtlas(asset.source)
            keys.forEach { key ->
                if (!regions.containsKey(key))
                    regions.put(key, atlas.findRegion(key) ?: throw IllegalStateException("Missing region $key in ${asset.source}"))
            }
        }

        fun spawn(def: FallingPieceDef, key: String, position: Vector2, velocity: Vector2) {
            MegaEntityFactory.fetch(FallingPiece::class)!!.spawn(
                props(
                    ConstKeys.DEF pairTo def,
                    ConstKeys.KEY pairTo key,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.TRAJECTORY pairTo velocity
                )
            )
        }

        fun throwDebris(keys: Array<String>, position: Vector2, random: GameRandom) = keys.forEach { key ->
            val velocity = GameObjectPools.fetch(Vector2::class).set(
                SPEEDS_X[random.nextInt(0, SPEEDS_X.size - 1)] * ConstVals.PPM,
                SPEEDS_Y[random.nextInt(0, SPEEDS_Y.size - 1)] * ConstVals.PPM
            )
            spawn(FallingPieceDef.DEBRIS, key, GameObjectPools.fetch(Vector2::class).set(position), velocity)
        }
    }

    var key = ""
        private set

    private val lifeTimer = Timer(LIFE)
    private val spinTimer = Timer(1f)
    private lateinit var region: TextureRegion
    private lateinit var size: FallingPieceSize
    private var spin: FallingPieceSpin? = null
    private var blink: Float? = null
    private var angle = 0f
    private var elapsed = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        load(services, BLOCK_REGIONS, TextureAsset.PLATFORMS_1)
        load(services, WOOD_CRATE_REGIONS, TextureAsset.DECORATIONS_1)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val def = spawnProps.get(ConstKeys.DEF, FallingPieceDef::class)
        key = spawnProps.get(ConstKeys.KEY, String::class)!!
        region = regions.get(key) ?: throw IllegalStateException("Region $key is not loaded: call FallingPiece.load")
        body.physics.gravity.y = required(spawnProps, def, ConstKeys.GRAVITY, Float::class) { it.gravity } * ConstVals.PPM
        body.physics.velocityClamp.y =
            optional(spawnProps, def, ConstKeys.CLAMP, Float::class) { it.fallClamp }?.times(ConstVals.PPM) ?: Float.MAX_VALUE
        body.physics.applyFrictionX = required(spawnProps, def, ConstKeys.FRICTION_X, Boolean::class) { it.frictionX }
        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val jitter = required(spawnProps, def, ConstKeys.JITTER, Float::class) { it.jitter } * ConstVals.PPM
        if (jitter > 0f) body.setCenter(position.x + services.random.nextFloat(-jitter, jitter), position.y + services.random.nextFloat(-jitter, jitter))
        else body.setCenter(position)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        size = required(spawnProps, def, ConstKeys.SIZE, FallingPieceSize::class) { it.size }
        spin = optional(spawnProps, def, ConstKeys.SPIN, FallingPieceSpin::class) { it.spin }
        blink = optional(spawnProps, def, ConstKeys.BLINK, Float::class) { it.blink }
        angle = 0f
        spin?.let { spinTimer.resetDuration(it.delay) }
        lifeTimer.reset()
        elapsed = 0f
    }

    private fun <T : Any> optional(
        spawnProps: Properties,
        def: FallingPieceDef?,
        key: String,
        type: KClass<T>,
        fromDef: (FallingPieceDef) -> T?
    ): T? = when {
        spawnProps.containsKey(key) -> spawnProps.get(key).let { value ->
            if (value != null && !type.isInstance(value)) throw IllegalArgumentException(
                "$TAG spawn property '$key' is a ${value::class.simpleName}: it must be a ${type.simpleName}"
            )
            type.javaObjectType.cast(value)
        }
        def != null -> fromDef(def)
        else -> throw IllegalArgumentException(
            "$TAG has no ${ConstKeys.DEF} and no '$key' spawn property: give a FallingPieceDef or every value"
        )
    }

    private fun <T : Any> required(
        spawnProps: Properties,
        def: FallingPieceDef?,
        key: String,
        type: KClass<T>,
        fromDef: (FallingPieceDef) -> T
    ): T = optional(spawnProps, def, key, type, fromDef)
        ?: throw IllegalArgumentException("$TAG spawn property '$key' is null: it needs a value")

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        elapsed += delta
        spin?.let { spin ->
            spinTimer.update(delta)
            if (spinTimer.isFinished()) {
                angle += spin.step
                spinTimer.reset()
            }
        }
        lifeTimer.update(delta)
        if (lifeTimer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.collisionOn = false
        body.physics.applyFrictionY = false
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)))
        .preProcess { _, sprite ->
            sprite.setRegion(region)
            when (val size = size) {
                is FallingPieceSize.Tiles -> sprite.setSize(size.size * ConstVals.PPM)
                is FallingPieceSize.RegionScale ->
                    sprite.setSize(region.regionWidth * size.scale, region.regionHeight * size.scale)
            }
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = angle
            sprite.setAlpha(1f)
            sprite.hidden = blink.let { it != null && (elapsed / it).toInt() % 2 == 1 }
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
