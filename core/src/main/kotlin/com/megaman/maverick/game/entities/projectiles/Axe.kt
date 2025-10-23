package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class Axe(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IDirectional, IEventListener {

    companion object {
        const val TAG = "Axe"
        private const val CULL_TIME = 1f
        private const val GRAVITY = 0.375f
        private const val MAX_BOUNCES = 2
        private const val SLOW_DOWN_SCALAR = 0.75f
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private var gravityScalar = 1f
    private var bounces = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, "$TAG/spin")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)

        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        bounces = 0
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.BEGIN_ROOM_TRANS) destroy()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (bounces >= MAX_BOUNCES) return

        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity == owner ||
            shieldEntity.isAny(
                PreciousGem::class,
                PreciousShard::class,
                PreciousGemBomb::class,
            )
        ) return

        bounces++

        val direction = UtilMethods.getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
        when (direction) {
            Direction.UP -> {
                body.physics.velocity.y = abs(body.physics.velocity.y) * SLOW_DOWN_SCALAR
                body.physics.velocity.x *= SLOW_DOWN_SCALAR
            }
            Direction.DOWN -> {
                body.physics.velocity.y = -abs(body.physics.velocity.y) * SLOW_DOWN_SCALAR
                body.physics.velocity.x *= SLOW_DOWN_SCALAR
            }
            Direction.LEFT -> {
                body.physics.velocity.x = -abs(body.physics.velocity.x) * SLOW_DOWN_SCALAR
                body.physics.velocity.y *= SLOW_DOWN_SCALAR
            }
            Direction.RIGHT -> {
                body.physics.velocity.x = abs(body.physics.velocity.x) * SLOW_DOWN_SCALAR
                body.physics.velocity.y *= SLOW_DOWN_SCALAR
            }
        }

        GameLogger.debug(TAG, "hitShield(): direction=$direction")

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (direction) {
                Direction.UP -> body.physics.gravity.set(0f, -GRAVITY * gravityScalar * ConstVals.PPM)
                Direction.DOWN -> body.physics.gravity.set(0f, GRAVITY * gravityScalar * ConstVals.PPM)
                Direction.LEFT -> body.physics.gravity.set(GRAVITY * gravityScalar * ConstVals.PPM, 0f)
                Direction.RIGHT -> body.physics.gravity.set(GRAVITY * gravityScalar * ConstVals.PPM, 0f)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.SHIELD, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(1.25f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(body.physics.velocity.x > 0f, false)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 2, 0.1f, true)))
        .build()
}
