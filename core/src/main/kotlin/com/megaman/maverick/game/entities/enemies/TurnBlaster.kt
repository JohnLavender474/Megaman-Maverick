package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.ReactManProjectile

import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class TurnBlaster(game: MegamanMaverickGame) : AbstractEnemy(game), IDirectional {

    companion object {
        const val TAG = "TurnBlaster"
        private const val MAX_ANGLE_OFFSET = 45f
        private const val TURN_SPEED = 90f
        private const val AIM_DUR = 2f
        private const val SHOOT_DELAY = 0.25f
        private const val ORB_SPEED = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val aimTimer = Timer(AIM_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private var orb: AbstractProjectile? = null
    private var angleOffset = 0f
    private var debug = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("base", atlas.findRegion("$TAG/base"))
            regions.put("dial", atlas.findRegion("$TAG/dial"))
            regions.put("tube", atlas.findRegion("$TAG/tube"))
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val position = when (direction) {
            Direction.UP -> Position.BOTTOM_CENTER
            Direction.DOWN -> Position.TOP_CENTER
            Direction.LEFT -> Position.CENTER_RIGHT
            Direction.RIGHT -> Position.CENTER_LEFT
        }
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.positionOnPoint(bounds.getPositionPoint(position), position)
        aimTimer.reset()
        shootDelayTimer.setToEnd()
        angleOffset = 0f
        debug = spawnProps.getOrDefault(ConstKeys.DEBUG, false, Boolean::class)
    }

    private fun spawnOrb() {
        orb =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.REACT_MAN_PROJECTILE) as AbstractProjectile
        val offset = Vector2(0f, 0.65f * ConstVals.PPM).rotateDeg(direction.rotation + angleOffset)
        val position = body.getCenter().add(offset)
        orb!!.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo position, ConstKeys.BIG pairTo false))
    }

    private fun shootOrb() {
        val rOrb = orb as ReactManProjectile
        rOrb.active = true
        rOrb.setTrajectory(Vector2(0f, ORB_SPEED * ConstVals.PPM).rotateDeg(direction.rotation + angleOffset))
        orb = null
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!shootDelayTimer.isFinished()) {
                shootDelayTimer.update(delta)
                if (shootDelayTimer.isJustFinished()) {
                    aimTimer.reset()
                    shootOrb()
                } else return@add
            }

            val desiredAngle = (megaman.body.getCenter().sub(body.getCenter()).angleDeg() - 90f) % 360f
            if (debug) GameLogger.debug(TAG, "desired angle: $desiredAngle")

            val currentAngle = direction.rotation + angleOffset
            if (debug) GameLogger.debug(TAG, "current angle: $currentAngle")

            val angleDiff = (desiredAngle - currentAngle + 180f) % 360f - 180f
            if (debug) GameLogger.debug(TAG, "angle diff: $angleDiff")

            if (angleDiff > 0f) angleOffset += TURN_SPEED * delta
            else if (angleDiff < 0f) angleOffset -= TURN_SPEED * delta

            angleOffset = angleOffset.coerceIn(-MAX_ANGLE_OFFSET, MAX_ANGLE_OFFSET)
            if (debug) GameLogger.debug(TAG, "new angleOffset: $angleOffset")

            aimTimer.update(delta)
            if (aimTimer.isFinished()) {
                spawnOrb()
                shootDelayTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val size = GameObjectPools.fetch(Vector2::class).set(1.25f, 1.25f).scl(ConstVals.PPM.toFloat())
        val sprites = OrderedMap<Any, GameSprite>()

        val baseSprite = GameSprite(regions.get("base"))
        baseSprite.setSize(size)
        sprites.put("base", baseSprite)

        val dialSprite = GameSprite(regions.get("dial"))
        dialSprite.setSize(size)
        sprites.put("dial", dialSprite)

        val tubeSprite = GameSprite(regions.get("tube"))
        tubeSprite.setSize(size)
        sprites.put("tube", tubeSprite)

        val updateFunctions = ObjectMap<Any, UpdateFunction<GameSprite>>()
        sprites.forEach { entry ->
            val key = entry.key
            updateFunctions.put(key) { _, sprite ->
                sprite.setCenter(body.getCenter())
                sprite.hidden = damageBlink

                sprite.setOriginCenter()
                var rotation = direction.rotation
                if (key != "base") rotation += angleOffset
                sprite.rotation = rotation
            }
        }

        return SpritesComponent(sprites, updateFunctions)
    }
}
