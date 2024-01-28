package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

open class SpriteBlock(game: MegamanMaverickGame) : Block(game) {

  companion object {
    const val TAG = "SpriteBlock"
  }

  lateinit var region: TextureRegion
  var hidden = false

  override fun init() {
    super.init()
    addComponent(defineSpritesComponent())
  }

  override fun spawn(spawnProps: Properties) {
    GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
    super.spawn(spawnProps)

    val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    body.set(bounds)

    val atlasKey = spawnProps.get(ConstKeys.ATLAS) as String
    val regionValue = spawnProps.get(ConstKeys.REGION) as String
    region = game.assMan.getTextureRegion(atlasKey, regionValue)
  }

  protected open fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()

    val spritesComponent = SpritesComponent(this, "block" to sprite)
    spritesComponent.putUpdateFunction("block") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setSize(body.width, body.height)
      val center = body.getCenter()
      _sprite.setCenter(center.x, center.y)
      _sprite.setRegion(region)
      _sprite.hidden = hidden
    }

    return spritesComponent
  }
}
