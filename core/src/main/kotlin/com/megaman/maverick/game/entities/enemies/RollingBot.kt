package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.DrippingToxicGoop
import com.megaman.maverick.game.entities.projectiles.RollingBotShot
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class RollingBot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    enum class RollingBotState { ROLLING, OPENING, SHOOTING, CLOSING }

    companion object {
        const val TAG = "RollingBot"

        private const val X_VEL = 3f

        private const val ROLL_DURATION = 1f
        private const val OPEN_DELAY = 0.5f

        private const val SHOOT_DELAY = 0.25f
        private const val BULLETS_TO_SHOOT = 3

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val TOXIC_GOOP_DMG_DUR = 0.25f

        private var rollRegion: TextureRegion? = null
        private var openRegion: TextureRegion? = null
        private var shootRegion: TextureRegion? = null
        private var closeRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private lateinit var state: RollingBotState

    private val rolling: Boolean
        get() = state == RollingBotState.ROLLING

    private val rollTimer = Timer(ROLL_DURATION)
    private val openTimer = Timer(OPEN_DELAY)
    private val shootTimer = Timer(SHOOT_DELAY)

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
        damageOverrides.put(DrippingToxicGoop::class, dmgNeg(10))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        rollTimer.reset()
        openTimer.reset()
        shootTimer.reset()

        state = RollingBotState.ROLLING

        bulletsShot = 0
    }

    override fun canBeDamagedBy(damager: IDamager) =
        super.canBeDamagedBy(damager) && (!rolling || damager is DrippingToxicGoop)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        return super.takeDamageFrom(damager)
    }

    override fun getDamageDuration(damager: IDamager) = when (damager) {
        is DrippingToxicGoop -> TOXIC_GOOP_DMG_DUR
        else -> super.getDamageDuration(damager)
    }

    private fun shoot() {
        val position = when {
            isFacing(Facing.LEFT) ->
                body.getPositionPoint(Position.CENTER_LEFT).add(-0.2f * ConstVals.PPM, 0.1f * ConstVals.PPM)

            else -> body.getPositionPoint(Position.CENTER_RIGHT).add(0.2f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        }

        val shot = MegaEntityFactory.fetch(RollingBotShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo position,
                ConstKeys.LEFT pairTo isFacing(Facing.LEFT)
            )
        )

        requestToPlaySound(SoundAsset.ICE_SHARD_2_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                RollingBotState.ROLLING -> {
                    body.physics.velocity.x = X_VEL * facing.value * ConstVals.PPM

                    rollTimer.update(delta)
                    if (body.isSensing(BodySense.FEET_ON_GROUND) && rollTimer.isFinished()) {
                        rollTimer.reset()
                        state = RollingBotState.OPENING
                    }
                }

                RollingBotState.OPENING -> {
                    body.physics.velocity.x = 0f

                    openTimer.update(delta)
                    if (openTimer.isFinished()) {
                        openTimer.reset()
                        state = RollingBotState.SHOOTING
                    }
                }

                RollingBotState.SHOOTING -> {
                    body.physics.velocity.x = 0f

                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shoot()
                        bulletsShot++
                        shootTimer.reset()

                        if (bulletsShot >= BULLETS_TO_SHOOT) {
                            bulletsShot = 0
                            state = RollingBotState.CLOSING
                        }
                    }
                }

                RollingBotState.CLOSING -> {
                    body.physics.velocity.x = 0f

                    openTimer.update(delta)
                    if (openTimer.isFinished()) {
                        openTimer.reset()
                        state = RollingBotState.ROLLING
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            var feetFixtureOffset = 0f
            when (state) {
                RollingBotState.ROLLING -> {
                    body.setSize(ConstVals.PPM.toFloat())
                    feetFixtureOffset = -0.375f * ConstVals.PPM
                }

                RollingBotState.OPENING,
                RollingBotState.SHOOTING,
                RollingBotState.CLOSING -> {
                    body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
                    feetFixtureOffset = -0.675f * ConstVals.PPM
                }
            }

            body.forEachFixture { fixture ->
                fixture as Fixture

                when {
                    fixture.getType() == FixtureType.FEET -> {
                        fixture.offsetFromBodyAttachment.y = feetFixtureOffset
                        return@forEachFixture
                    }

                    fixture.getType() == FixtureType.SHIELD -> fixture.setActive(rolling)
                }

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
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { state.name }
        val animations = objectMapOf<String, IAnimation>(
            RollingBotState.ROLLING.name pairTo Animation(rollRegion!!, 2, 4, 0.1f, true),
            RollingBotState.OPENING.name pairTo Animation(openRegion!!, 1, 3, 0.1f, false),
            RollingBotState.SHOOTING.name pairTo Animation(shootRegion!!),
            RollingBotState.CLOSING.name pairTo Animation(closeRegion!!, 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
