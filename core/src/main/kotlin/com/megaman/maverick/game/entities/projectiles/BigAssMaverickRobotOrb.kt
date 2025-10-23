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
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class BigAssMaverickRobotOrb(game: MegamanMaverickGame) : AbstractProjectile(game, size = Size.MEDIUM), IAnimatedEntity {

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

    internal var hit = false

    private val moveDelay = Timer()
    private val trajectory = Vector2()

    private var active = true

    private var canBeHit = true
    private val hitTimer = Timer(HIT_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
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

        val spawn = spawnProps.getOrDefault(ConstKeys.POSITION, Vector2.Zero, Vector2::class)
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        this.trajectory.set(trajectory)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse)

        val delay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        moveDelay.resetDuration(delay)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2.Zero, Vector2::class)
        body.physics.gravity.set(gravity)

        hit = false
        active = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)
        canBeHit = spawnProps.getOrDefault(ConstKeys.CAN_BE_HIT, true, Boolean::class)

        hitTimer.reset()

        val drawingSection =
            spawnProps.getOrDefault(ConstKeys.SECTION, DrawingSection.PLAYGROUND, DrawingSection::class)
        val drawingPriority = spawnProps.getOrDefault(ConstKeys.PRIORITY, 5, Int::class)
        sprites[TAG].priority.let {
            it.value = drawingPriority
            it.section = drawingSection
        }
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!canBeHit) {
            GameLogger.debug(TAG, "hitBlock(): canBeHit=false, do nothing")
            return
        }
        GameLogger.debug(TAG, "hitBlock(): blockFixture=$blockFixture, thisShape=$thisShape, otherShape=$otherShape")
        getHit()
    }

    override fun onBossDefeated(boss: AbstractBoss) {
        GameLogger.debug(TAG, "onBossDefeated(): boss=$boss")
        getHit()
    }

    private fun getHit() {
        GameLogger.debug(TAG, "onHit()")

        hit = true
        body.physics.velocity.setZero()

        val center = body.getCenter()
        body.setSize(HIT_SIZE * ConstVals.PPM)
        body.setCenter(center)

        requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!trajectory.isZero) {
            moveDelay.update(delta)
            if (moveDelay.isFinished() && !hit) body.physics.velocity.set(trajectory)
            if (moveDelay.isJustFinished()) requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
        }

        if (hit) {
            body.physics.gravity.setZero()
            body.physics.velocity.setZero()

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

            body.forEachFixture { it.setActive(active) }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5)))
        .preProcess { _, sprite ->
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
