package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.*
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.motion.MotionComponent
import com.engine.motion.MotionComponent.MotionDefinition
import com.engine.motion.Trajectory
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
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
    private lateinit var keys: ObjectSet<Int>
    private lateinit var spawnRoom: String
    private var light = false

    override fun init() {
        if (lightRegion == null || darkRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            lightRegion = atlas.findRegion("BulbBlaster/Light")
            darkRegion = atlas.findRegion("BulbBlaster/Dark")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        light = spawnProps.getOrDefault(ConstKeys.LIGHT, false, Boolean::class)
        keys = spawnProps.get(ConstKeys.KEYS, String::class)!!.replace("\\s+", "").split(",").map { it.toInt() }
            .toObjectSet()
        timer.reset()

        sendEvent()

        if (spawnProps.containsKey(ConstKeys.TRAJECTORY)) {
            val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
            val motionDefinition = MotionDefinition(motion = trajectory,
                function = { value, _ -> body.physics.velocity.set(value) },
                onReset = { body.setCenter(spawn) })
            putMotionDefinition(ConstKeys.TRAJECTORY, motionDefinition)
        }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        game.eventsMan.removeListener(this)
        clearMotionDefinitions()
        light = false
        sendEvent()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (room != spawnRoom) kill()
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
        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.8f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        return BodyComponentCreator.create(this, body)
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
            "light" to Animation(lightRegion!!, 1, 4, 0.1f, true), "dark" to Animation(darkRegion!!, 1, 4, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun sendEvent() {
        game.eventsMan.submitEvent(
            Event(
                EventType.REQ_BLACK_BACKGROUND, props(
                    ConstKeys.KEYS to keys,
                    ConstKeys.LIGHT to light,
                    ConstKeys.CENTER to body.getCenter(),
                    ConstKeys.RADIUS to LIGHT_RADIUS,
                    ConstKeys.RADIANCE to RADIANCE_FACTOR
                )
            )
        )
    }
}