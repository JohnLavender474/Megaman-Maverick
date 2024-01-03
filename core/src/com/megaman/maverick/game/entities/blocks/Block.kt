package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.engine.IGame2D
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyLabel
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.addBodyLabel
import com.megaman.maverick.game.world.setEntity

open class Block(game: IGame2D) : GameEntity(game), IBodyEntity {

  companion object {
    const val TAG = "Block"
    const val STANDARD_FRIC_X = .035f
    const val STANDARD_FRIC_Y = 0f
  }

  override fun init() {
    GameLogger.debug(TAG, "init(): Initializing Block entity.")
    val body = Body(BodyType.STATIC)
    val bodyFixture = Fixture(GameRectangle(), FixtureType.BLOCK)
    body.addFixture(bodyFixture)
    body.fixtures.forEach { (_, fixture) -> fixture.setEntity(this) }
    addComponent(BodyComponent(this, body))
    addComponent(UpdatablesComponent(this, { (bodyFixture.shape as GameRectangle).set(body) }))
    addComponent(
        DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val persist =
        if (spawnProps.containsKey(ConstKeys.PERSIST)) spawnProps.get(ConstKeys.PERSIST) as Boolean
        else false
    if (persist) removeComponent(CullablesComponent::class)
    else addComponent(CullablesComponent(this, getGameCameraCullingLogic(this)))

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
    if (bounds != null) body.set(bounds)

    val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
    if (position != null) body.setPosition(position)
  }
}
