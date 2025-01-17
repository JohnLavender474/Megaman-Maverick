package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
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
import com.mega.game.engine.common.enums.ProcessState
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.blocks.PushableBlock
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.megaman.components.headFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class FloorButton(game: MegamanMaverickGame) : Switch(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity {

    companion object {
        const val TAG = "FloorButton"
        private const val SWITCH_DUR = 0.2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val switchTimer = Timer(SWITCH_DUR)
    private val pushableBlocks = OrderedSet<PushableBlock>()
    private val reusableArrayOfShapes = Array<IGameShape2D>()
    private lateinit var spawnRoom: String
    private var key = -1

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("down", "switch", "up").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
        key = spawnProps.get(ConstKeys.KEY, Int::class)!!
        switchTimer.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        onDeactivate()
        pushableBlocks.clear()
    }

    override fun shouldBeginSwitchToDown(delta: Float) =
        reusableArrayOfShapes.any { it.overlaps(body.getBounds()) } || !pushableBlocks.isEmpty

    override fun shouldBeginSwitchToUp(delta: Float): Boolean {
        val iter = pushableBlocks.iterator()
        while (iter.hasNext) {
            val block = iter.next()
            if (!block.body.getBounds().overlaps(body.getBounds())) pushableBlocks.remove(block)
        }

        return reusableArrayOfShapes.none { it.overlaps(body.getBounds()) } && pushableBlocks.isEmpty
    }

    override fun shouldFinishSwitchToDown(delta: Float): Boolean {
        switchTimer.update(delta)
        return switchTimer.isFinished()
    }

    override fun shouldFinishSwitchToUp(delta: Float): Boolean {
        switchTimer.update(delta)
        return switchTimer.isFinished()
    }

    override fun onBeginSwitchToDown() {
        GameLogger.debug(TAG, "onBeginSwitchToDown()")
        switchTimer.reset()
        onActivate()
    }

    private fun onActivate() {
        GameLogger.debug(TAG, "onActivate()")
        game.eventsMan.submitEvent(Event(EventType.ACTIVATE_SWITCH, props(ConstKeys.KEY pairTo key)))
    }

    override fun onBeginSwitchToUp() {
        GameLogger.debug(TAG, "onBeginSwitchToUp()")
        switchTimer.reset()
        onDeactivate()
    }

    private fun onDeactivate() {
        GameLogger.debug(TAG, "onDeactivate()")
        game.eventsMan.submitEvent(Event(EventType.DEACTIVATE_SWITCH, props(ConstKeys.KEY pairTo key)))
    }

    override fun onFinishSwitchToDown() {
        GameLogger.debug(TAG, "onFinishSwitchToDown()")
        requestToPlaySound(SoundAsset.BUTTON_SOUND, false)
    }

    override fun onFinishSwitchToUp() {
        GameLogger.debug(TAG, "onFinishSwitchToUp()")
        requestToPlaySound(SoundAsset.BUTTON_SOUND, false)
    }

    override fun defineUpdatablesComponent(component: UpdatablesComponent) {
        component.put(ConstKeys.ARRAY) {
            reusableArrayOfShapes.clear()
            reusableArrayOfShapes.add(megaman.body.getBounds())
            reusableArrayOfShapes.add(megaman.leftSideFixture.getShape())
            reusableArrayOfShapes.add(megaman.rightSideFixture.getShape())
            reusableArrayOfShapes.add(megaman.feetFixture.getShape())
            reusableArrayOfShapes.add(megaman.headFixture.getShape())
        }
        super.defineUpdatablesComponent(component)
    }

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

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            val owner = block.getProperty(ConstKeys.OWNER, IGameEntity::class)
            if (owner != null && owner is PushableBlock) pushableBlocks.add(owner)
        }
        body.addFixture(bodyFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
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

    override fun getTag() = TAG
}
