package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.megaman.maverick.game.entities.utils.TickFrameDef
import kotlin.math.sign

object D1000Frames {

    val BODY = orderedMapOf(
        "stand" pairTo TickFrameDef(
            124, 92, 55, 56, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(20, 20, 20, 20),
            0
        ),
        "walk" pairTo TickFrameDef(
            125, 91, 55, 55, 2,
            intArrayOf(0, 1),
            intArrayOf(4, 4),
            0
        ),
        "crouch_drop" pairTo TickFrameDef(
            127, 90, 55, 55, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(2, 2, 2),
            null
        ),
        "crouch_shake" pairTo TickFrameDef(
            127, 83, 55, 55, 3,
            intArrayOf(0, 1, 0, 2, 1, 2),
            intArrayOf(4, 5, 4, 6, 4, 40),
            null
        ),
        "ram" pairTo TickFrameDef(
            135, 96, 56, 54, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(6, 4, 4),
            1
        ),
        "throw" pairTo TickFrameDef(
            134, 98, 55, 56, 6,
            intArrayOf(0, 1, 2, 3, 4, 5, 4),
            intArrayOf(8, 5, 4, 4, 3, 2, 3),
            3
        ),
        "stagger" pairTo TickFrameDef(
            132, 117, 55, 54, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(10, 10, 8),
            null
        ),
        "fall" pairTo TickFrameDef(
            118, 117, 55, 54, 2,
            intArrayOf(0, 1),
            intArrayOf(4, 4),
            0
        ),
        "land" pairTo TickFrameDef(
            128, 83, 55, 55, 3,
            intArrayOf(0, 1, 0, 2, 1, 2),
            intArrayOf(4, 5, 4, 6, 4, 40),
            null
        ),
        "stand_up" pairTo TickFrameDef(
            128, 82, 55, 55, 4,
            intArrayOf(0, 1, 0, 1, 2, 1, 2, 1, 3, 1, 3, 1, 3, 1, 3, 0),
            intArrayOf(6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 1, 1, 1, 40),
            null
        )
    )

    const val SPIN_FRAMES = 12
    val SPIN_RAMP_TICKS = intArrayOf(5, 4, 3)
    const val SPIN_LOOP_TICKS = 2

    const val CORE_SHUT_FRAME = 2

