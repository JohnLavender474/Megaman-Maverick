package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.explosions.ElecExplosion
import com.megaman.maverick.game.utils.extensions.pooledCopy
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.*

class ElecDevilBodyPiece(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, ILightSource {

    companion object {
        const val TAG = "ElecDevilBodyPiece"

        private const val BEGIN_DUR = 0.5f

        private const val LIGHT_SOURCE_RADIUS = 3
        private const val LIGHT_SOURCE_RADIANCE = 1.25f

        // if the life of a body piece exceeds this max TTL, then clearly something wrong has happened, i.e. the body
        // piece has not been culled properly
        private const val MAX_TTL = 5f

        private var region: TextureRegion? = null
    }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_SOURCE_RADIUS
    override var lightSourceRadiance = LIGHT_SOURCE_RADIANCE

    private val lightSourceSendEventDelay = Timer(ElecDevilConstants.LIGHT_SOURCE_SEND_EVENT_DELAY)

    private lateinit var processState: ProcessState

    private val start = Vector2()
    private val target = Vector2()

    private val beginTimer = Timer(BEGIN_DUR)
    private val ttlTimer = Timer(MAX_TTL)

    private lateinit var onEnd: () -> Unit

    private var speed = 0f

    override fun init() {
        GameLogger.debug(TAG, "init(): hashcode=${hashCode()}")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): hashcode=${hashCode()}, spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val start = spawnProps.get(ConstKeys.START, Vector2::class)!!
        body.setCenter(start)
        this.start.set(start)

        val target = spawnProps.get(ConstKeys.TARGET, Vector2::class)!!
        this.target.set(target)

        onEnd = spawnProps.get(ConstKeys.END) as () -> Unit
        processState = ProcessState.BEGIN

        beginTimer.reset()
        ttlTimer.reset()

        speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!

        lightSourceSendEventDelay.reset()
        lightSourceKeys.addAll(
            spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}") as ObjectSet<Int>
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): hashcode=${hashCode()}")
        super.onDestroy()
        lightSourceKeys.clear()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        body.setCenter(target)
        spawnExplosion()
        onEnd.invoke()
        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        ttlTimer.update(delta)
        // Ideally, this should never happened. However, one time while playing against the elec devil, I ran into an
        // instance where a body piece floated at its target destination indefinitely, causing the boss fight to be
        // soft locked. While I could not figure out the source of the bug, I've implemented this TTL timer to avoid
        // any indefinite soft lock caused by this issue.
        if (ttlTimer.isFinished()) {
            explodeAndDie()
            return@UpdatablesComponent
        }

        lightSourceSendEventDelay.update(delta)
        if (lightSourceSendEventDelay.isFinished()) {
            LightSourceUtils.sendLightSourceEvent(game, this)
            lightSourceSendEventDelay.reset()
        }

        beginTimer.update(delta)
        if (!beginTimer.isFinished()) return@UpdatablesComponent
        if (beginTimer.isJustFinished()) GameLogger.debug(TAG, "update(): beginTimer just finished")

        processState = ProcessState.CONTINUE

        body.physics.velocity.set(target.pooledCopy().sub(body.getCenter()).nor().scl(speed))

        if (processState == ProcessState.CONTINUE && start.dst2(target) < start.dst2(body.getCenter())) {
            GameLogger.debug(
                TAG, "update(): set processState to END: " +
                    "body.getCenter=${body.getCenter()}, start=$start, target=$target"
            )
            processState = ProcessState.END
            explodeAndDie()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val circle = GameCircle().setRadius(0.5f * ConstVals.PPM)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE pairTo circle.copy(), FixtureType.DAMAGER pairTo circle.copy())
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = target.pooledCopy().sub(start).nor().angleDeg()
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 1, 5, 0.1f, true)))
        .build()

    private fun spawnExplosion() {
        GameLogger.debug(TAG, "spawnExplosion(): hashcode=${hashCode()}")

        val explosion = MegaEntityFactory.fetch(ElecExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SOUND pairTo true,
                ConstKeys.OWNER pairTo owner,
                ConstKeys.POSITION pairTo body.getCenter(),
            )
        )
    }

    override fun getTag() = TAG

    override fun toString() =
        "{ ElecDevilBodyPiece:[\n" +
            "\tbounds=${body.getBounds()},\n" +
            "\tstart=$start,\n" +
            "\ttarget=$target,\n" +
            (if (spawned) "\tprocessState=$processState" else "\tnot spawned") +
            "\n] }"
}
