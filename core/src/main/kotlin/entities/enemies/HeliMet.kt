package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
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
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.enemies.HeliMet.HeliMetState.*
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class HeliMet(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectional, IFaceable {

    enum class HeliMetState {
        SHIELD,
        POP_UP,
        FLY
    }

    companion object {
        const val TAG = "HeliMet"
        private const val TARGET_VEL = 10f
        private const val POP_UP_DUR = 0.3f
        private const val SHIELD_ROTATION_PER_SECOND = 720f
        private const val SIDE_TO_SIDE_VEL = 2f
        private const val SIDE_TO_SIDE_DUR = 6f
        private const val SHOOT_DELAY = 1.5f
        private const val BULLET_VELOCITY = 15f
        private var shieldRegion: TextureRegion? = null
        private var popUpRegion: TextureRegion? = null
        private var flyRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )

    override var direction = Direction.UP
    override lateinit var facing: Facing

    private val popUpTimer = Timer(POP_UP_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private val sideToSideTimer = SmoothOscillationTimer(SIDE_TO_SIDE_DUR, -1f, 1f)

    private lateinit var heliMetState: HeliMetState
    private lateinit var target: Vector2

    override fun init() {
        if (shieldRegion == null || popUpRegion == null || flyRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            shieldRegion = atlas.findRegion("HeliMet/Shield")
            popUpRegion = atlas.findRegion("HeliMet/PopUp")
            flyRegion = atlas.findRegion("HeliMet/Fly")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        if (spawnProps.containsKey(ConstKeys.POSITION)) {
            val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            body.setCenter(spawn)
            target = spawnProps.get(ConstKeys.TARGET, Vector2::class)!!
        } else {
            val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            body.setCenter(spawn)
            val target1 =
                spawnProps.get("${ConstKeys.TARGET}_1", RectangleMapObject::class)!!
                    .rectangle.toGameRectangle().getCenter()
            val target2 =
                spawnProps.get("${ConstKeys.TARGET}_2", RectangleMapObject::class)!!
                    .rectangle.toGameRectangle().getCenter()
            val megamanCenter = megaman().body.getCenter()
            target = if (target1.dst2(megamanCenter) < target2.dst2(megamanCenter)) target1 else target2
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

        heliMetState = SHIELD
        popUpTimer.reset()
        shootDelayTimer.reset()
        sideToSideTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (heliMetState) {
                SHIELD -> {
                    body.physics.velocity = target.cpy().sub(body.getCenter()).nor().scl(TARGET_VEL * ConstVals.PPM)
                    if (body.getBounds().contains(target)) {
                        body.setCenter(target)
                        popUpTimer.reset()
                        heliMetState = POP_UP
                    }
                }

                POP_UP -> {
                    body.physics.velocity.setZero()
                    popUpTimer.update(delta)
                    if (popUpTimer.isFinished()) heliMetState = FLY
                }

                FLY -> {
                    facing = when (direction) {
                        Direction.UP -> if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                        Direction.DOWN -> if (megaman().body.getX() < body.getX()) Facing.RIGHT else Facing.LEFT
                        Direction.LEFT -> if (megaman().body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                        Direction.RIGHT -> if (megaman().body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
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
        body.setSize(0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            damageableFixture.setActive(heliMetState != SHIELD)
            shieldFixture.setActive(heliMetState == SHIELD)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())

            if (direction.isVertical())
                sprite.setFlip(isFacing(Facing.LEFT), false)
            else sprite.setFlip(false, isFacing(Facing.LEFT))

            sprite.setOriginCenter()
            val rotation =
                if (heliMetState == SHIELD) sprite.rotation + (SHIELD_ROTATION_PER_SECOND * delta)
                else direction.rotation
            sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (heliMetState) {
                SHIELD -> "shield"
                POP_UP -> "pop_up"
                FLY -> "fly"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "shield" pairTo Animation(shieldRegion!!),
            "pop_up" pairTo Animation(popUpRegion!!, 1, 3, 0.1f, false),
            "fly" pairTo Animation(flyRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        val spawn = when (direction) {
            Direction.UP -> body.getCenter()
                .add(
                    (ConstVals.PPM / 64f) * facing.value, -0.25f * ConstVals.PPM
                )

            Direction.DOWN -> body.getCenter()
                .add(
                    (ConstVals.PPM / 64f) * facing.value, 0.25f * ConstVals.PPM
                )

            Direction.LEFT -> body.getCenter()
                .add(
                    -0.25f * ConstVals.PPM, (ConstVals.PPM / 64f) * facing.value
                )

            Direction.RIGHT -> body.getCenter()
                .add(
                    0.25f * ConstVals.PPM, (ConstVals.PPM / 64f) * facing.value
                )
        }
        val trajectory = megaman().body.getCenter().sub(spawn).nor().scl(BULLET_VELOCITY * ConstVals.PPM)
        val spawnProps =
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.POSITION pairTo spawn
            )
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        bullet.spawn(spawnProps)
        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }
}
