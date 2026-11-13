package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getRandom
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
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.entities.megamanx.MegamanXSabreSlash
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs

class HoverGunner(services: MegaServices) : AbstractEnemy(services, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "HoverGunner"

        const val DIRECTIONS = 16
        const val WAKE_DISTANCE = 5.5f

        val RANGES = floatArrayOf(2f, 2.5f, 3f, 3.5f)

        private const val FLY_DUR = 1f
        private const val BURST_DUR = 1.05f
        private const val BURST_AIM_TIME = 0.33f
        private val BURST_SHOT_TIMES = floatArrayOf(0.37f, 0.52f, 0.67f)
        private const val FAN_DUR = 1.75f
        private const val FAN_POSE_TIME = 0.33f
        private const val FAN_FIRST_SHOT_TIME = 0.35f
        private const val FAN_SHOT_INTERVAL = 0.17f
        private const val MUZZLE_DUR = 0.28f
        private const val FAN_SHOTS = 9

        private const val FLY_SPEED = 7.5f
        private const val LEAVE_SPEED = 3.75f
        private const val SHOT_SPEED = 3.75f

        private const val WIDTH = 0.9f
        private const val HEIGHT = 0.7f
        private const val DAMAGER_SIZE = 0.425f
        private const val DAMAGER_OFFSET_X = 0.125f
        private const val DAMAGER_OFFSET_Y = -0.1375f
        const val SPRITE_OFFSET_X = 0.1f

        private val DEBRIS =
            arrayOf("$TAG/debris1", "$TAG/debris2", "$TAG/debris3", "$TAG/debris4", "$TAG/debris5")

        private val FLAME_FLICKER = intArrayOf(0, 1)
        private val FLAME_TICKS = intArrayOf(1, 1)

        val FRAMES = orderedMapOf(
            "fly" pairTo TickFrameDef(28, 16, 7, 7, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "aim_forward" pairTo TickFrameDef(16, 25, 7, 8, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "aim_up" pairTo TickFrameDef(16, 25, 7, 8, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "aim_down" pairTo TickFrameDef(16, 25, 7, 8, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "aim_up_diagonal" pairTo TickFrameDef(16, 25, 7, 8, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "aim_down_diagonal" pairTo TickFrameDef(16, 25, 7, 8, 2, FLAME_FLICKER, FLAME_TICKS, 0),
            "muzzle_flash" pairTo TickFrameDef(
                14, 14, 7, 7, 7,
                intArrayOf(0, 1, 2, 3, 4, 5, 6),
                intArrayOf(1, 1, 1, 2, 3, 4, 5),
                null
            )
        )

        fun aimIndex(dx: Float, dy: Float): Int {
            val degrees = (MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees + 360f) % 360f
            return Math.round(degrees / (360f / DIRECTIONS)) % DIRECTIONS
        }

        fun sideAngles(aim: Int): Pair<Int, Int> {
            val step = if (aim % 4 == 2) 2 else 1
            val a = (aim + step) % DIRECTIONS
            val b = (aim - step + DIRECTIONS) % DIRECTIONS
            val upA = MathUtils.sinDeg(a * 360f / DIRECTIONS)
            val upB = MathUtils.sinDeg(b * 360f / DIRECTIONS)
            return if (upA >= upB) Pair(a, b) else Pair(b, a)
        }

        fun fanOrder(aim: Int): IntArray {
            val (a, b) = sideAngles(aim)
            return intArrayOf(aim, a, aim, b, aim, a, aim, b, aim)
        }

        fun pose(aim: Int): String {
            val degrees = aim * 360f / DIRECTIONS
            val vertical = MathUtils.sinDeg(degrees)
            return when {
                abs(vertical) < 0.1f -> "aim_forward"
                abs(vertical) > 0.99f -> if (vertical > 0f) "aim_up" else "aim_down"
                vertical > 0f -> "aim_up_diagonal"
                else -> "aim_down_diagonal"
            }
        }

        private val MUZZLE_PIXELS = objectMapOf(
            "aim_forward" pairTo Vector2(-7f, 1f),
            "aim_up" pairTo Vector2(0f, 7f),
            "aim_down" pairTo Vector2(-1f, -7f),
            "aim_up_diagonal" pairTo Vector2(-6f, 7f),
            "aim_down_diagonal" pairTo Vector2(-6f, -5f)
        )

        fun muzzleOffset(pose: String, facingRight: Boolean, out: Vector2): Vector2 {
            val pixels = MUZZLE_PIXELS[pose] ?: throw IllegalArgumentException("HoverGunner: no muzzle for pose $pose")
            val side = if (facingRight) 1f else -1f
            return out.set(
                side * (SPRITE_OFFSET_X * ConstVals.PPM - pixels.x * TickFrames.SCALE),
                pixels.y * TickFrames.SCALE
            )
        }
    }

    private enum class HoverGunnerState { WAIT, FLY, BURST, FAN, LEAVE }

    override lateinit var facing: Facing
    override val damageNegotiator: IDamageNegotiator = SelfSizeDamageNegotiator(
        this,
        objectMapOf(
            Bullet::class pairTo dmgNeg(18),
            ChargedShot::class pairTo dmgNeg(30),
            MegamanXSabreSlash::class pairTo dmgNeg(30)
        )
    )

    private val animations = ObjectMap<String, IAnimation>()
    private val muzzle = Vector2()

    private lateinit var state: HoverGunnerState
    private var range = 0f
    private var bursts = 0
    private var aim = 0
    private var poseKey = "fly"

    private val flyTimer = Timer(FLY_DUR)
    private val burstTimer = Timer(BURST_DUR).also { timer ->
        timer.addRunnable(TimeMarkedRunnable(BURST_AIM_TIME) { aimAtPlayer(true) })
        BURST_SHOT_TIMES.forEach { time -> timer.addRunnable(TimeMarkedRunnable(time) { shoot(aim) }) }
    }
    private val fanTimer = Timer(FAN_DUR).also { timer ->
        timer.addRunnable(TimeMarkedRunnable(FAN_POSE_TIME) { showAimPose() })
        (0 until FAN_SHOTS).forEach { i ->
            timer.addRunnable(TimeMarkedRunnable(FAN_FIRST_SHOT_TIME + i * FAN_SHOT_INTERVAL) { shoot(fanOrder(aim)[i]) })
        }
    }
    private val muzzleTimer = Timer(MUZZLE_DUR)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        TickFrames.animations(atlas, TAG, FRAMES, animations)
        FallingPiece.load(services, DEBRIS)
        super.init()
        addComponent(
            AnimationsComponentBuilder(this)
                .key(TAG).animator(Animator({ poseKey }, animations))
                .key("muzzle").animator(Animator({ if (muzzleTimer.isFinished()) null else "muzzle_flash" }, animations))
                .build()
        )
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(Vector2())
        body.setCenter(center)

        val direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.LEFT, String::class)
        facing = if (direction == ConstKeys.LEFT) Facing.LEFT else Facing.RIGHT
        state = if (facing == Facing.LEFT) HoverGunnerState.FLY else HoverGunnerState.WAIT
        if (state == HoverGunnerState.FLY) putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))

        range = RANGES[getRandom(0, RANGES.size - 1)] * ConstVals.PPM
        bursts = 0
        aim = 0
        poseKey = "fly"
        flyTimer.reset()
        muzzleTimer.setToEnd()
    }

    override fun canDamage(damageable: IDamageableEntity) =
        state != HoverGunnerState.WAIT && super.canDamage(damageable)

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        state != HoverGunnerState.WAIT && super.canBeDamagedBy(damager)

    private fun setState(next: HoverGunnerState) {
        state = next
        when (next) {
            HoverGunnerState.FLY -> flyTimer.reset()
            HoverGunnerState.BURST -> burstTimer.reset()
            HoverGunnerState.FAN -> {
                fanTimer.reset()
                aimAtPlayer(false)
            }

            else -> {}
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            muzzleTimer.update(delta)

            val player = targetPlayer.body.getCenter()
            val center = body.getCenter()

            when (state) {
                HoverGunnerState.WAIT -> {
                    body.physics.velocity.setZero()
                    if (player.x - center.x >= WAKE_DISTANCE * ConstVals.PPM) {
                        putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))
                        setState(HoverGunnerState.FLY)
                    }
                }

                HoverGunnerState.FLY -> {
                    poseKey = "fly"
                    body.physics.velocity.set(FLY_SPEED * ConstVals.PPM * facing.value, 0f)
                    flyTimer.update(delta)
                    when {
                        abs(player.x - center.x) <= range -> {
                            body.physics.velocity.setZero()
                            setState(HoverGunnerState.BURST)
                        }

                        flyTimer.isFinished() -> setState(HoverGunnerState.LEAVE)
                    }
                }

                HoverGunnerState.BURST -> {
                    body.physics.velocity.setZero()
                    burstTimer.update(delta)
                    if (burstTimer.isFinished()) {
                        bursts++
                        setState(if (bursts >= 2) HoverGunnerState.FAN else HoverGunnerState.FLY)
                    }
                }

                HoverGunnerState.FAN -> {
                    body.physics.velocity.setZero()
                    fanTimer.update(delta)
                    if (fanTimer.isFinished()) setState(HoverGunnerState.LEAVE)
                }

                HoverGunnerState.LEAVE -> body.physics.velocity.set(0f, LEAVE_SPEED * ConstVals.PPM)
            }
        }
    }

    private fun aimAtPlayer(showPose: Boolean) {
        val player = targetPlayer.body.getCenter()
        val center = body.getCenter()
        aim = aimIndex(player.x - center.x, player.y - center.y)
        if (showPose) showAimPose()
    }

    private fun showAimPose() {
        poseKey = pose(aim)
        val horizontal = MathUtils.cosDeg(aim * 360f / DIRECTIONS)
        if (horizontal < -0.01f) facing = Facing.LEFT else if (horizontal > 0.01f) facing = Facing.RIGHT
    }

    private fun shoot(direction: Int) {
        val center = body.getCenter()
        muzzleOffset(poseKey, isFacing(Facing.RIGHT), muzzle)
        val spawn = Vector2(center).add(muzzle)
        val degrees = direction * 360f / DIRECTIONS
        val trajectory = Vector2(MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees))
            .scl(SHOT_SPEED * ConstVals.PPM)
        val shot = MegaEntityFactory.fetch(EnemyShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.TYPE pairTo EnemyShot.Kind.HOVER_GUNNER,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
        muzzleTimer.reset()
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
        FallingPiece.throwDebris(DEBRIS, body.getCenter())
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(DAMAGER_SIZE * ConstVals.PPM))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture.offsetFromBodyAttachment.set(
                DAMAGER_OFFSET_X * ConstVals.PPM * -facing.value, DAMAGER_OFFSET_Y * ConstVals.PPM
            )
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { damagerFixture }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2)))
        .preProcess { _, sprite ->
            val center = body.getCenter()
            TickFrames.place(sprite, FRAMES[poseKey], center.x + SPRITE_OFFSET_X * ConstVals.PPM * facing.value, center.y, isFacing(Facing.RIGHT))
            sprite.hidden = state == HoverGunnerState.WAIT || damageBlink
        }
        .sprite("muzzle", GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3)))
        .preProcess { _, sprite ->
            val center = body.getCenter()
            TickFrames.place(sprite, FRAMES["muzzle_flash"], center.x + muzzle.x, center.y + muzzle.y, false)
            sprite.hidden = muzzleTimer.isFinished() || state == HoverGunnerState.WAIT
        }
        .build()
}
