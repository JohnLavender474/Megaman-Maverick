package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.coerceX
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.entities.projectiles.Cokonut
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Cokeyro(game: MegamanMaverickGame) : AbstractEnemy(game), IFreezableEntity, IAnimatedEntity, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "Cokeyro"

        private const val SHAKE_DELAY = 1.5f
        private const val SHAKE_TIMES = 5
        private const val SHAKE_DUR = 0.4f
        private const val SHAKE_THROW_TIME = 0.3f
        private const val SHAKE_SCANNER_RADIUS = 6f

        private const val DIE_DUR = 1f
        private const val EXPLODE_DELAY = 0.1f

        private const val COKO_IMPULSE_Y = 12f
        private const val COKO_MAX_IMPULSE_X = 10f

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "shake" pairTo AnimationDef(2, 2, 0.1f, true),
            "die" pairTo AnimationDef(2, 1, 0.1f, true),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CokeyroState { IDLE, SHAKE, DIE }

    override val invincible: Boolean
        get() = state == CokeyroState.DIE || super.invincible
    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private lateinit var state: CokeyroState

    private val shakeScanner = GameCircle().setRadius(SHAKE_SCANNER_RADIUS * ConstVals.PPM)
    private val shakeDelay = Timer(SHAKE_DELAY)
    private val shakeTimer = Timer(SHAKE_DUR).addRunnable(TimeMarkedRunnable(SHAKE_THROW_TIME) { throwCokonut() })
    private var shakeTimes = 0

    private val dieTimer = Timer(DIE_DUR)
    private val explodeTimer = Timer(EXPLODE_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { shakeScanner }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        state = CokeyroState.IDLE

        shakeDelay.setToEnd()
        shakeTimer.reset()
        shakeTimes = 0

        dieTimer.reset()
        explodeTimer.reset()

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        GameLogger.debug(TAG, "takeDamageFrom(): damaged=$damaged, damager=$damager")
        if (damaged) startShaking()
        return damaged
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        state = CokeyroState.DIE
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) return@add

            when (state) {
                CokeyroState.IDLE -> {
                    FacingUtils.setFacingOf(this)

                    shakeScanner.setCenter(body.getCenter())

                    shakeDelay.update(delta)
                    if (shakeDelay.isFinished() && shouldStartShaking()) startShaking()
                }
                CokeyroState.SHAKE -> {
                    FacingUtils.setFacingOf(this)

                    shakeTimer.update(delta)
                    if (shakeTimer.isFinished()) onShakeTimerFinished()
                }
                CokeyroState.DIE -> {
                    explodeTimer.update(delta)
                    if (explodeTimer.isFinished()) {
                        explode(
                            props(
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.POSITION pairTo body.getBounds().getRandomPositionInBounds()
                            )
                        )
                        explodeTimer.reset()
                    }

                    dieTimer.update(delta)
                    if (dieTimer.isFinished()) {
                        explode()
                        destroy()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixtureTypes = gdxArrayOf(FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        headFixtureTypes.forEach { headFixtureType ->
            val headFixture =
                Fixture(body, headFixtureType, GameRectangle().setSize(1.5f * ConstVals.PPM, ConstVals.PPM.toFloat()))
            body.addFixture(headFixture)

            headFixture.drawingColor = Color.BLUE
            debugShapes.add { headFixture }

            body.preProcess.put("${ConstKeys.HEAD}_${headFixtureType.name.lowercase()}") preProcess@{
                val active = state != CokeyroState.DIE
                headFixture.setActive(active)

                if (!active) return@preProcess

                val offsetX: Float
                val offsetY: Float

                val shakeTime = shakeTimer.time

                if (state != CokeyroState.SHAKE ||
                    (shakeTime >= 0.1f && shakeTime < 0.2f) ||
                    (shakeTime >= 0.3f)
                ) {
                    offsetX = 0f
                    offsetY = 0.4f
                } else if (shakeTime < 0.1f) {
                    offsetX = 0.25f * -facing.value
                    offsetY = 0.3f
                } else {
                    offsetX = 0.25f * facing.value
                    offsetY = 0.3f
                }

                headFixture.offsetFromBodyAttachment.set(offsetX, offsetY).scl(ConstVals.PPM.toFloat())
            }
        }

        val trunkFixtureTypes = gdxArrayOf(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.SHIELD)
        trunkFixtureTypes.forEach { trunkFixtureType ->
            val trunkBounds = GameRectangle().setSize(0.375f * ConstVals.PPM, ConstVals.PPM.toFloat())

            val trunkFixture = Fixture(body, trunkFixtureType, trunkBounds)
            body.addFixture(trunkFixture)

            trunkFixture.attachedToBody = false

            trunkFixture.drawingColor = Color.ORANGE
            debugShapes.add { trunkFixture }

            if (trunkFixtureType == FixtureType.BODY) trunkFixture.setHitByProjectileReceiver { projectile ->
                if (state != CokeyroState.DIE && projectile.owner == megaman) startShaking()
            }

            body.preProcess.put("trunk_${trunkFixtureType.name.lowercase()}") {
                val position = body.getPositionPoint(Position.BOTTOM_CENTER)
                trunkBounds.setBottomCenterToPoint(position)
            }
        }


        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (frozen) "frozen" else state.name.lowercase() }
                .applyToAnimations { animations -> AnimationUtils.loadAnimationDefs(animDefs, animations, regions) }
                .build()
        )
        .build()

    private fun shouldStartShaking() = megaman.body.getBounds().overlaps(shakeScanner)

    private fun startShaking() {
        GameLogger.debug(TAG, "startShaking()")
        state = CokeyroState.SHAKE
        shakeTimer.reset()
        shakeTimes = 0
    }

    private fun onShakeTimerFinished() {
        GameLogger.debug(TAG, "onShakeTimerFinished()")
        if (shakeTimes >= SHAKE_TIMES) {
            shakeDelay.reset()
            state = CokeyroState.IDLE
        } else {
            shakeTimer.reset()
            shakeTimes++
        }
    }

    private fun throwCokonut() {
        GameLogger.debug(TAG, "throwCokonut()")

        val position = body.getCenter().add(0.25f * facing.value * ConstVals.PPM, 0.35f * ConstVals.PPM)

        val impulse = MegaUtilMethods.calculateJumpImpulse(
            position,
            megaman.body.getBounds().getCenter(),
            COKO_IMPULSE_Y * ConstVals.PPM
        )
        impulse.coerceX(-COKO_MAX_IMPULSE_X * ConstVals.PPM, COKO_MAX_IMPULSE_X * ConstVals.PPM)

        val cokonut = MegaEntityFactory.fetch(Cokonut::class)!!
        cokonut.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position
            )
        )

        requestToPlaySound(SoundAsset.BRUSH_SOUND, false)
    }
}
