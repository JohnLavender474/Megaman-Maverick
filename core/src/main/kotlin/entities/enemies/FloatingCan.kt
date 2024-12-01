package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.mega.game.engine.world.body.*
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
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.isNeighborOf
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class FloatingCan(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "FloatingCan"
        private var textureRegion: TextureRegion? = null
        private const val SPAWN_DELAY = 1f
        private const val SPAWN_BLINK = 0.1f
        private const val FLY_SPEED = 1.5f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 10
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
        body.setSize(ConstVals.PPM.toFloat())

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        shapes.add { damageableFixture}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.75f * ConstVals.PPM)
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
            targetCoordinateSupplier = { megaman().body.getCenter().toGridCoordinate() },
            allowDiagonal = { true },
            filter = { coordinate ->
                val bodies = game.getWorldContainer()!!.getBodies(coordinate.x, coordinate.y)
                var passable = true
                var blockingBody: IBody? = null

                for (otherBody in bodies) if (otherBody.getEntity().getEntityType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                if (!passable && coordinate.isNeighborOf(body.getCenter(false).toGridCoordinate()))
                    blockingBody?.let { passable = !body.getBounds(false).overlaps(it.getBounds(false)) }

                passable
            },
            properties = props(ConstKeys.HEURISTIC pairTo DynamicBodyHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(params, {
            if (spawnDelayTimer.isFinished())
                StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = { FLY_SPEED * ConstVals.PPM },
                    stopOnTargetReached = false,
                    stopOnTargetNull = false
                )
            else body.physics.velocity.setZero()
        }, { spawnDelayTimer.isFinished() })
        return pathfindingComponent
    }
}
