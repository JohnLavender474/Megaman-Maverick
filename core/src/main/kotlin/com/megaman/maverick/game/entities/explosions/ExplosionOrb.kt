package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class ExplosionOrb(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, ICullableEntity,
    IEventListener {

    companion object {
        const val TAG = "ExplosionOrb"
        private const val OOB_CULL_TIME = 0.5f
        private const val DEFAULT_EXPLOSION_ORB_SPEED = 3.5f
        private val RING_DIRECTIONS = gdxArrayOf(
            Vector2(-1f, 0f),
            Vector2(-1f, 1f),
            Vector2(0f, 1f),
            Vector2(1f, 1f),
            Vector2(1f, 0f),
            Vector2(1f, -1f),
            Vector2(0f, -1f),
            Vector2(-1f, -1f)
        )
        private var region: TextureRegion? = null

        fun spawnRing(center: Vector2, speed: Float = DEFAULT_EXPLOSION_ORB_SPEED) =
            RING_DIRECTIONS.forEach { direction ->
                val orb = MegaEntityFactory.fetch(ExplosionOrb::class)!!
                orb.spawn(
                    props(
                        ConstKeys.POSITION pairTo center,
                        ConstKeys.TRAJECTORY pairTo direction,
                        ConstKeys.SPEED pairTo speed
                    )
                )
            }
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        services.events.addListener(this)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val speed = spawnProps.getOrDefault(ConstKeys.SPEED, DEFAULT_EXPLOSION_ORB_SPEED, Float::class)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!).scl(speed * ConstVals.PPM)
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

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 20))
        sprite.setSize(3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.075f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOOB = getGameCameraCullingLogic(
            services.gameCamera,
            { defaultSprite.boundingRectangle.toGameRectangle() },
            OOB_CULL_TIME
        )
        val cullEvents = getStandardEventCullingLogic(this, objectSetOf(EventType.LEVEL_RESET))
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOOB,
                ConstKeys.CULL_EVENTS pairTo cullEvents
            )
        )
    }

    override fun getType() = EntityType.EXPLOSION
}
