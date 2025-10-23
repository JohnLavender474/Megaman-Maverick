package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IBody
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.SubmarineMissile
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.entities.utils.StateLoopHandler
import com.megaman.maverick.game.entities.utils.moveTowards
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SubmarineJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IFreezableEntity, IFaceable {

    companion object {
        const val TAG = "SubmarineJoe"

        private const val MIN_MOVE_DUR = 1f
        private const val IDLE_DUR = 0.25f
        private const val SHOOT_DUR = 0.7f
        private const val SHOOT_TIME = 0.5f
        private const val FROZEN_DUR = 0.5f
        private const val CHANGE_FACING_DELAY = 0.5f

        private const val MOVE_SPEED = 3f
        private const val MOVE_LERP_SCALAR = 10f

        private const val MISSILE_SPEED = 10f

        private const val AIM_LINE_LENGTH = 10f

        private const val PATHFINDING_UPDATE_INTERVAL = 0.1f
        private const val DEBUG_PATHFINDING = false

        private val animDefs = orderedMapOf(
            "move" pairTo AnimationDef(2, 1, 0.1f, true),
            "idle" pairTo AnimationDef(2, 1, 0.1f, true),
            "shoot" pairTo AnimationDef(1, 7, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SubmarineJoeState { MOVE, IDLE, SHOOT }

    override lateinit var facing: Facing
    private val changeFacingDelay = Timer(CHANGE_FACING_DELAY)

    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }
    private val frozenTimer = Timer(FROZEN_DUR)

    private val aimLine = GameLine()
    private val missileSpawnBounds = GameRectangle().setSize(2f * ConstVals.PPM, 0.5f * ConstVals.PPM)

    private val stateLoopHandler = object : StateLoopHandler<SubmarineJoeState>(
        SubmarineJoeState.entries.toGdxArray(),
        gdxArrayOf(
            SubmarineJoeState.MOVE pairTo Timer(MIN_MOVE_DUR),
            SubmarineJoeState.IDLE pairTo Timer(IDLE_DUR),
            SubmarineJoeState.SHOOT pairTo Timer(SHOOT_DUR)
                .addRunnable(TimeMarkedRunnable(SHOOT_TIME) { shoot() })
        )
    ) {

        override fun update(delta: Float) {
            super.update(delta)

            val currentState = getCurrentState()

            if (changeFacingDelay.isFinished() && currentState != SubmarineJoeState.SHOOT)
                FacingUtils.setFacingOf(this@SubmarineJoe)

            if (currentState != SubmarineJoeState.MOVE ||
                body.getBounds().overlaps(megaman.body.getBounds())
            ) body.physics.velocity.setZero()
        }

        override fun shouldGoToNextState(state: SubmarineJoeState, timer: Timer?): Boolean {
            if (state == SubmarineJoeState.MOVE) {
                if (!aimLine.overlaps(megaman.body.getBounds())) return false

                val overlapsBlock = MegaGameEntities
                    .getOfType(EntityType.BLOCK)
                    .any { (it as IBodyEntity).body.getBounds().overlaps(missileSpawnBounds) }
                if (overlapsBlock) return false
            }

            return super.shouldGoToNextState(state, timer)
        }

        override fun onChangeState(current: SubmarineJoeState, previous: SubmarineJoeState) {
            GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
            super.onChangeState(current, previous)
        }
    }

    private val currentState: SubmarineJoeState
        get() = stateLoopHandler.getCurrentState()

    private val canMove: Boolean
        get() = !game.isCameraRotating() && !frozen

    private val reusableBodySet = MutableOrderedSet<IBody>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(definePathfindingComponent())
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { aimLine }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(center)

        stateLoopHandler.reset()

        FacingUtils.setFacingOf(this)
        changeFacingDelay.reset()
        updateAimLine()

        frozen = false
    }

    override fun canBeDamagedBy(damager: IDamager) = !frozen && super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is IFreezerEntity) frozen = true
        return damaged
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()
        explode()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        reusableBodySet.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            frozenTimer.update(delta)
            changeFacingDelay.update(delta)

            updateAimLine()
            missileSpawnBounds.setCenter(getShootPosition())

            stateLoopHandler.update(delta)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { megaman.body.getPositionPoint(Position.BOTTOM_CENTER).toGridCoordinate() },
            allowDiagonal = { true },
            filter = filter@{ coordinate ->
                game.getWorldContainer()!!.getBodies(coordinate.x, coordinate.y, reusableBodySet)

                var passable = true
                var blockingBody: IBody? = null

                for (otherBody in reusableBodySet) if (otherBody.getEntity().getType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                reusableBodySet.clear()

                return@filter passable
            },
            properties = props(ConstKeys.HEURISTIC pairTo DynamicBodyHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(
            params,
            {
                if (canMove) StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = { MOVE_SPEED * ConstVals.PPM },
                    stopOnTargetReached = false,
                    onTargetNull = { moveTowards(megaman.body.getCenter(), MOVE_SPEED * ConstVals.PPM) },
                    trajectoryConsumer = { trajectory ->
                        body.physics.velocity.lerp(trajectory, MOVE_LERP_SCALAR * Gdx.graphics.deltaTime)
                    },
                    stopOnTargetNull = false,
                    shapes = if (DEBUG_PATHFINDING) game.getShapes() else null
                )
            },
            { currentState == SubmarineJoeState.MOVE && !body.getBounds().overlaps(megaman.body.getBounds()) },
            intervalTimer = Timer(PATHFINDING_UPDATE_INTERVAL)
        )
        return pathfindingComponent
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { stateLoopHandler.getCurrentState().name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun shoot() {
        val position = getShootPosition()

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(MISSILE_SPEED * ConstVals.PPM * facing.value, 0f)

        val missile = MegaEntityFactory.fetch(SubmarineMissile::class)!!
        missile.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position,
            )
        )

        requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    private fun getShootPosition() = body.getPositionPoint(Position.BOTTOM_CENTER)
        .add(0.5f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

    private fun updateAimLine() {
        val point1 = getShootPosition()
        val point2 = getShootPosition().add(AIM_LINE_LENGTH * ConstVals.PPM * facing.value, 0f)
        aimLine.setLocalPoints(point1, point2)
    }
}
