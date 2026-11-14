package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.players.SabreMove
import com.megaman.maverick.game.players.SabreParams
import com.megaman.maverick.game.players.SabreState
import com.megaman.maverick.game.players.bodyOverGround

internal object MegamanXSabreMoves {

    val FLOOR = SabreMove(
        bodyRegion = "sabre_floor",
        arcRegion = "sabre_floor_arc",
        stripFrames = 9,
        firstFrame = 1,
        frameTimes = MegamanXValues.SABRE_FLOOR_FRAME_TIMES,
        arcWidth = 112,
        arcHeight = 64,
        arcCenterAboveBody = 2,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            null,
            intArrayOf(42, 21, 11, 12),
            intArrayOf(37, 25, 23, 13),
            intArrayOf(53, 10, 52, 34),
            intArrayOf(64, 0, 43, 44),
            intArrayOf(48, 4, 43, 37),
            intArrayOf(46, 5, 42, 23),
            intArrayOf(47, 5, 23, 13),
            null
        ),
        artOnFrame = 1,
        swingEndFrame = 4,
        artOffFrame = 7,
        fullStrength = true
    )

    val AIR = SabreMove(
        bodyRegion = "sabre_air",
        arcRegion = "sabre_air_arc",
        stripFrames = 9,
        firstFrame = 0,
        frameTimes = MegamanXValues.SABRE_AIR_FRAME_TIMES,
        arcWidth = 104,
        arcHeight = 54,
        arcCenterAboveBody = -3,
        stripFacesLeft = false,
        hitBoxes = gdxArrayOf(
            null,
            null,
            intArrayOf(43, 31, 17, 8),
            intArrayOf(52, 21, 23, 19),
            intArrayOf(43, 12, 43, 28),
            intArrayOf(31, 0, 60, 36),
            intArrayOf(30, 11, 12, 18),
            null,
            null
        ),
        artOnFrame = 2,
        swingEndFrame = 5,
        artOffFrame = 6,
        fullStrength = true
    )

    val WALL = SabreMove(
        bodyRegion = "sabre_wall",
        arcRegion = "sabre_wall_arc",
        stripFrames = 8,
        firstFrame = 0,
        frameTimes = MegamanXValues.SABRE_WALL_FRAME_TIMES,
        arcWidth = 104,
        arcHeight = 54,
        arcCenterAboveBody = -3,
        stripFacesLeft = true,
        hitBoxes = gdxArrayOf(
            null,
            intArrayOf(45, 28, 16, 10),
            intArrayOf(8, 11, 35, 27),
            intArrayOf(7, 10, 36, 24),
            intArrayOf(6, 10, 37, 9),
            intArrayOf(19, 11, 24, 7),
            intArrayOf(35, 13, 8, 5),
            intArrayOf(35, 13, 8, 5)
        ),
        artOnFrame = 1,
        swingEndFrame = 3,
        artOffFrame = 6,
        fullStrength = true
    )

    val ALL = gdxArrayOf(FLOOR, AIR, WALL)
}

internal fun megamanXSabreParams(x: MegamanX) = SabreParams(
    moves = objectMapOf(
        SabreState.FLOOR pairTo gdxArrayOf(MegamanXSabreMoves.FLOOR),
        SabreState.AIR pairTo gdxArrayOf(MegamanXSabreMoves.AIR),
        SabreState.WALL pairTo gdxArrayOf(MegamanXSabreMoves.WALL)
    ),
    atlas = TextureAsset.MEGAMAN_X,
    spriteSize = MegamanXValues.SPRITE_SIZE,
    artPxPerBodyFrame = 48,
    sound = SoundAsset.WHIP_V2_SOUND,
    canStart = {
        !x.isAnyBehaviorActive(BehaviorType.AIR_DASHING, BehaviorType.CLIMBING) && !x.kickLocked &&
            !(x.isBehaviorActive(BehaviorType.WALL_SLIDING) && !x.wallGrabTimer.isFinished())
    },
    chooseState = {
        when {
            x.isBehaviorActive(BehaviorType.WALL_SLIDING) -> SabreState.WALL
            x.bodyOverGround -> SabreState.FLOOR
            else -> SabreState.AIR
        }
    },
    spriteOffsetY = { 0f },
    endOnStateChange = false,
    landingMoves = ObjectMap()
)
