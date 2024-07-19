package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.interfaces.swapFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import kotlin.math.abs

class ChargedShot(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable, IDirectionRotatable {

    companion object {
        private var fullyChargedRegion: TextureRegion? = null
        private var halfChargedRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    override var facing = Facing.RIGHT
    override lateinit var directionRotation: Direction
    var fullyCharged = false
        private set

    private lateinit var trajectory: Vector2

    override fun init() {
        if (fullyChargedRegion == null)
            fullyChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Shoot")

        if (halfChargedRegion == null)
            halfChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "HalfChargedShot")

        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity?
        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN) as Boolean
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        var bodyDimension = 0.75f * ConstVals.PPM
        var spriteDimension = ConstVals.PPM.toFloat()

        if (fullyCharged) spriteDimension *= 1.5f else bodyDimension /= 2f
        (firstSprite as GameSprite).setSize(spriteDimension)

        body.setSize(bodyDimension)
        body.fixtures.forEach { ((it.second as Fixture).rawShape as GameRectangle).setSize(bodyDimension) }

        trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as Vector2

        facing =
            if (directionRotation.isVertical()) if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
            else if (trajectory.y > 0f) Facing.RIGHT else Facing.LEFT

        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        body.setCenter(spawn)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity == owner) return
        if (shieldEntity is IOwnable && shieldEntity.owner == owner) return
        owner = shieldEntity

        swapFacing()
        if (directionRotation.isVertical()) trajectory.x *= -1f else trajectory.y *= -1f

        val deflection = shieldFixture.getOrDefaultProperty(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val newTrajectory =
            when (directionRotation) {
                Direction.UP -> {
                    when (deflection) {
                        Direction.UP -> Vector2(trajectory.x, 5f * ConstVals.PPM)
                        Direction.DOWN -> Vector2(trajectory.x, -5f * ConstVals.PPM)
                        else -> Vector2(trajectory.x, 0f)
                    }
                }

                Direction.DOWN -> {
                    when (deflection) {
                        Direction.UP -> Vector2(trajectory.x, -5f * ConstVals.PPM)
                        Direction.DOWN -> Vector2(trajectory.x, 5f * ConstVals.PPM)
                        else -> Vector2(trajectory.x, 0f)
                    }
                }

                Direction.LEFT -> {
                    when (deflection) {
                        Direction.UP -> Vector2(-5f * ConstVals.PPM, trajectory.y)
                        Direction.DOWN -> Vector2(5f * ConstVals.PPM, trajectory.y)
                        else -> Vector2(0f, trajectory.y)
                    }
                }

                Direction.RIGHT -> {
                    when (deflection) {
                        Direction.UP -> Vector2(5f * ConstVals.PPM, trajectory.y)
                        Direction.DOWN -> Vector2(-5f * ConstVals.PPM, trajectory.y)
                        else -> Vector2(0f, trajectory.y)
                    }
                }
            }
        trajectory.set(newTrajectory)

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        kill()
        val e = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CHARGED_SHOT_EXPLOSION)

        val direction =
            if (abs(trajectory.y) > abs(trajectory.x))
                (if (trajectory.y > 0f) Direction.UP else Direction.DOWN)
            else if (trajectory.x > 0f) Direction.RIGHT else Direction.LEFT

        val props =
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.OWNER to owner,
                ConstKeys.DIRECTION to direction,
                ConstKeys.BOOLEAN to fullyCharged,
            )
        game.engine.spawn(e!!, props)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(this, { body.physics.velocity.set(trajectory) })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val chargedAnimation = Animation(fullyChargedRegion!!, 1, 2, 0.05f, true)
        val halfChargedAnimation = Animation(halfChargedRegion!!, 1, 2, 0.05f, true)
        val animator =
            Animator(
                { if (fullyCharged) "charged" else "half" },
                objectMapOf("charged" to chargedAnimation, "half" to halfChargedAnimation)
            )
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.setPosition(body.getCenter(), Position.CENTER)

            val rotation =
                when (directionRotation) {
                    Direction.UP,
                    Direction.DOWN -> 0f

                    Direction.LEFT -> 90f
                    Direction.RIGHT -> 270f
                }
            _sprite.setOriginCenter()
            _sprite.rotation = rotation
        }
        return spritesComponent
    }
}
