package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class MockingByte(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MockingByte"

        private const val AWAKEN_DUR = 0.4f
        private const val LEAVE_NEST_DUR = 0.2f
        private const val ENTER_NEST_DUR = 0.2f
        private const val HOVER_DUR = 1f

        private val animDefs = orderedMapOf(
            "sleep" pairTo AnimationDef(),
            "awaken" pairTo AnimationDef(2, 2, 0.1f, false),
            "leave_nest" pairTo AnimationDef(2, 1, 0.1f, false),
            "enter_nest" pairTo AnimationDef(2, 1, 0.1f, false),
            "hover" pairTo AnimationDef(2, 1, 0.1f, true),
            "dive" pairTo AnimationDef(3, 1, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class MockingByteState { SLEEP, AWAKEN, LEAVE_NEST, ENTER_NEST, HOVER, DIVE }

    override lateinit var facing: Facing

    val currentState: MockingByteState
        get() = stateMachine.getCurrent()

    private lateinit var stateMachine: StateMachine<MockingByteState>
    private val stateTimers = orderedMapOf(
        MockingByteState.AWAKEN pairTo Timer(AWAKEN_DUR),
        MockingByteState.LEAVE_NEST pairTo Timer(LEAVE_NEST_DUR),
        MockingByteState.ENTER_NEST pairTo Timer(ENTER_NEST_DUR),
        MockingByteState.HOVER pairTo Timer(HOVER_DUR)
    )

    private var nestId = -1

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        nestId = spawnProps
            .get(ConstKeys.NEST, RectangleMapObject::class)!!.properties
            .get(ConstKeys.ID, Int::class.java)

        facing = Facing.LEFT
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<MockingByteState>()
        .initialState(MockingByteState.SLEEP)
        .onChangeState(this::onChangeState)
        // TODO
        .build()

    private fun onChangeState(current: MockingByteState, previous: MockingByteState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
    }
}
