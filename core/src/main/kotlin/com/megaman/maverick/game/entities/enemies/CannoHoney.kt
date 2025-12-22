package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.HoneyDrip
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.utils.StateLoopHandler
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.getShape
import com.megaman.maverick.game.utils.extensions.overlaps
import com.megaman.maverick.game.world.body.*

class CannoHoney(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IParentEntity<Beezee>, ICullable {

    companion object {
        const val TAG = "CannoHoney"

        private const val CULL_TIME = 0.5f
        private const val CULL_BOUNDS_SIZE = 6f

        private const val IDLE_DUR = 1f
        private const val SQUEEZE_DUR = 1f
        private const val SQUEEZE_TIME = 0.5f

        private const val MAX_BEES = 8
        private const val BEE_CYCLE_DELAY_NORMAL = 2f
        private const val BEE_CYCLE_DELAY_HARD = 1.5f
        private const val MIN_BEES_TO_CYCLE = 1
        private const val MAX_BEES_TO_CYCLE = 3

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(2, 1, 0.1f, false),
            "squeeze" pairTo AnimationDef(3, 1, gdxArrayOf(0.1f, 0.1f, 0.8f), true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CannoHoneyState { IDLE, SQUEEZE }

    override var children = Array<Beezee>()

    private val stateLoopHandler = StateLoopHandler<CannoHoneyState>(
        CannoHoneyState.entries.toGdxArray(),
        gdxArrayOf(
            CannoHoneyState.IDLE pairTo Timer(IDLE_DUR),
            CannoHoneyState.SQUEEZE pairTo Timer(SQUEEZE_DUR)
                .addRunnable(TimeMarkedRunnable(SQUEEZE_TIME) { squeeze() })
        )
    )
    private val currentState: CannoHoneyState
        get() = stateLoopHandler.getCurrentState()

    private val cullTimer = Timer(CULL_TIME)
    private val cullBounds = GameRectangle()
        .setSize(CULL_BOUNDS_SIZE * ConstVals.PPM)
        .also { it.drawingColor = Color.GRAY }

    private val scanners = Array<IGameShape2D>()

    private val beeCycleDelay = Timer()
    private val reusableBeeArray = Array<Beezee>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { cullBounds }
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateLoopHandler.reset()

        (0 until MAX_BEES).forEach { spawnBeezee() }

        putCullable(ConstKeys.CUSTOM_CULL, this)
        cullTimer.reset()

        val beeCycleDur = when (game.state.getDifficultyMode()) {
            DifficultyMode.NORMAL -> BEE_CYCLE_DELAY_NORMAL
            DifficultyMode.HARD -> BEE_CYCLE_DELAY_HARD
        }
        beeCycleDelay.resetDuration(beeCycleDur)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SCANNER)) {
                val scanner = (value as MapObject).getShape()
                scanners.add(scanner)
                addDebugShapeSupplier { scanner }
            }
        }
    }

    override fun canBeDamagedBy(damager: IDamager) =
        damager is Axe || super.canBeDamagedBy(damager)

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        // if the hive is destroyed by Megaman, then send the bees to attack Megaman, and release them
        // from the `children` array
        releaseChildren()

        explode()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        // if the hive is culled for being out of bounds, then destroy the children as well
        killChildren()

        clearDebugShapeSuppliers()
    }

    private fun releaseChildren() {
        GameLogger.debug(TAG, "releaseChildren(): children=${children.map { it.hashCode() }}")
        children.forEach { it.setToAttack() }
        children.clear()
    }

    private fun killChildren() {
        GameLogger.debug(TAG, "killChildren(): children=${children.map { it.hashCode() }}")
        children.forEach { it.destroy() }
        children.clear()
    }

    override fun shouldBeCulled(delta: Float) = cullTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val childIter = children.iterator()
            while (childIter.hasNext()) {
                val child = childIter.next()
                if (child.dead) childIter.remove()
            }

            cullBounds.setCenter(body.getCenter())

            if (!game.getGameCamera().overlaps(cullBounds)) cullTimer.update(delta)
            else cullTimer.reset()

            stateLoopHandler.update(delta)

            if (scanners.any { megaman.body.getBounds().overlaps(it) }) {
                beeCycleDelay.update(delta)

                if (beeCycleDelay.isFinished()) {
                    cycleBees()

                    val beeCycleDur = when (game.state.getDifficultyMode()) {
                        DifficultyMode.NORMAL -> BEE_CYCLE_DELAY_NORMAL
                        DifficultyMode.HARD -> BEE_CYCLE_DELAY_HARD
                    }
                    beeCycleDelay.resetDuration(beeCycleDur)
                }
            } else beeCycleDelay.setToEnd()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM, 1.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM, 3.5f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
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

    private fun squeeze() {
        GameLogger.debug(TAG, "squeeze()")

        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)

        val honeyDrip = MegaEntityFactory.fetch(HoneyDrip::class)!!
        honeyDrip.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn
            )
        )
    }

    private fun cycleBees() {
        val beesToCycle = reusableBeeArray

        beesToCycle.clear()
        for (child in children) if (!child.dead) beesToCycle.add(child)
        beesToCycle.shuffle()

        val numBeesToCycle = UtilMethods.getRandom(MIN_BEES_TO_CYCLE, MAX_BEES_TO_CYCLE)
        beesToCycle.truncate(numBeesToCycle)

        for (beeToCycle in beesToCycle) {
            beeToCycle.setToAttack()
            children.removeValue(beeToCycle, false)
        }

        (0 until numBeesToCycle).forEach { spawnBeezee() }

        beesToCycle.clear()
    }

    private fun spawnBeezee() {
        val spawn = body.getCenter()

        val beezee = MegaEntityFactory.fetch(Beezee::class)!!
        beezee.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn
            )
        )

        children.add(beezee)
    }
}
