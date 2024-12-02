package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball

import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class UFOhNoBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "UFOhNoBot"
        private const val RISE_VEL = 8f
        private const val X_VEL_WITH_BOMB = 5f
        private const val X_VEL_NO_BOMB = 10f
        private const val DROP_DURATION = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val dropDurationTimer = Timer(DROP_DURATION)
    private val triggers = Array<GameRectangle>()

    private lateinit var start: Vector2
    private lateinit var target: Vector2

    private var waiting = true
    private var dropped = false
    private var rising = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("no_bomb", atlas.findRegion("$TAG/no_bomb"))
            regions.put("with_bomb", atlas.findRegion("$TAG/with_bomb"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        waiting = spawnProps.getOrDefault(ConstKeys.WAIT, true, Boolean::class)

        dropped = false
        rising = false

        if (waiting) {
            spawnProps.getAllMatching { it.toString().contains(ConstKeys.TRIGGER) }.forEach {
                val trigger = (it.second as RectangleMapObject).rectangle.toGameRectangle(false)
                triggers.add(trigger)
            }
            start = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter(false)
            target = spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter(false)

            dropDurationTimer.reset()

            body.forEachFixture { it.setActive(false) }
        } else setToHover()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        triggers.clear()
    }

    private fun isMegamanUnderMe() = megaman().body.getMaxY() <= body.getY() &&
        megaman().body.getCenter().x >= body.getX() && megaman().body.getCenter().x <= body.getMaxX()

    private fun moveX() {
        val xVel = (if (dropped) X_VEL_NO_BOMB else X_VEL_WITH_BOMB) * ConstVals.PPM * facing.value
        body.physics.velocity.set(xVel, 0f)
    }

    private fun dropBomb() {
        val bomb = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.UFO_BOMB)!!
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER).sub(0f, 0.6f * ConstVals.PPM)
        bomb.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.OWNER pairTo this))
    }

    private fun setToHover() {
        facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        moveX()
    }

    override fun swapFacing() {
        super.swapFacing()
        GameLogger.debug(TAG, "swapFacing(): new facing = $facing")
        moveX()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (waiting) {
                if (triggers.any { trigger -> megaman().body.getBounds().overlaps(trigger) }) {
                    waiting = false
                    rising = true

                    body.setCenter(start)

                    val trajectory = target.cpy().sub(start).nor().scl(RISE_VEL * ConstVals.PPM)
                    body.physics.velocity.set(trajectory)

                    body.forEachFixture { it.setActive(true) }

                    facing = if (trajectory.x == 0f) {
                        if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                    } else if (trajectory.x < 0f) Facing.LEFT else Facing.RIGHT
                } else return@add
            }

            if (rising) {
                if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                    rising = false
                    setToHover()
                } else return@add
            }

            if (!dropped && isMegamanUnderMe()) {
                dropBomb()
                dropped = true
                dropDurationTimer.reset()
                body.physics.velocity.setZero()
            }

            if (!dropDurationTimer.isFinished()) {
                dropDurationTimer.update(delta)
                if (dropDurationTimer.isFinished()) moveX()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.RED
        debugShapes.add { bodyFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.15f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.5f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val leftSideFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -0.5f * ConstVals.PPM
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftSideFixture }

        val rightSideFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = 0.5f * ConstVals.PPM
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightSideFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (!waiting && !rising &&
                ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)))
            ) swapFacing()
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getPositionPoint(Position.TOP_CENTER), Position.TOP_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink || waiting
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (dropped) "no_bomb" else "with_bomb" }
        val animations = objectMapOf<String, IAnimation>(
            "no_bomb" pairTo Animation(regions["no_bomb"], 2, 2, 0.1f, true),
            "with_bomb" pairTo Animation(regions["with_bomb"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
