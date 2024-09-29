package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.*

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
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class Gachappan(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IAnimatedEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "Gachappan"
        private const val BULLET_SPEED = 7.5f
        private const val BALL_GRAVITY = -0.1f
        private const val BALL_IMPULSE = 12f
        private const val WAIT_DURATION = 0.9f
        private const val TRANS_DURATION = 0.3f
        private const val SHOOT_DURATION = 3f
        private var shootRegion: TextureRegion? = null
        private var waitRegion: TextureRegion? = null
        private var openRegion: TextureRegion? = null
    }

    enum class GachappanState { WAIT, OPENING, SHOOT, CLOSING }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(5),
        Fireball::class pairTo dmgNeg(15),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 5 else 3
        }
    )
    override lateinit var facing: Facing

    private lateinit var loop: Loop<GamePair<GachappanState, Timer>>

    override fun init() {
        if (waitRegion == null || shootRegion == null || openRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            waitRegion = atlas.findRegion("Gachappan/Wait")
            shootRegion = atlas.findRegion("Gachappan/Shoot")
            openRegion = atlas.findRegion("Gachappan/Open")
        }

        val throwTimes = gdxArrayOf(0.5f, 2.5f)
        val shootTimes = gdxArrayOf(1f, 1.5f, 2f)
        val runnables = Array<TimeMarkedRunnable>()
        throwTimes.forEach { runnables.add(TimeMarkedRunnable(it) { launchBall() }) }
        shootTimes.forEach { runnables.add(TimeMarkedRunnable(it) { shoot() }) }
        loop = Loop(
            gdxArrayOf(
                GachappanState.WAIT pairTo Timer(WAIT_DURATION),
                GachappanState.OPENING pairTo Timer(TRANS_DURATION),
                GachappanState.SHOOT pairTo Timer(SHOOT_DURATION).setRunnables(runnables),
                GachappanState.CLOSING pairTo Timer(TRANS_DURATION)
            )
        )

        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        loop.forEach { it.second.reset() }
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasDepletedHealth()) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            val props = props(
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_1_SOUND
            )
            explosion.spawn(props)
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
            val (_, timer) = loop.getCurrent()
            timer.update(it)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture1 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        damagerFixture1.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
        body.addFixture(damagerFixture1)
        damagerFixture1.rawShape.color = Color.RED
        debugShapes.add { damagerFixture1.getShape() }

        val damagerFixture2 = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(1f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        damagerFixture2.offsetFromBodyCenter.y = 0.5f * ConstVals.PPM
        body.addFixture(damagerFixture2)
        damagerFixture2.rawShape.color = Color.RED
        debugShapes.add { damagerFixture2.getShape() }

        val damageableFixture1 =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        damageableFixture1.offsetFromBodyCenter.y = -0.35f * ConstVals.PPM
        body.addFixture(damageableFixture1)
        damageableFixture1.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture1.getShape() }

        val damageableFixture2 =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.45f * ConstVals.PPM))
        damageableFixture2.offsetFromBodyCenter.y = -1.25f * ConstVals.PPM
        body.addFixture(damageableFixture2)
        damageableFixture2.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture2.getShape() }

        val shieldFixture1 =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1f * ConstVals.PPM, 3f * ConstVals.PPM))
        shieldFixture1.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture1)
        shieldFixture1.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture1.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val damageableFixtures = gdxArrayOf(damageableFixture1, damageableFixture2)
        body.preProcess.put(ConstKeys.DEFAULT) {
            val (gachappanState, _) = loop.getCurrent()
            val active = gachappanState == GachappanState.SHOOT
            damageableFixtures.forEach {
                it.active = active
                it.getShape().color = if (active) Color.PURPLE else Color.CLEAR
            }

            val offsetX = 0.5f * ConstVals.PPM * facing.value
            shieldFixture1.offsetFromBodyCenter.x = -offsetX
            damagerFixture2.offsetFromBodyCenter.x = -offsetX
            damageableFixture1.offsetFromBodyCenter.x = offsetX
            damageableFixture2.offsetFromBodyCenter.x = offsetX
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            val (gachappanState, _) = loop.getCurrent()
            gachappanState.name
        }
        val animations = objectMapOf<String, IAnimation>(
            GachappanState.WAIT.name pairTo Animation(waitRegion!!, 1, 3, 0.1f, false),
            GachappanState.OPENING.name pairTo Animation(openRegion!!, 1, 3, 0.1f, false),
            GachappanState.SHOOT.name pairTo Animation(shootRegion!!, 1, 3, 0.1f, true),
            GachappanState.CLOSING.name pairTo Animation(openRegion!!, 1, 3, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun launchBall() {
        val spawn = body.getTopCenterPoint()
        spawn.x += 0.25f * ConstVals.PPM * -facing.value
        val ball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.EXPLODING_BALL)!!
        val impulseX = (getMegaman().body.x - body.x) * 0.9f
        val impulseY = BALL_IMPULSE * ConstVals.PPM
        ball.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo Vector2(impulseX, impulseY),
                ConstKeys.GRAVITY pairTo Vector2(0f, BALL_GRAVITY * ConstVals.PPM)
            )
        )
        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun shoot() {
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        val spawn = body.getBottomCenterPoint().add(0.5f * ConstVals.PPM * facing.value, 0.175f * ConstVals.PPM)
        val trajectory = Vector2(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
        val bulletProps = props(
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.OWNER pairTo this
        )
        bullet.spawn(bulletProps)
        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}