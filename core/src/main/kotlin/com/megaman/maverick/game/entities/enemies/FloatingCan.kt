package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.world.body.*

// implements `IBossListener` to ensure is destroyed after each Reactor Monkey is defeated
class FloatingCan(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IBossListener, IAnimatedEntity {

    companion object {
        const val TAG = "FloatingCan"
        private var region: TextureRegion? = null
        private const val SPAWN_DELAY = 1f
        private const val SPAWN_DELAY_HARD = 0.75f
        private const val SPAWN_BLINK = 0.1f
        private const val FLY_SPEED = 1.5f
        private const val FLY_SPEED_HARD = 2.25f
        private const val MAX_SPAWNED = 6
    }

    private val spawningBlinkTimer = Timer(SPAWN_BLINK)
    private var spawnDelayBlink = false

    private val spawnDelayTimer = Timer()

    private val reusableBodySet = MutableOrderedSet<IBody>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source).findRegion(TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun canSpawn(spawnProps: Properties) =
        super.canSpawn(spawnProps) && MegaGameEntities.getOfTag(TAG).size < MAX_SPAWNED

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                (spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class))!!.getCenter()
            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(spawn)

        spawnDelayTimer.resetDuration(
            if (game.state.getDifficultyMode() == DifficultyMode.HARD) SPAWN_DELAY_HARD else SPAWN_DELAY
        )

        spawningBlinkTimer.reset()
        spawnDelayBlink = false
    }

    override fun canDamage(damageable: IDamageable) = spawnDelayTimer.isFinished() && super.canDamage(damageable)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            spawnDelayTimer.update(delta)
            if (!spawnDelayTimer.isFinished()) {
                spawningBlinkTimer.update(delta)
                if (spawningBlinkTimer.isFinished()) {
                    spawnDelayBlink = !spawnDelayBlink
                    spawningBlinkTimer.reset()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = if (!spawnDelayTimer.isFinished()) spawnDelayBlink else damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { megaman.body.getCenter().toGridCoordinate() },
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

        val pathfindingComponent = PathfindingComponent(params, {
            when {
                spawnDelayTimer.isFinished() -> StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = speed@{
                        val speed = if (game.state.getDifficultyMode() == DifficultyMode.HARD)
                            FLY_SPEED_HARD else FLY_SPEED
                        return@speed speed * ConstVals.PPM
                    },
                    stopOnTargetReached = false,
                    stopOnTargetNull = false
                )

                else -> body.physics.velocity.setZero()
            }
        }, { spawnDelayTimer.isFinished() })

        return pathfindingComponent
    }
}
