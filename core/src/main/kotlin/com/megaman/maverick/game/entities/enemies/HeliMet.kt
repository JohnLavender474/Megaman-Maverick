package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.GutsTankFist
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.HeliMet.HeliMetState.*
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class HeliMet(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFreezableEntity, IAnimatedEntity,
    IDirectional, IFaceable {

    companion object {
        const val TAG = "HeliMet"

        private const val TARGET_VEL = 10f

        private const val POP_UP_DUR = 0.3f

        private const val SHOOT_DELAY = 1.5f
        private const val BULLET_VELOCITY = 15f

        private const val SIDE_TO_SIDE_VEL = 2f
        private const val SIDE_TO_SIDE_DUR = 6f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class HeliMetState { SHIELD, POP_UP, FLY }

    override lateinit var direction: Direction
    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private lateinit var state: HeliMetState

    private val popUpTimer = Timer(POP_UP_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private val sideToSideTimer = SmoothOscillationTimer(SIDE_TO_SIDE_DUR, -1f, 1f)

    private val target = Vector2()
    private val tempTargetsQueue = Array<Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            HeliMetState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            regions.put("frozen", atlas.findRegion("$TAG/frozen"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(BigAssMaverickRobotOrb::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(GutsTankFist::class, dmgNeg(ConstVals.MAX_HEALTH))
        damageOverrides.put(PurpleBlast::class, dmgNeg(ConstVals.MAX_HEALTH))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        when {
            spawnProps.containsKey(ConstKeys.POSITION) -> {
                val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
                body.setCenter(spawn)

                target.set(spawnProps.get(ConstKeys.TARGET, Vector2::class)!!)
            }
            else -> {
                val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
                body.setCenter(spawn)

                val queue = tempTargetsQueue

                spawnProps.forEach { key, value ->
                    if (key.toString().contains(ConstKeys.TARGET)) {
                        val target = (value as RectangleMapObject).rectangle.getCenter()
                        queue.add(target)
                    }
                }

                val megamanCenter = megaman.body.getCenter()
                queue.sort sort@{ t1, t2 ->
                    val t1Dst = t1.dst2(megamanCenter)
                    val t2Dst = t2.dst2(megamanCenter)
                    return@sort t1Dst.compareTo(t2Dst)
                }

                val target = queue.first()
                this.target.set(target)

                queue.clear()
            }
        }

        val direction = spawnProps.get(ConstKeys.DIRECTION)
        this.direction = when (direction) {
            null -> Direction.UP
            is String -> Direction.valueOf(direction.uppercase())
            else -> direction as Direction
        }

        facing = when (this.direction) {
            Direction.UP -> if (target.x < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (target.x < body.getX()) Facing.RIGHT else Facing.LEFT
            Direction.LEFT -> if (target.y < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (target.y < body.getY()) Facing.RIGHT else Facing.LEFT
        }

        state = SHIELD

        popUpTimer.reset()
        shootDelayTimer.reset()
        sideToSideTimer.reset()

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun canBeDamagedBy(damager: IDamager) =
        damageOverrides.containsKey(damager::class) || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (damageOverrides.containsKey(damager::class)) explode()
        return super.takeDamageFrom(damager)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) {
                body.physics.velocity.setZero()
                return@add
            }

            when (state) {
                SHIELD -> {
                    val velocity = GameObjectPools.fetch(Vector2::class)
                        .set(target)
                        .sub(body.getCenter())
                        .nor()
                        .scl(TARGET_VEL * ConstVals.PPM)

                    body.physics.velocity.set(velocity)

                    if (body.getBounds().contains(target)) {
                        popUpTimer.reset()
                        body.setCenter(target)
                        state = POP_UP
                    }
                }
                POP_UP -> {
                    body.physics.velocity.setZero()

                    popUpTimer.update(delta)
                    if (popUpTimer.isFinished()) state = FLY
                }
                FLY -> {
                    facing = when (direction) {
                        Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                        Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
                        Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                        Direction.RIGHT -> if (megaman.body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
                    }

                    sideToSideTimer.update(delta)

                    when (direction) {
                        Direction.UP -> {
                            val velocityX = SIDE_TO_SIDE_VEL * ConstVals.PPM * sideToSideTimer.getValue()
                            body.physics.velocity.x = velocityX
                        }
                        Direction.DOWN -> {
                            val velocityX = SIDE_TO_SIDE_VEL * ConstVals.PPM * sideToSideTimer.getValue()
                            body.physics.velocity.x = -velocityX
                        }
                        Direction.LEFT -> {
                            val velocityY = SIDE_TO_SIDE_VEL * ConstVals.PPM * sideToSideTimer.getValue()
                            body.physics.velocity.y = -velocityY
                        }
                        Direction.RIGHT -> {
                            val velocityY = SIDE_TO_SIDE_VEL * ConstVals.PPM * sideToSideTimer.getValue()
                            body.physics.velocity.y = velocityY
                        }
                    }

                    shootDelayTimer.update(delta)
                    if (shootDelayTimer.isFinished()) {
                        shoot()
                        shootDelayTimer.reset()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            damageableFixture.setActive(state != SHIELD)
            shieldFixture.setActive(state == SHIELD)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { delta, sprite ->
            sprite.setCenter(body.getCenter())

            when {
                direction.isVertical() -> sprite.setFlip(isFacing(Facing.LEFT), false)
                else -> sprite.setFlip(false, isFacing(Facing.LEFT))
            }

            sprite.hidden = damageBlink

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .addAnimations(
                    "fly" pairTo Animation(regions["fly"], 1, 2, 0.1f, true),
                    "shield" pairTo Animation(regions["shield"], 2, 2, 0.1f, true),
                    "pop_up" pairTo Animation(regions["pop_up"], 1, 3, 0.1f, false)
                )
                .build()
        )
        .build()

    private fun shoot() {
        val spawn = when (direction) {
            Direction.UP -> body.getCenter().add(0.1f * ConstVals.PPM * facing.value, -0.5f * ConstVals.PPM)
            Direction.DOWN -> body.getCenter().add(0.1f * ConstVals.PPM * facing.value, 0.5f * ConstVals.PPM)
            Direction.LEFT -> body.getCenter().add(-0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM * facing.value)
            Direction.RIGHT -> body.getCenter().add(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM * facing.value)
        }

        val trajectory = megaman.body.getCenter().sub(spawn).nor().scl(BULLET_VELOCITY * ConstVals.PPM)

        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}
