package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.swapped
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter

class Snow(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

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

    private val position = Vector2()
    private val out = Vector2()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn()=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        position.set(spawn)

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
        position.set(sine.getMotionValue(out).swapped()).add(drift * delta, 0f)

        if (position.y < minY) destroy()
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!))
        .preProcess { delta, sprite ->
            val size = if (background) BACKGROUND_SIZE else PLAYGROUND_SIZE
            sprite.setSize(size * ConstVals.PPM)
            sprite.setCenter(position)

            val ratio = (fadeTimer.getRatio() / 10) * 10
            val alpha = if (fadingOut) 1f - ratio else 1f
            sprite.setAlpha(alpha)

            sprite.priority.section = if (background) DrawingSection.BACKGROUND else DrawingSection.PLAYGROUND
            sprite.priority.value = if (background) 0 else 15

            sprite.setOriginCenter()
            if (ROTATION_ENABLED) sprite.rotation += ROTATION_PER_SECOND * 360f * delta
            else sprite.rotation = 0f
        }.build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
