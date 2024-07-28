package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.ReactManProjectile
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class ReactMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    enum class ReactManState(val regionName: String) {
        DANCE("Dance"), JUMP("Jump"), RUN("Run"), STAND("Stand"), THROW("Throw")
    }

    companion object {
        const val TAG = "ReactMan"
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f
        private const val STAND_DUR = 0.5f
        private const val DANCE_DUR = 0.4f
        private const val RUN_DUR = 0.5f
        private const val RUN_SPEED = 6f
        private const val JUMP_IMPULSE = 10.5f
        private const val HORIZONTAL_SCALAR = 1f
        private const val VERTICAL_SCALAR = 1f
        private const val THROW_DELAY = 0.25f
        private const val PROJECTILE_SPEED = 10f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1),
        Fireball::class to dmgNeg(2),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val danceTimer = Timer(DANCE_DUR)
    private val runTimer = Timer(RUN_DUR)
    private val throwTimer = Timer(THROW_DELAY)

    private val projectilePosition: Vector2
        get() = body.getTopCenterPoint().add(0.1f * ConstVals.PPM * -facing.value, 0f)

    private var projectile: ReactManProjectile? = null
    private var jumped = false
    private var throwOnJump = true

    private lateinit var state: ReactManState

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            ReactManState.values().forEach {
                val region = atlas.findRegion("ReactMan/${it.regionName}")
                regions.put(it.regionName, region)
            }
            regions.put("Die", atlas.findRegion("ReactMan/Die"))
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        standTimer.reset()
        danceTimer.reset()
        throwTimer.reset()
        runTimer.reset()

        jumped = false
        throwOnJump = true

        state = ReactManState.DANCE
        facing = if (megaman.body.x <= body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        projectile?.kill()
        projectile = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            projectile?.body?.setCenter(projectilePosition)

            when (state) {
                ReactManState.DANCE -> {
                    body.physics.velocity.setZero()
                    danceTimer.update(delta)
                    if (danceTimer.isFinished()) {
                        danceTimer.reset()
                        state = ReactManState.STAND
                    }
                }

                ReactManState.STAND -> {
                    if (projectile == null) spawnProjectile()

                    body.physics.velocity.setZero()

                    if (megaman.body.x <= body.x) facing = Facing.LEFT
                    else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    standTimer.update(delta)
                    if (standTimer.isFinished()) {
                        standTimer.reset()
                        state = ReactManState.JUMP
                    }
                }

                ReactManState.JUMP -> {
                    if (!jumped) {
                        jump()
                        jumped = true
                        throwOnJump = !throwOnJump
                        return@add
                    }

                    if (megaman.body.x <= body.x) facing = Facing.LEFT
                    else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    if (throwOnJump) {
                        throwTimer.update(delta)
                        if (throwTimer.isJustFinished()) throwProjectile()
                    }

                    if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) {
                        jumped = false
                        throwTimer.reset()
                        state = if (throwOnJump) ReactManState.RUN else ReactManState.THROW
                    }
                }

                ReactManState.THROW -> {
                    if (projectile != null) throwProjectile()

                    body.physics.velocity.setZero()

                    throwTimer.update(delta)
                    if (throwTimer.isFinished()) {
                        throwTimer.reset()

                        if (megaman.body.x <= body.x) facing = Facing.LEFT
                        else if (megaman.body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                        state = ReactManState.RUN
                    }
                }

                ReactManState.RUN -> {
                    if ((megaman.body.x <= body.getMaxX() && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (megaman.body.getMaxX() >= body.x && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                    ) {
                        runTimer.reset()
                        state = ReactManState.STAND
                    }

                    runTimer.update(delta)
                    if (runTimer.isFinished()) {
                        runTimer.reset()
                        state = ReactManState.STAND
                        return@add
                    }

                    body.physics.velocity = Vector2(RUN_SPEED * ConstVals.PPM * facing.value, 0f)
                }
            }
        }
    }

    private fun jump() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPosition(),
            megaman.body.getPosition(),
            HORIZONTAL_SCALAR,
            JUMP_IMPULSE * ConstVals.PPM,
            VERTICAL_SCALAR
        )
        body.physics.velocity.set(impulse)
    }

    private fun spawnProjectile() {
        projectile = EntityFactories.fetch(
            EntityType.PROJECTILE, ProjectilesFactory.REACT_MAN_PROJECTILE
        )!! as ReactManProjectile

        game.engine.spawn(
            projectile!!, props(
                ConstKeys.POSITION to projectilePosition,
                ConstKeys.BIG to true,
                ConstKeys.OWNER to this,
                ConstKeys.ACTIVE to false
            )
        )
    }

    private fun throwProjectile() {
        val trajectory = megaman.body.getCenter().sub(body.getCenter()).nor().scl(PROJECTILE_SPEED * ConstVals.PPM)
        projectile!!.setTrajectory(trajectory)
        projectile!!.active = true
        projectile = null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.25f * ConstVals.PPM))
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }
        body.addFixture(damageableFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(
                0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        feetFixture.offsetFromBodyCenter.y = -0.625f * ConstVals.PPM
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }
        body.addFixture(feetFixture)

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        leftFixture.offsetFromBodyCenter.x = -0.625f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.rawShape.color = Color.YELLOW
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        leftFixture.offsetFromBodyCenter.x = 0.625f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            val size = if (defeated || state == ReactManState.DANCE) 1.5f else 2f
            _sprite.setSize(size * ConstVals.PPM)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "Die"
            else if (state == ReactManState.JUMP && throwOnJump && throwTimer.isFinished()) ReactManState.THROW.name
            else state.name
        }
        val animations = objectMapOf<String, IAnimation>(
            ReactManState.STAND.name to Animation(
                regions.get(ReactManState.STAND.regionName), 1, 3, gdxArrayOf(1f, 0.1f, 0.1f), true
            ),
            ReactManState.THROW.name to Animation(regions.get(ReactManState.THROW.regionName)),
            ReactManState.DANCE.name to Animation(regions.get(ReactManState.DANCE.regionName), 2, 2, 0.1f, false),
            ReactManState.RUN.name to Animation(regions.get(ReactManState.RUN.regionName), 2, 2, 0.1f, true),
            ReactManState.JUMP.name to Animation(regions.get(ReactManState.JUMP.regionName)),
            "Die" to Animation(regions.get("Die"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}