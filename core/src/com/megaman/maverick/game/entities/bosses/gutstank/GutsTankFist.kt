package com.megaman.maverick.game.entities.bosses.gutstank

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.enemies.HeliMet
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot

import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class GutsTankFist(game: MegamanMaverickGame) : AbstractEnemy(game, dmgDuration = DAMAGE_DURATION), IChildEntity,
    IDrawableShapesEntity, IFaceable {

    enum class GutsTankFistState {
        ATTACHED, LAUNCHED, RETURNING
    }

    companion object {
        const val TAG = "GutsTankFist"
        private const val DAMAGE_DURATION = 0.75f
        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED = 10f
        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 2f
        private const val FIST_OFFSET_X = -2.5f
        private const val FIST_OFFSET_Y = 0.65f
        private var fistRegion: TextureRegion? = null
        private var launchedRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 3 else 2
        }, ChargedShotExplosion::class to dmgNeg(1)
    )
    override var parent: IGameEntity? = null
    override lateinit var facing: Facing

    internal lateinit var state: GutsTankFistState

    private val launchDelayTimer = Timer(LAUNCH_DELAY)
    private val returnDelayTimer = Timer(RETURN_DELAY)

    private lateinit var target: Vector2

    private val attachment: Vector2
        get() = (parent as GutsTank).body.getCenter().add(FIST_OFFSET_X * ConstVals.PPM, FIST_OFFSET_Y * ConstVals.PPM)

    override fun init() {
        if (launchedRegion == null || fistRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            fistRegion = atlas.findRegion("GutsTank/Fist")
            launchedRegion = atlas.findRegion("GutsTank/FistLaunched")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        super.spawn(spawnProps)
        parent = spawnProps.get(ConstKeys.PARENT, GutsTank::class)
        state = GutsTankFistState.ATTACHED
        facing = Facing.LEFT
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (getCurrentHealth() <= ConstVals.MIN_HEALTH) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            game.engine.spawn(
                explosion, props(
                    ConstKeys.POSITION to body.getCenter(), ConstKeys.SOUND to SoundAsset.EXPLOSION_1_SOUND
                )
            )
        }
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (damager is AbstractProjectile) {
            damager.owner?.let { damagerOwner ->
                if (damagerOwner == this || damagerOwner == parent ||
                    damagerOwner == (parent as GutsTank).tankBlock ||
                    damagerOwner == (parent as GutsTank).bodyBlock ||
                    (damagerOwner is Met && (parent as GutsTank).runningMetsSet.contains(damagerOwner)) ||
                    (damagerOwner is HeliMet && (parent as GutsTank).flyingMetsSet.contains(damagerOwner))
                ) return false
            }
        }
        return super.canBeDamagedBy(damager)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = (parent as GutsTank).laugh()

    internal fun launch() {
        facing = if (getMegaman().body.x < body.getCenter().x) Facing.LEFT else Facing.RIGHT
        state = GutsTankFistState.LAUNCHED
        launchDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
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
                        target = game.megaman.body.getCenter()
                        requestToPlaySound(SoundAsset.BURST_SOUND, false)
                    }
                    body.physics.velocity = target.cpy().sub(body.getCenter()).nor().scl(LAUNCH_SPEED * ConstVals.PPM)
                    if (body.contains(target)) {
                        GameLogger.debug(TAG, "Fist hit target")
                        state = GutsTankFistState.RETURNING
                        returnDelayTimer.reset()
                    }
                }

                GutsTankFistState.RETURNING -> {
                    facing = if (attachment.x < body.getCenter().x) Facing.LEFT else Facing.RIGHT
                    returnDelayTimer.update(delta)
                    if (returnDelayTimer.isFinished()) {
                        body.physics.velocity = attachment.cpy().sub(body.getCenter()).nor().scl(
                            RETURN_SPEED * ConstVals.PPM
                        )
                        if (body.contains(attachment)) {
                            state = GutsTankFistState.ATTACHED
                            (parent as GutsTank).finishAttack(GutsTank.GutsTankAttackState.LAUNCH_FIST)
                        }
                    } else body.physics.velocity.setZero()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(1.05f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(1.05f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.offsetFromBodyCenter.x = 0.2f * facing.value * ConstVals.PPM
            damageableFixture.offsetFromBodyCenter.x = 0.75f * -facing.value * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val position = when (state) {
                GutsTankFistState.ATTACHED, GutsTankFistState.LAUNCHED -> Position.CENTER_LEFT

                GutsTankFistState.RETURNING -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                GutsTankFistState.ATTACHED -> "fist"
                else -> "launched"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "fist" to Animation(fistRegion!!),
            "launched" to Animation(launchedRegion!!, 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}