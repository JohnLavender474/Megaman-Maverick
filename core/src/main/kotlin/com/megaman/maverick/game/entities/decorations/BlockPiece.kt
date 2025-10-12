package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class BlockPiece(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "BlockPiece"
        private const val ALPHA = 1f
        private const val CULL_TIME = 2f
        private const val GRAVITY = 0.25f
        private const val START_ROTATION = 135f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class BlockPieceColor { RED, GOLD, BROWN, PINK }

    private lateinit var color: BlockPieceColor
    private val cullTimer = Timer(CULL_TIME)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
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
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
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
                .also { sprite -> sprite.setSize(0.5f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setRegion(regions[color.name.lowercase()])
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(ALPHA)
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + START_ROTATION
        }
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
