package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.hazards.RisingLavaRiver
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class FrozenBlock(game: MegaServices) : IceBlock(game), ISpritesEntity {

    companion object {
        const val TAG = "FrozenBlock"
        private var region: TextureRegion? = null
    }

    override fun init(vararg params: Any) {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) && when (game.levelState.currentLevel!!) {
        LevelDefinition.INFERNO_MAN -> LevelUtils.isInfernoManLevelFrozen(game.state)
        else -> true
    }

    override fun onSpawn(spawnProps: Properties) {
        val copyProps = spawnProps.copy()
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(ConstVals.PPM.toFloat())
            .setCenter(center)
        copyProps.put(ConstKeys.BOUNDS, bounds)
        super.onSpawn(copyProps)
    }

    override fun hitByProjectile(projectileFixture: IFixture) {
        val projectile = projectileFixture.getEntity() as IProjectileEntity
        if (projectile is Fireball) smokePuffAndDie()
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val consumerBounds = GameRectangle()
        val consumerFixture = Fixture(body, FixtureType.CONSUMER, consumerBounds)
        consumerFixture.setReceiver(Receivers.FILTER) {
            it.getType() == FixtureType.DEATH && it.getEntity() is RisingLavaRiver
        }
        consumerFixture.setReceiver(Receivers.CONSUMER) { _, _ -> smokePuffAndDie() }
        body.addFixture(consumerFixture)

        val shieldBounds = GameRectangle()
        val shieldFixture = Fixture(body, FixtureType.SHIELD, shieldBounds)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.FIXTURES) {
            consumerBounds.set(body)
            shieldBounds.set(body)
        }

        return BodyComponentCreator.amend(this, component)
    }

    private fun smokePuffAndDie() {
        destroy()

        val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
        puff.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

        game.audioMan.playSound(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(ConstVals.PPM.toFloat()) })
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()
}
