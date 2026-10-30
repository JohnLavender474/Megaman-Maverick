package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class TubeBeam(services: MegaServices) : AbstractProjectile(services), IAnimatedEntity, IDirectional, IEventListener {

    companion object {
        const val TAG = "TubeBeam"
        const val DEFAULT_CULL_TIME = 1.5f
        private var region: TextureRegion? = null
    }

    override lateinit var direction: Direction
    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)

    private lateinit var cullTimer: Timer
    private lateinit var trajectory: Vector2

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        services.events.addListener(this)

        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!

        val size = GameObjectPools.fetch(Vector2::class)
        if (direction.isHorizontal()) {
            size.x = 2f
            size.y = 1f
        } else {
            size.x = 1f
            size.y = 2f
        }
        size.scl(ConstVals.PPM.toFloat())
        body.setSize(size.x, size.y)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, DEFAULT_CULL_TIME, Float::class)
        cullTimer = Timer(cullTime)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.BEGIN_ROOM_TRANS) destroy()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        services.events.removeListener(this)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.set(trajectory)
            (damagerFixture.rawShape as GameRectangle).setSize(body.getSize())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, doUpdate = { true })
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setSize(2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = if (direction.isHorizontal()) 0f else 90f
            sprite.setCenter(body.getCenter())
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
