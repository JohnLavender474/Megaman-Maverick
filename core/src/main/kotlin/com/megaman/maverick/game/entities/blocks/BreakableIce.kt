package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

class BreakableIce(game: MegamanMaverickGame) : IceBlock(game), ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "BreakableIce"
        private const val BREAK_INDEX = 4
        private var region1: TextureRegion? = null
        private var region2: TextureRegion? = null
        private var region3: TextureRegion? = null
    }

    private var index = 1
    private var hit = false

    override fun init() {
        if (region1 == null || region2 == null || region3 == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            region1 = atlas.findRegion("$TAG/1")
            region2 = atlas.findRegion("$TAG/2")
            region3 = atlas.findRegion("$TAG/3")
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        hit = false
        index = 1
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        if (processState == ProcessState.BEGIN) hit()
    }

    override fun hitByHead(processState: ProcessState, headFixture: IFixture) {
        if (processState == ProcessState.BEGIN) hit()
    }

    override fun hitByProjectile(projectileFixture: IFixture) {
        val projectile = projectileFixture.getEntity() as IProjectileEntity
        when (projectile) {
            is Bullet, is TeardropBlast -> hit()
            is Fireball, is MoonScythe -> explodeAndDie()
            is ChargedShot -> if (projectile.fullyCharged) explodeAndDie() else hit(2)
        }
    }

    private fun hit(increment: Int = 1) {
        index += increment
        when {
            index >= BREAK_INDEX -> explodeAndDie()
            overlapsGameCamera() -> requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
        }
    }

    private fun explodeAndDie() {
        for (i in 0 until 5) {
            val shard = MegaEntityFactory.fetch(IceShard::class)!!
            shard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
        }

        destroy()
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setRegion(
                when (index) {
                    1 -> region1
                    2 -> region2
                    else -> region3
                }
            )
        }
        return spritesComponent
    }
}

