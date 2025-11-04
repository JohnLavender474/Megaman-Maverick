package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
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
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.PreciousBlock
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousGem.PreciousGemColor
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*
import java.util.*

// This isn't a "big" enemy, but his size is set to "LARGE" so that he is more powerful against weapons
class PreciousTron(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.LARGE), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PreciousTron"

        private const val APPEAR_DUR = 0.4f
        private const val DISAPPEAR_DUR = 0.4f
        private const val SHOOT_DUR = 1.25f
        private const val SHOOT_DUR_HARD = 0.75f
        private const val STAND_DUR = 0.5f
        private const val STAND_DUR_HARD = 0.35f

        private const val SHOOT_TIME = 0.25f

        private const val GRAVITY = -0.25f
        private const val GROUND_GRAV = -0.01f

        private const val MAX_RAND_POS_CANDIDATES = 3

        private const val GEM_CULL_TIME = 0.5f
        private const val GEM_THROW_SPEED = 10f
        private const val THROW_GEM_OFFSET_X = 2f

        private val GEM_COLORS = PreciousGemColor.entries.toGdxArray()

        private val animDefs = objectMapOf<String, AnimationDef>(
            "disappear" pairTo AnimationDef(2, 2, 0.1f, false),
            "appear" pairTo AnimationDef(2, 2, 0.1f, false),
            "shoot" pairTo AnimationDef(3, 2, 0.05f, false),
            "stand" pairTo AnimationDef(),
            "fall" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PreciousTronState { APPEAR, STAND, SHOOT, DISAPPEAR, FALL }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<PreciousTronState>
    private val currentState: PreciousTronState
        get() = stateMachine.getCurrentElement()

    private lateinit var timers: ObjectMap<PreciousTronState, Timer>

    private val currentPosition = Vector2()
    private val positionSuppliers = Array<() -> Vector2>()
    private val positionQueue = PriorityQueue(Comparator<Vector2> compare@{ o1, o2 ->
        val dist1 = o1.dst2(megaman.body.getCenter())
        val dist2 = o2.dst2(megaman.body.getCenter())
        return@compare dist1.compareTo(dist2)
    })

    private val tempVec2Arr = Array<Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        if (spawnProps.containsKey("${ConstKeys.CULL}_${ConstKeys.BOUNDS}")) {
            val cullBounds = spawnProps.get(
                "${ConstKeys.CULL}_${ConstKeys.BOUNDS}",
                RectangleMapObject::class
            )!!.rectangle.toGameRectangle(false)

            spawnProps.put(
                "${ConstKeys.CULL}_${ConstKeys.BOUNDS}_${ConstKeys.SUPPLIER}",
                { cullBounds }
            )
        }

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(
            ConstKeys.SPAWN,
            RectangleMapObject::class
        )!!.rectangle.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        currentPosition.set(spawn)

        val hardMode = game.state.getDifficultyMode() == DifficultyMode.HARD

        timers = orderedMapOf(
            PreciousTronState.APPEAR pairTo Timer(APPEAR_DUR),
            PreciousTronState.DISAPPEAR pairTo Timer(DISAPPEAR_DUR),
            PreciousTronState.SHOOT pairTo Timer(if (hardMode) SHOOT_DUR_HARD else SHOOT_DUR)
                .addRunnable(TimeMarkedRunnable(SHOOT_TIME) { throwGem() }),
            PreciousTronState.STAND pairTo Timer(if (hardMode) STAND_DUR_HARD else STAND_DUR)
        )

        stateMachine.reset()

        FacingUtils.setFacingOf(this)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.POSITION)) {
                val position = (value as RectangleMapObject)
                    .rectangle
                    .getPositionPoint(Position.BOTTOM_CENTER, false)
                positionSuppliers.add { position }
            }
        }

        GameLogger.debug(TAG, "onSpawn(): currentPosition=$currentPosition, positions=$positionSuppliers")
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        positionQueue.clear()
        positionSuppliers.clear()
        currentPosition.setZero()

        tempVec2Arr.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                PreciousTronState.FALL ->
                    if (body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND))
                        stateMachine.next()
                else -> {
                    if (currentState.equalsAny(PreciousTronState.STAND)) FacingUtils.setFacingOf(this)

                    val timer = timers[currentState]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(1f * ConstVals.PPM, 2f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = ConstVals.PPM * when {
                !body.isSensing(BodySense.FEET_ON_GROUND) && currentState == PreciousTronState.FALL -> GRAVITY
                else -> GROUND_GRAV
            }

            if (currentState.equalsAny(PreciousTronState.APPEAR, PreciousTronState.DISAPPEAR))
                body.physics.velocity.setZero()

            val active = currentState.equalsAny(
                PreciousTronState.STAND, PreciousTronState.FALL, PreciousTronState.SHOOT
            )
            body.forEachFixture { it.setActive(active) }
        }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(3f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
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
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder
        .create<PreciousTronState>()
        .onChangeState(this::onChangeState)
        .initialState(PreciousTronState.APPEAR)
        .transition(PreciousTronState.APPEAR, PreciousTronState.STAND) {
            body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousTronState.APPEAR, PreciousTronState.FALL) { true }
        .transition(PreciousTronState.FALL, PreciousTronState.STAND) {
            body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousTronState.STAND, PreciousTronState.SHOOT) { true }
        .transition(PreciousTronState.SHOOT, PreciousTronState.DISAPPEAR) { true }
        .transition(PreciousTronState.DISAPPEAR, PreciousTronState.APPEAR) { true }
        .build()

    private fun onChangeState(current: PreciousTronState, previous: PreciousTronState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
        timers[previous]?.reset()
        if (current == PreciousTronState.APPEAR) setNextPosition()
    }

    private fun throwGem() {
        FacingUtils.setFacingOf(this)

        GameLogger.debug(TAG, "throwGems(): facing=$facing")

        val spawn = body.getCenter().add(ConstVals.PPM.toFloat() * facing.value, 0.1f * ConstVals.PPM)

        val offset = GameObjectPools.fetch(Vector2::class)
            .set(THROW_GEM_OFFSET_X * facing.value, 0f)

        val target = GameObjectPools.fetch(Vector2::class)
            .set(spawn)
            .add(offset.x * ConstVals.PPM, offset.y * ConstVals.PPM)

        val color = GEM_COLORS.random()

        val speed = GEM_THROW_SPEED * ConstVals.PPM

        val gem = MegaEntityFactory.fetch(PreciousGem::class)!!
        gem.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.COLOR pairTo color,
                ConstKeys.SPEED pairTo speed,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.CULL_TIME pairTo GEM_CULL_TIME,
                "${ConstKeys.FIRST}_${ConstKeys.TARGET}" pairTo target,
                "${ConstKeys.BLOCK}_${ConstKeys.SHATTER}" pairTo true,
                "${ConstKeys.SHIELD}_${ConstKeys.SHATTER}" pairTo objectSetOf(
                    Axe::class,
                    Megaman::class,
                    PreciousBlock::class
                )
            )
        )

        gem.putProperty(ConstKeys.SPIN, false)
        gem.secondTargetSupplier = { megaman.body.getCenter() }
    }

    private fun setNextPosition() {
        positionSuppliers.forEach { if (it != currentPosition) positionQueue.add(it.invoke()) }

        GameLogger.debug(TAG, "setNextPosition(): positionQueue=$positionQueue")

        var candidateCount = 0
        val candidates = tempVec2Arr

        while (!positionQueue.isEmpty() && candidateCount < MAX_RAND_POS_CANDIDATES) {
            val candidate = positionQueue.poll()
            candidates.add(candidate)
            candidateCount++
        }

        GameLogger.debug(TAG, "setNextPosition(): candidates=$candidates")

        val nextPosition = candidates.random()
        body.setBottomCenterToPoint(nextPosition)
        currentPosition.set(nextPosition)

        GameLogger.debug(TAG, "setNextPosition(): nextPosition=$nextPosition")

        positionQueue.clear()
        candidates.clear()
    }
}
