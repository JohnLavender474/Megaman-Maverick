package com.maverick.game.engine.input

fun interface InputSource {

    fun read(): InputFrame
}
