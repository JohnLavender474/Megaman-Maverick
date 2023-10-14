package com.test.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.interfaces.Updatable
import com.engine.drawables.DrawingPriority
import com.engine.drawables.sprites.SpriteMatrix
import com.engine.drawables.sprites.SpriteMatrixParams
import com.test.game.ConstKeys

open class Background(model: TextureRegion, obj: RectangleMapObject) : Updatable {

  val spriteMatrix = SpriteMatrix(getParams(model, obj))

  companion object {
    private fun getParams(model: TextureRegion, obj: RectangleMapObject): SpriteMatrixParams {
      val props = obj.properties
      val rect = obj.rectangle
      return SpriteMatrixParams(
          model,
          props.get(ConstKeys.PRIORITY) as DrawingPriority,
          props.get(ConstKeys.WIDTH) as Float,
          props.get(ConstKeys.HEIGHT) as Float,
          props.get(ConstKeys.ROWS) as Int,
          props.get(ConstKeys.COLUMNS) as Int,
          rect.x,
          rect.y)
    }
  }

  /**
   * Optional method to update this background object.
   *
   * @param delta The time in seconds since the last update
   */
  override fun update(delta: Float) {}
}
