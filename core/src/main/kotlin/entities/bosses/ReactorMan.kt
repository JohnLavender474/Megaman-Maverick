package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
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
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.ReactManProjectile
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

class ReactorMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    enum class ReactManState(val regionName: String) {
        JUMP("Jump"),
        RUN("Run"),
        STAND("Stand"),
        THROW("Throw")
    }

    companion object {
        const val TAG = "ReactMan"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f

        private const val STAND_MAX_DUR = 0.75f
        private const val STAND_MIN_DUR = 0.25f

        private const val DANCE_DUR = 0.4f

        private const val RUN_DUR = 0.5f
        private const val RUN_MIN_SPEED = 8f
        private const val RUN_MAX_SPEED = 14f

        private const val JUMP_IMPULSE = 16f

        private const val THROW_DELAY = 0.25f
        private const val PROJECTILE_MIN_SPEED = 8f
        private const val PROJECTILE_MAX_SPEED = 14f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_MAX_DUR)
    private val danceTimer = Timer(DANCE_DUR)
    private val runTimer = Timer(RUN_DUR)
    private val throwTimer = Timer(THROW_DELAY)

    private val projectilePosition: Vector2
        get() = body.getTopCenterPoint().add(
            0.15f * ConstVals.PPM * -facing.value,
            (if (body.isSensing(BodySense.FEET_ON_GROUND)) 0.175f else 0.375f) * ConstVals.PPM
        )

    private var projectile: ReactManProjectile? = null
    private var jumped = false
    private var throwOnJump = true
    private lateinit var currentState: ReactManState

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            ReactManState.entries.forEach {
                val region = atlas.findRegion("$TAG/${it.regionName}")
                regions.put(it.regionName, region)
            }
            regions.put("Die", atlas.findRegion("$TAG/Die"))
            regions.put("Defeated", atlas.findRegion("$TAG/Defeated"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        standTimer.reset()
        danceTimer.reset()
        throwTimer.reset()
        runTimer.reset()

        jumped = false
        throwOnJump = true

        currentState = ReactManState.STAND
        facing = if (getMegaman().body.x <= body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDestroy() {
        super.onDestroy()
        projectile?.destroy()
        projectile = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                return@add
            }
            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            projectile?.body?.setCenter(projectilePosition)

            when (currentState) {
                ReactManState.STAND -> {
                    if (projectile == null) spawnProjectile()

                    if (getMegaman().body.x <= body.x) facing = Facing.LEFT
                    else if (getMegaman().body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        body.physics.velocity.setZero()

                        standTimer.update(delta)
                        if (standTimer.isFinished()) {
                            standTimer.resetDuration(getStandDur())
                            currentState = ReactManState.JUMP
                        }
                    }
                }

                ReactManState.JUMP -> {
                    if (!jumped) {
                        jump()
                        jumped = true
                        throwOnJump = !throwOnJump
                        return@add
                    }

                    if (getMegaman().body.x <= body.x) facing = Facing.LEFT
                    else if (getMegaman().body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                    if (throwOnJump) {
                        throwTimer.update(delta)
                        if (throwTimer.isJustFinished()) throwProjectile()
                    }

                    if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) {
                        jumped = false
                        throwTimer.reset()
                        currentState = if (throwOnJump) ReactManState.RUN else ReactManState.THROW
                    }
                }

                ReactManState.THROW -> {
                    if (projectile != null) throwProjectile()

                    body.physics.velocity.setZero()

                    throwTimer.update(delta)
                    if (throwTimer.isFinished()) {
                        throwTimer.reset()

                        if (getMegaman().body.x <= body.x) facing = Facing.LEFT
                        else if (getMegaman().body.getMaxX() >= body.getMaxX()) facing = Facing.RIGHT

                        currentState = ReactManState.RUN
                    }
                }

                ReactManState.RUN -> {
                    runTimer.update(delta)
                    if (runTimer.isFinished() || shouldStopRunning()) {
                        runTimer.reset()
                        currentState = ReactManState.STAND
                        return@add
                    }

                    body.physics.velocity = Vector2(getRunSpeed() * ConstVals.PPM * facing.value, 0f)
                }
            }
        }
    }

    private fun jump() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPosition(),
            getMegaman().body.getPosition(),
            JUMP_IMPULSE * ConstVals.PPM
        )
        body.physics.velocity.set(impulse)
    }

    private fun spawnProjectile() {
        projectile = EntityFactories.fetch(
            EntityType.PROJECTILE,
            ProjectilesFactory.REACT_MAN_PROJECTILE
        )!! as ReactManProjectile

        projectile!!.spawn(
            props(
                ConstKeys.POSITION pairTo projectilePosition,
                ConstKeys.BIG pairTo true,
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false
            )
        )
    }

    private fun throwProjectile() {
        val trajectory =
            getMegaman().body.getCenter().sub(body.getCenter()).nor().scl(getProjectileSpeed() * ConstVals.PPM)
        projectile!!.setTrajectory(trajectory)
        projectile!!.active = true
        projectile = null
    }

    private fun shouldStopRunning() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

    private fun getStandDur() = STAND_MIN_DUR + (STAND_MAX_DUR - STAND_MIN_DUR) * getHealthRatio()

    private fun getRunSpeed() = RUN_MAX_SPEED - (RUN_MAX_SPEED - RUN_MIN_SPEED) * getHealthRatio()

    private fun getProjectileSpeed() =
        PROJECTILE_MAX_SPEED - (PROJECTILE_MAX_SPEED - PROJECTILE_MIN_SPEED) * getHealthRatio()

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.35f * ConstVals.PPM)

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

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(
                0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        headFixture.offsetFromBodyCenter.y = 0.675f * ConstVals.PPM
        headFixture.rawShape.color = Color.ORANGE
        debugShapes.add { headFixture.getShape() }
        body.addFixture(headFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(
                0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        feetFixture.offsetFromBodyCenter.y = -0.675f * ConstVals.PPM
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
        rightFixture.offsetFromBodyCenter.x = 0.625f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "Defeated"
            else if (currentState == ReactManState.STAND) {
                if (body.isSensing(BodySense.FEET_ON_GROUND)) ReactManState.STAND.name else ReactManState.JUMP.name
            } else if (currentState == ReactManState.JUMP && throwOnJump && throwTimer.isFinished())
                ReactManState.THROW.name
            else currentState.name
        }
        val animations = objectMapOf<String, IAnimation>(
            ReactManState.STAND.name pairTo Animation(
                regions.get(ReactManState.STAND.regionName), 1, 3, gdxArrayOf(1f, 0.1f, 0.1f), true
            ),
            ReactManState.THROW.name pairTo Animation(regions.get(ReactManState.THROW.regionName)),
            ReactManState.RUN.name pairTo Animation(regions.get(ReactManState.RUN.regionName), 2, 2, 0.1f, true),
            ReactManState.JUMP.name pairTo Animation(regions.get(ReactManState.JUMP.regionName)),
            "Defeated" pairTo Animation(regions.get("Defeated"), 3, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}