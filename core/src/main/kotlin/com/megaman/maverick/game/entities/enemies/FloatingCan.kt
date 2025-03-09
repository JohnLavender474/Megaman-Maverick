package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

// implements `IBossListener` to ensure is destroyed after each Reactor Monkey is defeated
class FloatingCan(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IBossListener, IAnimatedEntity {

    companion object {
        const val TAG = "FloatingCan"
        private var region: TextureRegion? = null
        private const val SPAWN_DELAY = 1f
        private const val SPAWN_BLINK = 0.1f
        private const val FLY_SPEED = 1.5f
    }

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private val spawningBlinkTimer = Timer(SPAWN_BLINK)
    private var spawnDelayBlink = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source).findRegion(TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                (spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class))!!.getCenter()

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(spawn)

        spawnDelayTimer.reset()
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

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        shapes.add { damageableFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getCenter(), Position.CENTER)
            when {
                !spawnDelayTimer.isFinished() -> sprite.hidden = spawnDelayBlink
                else -> sprite.hidden = damageBlink
            }
        }
        return spritesComponent
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
            filter = { coordinate ->
                val bodies = game.getWorldContainer()!!.getBodies(coordinate.x, coordinate.y)
                var passable = true
                var blockingBody: IBody? = null

                for (otherBody in bodies) if (otherBody.getEntity().getType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                passable
            },
            properties = props(ConstKeys.HEURISTIC pairTo DynamicBodyHeuristic(game))
        )

        val pathfindingComponent = PathfindingComponent(params, {
            when {
                spawnDelayTimer.isFinished() -> StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = { FLY_SPEED * ConstVals.PPM },
                    stopOnTargetReached = false,
                    stopOnTargetNull = false
                )

                else -> body.physics.velocity.setZero()
            }
        }, { spawnDelayTimer.isFinished() })

        return pathfindingComponent
    }
}
