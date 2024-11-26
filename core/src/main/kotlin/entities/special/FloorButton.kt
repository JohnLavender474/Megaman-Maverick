package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.events.Event
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
import com.megaman.maverick.game.entities.blocks.PushableBlock
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.megaman.components.headFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setHitByBlockReceiver

class FloorButton(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity {

    companion object {
        const val TAG = "FloorButton"
        private const val SWITCH_DUR = 0.2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FloorButtonState { UP, SWITCH_TO_DOWN, SWITCH_TO_UP, DOWN }

    private val pushableBlocks = OrderedSet<PushableBlock>()
    private val switchTimer = Timer(SWITCH_DUR)
    private lateinit var state: FloorButtonState
    private lateinit var spawnRoom: String
    private var key = -1

    private val reusableArrayOfShapes = Array<IGameShape2D>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("down", "switch", "up").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        key = spawnProps.get(ConstKeys.KEY, Int::class)!!
        state = FloorButtonState.UP
        switchTimer.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        deactivate()
        pushableBlocks.clear()
    }

    private fun activate() {
        GameLogger.debug(TAG, "activate()")
        switchTimer.reset()
        state = FloorButtonState.SWITCH_TO_DOWN
        game.eventsMan.submitEvent(Event(EventType.ACTIVATE_SWITCH, props(ConstKeys.KEY pairTo key)))
    }

    private fun deactivate() {
        GameLogger.debug(TAG, "deactivate()")
        switchTimer.reset()
        state = FloorButtonState.SWITCH_TO_UP
        game.eventsMan.submitEvent(Event(EventType.DEACTIVATE_SWITCH, props(ConstKeys.KEY pairTo key)))
    }

    private fun enterPushableBlock(block: PushableBlock) = pushableBlocks.add(block)

    private fun exitPushableBlock(block: PushableBlock) = pushableBlocks.remove(block)

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    room != spawnRoom
                }
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        reusableArrayOfShapes.clear()
        reusableArrayOfShapes.add(megaman().body.getBodyBounds())
        reusableArrayOfShapes.add(megaman().leftSideFixture.getShape())
        reusableArrayOfShapes.add(megaman().rightSideFixture.getShape())
        reusableArrayOfShapes.add(megaman().feetFixture.getShape())
        reusableArrayOfShapes.add(megaman().headFixture.getShape())

        when (state) {
            FloorButtonState.UP ->
                if (reusableArrayOfShapes.any { it.overlaps(body) } || !pushableBlocks.isEmpty) activate()

            FloorButtonState.DOWN -> {
                val iter = pushableBlocks.iterator()
                while (iter.hasNext) {
                    val block = iter.next()
                    if (!block.body.overlaps(body as Rectangle)) exitPushableBlock(block)
                }

                if (reusableArrayOfShapes.none { it.overlaps(body) } && pushableBlocks.isEmpty) deactivate()
            }

            FloorButtonState.SWITCH_TO_DOWN -> {
                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    state = FloorButtonState.DOWN
                    requestToPlaySound(SoundAsset.SE_140, false)
                }
            }

            FloorButtonState.SWITCH_TO_UP -> {
                switchTimer.update(delta)
                if (switchTimer.isFinished()) {
                    state = FloorButtonState.UP
                    requestToPlaySound(SoundAsset.SE_140, false)
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByBlockReceiver { block ->
            val owner = block.getProperty(ConstKeys.OWNER, IGameEntity::class)
            if (owner != null && owner is PushableBlock) enterPushableBlock(owner)
        }
        body.addFixture(bodyFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { state.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "up" pairTo Animation(regions["up"]),
            "down" pairTo Animation(regions["down"]),
            "switch_to_up" pairTo Animation(regions["switch"], 2, 1, 0.1f, false),
            "switch_to_down" pairTo Animation(regions["switch"], 2, 1, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getEntityType() = EntityType.SPECIAL
}
