package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.audio.AudioComponent
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.world.getEntity

class BreakableIce(game: MegamanMaverickGame) : IceBlock(game), ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "BreakableIce"
        private const val BREAK_INDEX = 4
        private var region1: TextureRegion? = null
        private var region2: TextureRegion? = null
        private var region3: TextureRegion? = null
    }

    private var index = 1

    override fun init() {
        if (region1 == null || region2 == null || region3 == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            region1 = atlas.findRegion("BreakableIce/1")
            region2 = atlas.findRegion("BreakableIce/2")
            region3 = atlas.findRegion("BreakableIce/3")
        }
        super<IceBlock>.init()
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent(this))
    }

    override fun spawn(spawnProps: Properties) { // spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)
        index = 1
    }

    override fun hitByFeet(feetFixture: IFixture) = hit()

    override fun hitByHead(headFixture: IFixture) = hit()

    override fun hitByProjectile(projectileFixture: IFixture) {
        val projectile = projectileFixture.getEntity() as AbstractProjectile
        when (projectile) {
            is Bullet -> hit()
            is ChargedShot -> {
                if (projectile.fullyCharged) explodeAndDie()
                else hit(2)
            }
        }
    }

    private fun hit(increment: Int = 1) {
        index += increment
        if (index == BREAK_INDEX) explodeAndDie() else requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
    }

    private fun explodeAndDie() {
        for (i in 0 until 5) {
            val iceShard = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.ICE_SHARD)!!
            game.engine.spawn(iceShard, props(ConstKeys.POSITION to body.getCenter(), ConstKeys.INDEX to i))
        }
        kill()
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setRegion(
                when (index) {
                    1 -> region1
                    2 -> region2
                    3 -> region3
                    else -> throw IllegalStateException("Invalid index: $index")
                }
            )
        }
        return spritesComponent
    }
}

