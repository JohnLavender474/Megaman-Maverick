package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.MoonHead
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.enemies.MoonEyeStone
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Asteroid(game: MegamanMaverickGame) : AbstractProjectile(game), IOwnable<IGameEntity> {

    companion object {
        const val TAG = "Asteroid"

        const val REGULAR = "Regular"
        const val BLUE = "Blue"

        const val MIN_ROTATION_SPEED = 0.5f
        const val MAX_ROTATION_SPEED = 1.5f

        private const val BLINK_DUR = 0.01f

        private const val CULL_TIME = 3f

        private const val DEFAULT_MIN_Y = -100000f * ConstVals.PPM

        private val HIT_PROJS = objectSetOf<KClass<out IProjectileEntity>>(
            Bullet::class,
            Asteroid::class,
            SharpStar::class,
            MoonScythe::class,
            RocketBomb::class,
            PreciousGem::class,
            ChargedShot::class,
            ExplodingBall::class,
            BunbyRedRocket::class
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: IGameEntity? = null

    val impulse = Vector2()
    var hard = false

    private lateinit var type: String

    private var rotation = 0f
    private var rotationSpeed = 0f

    private var delayTimer: Timer? = null

    private val blinkTimer = Timer(BLINK_DUR)
    private var blink = false

    private var minY = DEFAULT_MIN_Y

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put(REGULAR, atlas.findRegion("$TAG/$REGULAR"))
            regions.put(BLUE, atlas.findRegion("$TAG/$BLUE"))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(spawn)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        this.impulse.set(impulse)

        rotationSpeed = spawnProps.getOrDefault(
            "${ConstKeys.ROTATION}_${ConstKeys.SPEED}",
            getRandom(MIN_ROTATION_SPEED, MAX_ROTATION_SPEED), Float::class
        )
        type = spawnProps.getOrDefault(ConstKeys.TYPE, REGULAR, String::class)
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        val delay = spawnProps.get(ConstKeys.DELAY, Float::class)
        delayTimer = if (delay != null) Timer(delay) else null

        blinkTimer.reset()
        blink = false

        val active = delayTimer == null
        body.physics.collisionOn = active
        body.forEachFixture { it.setActive(delayTimer == null) }

        hard = spawnProps.getOrDefault(ConstKeys.HARD, false, Boolean::class)

        minY = spawnProps.getOrDefault("${ConstKeys.MIN}_${ConstKeys.Y}", DEFAULT_MIN_Y, Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        impulse.setZero()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(update@{ delta ->
        if (body.getBounds().getY() < minY) {
            destroy()
            return@update
        }

        delayTimer?.let { timer ->
            timer.update(delta)
            if (timer.isFinished()) {
                blink = false
                delayTimer = null

                body.physics.collisionOn = true
                body.forEachFixture { it.setActive(true) }

                return@let
            }

            body.physics.collisionOn = false
            body.forEachFixture { it.setActive(false) }

            blinkTimer.update(delta)
            if (blinkTimer.isFinished()) {
                blink = !blink
                blinkTimer.reset()
            }
        }
    })

    override fun canDamage(damageable: IDamageable) = super.canDamage(damageable) &&
        !damageable.isAny(MoonHead::class, MoonEyeStone::class)

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val other = bodyFixture.getEntity()

        if (other == owner ||
            (other is Asteroid && !other.hard && hard) ||
            (other is IOwnable<*> && other.owner == owner) ||
            (other is IDamageable && canDamage(other))
        ) return

        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val block = blockFixture.getEntity()

        val filters = blockFixture.getBody().getBlockFilters()
        if (filters != null && filters.any { it.invoke(this, block) }) return

        explodeAndDie()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val other = projectileFixture.getEntity() as IProjectileEntity
        if (HIT_PROJS.contains(other::class)) {
            explodeAndDie()
            other.explodeAndDie()
        }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.625f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.625f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.65f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.6f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.IMPULSE) {
            if (game.isCameraRotating()) body.physics.velocity.setZero() else body.physics.velocity.set(impulse)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { delta, _ ->
            val region = regions.get(type)
            sprite.setRegion(region)

            sprite.setCenter(body.getCenter())

            rotation += rotationSpeed * ConstVals.PPM * delta
            sprite.setOriginCenter()
            sprite.rotation = rotation

            sprite.hidden = blink
        }
        return component
    }
}
