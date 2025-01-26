package com.megaman.maverick.game.entities

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.*

class PreciousGem(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "PreciousGem"
        private const val CULL_TIME = 5f
        private const val PAUSE_BEFORE_TARGET_MEGAMAN_DUR = 0.5f
        private const val SIZE_INCREASE_DELAY_DUR = 0.1f
        private val BODY_SIZES = gdxArrayOf(0.25f, 0.5f, 0.75f, 1f)
        private val regions = ObjectMap<PreciousGemColor, TextureRegion>()
    }

    enum class PreciousGemColor { PURPLE, BLUE, PINK }

    private var stateIndex = 0

    private val sizeIncreaseDelay = Timer(SIZE_INCREASE_DELAY_DUR)
    private var sizeIncreaseIndex = 0

    private val pauseBeforeTargetMegamanTimer = Timer(PAUSE_BEFORE_TARGET_MEGAMAN_DUR)
    internal var targetMegamanAfterPause = true
    internal var targetReached = false
        private set

    private val target = Vector2()
    private var speed = 0f

    private lateinit var color: PreciousGemColor

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            PreciousGemColor.entries.forEach { color ->
                regions.put(
                    color,
                    atlas.findRegion("$TAG/${color.name.lowercase()}")
                )
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        target.set(spawnProps.get(ConstKeys.TARGET, Vector2::class)!!)

        stateIndex = 0

        sizeIncreaseDelay.reset()
        sizeIncreaseIndex = 0
        setSizeByIndex(0)

        pauseBeforeTargetMegamanTimer.reset()
        targetMegamanAfterPause = spawnProps.get("${ConstKeys.TARGET}_${Megaman.TAG}", Boolean::class)!!
        targetReached = false

        speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!

        color = spawnProps.get(ConstKeys.COLOR, PreciousGemColor::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()
    }

    private fun setSizeByIndex(index: Int) {
        val center = body.getCenter()

        val size = BODY_SIZES[index]
        body.setSize(size * ConstVals.PPM).setCenter(center)

        body.forEachFixture { fixture ->
            val bounds = (fixture as Fixture).rawShape as GameRectangle
            bounds.set(body)
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (stateIndex) {
            0 -> {
                val trajectory = target.cpy().sub(body.getCenter()).nor().scl(speed)
                body.physics.velocity.set(trajectory)

                if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                    targetReached = true

                    body.physics.velocity.setZero()
                    body.setCenter(target)

                    stateIndex++
                    GameLogger.debug(TAG, "update(): target reached, stateIndex=$stateIndex")
                }
            }

            1 -> {
                if (sizeIncreaseIndex < BODY_SIZES.size - 1) {
                    sizeIncreaseDelay.update(delta)

                    if (sizeIncreaseDelay.isFinished()) {
                        sizeIncreaseIndex++
                        setSizeByIndex(sizeIncreaseIndex)

                        sizeIncreaseDelay.reset()
                    }
                }

                if (targetMegamanAfterPause) {
                    pauseBeforeTargetMegamanTimer.update(delta)

                    if (pauseBeforeTargetMegamanTimer.isFinished()) {
                        stateIndex++

                        targetReached = false

                        val trajectory = megaman.body.getCenter().cpy().sub(body.getCenter()).nor().scl(speed)
                        body.physics.velocity.set(trajectory)

                        GameLogger.debug(TAG, "update(): stateIndex=$stateIndex, trajectory=$trajectory")
                    }
                }
            }
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { color.name.lowercase() }
                .applyToAnimations { animations ->
                    PreciousGemColor.entries.forEach { color ->
                        val animation = Animation(regions[color], 2, 2, 0.1f, false)
                        animations.put(color.name.lowercase(), animation)
                    }
                }
                .build()
        )
        .build()
}
