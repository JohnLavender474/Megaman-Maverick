package com.megaman.maverick.game.entities.enemies


import com.badlogic.gdx.graphics.g2d.TextureRegion
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
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class BunbyTank(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity,
    IDrawableShapesEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "BunbyTank"
        private const val MOVE_SPEED = 3f
        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.35f
        private const val AFTER_SHOOT_DELAY = 0.75f
        private const val GRAVITY = 0.15f
        private const val ROCKET_SPEED = 10f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
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
        addDebugShapeSupplier { shootScanner }
        addDebugShapeSupplier { turnAroundScanner }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        facing = when (direction) {
            Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
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
        val offset = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> offset.set(0.5f * facing.value, 0.25f)
            Direction.DOWN -> offset.set(-0.5f * facing.value, 0.25f)
            Direction.LEFT -> offset.set(0.275f, 0.5f * facing.value)
            Direction.RIGHT -> offset.set(0.275f, -0.5f * facing.value)
        }.scl(ConstVals.PPM.toFloat())

        val spawn = body.getCenter().add(offset)

        val trajectory = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> trajectory.set(ROCKET_SPEED * facing.value, 0f)
            Direction.DOWN -> trajectory.set(-ROCKET_SPEED * facing.value, 0f)
            Direction.LEFT -> trajectory.set(0f, ROCKET_SPEED * facing.value)
            Direction.RIGHT -> trajectory.set(0f, -ROCKET_SPEED * facing.value)
        }.scl(ConstVals.PPM.toFloat() * movementScalar)

        val rocket = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BUNBY_RED_ROCKET)!!
        rocket.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.FACING pairTo facing,
                ConstKeys.DIRECTION pairTo direction
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

            val size = (if (direction.isVertical()) Vector2(5f, 0.75f)
            else Vector2(0.75f, 5f)).scl(ConstVals.PPM.toFloat())
            shootScanner.setSize(size)
            turnAroundScanner.setSize(size)

            val position = when (direction) {
                Direction.UP -> if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
                Direction.DOWN -> if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
                Direction.LEFT -> if (isFacing(Facing.LEFT)) Position.TOP_CENTER else Position.BOTTOM_CENTER
                Direction.RIGHT -> if (isFacing(Facing.LEFT)) Position.BOTTOM_CENTER else Position.TOP_CENTER
            }
            val shootScannerPosition = body.getPositionPoint(position)
            shootScanner.positionOnPoint(shootScannerPosition, position)
            val turnAroundScannerPosition = body.getPositionPoint(position.opposite())
            turnAroundScanner.positionOnPoint(turnAroundScannerPosition, position.opposite())

            if (!megaman.dead) {
                if (megaman.body.getBounds().overlaps(shootScanner)) {
                    body.physics.velocity.setZero()
                    shootTimer.reset()
                    return@add
                } else if (megaman.body.getBounds().overlaps(turnAroundScanner)) swapFacing()
            }

            val velocity = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> velocity.set(MOVE_SPEED * facing.value, 0f)
                Direction.DOWN -> velocity.set(-MOVE_SPEED * facing.value, 0f)
                Direction.LEFT -> velocity.set(0f, MOVE_SPEED * facing.value)
                Direction.RIGHT -> velocity.set(0f, -MOVE_SPEED * facing.value)
            }.scl(ConstVals.PPM.toFloat() * movementScalar)
            body.physics.velocity.set(velocity)

            val gravity = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> velocity.set(0f, -GRAVITY)
                Direction.DOWN -> velocity.set(0f, GRAVITY)
                Direction.LEFT -> velocity.set(GRAVITY, 0f)
                Direction.RIGHT -> velocity.set(-GRAVITY, 0f)
            }.scl(ConstVals.PPM.toFloat() * movementScalar)
            body.physics.gravity.set(gravity)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val leftSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftSideFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftSideFixture)
        debugShapes.add { leftSideFixture }

        val rightSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightSideFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        body.addFixture(rightSideFixture)
        debugShapes.add { rightSideFixture }

        val leftFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        leftFootFixture.offsetFromBodyAttachment.set(-ConstVals.PPM.toFloat(), -0.75f * ConstVals.PPM)
        leftFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        leftFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> leftFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> leftFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(leftFootFixture)
        debugShapes.add { leftFootFixture }

        val rightFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        rightFootFixture.offsetFromBodyAttachment.set(ConstVals.PPM.toFloat(), -0.75f * ConstVals.PPM)
        rightFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        rightFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> rightFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> rightFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(rightFootFixture)
        debugShapes.add { rightFootFixture }

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
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)

            if (direction == Direction.LEFT)
                sprite.translateX(0.15f * ConstVals.PPM)
            else if (direction == Direction.RIGHT)
                sprite.translateX(-0.15f * ConstVals.PPM)

            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (!afterShootDelayTimer.isFinished()) "still"
            else if (!shootTimer.isFinished()) "shoot"
            else "roll"
        }
        animations = objectMapOf(
            "still" pairTo Animation(regions.get("still")),
            "shoot" pairTo Animation(regions.get("shoot"), 3, 1, 0.15f, false),
            "roll" pairTo Animation(regions.get("roll"), 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
