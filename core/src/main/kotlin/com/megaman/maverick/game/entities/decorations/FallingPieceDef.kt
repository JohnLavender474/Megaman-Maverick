package com.megaman.maverick.game.entities.decorations

import com.megaman.maverick.game.entities.utils.TickFrames

enum class FallingPieceDef(
    val gravity: Float,
    val fallClamp: Float?,
    val frictionX: Boolean,
    val jitter: Float,
    val size: FallingPieceSize,
    val spin: FallingPieceSpin?,
    val blink: Float?
) {
    DEBRIS(
        gravity = -35f,
        fallClamp = 12f,
        frictionX = false,
        jitter = 0.2f,
        size = FallingPieceSize.RegionScale(TickFrames.SCALE),
        spin = null,
        blink = 0.033f
    ),
    BLOCK(
        gravity = -37.5f,
        fallClamp = null,
        frictionX = true,
        jitter = 0f,
        size = FallingPieceSize.Tiles(1f),
        spin = FallingPieceSpin(step = 45f, delay = 0.1f),
        blink = null
    ),
    WOOD_CRATE(
        gravity = -37.5f,
        fallClamp = null,
        frictionX = true,
        jitter = 0f,
        size = FallingPieceSize.Tiles(0.1875f),
        spin = FallingPieceSpin(step = 90f, delay = 0.05f),
        blink = null
    )
}

sealed interface FallingPieceSize {

    data class Tiles(val size: Float) : FallingPieceSize

    data class RegionScale(val scale: Float) : FallingPieceSize
}

data class FallingPieceSpin(val step: Float, val delay: Float)
