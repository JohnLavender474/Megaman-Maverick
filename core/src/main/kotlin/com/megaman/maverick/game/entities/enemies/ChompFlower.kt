package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.entities.utils.StateLoopHandler
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class ChompFlower(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectional {

    companion object {
        const val TAG = "ChompFlower"

        private const val FRAME_1_HEIGHT = 0.25f
        private const val FRAME_2_HEIGHT = 0.75f
        private const val FRAME_3_HEIGHT = 1f
        private const val FRAME_4_HEIGHT = 1.25f
        private const val FULL_HEIGHT = 1.5f

        private const val IDLE_DUR = 1f
        private const val RISE_DUR = 0.4f
        private const val CHOMP_DUR = 1f
        private const val FALL_DUR = 0.4f

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = objectMapOf(
            "rise" pairTo AnimationDef(2, 2, 0.1f, false),
            "chomp" pairTo AnimationDef(2, 1, 0.1f, true),
            "fall" pairTo AnimationDef(2, 2, 0.1f, false)
        )
    }

    private enum class ChompFlowerState { IDLE, RISE, CHOMP, FALL }

    override lateinit var direction: Direction

    private val stateLoopHandler = StateLoopHandler(
        ChompFlowerState.entries.toGdxArray(),
        gdxArrayOf(
            ChompFlowerState.IDLE pairTo Timer(IDLE_DUR),
            ChompFlowerState.RISE pairTo Timer(RISE_DUR),
            ChompFlowerState.CHOMP pairTo Timer(CHOMP_DUR),
            ChompFlowerState.FALL pairTo Timer(FALL_DUR)
        )
    )
    private val currentState: ChompFlowerState
        get() = stateLoopHandler.getCurrentState()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_ENEMIES.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP.name, String::class).uppercase()
        )

        val position = DirectionPositionMapper.getPosition(direction).opposite()
        val point = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(point, position)

        stateLoopHandler.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta -> stateLoopHandler.update(delta) }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setWidth(0.75f * ConstVals.PPM)

        val drawableShapesComponentBuilder = DrawableShapesComponentBuilder().addDebug { body.getBounds() }
        addComponent(drawableShapesComponentBuilder.build())

        body.preProcess.put(ConstKeys.DEFAULT) {
            val height = when (currentState) {
                ChompFlowerState.IDLE -> 0f
                ChompFlowerState.RISE -> {
                    val time = stateLoopHandler.getStateTimer()!!.time

                    if (time < 0.1f) FRAME_1_HEIGHT
                    else if (time < 0.2f) FRAME_2_HEIGHT
                    else if (time < 0.3f) FRAME_3_HEIGHT
                    else FRAME_4_HEIGHT
                }
                ChompFlowerState.CHOMP -> FULL_HEIGHT
                ChompFlowerState.FALL -> {
                    val time = stateLoopHandler.getStateTimer()!!.time

                    if (time < 0.1f) FRAME_4_HEIGHT
                    else if (time < 0.2f) FRAME_3_HEIGHT
                    else if (time < 0.3f) FRAME_2_HEIGHT
                    else FRAME_1_HEIGHT
                }
            }

            val oldBounds = GameObjectPools.fetch(GameRectangle::class).set(body.getBounds())

            when (direction) {
                Direction.UP -> {
                    body.setBottomCenterToPoint(oldBounds.getPositionPoint(Position.BOTTOM_CENTER))
                    body.setHeight(height * ConstVals.PPM)
                }
                Direction.DOWN -> {
                    body.setTopCenterToPoint(oldBounds.getPositionPoint(Position.TOP_CENTER))
                    body.setHeight(height * ConstVals.PPM)
                }
                Direction.LEFT -> {
                    body.setCenterRightToPoint(oldBounds.getPositionPoint(Position.CENTER_RIGHT))
                    body.setWidth(height * ConstVals.PPM)
                }
                Direction.RIGHT -> {
                    body.setCenterLeftToPoint(oldBounds.getPositionPoint(Position.CENTER_LEFT))
                    body.setWidth(height * ConstVals.PPM)
                }
            }

            body.forEachFixture {
                ((it as Fixture).rawShape as GameRectangle).set(body.getBounds())
                it.setActive(currentState != ChompFlowerState.IDLE)
            }
        }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = DirectionPositionMapper.getPosition(direction).opposite()
            sprite.setPosition(body.getBounds().getPositionPoint(position), position)

            sprite.hidden = damageBlink || currentState == ChompFlowerState.IDLE

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()
}
