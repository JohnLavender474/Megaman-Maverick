package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
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
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IHealthEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.decorations.ChargedShotResidual
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class ChargedShot(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ChargedShot"

        val FULL_BODY_SIZE: Vector2 = Vector2(0.75f, 1f).scl(ConstVals.PPM.toFloat())
        val HALF_BODY_SIZE: Vector2 = Vector2(0.5f, 0.75f).scl(ConstVals.PPM.toFloat())

        private const val BOUNCE_MAX = 2

        private const val HALF_CHARGED_SHOT_REGION_PREFIX = "Half"
        private const val CHARGED_SHOT_REGION_SUFFIX = "_v2"

        private val SPRITE_SIZE = Vector2(2f, 2f).scl(ConstVals.PPM.toFloat())
        private val regions = ObjectMap<String, TextureRegion>()
    }

    var fullyCharged = false
        private set

    private val trajectory = Vector2()
    private var bounced = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put(ConstKeys.FULL, atlas.findRegion("${TAG}${CHARGED_SHOT_REGION_SUFFIX}"))
            regions.put(ConstKeys.HALF, atlas.findRegion("${HALF_CHARGED_SHOT_REGION_PREFIX}${TAG}"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!

        val bodySize = if (fullyCharged) FULL_BODY_SIZE else HALF_BODY_SIZE
        body.setSize(bodySize)
        body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).setSize(bodySize) }

        trajectory.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        bounced = 0

        spawnResidual()
    }

    private fun spawnResidual() {
        val spawn = trajectory.cpy().nor().scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val residual = MegaEntityFactory.fetch(ChargedShotResidual::class)!!
        residual.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.BOOLEAN pairTo fullyCharged,
                ConstKeys.ROTATION pairTo trajectory.angleDeg(),
            )
        )
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable !is IHealthEntity || damageable.getCurrentHealth() > damageable.getMinHealth()) explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity != owner && !entity.dead && entity is IDamageable && !entity.canBeDamagedBy(this)) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        bounce(otherShape)

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity == owner || (shieldEntity is IOwnable<*> && shieldEntity.owner == owner)) return
        bounce(otherShape)
    }

    private fun bounce(shape: IGameShape2D) {
        bounced++

        if (bounced > BOUNCE_MAX) {
            explodeAndDie()
            return
        }

        val temp = GameObjectPools.fetch(Vector2::class).set(trajectory)
        val direction = UtilMethods.getOverlapPushDirection(body.getBounds(), shape) ?: Direction.UP
        when (direction) {
            Direction.UP, Direction.DOWN -> temp.y = -trajectory.y
            Direction.LEFT, Direction.RIGHT -> {
                temp.x = -trajectory.x
                temp.y = abs(trajectory.x / 2f)
            }
        }
        temp.nor().scl(trajectory.len())
        trajectory.set(temp)

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        val props = props(
            ConstKeys.OWNER pairTo owner,
            ConstKeys.BOOLEAN pairTo fullyCharged,
            ConstKeys.POSITION pairTo body.getCenter(),
            ConstKeys.ROTATION pairTo body.physics.velocity.angleDeg()
        )

        val explosion = MegaEntityFactory.fetch(ChargedShotExplosion::class)!!
        explosion.spawn(props)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        body.physics.velocity.let { if (canMove) it.set(trajectory.cpy().scl(movementScalar)) else it.setZero() }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG,
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10)).also { sprite -> sprite.setSize(SPRITE_SIZE) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg()
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (fullyCharged) ConstKeys.FULL else ConstKeys.HALF }
                .addAnimations(
                    ConstKeys.FULL pairTo Animation(regions[ConstKeys.FULL], 2, 1, 0.05f, true),
                    ConstKeys.HALF pairTo Animation(regions[ConstKeys.HALF], 2, 1, 0.05f, true)
                )
                .build()
        )
        .build()
}
