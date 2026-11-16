package com.megaman.maverick.game.entities.protoman

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.players.SabreMove

internal object ProtomanSabreMoves {

    val SLASH_1 = SabreMove(
        bodyRegion = "sabre_1",
        arcRegion = "sabre_1_arc",
        stripFrames = 5,
        firstFrame = 0,
        frameTimes = floatArrayOf(0.05f, 0.0333f, 0.0333f, 0.0833f, 0.05f),
        arcWidth = 104,
        arcHeight = 60,
        arcCenterAboveBody = -1,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            intArrayOf(23, 6, 29, 10),
            intArrayOf(24, 1, 78, 26),
            intArrayOf(58, 1, 46, 59),
            intArrayOf(58, 8, 45, 52),
            intArrayOf(58, 29, 32, 31)
        ),
        artOnFrame = 1,
        swingEndFrame = 2,
        artOffFrame = 4,
        fullStrength = false
    )

    val SLASH_2 = SabreMove(
        bodyRegion = "sabre_2",
        arcRegion = "sabre_2_arc",
        stripFrames = 5,
        firstFrame = 0,
        frameTimes = floatArrayOf(0.05f, 0.0333f, 0.0333f, 0.0833f, 0.05f),
        arcWidth = 104,
        arcHeight = 64,
        arcCenterAboveBody = 0,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            intArrayOf(34, 28, 27, 17),
            intArrayOf(22, 11, 80, 53),
            intArrayOf(48, 1, 55, 61),
            intArrayOf(69, 1, 35, 49),
            intArrayOf(69, 1, 34, 26)
        ),
        artOnFrame = 1,
        swingEndFrame = 2,
        artOffFrame = 4,
        fullStrength = false
    )

    val SLASH_3 = SabreMove(
        bodyRegion = "sabre_3",
        arcRegion = "sabre_3_arc",
        stripFrames = 4,
        firstFrame = 0,
        frameTimes = floatArrayOf(0.05f, 0.0333f, 0.0833f, 0.05f),
        arcWidth = 106,
        arcHeight = 40,
        arcCenterAboveBody = -11,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            intArrayOf(23, 18, 30, 5),
            intArrayOf(23, 1, 80, 24),
            intArrayOf(72, 1, 34, 39),
            intArrayOf(72, 11, 33, 29)
        ),
        artOnFrame = 1,
        swingEndFrame = 1,
        artOffFrame = 3,
        fullStrength = true
    )

    val AIR = SabreMove(
        bodyRegion = "sabre_air",
        arcRegion = "sabre_air_arc",
        stripFrames = 5,
        firstFrame = 0,
        frameTimes = floatArrayOf(0.05f, 0.0333f, 0.0333f, 0.0833f, 0.05f),
        arcWidth = 102,
        arcHeight = 82,
        arcCenterAboveBody = -5,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            intArrayOf(42, 48, 17, 27),
            intArrayOf(20, 31, 79, 51),
            intArrayOf(59, 1, 43, 78),
            intArrayOf(62, 1, 39, 49),
            intArrayOf(62, 1, 31, 29)
        ),
        artOnFrame = 1,
        swingEndFrame = 2,
        artOffFrame = 4,
        fullStrength = false
    )

    val AIR_LAND = SabreMove(
        bodyRegion = "sabre_air_land",
        arcRegion = "sabre_air_arc",
        stripFrames = 5,
        firstFrame = 0,
        frameTimes = floatArrayOf(0.05f, 0.0333f, 0.0333f, 0.0833f, 0.05f),
        arcWidth = 102,
        arcHeight = 82,
        arcCenterAboveBody = -11,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            intArrayOf(42, 48, 17, 27),
            intArrayOf(20, 31, 79, 51),
            intArrayOf(59, 1, 43, 78),
            intArrayOf(62, 1, 39, 49),
            intArrayOf(62, 1, 31, 29)
        ),
        artOnFrame = 1,
        swingEndFrame = 2,
        artOffFrame = 4,
        fullStrength = false
    )

    val ALL = gdxArrayOf(SLASH_1, SLASH_2, SLASH_3, AIR, AIR_LAND)
}
