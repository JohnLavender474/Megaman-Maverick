package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.DuoBall
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class DuoBallCanon(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DuoBallCanon"
        private const val DELAY = 0.5f

        private const val BULLETS_TO_SHOOT = 3
        private const val EACH_BULLET_DUR = 0.25f
        private const val BULLET_SPEED = 10f

        private const val BALLS_TO_LAUNCH = 2
        private const val EACH_BALL_DUR = 0.5f
        private const val BALL_IMPULSE = 8f

        private const val SHOOT_ANIM_DUR = 0.1f
        private const val SHOOT_SUFFIX = "_shoot"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DuoBallCanonDirection { STRAIGHT, UP }

    override lateinit var facing: Facing

    private val canonDirectionLoop = Loop(DuoBallCanonDirection.entries.toGdxArray())
    private val canonDirection: DuoBallCanonDirection
        get() = canonDirectionLoop.getCurrent()

    private val delayTimer = Timer(DELAY)
    private val bulletsTimer = Timer(BULLETS_TO_SHOOT * EACH_BALL_DUR).also { timer ->
        for (i in 0 until BULLETS_TO_SHOOT) {
            val time = i * EACH_BULLET_DUR
            val runnable = TimeMarkedRunnable(time) {
                shootBullet()
                shootAnimTimer.reset()
            }
            timer.addRunnable(runnable)
        }
    }
    private val ballsTimer = Timer(BALLS_TO_LAUNCH * EACH_BALL_DUR).also { timer ->
        for (i in 0 until BALLS_TO_LAUNCH) {
            val time = i * EACH_BALL_DUR
            val runnable = TimeMarkedRunnable(time) {
                launchBall()
                shootAnimTimer.reset()
            }
            timer.addRunnable(runnable)
        }
    }
    private val shootAnimTimer = Timer(SHOOT_ANIM_DUR)
    private val shooting: Boolean
        get() = !shootAnimTimer.isFinished()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            val keys = Array<String>()
            DuoBallCanonDirection.entries.forEach { direction ->
                val key = direction.name.lowercase()
                keys.add(key)
                keys.add("${key}${SHOOT_SUFFIX}")
            }
            keys.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        FacingUtils.setFacingOf(this)

        canonDirectionLoop.reset()

        delayTimer.reset()
        ballsTimer.reset()
        bulletsTimer.reset()
        shootAnimTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            shootAnimTimer.update(delta)

            delayTimer.update(delta)
            if (!delayTimer.isFinished()) {
                FacingUtils.setFacingOf(this)
                return@add
            }

            val attackTimer = if (canonDirection == DuoBallCanonDirection.UP) ballsTimer else bulletsTimer
            attackTimer.update(delta)

            if (attackTimer.isFinished()) {
                delayTimer.reset()
                attackTimer.reset()
                canonDirectionLoop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableBounds = GameRectangle()
        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damageableBounds)
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.125f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DAMAGEABLE) {
            val damageableWidth: Float
            val damageableHeight: Float
            when (canonDirection) {
                DuoBallCanonDirection.STRAIGHT -> {
                    damageableWidth = 1.5f
                    damageableHeight = 0.5f
                }

                DuoBallCanonDirection.UP -> {
                    damageableWidth = 1f
                    damageableHeight = 0.75f
                }
            }
            damageableBounds.setSize(damageableWidth * ConstVals.PPM, damageableHeight * ConstVals.PPM)
            damageableBounds.setBottomCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(isFacing(Facing.LEFT), false)

            sprite.hidden = damageBlink
        }.build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    var key = canonDirection.name.lowercase()
                    if (shooting) key = "${key}${SHOOT_SUFFIX}"
                    return@keySupplier key
                }
                .applyToAnimations { animations ->
                    DuoBallCanonDirection.entries.forEach { direction ->
                        val key = direction.name.lowercase()
                        animations.put(key, Animation(regions[key]))

                        val shootingKey = "${key}${SHOOT_SUFFIX}"
                        animations.put(shootingKey, Animation(regions[shootingKey], 2, 1, 0.1f, false))
                    }
                }
                .build()
        )
        .build()

    private fun shootBullet() {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(0.25f * facing.value * ConstVals.PPM, 0.6f * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class).set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun launchBall() {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(0.15f * facing.value * ConstVals.PPM, ConstVals.PPM.toFloat())

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(BALL_IMPULSE * facing.value, BALL_IMPULSE)
            .scl(ConstVals.PPM.toFloat())

        val ball = MegaEntityFactory.fetch(DuoBall::class)!!
        ball.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )

        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }
}
