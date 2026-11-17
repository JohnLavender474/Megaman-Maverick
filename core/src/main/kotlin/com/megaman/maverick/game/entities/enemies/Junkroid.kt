package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.players.PlayerSabreSlash
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

class Junkroid(services: MegaServices) : AbstractEnemy(services, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Junkroid"

        const val RANGE = 7f

        private const val FIRST_SHOT_DELAY = 0.25f
        private const val SHOT_DELAY = 1f
        private const val SHOOT_DUR = 0.5f
        private const val FIRE_TIME = 0.25f

        private const val WIDTH = 0.9375f
        private const val HURT_HEIGHT = 1.5625f
        const val ORIGIN_ABOVE_FEET = 1.1875f
        private const val MAP_POINT_ABOVE_FEET = 0.75f
        private const val GRAVITY = -30f
        private const val GROUND_GRAVITY = -0.01f
        private const val FALL_MAX = 12f
        const val HURL_X = 12f
        const val HURL_Y = 3f
        const val HURL_GRAVITY = -20f
        private const val MOUTH_BEHIND_ORIGIN = 1.5f
        private const val MOUTH_ABOVE_ORIGIN = 9.5f

        private val DEBRIS = arrayOf("$TAG/debris1", "$TAG/debris2", "$TAG/debris3", "$TAG/debris4")

        val FRAMES = orderedMapOf(
            "idle" pairTo TickFrameDef(41, 38, 20, 18, 1, intArrayOf(0), intArrayOf(1), 0),
            "shoot" pairTo TickFrameDef(
                41, 38, 20, 18, 3,
                intArrayOf(0, 1, 2, 2, 1, 0),
                intArrayOf(6, 4, 1, 15, 6, 6),
                null
            )
        )

        fun inRange(dx: Float) = abs(dx) < RANGE * ConstVals.PPM

        fun mouthOffset(side: Float, out: Vector2) =
            out.set(-side * MOUTH_BEHIND_ORIGIN * TickFrames.SCALE, MOUTH_ABOVE_ORIGIN * TickFrames.SCALE)

        fun hurlImpulse(side: Float, out: Vector2) =
            out.set(HURL_X * side, HURL_Y).scl(ConstVals.PPM.toFloat())
    }

    override lateinit var facing: Facing
    override val damageNegotiator: IDamageNegotiator = SelfSizeDamageNegotiator(
        this,
        objectMapOf(
            Bullet::class pairTo dmgNeg(8),
            ProtoBuster::class pairTo dmgNeg(15),
            ChargedShot::class pairTo dmgNeg { if ((it as ChargedShot).fullyCharged) 30 else 15 },
            PlayerSabreSlash::class pairTo dmgNeg {
                it as PlayerSabreSlash
                if (it.fullStrength) 23 else 16
            }
        )
    )

    private val animations = ObjectMap<String, IAnimation>()
    private val origin = Vector2()
    private val shotDelayTimer = Timer(FIRST_SHOT_DELAY)
    private val shootTimer = Timer(SHOOT_DUR).addRunnable(TimeMarkedRunnable(FIRE_TIME) { shoot() })

    private var active = true
    private var engaged = false

    private val shooting: Boolean
        get() = !shootTimer.isFinished()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        TickFrames.animations(atlas, TAG, FRAMES, animations)
        FallingPiece.load(services, DEBRIS)
        super.init()
        addComponent(AnimationsComponent(this, Animator({ if (shooting) "shoot" else "idle" }, animations)))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getCenter(GameObjectPools.fetch(Vector2::class))
        body.setBottomCenterToPoint(
            GameObjectPools.fetch(Vector2::class).set(center.x, center.y - MAP_POINT_ABOVE_FEET * ConstVals.PPM)
        )
        active = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class) || services.gameState.hardMode
        facing = if (targetPlayer.body.getCenter().x < center.x) Facing.LEFT else Facing.RIGHT
        engaged = false
        shotDelayTimer.resetDuration(FIRST_SHOT_DELAY)
        shootTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            origin.set(body.getCenter().x, body.getY() + ORIGIN_ABOVE_FEET * ConstVals.PPM)
            if (!active) return@add

            val dx = targetPlayer.body.getCenter().x - origin.x
            if (!shooting) facing = if (dx < 0f) Facing.LEFT else Facing.RIGHT

            if (!body.isSensing(BodySense.FEET_ON_GROUND)) return@add

            if (!inRange(dx)) {
                if (engaged) {
                    engaged = false
                    shootTimer.setToEnd()
                    shotDelayTimer.resetDuration(SHOT_DELAY)
                }
                return@add
            }
            engaged = true

            if (shooting) {
                shootTimer.update(delta)
                if (shootTimer.isFinished()) shotDelayTimer.resetDuration(SHOT_DELAY)
                return@add
            }

            shotDelayTimer.update(delta)
            if (shotDelayTimer.isFinished()) shootTimer.reset()
        }
    }

    private fun shoot() {
        val type = if (services.gameState.hardMode) EnemyShot.Kind.JUNKROID_HARD else EnemyShot.Kind.JUNKROID
        val side = facing.value.toFloat()
        val spawn = mouthOffset(side, Vector2()).add(origin)
        val trajectory = hurlImpulse(side, Vector2())
        val shot = MegaEntityFactory.fetch(EnemyShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.TYPE pairTo type,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun disintegrate(disintegrationProps: Properties?) {
        explode(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DAMAGER pairTo false
            )
        )
        FallingPiece.throwDebris(DEBRIS, body.getCenter(), services.random)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(WIDTH * ConstVals.PPM, (ORIGIN_ABOVE_FEET + HURT_HEIGHT / 2f) * ConstVals.PPM)
        body.physics.velocityClamp.set(0f, FALL_MAX * ConstVals.PPM)

        val hurtBox = GameRectangle().setSize(WIDTH * ConstVals.PPM, HURT_HEIGHT * ConstVals.PPM)
        val offsetY = ORIGIN_ABOVE_FEET * ConstVals.PPM - body.getHeight() / 2f

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, hurtBox.copy())
        damagerFixture.offsetFromBodyAttachment.y = offsetY
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, hurtBox.copy())
        damageableFixture.offsetFromBodyAttachment.y = offsetY
        body.addFixture(damageableFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize((WIDTH - 0.1f) * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { damageableFixture }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val key = if (shooting) "shoot" else "idle"
            TickFrames.place(
                sprite, FRAMES[key], body.getCenter().x, body.getY() + ORIGIN_ABOVE_FEET * ConstVals.PPM,
                isFacing(Facing.RIGHT)
            )
            sprite.hidden = damageBlink
        }
        return component
    }
}
