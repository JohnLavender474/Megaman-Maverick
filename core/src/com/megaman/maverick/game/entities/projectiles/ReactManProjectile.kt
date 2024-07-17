package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getOverlapPushDirection
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.IGameShape2D
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ReactManProjectile(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ReactManProjectile"
        private const val GRAVITY = -0.15f
        private const val DIE_DUR = 0.1f
        private val shatterTrajectories = objectMapOf(
            Direction.UP to gdxArrayOf(
                Vector2(3f, 7f),
                Vector2(0f, 7f),
                Vector2(-3f, 7f)
            ),
            Direction.DOWN to gdxArrayOf(
                Vector2(3f, -7f),
                Vector2(0f, -7f),
                Vector2(-3f, -7f)
            ),
            Direction.LEFT to gdxArrayOf(
                Vector2(-7f, 3f),
                Vector2(-7f, 0f),
                Vector2(-7f, -3f)
            ),
            Direction.RIGHT to gdxArrayOf(
                Vector2(7f, 3f),
                Vector2(7f, 0f),
                Vector2(7f, -3f)
            )
        )
        private var bigRegion: TextureRegion? = null
        private var smallRegion: TextureRegion? = null
        private var dyingRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    var active = false

    private val dyingTimer = Timer(DIE_DUR)
    private var dying = false
    private var big = false

    override fun init() {
        if (bigRegion == null || smallRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            bigRegion = atlas.findRegion("ReactManProjectile/Big")
            smallRegion = atlas.findRegion("ReactManProjectile/Small")
            dyingRegion = atlas.findRegion("ReactManProjectile/Die")
        }
        addComponents(defineProjectileComponents())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        big = spawnProps.get(ConstKeys.BIG, Boolean::class)!!

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity.set(trajectory)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        active = spawnProps.getOrDefault(ConstKeys.ACTIVE, false, Boolean::class)

        dyingTimer.reset()
        dying = false
    }

    fun setTrajectory(trajectory: Vector2) {
        body.physics.velocity.set(trajectory)
    }

    override fun hitBlock(blockFixture: IFixture) {
        if (!active) return
        if (big) shatter(blockFixture.getShape())
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) = if (big) kill() else {
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
        dying = true
    }

    private fun shatter(shape: IGameShape2D) {
        getMegamanMaverickGame().audioMan.playSound(SoundAsset.BURST_SOUND, false)
        val direction = getOverlapPushDirection(body, shape) ?: Direction.UP
        shatterTrajectories.get(direction).forEach { trajectory ->
            val projectile = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.REACT_MAN_PROJECTILE)!!
            game.engine.spawn(
                projectile, props(
                    ConstKeys.POSITION to when (direction) {
                        Direction.UP -> body.getTopCenterPoint()
                        Direction.DOWN -> body.getBottomCenterPoint()
                        Direction.LEFT -> body.getCenterLeftPoint()
                        Direction.RIGHT -> body.getCenterRightPoint()
                    },
                    ConstKeys.OWNER to owner,
                    ConstKeys.BIG to false,
                    ConstKeys.TRAJECTORY to trajectory.cpy().scl(ConstVals.PPM.toFloat()),
                    ConstKeys.GRAVITY_ON to true,
                    ConstKeys.ACTIVE to true
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (dying) {
            dyingTimer.update(delta)
            if (dyingTimer.isFinished()) kill()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.gravity.set(0f, GRAVITY * ConstVals.PPM)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = if (big) 0.65f else 0.35f
            body.setSize(size * ConstVals.PPM)
            body.fixtures.forEach {
                val shape = (it.second as Fixture).rawShape as GameRectangle
                shape.setSize(size * ConstVals.PPM)
            }
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2))
        sprite.setSize(0.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (big) "big" else if (!dying) "small" else "dying" }
        val animations = objectMapOf<String, IAnimation>(
            "big" to Animation(bigRegion!!, 1, 3, 0.1f, true),
            "small" to Animation(smallRegion!!, 2, 2, 0.1f, true),
            "dying" to Animation(dyingRegion!!, 1, 2, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}