package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.isAny
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.contracts.*
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.special.ToxicLiquidAnimator
import com.megaman.maverick.game.entities.special.ToxicLiquidClock
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

class ToxicWaterfall(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity {

    companion object {
        const val TAG = "ToxicWaterfall"
        private const val FORCE = 15f
        private const val ALPHA = 0.75f
        private const val SPLASH_FREQ = 0.2f
        private const val SPLASH_ALPHA = 0.25f
        private const val JOIN_REGION = "ToxicWaterfallJoin"
        private const val JOIN_DROP = 0.5f
        private const val PRIORITY = 10
        private var fallAnimator: ToxicLiquidAnimator? = null
        private var joinAnimator: ToxicLiquidAnimator? = null
    }

    private val bodiesInWater = OrderedMap<IBodyEntity, Timer>()
    private val fallSprites = Array<GameSprite>()
    private val joinSprites = Array<GameSprite>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (fallAnimator == null) {
            val atlas = TextureAsset.DECORATIONS_1.source
            val fall = services.assets.getTextureRegion(atlas, TAG)
            fallAnimator = ToxicLiquidAnimator(fall.splitAndFlatten(2, 2, Array()))
            val join = services.assets.getTextureRegion(atlas, JOIN_REGION)
            joinAnimator = ToxicLiquidAnimator(join.splitAndFlatten(2, 2, Array()))
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        ToxicLiquidClock.join(services)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        val join = spawnProps.getOrDefault(ConstKeys.JOIN, false, Boolean::class)
        setDrawables(bounds, join)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        bodiesInWater.clear()
        ToxicLiquidClock.leave(services)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        bodiesInWater.forEach { entry ->
            val timer = entry.value
            timer.update(delta)

            if (timer.isFinished()) {
                val entity = entry.key

                val overlap = GameObjectPools.fetch(Rectangle::class)
                Intersector.intersectRectangles(
                    body.getBounds().toGdxRectangle(),
                    entity.body.getBounds().toGdxRectangle(),
                    overlap
                )

                val splash = MegaEntityFactory.fetch(Splash::class)!!
                splash.spawn(
                    props(
                        ConstKeys.TYPE pairTo SplashType.TOXIC,
                        ConstKeys.POSITION pairTo overlap.getPositionPoint(Position.TOP_CENTER),
                        ConstKeys.PRIORITY pairTo DrawingPriority(DrawingSection.FOREGROUND, 15),
                        ConstKeys.ALPHA pairTo SPLASH_ALPHA,
                        ConstKeys.SOUND pairTo false
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
        consumerFixture.setReceiver(Receivers.FILTER) { fixture -> fixture.getType() == FixtureType.BODY }
        consumerFixture.setReceiver(Receivers.CONSUMER) { state, fixture ->
            val entity = fixture.getEntity() as IBodyEntity

            when (state) {
                ProcessState.BEGIN -> {
                    bodiesInWater.put(entity, Timer(SPLASH_FREQ))
                    if (entity.asPlayer != null || entity is AbstractEnemy)
                        requestToPlaySound(SoundAsset.SPLASH_SOUND, false)
                }

                ProcessState.END -> bodiesInWater.remove(entity)
                else -> {}
            }
        }
        body.addFixture(consumerFixture)

        val forceFixture = Fixture(body, FixtureType.FORCE)
        forceFixture.setReceiver(Receivers.VELOCITY_ALTERATION) { fixture, delta, _ ->
            val entity = fixture.getEntity()
            return@setReceiver when {
                !(entity.asPlayer != null || entity is AbstractEnemy) -> VelocityAlteration.addNone()
                else -> VelocityAlteration.add(0f, -FORCE * ConstVals.PPM * delta)
            }
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

    private fun setDrawables(bounds: GameRectangle, join: Boolean) {
        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / (2f * ConstVals.PPM)).toInt()
        check(rows >= 1 && columns >= 1) {
            "$TAG $mapObjectId: the map object is ${bounds.getWidth()} x ${bounds.getHeight()} px; it needs at least " +
                "${2 * ConstVals.PPM} x ${ConstVals.PPM} px"
        }
        val width = 2f * ConstVals.PPM
        val height = ConstVals.PPM.toFloat()

        while (fallSprites.size < rows * columns) addSprite(fallSprites, "fall", fallAnimator!!)
        while (joinSprites.size < columns) addSprite(joinSprites, "join", joinAnimator!!)

        for (i in 0 until fallSprites.size) {
            val x = i % columns
            val y = i / columns
            val sprite = fallSprites[i]
            sprite.hidden = i >= rows * columns || (join && y == 0)
            sprite.setBounds(bounds.getX() + x * width, bounds.getY() + y * height, width, height)
        }

        for (i in 0 until joinSprites.size) {
            val sprite = joinSprites[i]
            sprite.hidden = !join || i >= columns
            sprite.setBounds(
                bounds.getX() + i * width,
                bounds.getY() - JOIN_DROP * ConstVals.PPM,
                width,
                height + JOIN_DROP * ConstVals.PPM
            )
        }
    }

    private fun addSprite(list: Array<GameSprite>, prefix: String, animator: ToxicLiquidAnimator) {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, PRIORITY))
        sprite.setAlpha(ALPHA)
        val key = "${prefix}_${list.size}"
        sprites.put(key, sprite)
        list.add(sprite)
        getComponent(AnimationsComponent::class)!!.putAnimator(key, sprite, animator)
    }

    override fun getType() = EntityType.DECORATION
}
