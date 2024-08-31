package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.*
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setHitByBodyReceiver
import com.megaman.maverick.game.world.setHitByProjectileReceiver

class SeaMine(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IDrawableShapesEntity, IDamager, IHazard {

    companion object {
        const val TAG = "SeaMine"
        private const val SENSOR_RADIUS = 2f
        private const val TIME_TO_BLOW = 1f
        private const val CULL_TIME = 1f
        private const val SPEED = 1.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val blowTimer = Timer(TIME_TO_BLOW)
    private val sensor = GameCircle().setRadius(SENSOR_RADIUS * ConstVals.PPM)
    private var triggered = false

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("wait", atlas.findRegion("$TAG/wait"))
            regions.put("blow", atlas.findRegion("$TAG/blow"))
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { sensor }
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        blowTimer.reset()
        triggered = false
    }

    private fun trigger() {
        triggered = true
    }

    private fun explodeAndDie() {
        kill()
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        game.engine.spawn(
            explosion, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        sensor.setCenter(body.getCenter())
        if (!triggered && getMegaman().body.getBodyBounds().overlaps(sensor)) trigger()
        if (triggered) {
            body.physics.velocity = getMegaman().body.getCenter().sub(body.getCenter()).nor().scl(SPEED * ConstVals.PPM)
            blowTimer.update(delta)
            if (blowTimer.isFinished()) explodeAndDie()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.3f * ConstVals.PPM))
        bodyFixture.setHitByBodyReceiver { trigger() }
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.375f * ConstVals.PPM))
        shieldFixture.setHitByProjectileReceiver { trigger() }
        body.addFixture(shieldFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.add { game.eventsMan.addListener(cullOnEvents) }
        runnablesOnDestroy.add { game.eventsMan.removeListener(cullOnEvents) }
        val cullOutOfBounds = getGameCameraCullingLogic(this, CULL_TIME)
        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS to cullOnEvents, ConstKeys.CULL_OUT_OF_BOUNDS to cullOutOfBounds
            )
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (triggered) "blow" else "wait" }
        val animations = objectMapOf<String, IAnimation>(
            "blow" to Animation(regions["blow"], 1, 2, 0.1f, true), "wait" to Animation(regions["wait"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}