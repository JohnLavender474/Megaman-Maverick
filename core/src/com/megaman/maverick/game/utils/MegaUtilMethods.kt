package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals

object MegaUtilMethods {

  fun vector2FromString(input: String): Vector2 {
    val parsed = input.replace("\\s+", "").split(",")
    val x = parsed[0].toFloat()
    val y = parsed[1].toFloat()
    return Vector2(x, y)
  }

  fun getDefaultFontSize() = Math.round(ConstVals.PPM / 2f)
}
