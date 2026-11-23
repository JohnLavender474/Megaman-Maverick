package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class TimberWalkerChipWave(services: MegaServices) : AbstractProjectile(services) {

    companion object {
        const val TAG = "TimberWalkerChipWave"
        private const val BODY_WIDTH = 1.25f
        private const val BODY_HEIGHT = 1f
        private const val WALL_SENSOR_SIZE = 0.5f
        private const val SPRITE_SIZE = 2f
        private const val FRAME_DUR = 0.08f
        private val regions = Array<TextureRegion>()
    }

    private lateinit var wave: Animation

    override fun init(vararg params: Any) {
        if (regions.isEmpty) for (i in 1..3) regions.add(
            services.assets.getTextureRegion(TextureAsset.TIMBER_WALKER.source, "$TAG/wave_$i")
        )
        super.init()
        wave = Animation(regions, FRAME_DUR, loop = true)
        addComponent(UpdatablesComponent({ delta -> wave.update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        wave.reset()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        destroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val wallSensor =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(WALL_SENSOR_SIZE * ConstVals.PPM))
        wallSensor.offsetFromBodyAttachment.y = (BODY_HEIGHT - WALL_SENSOR_SIZE) * ConstVals.PPM / 2f
        body.addFixture(wallSensor)
        debugShapes.add { wallSensor }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setRegion(wave.getCurrentRegion())
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(body.physics.velocity.x > 0f, false)
        }
        return component
    }
}
