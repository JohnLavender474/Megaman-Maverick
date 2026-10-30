package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAudioEntity
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class BlockPiece(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "BlockPiece"

        private const val ALPHA = 1f
        private const val CULL_TIME = 2f
        private const val GRAVITY = 37.5f
        private const val ROTATION_DELAY = 0.1f
        private const val ROTATION_DELTA = 45f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class BlockPieceColor { RED, GOLD, BROWN, PINK }

    private lateinit var color: BlockPieceColor

    private val cullTimer = Timer(CULL_TIME)

    private var rotation = 0f
    private val rotationTimer = Timer(ROTATION_DELAY)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            BlockPieceColor.entries.forEach { color ->
                val key = color.name.lowercase()
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        color = spawnProps.getOrDefault(ConstKeys.COLOR, BlockPieceColor.RED, BlockPieceColor::class)

        cullTimer.reset()

        rotation = 0f
        rotationTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        rotationTimer.update(delta)
        if (rotationTimer.isFinished()) {
            rotation += ROTATION_DELTA
            rotationTimer.reset()
        }

        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.collisionOn = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = -GRAVITY * ConstVals.PPM
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
                .also { sprite -> sprite.setSize(1f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setRegion(regions[color.name.lowercase()])

            sprite.setCenter(body.getCenter())

            sprite.setAlpha(ALPHA)

            sprite.setOriginCenter()
            sprite.rotation = rotation
        }
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
