package com.mega.game.engine.common.extensions


fun <T> Array<T>.toGdxArray(): com.badlogic.gdx.utils.Array<T> {
    val gdxArray = com.badlogic.gdx.utils.Array<T>()
    forEach { gdxArray.add(it) }
    return gdxArray
}
