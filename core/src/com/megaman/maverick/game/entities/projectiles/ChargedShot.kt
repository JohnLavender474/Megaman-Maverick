package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import kotlin.math.abs

class ChargedShot(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable,
    IDirectionRotatable {

    companion object {
        private const val BOUNCE_LIMIT = 3
        private var fullyChargedRegion: TextureRegion? = null
        private var halfChargedRegion: TextureRegion? = null
    }

    override var facing = Facing.RIGHT
    override var directionRotation: Direction? = null
    var fullyCharged = false
        private set

    private lateinit var trajectory: Vector2
    private var bounced = 0

    override fun init() {
        if (fullyChargedRegion == null)
            fullyChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Shoot")
        if (halfChargedRegion == null)
            halfChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "HalfChargedShot")
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        var bodyDimension = 0.75f * ConstVals.PPM
        var spriteDimension = ConstVals.PPM.toFloat()

        if (fullyCharged) spriteDimension *= 1.5f else bodyDimension /= 2f
        (firstSprite as GameSprite).setSize(spriteDimension)

        body.setSize(bodyDimension)
        body.fixtures.forEach { ((it.second as Fixture).rawShape as GameRectangle).setSize(bodyDimension) }

        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!

        facing =
            if (directionRotation?.isVertical() == true) if (trajectory.x > 0f) Facing.RIGHT else Facing.LEFT
            else if (trajectory.y > 0f) Facing.RIGHT else Facing.LEFT

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        bounced = 0
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun hitSand(sandFixture: IFixture) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity == owner) return
        if (shieldEntity is IOwnable && shieldEntity.owner == owner) return

        bounced++
        if (bounced >= BOUNCE_LIMIT) {
            explodeAndDie()
            return
        }

        owner = shieldEntity

        swapFacing()
        if (directionRotation?.isVertical() == true) trajectory.x *= -1f else trajectory.y *= -1f

        val deflection = shieldFixture.getProperty(ConstKeys.DIRECTION, Direction::class)
        val newTrajectory =
            when (directionRotation!!) {
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
        destroy()

        val e = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CHARGED_SHOT_EXPLOSION)!!
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
        e.spawn(props)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent({ body.physics.velocity.set(trajectory) })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameRectangle())
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

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

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(
                if (directionRotation!!.equalsAny(Direction.UP, Direction.LEFT)) isFacing(Facing.LEFT)
                else isFacing(Facing.RIGHT),
                false
            )
            _sprite.setPosition(body.getCenter(), Position.CENTER)
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
        }
        return spritesComponent
    }
}
