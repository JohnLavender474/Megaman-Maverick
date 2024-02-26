package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toInt
import com.engine.common.extensions.toObjectSet
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.events.Event
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
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class BulbBlaster(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity,
    IMotionEntity {

    companion object {
        const val TAG = "BulbBlaster"
        private const val STATE_DUR = 3f
        private const val LIGHT_RADIUS = 5
        private const val RADIANCE_FACTOR = 2f
        private var lightRegion: TextureRegion? = null
        private var darkRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    private val timer = Timer(STATE_DUR)
    private lateinit var keys: ObjectSet<Int>
    private var light = false

    override fun init() {
        if (lightRegion == null || darkRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            lightRegion = atlas.findRegion("BulbBlaster/Light")
            darkRegion = atlas.findRegion("BulbBlaster/Dark")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)

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
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        clearMotionDefinitions()
        light = false
        sendEvent()
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
        body.setSize(1f * ConstVals.PPM)

        // body fixture
        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        // damageable fixture
        val damageableFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        // shield fixture
        val shieldFixture = Fixture(GameRectangle().set(body), FixtureType.SHIELD)
        body.addFixture(shieldFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
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