package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class Lava(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IDirectional, IFaceable {

    companion object {
        const val TAG = "Lava"
        const val FLOW = "Flow"
        const val FALL = "Fall"
        const val TOP = "Top"
        const val MOVE_BEFORE_KILL = "move_before_kill"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction = Direction.UP
    override lateinit var facing: Facing

    var moveBeforeKill = false
        private set
    var movingBeforeKill = false
        private set

    private val moving: Boolean
        get() = !body.getCenter().epsilonEquals(moveTarget, 0.1f * ConstVals.PPM)

    private lateinit var drawingSection: DrawingSection
    private lateinit var type: String
    private lateinit var moveTarget: Vector2
    private lateinit var bodyMatrix: Matrix<GameRectangle>

    private var spawnRoom: String? = null
    private var speed = 0f
    private var spritePriorityValue = 0
    private var doCull = false
    private var black = false

    private val pair = GamePair<Int,Int>(0, 0)
    private val matrix = Matrix<GameRectangle>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put(FLOW, atlas.findRegion("$TAG/${FLOW}"))
            regions.put("${TOP}${FLOW}1", atlas.findRegion("$TAG/${TOP}${FLOW}1"))
            regions.put("${TOP}${FLOW}2", atlas.findRegion("$TAG/${TOP}${FLOW}2"))
            regions.put("${TOP}${FLOW}3", atlas.findRegion("$TAG/${TOP}${FLOW}3"))
            regions.put("${FLOW}1", atlas.findRegion("$TAG/${FLOW}1"))
            regions.put("${FLOW}2", atlas.findRegion("$TAG/${FLOW}2"))
            regions.put("${FLOW}3", atlas.findRegion("$TAG/${FLOW}3"))
            regions.put(FALL, atlas.findRegion("$TAG/${FALL}"))
            regions.put("${FALL}_${ConstKeys.BLACK}", atlas.findRegion("$TAG/${FALL}Black"))
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, FLOW, String::class)
        drawingSection = DrawingSection.valueOf(
            spawnProps.getOrDefault(ConstKeys.SECTION, "foreground", String::class).uppercase()
        )
        spritePriorityValue = spawnProps.getOrDefault(ConstKeys.PRIORITY, if (type == FALL) 2 else 1, Int::class)
        black = spawnProps.getOrDefault(ConstKeys.BLACK, false, Boolean::class)
        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "right", String::class).uppercase())
        speed = spawnProps.getOrDefault(ConstKeys.SPEED, 0f, Float::class)
        doCull = spawnProps.getOrDefault(ConstKeys.CULL, false, Boolean::class)
        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)

        val moveX = spawnProps.getOrDefault("${ConstKeys.MOVE}_${ConstKeys.X}", 0f, Float::class)
        val moveY = spawnProps.getOrDefault("${ConstKeys.MOVE}_${ConstKeys.Y}", 0f, Float::class)
        moveTarget = body.getCenter().add(moveX * ConstVals.PPM, moveY * ConstVals.PPM)

        moveBeforeKill = spawnProps.containsKey(MOVE_BEFORE_KILL)
        removeProperty(MOVE_BEFORE_KILL)
        if (moveBeforeKill) putProperty(MOVE_BEFORE_KILL, spawnProps.get(MOVE_BEFORE_KILL))
        movingBeforeKill = false

        val playSound = spawnProps.getOrDefault(ConstKeys.SOUND, false, Boolean::class)
        if (playSound && overlapsGameCamera()) requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)

        val dimensions = bounds.getSplitDimensions(ConstVals.PPM.toFloat(), pair)
        defineDrawables(dimensions.first, dimensions.second)
    }

    fun moveBeforeKill() {
        movingBeforeKill = true
        val moveBeforeKillTargetRaw = getProperty(MOVE_BEFORE_KILL, String::class)!!.split(",").map { it.toFloat() }
        val targetOffset = Vector2(moveBeforeKillTargetRaw[0], moveBeforeKillTargetRaw[1]).scl(ConstVals.PPM.toFloat())
        moveTarget.add(targetOffset)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        bodyMatrix = body.getBounds().splitByCellSize(ConstVals.PPM.toFloat(), matrix)
        if (movingBeforeKill && !moving) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (moving) {
                val direction = moveTarget.cpy().sub(body.getCenter()).nor()
                body.physics.velocity.set(direction.scl(speed * ConstVals.PPM))
            } else body.physics.velocity.set(0f, 0f)

            (deathFixture.rawShape as GameRectangle).set(body)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullEvents =
            objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS)
        val cullOnEvents = CullableOnEvent({ event ->
            when {
                !doCull -> false
                event.key == EventType.SET_TO_ROOM_NO_TRANS -> {
                    val roomName = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val doKill = spawnRoom != roomName
                    GameLogger.debug(TAG, "Room: $roomName, Spawn Room: $spawnRoom, Do Kill: $doKill")
                    doKill
                }

                else -> {
                    val doKill = cullEvents.contains(event.key)
                    GameLogger.debug(TAG, "Event: ${event.key}, Do Kill: $doKill")
                    doKill
                }
            }
        }, cullEvents)

        runnablesOnSpawn.put(ConstKeys.CULL_EVENTS) { game.eventsMan.addListener(cullOnEvents) }
        runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }

        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvents))
    }

    private fun defineDrawables(rows: Int, cols: Int) {
        val sprites = OrderedMap<Any, GameSprite>()
        val updateFunctions = ObjectMap<Any, UpdateFunction<GameSprite>>()
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val sprite = GameSprite()
                sprite.setSize(1.05f * ConstVals.PPM)

                val spriteKey = "lava_$${col}_${row}"
                sprites.put(spriteKey, sprite)

                updateFunctions.put(spriteKey, UpdateFunction { _, _ ->
                    val bounds = bodyMatrix[col, row]!!
                    sprite.setCenter(bounds.getCenter())
                    sprite.setOriginCenter()
                    sprite.rotation = direction.rotation
                    sprite.setFlip(isFacing(Facing.LEFT), false)
                    sprite.priority.section = drawingSection
                    sprite.priority.value = spritePriorityValue
                })

                val region = if (type == FLOW) {
                    val index = ((row + col) % 3) + 1
                    var regionKey = "$FLOW$index"
                    if (row == rows - 1) regionKey = "$TOP$regionKey"
                    regions[regionKey]
                } else if (black) regions["${FALL}_${ConstKeys.BLACK}"] else regions[FALL]

                val animation = Animation(region!!, 3, 1, 0.1f, true)
                val animator = Animator(animation)
                animators.add({ sprite } pairTo animator)
            }
        }

        addComponent(SpritesComponent(sprites, updateFunctions))
        addComponent(AnimationsComponent(animators))
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
