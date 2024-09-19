package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable


import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class BunbyTank(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity, IFaceable,
    IDirectionRotatable {

    companion object {
        const val TAG = "BunbyTank"
        private const val MOVE_SPEED = 3f
        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.35f
        private const val AFTER_SHOOT_DELAY = 0.75f
        private const val GRAVITY = 0.15f
        private const val ROCKET_SPEED = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(5),
        Fireball::class to dmgNeg(15),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }
    override lateinit var facing: Facing

    private val shootTimer = Timer(SHOOT_DUR, TimeMarkedRunnable(SHOOT_TIME) { shoot() })
    private val afterShootDelayTimer = Timer(AFTER_SHOOT_DELAY)
    private val shootScanner = GameRectangle()
    private val turnAroundScanner = GameRectangle()
    private lateinit var animations: ObjectMap<String, IAnimation>

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("still", atlas.findRegion("$TAG/Still"))
            regions.put("roll", atlas.findRegion("$TAG/Roll"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        shootScanner.color = Color.BLUE
        addDebugShapeSupplier { shootScanner }
        turnAroundScanner.color = Color.GREEN
        addDebugShapeSupplier { turnAroundScanner }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        facing = when (directionRotation!!) {
            Direction.UP -> if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (getMegaman().body.x < body.x) Facing.RIGHT else Facing.LEFT
            Direction.LEFT -> if (getMegaman().body.y < body.y) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (getMegaman().body.y < body.y) Facing.RIGHT else Facing.LEFT
        }

        shootTimer.setToEnd()
        afterShootDelayTimer.setToEnd()

        val shootFrameDuration =
            spawnProps.getOrDefault("${ConstKeys.SHOOT}_${ConstKeys.FRAME}", 0.15f, Float::class)
        animations.get("shoot").setFrameDuration(shootFrameDuration)

        val rollFrameDuration =
            spawnProps.getOrDefault("${ConstKeys.ROLL}_${ConstKeys.FRAME}", 0.1f, Float::class)
        animations.get("roll").setFrameDuration(rollFrameDuration)
    }

    private fun shoot() {
        val spawn = body.getCenter().add(
            (when (directionRotation!!) {
                Direction.UP -> Vector2(0.5f * facing.value, 0.175f)
                Direction.DOWN -> Vector2(-0.5f * facing.value, 0.175f)
                Direction.LEFT -> Vector2(0.175f, 0.5f * facing.value)
                Direction.RIGHT -> Vector2(0.175f, -0.5f * facing.value)
            }).scl(ConstVals.PPM.toFloat())
        )

        val trajectory = when (directionRotation!!) {
            Direction.UP -> Vector2(ROCKET_SPEED * facing.value, 0f)
            Direction.DOWN -> Vector2(-ROCKET_SPEED * facing.value, 0f)
            Direction.LEFT -> Vector2(0f, ROCKET_SPEED * facing.value)
            Direction.RIGHT -> Vector2(0f, -ROCKET_SPEED * facing.value)
        }.scl(ConstVals.PPM.toFloat() * movementScalar)

        val rocket = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BUNBY_RED_ROCKET)!!
        rocket.spawn(
            props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to spawn,
                ConstKeys.TRAJECTORY to trajectory,
                ConstKeys.FACING to facing,
                ConstKeys.DIRECTION to directionRotation
            )
        )

        requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!afterShootDelayTimer.isFinished()) {
                afterShootDelayTimer.update(delta)
                return@add
            }

            if (!shootTimer.isFinished()) {
                shootTimer.update(delta)
                if (shootTimer.isJustFinished()) afterShootDelayTimer.reset()
                return@add
            }

            val size = (if (directionRotation!!.isVertical()) Vector2(5f, 0.75f)
            else Vector2(0.75f, 5f)).scl(ConstVals.PPM.toFloat())
            shootScanner.setSize(size)
            turnAroundScanner.setSize(size)

            val position = when (directionRotation!!) {
                Direction.UP -> if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
                Direction.DOWN -> if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                Direction.LEFT -> if (isFacing(Facing.LEFT)) Position.TOP_CENTER else Position.BOTTOM_CENTER
                Direction.RIGHT -> if (isFacing(Facing.LEFT)) Position.BOTTOM_CENTER else Position.TOP_CENTER
            }
            val shootScannerPosition = body.getPositionPoint(position)
            shootScanner.positionOnPoint(shootScannerPosition, position)
            val turnAroundScannerPosition = body.getPositionPoint(position.opposite())
            turnAroundScanner.positionOnPoint(turnAroundScannerPosition, position.opposite())

            if (!getMegaman().dead) {
                if (getMegaman().body.overlaps(shootScanner as Rectangle)) {
                    body.physics.velocity.setZero()
                    shootTimer.reset()
                    return@add
                } else if (getMegaman().body.overlaps(turnAroundScanner as Rectangle)) swapFacing()
            }

            body.physics.velocity = (when (directionRotation!!) {
                Direction.UP -> Vector2(MOVE_SPEED * facing.value, 0f)
                Direction.DOWN -> Vector2(-MOVE_SPEED * facing.value, 0f)
                Direction.LEFT -> Vector2(0f, MOVE_SPEED * facing.value)
                Direction.RIGHT -> Vector2(0f, -MOVE_SPEED * facing.value)
            }).scl(ConstVals.PPM.toFloat() * movementScalar)

            body.physics.gravity = (when (directionRotation!!) {
                Direction.UP -> Vector2(0f, -GRAVITY)
                Direction.DOWN -> Vector2(0f, GRAVITY)
                Direction.LEFT -> Vector2(GRAVITY, 0f)
                Direction.RIGHT -> Vector2(-GRAVITY, 0f)
            }).scl(ConstVals.PPM.toFloat() * movementScalar)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, 1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val leftSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftSideFixture.offsetFromBodyCenter.x = -0.5f * ConstVals.PPM
        body.addFixture(leftSideFixture)
        leftSideFixture.rawShape.color = Color.BLUE
        debugShapes.add { leftSideFixture.getShape() }

        val rightSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightSideFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM
        body.addFixture(rightSideFixture)
        rightSideFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightSideFixture.getShape() }

        val leftFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        leftFootFixture.offsetFromBodyCenter = Vector2(-0.75f * ConstVals.PPM, -0.625f * ConstVals.PPM)
        leftFootFixture.setConsumer { _, fixture ->
            when (fixture.getFixtureType()) {
                FixtureType.BLOCK -> leftFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> leftFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(leftFootFixture)
        leftFootFixture.rawShape.color = Color.GREEN
        debugShapes.add { leftFootFixture.getShape() }

        val rightFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        rightFootFixture.offsetFromBodyCenter = Vector2(0.75f * ConstVals.PPM, -0.625f * ConstVals.PPM)
        rightFootFixture.setConsumer { _, fixture ->
            when (fixture.getFixtureType()) {
                FixtureType.BLOCK -> rightFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> rightFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(rightFootFixture)
        rightFootFixture.rawShape.color = Color.ORANGE
        debugShapes.add { rightFootFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            leftFootFixture.putProperty(ConstKeys.BLOCK, false)
            leftFootFixture.putProperty(ConstKeys.DEATH, false)
            rightFootFixture.putProperty(ConstKeys.BLOCK, false)
            rightFootFixture.putProperty(ConstKeys.DEATH, false)
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            if (isFacing(Facing.LEFT)) {
                if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                    leftFootFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                    leftFootFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                ) facing = Facing.RIGHT
            } else if (isFacing(Facing.RIGHT)) {
                if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ||
                    rightFootFixture.getProperty(ConstKeys.BLOCK, Boolean::class) != true ||
                    rightFootFixture.getProperty(ConstKeys.DEATH, Boolean::class) == true
                ) facing = Facing.LEFT
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation

            val position = when (directionRotation!!) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            if (directionRotation == Direction.LEFT)
                _sprite.translateX(0.15f * ConstVals.PPM)
            else if (directionRotation == Direction.RIGHT)
                _sprite.translateX(-0.15f * ConstVals.PPM)

            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!afterShootDelayTimer.isFinished()) "still"
            else if (!shootTimer.isFinished()) "shoot"
            else "roll"
        }
        animations = objectMapOf(
            "still" to Animation(regions.get("still")),
            "shoot" to Animation(regions.get("shoot"), 3, 1, 0.15f, false),
            "roll" to Animation(regions.get("roll"), 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}