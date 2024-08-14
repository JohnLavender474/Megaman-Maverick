package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.getOverlapPushDirection
import com.engine.common.getSingleMostDirectionFromStartToTarget
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SpitFireball(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "SpitFireball"
        private const val FIREBALLS_TO_SPAWN = 2
        private const val FIREBALL_IMPULSE = 3f
        private const val FIREBALL_GRAVITY = -0.15f
        private const val FIREBALL_CULL_TIME = 0.15f
        private val angles = ObjectMap<Direction, Array<Float>>()
        private var region: TextureRegion? = null
    }

    private var spawnFireballsOnHit = true

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        if (angles.isEmpty) {
            angles.put(Direction.UP, gdxArrayOf(45f, 315f))
            angles.put(Direction.LEFT, gdxArrayOf(45f, 135f))
            angles.put(Direction.DOWN, gdxArrayOf(135f, 225f))
            angles.put(Direction.RIGHT, gdxArrayOf(225f, 315f))
        }
        super<AbstractProjectile>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)
        spawnFireballsOnHit = spawnProps.getOrDefault(ConstKeys.SPAWN, true, Boolean::class)
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie(blockFixture.getShape().getBoundingRectangle())

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable is IBodyEntity) explodeAndDie(damageable.body.copy())
    }

    override fun explodeAndDie(vararg params: Any?) {
        kill()

        val bounds = params[0] as GameRectangle
        val direction = getOverlapPushDirection(body, bounds) ?: getSingleMostDirectionFromStartToTarget(
            body.getCenter(),
            bounds.getCenter()
        )

        for (i in 0 until FIREBALLS_TO_SPAWN) {
            val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!!
            val angle = angles[direction]!![i]
            val trajectory = Vector2(0f, FIREBALL_IMPULSE * ConstVals.PPM).rotateDeg(angle)
            val position = when (direction) {
                Direction.UP -> body.getTopCenterPoint()
                Direction.DOWN -> body.getBottomCenterPoint()
                Direction.LEFT -> body.getCenterLeftPoint()
                Direction.RIGHT -> body.getCenterRightPoint()
            }
            game.engine.spawn(
                fireball, props(
                    ConstKeys.OWNER to owner,
                    ConstKeys.POSITION to position,
                    ConstKeys.TRAJECTORY to trajectory,
                    ConstKeys.GRAVITY to Vector2(0f, FIREBALL_GRAVITY * ConstVals.PPM),
                    ConstKeys.CULL_TIME to FIREBALL_CULL_TIME,
                )
            )
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.15f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle(body))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setOriginCenter()
            _sprite.rotation = body.physics.velocity.angleDeg()
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}