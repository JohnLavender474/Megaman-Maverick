package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class Screwie(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        private var atlas: TextureAtlas? = null
        private const val SHOOT_DUR = 2f
        private const val DOWN_DUR = 1f
        private const val RISE_DROP_DUR = .3f
        private const val BULLET_VEL = 10f
        private val BULLET_TRAJECTORIES = gdxArrayOf(
            Vector2(-BULLET_VEL, 0f),
            Vector2(-BULLET_VEL, BULLET_VEL),
            Vector2(0f, BULLET_VEL),
            Vector2(BULLET_VEL, BULLET_VEL),
            Vector2(BULLET_VEL, 0f),
        )
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }
    )

    private val downTimer = Timer(DOWN_DUR)
    private val riseTimer = Timer(RISE_DROP_DUR)
    private val shootTimer = Timer(SHOOT_DUR)
    private val dropTimer = Timer(RISE_DROP_DUR)

    private lateinit var animations: ObjectMap<String, IAnimation>

    private var upsideDown = false
    private var type = ""

    private val down: Boolean
        get() = !downTimer.isFinished()
    private val shooting: Boolean
        get() = !shootTimer.isFinished()
    private val rising: Boolean
        get() = !riseTimer.isFinished()

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        shootTimer.setRunnables(
            gdxArrayOf(TimeMarkedRunnable(0.5f) { shoot() },
                TimeMarkedRunnable(1.0f) { shoot() },
                TimeMarkedRunnable(1.5f) { shoot() })
        )
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, "red") as String
        upsideDown = spawnProps.getOrDefault(ConstKeys.DOWN, false) as Boolean

        downTimer.reset()
        riseTimer.setToEnd()
        shootTimer.reset()
        dropTimer.setToEnd()

        val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        val animationDuration = spawnProps.getOrDefault(
            "${ConstKeys.ANIMATION}_${ConstKeys.DURATION}", 0.1f,
            Float::class
        )
        animations.values().forEach { it.setFrameDuration(animationDuration) }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM, 0.5f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.15f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        shapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.65f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        shapes.add { damageableFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val damageableBounds = damageableFixture.rawShape as GameRectangle
            if (down) {
                damageableBounds.height = 0.2f * ConstVals.PPM
                damageableFixture.offsetFromBodyCenter.y = (if (upsideDown) 0.15f else -0.15f) * ConstVals.PPM
            } else {
                damageableBounds.height = 0.65f * ConstVals.PPM
                damageableFixture.offsetFromBodyCenter.y = 0f
            }
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (game.isCameraRotating()) return@add

            if (!downTimer.isFinished()) {
                downTimer.update(it)
                if (downTimer.isFinished()) riseTimer.reset()
            } else if (!riseTimer.isFinished()) {
                riseTimer.update(it)
                if (riseTimer.isFinished()) shootTimer.reset()
            } else if (!shootTimer.isFinished()) {
                shootTimer.update(it)
                if (shootTimer.isFinished()) dropTimer.reset()
            } else if (!dropTimer.isFinished()) {
                dropTimer.update(it)
                if (dropTimer.isFinished()) downTimer.reset()
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
            _sprite.setFlip(false, upsideDown)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            val key = if (down) "down" else if (shooting) "shoot" else if (rising) "rise" else "drop"
            "$type-$key"
        }
        animations = objectMapOf(
            "red-down" to Animation(atlas!!.findRegion("RedScrewie/Down")),
            "red-rise" to Animation(atlas!!.findRegion("RedScrewie/Rise"), 1, 3, 0.1f, false),
            "red-drop" to Animation(atlas!!.findRegion("RedScrewie/Drop"), 1, 3, 0.1f, false),
            "red-shoot" to Animation(atlas!!.findRegion("RedScrewie/Shoot"), 1, 3, 0.1f, true),
            "blue-down" to Animation(atlas!!.findRegion("BlueScrewie/Down")),
            "blue-rise" to Animation(atlas!!.findRegion("BlueScrewie/Rise"), 1, 3, 0.1f, false),
            "blue-drop" to Animation(atlas!!.findRegion("BlueScrewie/Drop"), 1, 3, 0.1f, false),
            "blue-shoot" to Animation(atlas!!.findRegion("BlueScrewie/Shoot"), 1, 3, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

        BULLET_TRAJECTORIES.forEach {
            val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!

            val spawn = Vector2(body.getCenter())
            if (it.x > 0) spawn.x += 0.2f * ConstVals.PPM else if (it.x < 0) spawn.x -= 0.2f * ConstVals.PPM
            spawn.y += (if (upsideDown) -0.215f else 0.215f) * ConstVals.PPM

            val trajectory = Vector2(it).scl(movementScalar * ConstVals.PPM)
            if (upsideDown) trajectory.y *= -1f
            bullet.spawn(
                props(
                    ConstKeys.TRAJECTORY to trajectory,
                    ConstKeys.POSITION to spawn,
                    ConstKeys.OWNER to this,
                    ConstKeys.DIRECTION to getMegaman().directionRotation!!
                )
            )
        }
    }
}
