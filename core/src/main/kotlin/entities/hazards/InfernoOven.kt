package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getOpposingPosition
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class InfernoOven(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity, IDirectional, IDamager, IHazard {

    companion object {
        const val TAG = "InfernoOven"

        private const val FLAME = "flame"

        private const val SPRITE_WIDTH = 2f
        private const val SPRITE_HEIGHT = 6f

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 6f

        private const val DAMAGER_WIDTH = 0.5f
        private const val DAMAGER_HEIGHT = 3f

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = ObjectMap<String, AnimationDef>()
    }

    private enum class InfernoOvenState(val duration: Float) { COLD(1f), WARMING_UP(0.5f), HOT(1f) }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val loop = Loop(InfernoOvenState.entries.toGdxArray())
    private val currentState: InfernoOvenState
        get() = loop.getCurrent()
    private val timers = OrderedMap<InfernoOvenState, Timer>()
    private lateinit var spawnRoom: String

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            InfernoOvenState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            regions.put(FLAME, atlas.findRegion("$TAG/$FLAME"))
        }
        if (animDefs.isEmpty) {
            animDefs.put(InfernoOvenState.COLD.name.lowercase(), AnimationDef())
            animDefs.put(InfernoOvenState.WARMING_UP.name.lowercase(), AnimationDef(1, 2, 0.1f, true))
            animDefs.put(InfernoOvenState.HOT.name.lowercase(), AnimationDef(1, 2, 0.1f, true))
            animDefs.put(FLAME, AnimationDef(1, 2, 0.1f, true))
        }
        if (timers.isEmpty) InfernoOvenState.entries.forEach { state ->
            val timer = Timer(state.duration)
            timers.put(state, timer)
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val size = GameObjectPools.fetch(Vector2::class)
        when {
            direction.isHorizontal() -> size.set(BODY_HEIGHT, BODY_WIDTH)
            else -> size.set(BODY_WIDTH, BODY_HEIGHT)
        }.scl(ConstVals.PPM.toFloat())
        body.setSize(size)

        val position = direction.getOpposingPosition()
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        loop.reset()
        timers.values().forEach { it.reset() }

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!overlapsGameCamera()) {
            loop.reset()
            animators[FLAME].reset()
            timers.values().forEach { it.reset() }
            return@UpdatablesComponent
        }

        val timer = timers[currentState]
        timer.update(delta)

        if (timer.isFinished()) {
            val nextState = loop.next()

            if (nextState == InfernoOvenState.HOT && overlapsGameCamera())
                requestToPlaySound(SoundAsset.FLAMETHROWER_SOUND, false)

            timer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(DAMAGER_WIDTH * ConstVals.PPM, DAMAGER_HEIGHT * ConstVals.PPM)
        )
        damagerFixture.offsetFromBodyAttachment.y = DAMAGER_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            /*
            val size = GameObjectPools.fetch(Vector2::class)
            val damagerBounds = damagerFixture.rawShape as GameRectangle
            damagerBounds.setSize(
                when {
                    direction.isHorizontal() -> size.set(DAMAGER_HEIGHT, DAMAGER_WIDTH)
                    else -> size.set(DAMAGER_WIDTH, DAMAGER_HEIGHT)
                }.scl(ConstVals.PPM.toFloat())
            )
             */

            damagerFixture.setActive(currentState == InfernoOvenState.HOT)
            damagerFixture.drawingColor = if (damagerFixture.isActive()) Color.RED else Color.WHITE
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    private fun rotateAndPositionSprite(sprite: GameSprite) {
        sprite.setOriginCenter()
        sprite.rotation = direction.rotation
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        sprite.setPosition(body.getPositionPoint(position), position)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite()
                .also { sprite -> sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM) }
        )
        .updatable { _, sprite -> rotateAndPositionSprite(sprite) }
        .sprite(
            FLAME, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            rotateAndPositionSprite(sprite)
            sprite.hidden = currentState != InfernoOvenState.HOT
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    InfernoOvenState.entries.forEach { state ->
                        val key = state.name.lowercase()
                        val region = regions[key]
                        val animDef = animDefs[key]
                        animations.put(
                            key,
                            Animation(region, animDef.rows, animDef.cols, animDef.durations, animDef.loop)
                        )
                    }
                }
                .build()
        )
        .key(FLAME)
        .animator(
            Animator(animDefs[FLAME].let { animDef ->
                Animation(
                    regions[FLAME],
                    animDef.rows,
                    animDef.cols,
                    animDef.durations,
                    animDef.loop
                )
            }).also { animator -> animator.shouldAnimatePredicate = { currentState == InfernoOvenState.HOT } }
        )
        .build()

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
