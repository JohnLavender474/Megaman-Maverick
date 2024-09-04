package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
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
        private const val IDLE_DUR = 1f
        private const val MAX_CHILDREN = 3
        private const val LAUNCH_PENGUINS_DUR = 0.75f
        private const val SHOOT_SNOWBALLS_DUR = 0.5f
        private const val SNOWBALL_IMPULSE_Y = 10f
        private const val SNOWBALL_GRAVITY = -0.15f
        private const val SNOWBALLS_TO_LAUNCH = 3
        private var region: TextureRegion? = null
    }

    private enum class PenguinMiniBossState { IDLE, LAUNCH_PENGUINS, SHOOT_SNOWBALLS }

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
            if (it.fullyCharged) 2 else 1
        }
    )
    override var children = Array<GameEntity>()
    override lateinit var facing: Facing

    private var launchPenguins = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.BOSSES.source, "$TAG/$TAG")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MINI, true)
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        penguinMiniBossState = PenguinMiniBossState.IDLE
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)
        facing = if (left) Facing.LEFT else Facing.RIGHT
        snowballsLaunched = 0
        launchPenguins = false
    }

    override fun onDestroy() {
        super.onDestroy()
        children.forEach { it.destroy() }
        children.clear()
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        children.forEach { it.destroy() }
        children.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next()
                if (!(child as MegaGameEntity).spawned) iter.remove()
            }

            when (penguinMiniBossState) {
                PenguinMiniBossState.IDLE -> {
                    idleTimer.update(delta)
                    if (idleTimer.isFinished()) {
                        idleTimer.reset()
                        penguinMiniBossState =
                            if (launchPenguins) PenguinMiniBossState.LAUNCH_PENGUINS else PenguinMiniBossState.SHOOT_SNOWBALLS
                        launchPenguins = !launchPenguins
                    }
                }

                PenguinMiniBossState.LAUNCH_PENGUINS -> {
                    if (children.size >= MAX_CHILDREN) {
                        penguinMiniBossState = PenguinMiniBossState.IDLE
                        return@add
                    }
                    launchPenguinsTimer.update(delta)
                    if (launchPenguinsTimer.isFinished()) {
                        launchBabyPenguin()
                        penguinMiniBossState = PenguinMiniBossState.IDLE
                        launchPenguinsTimer.reset()
                    }
                }

                PenguinMiniBossState.SHOOT_SNOWBALLS -> {
                    if (snowballsLaunched >= SNOWBALLS_TO_LAUNCH) {
                        snowballsLaunched = 0
                        penguinMiniBossState = PenguinMiniBossState.IDLE
                    }
                    shootSnowballsTimer.update(delta)
                    if (shootSnowballsTimer.isFinished()) {
                        shootSnowball()
                        snowballsLaunched++
                        shootSnowballsTimer.reset()
                    }
                }
            }
        }
    }

    private fun shootSnowball() {
        val snowball = EntityFactories.fetch(EntityType.PROJECTILE, "Snowball")!! as Snowball
        val spawn = body.getBottomCenterPoint().add(0f, 0.15f * ConstVals.PPM)
        val impulseX = (getMegaman().body.x - body.x) * 1.5f
        val impulseY = SNOWBALL_IMPULSE_Y * ConstVals.PPM
        val trajectory = Vector2(impulseX, impulseY)
        val gravity = Vector2(0f, SNOWBALL_GRAVITY * ConstVals.PPM)
        snowball.spawn(
            props(
                ConstKeys.POSITION to spawn,
                ConstKeys.TRAJECTORY to trajectory,
                ConstKeys.GRAVITY_ON to true,
                ConstKeys.GRAVITY to gravity,
                ConstKeys.OWNER to this
            )
        )
        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun launchBabyPenguin() {
        val penguin = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.BABY_PENGUIN)!! as BabyPenguin
        val spawn = body.getBottomCenterPoint().add(0f, 0.15f * ConstVals.PPM)
        penguin.spawn(
            props(
                ConstKeys.POSITION to spawn,
                ConstKeys.LEFT to (facing == Facing.LEFT)
            )
        )
        children.add(penguin)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2.5f * ConstVals.PPM, 3f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2f * ConstVals.PPM))
        shieldFixture.offsetFromBodyCenter.y = -0.35f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.35f * ConstVals.PPM))
        damageableFixture.offsetFromBodyCenter.y = 1.25f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 8, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}