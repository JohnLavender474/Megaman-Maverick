package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.items.HeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.ReactorMonkeyBall
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class ReactorMonkey(game: MegamanMaverickGame) :
    AbstractBoss(game, dmgDuration = DEFAULT_MINI_BOSS_DMG_DURATION, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMonkey"
        const val BALL_SPAWN_Y_KEY = "${ConstKeys.BALL}_${ConstKeys.SPAWN}_${ConstKeys.Y}"

        private const val MIN_THROW_DELAY = 0.75f
        private const val MAX_THROW_DELAY = 2.25f

        private const val THROW_DUR = 0.3f

        private const val BALL_CATCH_RADIUS = 0.25f
        private const val BALL_IMPULSE_Y = 6f

        private const val HORIZONTAL_SCALAR = 1.25f
        private const val VERTICAL_SCALAR = 1f

        private const val DEFAULT_BALL_SPAWN_Y = 8f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ReactorMonkeyState { STAND, THROW }

    override lateinit var facing: Facing

    private lateinit var state: ReactorMonkeyState
    private val position = Vector2()

    private var monkeyBall: ReactorMonkeyBall? = null

    private val throwTimer = Timer(THROW_DUR)
    private lateinit var throwDelayTimer: Timer

    private var ballSpawnY = DEFAULT_BALL_SPAWN_Y
    private val ballCatchArea = GameCircle().setRadius(BALL_CATCH_RADIUS * ConstVals.PPM)

    private var megaHeartTank: MegaHeartTank? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            gdxArrayOf("stand", "stand_damaged", "throw", "throw_damaged").forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(Fireball::class, dmgNeg(4))
        damageOverrides.put(MagmaWave::class, dmgNeg(4))
        damageOverrides.put(MagmaFlame::class, dmgNeg(4))
        damageOverrides.put(MoonScythe::class, dmgNeg(2))
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        position.set(spawn)

        throwDelayTimer = Timer(MAX_THROW_DELAY)
        throwTimer.reset()

        state = ReactorMonkeyState.STAND

        facing = if (megaman.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT
        ballSpawnY = spawnProps.getOrDefault(BALL_SPAWN_Y_KEY, DEFAULT_BALL_SPAWN_Y, Float::class)

        megaHeartTank = when {
            spawnProps.containsKey("${ConstKeys.HEART}_${ConstKeys.TANK}") ->
                MegaHeartTank.valueOf(
                    spawnProps.get("${ConstKeys.HEART}_${ConstKeys.TANK}", String::class)!!.uppercase()
                )

            else -> null
        }
    }

    override fun isReady(delta: Float) = true

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        monkeyBall?.let { ball -> if (!ball.dead) ball.destroy() }
        monkeyBall = null

        if (megaHeartTank != null) {
            val heartTank = MegaEntityFactory.fetch(HeartTank::class)!!
            heartTank.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.VALUE pairTo megaHeartTank))
        }
    }

    fun spawnNewMonkeyBall() {
        if (this.monkeyBall != null) throw IllegalStateException("Monkey ball should be null when a new one is spawned")

        val spawn = body.getPositionPoint(Position.TOP_CENTER).add(0f, ballSpawnY * ConstVals.PPM)

        val monkeyBall = MegaEntityFactory.fetch(ReactorMonkeyBall::class)!!
        monkeyBall.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.COLLIDE pairTo false,
                ConstKeys.POSITION pairTo spawn
            )
        )

        this.monkeyBall = monkeyBall
    }

    fun catchMonkeyBall() {
        monkeyBall?.let { ball ->
            ball.body.physics.velocity.setZero()
            ball.body.physics.gravityOn = false
            ball.hidden = true
        }
    }

    fun hurlMonkeyBall() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            ballCatchArea.getCenter(),
            megaman.body.getPosition(),
            BALL_IMPULSE_Y * ConstVals.PPM,
            HORIZONTAL_SCALAR,
            VERTICAL_SCALAR
        )
        monkeyBall?.let { ball ->
            ball.body.physics.velocity.set(impulse)
            ball.body.physics.gravityOn = true
            ball.hidden = false
            ball.collide = true
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add

            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            ballCatchArea.setCenter(body.getPositionPoint(Position.TOP_CENTER).add(0f, 1.75f * ConstVals.PPM))
            facing = if (megaman.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

            if (state == ReactorMonkeyState.STAND) {
                throwDelayTimer.update(delta)
                if (throwDelayTimer.isFinished()) {
                    when {
                        monkeyBall == null -> spawnNewMonkeyBall()
                        ballCatchArea.contains(monkeyBall!!.body.getCenter()) ||
                            monkeyBall!!.body.getY() < ballCatchArea.getY() -> {
                            catchMonkeyBall()
                            state = ReactorMonkeyState.THROW
                            throwDelayTimer.resetDuration(MIN_THROW_DELAY + (MAX_THROW_DELAY - MIN_THROW_DELAY) * getHealthRatio())
                        }
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
        body.setSize(3.5f * ConstVals.PPM, 5f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.putProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", false)
        body.addFixture(bodyFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            body.setBottomCenterToPoint(position)
            body.forEachFixture { fixture -> fixture.setActive(!defeated) }
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { ballCatchArea }), debug = true
            )
        )

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(10f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.75f * ConstVals.PPM * -facing.value)

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink || !ready

            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = key@{
            var key = state.name.lowercase()
            if (!damageTimer.isFinished()) key += "_damaged"
            return@key key
        }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.1f), true),
            "stand_damaged" pairTo Animation(regions["stand_damaged"]),
            "throw" pairTo Animation(regions["throw"], 3, 1, 0.1f, false),
            "throw_damaged" pairTo Animation(regions["throw_damaged"], 3, 1, 0.1f, false)
        )
        val onChangeKey: (String?, String?) -> Unit = { previous, current ->
            if ((current == "throw_damaged" && previous == "throw") ||
                (current == "throw" && previous == "throw_damaged")
            ) {
                val time = animations[previous].getCurrentTime()
                animations[current].setCurrentTime(time)
                GameLogger.debug(TAG, "onChangeKey(): set current to time=$time")
            }
        }
        val animator = Animator(
            keySupplier,
            animations,
            onChangeKey = onChangeKey
        )
        return AnimationsComponent(this, animator)
    }
}
