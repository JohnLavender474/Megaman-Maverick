package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.getOverlapPushDirection
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.IGameShape2D
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera


import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class Snowhead(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

    companion object {
        const val TAG = "Snowhead"
        private const val GRAVITY = -0.15f
        private val BULLET_TRAJECTORIES = gdxArrayOf(
            Vector2(-7f, 5f),
            Vector2(-3f, 7f),
            Vector2(3f, 7f),
            Vector2(7f, 5f),
        )
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(
            TextureAsset.ENEMIES_2.source, "SnowheadThrower/Snowhead"
        )
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity = trajectory
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        facing = if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
    }

    private fun bounceBullets(collisionShape: IGameShape2D) {
        val bullets = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET, BULLET_TRAJECTORIES.size)
        val direction = getOverlapPushDirection(body, collisionShape) ?: Direction.UP
        val spawn = when (direction) {
            Direction.UP -> body.getTopCenterPoint().add(0f, 0.1f * ConstVals.PPM)
            Direction.DOWN -> body.getBottomCenterPoint().sub(0f, 0.1f * ConstVals.PPM)
            Direction.LEFT -> body.getCenterLeftPoint().sub(0.1f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getCenterRightPoint().add(0.1f * ConstVals.PPM, 0f)
        }
        for (i in 0 until bullets.size) {
            val trajectory = BULLET_TRAJECTORIES[i].cpy()
            trajectory.rotateDeg(direction.rotation)
            game.engine.spawn(
                bullets[i], props(
                    ConstKeys.POSITION to spawn,
                    ConstKeys.TRAJECTORY to trajectory.scl(ConstVals.PPM.toFloat()),
                    ConstKeys.GRAVITY to Vector2(0f, GRAVITY * ConstVals.PPM),
                    ConstKeys.OWNER to owner
                )
            )
        }
    }

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        if (overlapsGameCamera()) playSoundNow(SoundAsset.CHILL_SHOOT_SOUND, false)
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        game.engine.spawn(explosion, props(ConstKeys.POSITION to body.getCenter()))
    }

    override fun hitBlock(blockFixture: IFixture) {
        bounceBullets(blockFixture.getShape())
        explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        if (shieldFixture.getEntity() == owner) return
        bounceBullets(shieldFixture.getShape())
        explodeAndDie()
    }

    override fun hitProjectile(projectileFixture: IFixture) {
        val projectile = projectileFixture.getEntity() as AbstractProjectile
        if (projectile.owner == owner) return
        if (projectile.owner is Megaman) explodeAndDie()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle().set(body))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setRegion(region!!)
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}