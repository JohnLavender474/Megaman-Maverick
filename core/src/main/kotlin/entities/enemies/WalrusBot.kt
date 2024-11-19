package com.megaman.maverick.game.entities.enemies


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.set
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.megaman.maverick.game.entities.contracts.megaman
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
import kotlin.math.abs
import kotlin.reflect.KClass

class WalrusBot(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IAnimatedEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "WalrusBot"
        private const val SHOOT_DUR = 0.8f
        private const val STAND_DUR = 1.25f
        private const val JET_DUR = 0.35f
        private const val JET_IMPULSE = 15f
        private const val SLIDE_MIN_VEL = 0.2f
        private const val VEL_CLAMP = 10f
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f
        private const val SNOWBALL_X = 8f
        private const val SNOWBALL_Y = 5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class WalrusBotState { STAND, SLIDE, SHOOT, JET }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(5),
        Fireball::class pairTo dmgNeg(15),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override lateinit var facing: Facing

    private val scannerBox = GameRectangle().setSize(10f * ConstVals.PPM, ConstVals.PPM.toFloat())
    private val shootTimer = Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(0.1f) { shoot() }))
    private val standTimer = Timer(STAND_DUR)
    private val jetTimer = Timer(JET_DUR)
    private lateinit var walrusBotState: WalrusBotState
    private lateinit var stateQueudForAfterShoot: WalrusBotState

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("stand", atlas.findRegion("$TAG/stand"))
            regions.put("shoot", atlas.findRegion("$TAG/shoot"))
            regions.put("jet", atlas.findRegion("$TAG/jet"))
            regions.put("slide", atlas.findRegion("$TAG/slide"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { scannerBox }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        walrusBotState = WalrusBotState.STAND
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    private fun shoot() {
        val snowball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL)!!
        val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0.375f * ConstVals.PPM)
        val trajectory = Vector2(SNOWBALL_X * ConstVals.PPM * facing.value, SNOWBALL_Y * ConstVals.PPM)
        val gravity = Vector2(0f, GRAVITY * ConstVals.PPM)
        snowball.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.GRAVITY pairTo gravity,
                ConstKeys.GRAVITY_ON pairTo true,
                ConstKeys.OWNER pairTo this
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (walrusBotState) {
                WalrusBotState.STAND -> {
                    facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
                    standTimer.update(delta)
                    if (standTimer.isFinished()) {
                        standTimer.reset()
                        walrusBotState = WalrusBotState.SHOOT
                        stateQueudForAfterShoot = WalrusBotState.STAND
                        val scannerBoxPosition =
                            if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
                        scannerBox.positionOnPoint(body.getPositionPoint(scannerBoxPosition), scannerBoxPosition)
                    }
                }

                WalrusBotState.SHOOT -> {
                    if (scannerBox.overlaps(megaman.body as Rectangle)) stateQueudForAfterShoot =
                        WalrusBotState.JET
                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shootTimer.reset()
                        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
                        walrusBotState = stateQueudForAfterShoot
                    }
                }

                WalrusBotState.JET -> {
                    body.physics.velocity.x += JET_IMPULSE * ConstVals.PPM * delta * facing.value
                    jetTimer.update(delta)
                    if (jetTimer.isFinished()) {
                        jetTimer.reset()
                        walrusBotState = WalrusBotState.SLIDE
                    }
                }

                WalrusBotState.SLIDE -> {
                    facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
                    if (body.isSensing(BodySense.FEET_ON_GROUND) &&
                        abs(body.physics.velocity.x) < SLIDE_MIN_VEL * ConstVals.PPM
                    ) walrusBotState = WalrusBotState.STAND
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.35f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.625f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.25f * ConstVals.PPM, 1.4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { walrusBotState.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.15f), true),
            "shoot" pairTo Animation(regions["shoot"], 7, 1, 0.1f, false),
            "jet" pairTo Animation(regions["jet"], 2, 2, 0.1f, true),
            "slide" pairTo Animation(regions["slide"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
