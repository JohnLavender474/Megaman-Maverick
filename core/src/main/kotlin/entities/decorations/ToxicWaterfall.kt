package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

class ToxicWaterfall(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity {

    companion object {
        const val TAG = "ToxicWaterfall"
        private const val FORCE = 15f
        private const val ALPHA = 0.75f
        private const val SPLASH_FREQ = 0.2f
        private const val SPLASH_ALPHA = 0.25f
        private var region: TextureRegion? = null
    }

    private val bodiesInWater = OrderedMap<IBodyEntity, Timer>()

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        defineDrawables(bounds)
    }

    override fun onDestroy() {
        super.onDestroy()
        bodiesInWater.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        bodiesInWater.forEach { entry ->
            val timer = entry.value
            timer.update(delta)

            if (timer.isFinished()) {
                val entity = entry.key

                val overlap = Rectangle()
                Intersector.intersectRectangles(
                    body.getBounds().toGdxRectangle(),
                    entity.body.getBounds().toGdxRectangle(),
                    overlap
                )

                val toxicSplash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!
                toxicSplash.spawn(
                    props(
                        ConstKeys.TYPE pairTo SplashType.TOXIC,
                        ConstKeys.POSITION pairTo overlap.getPositionPoint(Position.TOP_CENTER),
                        ConstKeys.PRIORITY pairTo DrawingPriority(DrawingSection.FOREGROUND, 15),
                        ConstKeys.ALPHA pairTo SPLASH_ALPHA
                    )
                )

                timer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val consumerFixture = Fixture(body, FixtureType.CONSUMER)
        consumerFixture.setConsumer { state, fixture ->
            if (fixture.getType() != FixtureType.BODY) return@setConsumer
            val entity = fixture.getEntity() as IBodyEntity

            if (state == ProcessState.BEGIN) {
                bodiesInWater.put(entity, Timer(SPLASH_FREQ))
                if (entity.isAny(Megaman::class, AbstractEnemy::class))
                    requestToPlaySound(SoundAsset.SPLASH_SOUND, false)
            } else if (state == ProcessState.END) bodiesInWater.remove(entity)
        }
        body.addFixture(consumerFixture)

        val forceFixture = Fixture(body, FixtureType.FORCE)
        forceFixture.setVelocityAlteration { fixture, delta ->
            val entity = fixture.getEntity()
            return@setVelocityAlteration if (!entity.isAny(
                    Megaman::class,
                    AbstractEnemy::class
                )
            ) VelocityAlteration.addNone()
            else VelocityAlteration.add(0f, -FORCE * ConstVals.PPM * delta)
        }
        body.addFixture(forceFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            (consumerFixture.rawShape as GameRectangle).set(body)
            (forceFixture.rawShape as GameRectangle).set(body)
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / (2f * ConstVals.PPM)).toInt()

        for (x in 0 until columns) {
            for (y in 0 until rows) {
                val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
                sprite.setBounds(
                    bounds.getX() + 2f * x * ConstVals.PPM,
                    bounds.getY() + y * ConstVals.PPM,
                    2f * ConstVals.PPM,
                    ConstVals.PPM.toFloat()
                )
                sprite.setAlpha(ALPHA)
                sprites.put("${x}_${y}", sprite)

                val animation = Animation(region!!, 2, 2, 0.1f, true)
                val animator = Animator(animation)
                animators.add({ sprite } pairTo animator)
            }
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    override fun getEntityType() = EntityType.DECORATION
}
