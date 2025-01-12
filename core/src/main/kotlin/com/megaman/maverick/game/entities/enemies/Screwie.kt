package com.megaman.maverick.game.entities.enemies

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
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import kotlin.reflect.KClass

class Screwie(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "Screwie"
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
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
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
            gdxArrayOf(
                TimeMarkedRunnable(0.5f) { shoot() },
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
        body.setSize(ConstVals.PPM.toFloat(), 0.75f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.15f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        shapes.add { damagerFixture }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setWidth(0.75f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        shapes.add { damageableFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damageableBounds = damageableFixture.rawShape as GameRectangle
            when {
                down -> {
                    damageableBounds.setHeight(0.5f * ConstVals.PPM)
                    damageableFixture.offsetFromBodyAttachment.y = (if (upsideDown) 0.15f else -0.15f) * ConstVals.PPM
                }

                else -> {
                    damageableBounds.setHeight(0.75f * ConstVals.PPM)
                    damageableFixture.offsetFromBodyAttachment.y = 0f
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (game.isCameraRotating()) return@add

            when {
                !downTimer.isFinished() -> {
                    downTimer.update(it)
                    if (downTimer.isFinished()) riseTimer.reset()
                }

                !riseTimer.isFinished() -> {
                    riseTimer.update(it)
                    if (riseTimer.isFinished()) shootTimer.reset()
                }

                !shootTimer.isFinished() -> {
                    shootTimer.update(it)
                    if (shootTimer.isFinished()) dropTimer.reset()
                }

                !dropTimer.isFinished() -> {
                    dropTimer.update(it)
                    if (dropTimer.isFinished()) downTimer.reset()
                }
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
            sprite.setFlip(false, upsideDown)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            val key = if (down) "down" else if (shooting) "shoot" else if (rising) "rise" else "drop"
            "$type-$key"
        }
        animations = objectMapOf(
            "red-down" pairTo Animation(atlas!!.findRegion("RedScrewie/Down")),
            "red-rise" pairTo Animation(atlas!!.findRegion("RedScrewie/Rise"), 1, 3, 0.1f, false),
            "red-drop" pairTo Animation(atlas!!.findRegion("RedScrewie/Drop"), 1, 3, 0.1f, false),
            "red-shoot" pairTo Animation(atlas!!.findRegion("RedScrewie/Shoot"), 1, 3, 0.1f, true),
            "blue-down" pairTo Animation(atlas!!.findRegion("BlueScrewie/Down")),
            "blue-rise" pairTo Animation(atlas!!.findRegion("BlueScrewie/Rise"), 1, 3, 0.1f, false),
            "blue-drop" pairTo Animation(atlas!!.findRegion("BlueScrewie/Drop"), 1, 3, 0.1f, false),
            "blue-shoot" pairTo Animation(atlas!!.findRegion("BlueScrewie/Shoot"), 1, 3, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

        BULLET_TRAJECTORIES.forEach {
            val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!

            val spawn = body.getCenter()
            when {
                it.x > 0 -> spawn.x += 0.2f * ConstVals.PPM
                it.x < 0 -> spawn.x -= 0.2f * ConstVals.PPM
            }
            spawn.y += when {
                upsideDown -> -0.35f
                else -> 0.35f
            } * ConstVals.PPM

            val trajectory = it.cpy().scl(movementScalar * ConstVals.PPM)
            if (upsideDown) trajectory.y *= -1f

            bullet.spawn(
                props(
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.DIRECTION pairTo megaman.direction
                )
            )
        }
    }
}
