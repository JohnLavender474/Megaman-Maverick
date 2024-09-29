package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

class TankBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TankBot"
        private const val X_VEL = 2f
        private const val TURN_DUR = 0.4f
        private const val TURN_DELAY = 0.25f
        private const val SHOOT_DELAY = 1f
        private const val LAUNCH_IMPULSE_X = 8f
        private const val LAUNCH_IMPULSE_Y = 8f
        private const val LAUNCH_GRAVITY = -0.15f
        private val regions = ObjectMap<String, TextureRegion>()
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
            if (it.fullyCharged) 15 else 10
        }
    )
    override lateinit var facing: Facing

    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private val turnTimer = Timer(TURN_DUR)
    private val turnDelayTimer = Timer(TURN_DELAY)
    private var stopped = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("roll", atlas.findRegion("$TAG/roll"))
            regions.put("stop", atlas.findRegion("$TAG/stop"))
            regions.put("turn", atlas.findRegion("$TAG/turn"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        shootDelayTimer.reset()
        turnTimer.setToEnd()
        turnDelayTimer.setToEnd()
        stopped = false
    }

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")
        val spawn = body.getCenter().add(0.075f * ConstVals.PPM * facing.value, 0.375f * ConstVals.PPM)
        val impulse = Vector2(LAUNCH_IMPULSE_X * facing.value, LAUNCH_IMPULSE_Y).scl(ConstVals.PPM.toFloat())
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.GRAVITY pairTo Vector2(0f, LAUNCH_GRAVITY * ConstVals.PPM),
                ConstKeys.TRAJECTORY pairTo impulse
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun startTurning() {
        GameLogger.debug(TAG, "startTurning()")
        body.physics.velocity.x = 0f
        turnDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!turnDelayTimer.isFinished()) {
                turnDelayTimer.update(delta)
                if (turnDelayTimer.isFinished()) turnTimer.reset()
                else return@add
            }

            if (!turnTimer.isFinished()) {
                turnTimer.update(delta)
                if (turnTimer.isFinished()) swapFacing()
                else return@add
            }

            if ((isFacing(Facing.LEFT) && getMegaman().body.getX() > body.getMaxX()) ||
                (isFacing(Facing.RIGHT) && getMegaman().body.getMaxX() < body.getX())
            ) {
                startTurning()
                return@add
            }

            if ((isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            ) {
                stopped = true
                body.physics.velocity.x = 0f
            } else {
                stopped = false
                body.physics.velocity.x = X_VEL * ConstVals.PPM * facing.value
            }

            shootDelayTimer.update(delta)
            if (shootDelayTimer.isFinished()) {
                shoot()
                shootDelayTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 0.85f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.offsetFromBodyCenter = Vector2(-0.65f * ConstVals.PPM, -0.5f * ConstVals.PPM)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.rawShape.color = Color.YELLOW
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.offsetFromBodyCenter = Vector2(0.65f * ConstVals.PPM, -0.5f * ConstVals.PPM)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM, 1.05f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!turnDelayTimer.isFinished() || stopped) "stop"
            else if (!turnTimer.isFinished()) "turn"
            else "roll"
        }
        val animations = objectMapOf<String, IAnimation>(
            "stop" pairTo Animation(regions["stop"]),
            "turn" pairTo Animation(regions["turn"], 2, 2, 0.1f, true),
            "roll" pairTo Animation(regions["roll"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}