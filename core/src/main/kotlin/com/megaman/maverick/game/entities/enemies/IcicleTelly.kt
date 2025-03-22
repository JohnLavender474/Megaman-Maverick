package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
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
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.entities.projectiles.FallingIcicle.FallingIcicleState
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class IcicleTelly(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "IcicleTelly"

        private const val VEL_X = 2f

        private const val DROP_DUR = 0.5f
        private const val DROP_TIME = 0.2f

        private const val SPAWN_DUR = 0.5f

        private const val CAN_DROP_DELAY = 0.5f

        private val animDefs = orderedMapOf(
            IcicleTellyState.SPIN pairTo AnimationDef(2, 2, 0.15f, true),
            IcicleTellyState.DROP_ICICLE pairTo AnimationDef(1, 3, gdxArrayOf(0.1f, 0.1f, 0.3f), false),
            IcicleTellyState.SPAWN_ICICLE pairTo AnimationDef(1, 3, gdxArrayOf(0.3f, 0.1f, 0.1f), false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class IcicleTellyState { SPIN, DROP_ICICLE, SPAWN_ICICLE }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<IcicleTellyState>
    private val currentState: IcicleTellyState
        get() = stateMachine.getCurrent()
    private val stateTimers = orderedMapOf(
        IcicleTellyState.DROP_ICICLE pairTo Timer(DROP_DUR).addRunnable(TimeMarkedRunnable(DROP_TIME) { dropIcicle() }),
        IcicleTellyState.SPAWN_ICICLE pairTo Timer(SPAWN_DUR)
    )
    private val canDropDelay = Timer(CAN_DROP_DELAY)

    private var icicleShattered = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys()
                .map { it.name.lowercase() }
                .forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        FacingUtils.setFacingOf(this)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        canDropDelay.setToEnd()

        icicleShattered = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                IcicleTellyState.SPIN -> {
                    FacingUtils.setFacingOf(this)

                    val velX = VEL_X * ConstVals.PPM * facing.value
                    body.physics.velocity.x = velX

                    canDropDelay.update(delta)

                    if (canDropIcicle() && shouldDropIcicle()) stateMachine.next()
                }

                IcicleTellyState.DROP_ICICLE, IcicleTellyState.SPAWN_ICICLE -> {
                    body.physics.velocity.setZero()

                    val timer = stateTimers[currentState]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val icicleFixture =
            Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        icicleFixture.attachedToBody = false
        icicleFixture.setFilter filter@{ fixture ->
            if (fixture.getType() == FixtureType.PROJECTILE) return@filter true

            val entity = fixture.getEntity()
            return@filter fixture.getType() == FixtureType.DAMAGEABLE && entity == megaman
        }
        icicleFixture.setConsumer { processState, fixture ->
            if (processState == ProcessState.BEGIN && currentState == IcicleTellyState.SPIN) shatterIcicle()
        }
        body.addFixture(icicleFixture)
        icicleFixture.drawingColor = Color.PURPLE
        debugShapes.add { if (icicleFixture.isActive()) icicleFixture else null }

        val icicleDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        icicleFixture.attachedToBody = false
        body.addFixture(icicleDamagerFixture)

        val bodyDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(bodyDamagerFixture)

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEF) {
            val center = body.getCenter()

            val size = GameObjectPools.fetch(Vector2::class)
            when (currentState) {
                IcicleTellyState.SPIN -> size.set(ConstVals.PPM.toFloat())
                IcicleTellyState.DROP_ICICLE,
                IcicleTellyState.SPAWN_ICICLE -> size.set(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
            }
            body.setSize(size)
            body.setCenter(center)

            val bodyDamagerBounds = bodyDamagerFixture.rawShape as GameRectangle
            bodyDamagerBounds.setSize(size)

            val active = currentState == IcicleTellyState.SPIN

            icicleFixture.setActive(active)
            icicleDamagerFixture.setActive(active)

            if (active) {
                val icicle = icicleFixture.rawShape as GameRectangle
                icicle.positionOnPoint(body.getPositionPoint(Position.BOTTOM_CENTER), Position.TOP_CENTER)

                val damager = icicleDamagerFixture.rawShape as GameRectangle
                damager.set(icicleFixture.rawShape as GameRectangle)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.translateY(-0.3f * ConstVals.PPM)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key.name.lowercase()
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    private fun canDropIcicle() = canDropDelay.isFinished()

    private fun shouldDropIcicle(): Boolean {
        val bodyCenter = body.getCenter()
        val megamanCenter = megaman.body.getCenter()
        return megamanCenter.y <= bodyCenter.y && bodyCenter.x.epsilonEquals(megamanCenter.x, 0.25f * ConstVals.PPM)
    }

    private fun dropIcicle() {
        val spawn = body.getCenter()

        val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
        icicle.spawn(
            props(
                ConstKeys.STATE pairTo FallingIcicleState.FALL,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.FACING pairTo facing
            )
        )
    }

    private fun shatterIcicle() {
        val position = body.getPositionPoint(Position.BOTTOM_CENTER)
        IceShard.spawn5(position, FallingIcicle.TAG)

        icicleShattered = true
        stateMachine.next()
    }

    private fun buildStateMachine() = EnumStateMachineBuilder.create<IcicleTellyState>()
        .initialState(IcicleTellyState.SPIN)
        .setOnChangeState(this::onChangeState)
        .transition(IcicleTellyState.SPIN, IcicleTellyState.SPAWN_ICICLE) { icicleShattered }
        .transition(IcicleTellyState.SPIN, IcicleTellyState.DROP_ICICLE) { true }
        .transition(IcicleTellyState.DROP_ICICLE, IcicleTellyState.SPAWN_ICICLE) { true }
        .transition(IcicleTellyState.SPAWN_ICICLE, IcicleTellyState.SPIN) { true }
        .build()

    private fun onChangeState(current: IcicleTellyState, previous: IcicleTellyState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
        stateTimers[current]?.reset()

        if (previous == IcicleTellyState.SPIN && icicleShattered) icicleShattered = false
    }
}
