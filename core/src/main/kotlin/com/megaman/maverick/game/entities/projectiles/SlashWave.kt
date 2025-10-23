package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
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
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.*
import kotlin.math.max

class SlashWave(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "SlashWave"

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 1.5f

        private const val CIRCLE_RADIUS = 0.5f

        private const val DEFAULT_DISSIPATE_DELAY = 0.25f
        private const val DEFAULT_DISSIPATE_DUR = 0.2f

        private var region: TextureRegion? = null
    }

    private var dissipate = false
    private val dissipateDelay = Timer()
    private val dissipateTimer = Timer()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(center)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        dissipate = spawnProps.getOrDefault(ConstKeys.DISSIPATE, false, Boolean::class)

        val dissipateDur = spawnProps.getOrDefault(
            "${ConstKeys.DISSIPATE}_${ConstKeys.DURATION}",
            DEFAULT_DISSIPATE_DUR,
            Float::class
        )
        dissipateTimer.resetDuration(dissipateDur)

        val dissipateDelay = spawnProps.getOrDefault(
            "${ConstKeys.DISSIPATE}_${ConstKeys.DELAY}",
            DEFAULT_DISSIPATE_DELAY,
            Float::class
        )
        this.dissipateDelay.resetDuration(dissipateDelay)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val circle = GameCircle().setRadius(CIRCLE_RADIUS * ConstVals.PPM)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            body = body,
            entity = this,
            debugShapes = debugShapes,
            bodyFixtureDefs = BodyFixtureDef.of(
                FixtureType.PROJECTILE pairTo circle.copy(),
                FixtureType.DAMAGER pairTo circle.copy()
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM, 1.5f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + 180f

            val alpha = if (dissipate) max(0f, 1f - dissipateTimer.getRatio()) else 1f
            sprite.setAlpha(alpha)
        }
        .build()

    fun getDissipation() = max(1, (1f - dissipateTimer.getRatio()).times(10).toInt())

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 1, 0.1f, true)))
        .build()

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (dissipate) {
            if (!dissipateDelay.isFinished()) dissipateDelay.update(delta)
            else {
                dissipateTimer.update(delta)
                if (dissipateTimer.isFinished()) destroy()
            }
        } else {
            dissipateDelay.reset()
            dissipateTimer.reset()
        }
    })
}
