package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.math.sqrt

class ReactorMonkeyBall(services: MegaServices) : AbstractProjectile(services) {

    companion object {
        const val TAG = "ReactorMonkeyBall"
        private const val GRAVITY = -22.5f
        private const val MAX_BOUNCES = 3
        private const val BOUNCE_STRIPS = 3
        private const val SKIP_SPIKE_TIME = 0.3f
        private const val SKIP_HOP_HEIGHT = 1.5f
        private const val SKIP_MIN_SPEED_X = 3f
        private const val SKIP_MAX_SPEED_X = 8f
        private const val BOUNCE_VEL_SCALAR = 0.75f
        private const val EXPLODING_ALPHA = 0.5f
        private val INIT_IMPULSE = Vector2(0f, -3f * ConstVals.PPM)
        private val ANIM_DURS = gdxArrayOf(0.1f, 0.075f, 0.05f)
        private val CALM_KEYS = Array<String>().apply { for (i in 0..BOUNCE_STRIPS) add(i.toString()) }
        private val MAD_KEYS = Array<String>().apply { for (i in 0..BOUNCE_STRIPS) add("mad_$i") }
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: IGameEntity? = null

    var hidden = false
    var collide = false

    private var exploding = false
    private var bounces = 0
    private var maxBounces = MAX_BOUNCES
    private var enraged = false

    private var skipping = false
    private var skipHopStarted = false
    private val skipHopVelocity = Vector2()

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val calmAtlas = services.assets.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            val madAtlas = services.assets.getTextureAtlas(TextureAsset.REACTOR_MONKEY.source)
            for (i in 0..BOUNCE_STRIPS) {
                regions.put(CALM_KEYS[i], calmAtlas.findRegion("$TAG/${CALM_KEYS[i]}"))
                regions.put(MAD_KEYS[i], madAtlas.findRegion("$TAG/${MAD_KEYS[i]}"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        collide = spawnProps.getOrDefault(ConstKeys.COLLIDE, false, Boolean::class)
        enraged = spawnProps.getOrDefault(ConstKeys.RED, false, Boolean::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.gravityOn = true
        body.physics.velocity.set(INIT_IMPULSE)

        exploding = false
        hidden = false
        bounces = 0
        maxBounces = MAX_BOUNCES
        skipping = false
        skipHopStarted = false
        skipHopVelocity.setZero()
    }

    override fun onDestroy() {
        super.onDestroy()
        exploding = false
    }

    fun lob(velocity: Vector2) {
        body.physics.velocity.set(velocity)
        release()
    }

    fun skip(floorTarget: Vector2, hopTargetX: Float, maxBounces: Int) {
        val ppm = ConstVals.PPM
        val gravity = GRAVITY * ppm
        val center = body.getCenter()
        val direction = if (floorTarget.x >= center.x) 1f else -1f
        val dx = floorTarget.x - center.x
        val dy = floorTarget.y + body.getHeight() / 2f - center.y
        val spikeTime = SKIP_SPIKE_TIME
        body.physics.velocity.set(dx / spikeTime, (dy - 0.5f * gravity * spikeTime * spikeTime) / spikeTime)

        val hopSpeedY = sqrt(2f * abs(gravity) * SKIP_HOP_HEIGHT * ppm)
        val hopTime = 2f * hopSpeedY / abs(gravity)
        val hopSpeedX = MathUtils.clamp(
            (hopTargetX - floorTarget.x) * direction / hopTime, SKIP_MIN_SPEED_X * ppm, SKIP_MAX_SPEED_X * ppm
        )
        skipHopVelocity.set(direction * hopSpeedX, hopSpeedY)
        this.maxBounces = maxBounces
        skipping = true
        skipHopStarted = false
        release()
    }

    private fun release() {
        body.physics.gravityOn = true
        hidden = false
        collide = true
    }

    // TODO: the Fallout Pool leaves a puddle at the enraged ball's last bounce here (item 077, branch 077i later)
    override fun explodeAndDie(vararg params: Any?) {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION_FIELD)!!
        val props = props(
            ConstKeys.BOUNDS pairTo GameRectangle(body),
            ConstKeys.OWNER pairTo this,
            ConstKeys.END pairTo { if (exploding) destroy() }
        )
        explosion.spawn(props)
        exploding = true
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
    }

    private fun bounce(direction: Direction, add: Boolean = true) {
        if (!collide) return

        if (add) bounces++

        when {
            bounces >= maxBounces -> explodeAndDie()
            else -> {
                when (direction) {
                    Direction.UP -> {
                        if (skipping) skipHop()
                        else body.physics.velocity.y = abs(body.physics.velocity.y) * BOUNCE_VEL_SCALAR
                    }

                    Direction.DOWN -> body.physics.velocity.y = -abs(body.physics.velocity.y) * BOUNCE_VEL_SCALAR
                    Direction.LEFT -> body.physics.velocity.x = -abs(body.physics.velocity.x) * BOUNCE_VEL_SCALAR
                    Direction.RIGHT -> body.physics.velocity.x = abs(body.physics.velocity.x) * BOUNCE_VEL_SCALAR
                }
                requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
            }
        }
    }

    private fun skipHop() {
        if (!skipHopStarted) {
            body.physics.velocity.x = skipHopVelocity.x
            skipHopStarted = true
        }
        body.physics.velocity.y = skipHopVelocity.y
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> bounce(Direction.DOWN) }
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> bounce(Direction.UP) }
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> bounce(Direction.RIGHT) }
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> bounce(Direction.LEFT) }
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val circle = GameCircle().setRadius(body.getWidth() / 2f)

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.SHIELD pairTo circle.copy(),
                FixtureType.DAMAGER pairTo circle.copy(),
                FixtureType.PROJECTILE pairTo circle.copy(),
                FixtureType.WATER_LISTENER pairTo circle.copy()
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(4f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.hidden = hidden
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(if (exploding) EXPLODING_ALPHA else 1f)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            val index = bounces.coerceIn(0, BOUNCE_STRIPS)
            if (enraged) MAD_KEYS[index] else CALM_KEYS[index]
        }

        val animations = ObjectMap<String, IAnimation>()
        for (i in 0..BOUNCE_STRIPS) {
            for (key in gdxArrayOf(CALM_KEYS[i], MAD_KEYS[i])) {
                val region = regions[key]!!
                val animation = if (i == 0) Animation(region) else Animation(region, 2, 1, ANIM_DURS[i - 1], true)
                animations.put(key, animation)
            }
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
