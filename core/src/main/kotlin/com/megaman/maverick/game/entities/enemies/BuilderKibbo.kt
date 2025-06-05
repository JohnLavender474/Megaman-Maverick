package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.coerceX
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.KibboHammer
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class BuilderKibbo(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BuilderKibbo"

        private const val STAND_DUR = 1f
        private const val PRE_THROW_DUR = 0.25f
        private const val THROW_DUR = 0.5f

        private const val HAMMER_MAX_IMPULSE_X = 10f
        private const val HAMMER_IMPULSE_Y = 14f
        private const val HAMMER_MIN_IMPULSE_Y = 1f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAV = -0.01f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(),
            "jump" pairTo AnimationDef(),
            "pre_throw" pairTo AnimationDef(),
            "throw" pairTo AnimationDef(3, 1, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class BuilderKibboState { STAND, PRE_THROW, THROW }

    override lateinit var facing: Facing

    private val loop = Loop(BuilderKibboState.entries.toGdxArray())
    private val currentState: BuilderKibboState
        get() = loop.getCurrent()
    private val stateTimers = orderedMapOf(
        BuilderKibboState.STAND pairTo Timer(STAND_DUR),
        BuilderKibboState.THROW pairTo Timer(THROW_DUR),
        BuilderKibboState.PRE_THROW pairTo Timer(PRE_THROW_DUR)
    )

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!megaman.ready) {
                loop.reset()
                stateTimers.values().forEach { it.reset() }
            }

            val stateTimer = stateTimers[currentState]
            if (shouldUpdateStateTimer()) stateTimer.update(delta)
            if (stateTimer.isFinished()) onFinishStateTimer(stateTimer)

            if (currentState != BuilderKibboState.THROW) FacingUtils.setFacingOf(this)
        }
    }

    private fun shouldUpdateStateTimer() = when(currentState) {
        BuilderKibboState.STAND -> body.isSensing(BodySense.FEET_ON_GROUND)
        else -> true
    }

    private fun onFinishStateTimer(stateTimer: Timer) {
        stateTimer.reset()
        val nextState = loop.next()
        if (nextState == BuilderKibboState.THROW) throwHammer()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionY = false
        body.setSize(1.25f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(
            body,
            FixtureType.FEET,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.5f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { if (shieldFixture.isActive()) shieldFixture else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAV else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            shieldFixture.offsetFromBodyAttachment.x = 0.85f * ConstVals.PPM * facing.value
            shieldFixture.offsetFromBodyAttachment.y = 0.2f * ConstVals.PPM
            shieldFixture.setActive(currentState != BuilderKibboState.THROW)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = if (invincible) damageBlink else false
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when (currentState) {
                        BuilderKibboState.STAND -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "stand" else "jump"
                        else -> currentState.name.lowercase()
                    }
                }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun throwHammer() {
        val spawn = body.getCenter()
        spawn.x += 0.1f * ConstVals.PPM * -facing.value
        spawn.y += 0.25f * ConstVals.PPM

        val impulse = MegaUtilMethods
            .calculateJumpImpulse(
                spawn,
                megaman.body.getBounds().getPositionPoint(Position.BOTTOM_CENTER),
                HAMMER_IMPULSE_Y * ConstVals.PPM
            )
            .let {
                if (it.y < HAMMER_MIN_IMPULSE_Y * ConstVals.PPM) it.y = HAMMER_MIN_IMPULSE_Y * ConstVals.PPM
                when (facing) {
                    Facing.LEFT -> it.coerceX(-HAMMER_MAX_IMPULSE_X * ConstVals.PPM, 0f)
                    Facing.RIGHT -> it.coerceX(0f, HAMMER_MAX_IMPULSE_X * ConstVals.PPM)
                }
            }

        val hammer = MegaEntityFactory.fetch(KibboHammer::class)!!
        hammer.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )
    }
}
