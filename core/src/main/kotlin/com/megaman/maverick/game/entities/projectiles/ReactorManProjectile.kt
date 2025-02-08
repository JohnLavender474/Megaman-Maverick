package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.*

class ReactorManProjectile(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ReactorManProjectile"

        private const val GRAVITY = -0.15f
        private const val DIE_DUR = 0.05f

        private val shatterTrajectories = objectMapOf(
            Direction.UP pairTo gdxArrayOf(
                Vector2(5f, 9f),
                Vector2(0f, 9f),
                Vector2(-5f, 9f)
            ),
            Direction.DOWN pairTo gdxArrayOf(
                Vector2(5f, -9f),
                Vector2(0f, -9f),
                Vector2(-5f, -9f)
            ),
            Direction.LEFT pairTo gdxArrayOf(
                Vector2(-9f, 5f),
                Vector2(-9f, 0f),
                Vector2(-9f, -5f)
            ),
            Direction.RIGHT pairTo gdxArrayOf(
                Vector2(9f, 5f),
                Vector2(9f, 0f),
                Vector2(9f, -5f)
            )
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    var active = false

    private val dyingTimer = Timer(DIE_DUR)
    private var dying = false
    private var big = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            gdxArrayOf("big", "small", "die").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        big = spawnProps.get(ConstKeys.BIG, Boolean::class)!!

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
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

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!active) return
        if (big) shatter(blockFixture.getShape())
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        if (big) destroy() else {
            body.physics.velocity.setZero()
            body.physics.gravityOn = false
            dying = true
        }
    }

    private fun shatter(shape: IGameShape2D) {
        playSoundNow(SoundAsset.BURST_SOUND, false)

        val direction = getOverlapPushDirection(body.getBounds(), shape) ?: Direction.UP

        shatterTrajectories.get(direction).forEach { trajectory ->
            val projectile = MegaEntityFactory.fetch(ReactorManProjectile::class)!!
            projectile.spawn(
                props(
                    ConstKeys.POSITION pairTo when (direction) {
                        Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
                        Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
                        Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                        Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
                    },
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.BIG pairTo false,
                    ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat()),
                    ConstKeys.GRAVITY_ON pairTo true,
                    ConstKeys.ACTIVE pairTo true
                )
            )
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (dying) {
            dyingTimer.update(delta)
            if (dyingTimer.isFinished()) destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.gravity.set(0f, GRAVITY * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = if (big) 0.65f else 0.35f
            body.setSize(size * ConstVals.PPM)
            body.forEachFixture {
                val shape = (it as Fixture).rawShape as GameRectangle
                shape.setSize(size * ConstVals.PPM)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when {
                big -> "big"
                !dying -> "small"
                else -> "dying"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "big" pairTo Animation(regions["big"], 1, 3, 0.1f, true),
            "small" pairTo Animation(regions["small"], 2, 2, 0.1f, true),
            "dying" pairTo Animation(regions["dying"], 1, 2, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
