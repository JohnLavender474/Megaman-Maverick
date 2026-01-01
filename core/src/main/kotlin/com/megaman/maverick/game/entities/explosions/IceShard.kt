package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.BreakableIce
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class IceShard(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "IceShard"

        private const val MAX_SHARDS = 5
        private val SPAWNED_QUEUE = Queue<IceShard>()

        private const val FADE_OUT_DUR = 0.25f

        private const val GRAVITY = -0.15f

        private val SCALARS = ObjectMap<String, Float>()

        private val TRAJECTORIES = gdxArrayOf(
            Vector2(-7f, 5f),
            Vector2(-3f, 7f),
            Vector2(0f, 9f),
            Vector2(3f, 7f),
            Vector2(7f, 5f),
        )

        private val TEXTURES = ObjectMap<String, Array<TextureRegion>>()

        fun spawn5(center: Vector2, tag: String = BreakableIce.TAG) {
            for (i in 0 until 5) {
                val shard = MegaEntityFactory.fetch(IceShard::class)!!
                shard.spawn(props(ConstKeys.POSITION pairTo center, ConstKeys.INDEX pairTo i, ConstKeys.TAG pairTo tag))
            }
        }
    }

    private val fadeOutTimer = Timer(FADE_OUT_DUR)
    private var fadeOut = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (TEXTURES.isEmpty) {
            val region1 = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "BreakableIce/Shards")
            val array1 = Array<TextureRegion>()
            val out1 = region1.splitAndFlatten(1, 5, array1)
            TEXTURES.put(BreakableIce.TAG, out1)
            SCALARS.put(BreakableIce.TAG, 1f)

            val region2 = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "IcicleShards")
            val array2 = Array<TextureRegion>()
            val out2 = region2.splitAndFlatten(1, 5, array2)
            TEXTURES.put(FallingIcicle.TAG, out2)
            SCALARS.put(FallingIcicle.TAG, 0.5f)
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val tag = spawnProps.getOrDefault(ConstKeys.TAG, BreakableIce.TAG, String::class)
        val scalar = SCALARS[tag]

        val index = spawnProps.get(ConstKeys.INDEX, Int::class)!!
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(TRAJECTORIES[index])
            .scl(scalar * ConstVals.PPM)
        body.physics.velocity.set(velocity)

        val region = TEXTURES[tag].get(index)
        defaultSprite.setRegion(region)
        defaultSprite.setSize(scalar * ConstVals.PPM)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ICE_SHARD_2_SOUND, false)

        fadeOutTimer.reset()
        fadeOut = false

        SPAWNED_QUEUE.addLast(this)
        if (SPAWNED_QUEUE.size > MAX_SHARDS) {
            val shardToFadeOut = SPAWNED_QUEUE.removeFirst()
            shardToFadeOut.setToFadeOut()
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        SPAWNED_QUEUE.removeValue(this, false)
    }

    private fun setToFadeOut() {
        fadeOut = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (fadeOut) {
            fadeOutTimer.update(delta)
            if (fadeOutTimer.isFinished()) destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL pairTo cullOnOutOfBounds))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(if (fadeOut) 1f - fadeOutTimer.getRatio() else 1f)
        }
        return component
    }

    override fun getType() = EntityType.EXPLOSION
}
