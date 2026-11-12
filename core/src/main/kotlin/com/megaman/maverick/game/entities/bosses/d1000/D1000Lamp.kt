package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import kotlin.math.min
import com.megaman.maverick.game.entities.bosses.D1000

enum class D1000Tell(val red: Float, val green: Float, val blue: Float) {
    OFF(0.45f, 0.2f, 0.18f),
    RAM(1f, 0.3f, 0.15f),
    BOMB(1f, 0.85f, 0.15f),
    SLAM(0.3f, 0.65f, 1f),
    EYE(0.35f, 1f, 0.4f)
}

class D1000Lamp {

    companion object {
        const val LEAD_DUR = 0.4f
        const val BLINK_DUR = 4f / TickFrames.FPS
        const val REGION = "D1000/lamp"
        const val FACE_REGION = "D1000/face"
        const val GLOW_REGION = "D1000/glow"
        const val MOUTH_REGION = "D1000/mouth"
        const val SCREW_REGION = "D1000/screw"
        const val PUFF_REGION = "D1000/puff"
        const val CHARGE_REGION = "D1000/charge"
        const val CHARGE_GLOW_REGION = "D1000/charge_glow"

        const val FACE_IDLE_RED = 0.85f
        const val FACE_IDLE_GREEN = 0.2f
        const val FACE_IDLE_BLUE = 0.12f
        const val IDLE_GLOW = 0.3f
        const val LEAD_GLOW_DIM = 0.25f
        const val RUN_GLOW_MIN = 0.55f
        const val RUN_GLOW_HZ = 1.5f
        const val SCREW_STEP_LEAD = 2f / TickFrames.FPS
        const val SCREW_STEP_RUN = 4f / TickFrames.FPS
        const val PUFF_STEP = 3f / TickFrames.FPS
        const val CHARGE_GROW_SHARE = 0.6f
        const val CHARGE_FLASH_DUR = 3f / TickFrames.FPS
        const val CHARGE_GLOW_LOW = 0.35f
        const val CHARGE_GLOW_HIGH = 0.7f

        fun chargeFrame(progress: Float, clock: Float) = when {
            progress < CHARGE_GROW_SHARE -> min(
                (progress / CHARGE_GROW_SHARE * D1000Frames.CHARGE_GROW_FRAMES).toInt(), D1000Frames.CHARGE_GROW_FRAMES - 1
            )

            chargeFlash(clock) -> D1000Frames.CHARGE_FLASH
            else -> D1000Frames.CHARGE_GROW_FRAMES - 1
        }

        fun chargeGlow(progress: Float, clock: Float) = when {
            progress < CHARGE_GROW_SHARE ->
                CHARGE_GLOW_LOW + (CHARGE_GLOW_HIGH - CHARGE_GLOW_LOW) * progress / CHARGE_GROW_SHARE
            chargeFlash(clock) -> 1f
            else -> CHARGE_GLOW_HIGH
        }

        private fun chargeFlash(clock: Float) = (clock / CHARGE_FLASH_DUR).toInt() % 2 == 1
    }

    var tell = D1000Tell.OFF
        private set
    private var leading = false
    private var time = 0f
    private var puffTime = 0f
    private lateinit var regions: Array<TextureRegion>
    private lateinit var faceRegions: Array<TextureRegion>
    private lateinit var glowRegions: Array<TextureRegion>
    private lateinit var mouthRegions: Array<TextureRegion>
    private lateinit var screwRegions: Array<TextureRegion>
    private lateinit var puffRegions: Array<TextureRegion>
    private lateinit var chargeRegions: Array<TextureRegion>
    private lateinit var chargeGlowRegions: Array<TextureRegion>

    fun load(atlas: TextureAtlas) {
        regions = slice(atlas, REGION, D1000Frames.LAMP_FRAMES, D1000Frames.LAMP_WIDTH, D1000Frames.LAMP_HEIGHT)
        faceRegions =
            slice(atlas, FACE_REGION, D1000Frames.LAMP_FRAMES, D1000Frames.LAMP_WIDTH, D1000Frames.LAMP_HEIGHT)
        glowRegions =
            slice(atlas, GLOW_REGION, D1000Frames.LAMP_FRAMES, D1000Frames.LAMP_WIDTH, D1000Frames.LAMP_HEIGHT)
        mouthRegions =
            slice(atlas, MOUTH_REGION, D1000Frames.LAMP_FRAMES, D1000Frames.MOUTH_WIDTH, D1000Frames.MOUTH_HEIGHT)
        screwRegions = slice(
            atlas, SCREW_REGION, D1000Frames.LAMP_FRAMES * D1000Frames.SCREW_POSES, D1000Frames.SCREW_WIDTH,
            D1000Frames.SCREW_HEIGHT
        )
        puffRegions =
            slice(atlas, PUFF_REGION, D1000Frames.PUFF_FRAMES, D1000Frames.PUFF_WIDTH, D1000Frames.PUFF_HEIGHT)
        chargeRegions = slice(
            atlas, CHARGE_REGION, D1000Frames.CHARGE_FRAMES, D1000Frames.CHARGE_SIZE, D1000Frames.CHARGE_SIZE
        )
        chargeGlowRegions = slice(
            atlas, CHARGE_GLOW_REGION, D1000Frames.CHARGE_FRAMES, D1000Frames.CHARGE_SIZE, D1000Frames.CHARGE_SIZE
        )
    }

