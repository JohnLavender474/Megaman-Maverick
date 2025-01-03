package com.mega.game.engine.common.enums

enum class Facing(val value: Int) {
    LEFT(-1),
    RIGHT(1);

    fun opposite() = if (this == LEFT) RIGHT else LEFT
}
