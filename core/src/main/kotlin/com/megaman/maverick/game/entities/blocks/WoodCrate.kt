package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setEntity

class WoodCrate(game: MegamanMaverickGame) : Block(game), ISpritesEntity {

    companion object {
        const val TAG = "WoodCrate"

        private const val HIT_BY_FEET_IMPULSE_Y_ABOVE_WATER = -5f
        private const val HIT_BY_FEET_IMPULSE_Y_IN_WATER = -1f

        private const val FLOAT_UP_IMPULSE_Y = 2f
        private const val FLOAT_UP_MAX_VEL_Y = 3f

        private var region: TextureRegion? = null
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val copyProps = spawnProps.copy()
        copyProps.remove(ConstKeys.BOUNDS)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps, copyProps=$copyProps")
        super.onSpawn(copyProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body
        body.setSize(2f * ConstVals.PPM)

        val waterListener = Fixture(body, FixtureType.WATER_LISTENER, GameCircle().setRadius(0.1f * ConstVals.PPM))
        waterListener.setEntity(this)
        body.addFixture(waterListener)
        waterListener.drawingColor = Color.BLUE
        debugShapeSuppliers.add { waterListener }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.setEntity(this)
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapeSuppliers.add { feetFixture }

        body.preProcess.put(ConstKeys.FALL) {
            if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y < 0f)
                body.physics.velocity.y = 0f
        }

        return component
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        // If the block doesn't have its "feet" on the ground and is not sensing itself as being
        // "in the water", then that means that the block must be floating directly above water,
        // in which case it can be pushed down on the BEGIN state.
        if (processState == ProcessState.BEGIN &&
            !body.isSensing(BodySense.FEET_ON_GROUND)
            // !body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.IN_WATER)
        ) {
            GameLogger.debug(TAG, "hitByFeet(): apply negative Y impulse")
            val impulseY = when {
                body.isSensing(BodySense.IN_WATER) -> HIT_BY_FEET_IMPULSE_Y_IN_WATER
                else -> HIT_BY_FEET_IMPULSE_Y_ABOVE_WATER
            }
            body.physics.velocity.y = impulseY * ConstVals.PPM
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (body.isSensing(BodySense.IN_WATER)) {
            if (body.physics.velocity.y > FLOAT_UP_MAX_VEL_Y * ConstVals.PPM)
                body.physics.velocity.y = FLOAT_UP_MAX_VEL_Y * ConstVals.PPM
            else body.physics.velocity.y += FLOAT_UP_IMPULSE_Y * ConstVals.PPM * delta
        } else if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()
}
