package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
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
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class LampeonBullet(game: MegamanMaverickGame) : AbstractProjectile(game), IFaceable {

    companion object {
        const val TAG = "LampeonBullet"
        private val ANGLES = gdxArrayOf(80f, 85f, 90f, 95f, 100f)
        private const val RANDOM_SPREAD = 1f
        private val regKeys = (0..4).map { it.toString() }
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private var index = -1

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, regKeys, regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        GameLogger.debug(TAG, "canSpawn(): spawnProps=$spawnProps")

        val index = spawnProps.getOrDefault(ConstKeys.INDEX, -1, Int::class)
        if (index < 0 || index > 4) return false

        return super.canSpawn(spawnProps)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!

        val index = spawnProps.getOrDefault(ConstKeys.INDEX, -1, Int::class)
        if (index < 0 || index > 4) throw IllegalStateException("Index is invalid: $index")

        val angle = ANGLES[index] + 270f + UtilMethods.getRandom(-RANDOM_SPREAD, RANDOM_SPREAD)

        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(1f, 0f)
            .setAngleDeg(angle)
            .scl(speed)
        if (isFacing(Facing.LEFT)) trajectory.x *= -1

        body.physics.velocity.set(trajectory)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        this.index = index
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shield = shieldFixture.getEntity()
        if (shield is LampeonBullet || shield == owner) return
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
        explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")

        destroy()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(ConstKeys.OWNER pairTo owner, ConstKeys.POSITION pairTo body.getCenter()))

        if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, /* FixtureType.SHIELD */)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(0.5f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { index.toString() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDef(AnimationDef(2, 1, 0.1f, true), animations, regions, regKeys)
                }
                .build()
        )
        .build()
}
