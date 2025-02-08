package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.HeliMet.HeliMetState.*
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class HeliMet(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IDirectional,
    IFaceable {

    companion object {
        const val TAG = "HeliMet"
        private const val TARGET_VEL = 10f
        private const val POP_UP_DUR = 0.3f
        private const val SHOOT_DELAY = 1.5f
        private const val BULLET_VELOCITY = 15f
        private const val SIDE_TO_SIDE_VEL = 2f
        private const val SIDE_TO_SIDE_DUR = 6f
        private const val SHIELD_ROTATION_PER_SECOND = 720f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class HeliMetState { SHIELD, POP_UP, FLY }

    override lateinit var direction: Direction
    override lateinit var facing: Facing

    private lateinit var state: HeliMetState

    private val popUpTimer = Timer(POP_UP_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private val sideToSideTimer = SmoothOscillationTimer(SIDE_TO_SIDE_DUR, -1f, 1f)

    private val target = Vector2()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            HeliMetState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
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

                val target1 = spawnProps.get("${ConstKeys.TARGET}_1", RectangleMapObject::class)!!
                    .rectangle.toGameRectangle().getCenter()

                val target2 = spawnProps.get("${ConstKeys.TARGET}_2", RectangleMapObject::class)!!
                    .rectangle.toGameRectangle().getCenter()

                val megamanCenter = megaman.body.getCenter()

                target.set(if (target1.dst2(megamanCenter) < target2.dst2(megamanCenter)) target1 else target2)
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
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
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
        .updatable { delta, sprite ->
            sprite.setCenter(body.getCenter())

            when {
                direction.isVertical() -> sprite.setFlip(isFacing(Facing.LEFT), false)
                else -> sprite.setFlip(false, isFacing(Facing.LEFT))
            }

            sprite.hidden = damageBlink

            sprite.setOriginCenter()
            sprite.rotation = when (state) {
                SHIELD -> sprite.rotation + (SHIELD_ROTATION_PER_SECOND * delta)
                else -> direction.rotation
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .addAnimations(
                    "shield" pairTo Animation(regions["shield"]),
                    "fly" pairTo Animation(regions["fly"], 1, 2, 0.1f, true),
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
