package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.engine.IGame2D
import com.engine.common.objects.Properties
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.PhysicsData
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.world.BodyLabel
import com.megaman.maverick.game.world.addBodyLabel

/** A block is a static entity that can be collided with. */
open class Block(game: IGame2D) : GameEntity(game), IBodyEntity {

  companion object {
    const val TAG = "Block"
    const val STANDARD_FRIC_X = .035f
    const val STANDARD_FRIC_Y = 0f
  }

  override fun init() {
    Gdx.app.debug(TAG, "init(): Initializing Block entity.")

    val physicsData = PhysicsData()
    physicsData.frictionToApply.x = STANDARD_FRIC_X
    addComponent(BodyComponent(this, Body(BodyType.STATIC, physicsData)))
  }

  override fun spawn(spawnProps: Properties) {
    Gdx.app.debug(TAG, "spawn(): Spawning Block entity with properties: $spawnProps")

    super.spawn(spawnProps)

    if (properties.containsKey(ConstKeys.FRICTION_X))
        body.physics.frictionToApply.x = properties.get(ConstKeys.FRICTION_X) as Float
    else body.physics.frictionToApply.x = STANDARD_FRIC_X

    if (properties.containsKey(ConstKeys.FRICTION_Y))
        body.physics.frictionToApply.y = properties.get(ConstKeys.FRICTION_Y) as Float
    else body.physics.frictionToApply.y = STANDARD_FRIC_Y

    if (properties.containsKey(ConstKeys.GRAVITY_ON))
        body.physics.gravityOn = properties.get(ConstKeys.GRAVITY_ON) as Boolean

    if (properties.containsKey(ConstKeys.RESIST_ON))
        body.physics.takeFrictionFromOthers = properties.get(ConstKeys.RESIST_ON) as Boolean

    if (properties.containsKey(ConstKeys.BODY_LABELS)) {
      val labels = (properties.get(ConstKeys.BODY_LABELS) as String).replace("\\s+", "").split(",")
      for (label in labels) {
        val bodyLabel = BodyLabel.valueOf(label)
        body.addBodyLabel(bodyLabel)
      }
    }

    val bounds = spawnProps.get(ConstKeys.BOUNDS, Rectangle::class)
    if (bounds != null) {
      body.set(bounds)
      return
    }

    val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
    if (position != null) body.setPosition(position)
  }
}
