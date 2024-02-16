package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.BodyComponent
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import com.megaman.maverick.game.world.setEntity

class Lift(game: MegamanMaverickGame) : Block(game), ISpriteEntity {

    enum class LiftState {
        LIFTING, FALLING, STOPPED
    }

    companion object {
        const val TAG = "Lift"
        private var region: TextureRegion? = null
        private const val LIFT_SPEED = 5f
        private const val FALL_SPEED = 2f
    }

    lateinit var currentState: LiftState
        private set
    lateinit var stopPoint: Vector2
        private set

    override fun init() {
        super<Block>.init()
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Lift")
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        currentState = LiftState.STOPPED
        stopPoint = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(stopPoint)
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()

        // head fixture
        val headFixture = Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f), FixtureType.HEAD)
        headFixture.offsetFromBodyCenter.y = 0.5f * ConstVals.PPM
        headFixture.setEntity(this)
        bodyComponent.body.addFixture(headFixture)
        headFixture.shape.color = Color.BLUE
        debugShapeSuppliers.add { headFixture.shape }

        return bodyComponent
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        val megaman = getMegamanMaverickGame().megaman
        val megamanOverlapping = !megaman.dead && megaman.body.fixtures.any {
            it.second.fixtureLabel.equalsAny(
                FixtureType.SIDE, FixtureType.FEET
            ) && it.second.shape.overlaps(body)
        }

        currentState = if (megamanOverlapping && !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) LiftState.LIFTING
        else if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) || body.getCenter().y > stopPoint.y) LiftState.FALLING
        else LiftState.STOPPED

        when (currentState) {
            LiftState.LIFTING -> body.physics.velocity.y = LIFT_SPEED * ConstVals.PPM
            LiftState.FALLING -> body.physics.velocity.y = -FALL_SPEED * ConstVals.PPM
            else -> {
                body.physics.velocity.y = 0f
                body.setCenter(stopPoint)
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.75f * ConstVals.PPM)
        sprite.setRegion(region!!)

        val spritesComponent = SpritesComponent(this, "lift" to sprite)
        spritesComponent.putUpdateFunction("lift") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

}