    private fun slice(atlas: TextureAtlas, key: String, count: Int, width: Int, height: Int): Array<TextureRegion> {
        val strip = atlas.findRegion(key) ?: throw IllegalStateException("Missing region $key")
        return Array(count) { TextureRegion(strip, 0, it * height, width, height) }
    }

    fun tell(action: D1000Action?, lead: Boolean) {
        tell = action?.family ?: D1000Tell.OFF
        leading = lead
        time = 0f
    }

    fun update(delta: Float) {
        time += delta
        puffTime += delta
    }

    private fun blinkOff() = leading && (time / BLINK_DUR).toInt() % 2 == 1

    fun shown() = if (blinkOff()) D1000Tell.OFF else tell

    fun glow() = when {
        tell == D1000Tell.OFF -> IDLE_GLOW
        leading -> if (blinkOff()) LEAD_GLOW_DIM else 1f
        else -> RUN_GLOW_MIN + (1f - RUN_GLOW_MIN) * (0.5f + 0.5f * MathUtils.sin(time * MathUtils.PI2 * RUN_GLOW_HZ))
    }

    fun mouthOpen() = tell == D1000Tell.BOMB && !blinkOff()

    fun screwPose() =
        if (tell != D1000Tell.EYE) null
        else (time / if (leading) SCREW_STEP_LEAD else SCREW_STEP_RUN).toInt() % D1000Frames.SCREW_POSES

    fun puffFrame() = (puffTime / PUFF_STEP).toInt() % D1000Frames.PUFF_FRAMES

    fun place(sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean) {
        val color = shown()
        put(sprite, regions[D1000Frames.lampFrame(anim, artFrame)], D1000Frames.lampDef(anim), originX, originY,
            facingRight)
        sprite.setColor(color.red, color.green, color.blue, 1f)
    }

    fun placeFace(
        sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean
    ) {
        put(sprite, faceRegions[D1000Frames.lampFrame(anim, artFrame)], D1000Frames.lampDef(anim), originX, originY,
            facingRight)
        if (tell == D1000Tell.OFF) sprite.setColor(FACE_IDLE_RED, FACE_IDLE_GREEN, FACE_IDLE_BLUE, 1f)
        else sprite.setColor(tell.red, tell.green, tell.blue, 1f)
    }

    fun placeGlow(
        sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean
    ) {
        put(sprite, glowRegions[D1000Frames.lampFrame(anim, artFrame)], D1000Frames.lampDef(anim), originX, originY,
            facingRight)
        sprite.setColor(tell.red, tell.green, tell.blue, glow())
    }

    fun placeMouth(
        sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean
    ): Boolean {
        if (!mouthOpen()) return false
        put(sprite, mouthRegions[D1000Frames.lampFrame(anim, artFrame)], D1000Frames.mouthDef(anim), originX, originY,
            facingRight)
        return true
    }

    fun placeScrew(
        sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean
    ): Boolean {
        val pose = screwPose() ?: return false
        val index = D1000Frames.lampFrame(anim, artFrame) * D1000Frames.SCREW_POSES + pose
        put(sprite, screwRegions[index], D1000Frames.screwDef(anim), originX, originY, facingRight)
        return true
    }

    fun placePuff(sprite: GameSprite, anim: String, artFrame: Int, originX: Float, originY: Float, facingRight: Boolean) =
        put(sprite, puffRegions[puffFrame()], D1000Frames.puffDef(anim, artFrame), originX, originY, facingRight)

    fun placeCharge(sprite: GameSprite, progress: Float, clock: Float, x: Float, y: Float, facingRight: Boolean) {
        put(sprite, chargeRegions[chargeFrame(progress, clock)], D1000Frames.CHARGE_DEF, x, y, facingRight)
    }

    fun placeChargeGlow(sprite: GameSprite, progress: Float, clock: Float, x: Float, y: Float, facingRight: Boolean) {
        put(sprite, chargeGlowRegions[chargeFrame(progress, clock)], D1000Frames.CHARGE_DEF, x, y, facingRight)
        sprite.setColor(1f, 1f, 1f, chargeGlow(progress, clock))
    }

    private fun put(
        sprite: GameSprite, region: TextureRegion, def: TickFrameDef, originX: Float, originY: Float,
        facingRight: Boolean
    ) {
        sprite.setRegion(region)
        TickFrames.place(sprite, def, originX, originY, facingRight)
    }
}
