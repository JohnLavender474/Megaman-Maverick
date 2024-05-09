package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandomBool
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.enemies.BabyPenguin
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.Snowball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class PenguinMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IParentEntity, IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PenguinMiniBoss"
        private const val IDLE_DUR = 2f
        private const val LAUNCH_PENGUINS_DUR = 1f
        private const val SHOOT_SNOWBALLS_DUR = 1f
        private const val SNOWBALLS_TO_LAUNCH = 3
        private var region: TextureRegion? = null
    }

    enum class PenguinMiniBossState {
        IDLE,
        LAUNCH_PENGUINS,
        SHOOT_SNOWBALLS
    }

    private val idleTimer = Timer(IDLE_DUR)
    private val launchPenguinsTimer = Timer(LAUNCH_PENGUINS_DUR)
    private val shootSnowballsTimer = Timer(SHOOT_SNOWBALLS_DUR)
    private lateinit var penguinMiniBossState: PenguinMiniBossState
    private var snowballsLaunched = 0

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(2),
        Fireball::class to dmgNeg(10),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 5 else 3
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 3 else 1
        }
    )
    override var children = Array<IGameEntity>()
    override lateinit var facing: Facing

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.TEST.source, "PenguinMiniBoss")
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        penguinMiniBossState = PenguinMiniBossState.IDLE
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT
        snowballsLaunched = 0
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (penguinMiniBossState) {
                PenguinMiniBossState.IDLE -> {
                    idleTimer.update(delta)
                    if (idleTimer.isFinished()) {
                        idleTimer.reset()
                        val randomBool = getRandomBool()
                        penguinMiniBossState =
                            if (randomBool) PenguinMiniBossState.LAUNCH_PENGUINS else PenguinMiniBossState.SHOOT_SNOWBALLS
                    }
                }

                PenguinMiniBossState.LAUNCH_PENGUINS -> {
                    launchPenguinsTimer.update(delta)
                    if (launchPenguinsTimer.isFinished()) {
                        launchBabyPenguin()
                        penguinMiniBossState = PenguinMiniBossState.IDLE
                        launchPenguinsTimer.reset()
                    }
                }

                PenguinMiniBossState.SHOOT_SNOWBALLS -> {
                    shootSnowballsTimer.update(delta)
                    if (shootSnowballsTimer.isFinished()) {
                        shootSnowball()
                        snowballsLaunched++
                        if (snowballsLaunched == SNOWBALLS_TO_LAUNCH) penguinMiniBossState = PenguinMiniBossState.IDLE
                        shootSnowballsTimer.reset()
                    }
                }
            }
        }
    }

    private fun shootSnowball() {
        val snowball = EntityFactories.fetch(EntityType.PROJECTILE, "Snowball")!! as Snowball
        val spawn = Vector2() // TODO: Get spawn point
        val trajectory = Vector2() // TODO: Get trajectory
        game.engine.spawn(
            snowball, props(
                ConstKeys.POSITION to spawn,
                ConstKeys.TRAJECTORY to trajectory,
                ConstKeys.OWNER to this
            )
        )
    }

    private fun launchBabyPenguin() {
        val penguin = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.BABY_PENGUIN)!! as BabyPenguin
        val spawn = Vector2() // TODO: Get spawn point
        game.engine.spawn(
            penguin, props(
                ConstKeys.POSITION to spawn,
                ConstKeys.LEFT to (facing == Facing.LEFT)
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}