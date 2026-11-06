package com.megaman.maverick.game.entities.bosses

import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.megaman.maverick.game.entities.utils.X6FrameDef

object D1000Frames {

    val BODY = orderedMapOf(
        "stand" pairTo X6FrameDef(
            100, 74, 44, 45, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(20, 20, 20, 20),
            0
        ),
        "walk" pairTo X6FrameDef(
            100, 73, 44, 44, 2,
            intArrayOf(0, 1),
            intArrayOf(4, 4),
            0
        ),
        "crouch_drop" pairTo X6FrameDef(
            102, 72, 44, 44, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(2, 2, 2),
            null
        ),
        "crouch_shake" pairTo X6FrameDef(
            102, 66, 44, 44, 3,
            intArrayOf(0, 1, 0, 2, 1, 2),
            intArrayOf(4, 5, 4, 6, 4, 40),
            null
        ),
        "ram" pairTo X6FrameDef(
            108, 78, 45, 44, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(6, 4, 4),
            1
        ),
        "throw" pairTo X6FrameDef(
            107, 79, 44, 45, 6,
            intArrayOf(0, 1, 2, 3, 4, 5, 4),
            intArrayOf(8, 5, 4, 4, 3, 2, 3),
            3
        ),
        "stagger" pairTo X6FrameDef(
            106, 95, 44, 44, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(10, 10, 8),
            null
        ),
        "fall" pairTo X6FrameDef(
            95, 95, 44, 44, 2,
            intArrayOf(0, 1),
            intArrayOf(4, 4),
            0
        ),
        "land" pairTo X6FrameDef(
            102, 66, 44, 44, 3,
            intArrayOf(0, 1, 0, 2, 1, 2),
            intArrayOf(4, 5, 4, 6, 4, 40),
            null
        ),
        "stand_up" pairTo X6FrameDef(
            102, 66, 44, 44, 4,
            intArrayOf(0, 1, 0, 1, 2, 1, 2, 1, 3, 1, 3, 1, 3, 1, 3, 0),
            intArrayOf(6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 1, 1, 1, 40),
            null
        )
    )

    val CORE = orderedMapOf(
        "core" pairTo X6FrameDef(
            16, 16, 8, 8, 1,
            intArrayOf(0),
            intArrayOf(1),
            null
        ),
        "core_appear" pairTo X6FrameDef(
            16, 16, 8, 8, 5,
            intArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 3, 4, 2, 3, 4),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3),
            null
        ),
        "core_vanish" pairTo X6FrameDef(
            16, 16, 8, 8, 5,
            intArrayOf(0, 1, 2, 3, 4, 1, 2, 3, 4, 0, 1, 2, 3, 4, 1, 2, 3, 4, 0, 3, 4),
            intArrayOf(3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3),
            null
        )
    )

    val BOMB = orderedMapOf(
        "bomb" pairTo X6FrameDef(
            16, 16, 8, 8, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        )
    )

    val PILLAR = orderedMapOf(
        "pillar_start" pairTo X6FrameDef(
            13, 112, 7, 64, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(1, 2, 2),
            null
        ),
        "pillar" pairTo X6FrameDef(
            24, 112, 12, 64, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        ),
        "pillar_end" pairTo X6FrameDef(
            13, 112, 7, 64, 3,
            intArrayOf(0, 1, 2),
            intArrayOf(2, 2, 1),
            null
        ),
        "pillar_base" pairTo X6FrameDef(
            36, 15, 18, 11, 4,
            intArrayOf(0, 1, 2, 3),
            intArrayOf(2, 2, 2, 2),
            0
        )
    )
}
