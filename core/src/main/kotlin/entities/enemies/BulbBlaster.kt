package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Trajectory
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.decorations.LightSource
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class BulbBlaster(game: MegamanMaverickGame) : AbstractEnemy(game), IEventListener, IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "BulbBlaster"
        private const val STATE_DUR = 3f
        private const val LIGHT_RADIUS = 5
        private const val RADIANCE_FACTOR = 2f
        private var lightRegion: TextureRegion? = null
        private var darkRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override val eventKeyMask = objectSetOf<Any>(EventType.END_ROOM_TRANS)

    private val timer = Timer(STATE_DUR)
    private var lightSource: LightSource? = null
    private lateinit var keys: ObjectSet<Int>
    private lateinit var spawnRoom: String
    private var light = false

    override fun init() {
        if (lightRegion == null || darkRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            lightRegion = atlas.findRegion("$TAG/Light")
            darkRegion = atlas.findRegion("$TAG/Dark")
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = if (spawnProps.containsKey(ConstKeys.POSITION))
            spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        lightSource = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.LIGHT_SOURCE)!! as LightSource
        lightSource!!.spawn(props(ConstKeys.BOUNDS pairTo body))

        light = spawnProps.getOrDefault(ConstKeys.LIGHT, false, Boolean::class)
        keys = spawnProps.get(ConstKeys.KEYS, String::class)!!
            .replace("\\s+", "")
            .split(",")
            .map { it.toInt() }
            .toObjectSet()
        timer.reset()

        sendEvent()

        if (spawnProps.containsKey(ConstKeys.TRAJECTORY)) {
            val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
            val motionDefinition = MotionDefinition(
                motion = trajectory,
                function = { value, _ -> body.physics.velocity.set(value) },
                onReset = { body.setCenter(spawn) })
            putMotionDefinition(ConstKeys.TRAJECTORY, motionDefinition)
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
        clearMotionDefinitions()
        light = false
        sendEvent()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (room != spawnRoom) destroy()
            }
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            timer.update(it)
            if (timer.isFinished()) {
                light = !light
                timer.reset()
            }
            sendEvent()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM)
        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (light) "light" else "dark" }
        val animations = objectMapOf<String, IAnimation>(
            "light" pairTo Animation(lightRegion!!, 1, 4, 0.1f, true),
            "dark" pairTo Animation(darkRegion!!, 1, 4, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun sendEvent() {
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_LIGHT_SOURCE, props(
                    ConstKeys.KEYS pairTo keys,
                    ConstKeys.LIGHT pairTo light,
                    ConstKeys.CENTER pairTo body.getCenter(),
                    ConstKeys.RADIUS pairTo LIGHT_RADIUS,
                    ConstKeys.RADIANCE pairTo RADIANCE_FACTOR
                )
            )
        )
    }
}
