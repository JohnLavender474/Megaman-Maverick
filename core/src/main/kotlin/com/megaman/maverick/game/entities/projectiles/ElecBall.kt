package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.*

class ElecBall(services: MegaServices) : AbstractProjectile(services), IAnimatedEntity, ILightSource {

    companion object {
        const val TAG = "ElecBall"

        const val BODY_SIZE = 0.5f

        private const val SPEED = 10f

        private const val HIT_DUR = 0.2f

        private const val LIGHT_SOURCE_RADIUS = 3
        private const val LIGHT_SOURCE_RADIANCE = 1.25f

        private const val BLUE = "blue"
        private const val BOUNCE_COOLDOWN = 0.15f
        const val DART_SPEED = "${ConstKeys.END}_${ConstKeys.SPEED}"

        private val COLORS = gdxArrayOf(BLUE, "red", "green", "yellow")

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = OrderedMap<String, AnimationDef>().also { defs ->
            COLORS.forEach { color ->
                defs.put(colorKey("orb", color), AnimationDef(3, 1, 0.05f, true))
                defs.put(colorKey("hit", color), AnimationDef(2, 2, 0.05f, false))
            }
        }

        private fun colorKey(base: String, color: String) = if (color == BLUE) base else "${base}_$color"
    }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_SOURCE_RADIUS
    override var lightSourceRadiance = LIGHT_SOURCE_RADIANCE

    private val hitTimer = Timer(HIT_DUR)
    private var hit = false

    private var color = BLUE
    private var stallMoveDur = 0f
    private var stallPauseDur = 0f
    private var dartSpeed = 0f
    private var stallTime = 0f
    private var darted = false
    private var bounces = 0
    private var bounceCooldown = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val target = spawnProps.get(ConstKeys.TARGET, Vector2::class)!!
        val speed = spawnProps.getOrDefault(ConstKeys.SPEED, SPEED, Float::class)
        val trajectory = target.sub(spawn).nor().scl(speed * ConstVals.PPM)
        body.physics.velocity.set(trajectory)

        color = spawnProps.getOrDefault(ConstKeys.COLOR, BLUE, String::class)
        if (!COLORS.contains(color, false)) throw IllegalArgumentException("ElecBall has no color $color")
        stallMoveDur = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        stallPauseDur = spawnProps.getOrDefault(ConstKeys.PAUSE, 0f, Float::class)
        dartSpeed = spawnProps.getOrDefault(DART_SPEED, 0f, Float::class)
        stallTime = 0f
        darted = false
        bounces = spawnProps.getOrDefault(ConstKeys.BOUNCE, 0, Int::class)
        bounceCooldown = 0f

        lightSourceKeys.addAll(
            spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}") as ObjectSet<Int>
        )

        hitTimer.reset()
        hit = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (bounces == 0) {
            hit()
            return
        }
        if (bounceCooldown > 0f) return
        bounceOff(otherShape)
    }

    private fun bounceOff(blockShape: IGameShape2D) {
        val center = body.getCenter()
        val velocity = body.physics.velocity
        val hitsTopOrBottom = center.y >= blockShape.getMaxY() || center.y <= blockShape.getY()
        if (hitsTopOrBottom) velocity.y = -velocity.y else velocity.x = -velocity.x
        bounces--
        bounceCooldown = BOUNCE_COOLDOWN
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity == owner) return

        hit()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = shieldFixture.getEntity()
        if (entity == owner) return

        hit()
    }

    private fun hit() {
        hit = true
        requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        LightSourceUtils.sendLightSourceEvent(this)

        if (hit) {
            body.physics.velocity.setZero()

            hitTimer.update(delta)
            if (hitTimer.isFinished()) destroy()
            return@UpdatablesComponent
        }

        bounceCooldown -= delta
        if (stallMoveDur > 0f && !darted) updateStall(delta)
    })

    private fun updateStall(delta: Float) {
        stallTime += delta
        if (stallTime < stallMoveDur) return
        if (stallTime < stallMoveDur + stallPauseDur) {
            body.physics.velocity.setZero()
            return
        }
        darted = true
        val trajectory = targetPlayer.body.getCenter().sub(body.getCenter()).nor().scl(dartSpeed * ConstVals.PPM)
        body.physics.velocity.set(trajectory)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.PROJECTILE))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(4f * ConstVals.PPM) }
        )
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { colorKey(if (hit) "hit" else "orb", color) }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()
}
