package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setBounds
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.misc.LightSourceUtils

class WilyGenerator(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "WilyGenerator"

        private const val BODY_SIZE = 9f
        private const val SPARK_SIZE = 2f

        private const val ON_FRAMES = 8
        private const val ON_FRAME_DURATION = 0.12f
        private const val POWER_DOWN_FRAMES = 6
        private const val POWER_DOWN_FRAME_DURATION = 0.1f
        private const val SPARK_FRAMES = 8
        private const val SPARK_LIT_FRAMES = 6
        private const val SPARK_FRAME_DURATION = 0.05f

        private const val SPARK_GAP_MIN = 1.2f
        private const val SPARK_GAP_MAX = 3.6f
        private const val DOUBLE_BURST_CHANCE = 0.3f

        private const val CORE_OFFSET_Y = 5.75f
        private const val TIP_OFFSET_X = 3.125f
        private const val TIP_OFFSET_Y = 3.875f

        private const val CORE_RADIUS = 3
        private const val DIM_CORE_RADIUS = 2
        private const val TIP_RADIUS = 1
        private const val BURST_RADIUS = 2
        private const val RADIANCE = 1f
        private const val BRIGHT_RADIANCE = 1.5f

        private val onFrames = Array<TextureRegion>()
        private val powerDownFrames = Array<TextureRegion>()
        private val sparkFrames = Array<TextureRegion>()
        private var offRegion: TextureRegion? = null
    }

    enum class GeneratorState { ON, POWER_DOWN, OFF }

    var state = GeneratorState.ON
        private set

    private inner class Coil(val side: Int) {
        val tip = Vector2()
        val gapTimer = Timer(SPARK_GAP_MAX)
        var burstTime = 0f
        var burstsLeft = 0

        val bursting: Boolean
            get() = burstsLeft > 0

        val sparkFrame: Int
            get() = (burstTime / SPARK_FRAME_DURATION).toInt().coerceAtMost(SPARK_FRAMES - 1)

        fun resetGap() {
            gapTimer.resetDuration(services.random.nextFloat(SPARK_GAP_MIN, SPARK_GAP_MAX))
        }

        fun stop() {
            burstsLeft = 0
            burstTime = 0f
        }

        fun update(delta: Float) {
            if (!bursting) {
                gapTimer.update(delta)
                if (!gapTimer.isFinished()) return
                burstsLeft = if (services.random.nextFloat(0f, 1f) < DOUBLE_BURST_CHANCE) 2 else 1
                burstTime = 0f
                return
            }
            burstTime += delta
            if (burstTime < SPARK_FRAMES * SPARK_FRAME_DURATION) return
            burstTime = 0f
            burstsLeft--
            if (!bursting) resetGap()
        }
    }

    private val lightKeys = ObjectSet<Int>()
    private val bounds = GameRectangle().setSize(BODY_SIZE * ConstVals.PPM)
    private val anchor = Vector2()
    private val core = Vector2()
    private val coils = gdxArrayOf(Coil(-1), Coil(1))
    private var stateTime = 0f
    private val cullOutOfBounds = getGameCameraCullingLogic(services.gameCamera, { bounds })

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (offRegion == null) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_2.source)
            atlas.findRegion("$TAG/on").splitAndFlatten(2, 4, onFrames)
            atlas.findRegion("$TAG/power_down").splitAndFlatten(2, 3, powerDownFrames)
            atlas.findRegion("$TAG/sparks").splitAndFlatten(1, SPARK_FRAMES, sparkFrames)
            offRegion = atlas.findRegion("$TAG/off")
        }
        super.init()
        addComponent(CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds)))
        addComponent(defineSpritesComponent())
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER, anchor)
        bounds.setBottomCenterToPoint(anchor)
        core.set(anchor.x, anchor.y + CORE_OFFSET_Y * ConstVals.PPM)
        coils.forEach { coil ->
            coil.tip.set(anchor.x + coil.side * TIP_OFFSET_X * ConstVals.PPM, anchor.y + TIP_OFFSET_Y * ConstVals.PPM)
            coil.stop()
            coil.resetGap()
        }
        lightKeys.clear()
        spawnProps.get(ConstKeys.KEYS, String::class)!!.split(",").forEach { lightKeys.add(it.trim().toInt()) }
        state = GeneratorState.ON
        stateTime = 0f
        cullOutOfBounds.reset()
    }

    fun powerDown() {
        GameLogger.debug(TAG, "powerDown(): state=$state")
        if (state != GeneratorState.ON) return
        state = GeneratorState.POWER_DOWN
        stateTime = 0f
        coils.forEach { it.stop() }
    }

    private fun update(delta: Float) {
        stateTime += delta
        when (state) {
            GeneratorState.ON -> {
                coils.forEach { it.update(delta) }
                sendLight(core, RADIANCE, CORE_RADIUS)
                coils.forEach { coil ->
                    sendLight(coil.tip, RADIANCE, TIP_RADIUS)
                    if (coil.bursting && coil.sparkFrame < SPARK_LIT_FRAMES)
                        sendLight(coil.tip, BRIGHT_RADIANCE, BURST_RADIUS)
                }
            }

            GeneratorState.POWER_DOWN -> {
                when (powerDownFrame()) {
                    0 -> {
                        sendLight(core, BRIGHT_RADIANCE, CORE_RADIUS)
                        coils.forEach { sendLight(it.tip, RADIANCE, TIP_RADIUS) }
                    }

                    1, 2 -> sendLight(core, RADIANCE, CORE_RADIUS)
                    3 -> sendLight(core, RADIANCE, DIM_CORE_RADIUS)
                }
                if (stateTime >= POWER_DOWN_FRAMES * POWER_DOWN_FRAME_DURATION) state = GeneratorState.OFF
            }

            GeneratorState.OFF -> {}
        }
    }

    private fun powerDownFrame() =
        (stateTime / POWER_DOWN_FRAME_DURATION).toInt().coerceAtMost(POWER_DOWN_FRAMES - 1)

    private fun sendLight(center: Vector2, radiance: Float, radius: Int) =
        LightSourceUtils.sendLightSourceEvent(this, lightKeys, center, radiance, radius)

    private fun bodyRegion() = when (state) {
        GeneratorState.ON -> onFrames[(stateTime / ON_FRAME_DURATION).toInt() % ON_FRAMES]
        GeneratorState.POWER_DOWN -> powerDownFrames[powerDownFrame()]
        GeneratorState.OFF -> offRegion!!
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val builder = SpritesComponentBuilder()
            .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -2)))
            .preProcess { _, sprite ->
                sprite.setRegion(bodyRegion())
                sprite.setBounds(bounds)
            }
        coils.forEach { coil ->
            builder
                .sprite("spark_${coil.side}", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1)).also {
                    it.setSize(SPARK_SIZE * ConstVals.PPM, SPARK_SIZE * ConstVals.PPM)
                })
                .preProcess { _, sprite ->
                    sprite.hidden = state != GeneratorState.ON || !coil.bursting
                    sprite.setRegion(sparkFrames[coil.sparkFrame])
                    sprite.setFlip(coil.side > 0, false)
                    sprite.setCenter(coil.tip.x, coil.tip.y)
                }
        }
        return builder.build()
    }

    override fun getType() = EntityType.DECORATION
}
