package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class IceShard(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "IceShard"
        private const val GRAVITY = -0.15f
        private val TEXTURES = Array<TextureRegion>()
        private val TRAJECTORIES = gdxArrayOf(
            Vector2(-7f, 5f),
            Vector2(-3f, 7f),
            Vector2(0f, 9f),
            Vector2(3f, 7f),
            Vector2(7f, 5f),
        )
    }

    override fun getEntityType() = EntityType.EXPLOSION

    override fun init() {
        if (TEXTURES.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            val region = atlas.findRegion("BreakableIce/Shards")
            region.splitAndFlatten(1, 5, TEXTURES)
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

        val index = spawnProps.get(ConstKeys.INDEX, Int::class)!!
        body.physics.velocity = TRAJECTORIES[index].cpy().scl(ConstVals.PPM.toFloat())

        val region = TEXTURES[index]
        defaultSprite.setRegion(region)

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

}
