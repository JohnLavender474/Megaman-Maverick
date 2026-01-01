package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class CartinJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFreezableEntity, ISpritesEntity, IAnimatedEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "CartinJoe"

        private const val VEL_X = 5f
        private const val MAX_VEL_Y = 8f

        private const val GROUND_GRAV = -0.001f
        private const val GRAVITY = -0.25f

        private const val WAIT_DUR = 0.75f
        private const val SHOOT_DUR = 0.25f

        private const val BULLET_SPEED = 10f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    override lateinit var direction: Direction

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(
        this,
        onFrozen = {
            waitTimer.reset()
            shootTimer.setToEnd()
        }
    )

    val shooting: Boolean
        get() = !shootTimer.isFinished()

    private val waitTimer = Timer(WAIT_DUR)
    private val shootTimer = Timer(SHOOT_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("move", "shoot", "frozen").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT

        waitTimer.reset()
        shootTimer.setToEnd()

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        frozen = false
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()
        explode()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            freezeHandler.update(it)
            if (frozen) return@add

            shootTimer.update(it)
            if (shootTimer.isJustFinished()) waitTimer.reset()

            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                shoot()
                shootTimer.reset()
            }

            if (FacingUtils.isFacingBlock(this)) swapFacing()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.velocityClamp.y = MAX_VEL_Y * ConstVals.PPM
        body.setSize(1.25f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.85f, 0.65f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.275f * ConstVals.PPM
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyAttachment.y = 0.75f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val onBounce: () -> Unit = {
            swapFacing()
            GameLogger.debug(TAG, "onBounce: swap facing pairTo $facing")
        }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setRunnable(onBounce)
        leftFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -0.25f * ConstVals.PPM)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setRunnable(onBounce)
        rightFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -0.25f * ConstVals.PPM)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAV else GRAVITY
            body.physics.velocity.x = if (frozen) 0f else VEL_X * ConstVals.PPM * facing.value
        }

        body.forEachFixture { it.putProperty(ConstKeys.DEATH_LISTENER, false) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            if (frozen) "frozen"
            else if (shooting) "shoot"
            else "move"
        }
        val animations = objectMapOf<String, IAnimation>(
            "frozen" pairTo Animation(regions["frozen"]),
            "shoot" pairTo Animation(regions["shoot"], 2, 1, 0.1f, true),
            "move" pairTo Animation(regions["move"], 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.25f * facing.value, 0.5f)
            Direction.DOWN -> spawn.set(0.25f * facing.value, -0.5f)
            Direction.LEFT -> spawn.set(-0.4f, 0.25f * facing.value)
            Direction.RIGHT -> spawn.set(0.4f, 0.25f * facing.value)
        }.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class)
        when {
            direction.isVertical() -> trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
            else -> trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)
        }

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.DIRECTION pairTo direction
        )

        val entity = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        entity.spawn(props)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}
