package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.swapped
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class Snow(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "Snow"
        private const val FADE_DUR = 0.5f
        private const val SWITCH_DELAY = 0.5f
        private const val PLAYGROUND_SIZE = 0.125f
        private const val BACKGROUND_SIZE = 0.0625f
        private const val MAX_SPAWNED_ALLOWED = 50
        private const val ROTATION_PER_SECOND = 1f
        private const val ROTATION_ENABLED = false
        private var region: TextureRegion? = null
    }

    private lateinit var sine: SineWave

    private val switchTimer = Timer(SWITCH_DELAY)
    private val fadeTimer = Timer(FADE_DUR)

    private var minAmplitude = 0f
    private var maxAmplitude = 0f
    private var minFrequency = 0f
    private var maxFrequency = 0f
    private var drift = 0f
    private var minY = 0f

    private var background = false
    private var fadingOut = false

    private val out = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn()=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        background = spawnProps.getOrDefault(ConstKeys.BACKGROUND, false, Boolean::class)

        minFrequency = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.FREQUENCY}", Float::class)!!
        maxFrequency = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.FREQUENCY}", Float::class)!!

        minAmplitude = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.AMPLITUDE}", Float::class)!!
        maxAmplitude = spawnProps.get("${ConstKeys.MAX}_${ConstKeys.AMPLITUDE}", Float::class)!!

        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        val amplitude = UtilMethods.getRandom(minAmplitude, maxAmplitude)
        val frequency = UtilMethods.getRandom(minFrequency, maxFrequency)
        sine = SineWave(spawn.cpy().swapped(), speed, amplitude, frequency)

        drift = spawnProps.getOrDefault(ConstKeys.DRIFT, 0f, Float::class)
        minY = spawnProps.get("${ConstKeys.MIN}_${ConstKeys.Y}", Float::class)!!

        switchTimer.reset()

        fadingOut = false
        fadeTimer.reset()

        val setOfAllSnow = MegaGameEntities.getOfTag(getTag())

        GameLogger.debug(
            TAG,
            "onSpawn(): " +
                "setOfAllSnowSize=${setOfAllSnow.size}, " +
                "megamanPos=${megaman.body.getCenter()}"
        )

        if (setOfAllSnow.size > MAX_SPAWNED_ALLOWED) {
            val iter = setOfAllSnow.iterator()
            while (iter.hasNext) {
                val snow = iter.next()
                if (snow is Snow && !snow.isFadingOut()) {
                    snow.fadeOutToDestroy()
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val size = MegaGameEntities.getOfTag(getTag()).size
        GameLogger.debug(
            TAG,
            "onDestroy(): snowAmount=$size"
        )
    }

    fun isFadingOut() = fadingOut

    fun fadeOutToDestroy() {
        GameLogger.debug(TAG, "fadeOutToDestroy()")
        if (isFadingOut()) {
            GameLogger.debug(TAG, "fadeOutToDestroy(): already fading out, doing nothing")
            return
        }
        fadingOut = true
        fadeTimer.reset()
    }

    private fun handleHit() {
        if (!background) destroy()
    }

    private fun adjust() {
        sine.amplitude = UtilMethods.getRandom(minAmplitude, maxAmplitude)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (fadingOut) {
            fadeTimer.update(delta)
            if (fadeTimer.isFinished()) {
                GameLogger.debug(TAG, "update(): faded out, now destroying")
                destroy()
                return@UpdatablesComponent
            }
        }

        switchTimer.update(delta)
        if (switchTimer.isFinished()) {
            adjust()
            switchTimer.reset()
        }

        sine.update(delta)
        val position = sine.getMotionValue(out).swapped()
        body.setPosition(position).translate(drift * delta, 0f)

        if (body.getY() < minY) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val bodyRect = GameRectangle()
        val bodyFixture = Fixture(body, FixtureType.BODY, bodyRect)
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            GameLogger.debug(TAG, "hitByBlock(): background=$background, mapObjId=${block.id}")
            handleHit()
        }
        bodyFixture.setHitByBodyReceiver receiver@{ entity, processState ->
            if (processState != ProcessState.BEGIN) return@receiver
            if ((entity as MegaGameEntity).getTag() == getTag()) return@receiver
            GameLogger.debug(TAG, "hitByBody(): background=$background, body=${entity.body.getBounds()}")
            handleHit()
        }
        bodyFixture.setHitByProjectileReceiver {
            GameLogger.debug(TAG, "hitByProjectile(): background=$background, projectile=$it")
            handleHit()
        }
        bodyFixture.setHitByWaterReceiver {
            GameLogger.debug(TAG, "hitByWater(): background=$background, water=$it")
            handleHit()
        }
        body.addFixture(bodyFixture)

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle(body))
        waterListenerFixture.setHitByWaterReceiver { destroy() }
        waterListenerFixture.putProperty(ConstKeys.SPLASH, false)
        body.addFixture(waterListenerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = if (background) BACKGROUND_SIZE else PLAYGROUND_SIZE
            body.setSize(size * ConstVals.PPM)
            bodyRect.set(body)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!))
        .updatable { delta, sprite ->
            sprite.setBounds(body.getBounds())

            val alpha = if (fadingOut) 1f - fadeTimer.getRatio() else 1f
            sprite.setAlpha(alpha)

            sprite.priority.section = if (background) DrawingSection.BACKGROUND else DrawingSection.PLAYGROUND

            sprite.setOriginCenter()

            if (ROTATION_ENABLED) sprite.rotation += ROTATION_PER_SECOND * 360f * delta
            else sprite.rotation = 0f
        }.build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
