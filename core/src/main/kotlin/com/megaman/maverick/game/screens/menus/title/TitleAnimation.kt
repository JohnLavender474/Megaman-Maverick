package com.megaman.maverick.game.screens.menus.title

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.megaman.maverick.game.ConstVals
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class TitleAnimation(titleLettersAtlas: TextureAtlas, private val formedTitle: TextureRegion) : Updatable, Resettable {

    companion object {
        const val TAG = "TitleAnimation"
        private const val NES_PIXEL = ConstVals.PPM / 16f
        private const val IMAGE_SIZE = 256
        private const val IMAGE_TOP_CROP = 16
        private const val VISIBLE_ROWS = 224
        private const val STEP_DURATION = 3f / 60f
        private const val MAX_STEPS_PER_UPDATE = 10
        private const val LEAD_STEPS = 6
        private const val SWOOSH_SCAN_STEPS = 7
        private const val SWOOSH_EDGE_COLUMNS = 2
        private const val BEAM_STEPS = 2
        private const val BEAM_COLUMNS = 3
        private const val FULL_REVEAL = 99
        private const val FIRST_TOP_START = 8
        private const val FIRST_BOTTOM_START = 20
        private const val START_INTERVAL = 2
        private const val TOP_BEAM_COLOR = 0x1AD7F4
        private const val BOTTOM_BEAM_COLOR = 0xFA9687
        private const val WHITE_COLOR = 0xFCFCFC
        private const val SWOOSH_X = 3
        private const val SWOOSH_Y = 35
        private val REVEAL_HALF_WIDTHS = intArrayOf(1, 3, 6, 10, 15, FULL_REVEAL)
        private val TOP_LETTERS = arrayOf("M" to 14, "E" to 57, "G" to 82, "A" to 107, "M" to 140, "A" to 174, "N" to 201)
        private val BOTTOM_LETTERS = arrayOf(
            "M" to 46, "A" to 67, "V" to 86, "E" to 109, "R" to 131, "I" to 154, "C" to 167, "K" to 189
        )
        private const val TOP_Y = 23
        private const val BOTTOM_Y = 66
        private const val LAST_REVEAL_STEP = 42
        private const val FIRST_FLASH_STEP = 43
        private const val SECOND_FLASH_STEP = 45
        private const val FORMED_STEP = LEAD_STEPS + SECOND_FLASH_STEP + 1
    }

    var formed = false
        private set

    private var step = 0
    private var accumulator = 0f

    private val plate = titleLettersAtlas.findRegion("title_plate")
    private val swoosh = titleLettersAtlas.findRegion("swoosh")
    private val swooshWhite = titleLettersAtlas.findRegion("swoosh_white")
    private val swooshScratch = TextureRegion(swoosh)
    private val pixel = titleLettersAtlas.findRegion("pixel")
    private val letters = Array<TitleLetter>()
    private val beamColors = FloatArray(BEAM_COLUMNS * 3)
    private val whiteComponents = FloatArray(3)

    init {
        requireNotNull(plate) { "Missing region title_plate in the TitleLetters atlas" }
        requireNotNull(swoosh) { "Missing region swoosh in the TitleLetters atlas" }
        requireNotNull(swooshWhite) { "Missing region swoosh_white in the TitleLetters atlas" }
        requireNotNull(pixel) { "Missing region pixel in the TitleLetters atlas" }
        addLetters(titleLettersAtlas, "top", TOP_LETTERS, TOP_Y, FIRST_TOP_START, TOP_BEAM_COLOR)
        addLetters(titleLettersAtlas, "bot", BOTTOM_LETTERS, BOTTOM_Y, FIRST_BOTTOM_START, BOTTOM_BEAM_COLOR)
        setComponents(WHITE_COLOR, whiteComponents, 0)
        reset()
    }

    private fun addLetters(
        atlas: TextureAtlas,
        prefix: String,
        definitions: kotlin.Array<Pair<String, Int>>,
        homeY: Int,
        firstStart: Int,
        beamColor: Int
    ) {
        definitions.forEachIndexed { index, (letter, homeX) ->
            val name = "${prefix}_${index + 1}_$letter"
            val region = requireNotNull(atlas.findRegion(name)) { "Missing region $name in the TitleLetters atlas" }
            val whiteName = "${name}_white"
            val whiteRegion =
                requireNotNull(atlas.findRegion(whiteName)) { "Missing region $whiteName in the TitleLetters atlas" }
            letters.add(
                TitleLetter(
                    name = name,
                    homeX = homeX,
                    homeY = homeY,
                    startStep = firstStart + START_INTERVAL * index,
                    beamColor = beamColor,
                    region = region,
                    whiteRegion = whiteRegion
                )
            )
        }
    }

    private fun setComponents(rgb: Int, out: FloatArray, offset: Int) {
        out[offset] = ((rgb shr 16) and 0xFF) / 255f
        out[offset + 1] = ((rgb shr 8) and 0xFF) / 255f
        out[offset + 2] = (rgb and 0xFF) / 255f
    }

    override fun reset() {
        step = 0
        accumulator = 0f
        formed = false
    }

    fun skipToEnd() {
        step = FORMED_STEP
        accumulator = 0f
        formed = true
    }

    override fun update(delta: Float) {
        if (formed) return
        accumulator += delta
        var steps = 0
        while (accumulator >= STEP_DURATION && steps < MAX_STEPS_PER_UPDATE) {
            accumulator -= STEP_DURATION
            steps++
            step++
            if (step >= FORMED_STEP) {
                skipToEnd()
                return
            }
        }
        if (steps == MAX_STEPS_PER_UPDATE) accumulator = 0f
    }

    fun draw(batch: Batch) {
        val t = step - LEAD_STEPS
        when {
            formed || t == SECOND_FLASH_STEP - 1 -> drawRegion(batch, formedTitle, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
            t < 0 -> drawRegion(batch, plate, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
            t == FIRST_FLASH_STEP || t == SECOND_FLASH_STEP -> drawFlash(batch)
            else -> drawEntrance(batch, t)
        }
    }

    private fun drawEntrance(batch: Batch, t: Int) {
        drawRegion(batch, plate, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        drawSwoosh(batch, t)
        for (i in 0 until letters.size) drawLetter(batch, letters[i], t - letters[i].startStep)
    }

    private fun drawSwoosh(batch: Batch, t: Int) {
        val width = swoosh.regionWidth
        if (t >= SWOOSH_SCAN_STEPS) {
            drawPiece(batch, swoosh, swooshScratch, SWOOSH_X, SWOOSH_Y, 0, width)
            return
        }
        val edge = round(width * (t + 1) / SWOOSH_SCAN_STEPS.toDouble()).toInt()
        val solid = max(0, edge - SWOOSH_EDGE_COLUMNS)
        drawPiece(batch, swoosh, swooshScratch, SWOOSH_X, SWOOSH_Y, 0, solid)
        drawPiece(batch, swooshWhite, swooshScratch, SWOOSH_X, SWOOSH_Y, solid, edge - solid)
    }

    private fun drawLetter(batch: Batch, letter: TitleLetter, k: Int) {
        if (k < 0) return
        if (k < BEAM_STEPS) {
            drawBeam(batch, letter, k)
            return
        }
        val reveal = REVEAL_HALF_WIDTHS[min(k - BEAM_STEPS, REVEAL_HALF_WIDTHS.size - 1)]
        val left = max(0, letter.centerColumn - reveal)
        val right = min(letter.width - 1, letter.centerColumn + reveal)
        drawPiece(batch, letter.region, letter.scratch, letter.homeX, letter.homeY, left, right - left + 1)
        if (reveal == FULL_REVEAL) return
        val leftEdge = letter.centerColumn - reveal
        val rightEdge = letter.centerColumn + reveal
        if (leftEdge >= 0) drawPiece(batch, letter.whiteRegion, letter.scratch, letter.homeX, letter.homeY, leftEdge, 1)
        if (rightEdge < letter.width) {
            drawPiece(batch, letter.whiteRegion, letter.scratch, letter.homeX, letter.homeY, rightEdge, 1)
        }
    }

    private fun drawBeam(batch: Batch, letter: TitleLetter, k: Int) {
        val rows = round((letter.bottomRow - IMAGE_TOP_CROP) * (k + 1) / BEAM_STEPS.toDouble()).toInt()
        val x = letter.homeX + letter.centerColumn - 1
        setComponents(letter.beamColor, beamColors, 0)
        System.arraycopy(whiteComponents, 0, beamColors, 3, 3)
        System.arraycopy(beamColors, 0, beamColors, 6, 3)
        for (column in 0 until BEAM_COLUMNS) {
            batch.setColor(beamColors[column * 3], beamColors[column * 3 + 1], beamColors[column * 3 + 2], 1f)
            batch.draw(
                pixel,
                (x + column) * NES_PIXEL,
                (VISIBLE_ROWS - rows) * NES_PIXEL,
                NES_PIXEL,
                rows * NES_PIXEL
            )
        }
        batch.setColor(Color.WHITE)
    }

    private fun drawFlash(batch: Batch) {
        drawRegion(batch, plate, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
        drawPiece(batch, swooshWhite, swooshScratch, SWOOSH_X, SWOOSH_Y, 0, swooshWhite.regionWidth)
        for (i in 0 until letters.size) {
            val letter = letters[i]
            drawPiece(batch, letter.whiteRegion, letter.scratch, letter.homeX, letter.homeY, 0, letter.width)
        }
    }

    private fun drawPiece(
        batch: Batch,
        source: TextureRegion,
        scratch: TextureRegion,
        homeX: Int,
        homeY: Int,
        firstColumn: Int,
        columns: Int
    ) {
        if (columns <= 0) return
        scratch.setRegion(source, firstColumn, 0, columns, source.regionHeight)
        drawRegion(batch, scratch, homeX + firstColumn, homeY, columns, source.regionHeight)
    }

    private fun drawRegion(batch: Batch, region: TextureRegion, imageX: Int, imageY: Int, width: Int, height: Int) {
        val screenRow = imageY - IMAGE_TOP_CROP
        batch.draw(
            region,
            imageX * NES_PIXEL,
            (VISIBLE_ROWS - screenRow - height) * NES_PIXEL,
            width * NES_PIXEL,
            height * NES_PIXEL
        )
    }
}
