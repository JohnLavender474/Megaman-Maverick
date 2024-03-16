package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Picket(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

    companion object {
        const val TAG = "Picket"
        private var region: TextureRegion? = null
        private const val GRAVITY = -0.15f
    }

    override var owner: IGameEntity? = null

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Picket")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val impulseX = spawnProps.get(ConstKeys.X, Float::class)!!
        val impulseY = spawnProps.get(ConstKeys.Y, Float::class)!!
        body.physics.velocity.set(impulseX, impulseY)
    }

    override fun hitBlock(blockFixture: Fixture) {
        // TODO: certain blocks breakable with picket
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM), FixtureType.BODY)
        body.addFixture(bodyFixture)
        bodyFixture.shape.color = Color.YELLOW
        debugShapes.add { bodyFixture.shape }

        val projectileFixture =
            Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM), FixtureType.PROJECTILE)
        body.addFixture(projectileFixture)
        projectileFixture.shape.color = Color.GREEN
        debugShapes.add { projectileFixture.shape }

        val damagerFixture = Fixture(GameRectangle().setSize(0.4f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        val shieldFixture = Fixture(GameRectangle().setSize(0.5f * ConstVals.PPM), FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        debugShapes.add { shieldFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, "picket" to sprite)
        spritesComponent.putUpdateFunction("picket") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.1f)
        return AnimationsComponent(this, Animator(animation))
    }
}
