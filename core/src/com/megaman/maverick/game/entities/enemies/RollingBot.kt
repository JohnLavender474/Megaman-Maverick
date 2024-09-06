package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.isFacing
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
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

class RollingBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    enum class RollingBotState {
        ROLLING, OPENING, SHOOTING, CLOSING
    }

    companion object {
        const val TAG = "RollingBot"
        private const val X_VEL = 3f
        private const val ROLL_DURATION = 1f
        private const val OPEN_DELAY = 0.45f
        private const val SHOOT_DELAY = 0.65f
        private const val BULLETS_TO_SHOOT = 3
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.0001f
        private var rollRegion: TextureRegion? = null
        private var openRegion: TextureRegion? = null
        private var shootRegion: TextureRegion? = null
        private var closeRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(5),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 5
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 5 else 3
        }
    )
    override lateinit var facing: Facing

    private val rollTimer = Timer(ROLL_DURATION)
    private val openTimer = Timer(OPEN_DELAY)
    private val shootTimer = Timer(SHOOT_DELAY)
    private lateinit var rollingBotState: RollingBotState
    private var bulletsShot = 0

    override fun init() {
        if (rollRegion == null || openRegion == null || shootRegion == null || closeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            rollRegion = atlas.findRegion("RollerBot/Roll")
            openRegion = atlas.findRegion("RollerBot/Open")
            shootRegion = atlas.findRegion("RollerBot/Shoot")
            closeRegion = atlas.findRegion("RollerBot/Close")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        rollTimer.reset()
        openTimer.reset()
        shootTimer.reset()
        rollingBotState = RollingBotState.ROLLING
        bulletsShot = 0
    }

    private fun shoot() {
        requestToPlaySound(SoundAsset.ICE_SHARD_2_SOUND, false)
        val rollingBotShot = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ROLLING_BOT_SHOT)!!
        val position = if (isFacing(Facing.LEFT))
            body.getCenterLeftPoint().add(-0.2f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        else body.getCenterRightPoint().add(0.2f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        rollingBotShot.spawn(
            props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to position,
                ConstKeys.LEFT to isFacing(Facing.LEFT)
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (rollingBotState) {
                RollingBotState.ROLLING -> {
                    body.physics.velocity.x = X_VEL * facing.value * ConstVals.PPM
                    rollTimer.update(delta)
                    if (rollTimer.isFinished()) {
                        rollTimer.reset()
                        rollingBotState = RollingBotState.OPENING
                    }
                }

                RollingBotState.OPENING -> {
                    body.physics.velocity.x = 0f
                    openTimer.update(delta)
                    if (openTimer.isFinished()) {
                        openTimer.reset()
                        rollingBotState = RollingBotState.SHOOTING
                    }
                }

                RollingBotState.SHOOTING -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
                    body.physics.velocity.x = 0f

                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shoot()
                        bulletsShot++
                        shootTimer.reset()
                        if (bulletsShot >= BULLETS_TO_SHOOT) {
                            bulletsShot = 0
                            rollingBotState = RollingBotState.CLOSING
                        }
                    }
                }

                RollingBotState.CLOSING -> {
                    body.physics.velocity.x = 0f
                    openTimer.update(delta)
                    if (openTimer.isFinished()) {
                        openTimer.reset()
                        rollingBotState = RollingBotState.ROLLING
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            var feetFixtureOffset = 0f
            when (rollingBotState) {
                RollingBotState.ROLLING -> {
                    body.setSize(0.5f * ConstVals.PPM)
                    feetFixtureOffset = -0.25f * ConstVals.PPM
                }

                RollingBotState.OPENING,
                RollingBotState.SHOOTING,
                RollingBotState.CLOSING -> {
                    body.setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
                    feetFixtureOffset = -0.575f * ConstVals.PPM
                }
            }

            body.fixtures.forEach {
                val fixture = it.second as Fixture

                if (fixture.type == FixtureType.FEET) {
                    fixture.offsetFromBodyCenter.y = feetFixtureOffset
                    return@forEach
                }

                if (fixture.type == FixtureType.SHIELD)
                    fixture.active = rollingBotState != RollingBotState.SHOOTING
                if (fixture.type == FixtureType.DAMAGEABLE)
                    fixture.active = rollingBotState == RollingBotState.SHOOTING

                val fixtureShape = fixture.rawShape as GameRectangle
                fixtureShape.set(body)
            }

            body.physics.gravity.y = ConstVals.PPM *
                    (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { rollingBotState.name }
        val animations = objectMapOf<String, IAnimation>(
            RollingBotState.ROLLING.name to Animation(rollRegion!!, 2, 4, 0.1f, true),
            RollingBotState.OPENING.name to Animation(openRegion!!, 1, 3, 0.1f, false),
            RollingBotState.SHOOTING.name to Animation(shootRegion!!),
            RollingBotState.CLOSING.name to Animation(closeRegion!!, 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}