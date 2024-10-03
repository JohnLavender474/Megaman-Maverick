package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity

class Snowhead(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

    companion object {
        const val TAG = "Snowhead"
        private const val GRAVITY = -0.15f
        private val SNOWBALL_TRAJECTORIES = gdxArrayOf(
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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity = trajectory
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        facing = if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
    }

    private fun bounceBullets(collisionShape: IGameShape2D) {
        val snowballs = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL, SNOWBALL_TRAJECTORIES.size)
        val direction = getOverlapPushDirection(body, collisionShape) ?: Direction.UP
        val spawn = when (direction) {
            Direction.UP -> body.getTopCenterPoint().add(0f, 0.1f * ConstVals.PPM)
            Direction.DOWN -> body.getBottomCenterPoint().sub(0f, 0.1f * ConstVals.PPM)
            Direction.LEFT -> body.getCenterLeftPoint().sub(0.1f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getCenterRightPoint().add(0.1f * ConstVals.PPM, 0f)
        }
        for (i in 0 until snowballs.size) {
            val trajectory = SNOWBALL_TRAJECTORIES[i].cpy()
            trajectory.rotateDeg(direction.rotation)
            snowballs[i].spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TRAJECTORY pairTo trajectory.scl(ConstVals.PPM.toFloat()),
                    ConstKeys.GRAVITY pairTo Vector2(0f, GRAVITY * ConstVals.PPM),
                    ConstKeys.GRAVITY_ON pairTo true,
                    ConstKeys.OWNER pairTo owner
                )
            )
        }
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        if (overlapsGameCamera()) playSoundNow(SoundAsset.CHILL_SHOOT_SOUND, false)
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
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
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.takeFrictionFromOthers = false

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
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
