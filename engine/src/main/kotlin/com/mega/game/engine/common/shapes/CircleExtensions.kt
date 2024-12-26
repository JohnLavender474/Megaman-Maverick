package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Circle

fun Circle.getBoundingRectangle(out: GameRectangle) = out.set(x - radius, y - radius, radius * 2f, radius * 2f)

fun Circle.toGameCircle(out: GameCircle) = out.setRadius(radius).setPosition(x, y)
