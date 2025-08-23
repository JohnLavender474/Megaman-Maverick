package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class SeaMine(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IDrawableShapesEntity, IDamager, IHazard {

    companion object {
        const val TAG = "SeaMine"

        private const val SENSOR_RADIUS = 2f
        private const val SENSOR_RADIUS_HARD = 3f

        private const val TIME_TO_BLOW = 1f
        private const val TIME_TO_BLOW_HARD = 1.5f

        private const val SPEED = 1.5f
        private const val SPEED_HARD = 2f

        private val animDefs = orderedMapOf(
            "wait" pairTo AnimationDef(1, 3, 0.2f, true),
            "blow" pairTo AnimationDef(1, 2, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val sensor = GameCircle()
    private val blowTimer = Timer()
    private var triggered = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { sensor }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val blowDur = if (game.state.getDifficultyMode() == DifficultyMode.HARD)
            TIME_TO_BLOW_HARD else TIME_TO_BLOW
        blowTimer.resetDuration(blowDur)

        triggered = false

        val sensorRadius = if (game.state.getDifficultyMode() == DifficultyMode.HARD)
            SENSOR_RADIUS_HARD else SENSOR_RADIUS
        sensor.setRadius(sensorRadius * ConstVals.PPM)
    }

    private fun trigger() {
        GameLogger.debug(TAG, "trigger()")
        triggered = true
    }

    private fun explodeAndDie() {
        GameLogger.debug(TAG, "explodeAndDie()")

        destroy()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        sensor.setCenter(body.getCenter())

        if (!triggered && megaman.body.getBounds().overlaps(sensor)) trigger()

        if (triggered) {
            val speed = if (game.state.getDifficultyMode() == DifficultyMode.HARD)
                SPEED_HARD else SPEED

            val velocity = GameObjectPools.fetch(Vector2::class)
                .set(megaman.body.getCenter())
                .sub(body.getCenter())
                .nor()
                .scl(speed * ConstVals.PPM)
            body.physics.velocity.set(velocity)

            blowTimer.update(delta)
            if (blowTimer.isFinished()) explodeAndDie()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.5f * ConstVals.PPM))
        bodyFixture.setHitByBodyReceiver { _, state -> if (state == ProcessState.BEGIN) trigger() }
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.55f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.55f * ConstVals.PPM))
        shieldFixture.setHitByProjectileReceiver { trigger() }
        body.addFixture(shieldFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING)
        val cullOnEvents = CullableOnEvent({ cullEvents.contains(it) }, cullEvents)
        runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
        runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }

        val cullOutOfBounds = getGameCameraCullingLogic(this)

        return CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS pairTo cullOnEvents,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOutOfBounds
            )
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (triggered) "blow" else "wait" }
        val animations = ObjectMap<String, IAnimation>()
        animDefs.forEach { entry ->
            val key = entry.key
            val (rows, columns, durations, loop) = entry.value
            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
