package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.math.Vector2
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.motion.RotatingLine
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import java.util.*

class LaserBeamer(game: MegamanMaverickGame) :
    GameEntity(game), ISpriteEntity, IBodyEntity, IDamager {

  companion object {
    private var region: TextureRegion? = null
    private val CONTACT_RADII = floatArrayOf(2f, 5f, 8f)

    private const val SPEED = 2f
    private const val RADIUS = 10f

    private const val CONTACT_TIME = .05f
    private const val SWITCH_TIME = 1f

    private const val MIN_DEGREES = 200f
    private const val MAX_DEGREES = 340f
    private const val INIT_DEGREES = 270f

    private const val THICKNESS = ConstVals.PPM / 32f
  }

  private val contactTimer = Timer(CONTACT_TIME)
  private val switchTimer = Timer(SWITCH_TIME).setToEnd()

  private lateinit var laser: GameLine
  private lateinit var contactGlow: GameCircle
  private lateinit var rotatingLine: RotatingLine
  private lateinit var laserFixture: Fixture
  private lateinit var damagerFixture: Fixture
  private lateinit var contacts: PriorityQueue<Vector2>

  private var clockwise = false
  private var contactIndex = 0

  override fun init() {
    if (region == null)
        region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "LaserBeamer")

    addComponent(defineBodyComponent())
    addComponent(defineSpritesCompoent())
    addComponent(defineUpdatablesComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val spawn = (spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getBottomCenterPoint()
    body.setPosition(spawn)
    rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, SPEED * ConstVals.PPM, INIT_DEGREES)

    val line = rotatingLine.line

    laserFixture.shape = line
    damagerFixture.shape = line

    line.thickness = THICKNESS
    line.shapeType = Filled
    line.color = RED
    contactGlow.color = WHITE
    addComponent(DrawableShapesComponent(this, line, contactGlow))

    contactTimer.reset()
    switchTimer.setToEnd()

    contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
      val d1 = p1.dst2(spawn)
      val d2 = p2.dst2(spawn)
      d1.compareTo(d2)
    }

    laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
  }

  override fun canDamage(damageable: IDamageable): Boolean {
    return true
  }

  override fun onDamageInflictedTo(damageable: IDamageable) {
    // do thing
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(ConstVals.PPM.toFloat())

    // laser fixture
    laserFixture = Fixture(GameLine(), FixtureType.LASER)
    laserFixture.offsetFromBodyCenter.y = ConstVals.PPM / 16f
    body.addFixture(laserFixture)

    // damager fixture
    damagerFixture = Fixture(GameLine(), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // shield fixture
    val shieldFixture =
        Fixture(
            GameRectangle().setSize(ConstVals.PPM.toFloat(), ConstVals.PPM * 0.85f),
            FixtureType.SHIELD)
    shieldFixture.offsetFromBodyCenter.y = ConstVals.PPM / 2f
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)

    val SpritesComponent = SpritesComponent(this, "laserBeamer" to sprite)
    SpritesComponent.putUpdateFunction("laserBeamer") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(rotatingLine.getMotionValue(), Position.BOTTOM_CENTER)
      _sprite.translateY(-.06f * ConstVals.PPM)
    }

    return SpritesComponent
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            contactTimer.update(it)
            if (contactTimer.isFinished()) {
              contactIndex++
              contactTimer.reset()
            }

            if (contactIndex > 2) contactIndex = 0

            val end = rotatingLine.getEndPoint()
            contactGlow = GameCircle()
            if (contacts.isNotEmpty()) {
              val closest = contacts.poll()
              end.set(closest)
              contactGlow.setPosition(closest.x, closest.y)
              contactGlow.setRadius(CONTACT_RADII[contactIndex])
            }
            contacts.clear()

            val origin = rotatingLine.getMotionValue()
            laser.setLocalPoints(origin, end)

            switchTimer.update(it)
            if (!switchTimer.isFinished()) return@UpdatablesComponent

            if (switchTimer.isJustFinished()) {
              clockwise = !clockwise
              var speed = SPEED * ConstVals.PPM
              if (clockwise) speed *= -1f
              rotatingLine.speed = speed
            }

            rotatingLine.update(it)

            if (clockwise && rotatingLine.degrees <= MIN_DEGREES) {
              rotatingLine.degrees = MIN_DEGREES
              switchTimer.reset()
            } else if (!clockwise && rotatingLine.degrees >= MAX_DEGREES) {
              rotatingLine.degrees = MAX_DEGREES
              switchTimer.reset()
            }
          })
}
