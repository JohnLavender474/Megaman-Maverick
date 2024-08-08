package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.projectiles.ReactorMonkeyBall
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class ReactorMonkeyMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    enum class ReactorMonkeyState {
        STAND, THROW
    }

    companion object {
        const val TAG = "ReactorMonkeyMiniBoss"
        private const val MIN_THROW_DELAY = 0.5f
        private const val MAX_THROW_DELAY = 2f
        private const val THROW_DUR = 0.3f
        private const val BALL_CATCH_RADIUS = 0.25f
        private const val BALL_IMPULSE_Y = 6.5f
        private const val HORIZONTAL_SCALAR = 1.1f
        private const val VERTICAL_SCALAR = 0.75f
        private var standRegion: TextureRegion? = null
        private var throwRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(Bullet::class to dmgNeg(1),
        Fireball::class to dmgNeg(3),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 3 else 2
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        })
    override lateinit var facing: Facing

    private val throwTimer = Timer(THROW_DUR)
    private lateinit var throwDelayTimer: Timer
    private lateinit var state: ReactorMonkeyState
    private var monkeyBall: ReactorMonkeyBall? = null
    private val ballCatchArea = GameCircle().setRadius(BALL_CATCH_RADIUS * ConstVals.PPM)

    override fun init() {
        if (standRegion == null || throwRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            standRegion = atlas.findRegion("${TAG}/Stand")
            throwRegion = atlas.findRegion("${TAG}/Throw")
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Boss key = ${spawnProps.get("${ConstKeys.BOSS}_${ConstKeys.KEY}")}")
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        throwDelayTimer = Timer(MAX_THROW_DELAY)
        throwTimer.reset()
        state = ReactorMonkeyState.STAND
        facing = if (getMegaman().body.x >= body.x) Facing.RIGHT else Facing.LEFT
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        monkeyBall?.kill()
        monkeyBall = null
    }

    fun spawnNewMonkeyBall() {
        if (monkeyBall != null) throw IllegalStateException("Monkey ball should be null when a new one is spawned")
        monkeyBall =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.REACTOR_MONKEY_BALL)!! as ReactorMonkeyBall
        game.engine.spawn(
            monkeyBall!!, props(
                ConstKeys.POSITION to body.getTopCenterPoint().add(0f, 4f * ConstVals.PPM), ConstKeys.OWNER to this
            )
        )
    }

    fun catchMonkeyBall() {
        monkeyBall!!.body.physics.gravityOn = false
        monkeyBall!!.body.physics.velocity.setZero()
        monkeyBall!!.firstSprite!!.hidden = true
    }

    fun hurlMonkeyBall() {
        monkeyBall!!.body.physics.gravityOn = true
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPosition(), getMegaman().body.getPosition(), HORIZONTAL_SCALAR,
            BALL_IMPULSE_Y * ConstVals.PPM, VERTICAL_SCALAR
        )
        monkeyBall!!.body.physics.velocity.set(impulse)
        monkeyBall!!.firstSprite!!.hidden = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            ballCatchArea.setCenter(body.getTopCenterPoint().add(0f, 1.75f * ConstVals.PPM))
            facing = if (getMegaman().body.x >= body.x) Facing.RIGHT else Facing.LEFT

            if (state == ReactorMonkeyState.STAND) {
                throwDelayTimer.update(delta)
                if (throwDelayTimer.isFinished()) {
                    if (monkeyBall == null) spawnNewMonkeyBall()
                    else if (monkeyBall!!.body != null && ballCatchArea.contains(monkeyBall!!.body.getCenter())) {
                        catchMonkeyBall()
                        state = ReactorMonkeyState.THROW
                        throwDelayTimer.resetDuration(MIN_THROW_DELAY + (MAX_THROW_DELAY - MIN_THROW_DELAY) * getHealthRatio())
                    }
                }
            } else {
                throwTimer.update(delta)
                if (throwTimer.time >= 0.2f && monkeyBall != null) {
                    hurlMonkeyBall()
                    monkeyBall = null
                }
                if (throwTimer.isFinished()) {
                    state = ReactorMonkeyState.STAND
                    throwTimer.reset()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2.75f * ConstVals.PPM, 3.5f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture1)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        addComponent(
            DrawableShapesComponent(
                this, debugShapeSuppliers = gdxArrayOf({ body }, { ballCatchArea }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(6.25f * ConstVals.PPM, 6.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { state.name }
        val animations = objectMapOf<String, IAnimation>(
            ReactorMonkeyState.STAND.name to Animation(standRegion!!, 1, 2, gdxArrayOf(1f, 0.1f), true),
            ReactorMonkeyState.THROW.name to Animation(throwRegion!!, 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}