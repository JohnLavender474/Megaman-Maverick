package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getEndPoint
import com.megaman.maverick.game.utils.extensions.getOrigin
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*

class LaserBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity, IDrawableShapesEntity,
    IDamager {

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
    }

    private val contactTimer = Timer(CONTACT_TIME)
    private val switchTimer = Timer(SWITCH_TIME)

    private val laser = GameLine()
    private val contactGlow = GameCircle()

    private lateinit var rotatingLine: RotatingLine

    private lateinit var laserFixture: Fixture
    private lateinit var damagerFixture: Fixture

    private val contacts = PriorityQueue { p1: Vector2, p2: Vector2 ->
        val origin = rotatingLine.getOrigin()
        val d1 = p1.dst2(origin)
        val d2 = p2.dst2(origin)
        d1.compareTo(d2)
    }

    private var clockwise = false
    private var contactIndex = 0

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)

        addComponent(MotionComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())

        contactGlow.drawingColor = Color.WHITE
        contactGlow.drawingShapeType = ShapeType.Filled

        val prodShapeSuppliers: Array<() -> IDrawableShape?> = gdxArrayOf({ contactGlow }, { damagerFixture })
        addComponent(DrawableShapesComponent(prodShapeSuppliers = prodShapeSuppliers, debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        rotatingLine = RotatingLine(spawn, RADIUS * ConstVals.PPM, SPEED * ConstVals.PPM, INIT_DEGREES)

        contactTimer.reset()
        switchTimer.reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        contacts.clear()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        laserFixture = Fixture(body, FixtureType.LASER, laser)
        laserFixture.putProperty(ConstKeys.COLLECTION, contacts)
        laserFixture.attachedToBody = false
        body.addFixture(laserFixture)

        damagerFixture = Fixture(body, FixtureType.DAMAGER, laser)
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.85f * ConstVals.PPM)
        )
        shieldFixture.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            laserFixture.putProperty(ConstKeys.LINE, rotatingLine.line)

            contacts.clear()
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            val origin = rotatingLine.getOrigin()
            laser.setFirstLocalPoint(origin)

            val end = if (contacts.isEmpty()) rotatingLine.getEndPoint() else contacts.peek()
            laser.setSecondLocalPoint(end)

            laserFixture.setShape(laser)
            damagerFixture.setShape(laser)

            contactGlow.setCenter(end)
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!).also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setPosition(rotatingLine.getOrigin(), Position.BOTTOM_CENTER)
            sprite.translateY(-0.1f * ConstVals.PPM)
        }
        .build()

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        contactTimer.update(it)
        if (contactTimer.isFinished()) {
            contactIndex++
            contactTimer.reset()
        }
        if (contactIndex > 2) contactIndex = 0
        contactGlow.setRadius(CONTACT_RADII[contactIndex])

        switchTimer.update(it)
        if (!switchTimer.isFinished()) return@UpdatablesComponent

        if (switchTimer.isJustFinished()) {
            clockwise = !clockwise
            var speed = SPEED * ConstVals.PPM
            if (clockwise) speed *= -1f
            rotatingLine.speed = speed

            // GameLogger.debug(TAG, "update(): switchTimer.isJustFinished(), clockwise=$clockwise")
        }

        rotatingLine.update(it)

        if (clockwise && rotatingLine.degrees <= MIN_DEGREES) {
            rotatingLine.degrees = MIN_DEGREES
            switchTimer.reset()
            // GameLogger.debug(TAG, "update(): clockwise && rotatingLine.degrees <= MIN_DEGREES")
        } else if (!clockwise && rotatingLine.degrees >= MAX_DEGREES) {
            rotatingLine.degrees = MAX_DEGREES
            switchTimer.reset()
            // GameLogger.debug(TAG, "update(): !clockwise && rotatingLine.degrees >= MAX_DEGREES")
        }

        // GameLogger.debug(TAG, "update(): rawLine=${rotatingLine.line}, gameLine=$laser")
    })

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
