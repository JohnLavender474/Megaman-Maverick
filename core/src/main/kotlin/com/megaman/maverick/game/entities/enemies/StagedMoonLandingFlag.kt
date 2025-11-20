package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.points.Points
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class StagedMoonLandingFlag(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity,
    IFaceable, IScalableGravityEntity {

    companion object {
        const val TAG = "StagedMoonLandingFlag"

        private const val HIDDEN_WIDTH = 0.5f
        private const val HIDDEN_HEIGHT = 0.25f

        private const val RISE_DUR = 0.4f
        private const val FALL_DUR = 0.4f

        private const val UNFURLED_WIDTH = 0.5f
        private const val UNFURLED_HEIGHT = 2f

        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f

        private const val SHIELD_SHOW_DUR = 1f
        private const val SHIELD_BLINK_DUR = 0.1f
        private const val SHIELD_INVINCIBLE_DUR = 0.1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FlagState { HIDDEN, RISE, STAND, FALL }

    override lateinit var facing: Facing
    override var gravityScalar = 1f

    private val loop = Loop(FlagState.entries.toGdxArray())
    private val currentState: FlagState
        get() = loop.getCurrent()
    private val stateTimers = OrderedMap<FlagState, Timer>()

    private var parentId = -1

    private lateinit var shieldDamager1: Fixture
    private val shield1Health = Points(0, 100, 100)
    private val shield1Invincible = Timer(SHIELD_INVINCIBLE_DUR)
    private val shield1Timer = Timer(SHIELD_SHOW_DUR)
    private val shield1BlinkTimer = Timer(SHIELD_BLINK_DUR)
    private var shield1Blink = false

    private lateinit var shieldDamager2: Fixture
    private val shield2Health = Points(0, 100, 100)
    private val shield2Invincible = Timer(SHIELD_INVINCIBLE_DUR)
    private val shield2Timer = Timer(SHIELD_SHOW_DUR)
    private val shield2BlinkTimer = Timer(SHIELD_BLINK_DUR)
    private var shield2Blink = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            FlagState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            for (i in 1..3) {
                val key = "${ConstKeys.SHIELD}$i"
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        if (stateTimers.isEmpty) {
            stateTimers.put(FlagState.RISE, Timer(RISE_DUR))
            stateTimers.put(FlagState.FALL, Timer(FALL_DUR))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(Wanaan::class, dmgNeg(ConstVals.MAX_HEALTH))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        parentId = spawnProps.getOrDefault("${ConstKeys.PARENT}_${ConstKeys.ID}", -1, Int::class)

        body.setSize(HIDDEN_WIDTH * ConstVals.PPM, HIDDEN_HEIGHT * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse).scl(movementScalar)

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        shield1Timer.setToEnd()
        shield1Health.setToMax()
        shield1Invincible.setToEnd()
        shield1BlinkTimer.reset()
        shield1Blink = false

        shield2Timer.setToEnd()
        shield2Health.setToMax()
        shield2Invincible.setToEnd()
        shield2BlinkTimer.reset()
        shield2Blink = false

        FacingUtils.setFacingOf(this)

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        AstroAssAssaulter.FLAGS.remove(parentId)
    }

    override fun onHealthDepleted() = setToBeDestroyed()

    override fun canBeDamagedBy(damager: IDamager) = damager is Wanaan || super.canBeDamagedBy(damager)

    internal fun setToBeDestroyed() {
        GameLogger.debug(TAG, "setToBeDestroyed()")
        if (currentState == FlagState.HIDDEN) {
            destroy()
            return
        }
        if (currentState != FlagState.FALL) loop.setIndex(FlagState.FALL.ordinal)
    }

    private fun resetBodySizeOnUnfurling() {
        val oldPos = body.getPositionPoint(Position.BOTTOM_CENTER)
        val oldBounds = body.getBounds()

        body.setSize(UNFURLED_WIDTH * ConstVals.PPM, UNFURLED_HEIGHT * ConstVals.PPM)
        body.setBottomCenterToPoint(oldPos)

        val newBounds = body.getBounds()

        GameLogger.debug(TAG, "resetBodySizeOnUnfurling(): oldBounds=$oldBounds, newBounds=$newBounds")
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            shield1Timer.update(delta)
            if (!shield1Timer.isFinished()) {
                shield1BlinkTimer.update(delta)
                if (shield1BlinkTimer.isFinished()) {
                    shield1Blink = !shield1Blink
                    shield1BlinkTimer.reset()
                }
            }
            shield1Invincible.update(delta)

            shield2Timer.update(delta)
            if (!shield2Timer.isFinished()) {
                shield2BlinkTimer.update(delta)
                if (shield2BlinkTimer.isFinished()) {
                    shield2Blink = !shield2Blink
                    shield2BlinkTimer.reset()
                }
            }
            shield2Invincible.update(delta)

            FacingUtils.setFacingOf(this)

            when (currentState) {
                FlagState.HIDDEN -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        requestToPlaySound(SoundAsset.BRUSH_SOUND, false)
                        resetBodySizeOnUnfurling()
                        loop.next()
                    }
                }
                FlagState.RISE -> {
                    val timer = stateTimers[currentState]
                    timer.update(delta)
                    if (timer.isFinished()) loop.next()
                }
                FlagState.STAND -> {}
                FlagState.FALL -> {
                    val timer = stateTimers[currentState]
                    timer.update(delta)
                    if (timer.isFinished()) destroy()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val value = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.set(0f, -value * ConstVals.PPM * gravityScalar)
        }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        body.addFixture(damageableFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.attachedToBody = false
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val shieldFixture1 = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        shieldFixture1.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) {
                shield1Timer.reset()
                if (shield1Invincible.isFinished()) {
                    val damage = if (projectile is MoonScythe) 15 else 10
                    shield1Health.translate(-damage)

                    shield1Invincible.reset()

                    requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
                }
            }
        }
        shieldFixture1.offsetFromBodyAttachment.x = -0.5f * ConstVals.PPM
        body.addFixture(shieldFixture1)
        shieldFixture1.drawingColor = Color.ORANGE
        debugShapes.add { shieldFixture1 }

        val shieldFixture2 = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        shieldFixture2.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) {
                shield2Timer.reset()
                if (shield2Invincible.isFinished()) {
                    val damage = if (projectile is MoonScythe) 15 else 10
                    shield1Health.translate(-damage)

                    shield1Invincible.reset()

                    requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
                }
            }
        }
        shieldFixture2.offsetFromBodyAttachment.x = 0.5f * ConstVals.PPM
        body.addFixture(shieldFixture2)
        shieldFixture2.drawingColor = Color.ORANGE
        debugShapes.add { shieldFixture2 }

        shieldDamager1 = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        shieldDamager1.setHitByDamageableReceiver { damageable, _ ->
            if (damageable == megaman) shield1Timer.reset()
        }
        shieldDamager1.attachedToBody = false
        body.addFixture(shieldDamager1)

        shieldDamager2 = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        shieldDamager2.setHitByDamageableReceiver { damageable, _ ->
            if (damageable == megaman) shield2Timer.reset()
        }
        shieldDamager2.attachedToBody = false
        body.addFixture(shieldDamager2)

        val fixturesToResizeToBody = gdxArrayOf(bodyFixture, damagerFixture, damageableFixture)
        body.preProcess.put(ConstKeys.DEFAULT) {
            fixturesToResizeToBody.forEach { fixture ->
                val bounds = fixture.rawShape as GameRectangle
                bounds.set(body)
            }

            val bounds = feetFixture.rawShape as GameRectangle
            bounds.positionOnPoint(body.getPositionPoint(Position.BOTTOM_CENTER), Position.CENTER)

            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f

            val shieldsActive = currentState == FlagState.STAND
            shieldFixture1.setActive(shieldsActive && !shield1Health.isMin())
            shieldDamager1.setActive(shieldsActive && !shield1Health.isMin())
            shieldFixture2.setActive(shieldsActive && !shield2Health.isMin())
            shieldDamager2.setActive(shieldsActive && !shield2Health.isMin())

            val shieldDamager1Bounds = shieldDamager1.rawShape as GameRectangle
            shieldDamager1Bounds.set(shieldFixture1.getShape() as GameRectangle)

            val shieldDamager2Bounds = shieldDamager2.rawShape as GameRectangle
            shieldDamager2Bounds.set(shieldFixture2.getShape() as GameRectangle)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, doUpdate = { !game.isCameraRotating() })
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        // flag
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        // shield 1
        .sprite(
            "${ConstKeys.SHIELD}_1",
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.hidden = shield1Timer.isFinished() || shield1Blink || currentState != FlagState.STAND
            sprite.setCenter(shieldDamager1.getShape().getCenter())
        }
        // shield 2
        .sprite(
            "${ConstKeys.SHIELD}_2",
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.hidden = shield2Timer.isFinished() || shield2Blink || currentState != FlagState.STAND
            sprite.setCenter(shieldDamager2.getShape().getCenter())
            sprite.setFlip(true, false)
        }
        // build
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        // flag
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animations.putAll(
                        "stand" pairTo Animation(regions["stand"]),
                        "rise" pairTo Animation(regions["rise"], 3, 1, 0.1f, false),
                        "fall" pairTo Animation(regions["fall"], 3, 1, 0.1f, false),
                        "hidden" pairTo Animation(regions["hidden"])
                    )
                }
                .build()
        )
        // shield 1
        .key("${ConstKeys.SHIELD}_1")
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        shield1Health.current >= 75 -> "${ConstKeys.SHIELD}1"
                        shield1Health.current >= 25 -> "${ConstKeys.SHIELD}2"
                        else -> "${ConstKeys.SHIELD}3"
                    }
                }
                .applyToAnimations { animations ->
                    for (i in 1..3) {
                        val key = "${ConstKeys.SHIELD}$i"
                        animations.put(key, Animation(regions[key], 3, 1, 0.1f, true))
                    }
                }
                .build()
        )
        // shield 2
        .key("${ConstKeys.SHIELD}_2")
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        shield2Health.current >= 75 -> "${ConstKeys.SHIELD}1"
                        shield2Health.current >= 25 -> "${ConstKeys.SHIELD}2"
                        else -> "${ConstKeys.SHIELD}3"
                    }
                }
                .applyToAnimations { animations ->
                    for (i in 1..3) {
                        val key = "${ConstKeys.SHIELD}$i"
                        animations.put(key, Animation(regions[key], 3, 1, 0.1f, true))
                    }
                }
                .build()
        )
        // build
        .build()
}
