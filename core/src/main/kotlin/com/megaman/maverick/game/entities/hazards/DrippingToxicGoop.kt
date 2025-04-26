package com.megaman.maverick.game.entities.hazards

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
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.math.floor

class DrippingToxicGoop(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IDamager, IHazard {

    companion object {
        const val TAG = "DrippingToxicGoop"
        private const val SMOKE_DELAY = 0.5f
        private const val DAMAGE_TO_MEGAMAN = 3
        private var region: TextureRegion? = null
    }

    private val smokinBodies = OrderedMap<IBodyEntity, Timer>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        defineDrawables(bounds)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        smokinBodies.clear()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        val bodyEntity = damageable as IBodyEntity

        if (!smokinBodies.containsKey(bodyEntity)) {
            spawnSmokePuffFrom(bodyEntity.body.getBounds())
            smokinBodies.put(bodyEntity, Timer(SMOKE_DELAY))
        }
    }

    override fun getDamageToMegaman() = DAMAGE_TO_MEGAMAN

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val iter = smokinBodies.iterator()
        while (iter.hasNext) {
            val entry = iter.next()

            val entity = entry.key
            if (!entity.body.getBounds().overlaps(body.getBounds()) || (entity as MegaGameEntity).dead) {
                iter.remove()
                continue
            }

            val timer = entry.value
            timer.update(delta)
            if (timer.isFinished()) {
                spawnSmokePuffFrom(entity.body.getBounds())
                timer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val consumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        consumerFixture.setFilter { fixture ->
            fixture.getType() == FixtureType.BODY && fixture.getEntity() is IDamageable
        }
        consumerFixture.setConsumer { processState, fixture ->
            when (processState) {
                ProcessState.BEGIN, ProcessState.CONTINUE -> {
                    val bodyEntity = fixture.getEntity() as IBodyEntity

                    if (!smokinBodies.containsKey(bodyEntity)) {
                        smokinBodies.put(bodyEntity, Timer(SMOKE_DELAY))

                        spawnSmokePuffFrom(bodyEntity.body.getBounds())
                    }
                }

                else -> {}
            }
        }
        body.addFixture(consumerFixture)

        body.preProcess.put(ConstKeys.FIXTURES) {
            body.forEachFixture { fixture ->
                fixture as Fixture
                (fixture.rawShape as GameRectangle).set(body)
            }
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds))
    }

    private fun spawnSmokePuffFrom(bounds: GameRectangle) {
        val overlap = GameObjectPools.fetch(Rectangle::class)

        if (Intersector.intersectRectangles(bounds.toGdxRectangle(), body.getBounds().toGdxRectangle(), overlap)) {
            val position = overlap.getPositionPoint(Position.BOTTOM_CENTER)

            val smokePuff = MegaEntityFactory.fetch(SmokePuff::class)!!
            smokePuff.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.DIRECTION pairTo Direction.UP
                )
            )

            requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)
        }
    }

    private fun defineDrawables(bounds: GameRectangle) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val rows = floor(bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = floor(bounds.getWidth() / ConstVals.PPM).toInt()

        for (x in 0 until columns) for (y in 0 until rows) {
            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
            sprite.setBounds(
                bounds.getX() + x * ConstVals.PPM,
                bounds.getY() + y * ConstVals.PPM,
                ConstVals.PPM.toFloat(),
                ConstVals.PPM.toFloat()
            )
            sprites.put("${x}_${y}", sprite)

            val animation = Animation(region!!, 2, 2, 0.1f, true)
            val animator = Animator(animation)
            animators.add({ sprite } pairTo animator)
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators))
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
