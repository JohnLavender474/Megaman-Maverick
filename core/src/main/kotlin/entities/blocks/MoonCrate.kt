package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.isSensing

class MoonCrate(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IDirectional {

    companion object {
        const val TAG = "MoonBlock"
        private const val BODY_SIZE = 2f
        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds = GameObjectPools.fetch(GameRectangle::class).setSize(BODY_SIZE * ConstVals.PPM).setCenter(center)
        spawnProps.put(ConstKeys.BOUNDS, bounds)
        super.onSpawn(spawnProps)
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body
        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(2f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.DIRECTION) {
            this.direction = megaman().direction

            val value = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.let { gravity ->
                when (direction) {
                    Direction.UP -> gravity.set(0f, -value)
                    Direction.DOWN -> gravity.set(0f, value)
                    Direction.LEFT -> gravity.set(value, 0f)
                    Direction.RIGHT -> gravity.set(-value, 0f)
                }.scl(ConstVals.PPM.toFloat())
            }
        }

        return component
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!))
        .updatable { _, sprite ->
            sprite.setBounds(body.getBounds())
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()
}
