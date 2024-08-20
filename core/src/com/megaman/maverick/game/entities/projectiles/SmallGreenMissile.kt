package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.getOverlapPushDirection
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.overlapsGameCamera
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SmallGreenMissile(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectionRotatable {

    companion object {
        const val TAG = "SmallGreenMissile"
        private const val GRAVITY = -0.15f
        private var region: TextureRegion? = null
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private var speed = 0f

    override fun init() {
        if (region == null) region =
            game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, "SmallGreenMissile")
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        speed = 0f
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie(blockFixture.getShape() as GameRectangle)

    override fun hitSand(sandFixture: IFixture) = explodeAndDie(sandFixture.getShape() as GameRectangle)

    override fun explodeAndDie(vararg params: Any?) {
        kill()

        val hitBounds = params[0] as GameRectangle
        val overlap = GameRectangle()
        val direction = getOverlapPushDirection(body, hitBounds, overlap)
        val position = overlap.getPositionPoint(
            when (direction) {
                Direction.UP, null -> Position.TOP_CENTER
                Direction.DOWN -> Position.BOTTOM_CENTER
                Direction.LEFT -> Position.CENTER_LEFT
                Direction.RIGHT -> Position.CENTER_RIGHT
            }
        )
        val greenExplosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.GREEN_EXPLOSION)!!
        game.engine.spawn(
            greenExplosion, props(
                ConstKeys.POSITION to position,
                ConstKeys.DIRECTION to direction,
                ConstKeys.OWNER to owner
            )
        )

        if (overlapsGameCamera()) playSoundNow(SoundAsset.BLAST_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle(body))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity = when (directionRotation!!) {
                Direction.UP -> Vector2(0f, GRAVITY)
                Direction.DOWN -> Vector2(0f, -GRAVITY)
                Direction.LEFT -> Vector2(GRAVITY, 0f)
                Direction.RIGHT -> Vector2(-GRAVITY, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.35f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}