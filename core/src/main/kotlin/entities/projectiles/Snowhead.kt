package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.set
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.entities.enemies.SnowheadThrower
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class Snowhead(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

    companion object {
        const val TAG = "Snowhead"
        private const val GRAVITY = -0.2f
        private val SNOWBALL_TRAJECTORIES = gdxArrayOf(
            Vector2(-6f, 6f), Vector2(-3f, 8f), Vector2(3f, 8f), Vector2(6f, 6f),
        )
        private var region: TextureRegion? = null
        private var noFaceRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    override fun init() {
        if (region == null || noFaceRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            region = atlas.findRegion("${SnowheadThrower.TAG}/$TAG")
            noFaceRegion = atlas.findRegion("${SnowheadThrower.TAG}/NoFace")
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val size = spawnProps.getOrDefault(
            ConstKeys.SIZE,
            GameObjectPools.fetch(Vector2::class).set(ConstVals.PPM.toFloat()),
            Vector2::class
        )
        body.setSize(size.x, size.y)
        defaultSprite.setSize(size.cpy().scl(2f))

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        facing = if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        val noFace = spawnProps.getOrDefault("${ConstKeys.NO}_${ConstKeys.FACE}", false, Boolean::class)
        defaultSprite.setRegion(if (noFace) noFaceRegion!! else region!!)
        defaultSprite.priority.section = spawnProps.getOrDefault(
            ConstKeys.SECTION,
            DrawingSection.PLAYGROUND,
            DrawingSection::class
        )
        defaultSprite.priority.value = spawnProps.getOrDefault(ConstKeys.PRIORITY, 0, Int::class)
    }

    private fun bounceBullets(collisionShape: IGameShape2D) {
        val snowballs =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL, SNOWBALL_TRAJECTORIES.size)
        val direction = getOverlapPushDirection(body.getBounds(), collisionShape) ?: Direction.UP
        val spawn = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER).add(0f, 0.1f * ConstVals.PPM)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER).sub(0f, 0.1f * ConstVals.PPM)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT).sub(0.1f * ConstVals.PPM, 0f)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT).add(0.1f * ConstVals.PPM, 0f)
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

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        bounceBullets(blockFixture.getShape())
        explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        bounceBullets(waterFixture.getShape())
        explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (shieldFixture.getEntity() == owner) return
        bounceBullets(shieldFixture.getShape())
        explodeAndDie()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity() as AbstractProjectile
        if (projectile.owner == owner) return
        if (projectile.owner is Megaman) explodeAndDie()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture -> ((fixture as Fixture).rawShape as GameRectangle).set(body) }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
