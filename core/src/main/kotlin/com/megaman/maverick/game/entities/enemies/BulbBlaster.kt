package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
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
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.entities.contracts.ILightSourceEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

// implements `IBossListener` to ensure is destroyed after 2nd Desert Man fight
class BulbBlaster(game: MegamanMaverickGame) : AbstractEnemy(game), ILightSourceEntity, IAnimatedEntity, IMotionEntity,
    IBossListener, IEventListener {

    companion object {
        const val TAG = "BulbBlaster"
        private const val STATE_DUR = 3f
        private const val RADIUS = 5
        private const val RADIANCE = 2f
        private var lightRegion: TextureRegion? = null
        private var darkRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.END_ROOM_TRANS)
    override var invincible = false // bulb blaster can never be damaged
    override val lightSourceKeys = ObjectSet<Int>()
    override var lightSourceRadius = RADIUS
    override var lightSourceRadiance = RADIANCE
    override val lightSourceCenter: Vector2
        get() = body.getCenter()

    private lateinit var spawnRoom: String
    private val timer = Timer(STATE_DUR)
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

        light = spawnProps.getOrDefault(ConstKeys.LIGHT, false, Boolean::class)
        lightSourceKeys.addAll(
            spawnProps.get(ConstKeys.KEYS, String::class)!!
            .replace("\\s+", "")
            .split(",")
            .map { it.toInt() }
            .toObjectSet()
        )
        timer.reset()

        if (light) LightSourceUtils.sendLightSourceEvent(game, this)

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
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
        clearMotionDefinitions()
        light = false
        lightSourceKeys.clear()
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
            if (light) LightSourceUtils.sendLightSourceEvent(game, this)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.75f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (light) "light" else "dark" }
        val animations = objectMapOf<String, IAnimation>(
            "light" pairTo Animation(lightRegion!!, 1, 4, 0.1f, true),
            "dark" pairTo Animation(darkRegion!!, 1, 4, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
