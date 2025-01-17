package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
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
        private const val GRAVITY = -0.15f
        private val TEXTURES = ObjectMap<String, Array<TextureRegion>>()
        private val SCALARS = ObjectMap<String, Float>()
        private val TRAJECTORIES = gdxArrayOf(
            Vector2(-7f, 5f),
            Vector2(-3f, 7f),
            Vector2(0f, 9f),
            Vector2(3f, 7f),
            Vector2(7f, 5f),
        )
    }

    override fun init() {
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
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
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
    }

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
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        sprite.setAlpha(0.75f)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.EXPLOSION
}
