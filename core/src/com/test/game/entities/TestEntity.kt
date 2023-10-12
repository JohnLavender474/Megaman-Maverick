package com.test.game.entities

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.IGame2D
import com.engine.audio.SoundComponent
import com.engine.behaviors.Behavior
import com.engine.behaviors.BehaviorsComponent
import com.engine.common.enums.Facing
import com.engine.common.extensions.containsAny
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.controller.ControllerComponent
import com.engine.controller.buttons.ButtonActuator
import com.engine.damage.Damageable
import com.engine.damage.DamageableComponent
import com.engine.drawables.shapes.DrawableShapeHandle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.entities.GameEntity
import com.engine.points.Points
import com.engine.points.PointsComponent
import com.engine.points.PointsHandle
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.test.game.ConstVals

class TestEntity(game: IGame2D) : GameEntity(game) {

  companion object Values {
    const val START_MAX_HEALTH = 14

    const val CLAMP_X = 15f
    const val CLAMP_Y = 25f

    const val RUN_SPEED = 5f
    const val RUN_IMPULSE = 50f
    const val ICE_RUN_IMPULSE = 15f
    const val WATER_RUN_SPEED = 2.25f

    const val SWIM_VEL_Y = 20f

    const val JUMP_VEL = 24f
    const val WATER_JUMP_VEL = 28f
    const val WATER_WALL_JUMP_VEL = 38f
    const val WALL_JUMP_VEL = 42f

    const val WALL_JUMP_HORIZONTAL = 10f
    const val WALL_JUMP_IMPETUS_TIME = .1f

    const val GROUND_GRAVITY = -.0015f
    const val GRAVITY = -.375f
    const val ICE_GRAVITY = -.5f
    const val WATER_GRAVITY = -.25f
    const val WATER_ICE_GRAVITY = -.4f

    const val AIR_DASH_VEL = 12f
    const val AIR_DASH_END_BUMP = 3f
    const val WATER_AIR_DASH_VEL = 6f
    const val WATER_AIR_DASH_END_BUMP = 2f
    const val MAX_AIR_DASH_TIME = .25f

    const val GROUND_SLIDE_VEL = 12f
    const val WATER_GROUND_SLIDE_VEL = 6f
    const val MAX_GROUND_SLIDE_TIME = .35f

    const val CLIMB_VEL = 2.5f

    const val DAMAGE_DURATION = .75f
    const val DAMAGE_RECOVERY_TIME = 1.5f
    const val DAMAGE_RECOVERY_FLASH_DURATION = .05f

    const val TIME_TO_HALFWAY_CHARGED = .5f
    const val TIME_TO_FULLY_CHARGED = 1.25f

    const val SHOOT_ANIM_TIME = .3f

    const val DMG_X = 8f
    const val DMG_Y = 5f
  }

  enum class AButtonTask {
    JUMP,
    SWIM,
    AIR_DASH
  }

  enum class Weapon {
    BUSTER,
    FLAME_TOSS
  }

  enum class Props {
    FACING,
    UNDER_WATER,
    RUNNING,
    A_BUTTON_TASK,
    WEAPON,
    UPSIDE_DOWN,
    TIMERS
  }

  init {
    // points
    val pointsComponent = pointsComponent()
    addComponent(pointsComponent)

    // damageable
    val damageableComponent = damageableComponent()
    addComponent(damageableComponent)

    // body
    val bodyComponent = bodyComponent()
    addComponent(bodyComponent)

    // behavior
    val behaviorsComponent = behaviorsComponent(bodyComponent.body)
    addComponent(behaviorsComponent)

    // controller
    addComponent(controllerComponent(bodyComponent.body))

    // sprite
    addComponent(spriteComponent())
  }

  override fun spawn(spawnProps: Properties) {
    val spawn = spawnProps.get("spawn", Vector2::class)!!
    val body = getComponent(BodyComponent::class)!!.body
    body.setPosition(spawn)

    putProperty(Props.FACING, Facing.RIGHT)
    putProperty(Props.RUNNING, false)
    putProperty(Props.A_BUTTON_TASK, AButtonTask.JUMP)
    putProperty(Props.WEAPON, Weapon.BUSTER)
    putProperty(Props.UPSIDE_DOWN, false)
    putProperty(Props.TIMERS, ObjectMap<String, Timer>())
  }

  override fun onDestroy() {
    super.onDestroy()
    getComponent(BodyComponent::class)!!.body.physics.velocity.setZero()
  }

  /* ---------------------------------------------------------------------- */
  // Sprite Component

  private fun spriteComponent(): SpriteComponent {
    val sprite = GameSprite(4)
    sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
    sprite.setOriginCenter()

    val spriteComponent = SpriteComponent(this, "player" to sprite)
    spriteComponent.putUpdateFunction("player") { _, player ->
      player.let {
        val flipX = getProperty(Props.FACING) == "left"
        val flipY = getProperty(Props.UPSIDE_DOWN) == true

        it.setFlip(flipX, flipY)
      }
    }

    return spriteComponent
  }

  /* ---------------------------------------------------------------------- */
  // Updatables Component

  private fun updatablesComponent() = UpdatablesComponent(this, { delta -> })

