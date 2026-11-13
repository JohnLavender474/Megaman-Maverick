package com.megaman.maverick.game.entities.decorations

enum class BlockPieceColor {
    RED,
    GOLD,
    BROWN,
    PINK;

    val regionKey = "BlockPiece/${name.lowercase()}"
}
