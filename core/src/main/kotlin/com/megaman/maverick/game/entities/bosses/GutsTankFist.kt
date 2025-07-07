package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IChildEntity
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
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTank.GutsTankAttackState
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.HeliMet
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.MagmaWave
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class GutsTankFist(game: MegamanMaverickGame) : AbstractEnemy(game, dmgDuration = DAMAGE_DURATION, size = Size.MEDIUM),
    IChildEntity, IDrawableShapesEntity, IFaceable {

    enum class GutsTankFistState { ATTACHED, LAUNCHED, RETURNING }

    companion object {
        const val TAG = "GutsTankFist"

        private const val DAMAGE_DURATION = 0.75f

        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED_IN_CAM = 10f
        private const val LAUNCH_SPEED_OUT_OF_CAM = 15f

        private const val RETURN_DELAY = 0.5f
        private const val RETURN_SPEED_IN_CAM = 6f
        private const val RETURN_SPEED_OUT_OF_CAM = 15f

        private const val FIST_OFFSET_X = -4f
        private const val FIST_OFFSET_Y = 2f

        private const val LAUNCH_DIST = 16f

        private var fistRegion: TextureRegion? = null
        private var launchedRegion: TextureRegion? = null
    }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager): Int {
            if (damager !is IProjectileEntity || damager.owner != megaman) return 0
            return when (damager) {
                is ChargedShot -> if (damager.fullyCharged) 3 else 2
                is MoonScythe, is MagmaWave, is Axe -> 4
                else -> 2
            }
        }
    }

    override var parent: IGameEntity? = null
    override lateinit var facing: Facing

    internal lateinit var fistState: GutsTankFistState

    private val launchDelayTimer = Timer(LAUNCH_DELAY)
    private val returnDelayTimer = Timer(RETURN_DELAY)

    private val target = Vector2()

    private val attachment: Vector2
        get() = (parent as GutsTank).body.getCenter().add(FIST_OFFSET_X * ConstVals.PPM, FIST_OFFSET_Y * ConstVals.PPM)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (launchedRegion == null || fistRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            fistRegion = atlas.findRegion("${GutsTank.TAG}/fist")
            launchedRegion = atlas.findRegion("${GutsTank.TAG}/fist_launched")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        super.onSpawn(spawnProps)
        parent = spawnProps.get(ConstKeys.PARENT, GutsTank::class)
        fistState = GutsTankFistState.ATTACHED
        facing = Facing.LEFT
    }

    override fun onDestroy() {
        super.onDestroy()
        if (getCurrentHealth() <= ConstVals.MIN_HEALTH) {
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
                )
            )
        }
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (damager is AbstractProjectile) {
            damager.owner?.let { damagerOwner ->
                if (damagerOwner == this ||
                    damagerOwner == parent ||
                    damagerOwner == (parent as GutsTank).tankBlock ||
                    (damagerOwner is Met && (parent as GutsTank).runningMets.contains(damagerOwner)) ||
                    (damagerOwner is HeliMet && (parent as GutsTank).heliMets.contains(damagerOwner))
                ) return false
            }
        }
        return super.canBeDamagedBy(damager)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable == megaman) (parent as GutsTank).laugh()
    }

    internal fun launch() {
        facing = if (megaman.body.getX() < body.getCenter().x) Facing.LEFT else Facing.RIGHT
        fistState = GutsTankFistState.LAUNCHED
        launchDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (fistState) {
                GutsTankFistState.ATTACHED -> {
                    facing = Facing.LEFT
                    body.physics.velocity.setZero()
                    body.setCenter(attachment)
                }
                GutsTankFistState.LAUNCHED -> {
                    launchDelayTimer.update(delta)
                    if (!launchDelayTimer.isFinished()) {
                        body.physics.velocity.setZero()
                        return@add
                    }
                    if (launchDelayTimer.isJustFinished()) {
                        val direction = megaman.body.getCenter()
                            .sub(body.getCenter())
                            .nor()
                            .scl(LAUNCH_DIST * ConstVals.PPM)
                        target.set(body.getCenter().add(direction))

                        requestToPlaySound(SoundAsset.BURST_SOUND, false)
                    }

                    val speed = when {
                        game.getGameCamera().getRotatedBounds().overlaps(body.getBounds()) -> LAUNCH_SPEED_IN_CAM
                        else -> LAUNCH_SPEED_OUT_OF_CAM
                    }

                    val velocity = GameObjectPools.fetch(Vector2::class)
                        .set(target)
                        .sub(body.getCenter())
                        .nor()
                        .scl(speed * ConstVals.PPM)
                    body.physics.velocity.set(velocity)

                    if (body.getBounds().contains(target)) {
                        GameLogger.debug(TAG, "Fist hit target")
                        fistState = GutsTankFistState.RETURNING
                        returnDelayTimer.reset()
                    }
                }
                GutsTankFistState.RETURNING -> {
                    facing = if (attachment.x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

                    returnDelayTimer.update(delta)

                    if (returnDelayTimer.isFinished()) {
                        val speed = when {
                            game.getGameCamera().getRotatedBounds().overlaps(body.getBounds()) -> RETURN_SPEED_IN_CAM
                            else -> RETURN_SPEED_OUT_OF_CAM
                        }

                        val velocity = GameObjectPools.fetch(Vector2::class)
                            .set(attachment)
                            .sub(body.getCenter())
                            .nor()
                            .scl(speed * ConstVals.PPM)
                        body.physics.velocity.set(velocity)

                        if (body.getBounds().getCenter().epsilonEquals(attachment, 0.1f * ConstVals.PPM)) {
                            fistState = GutsTankFistState.ATTACHED
                            (parent as GutsTank).finishAttack(GutsTankAttackState.LAUNCH_FIST)
                        }
                    } else body.physics.velocity.setZero()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        // debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(bodyFixture)
        // debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(1.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        // debugShapes.add { damagerFixture }

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        // debugShapes.add { damageableFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        // debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.offsetFromBodyAttachment.x = 0.65f * facing.value * ConstVals.PPM
            damageableFixture.offsetFromBodyAttachment.x = 0.75f * -facing.value * ConstVals.PPM
            damagerFixture.offsetFromBodyAttachment.x = 0.5f * -facing.value * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(6f * ConstVals.PPM, 3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (fistState) {
                GutsTankFistState.ATTACHED -> "fist"
                else -> "launched"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "fist" pairTo Animation(fistRegion!!),
            "launched" pairTo Animation(launchedRegion!!, 2, 2, 0.05f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
