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
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.GutsTank.GutsTankAttackState
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.HeliMet
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class GutsTankFist(game: MegamanMaverickGame) : AbstractEnemy(game, dmgDuration = DAMAGE_DURATION, size = Size.MEDIUM),
    IChildEntity, IDrawableShapesEntity, IFaceable {

    enum class GutsTankFistState { ATTACHED, LAUNCHED, RETURNING }

    companion object {
        const val TAG = "GutsTankFist"
        private const val DAMAGE_DURATION = 0.75f
        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED = 10f
        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 2f
        private const val FIST_OFFSET_X = -2.35f
        private const val FIST_OFFSET_Y = 0.75f
        private var fistRegion: TextureRegion? = null
        private var launchedRegion: TextureRegion? = null
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
        if (launchedRegion == null || fistRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            fistRegion = atlas.findRegion("GutsTank/Fist")
            launchedRegion = atlas.findRegion("GutsTank/FistLaunched")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
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
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            explosion.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_1_SOUND
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

    override fun onDamageInflictedTo(damageable: IDamageable) = (parent as GutsTank).laugh()

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
                        target.set(megaman.body.getPositionPoint(Position.CENTER_LEFT))
                        requestToPlaySound(SoundAsset.BURST_SOUND, false)
                    }

                    val velocity = GameObjectPools.fetch(Vector2::class)
                        .set(target)
                        .sub(body.getCenter())
                        .nor()
                        .scl(LAUNCH_SPEED * ConstVals.PPM)
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
                        val velocity = GameObjectPools.fetch(Vector2::class)
                            .set(attachment)
                            .sub(body.getCenter())
                            .nor()
                            .scl(RETURN_SPEED * ConstVals.PPM)
                        body.physics.velocity.set(velocity)

                        if (body.getBounds().contains(attachment)) {
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
        body.setSize(1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(1.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.offsetFromBodyAttachment.x = 0.65f * facing.value * ConstVals.PPM
            damageableFixture.offsetFromBodyAttachment.x = 0.75f * -facing.value * ConstVals.PPM
            damagerFixture.offsetFromBodyAttachment.x = 0.5f * -facing.value * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            val position = when (fistState) {
                GutsTankFistState.ATTACHED, GutsTankFistState.LAUNCHED -> Position.CENTER_LEFT

                GutsTankFistState.RETURNING -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (fistState) {
                GutsTankFistState.ATTACHED -> "fist"
                else -> "launched"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "fist" pairTo Animation(fistRegion!!),
            "launched" pairTo Animation(launchedRegion!!, 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
