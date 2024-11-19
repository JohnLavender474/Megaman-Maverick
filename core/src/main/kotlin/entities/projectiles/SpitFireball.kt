package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.getSingleMostDirectionFromStartToTarget
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

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
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)
        spawnFireballsOnHit = spawnProps.getOrDefault(ConstKeys.SPAWN, true, Boolean::class)
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (spawnFireballsOnHit) explodeAndDie(thisShape, otherShape)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val thisShape = params[0] as IGameShape2D
        val otherShape = params[1] as IGameShape2D
        val direction = getOverlapPushDirection(thisShape, otherShape) ?: getSingleMostDirectionFromStartToTarget(
            thisShape.getCenter(),
            otherShape.getCenter()
        )

        for (i in 0 until FIREBALLS_TO_SPAWN) {
            val fireball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIREBALL)!!
            val angle = angles[direction]!![i]
            val trajectory = Vector2(0f, FIREBALL_IMPULSE * ConstVals.PPM).rotateDeg(angle)
            fireball.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    ConstKeys.GRAVITY pairTo Vector2(0f, FIREBALL_GRAVITY * ConstVals.PPM),
                    ConstKeys.CULL_TIME pairTo FIREBALL_CULL_TIME,
                )
            )
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
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
