package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getRandom
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

// TODO: DebrisPiece, BlockPiece and WoodCratePiece are the same kind of falling piece; merge them into one entity (item 137)
class DebrisPiece(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "DebrisPiece"

        private const val GRAVITY = -35f
        private const val FALL_MAX = 12f
        private const val LIFE = 2f
        private const val BLINK = 0.033f
        private const val START_OFFSET = 0.2f
        private const val SIZE = 0.25f

        private val SPEEDS_X = floatArrayOf(-5.6f, -3.75f, 2.8f, 4.7f, -6.6f, -4.7f, 3.75f, 5.6f)
        private val SPEEDS_Y = floatArrayOf(6.6f, 8.4f, 11.25f, 5.6f, 7.5f, 9.4f, 10.3f, 4.7f)

        private val regions = ObjectMap<String, TextureRegion>()

        fun load(services: MegaServices, keys: Array<String>, asset: TextureAsset = TextureAsset.ENEMIES_2) {
            val atlas = services.assets.getTextureAtlas(asset.source)
            keys.forEach { key ->
                if (!regions.containsKey(key))
                    regions.put(key, atlas.findRegion(key) ?: throw IllegalStateException("Missing debris region $key"))
            }
        }

        fun spawnPieces(keys: Array<String>, position: Vector2, parentSpeedX: Float = 0f) = keys.forEach { key ->
            val debris = MegaEntityFactory.fetch(DebrisPiece::class)!!
            debris.spawn(
                props(
                    ConstKeys.KEY pairTo key,
                    ConstKeys.POSITION pairTo position.cpy(),
                    ConstKeys.SPEED pairTo parentSpeedX
                )
            )
        }

        fun spawnPiece(key: String, position: Vector2, velocity: Vector2) {
            val debris = MegaEntityFactory.fetch(DebrisPiece::class)!!
            debris.spawn(
                props(
                    ConstKeys.KEY pairTo key,
                    ConstKeys.POSITION pairTo position.cpy(),
                    ConstKeys.TRAJECTORY pairTo velocity.cpy()
                )
            )
        }
    }

    var key = ""
        private set

    private val lifeTimer = Timer(LIFE)
    private var region: TextureRegion? = null
    private var elapsed = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(UpdatablesComponent({ delta ->
            elapsed += delta
            lifeTimer.update(delta)
            if (lifeTimer.isFinished()) destroy()
        }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        key = spawnProps.get(ConstKeys.KEY, String::class)!!
        region = regions.get(key) ?: throw IllegalStateException("Debris region $key is not loaded: call DebrisPiece.load")

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val offset = START_OFFSET * ConstVals.PPM
        body.setCenter(position.x + getRandom(-offset, offset), position.y + getRandom(-offset, offset))

        val given = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)
        if (given != null) body.physics.velocity.set(given)
        else {
            val parentSpeedX = spawnProps.getOrDefault(ConstKeys.SPEED, 0f, Float::class)
            val speedX = SPEEDS_X[getRandom(0, SPEEDS_X.size - 1)] + parentSpeedX / ConstVals.PPM / 2f
            val speedY = SPEEDS_Y[getRandom(0, SPEEDS_Y.size - 1)]
            body.physics.velocity.set(speedX * ConstVals.PPM, speedY * ConstVals.PPM)
        }

        lifeTimer.reset()
        elapsed = 0f
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(SIZE * ConstVals.PPM)
        body.physics.velocityClamp.y = FALL_MAX * ConstVals.PPM
        body.physics.collisionOn = false
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)))
        .preProcess { _, sprite ->
            val region = region!!
            sprite.setRegion(region)
            sprite.setSize(region.regionWidth * TickFrames.SCALE, region.regionHeight * TickFrames.SCALE)
            sprite.setCenter(body.getCenter())
            sprite.hidden = (elapsed / BLINK).toInt() % 2 == 1
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
