package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class BigAssMaverickRobotOrb(game: MegamanMaverickGame) : AbstractProjectile(game, size = Size.SMALL), IAnimatedEntity {

    companion object {
        const val TAG = "BigAssMaverickRobotOrb"
        private const val BALL_SIZE = 1f
        private const val HIT_SIZE = 2f
        private const val HIT_DUR = 0.2f
        private val animDefs = orderedMapOf(
            "hit" pairTo AnimationDef(2, 2, 0.05f, false),
            "ball" pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val moveDelay = Timer()
    private val trajectory = Vector2()

    private val hitTimer = Timer(HIT_DUR)
    private var hit = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        body.setSize(BALL_SIZE * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)

        val delay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        moveDelay.resetDuration(delay)

        hit = false
        hitTimer.reset()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        hit = true

        body.physics.velocity.setZero()

        val center = body.getCenter()
        body.setSize(HIT_SIZE * ConstVals.PPM)
        body.setCenter(center)

        requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        moveDelay.update(delta)
        if (moveDelay.isFinished() && !hit) body.physics.velocity.set(trajectory)
        if (moveDelay.isJustFinished()) requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)

        if (hit) {
            hitTimer.update(delta)
            if (hitTimer.isFinished()) destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle())
        body.addFixture(projectileFixture)
        projectileFixture.drawingColor = Color.RED
        debugShapes.add { projectileFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val projCircle = projectileFixture.rawShape as GameCircle
            projCircle.setRadius(body.getWidth() / 2f)

            val damagerCircle = damagerFixture.rawShape as GameCircle
            damagerCircle.setRadius(body.getWidth() / 2f)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5)))
        .updatable { _, sprite ->
            val size = if (hit) 4f else 2f
            sprite.setSize(size * ConstVals.PPM)
            sprite.setCenter(body.getCenter())
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (hit) "hit" else "ball" }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()
}
