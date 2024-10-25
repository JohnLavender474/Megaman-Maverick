package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.getOverlapPushDirection
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.math.abs

class SmallMissile(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectionRotatable {

    companion object {
        const val TAG = "SmallMissile"
        const val WAVE_EXPLOSION = "wave_explosion"
        const val DEFAULT_EXPLOSION = "default_explosion"
        private const val GRAVITY = -0.15f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private lateinit var explosionType: String

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            regions.put("green", atlas.findRegion("SmallGreenMissile"))
            regions.put("purple", atlas.findRegion("SmallPurpleMissile"))
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)
        body.physics.gravityOn = gravityOn

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.velocity.set(trajectory)

        val region = regions.get(spawnProps.getOrDefault(ConstKeys.COLOR, "green", String::class))
        firstSprite!!.setRegion(region)

        explosionType = spawnProps.getOrDefault(ConstKeys.EXPLOSION, DEFAULT_EXPLOSION, String::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable is IBodyEntity) explodeAndDie(damageable.body as GameRectangle)
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie(blockFixture.getShape() as GameRectangle)

    override fun hitSand(sandFixture: IFixture) = explodeAndDie(sandFixture.getShape() as GameRectangle)

    override fun hitShield(shieldFixture: IFixture) {
        val left = body.x < shieldFixture.getShape().getX()
        body.physics.velocity.x = if (left) -abs(body.physics.velocity.x) else abs(body.physics.velocity.x)
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()

        if (explosionType == DEFAULT_EXPLOSION) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            explosion.spawn(props(ConstKeys.OWNER pairTo owner, ConstKeys.POSITION pairTo body.getCenter()))

            if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
        } else if (explosionType == WAVE_EXPLOSION) {
            val hitBounds = params[0] as GameRectangle
            val overlap = GameRectangle()
            val direction = getOverlapPushDirection(body, hitBounds, overlap)
            val position = overlap.getPositionPoint(
                when (direction) {
                    Direction.UP, null -> Position.TOP_CENTER
                    Direction.DOWN -> Position.BOTTOM_CENTER
                    Direction.LEFT -> Position.CENTER_LEFT
                    Direction.RIGHT -> Position.CENTER_RIGHT
                }
            )
            val greenExplosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.GREEN_EXPLOSION)!!
            greenExplosion.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.DIRECTION pairTo direction,
                    ConstKeys.OWNER pairTo owner
                )
            )

            if (overlapsGameCamera()) playSoundNow(SoundAsset.BLAST_SOUND, false)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.35f * ConstVals.PPM, 0.65f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity = when (directionRotation!!) {
                Direction.UP -> Vector2(0f, -GRAVITY)
                Direction.DOWN -> Vector2(0f, GRAVITY)
                Direction.LEFT -> Vector2(GRAVITY, 0f)
                Direction.RIGHT -> Vector2(-GRAVITY, 0f)
            }.scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.PROJECTILE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(0.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation!!.rotation
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }
}
