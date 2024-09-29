package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*

class LaserBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IHazard, ISpritesEntity, IBodyEntity,
    IDrawableShapesEntity, IDamager {

    companion object {
        const val TAG = "LaserBeamer"
        private var region: TextureRegion? = null
        private val CONTACT_RADII = floatArrayOf(2f, 5f, 8f)
        private const val SPEED = 2f
        private const val RADIUS = 10f
        private const val CONTACT_TIME = 0.05f
        private const val SWITCH_TIME = 1f
        private const val MIN_DEGREES = 200f
        private const val MAX_DEGREES = 340f
        private const val INIT_DEGREES = 270f
        private const val THICKNESS = ConstVals.PPM / 32f
    }

    private val contactTimer = Timer(CONTACT_TIME)
    private val switchTimer = Timer(SWITCH_TIME)

    private lateinit var laser: GameLine
    private lateinit var contactGlow: GameCircle
    private lateinit var rotatingLine: RotatingLine
    private lateinit var laserFixture: Fixture
    private lateinit var contacts: PriorityQueue<Vector2>

    private var clockwise = false
    private var contactIndex = 0

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "LaserBeamer")

        laser = GameLine()
        laser.thickness = THICKNESS
        laser.shapeType = Filled
        laser.color = RED

        contactGlow = GameCircle()
        contactGlow.color = WHITE
        contactGlow.shapeType = Filled

        addComponent(
            DrawableShapesComponent(prodShapeSuppliers = gdxArrayOf({ contactGlow }))
        )
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineUpdatablesComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = (spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getCenter()
        body.setCenter(spawn)

        rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, SPEED * ConstVals.PPM, INIT_DEGREES)
        contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
            val d1 = p1.dst2(rotatingLine.getOrigin())
            val d2 = p2.dst2(rotatingLine.getOrigin())
            d1.compareTo(d2)
        }
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)

        contactTimer.reset()
        switchTimer.setToEnd()
    }

    override fun getTag() = TAG

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        laserFixture = Fixture(body, FixtureType.LASER, GameLine())
        laserFixture.attachedToBody = false
        laserFixture.rawShape = laser
        body.addFixture(laserFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameLine())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        addProdShapeSupplier { damagerFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(ConstVals.PPM.toFloat(), ConstVals.PPM * 0.85f)
        )
        shieldFixture.offsetFromBodyCenter.y = ConstVals.PPM / 2f
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            laserFixture.putProperty(ConstKeys.LINE, rotatingLine.line)
            contacts.clear()
        })

        body.postProcess.put(ConstKeys.DEFAULT, Updatable {
            val end = if (contacts.isEmpty()) rotatingLine.getEndPoint()
            else contacts.first()
            laser.setFirstLocalPoint(rotatingLine.getOrigin())
            laser.setSecondLocalPoint(end)

            GameLogger.debug(TAG, "[postProcess] Laser = $laser. End point = $end. Contacts = $contacts")

            laserFixture.rawShape = laser
            damagerFixture.rawShape = laser

            contactGlow.setCenter(end.x, end.y)
        })

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(rotatingLine.getOrigin(), Position.BOTTOM_CENTER)
            _sprite.translateY(-0.06f * ConstVals.PPM)
        }
        return spritesComponent
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        contactTimer.update(it)
        if (contactTimer.isFinished()) {
            contactIndex++
            contactTimer.reset()
        }
        if (contactIndex > 2) contactIndex = 0
        contactGlow.setRadius(CONTACT_RADII[contactIndex])

        /*
        val end =
            if (contacts.isEmpty()) rotatingLine.getEndPoint()
            else contacts.first()
        contacts.clear()
        laser.setFirstLocalPoint(rotatingLine.getOrigin())
        laser.setSecondLocalPoint(end)
         */

        switchTimer.update(it)
        if (!switchTimer.isFinished()) return@UpdatablesComponent

        if (switchTimer.isJustFinished()) {
            clockwise = !clockwise
            var speed = SPEED * ConstVals.PPM
            if (clockwise) speed *= -1f
            rotatingLine.speed = speed
            GameLogger.debug(TAG, "update: switchTimer.isJustFinished(), clockwise = $clockwise")
        }

        rotatingLine.update(it)

        if (clockwise && rotatingLine.degrees <= MIN_DEGREES) {
            rotatingLine.degrees = MIN_DEGREES
            switchTimer.reset()
            GameLogger.debug(TAG, "update: clockwise && rotatingLine.degrees <= MIN_DEGREES")
        } else if (!clockwise && rotatingLine.degrees >= MAX_DEGREES) {
            rotatingLine.degrees = MAX_DEGREES
            switchTimer.reset()
            GameLogger.debug(TAG, "update: !clockwise && rotatingLine.degrees >= MAX_DEGREES")
        }
    })
}
