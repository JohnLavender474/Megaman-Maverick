package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory

import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class ChargedShot(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "ChargedShot"
        private const val BOUNCE_LIMIT = 3
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var facing = Facing.RIGHT
    override var direction = Direction.UP

    var fullyCharged = false
        private set

    private val trajectory = Vector2()
    private var bounced = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put("full", atlas.findRegion("ChargedShot"))
            regions.put("half", atlas.findRegion("HalfChargedShot"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        var bodyDimension = 0.75f * ConstVals.PPM
        var spriteDimension = ConstVals.PPM.toFloat()

        if (fullyCharged) spriteDimension *= 1.5f else bodyDimension /= 2f
        defaultSprite.setSize(spriteDimension)

        body.setSize(bodyDimension)
        body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).setSize(bodyDimension) }

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)

        facing = when {
            direction.isVertical() == true -> if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
            trajectory.y > 0f -> Facing.RIGHT
            else -> Facing.LEFT
        }

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        bounced = 0

        spawnResidual()
    }

    private fun spawnResidual() {
        val position = when (direction) {
            Direction.UP, Direction.DOWN ->
                if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT

            Direction.LEFT, Direction.RIGHT ->
                if (isFacing(Facing.LEFT)) Position.BOTTOM_CENTER else Position.TOP_CENTER
        }
        val residual = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.CHARGED_SHOT_RESIDUAL)!!
        residual.spawn(
            props(
                ConstKeys.SPAWN pairTo body.getCenter(),
                ConstKeys.POSITION pairTo position,
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.FACING pairTo facing,
                ConstKeys.BOOLEAN pairTo fullyCharged
            )
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable !is IHealthEntity || damageable.getCurrentHealth() > ConstVals.MIN_HEALTH) explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity != owner && entity is IDamageable && !entity.canBeDamagedBy(this)) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity == owner) return
        if (shieldEntity is IOwnable && shieldEntity.owner == owner) return

        bounced++
        if (bounced >= BOUNCE_LIMIT) {
            explodeAndDie()
            return
        }

        swapFacing()
        if (direction.isVertical() == true) trajectory.x *= -1f else trajectory.y *= -1f

        val deflection = shieldFixture.getOrDefaultProperty(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        when (direction) {
            Direction.UP -> {
                when (deflection) {
                    Direction.UP -> trajectory.set(trajectory.x, 5f * ConstVals.PPM)
                    Direction.DOWN -> trajectory.set(trajectory.x, -5f * ConstVals.PPM)
                    else -> trajectory.set(trajectory.x, 0f)
                }
            }

            Direction.DOWN -> {
                when (deflection) {
                    Direction.UP -> trajectory.set(trajectory.x, -5f * ConstVals.PPM)
                    Direction.DOWN -> trajectory.set(trajectory.x, 5f * ConstVals.PPM)
                    else -> trajectory.set(trajectory.x, 0f)
                }
            }

            Direction.LEFT -> {
                when (deflection) {
                    Direction.UP -> trajectory.set(-5f * ConstVals.PPM, trajectory.y)
                    Direction.DOWN -> trajectory.set(5f * ConstVals.PPM, trajectory.y)
                    else -> trajectory.set(0f, trajectory.y)
                }
            }

            Direction.RIGHT -> {
                when (deflection) {
                    Direction.UP -> trajectory.set(5f * ConstVals.PPM, trajectory.y)
                    Direction.DOWN -> trajectory.set(-5f * ConstVals.PPM, trajectory.y)
                    else -> trajectory.set(0f, trajectory.y)
                }
            }
        }

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val e = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CHARGED_SHOT_EXPLOSION)!!

        val direction = when {
            abs(trajectory.y) > abs(trajectory.x) -> (if (trajectory.y > 0f) Direction.UP else Direction.DOWN)
            trajectory.x > 0f -> Direction.RIGHT
            else -> Direction.LEFT
        }

        val props = props(
            ConstKeys.POSITION pairTo body.getCenter(),
            ConstKeys.OWNER pairTo owner,
            ConstKeys.DIRECTION pairTo direction,
            ConstKeys.BOOLEAN pairTo fullyCharged,
        )

        e.spawn(props)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        body.physics.velocity.let { if (canMove) it.set(trajectory.cpy().scl(movementScalar)) else it.setZero() }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (fullyCharged) "full" else "half" }
        val animations = objectMapOf<String, IAnimation>(
            "full" pairTo Animation(regions["full"], 2, 1, 0.05f, true),
            "half" pairTo Animation(regions["half"], 2, 1, 0.05f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 4))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(
                when {
                    direction.equalsAny(Direction.UP, Direction.LEFT) -> isFacing(Facing.LEFT)
                    else -> isFacing(Facing.RIGHT)
                },
                false
            )
            sprite.setPosition(body.getCenter(), Position.CENTER)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return spritesComponent
    }
}
