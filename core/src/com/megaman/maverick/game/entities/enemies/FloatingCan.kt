package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.isNeighborOf
import com.megaman.maverick.game.utils.toGridCoordinate
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import kotlin.reflect.KClass

class FloatingCan(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "FloatingCan"
        private var textureRegion: TextureRegion? = null
        private const val SPAWN_DELAY = 1f
        private const val SPAWN_BLINK = 0.1f
        private const val FLY_SPEED = 1.5f
        private const val DEBUG_PATHFINDING = false
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 5
        }
    )

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private val spawningBlinkTimer = Timer(SPAWN_BLINK)

    private var spawnDelayBlink = false

    override fun init() {
        if (textureRegion == null) textureRegion =
            game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source).findRegion("FloatingCan")
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS)) (spawnProps.get(
            ConstKeys.BOUNDS, GameRectangle::class
        ))!!.getCenter() else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
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
        body.setSize(0.75f * ConstVals.PPM)

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        shapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getCenter(), Position.CENTER)
            if (!spawnDelayTimer.isFinished()) _sprite.hidden = spawnDelayBlink
            else if (spawnDelayTimer.isJustFinished()) _sprite.hidden = if (invincible) damageBlink else false
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(textureRegion!!, 1, 4, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { getMegaman().body.getCenter().toGridCoordinate() },
            allowDiagonal = { true },
            filter = { coordinate ->
                val bodies = game.getWorldContainer().getBodies(coordinate.x, coordinate.y)
                var passable = true
                var blockingBody: Body? = null

                for (otherBody in bodies) if (otherBody.getEntity().getEntityType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                if (!passable && coordinate.isNeighborOf(
                        body.getCenter().toGridCoordinate()
                    )
                ) blockingBody?.let { passable = !body.overlaps(it as Rectangle) }

                passable
            },
            properties = props(ConstKeys.HEURISTIC to DynamicBodyHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(params, {
            StandardPathfinderResultConsumer.consume(
                it,
                body,
                body.getCenter(),
                { FLY_SPEED * ConstVals.PPM },
                body,
                stopOnTargetReached = false,
                stopOnTargetNull = false
            )
        })
        return pathfindingComponent
        /*
        val params = PathfinderParams(startSupplier = { body.getCenter() },
            targetSupplier = { game.megaman.body.getCenterPoint() },
            allowDiagonal = { true },
            filter = { _, objs ->
                objs.none { it is Fixture && it.getFixtureType() == FixtureType.BLOCK }
            })
        val pathfindingComponent = PathfindingComponent(params, {
            StandardPathfinderResultConsumer.consume(
                it,
                body,
                body.getCenter(),
                FLY_SPEED,
                body.fixtures.find { pair -> pair.first == FixtureType.DAMAGER }!!.second.getShape() as GameRectangle,
                stopOnTargetReached = false,
                stopOnTargetNull = false,
                postProcess = { if (!spawnDelayTimer.isFinished()) body.physics.velocity.setZero() },
                shapes = if (DEBUG_PATHFINDING) game.getShapes() else null
            )
        })
        pathfindingComponent.updateIntervalTimer = Timer(0.1f)
        return pathfindingComponent

         */
    }
}
