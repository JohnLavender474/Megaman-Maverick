package com.megaman.maverick.game.drawables.sprites

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.drawables.IDrawable
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sprites.SpriteMatrix
import com.engine.drawables.sprites.SpriteMatrixParams
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.toProps

open class Background(model: TextureRegion, rectangle: Rectangle, props: Properties) :
    Updatable, IDrawable<Batch> {

  val spriteMatrix = SpriteMatrix(getParams(model, rectangle, props))

  companion object {
    private fun getParams(
        model: TextureRegion,
        rectangle: Rectangle,
        props: Properties
    ): SpriteMatrixParams {
      return SpriteMatrixParams(
          model,
          props.get(ConstKeys.PRIORITY) as DrawingPriority,
          props.get(ConstKeys.WIDTH) as Float,
          props.get(ConstKeys.HEIGHT) as Float,
          props.get(ConstKeys.ROWS) as Int,
          props.get(ConstKeys.COLUMNS) as Int,
          rectangle.x,
          rectangle.y)
    }
  }

  constructor(
      model: TextureRegion,
      mapObject: RectangleMapObject
  ) : this(model, mapObject.rectangle, mapObject.properties.toProps())

  override fun update(delta: Float) {}

  override fun draw(drawer: Batch) = spriteMatrix.draw(drawer)
}