    val CORE = orderedMapOf(
        "core" pairTo TickFrameDef(
            24, 24, 12, 12, 3,
            intArrayOf(0, 1, 2, 1),
            intArrayOf(90, 3, 5, 3),
            0
        ),
        "core_stare" pairTo TickFrameDef(
            24, 24, 12, 12, 1,
            intArrayOf(0),
            intArrayOf(1),
            null
        ),
        "core_angry" pairTo TickFrameDef(
            24, 24, 12, 12, 2,
            intArrayOf(0, 1),
            intArrayOf(8, 8),
            0
        ),
        "core_appear" pairTo TickFrameDef(
            24, 24, 12, 12, 5,
            intArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 3, 4, 2, 3, 4),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3),
            null
        ),
        "core_vanish" pairTo TickFrameDef(
            24, 24, 12, 12, 5,
            intArrayOf(0, 1, 2, 3, 4, 1, 2, 3, 4, 0, 1, 2, 3, 4, 1, 2, 3, 4, 0, 3, 4),
            intArrayOf(3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3),
            null
        ),
        "core_hit" pairTo TickFrameDef(
            24, 24, 12, 12, 1,
            intArrayOf(0),
            intArrayOf(1),
            null
        ),
        "core_spin" pairTo TickFrameDef(
            24, 24, 12, 12, SPIN_FRAMES,
            IntArray((SPIN_RAMP_TICKS.size + 1) * SPIN_FRAMES) { it % SPIN_FRAMES },
            IntArray((SPIN_RAMP_TICKS.size + 1) * SPIN_FRAMES) {
                if (it < SPIN_RAMP_TICKS.size * SPIN_FRAMES) SPIN_RAMP_TICKS[it / SPIN_FRAMES] else SPIN_LOOP_TICKS
            },
            SPIN_RAMP_TICKS.size * SPIN_FRAMES
        )
    )

    const val EYE_GLOW_SIZE = 32
    const val EYE_GLOW_FRAMES = 5
    const val EYE_GLOW_FLASH = 4
    val EYE_GLOW_DEF = TickFrameDef(
        EYE_GLOW_SIZE, EYE_GLOW_SIZE, EYE_GLOW_SIZE / 2, EYE_GLOW_SIZE / 2, 1, intArrayOf(0), intArrayOf(1), null
    )

    const val BEAM_WIDTH = 24
    const val BEAM_HEIGHT = 132
    const val BEAM_LOOP_FRAMES = 4
    const val BEAM_FLASH_FRAMES = 4

    const val THRUSTER_SIZE = 12
    const val THRUSTER_IDLE_FRAMES = 4
    const val THRUSTER_FLARE_FRAMES = 4
    const val THRUSTER_TOP = 22

    const val PUPIL_SIZE = 2
    const val PUPIL_FRAMES = 3
    const val PUPIL_REST_X = 9
    const val PUPIL_REST_Y = 13
    const val PUPIL_REACH_X = 2
    const val PUPIL_REACH_Y = 1
    const val PUPIL_OPEN = 0
    const val PUPIL_ANGRY = 1
    const val PUPIL_HIT = 2

    val IRIS_ROWS = arrayOf(
        intArrayOf(8, 7, 9),
        intArrayOf(9, 5, 11),
        intArrayOf(10, 5, 11),
        intArrayOf(11, 4, 12),
        intArrayOf(12, 4, 12),
        intArrayOf(13, 4, 12),
        intArrayOf(14, 5, 11),
        intArrayOf(15, 5, 11),
        intArrayOf(16, 7, 9)
    )

    fun inIris(left: Int, top: Int): Boolean {
        for (y in top until top + PUPIL_SIZE) {
            val row = IRIS_ROWS.firstOrNull { it[0] == y } ?: return false
            if (left < row[1] || left + PUPIL_SIZE - 1 > row[2]) return false
        }
        return true
    }

    fun pupilOffset(towardX: Float, towardY: Float, out: IntArray): IntArray {
        var dx = MathUtils.round(towardX * PUPIL_REACH_X).coerceIn(-PUPIL_REACH_X, PUPIL_REACH_X)
        var dy = MathUtils.round(towardY * PUPIL_REACH_Y).coerceIn(-PUPIL_REACH_Y, PUPIL_REACH_Y)
        while (!inIris(PUPIL_REST_X + dx, PUPIL_REST_Y + dy)) {
            if (dx != 0) dx -= dx.sign else if (dy != 0) dy -= dy.sign
            else throw IllegalStateException("The pupil's rest place is outside the iris")
        }
        out[0] = dx
        out[1] = dy
        return out
    }

    val BOMB = orderedMapOf(
        "bomb" pairTo TickFrameDef(
            16, 16, 8, 8, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        )
    )

    val PILLAR = orderedMapOf(
        "pillar_start" pairTo TickFrameDef(
            13, 112, 7, 64, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(1, 2, 2),
            null
        ),
        "pillar" pairTo TickFrameDef(
            24, 112, 12, 64, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        ),
        "pillar_end" pairTo TickFrameDef(
            13, 112, 7, 64, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(2, 2, 1),
            null
        ),
        "pillar_base" pairTo TickFrameDef(
            36, 15, 18, 11, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        )
    )

    val MARKER = orderedMapOf(
        "marker" pairTo TickFrameDef(
            32, 12, 16, 12, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(3, 3, 3, 3),
            0
        )
    )

    val DUST = orderedMapOf(
        "dust" pairTo TickFrameDef(
            16, 24, 8, 0, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(4, 4, 4, 4),
            0
        )
    )

    const val LAMP_LEFT = 8
    const val LAMP_TOP = 4
    const val LAMP_WIDTH = 40
    const val LAMP_HEIGHT = 42

    const val MOUTH_LEFT = 15
    const val MOUTH_TOP = 34
    const val MOUTH_WIDTH = 13
    const val MOUTH_HEIGHT = 8

    const val SCREW_LEFT = 73
    const val SCREW_TOP = 26
    const val SCREW_WIDTH = 12
    const val SCREW_HEIGHT = 13
    const val SCREW_POSES = 4

    const val CHARGE_SIZE = 24
    const val CHARGE_FRAMES = 7
    const val CHARGE_GROW_FRAMES = 6
    const val CHARGE_FLASH = 6
    val CHARGE_DEF = TickFrameDef(
        CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE / 2, CHARGE_SIZE / 2, 1, intArrayOf(0), intArrayOf(1), null
    )

    const val PUFF_WIDTH = 22
    const val PUFF_HEIGHT = 30
    const val PUFF_FRAMES = 6
    const val PUFF_OVERLAP = 4

    private fun overlayDefs(left: Int, top: Int, width: Int, height: Int) =
        ObjectMap<String, TickFrameDef>().also { defs ->
            BODY.keys().forEach { key ->
                val body = BODY[key]
                defs.put(
                    key, TickFrameDef(
                        width, height, body.originX - left, body.originY - top, 1, intArrayOf(0), intArrayOf(1), null
                    )
                )
            }
        }

    private val LAMP_DEFS = overlayDefs(LAMP_LEFT, LAMP_TOP, LAMP_WIDTH, LAMP_HEIGHT)
    private val MOUTH_DEFS = overlayDefs(MOUTH_LEFT, MOUTH_TOP, MOUTH_WIDTH, MOUTH_HEIGHT)
    private val SCREW_DEFS = overlayDefs(SCREW_LEFT, SCREW_TOP, SCREW_WIDTH, SCREW_HEIGHT)

    private fun def(defs: ObjectMap<String, TickFrameDef>, anim: String): TickFrameDef =
        defs[anim] ?: throw IllegalStateException("D-1000 has no body animation $anim")

    fun lampDef(anim: String) = def(LAMP_DEFS, anim)

    fun mouthDef(anim: String) = def(MOUTH_DEFS, anim)

    fun screwDef(anim: String) = def(SCREW_DEFS, anim)

    private val LAMP_START = ObjectMap<String, Int>().also { starts ->
        var start = 0
        BODY.keys().forEach { key ->
            starts.put(key, start)
            start += BODY[key].count
        }
    }

    val LAMP_FRAMES = BODY.values().sumOf { it.count }

    fun lampFrame(anim: String, artFrame: Int) =
        (LAMP_START[anim] ?: throw IllegalStateException("D-1000 has no body animation $anim")) + artFrame

    val NOZZLES = orderedMapOf(
        "stand" pairTo arrayOf(
            intArrayOf(75, 9),
            intArrayOf(76, 7),
            intArrayOf(75, 7),
            intArrayOf(77, 8)
        ),
        "walk" pairTo arrayOf(
            intArrayOf(75, 8),
            intArrayOf(74, 7)
        ),
        "crouch_drop" pairTo arrayOf(
            intArrayOf(74, 7),
            intArrayOf(74, 7),
            intArrayOf(74, 7)
        ),
        "crouch_shake" pairTo arrayOf(
            intArrayOf(74, 8),
            intArrayOf(74, 7),
            intArrayOf(74, 7)
        ),
        "ram" pairTo arrayOf(
            intArrayOf(75, 7),
            intArrayOf(74, 7),
            intArrayOf(73, 7)
        ),
        "throw" pairTo arrayOf(
            intArrayOf(76, 7),
            intArrayOf(74, 8),
            intArrayOf(73, 9),
            intArrayOf(72, 10),
            intArrayOf(72, 10),
            intArrayOf(72, 10)
        ),
        "stagger" pairTo arrayOf(
            intArrayOf(75, 7),
            intArrayOf(75, 7),
            intArrayOf(75, 7)
        ),
        "fall" pairTo arrayOf(
            intArrayOf(75, 8),
            intArrayOf(75, 7)
        ),
        "land" pairTo arrayOf(
            intArrayOf(75, 8),
            intArrayOf(75, 7),
            intArrayOf(75, 7)
        ),
        "stand_up" pairTo arrayOf(
            intArrayOf(75, 7),
            intArrayOf(75, 7),
            intArrayOf(75, 7),
            intArrayOf(75, 7)
        )
    )

    private val PUFF_DEFS = ObjectMap<String, Array<TickFrameDef>>().also { defs ->
        BODY.keys().forEach { key ->
            val body = BODY[key]
            val nozzles = NOZZLES[key] ?: throw IllegalStateException("D-1000 has no nozzle anchors for $key")
            defs.put(key, Array(body.count) { frame ->
                val left = nozzles[frame][0] - PUFF_WIDTH / 2
                val top = nozzles[frame][1] - PUFF_HEIGHT + PUFF_OVERLAP
                TickFrameDef(
                    PUFF_WIDTH, PUFF_HEIGHT, body.originX - left, body.originY - top, 1,
                    intArrayOf(0), intArrayOf(1), null
                )
            })
        }
    }

    fun puffDef(anim: String, artFrame: Int): TickFrameDef =
        (PUFF_DEFS[anim] ?: throw IllegalStateException("D-1000 has no body animation $anim"))[artFrame]

    val BODY_BOXES = orderedMapOf(
        "stand" pairTo arrayOf(
            intArrayOf(6, 8, 26, 7, 64, 4, 35, 11, 4, 15, 101, 30, 2, 45, 121, 32, 27, 77, 71, 6, 23, 83, 47, 9),
            intArrayOf(6, 7, 27, 7, 64, 3, 35, 11, 3, 14, 101, 32, 2, 46, 121, 31, 34, 77, 65, 6, 23, 83, 47, 9),
            intArrayOf(4, 6, 100, 39, 2, 45, 121, 26, 46, 71, 77, 6, 27, 77, 71, 6, 23, 83, 47, 9),
            intArrayOf(6, 8, 28, 7, 64, 3, 35, 12, 4, 15, 100, 30, 2, 45, 121, 31, 34, 76, 64, 6, 23, 82, 47, 9)
        ),
        "walk" pairTo arrayOf(
            intArrayOf(6, 7, 26, 7, 64, 3, 35, 11, 4, 14, 101, 30, 2, 44, 121, 32, 27, 76, 71, 6, 23, 82, 47, 9),
            intArrayOf(6, 7, 25, 6, 64, 2, 34, 11, 3, 13, 101, 31, 2, 44, 122, 31, 34, 75, 65, 6, 24, 81, 47, 9)
        ),
        "crouch_drop" pairTo arrayOf(
            intArrayOf(6, 7, 25, 6, 64, 2, 34, 11, 3, 13, 101, 31, 2, 44, 122, 31, 34, 75, 65, 6, 24, 81, 47, 9),
            intArrayOf(5, 7, 28, 7, 63, 2, 35, 12, 3, 14, 102, 20, 2, 34, 123, 37, 25, 71, 48, 9),
            intArrayOf(6, 7, 25, 6, 64, 2, 34, 11, 3, 13, 102, 20, 2, 33, 123, 38, 26, 71, 46, 8)
        ),
        "crouch_shake" pairTo arrayOf(
            intArrayOf(6, 7, 26, 7, 64, 2, 34, 12, 3, 14, 102, 19, 2, 33, 123, 37, 26, 70, 46, 9),
            intArrayOf(6, 7, 25, 6, 64, 2, 34, 11, 3, 13, 102, 20, 2, 33, 123, 38, 26, 71, 46, 8),
            intArrayOf(5, 7, 28, 7, 63, 2, 35, 12, 3, 14, 102, 20, 2, 34, 123, 37, 25, 71, 48, 9)
        ),
        "ram" pairTo arrayOf(
            intArrayOf(7, 6, 26, 7, 65, 1, 34, 12, 4, 13, 101, 30, 2, 43, 122, 32, 27, 75, 71, 6, 24, 81, 46, 9),
            intArrayOf(4, 6, 100, 22, 2, 28, 126, 32, 7, 60, 105, 11, 34, 71, 63, 10, 46, 81, 40, 13),
            intArrayOf(3, 6, 101, 22, 3, 28, 126, 33, 4, 61, 109, 10, 35, 71, 63, 10, 47, 81, 41, 13)
        ),
        "throw" pairTo arrayOf(
            intArrayOf(6, 7, 27, 7, 64, 3, 35, 11, 3, 14, 101, 32, 2, 46, 121, 31, 34, 77, 65, 6, 23, 83, 47, 9),
            intArrayOf(6, 8, 25, 6, 64, 3, 34, 11, 3, 14, 101, 31, 1, 45, 123, 32, 27, 77, 72, 6, 24, 83, 46, 9),
            intArrayOf(68, 2, 20, 6, 3, 8, 100, 22, 2, 30, 126, 31, 7, 61, 106, 12, 33, 73, 63, 10, 46, 83, 40, 13),
            intArrayOf(2, 9, 101, 21, 2, 30, 126, 34, 7, 64, 105, 9, 34, 73, 63, 10, 46, 83, 41, 13),
            intArrayOf(2, 9, 101, 21, 2, 30, 126, 34, 7, 64, 105, 9, 34, 73, 63, 10, 46, 83, 41, 13),
            intArrayOf(2, 9, 101, 21, 2, 30, 126, 34, 7, 64, 105, 9, 34, 73, 63, 10, 46, 83, 41, 13)
        ),
        "stagger" pairTo arrayOf(
            intArrayOf(6, 6, 26, 7, 64, 2, 35, 11, 4, 13, 101, 30, 2, 43, 121, 32, 27, 75, 71, 6, 23, 81, 47, 9),
            intArrayOf(6, 6, 26, 7, 64, 2, 35, 11, 4, 13, 101, 26, 2, 39, 125, 32, 33, 71, 75, 21, 45, 92, 40, 13),
            intArrayOf(3, 6, 100, 50, 2, 56, 112, 15, 33, 71, 84, 11, 19, 82, 74, 20, 31, 102, 40, 13)
        ),
        "fall" pairTo arrayOf(
            intArrayOf(3, 7, 100, 48, 2, 55, 112, 17, 31, 72, 84, 10, 19, 82, 74, 19, 32, 101, 40, 13),
            intArrayOf(3, 6, 100, 50, 2, 56, 112, 15, 33, 71, 84, 11, 19, 82, 74, 20, 31, 102, 40, 13)
        ),
        "land" pairTo arrayOf(
            intArrayOf(6, 7, 26, 7, 64, 3, 35, 11, 4, 14, 102, 19, 3, 33, 123, 38, 27, 71, 45, 8),
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 19, 3, 33, 123, 38, 27, 71, 46, 8),
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 20, 2, 34, 124, 37, 26, 71, 47, 9)
        ),
        "stand_up" pairTo arrayOf(
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 20, 2, 34, 124, 37, 26, 71, 47, 9),
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 20, 2, 34, 124, 37, 26, 71, 47, 9),
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 20, 2, 34, 124, 37, 26, 71, 47, 9),
            intArrayOf(6, 7, 28, 7, 64, 2, 35, 12, 4, 14, 102, 20, 2, 34, 124, 37, 26, 71, 47, 9)
        )
    )

    private val BALL = intArrayOf(7, 0, 10, 24, 3, 2, 18, 20, 0, 7, 24, 10)
    private val EYE = intArrayOf(2, 6, 13, 13)

    val CORE_BOXES = orderedMapOf(
        "core" pairTo Array(3) { BALL },
        "core_stare" pairTo arrayOf(BALL),
        "core_angry" pairTo Array(2) { BALL },
        "core_appear" pairTo Array(5) { BALL },
        "core_vanish" pairTo Array(5) { BALL },
        "core_hit" pairTo arrayOf(BALL),
        "core_spin" pairTo Array(SPIN_FRAMES) { BALL }
    )

    val EYE_BOXES = orderedMapOf(
        "core" pairTo Array(3) { EYE },
        "core_stare" pairTo arrayOf(EYE),
        "core_angry" pairTo Array(2) { EYE },
        "core_appear" pairTo Array(5) { BALL },
        "core_vanish" pairTo Array(5) { BALL },
        "core_hit" pairTo arrayOf(EYE),
        "core_spin" pairTo Array(SPIN_FRAMES) { BALL }
    )
}