  /* ---------------------------------------------------------------------- */
  // Damageable Component

  private fun damageableComponent(): DamageableComponent {
    val damageable = Damageable { damager ->
      // TODO: take damage
      false
    }

    return DamageableComponent(
        this, damageable, Timer(DAMAGE_DURATION), Timer(DAMAGE_RECOVERY_TIME))
  }

  /* ---------------------------------------------------------------------- */
  // Points Component

  private fun pointsComponent(): PointsComponent {
    // health
    val health =
        PointsHandle(
            Points(0, 10, 10),
            listener = { points ->
              if (points.current == 0) {
                dead = true
              }
            },
            onReset = { handle -> handle.points.setToMax() })

    return PointsComponent(this, "health" to health)
  }

  /* ---------------------------------------------------------------------- */
  // Behaviors Component

  private fun behaviorsComponent(body: Body): BehaviorsComponent {
    val behaviorsComponent = BehaviorsComponent(this)

    // jump
    val jump =
        Behavior(
            evaluate = { _ ->
              /*
              if (behaviorsComponent.isAnyBehaviorActive("swim", "climb")) {
                return@Behavior false
              }
              */

              val labels = body.getProperty("labels") as ObjectSet<*>
              /*
              if (labels.containsAny("head_touch_block")) {
                return@Behavior false
              }
               */

              val upsideDown = getProperty("upside_down") == true
              val controllerPoller = game.controllerPoller

              /*
              if (controllerPoller.isButtonPressed(if (upsideDown) "up" else "down")) {
                return@Behavior false
              }
               */

              if (behaviorsComponent.isBehaviorActive("jump")) {
                val velocity = body.physics.velocity
                return@Behavior if (upsideDown) velocity.y <= 0f else velocity.y >= 0f
              }

              val aButtonTask = getProperty("a_button_task") as AButtonTask

              return@Behavior aButtonTask == AButtonTask.JUMP &&
                  controllerPoller.isButtonPressed("A") &&
                  labels.containsAny("feet_on_ground", "wall_sliding")
            },
            init = {
              val v = Vector2()

              v.x =
                  if (behaviorsComponent.isBehaviorActive("wall_sliding")) {
                    var x = WALL_JUMP_HORIZONTAL * ConstVals.PPM
                    if (getProperty("facing") == "left") x *= -1
                    x
                  } else {
                    body.physics.velocity.x
                  }

              v.y =
                  if (getProperty("under_water") == true) {
                    if (behaviorsComponent.isBehaviorActive("wall_sliding"))
                        WATER_WALL_JUMP_VEL * ConstVals.PPM
                    else WATER_JUMP_VEL * ConstVals.PPM
                  } else {
                    if (behaviorsComponent.isBehaviorActive("wall_sliding"))
                        WALL_JUMP_VEL * ConstVals.PPM
                    else JUMP_VEL * ConstVals.PPM
                  }

              body.physics.velocity.set(v)

              if (behaviorsComponent.isBehaviorActive("wall_sliding")) {
                getComponent(SoundComponent::class)?.requestToPlaySound("wall_jump", false)
              }
            },
            end = { body.physics.velocity.y = 0f })

    behaviorsComponent.addBehavior("jump", jump)
    return behaviorsComponent
  }

  /* ---------------------------------------------------------------------- */
  // Controller Component

  private fun controllerComponent(body: Body): ControllerComponent {
    val left =
        ButtonActuator(
            onPressContinued = { poller, delta ->
              if (poller.isButtonPressed("right")) {
                return@ButtonActuator
              }

              putProperty("running", true)

              val threshold = RUN_SPEED * ConstVals.PPM
              if (body.physics.velocity.x > -threshold) {
                body.physics.velocity.x -= RUN_IMPULSE * delta * ConstVals.PPM
              }
            },
            onJustReleased = { poller ->
              if (!poller.isButtonPressed("right")) {
                putProperty("running", false)
              }
            })

    val right =
        ButtonActuator(
            onPressContinued = { poller, delta ->
              if (poller.isButtonPressed("left")) {
                return@ButtonActuator
              }

              putProperty("running", true)

              val threshold = RUN_SPEED * ConstVals.PPM
              if (body.physics.velocity.x < threshold) {
                body.physics.velocity.x += RUN_IMPULSE * delta * ConstVals.PPM
              }
            },
            onJustReleased = { poller ->
              if (!poller.isButtonPressed("left")) {
                putProperty("running", false)
              }
            })

    return ControllerComponent(this, "left" to left, "right" to right)
  }

  /* ---------------------------------------------------------------------- */
  // Body Component

  private fun bodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.width = .75f * ConstVals.PPM
    body.physics.takeFrictionFromOthers = true
    body.physics.velocityClamp.set(15f * ConstVals.PPM, 25f * ConstVals.PPM)

    val shapes = Array<DrawableShapeHandle>()

    // player fixture
    val playerFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), "player")
    body.fixtures.put("player", playerFixture)
    shapes.add(DrawableShapeHandle(playerFixture.shape, ShapeRenderer.ShapeType.Line))

    // TODO: other fixtures

    return BodyComponent(this, body)
  }
}
