package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.FallingPiece
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.utils.GravityFrame
import com.megaman.maverick.game.entities.utils.HalfDomeShield
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class MetHelmet(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, ICullableEntity,
    IOwnable<Met> {

    enum class HelmetStatus { FLYING, SEATED, RESTING }

    companion object {
        const val TAG = "MetHelmet"

        private const val GRAVITY = 15f
        private const val HOMING_SPEED = 2f
        private const val HOMING_ACCEL = 1f
        private const val HOMING_GAIN = 4f
        private const val DRIFT_SPEED_HALF = 3f
        private const val DRIFT_SPEED_FULL = 4.5f
        const val DOME_RADIUS = 0.6f

        private const val GROUND_GRAVITY = 1.5f
        private const val WIDTH = 1f
        private const val HEIGHT = 0.6f
        private const val SEAT_HEIGHT = 0.5f
        private const val SPIN_STEP_DEGREES = 45f
        private const val SPIN_STEP_TIME = 0.05f
        private const val GLOW_PERIOD = 1.2f
        private const val GLOW_MIN_BRIGHTNESS = 0.6f
        private const val DRAW_PRIORITY_IN_PLAY = 5
        private const val DRAW_PRIORITY_IDLE = -2
        private const val REGION_ROWS = 16f
        private const val HELMET_ROWS = 11f

        const val HITS_TO_BREAK = 3

        private val MET_PIECES = arrayOf("Met/helmet_piece1", "Met/helmet_piece2", "Met/helmet_piece3", "Met/helmet_piece4")
        private val SNOW_MET_PIECES =
            arrayOf("SnowMet/helmet_piece1", "SnowMet/helmet_piece2", "SnowMet/helmet_piece3", "SnowMet/helmet_piece4")

        private val regions = ObjectMap<String, TextureRegion>()

        fun loadPieces(services: MegaServices) {
            FallingPiece.load(services, MET_PIECES, TextureAsset.ENEMIES_1)
            FallingPiece.load(services, SNOW_MET_PIECES, TextureAsset.ENEMIES_1)
        }

        fun throwPieces(snowLook: Boolean, position: Vector2, random: GameRandom) =
            FallingPiece.throwDebris(if (snowLook) SNOW_MET_PIECES else MET_PIECES, position, random)

        private fun launchSpeed(apex: Float) = sqrt(2f * GRAVITY * apex)

        fun flightTime(apex: Float) = 2f * launchSpeed(apex) / GRAVITY
    }

    override var owner: Met? = null

    var status = HelmetStatus.FLYING
        private set
    var hits = 0
        private set

    private var direction = Direction.UP
    private var facing = Facing.RIGHT
    private var snowLook = false
    private var launcher: ChargedShot? = null
    private var spinSteps = 0
    private var spinTime = 0f
    private var glowTime = 0f
    private val local = GameRectangle()
    private val world = GameRectangle()
    private val ownerLocal = GameRectangle()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("Met", atlas.findRegion("Met/Helmet"))
            regions.put("SnowMet", atlas.findRegion("SnowMet/Helmet"))
        }
        loadPieces(services)
        super.init()
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val met = spawnProps.get(ConstKeys.OWNER, Met::class)!!
        owner = met
        // TODO: the helmet keeps its launch gravity; a Met turned while its helmet flies is homed in the old frame (079a)
        direction = met.direction
        facing = met.facing
        body.direction = direction
        snowLook = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class) == "Snow"
        launcher = spawnProps.get(ConstKeys.SOURCE, ChargedShot::class)!!
        hits = spawnProps.get(ConstKeys.COUNT, Int::class)!!
        status = HelmetStatus.FLYING
        spinSteps = 0
        spinTime = 0f
        glowTime = 0f
        seatOnOwner()
        val speed = launchSpeed(spawnProps.get(ConstKeys.HEIGHT, Float::class)!!) * ConstVals.PPM
        val shot = launcher!!
        val shotLocal = GravityFrame.toLocal(direction, shot.body.getBounds(world), local)
        val shotCenterX = centerX(shotLocal)
        GravityFrame.toLocal(direction, met.body.getBounds(world), ownerLocal)
        val fromShot = centerX(ownerLocal) - shotCenterX
        val away = if (fromShot != 0f) fromShot.sign else -met.facing.value.toFloat()
        val drift = (if (shot.fullyCharged) DRIFT_SPEED_FULL else DRIFT_SPEED_HALF) * ConstVals.PPM * away
        GravityFrame.velocityToWorld(direction, drift, speed, body.physics.velocity)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        val lostBy = owner
        owner = null
        launcher = null
        lostBy?.onHelmetLost(this)
    }

    fun claim(met: Met) {
        GameLogger.debug(TAG, "claim()")
        owner = met
    }

    fun consume() {
        owner = null
        destroy()
    }

    fun release() {
        GameLogger.debug(TAG, "release()")
        owner = null
        if (status == HelmetStatus.SEATED) status = HelmetStatus.FLYING
    }

    private fun update(delta: Float) {
        if (launcher?.existenceState?.liveNow != true) launcher = null
        updateLook(delta)

        val met = owner
        if (status == HelmetStatus.SEATED && met != null) {
            seatOnOwner()
            return
        }

        if (body.isSensing(BodySense.FEET_ON_GROUND)) {
            status = HelmetStatus.RESTING
            setLateralVelocity(0f)
            return
        }

        status = HelmetStatus.FLYING
        if (met == null) {
            setLateralVelocity(0f)
            return
        }

        GravityFrame.toLocal(direction, met.body.getBounds(world), ownerLocal)
        GravityFrame.toLocal(direction, body.getBounds(world), local)
        val dx = centerX(ownerLocal) - centerX(local)
        val maxSpeed = HOMING_SPEED * ConstVals.PPM
        val wanted = (dx * HOMING_GAIN).coerceIn(-maxSpeed, maxSpeed)
        val current = localVelocity().x
        val step = HOMING_ACCEL * ConstVals.PPM * delta
        setLateralVelocity(current + (wanted - current).coerceIn(-step, step))

        val seatY = ownerLocal.getY() + SEAT_HEIGHT * ConstVals.PPM
        if (localVelocity().y < 0f && local.getY() <= seatY && abs(dx) < WIDTH * ConstVals.PPM) {
            status = HelmetStatus.SEATED
            seatOnOwner()
            met.onHelmetSeated()
        }
    }

    private fun updateLook(delta: Float) {
        if (status == HelmetStatus.FLYING) {
            spinTime += delta
            while (spinTime >= SPIN_STEP_TIME) {
                spinTime -= SPIN_STEP_TIME
                spinSteps = (spinSteps + 1) % (360f / SPIN_STEP_DEGREES).toInt()
            }
        } else spinSteps = 0
        glowTime = if (idle()) (glowTime + delta) % GLOW_PERIOD else 0f
    }

    private fun idle() = owner == null && status == HelmetStatus.RESTING

    private fun seatOnOwner() {
        val met = owner!!
        GravityFrame.toLocal(direction, met.body.getBounds(world), ownerLocal)
        local.setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
        local.setPosition(centerX(ownerLocal) - local.getWidth() / 2f, ownerLocal.getY() + SEAT_HEIGHT * ConstVals.PPM)
        body.set(GravityFrame.toWorld(direction, local, world))
        body.physics.velocity.setZero()
    }

    private fun localVelocity() =
        GravityFrame.velocityToLocal(direction, body.physics.velocity, GameObjectPools.fetch(Vector2::class))

    private fun setLateralVelocity(speed: Float) {
        if (direction.isVertical()) body.physics.velocity.x = speed else body.physics.velocity.y = speed
    }

    private fun bonk() {
        if (status != HelmetStatus.FLYING || localVelocity().y <= 0f) return
        GameLogger.debug(TAG, "bonk()")
        GravityFrame.velocityToWorld(direction, localVelocity().x, 0f, body.physics.velocity)
    }

    private fun onHitByProjectile(projectile: IProjectileEntity) {
        if (projectile !is ChargedShot || projectile === launcher || status == HelmetStatus.SEATED) return
        hits++
        if (hits < HITS_TO_BREAK) return
        GameLogger.debug(TAG, "onHitByProjectile(): broken")
        val brokenFor = owner
        owner = null
        throwPieces(snowLook, body.getCenter(), services.random)
        destroy()
        brokenFor?.onHelmetBroken()
    }

    private fun centerX(rect: GameRectangle) = rect.getX() + rect.getWidth() / 2f

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetFixture.drawingColor = Color.GREEN
        body.addFixture(feetFixture)

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> bonk() }
        headFixture.drawingColor = Color.ORANGE
        body.addFixture(headFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravityOn = !(status == HelmetStatus.SEATED && owner != null)
            val solidToShots = !idle()
            body.fixtures.get(FixtureType.SHIELD).first().setActive(solidToShots)
            body.fixtures.get(FixtureType.BODY).first().setActive(solidToShots)
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            GravityFrame.velocityToWorld(direction, 0f, -gravity * ConstVals.PPM, gravityVec)
            body.physics.gravity.set(gravityVec)
        }

        addComponent(
            DrawableShapesComponent(
                debugShapeSuppliers = gdxArrayOf({ body.getBounds() }, { feetFixture }, { headFixture }), debug = true
            )
        )
        HalfDomeShield(body, DOME_RADIUS * ConstVals.PPM, direction = { direction }, passes = { it === launcher })
        val component = BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
        body.fixtures.get(FixtureType.SHIELD).first().setReceiver(Receivers.HIT_BY_PROJECTILE, ::onHitByProjectile)
        return component
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, DRAW_PRIORITY_IN_PLAY))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            owner?.let { facing = it.facing }
            sprite.setRegion(regions[if (snowLook) "SnowMet" else "Met"])
            sprite.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
            val idle = idle()
            sprite.priority.value = if (idle) DRAW_PRIORITY_IDLE else DRAW_PRIORITY_IN_PLAY
            sprite.setFlip(facing == Facing.LEFT, (direction == Direction.DOWN) != idle)
            sprite.setOriginCenter()
            val spin = spinSteps * SPIN_STEP_DEGREES * if (facing == Facing.LEFT) 1f else -1f
            sprite.rotation = spin + when (direction) {
                Direction.UP, Direction.DOWN -> 0f
                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }
            val brightness = if (idle)
                GLOW_MIN_BRIGHTNESS + (1f - GLOW_MIN_BRIGHTNESS) * (0.5f + 0.5f * MathUtils.sin(MathUtils.PI2 * glowTime / GLOW_PERIOD))
            else 1f
            sprite.setColor(brightness, brightness, brightness, 1f)
            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            sprite.setPosition(body.getPositionPoint(position), position)
            if (idle) {
                val drop = (REGION_ROWS - HELMET_ROWS) / REGION_ROWS * sprite.height
                val offset = GravityFrame.velocityToWorld(direction, 0f, -drop, GameObjectPools.fetch(Vector2::class))
                sprite.translate(offset.x, offset.y)
            }
        }
        return component
    }

    override fun getType() = EntityType.HAZARD
}
