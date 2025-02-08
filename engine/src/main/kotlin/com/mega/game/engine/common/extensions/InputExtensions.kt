package com.mega.game.engine.common.extensions

import com.badlogic.gdx.Input

fun Input.isAnyKeyJustPressed(vararg keys: Int) = keys.any { key -> isKeyJustPressed(key) }
