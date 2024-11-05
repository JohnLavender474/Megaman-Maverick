package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
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
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
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
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class ReactorMonkeyMiniBoss(game: MegamanMaverickGame) :
    AbstractBoss(game, dmgDuration = DEFAULT_MINI_BOSS_DMG_DURATION), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMonkeyMiniBoss"
        const val BALL_SPAWN_Y_KEY = "${ConstKeys.BALL}_${ConstKeys.SPAWN}_${ConstKeys.Y}"
        private const val MIN_THROW_DELAY = 0.75f
        private const val MAX_THROW_DELAY = 2.25f
        private const val THROW_DUR = 0.3f
        private const val BALL_CATCH_RADIUS = 0.25f
        private const val BALL_IMPULSE_Y = 6f
        private const val HORIZONTAL_SCALAR = 1.25f
        private const val VERTICAL_SCALAR = 1f
        private const val DEFAULT_BALL_SPAWN_Y = 3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ReactorMonkeyState { STAND, THROW }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(3),
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

    private val throwTimer = Timer(THROW_DUR)
    private val ballCatchArea = GameCircle().setRadius(BALL_CATCH_RADIUS * ConstVals.PPM)

    private lateinit var throwDelayTimer: Timer
    private lateinit var reactorMonkeyState: ReactorMonkeyState

    private var monkeyBall: ReactorMonkeyBall? = null
    private var ballSpawnY = DEFAULT_BALL_SPAWN_Y

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("stand", atlas.findRegion("$TAG/Stand"))
            regions.put("stand_damaged", atlas.findRegion("$TAG/StandDamaged"))
            regions.put("throw", atlas.findRegion("$TAG/Throw"))
            regions.put("throw_damaged", atlas.findRegion("$TAG/ThrowDamaged"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        throwDelayTimer = Timer(MAX_THROW_DELAY)
        throwTimer.reset()

        reactorMonkeyState = ReactorMonkeyState.STAND

        facing = if (getMegaman().body.x >= body.x) Facing.RIGHT else Facing.LEFT
        ballSpawnY = spawnProps.getOrDefault(BALL_SPAWN_Y_KEY, DEFAULT_BALL_SPAWN_Y, Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        monkeyBall?.let { ball -> if (!ball.dead) ball.destroy() }
        monkeyBall = null
    }

    fun spawnNewMonkeyBall() {
        if (monkeyBall != null) throw IllegalStateException("Monkey ball should be null when a new one is spawned")
        monkeyBall =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.REACTOR_MONKEY_BALL)!! as ReactorMonkeyBall
        val spawn = body.getTopCenterPoint().add(0f, ballSpawnY * ConstVals.PPM)
        monkeyBall!!.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.OWNER pairTo this))
    }

    fun catchMonkeyBall() {
        monkeyBall?.let { ball ->
            ball.body.physics.gravityOn = false
            ball.body.physics.velocity.setZero()
            ball.hidden = true
        }
    }

    fun hurlMonkeyBall() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            ballCatchArea.getCenter(),
            getMegaman().body.getPosition(),
            BALL_IMPULSE_Y * ConstVals.PPM,
            HORIZONTAL_SCALAR,
            VERTICAL_SCALAR
        )
        monkeyBall?.let { ball ->
            ball.body.physics.velocity.set(impulse)
            ball.body.physics.gravityOn = true
            ball.hidden = false
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

            ballCatchArea.setCenter(body.getTopCenterPoint().add(0f, 1.75f * ConstVals.PPM))
            facing = if (getMegaman().body.x >= body.x) Facing.RIGHT else Facing.LEFT

            if (reactorMonkeyState == ReactorMonkeyState.STAND) {
                throwDelayTimer.update(delta)
                if (throwDelayTimer.isFinished()) {
                    if (monkeyBall == null) spawnNewMonkeyBall()
                    else if (monkeyBall!!.body != null && (
                            ballCatchArea.contains(monkeyBall!!.body.getCenter()) ||
                                monkeyBall!!.body.y < ballCatchArea.getY())
                    ) {
                        catchMonkeyBall()
                        reactorMonkeyState = ReactorMonkeyState.THROW
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
                    reactorMonkeyState = ReactorMonkeyState.STAND
                    throwTimer.reset()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM, 4.25f * ConstVals.PPM)
        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body }, { ballCatchArea }),
                debug = true
            )
        )
        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(7.5f * ConstVals.PPM, 8f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink || !ready
            _sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            var key = reactorMonkeyState.name.lowercase()
            if (!damageTimer.isFinished()) key += "_damaged"
            key
        }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 1, 2, gdxArrayOf(1f, 0.1f), true),
            "stand_damaged" pairTo Animation(regions["stand_damaged"]),
            "throw" pairTo Animation(regions["throw"], 1, 3, 0.1f, false),
            "throw_damaged" pairTo Animation(regions["throw_damaged"], 1, 3, 0.1f, false)
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
