package com.megaman.maverick.game.entities.bosses.mechadragon

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.ConstVals
import kotlin.math.abs

class ArtBox(val x: Int, val y: Int, val width: Int, val height: Int)

class ArtPoint(val x: Int, val y: Int)

object MechaDragonArt {

    const val CELL_WIDTH = 128
    const val AIR_CELL_HEIGHT = 144
    const val GROUND_CELL_HEIGHT = 128
    const val AIR_CENTER_ROW = 64
    const val GROUND_CENTER_ROW = 56
    const val FEET_ANCHOR_X = 76
    const val FROZEN_FOOT_ROW = 133
    const val DEFEATED_FOOT_ROW = 135

    val GAME_PX_PER_ART_PX = ConstVals.PPM / 16f

    val SWEEP_ANGLES = gdxArrayOf(30f, 15f, 0f, -15f, -30f, -45f)
    val SWEEP_MOUTHS = gdxArrayOf(
        ArtPoint(115, 30), ArtPoint(116, 36), ArtPoint(115, 42), ArtPoint(113, 47), ArtPoint(109, 52),
        ArtPoint(104, 55)
    )
    val SWEEP_HEAD_BOXES = gdxArrayOf(
        ArtBox(86, 9, 42, 34), ArtBox(89, 9, 38, 39), ArtBox(91, 11, 33, 44), ArtBox(94, 15, 29, 46),
        ArtBox(92, 20, 29, 46), ArtBox(89, 26, 30, 43)
    )
    val WINDUP_HEAD_BOXES = gdxArrayOf(ArtBox(88, 9, 37, 44), ArtBox(86, 9, 39, 42), ArtBox(84, 10, 43, 30))
    val END_HEAD_BOXES = gdxArrayOf(ArtBox(90, 24, 28, 40), ArtBox(93, 19, 28, 41))
    val CHOMP_HEAD_BOXES = gdxArrayOf(
        ArtBox(86, 4, 39, 43), ArtBox(98, 82, 29, 41), ArtBox(98, 84, 28, 41), ArtBox(90, 20, 28, 44)
    )
    val CHOMP_MOUTH = ArtPoint(125, 115)
    val STAND_HEAD_BOX = ArtBox(92, 18, 28, 42)
    val GROUND_TORSO_BOX = ArtBox(44, 30, 40, 76)
    val GROUND_NECK_CENTER = ArtPoint(84, 46)
    val GROUND_TAIL_BOXES = gdxArrayOf(ArtBox(20, 100, 24, 16), ArtBox(0, 112, 20, 16))
    val GROUND_LEGS_BOX = ArtBox(58, 104, 36, 24)

    fun sweepCell(angle: Float): Int {
        var best = 0
        for (i in 1 until SWEEP_ANGLES.size)
            if (abs(SWEEP_ANGLES[i] - angle) < abs(SWEEP_ANGLES[best] - angle)) best = i
        return best
    }

    fun cellAt(durations: Array<Float>, time: Float): Int {
        var remaining = time
        var index = 0
        while (index < durations.size - 1 && remaining > durations[index]) {
            remaining -= durations[index]
            index++
        }
        return index
    }

    fun artToOffset(artX: Float, artY: Float, centerRow: Int, facing: Facing, out: Vector2): Vector2 = out.set(
        (artX - CELL_WIDTH / 2f) * GAME_PX_PER_ART_PX * facing.value,
        (centerRow - artY) * GAME_PX_PER_ART_PX
    )

    fun pixelOffset(point: ArtPoint, centerRow: Int, facing: Facing, out: Vector2) =
        artToOffset(point.x + 0.5f, point.y + 0.5f, centerRow, facing, out)

    fun boxOffset(box: ArtBox, centerRow: Int, facing: Facing, out: Vector2) =
        artToOffset(box.x + box.width / 2f, box.y + box.height / 2f, centerRow, facing, out)

    fun groundCenter(landTarget: Vector2, facing: Facing, out: Vector2): Vector2 = out.set(
        landTarget.x - (FEET_ANCHOR_X - CELL_WIDTH / 2f) * GAME_PX_PER_ART_PX * facing.value,
        landTarget.y + (GROUND_CELL_HEIGHT - GROUND_CENTER_ROW) * GAME_PX_PER_ART_PX
    )

    fun spriteBottomBelowCenter(centerRow: Int, cellHeight: Int) = (cellHeight - centerRow) * GAME_PX_PER_ART_PX

    fun standingSpriteBottomBelowCenter(footRow: Int) =
        spriteBottomBelowCenter(GROUND_CENTER_ROW, GROUND_CELL_HEIGHT) + (AIR_CELL_HEIGHT - 1 - footRow) * GAME_PX_PER_ART_PX
}
