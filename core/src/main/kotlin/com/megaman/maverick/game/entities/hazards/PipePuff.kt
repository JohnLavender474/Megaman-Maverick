package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class PipePuff(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IDamager, IHazard, IDirectional {

    companion object {
        const val TAG = "PipePuff"

        private const val VERT_BODY_WIDTH = 0.5f
        private const val VERT_BODY_HEIGHT = 1.25f

        private const val HORIZ_BODY_WIDTH = 1.25f
        private const val HORIZ_BODY_HEIGHT = 0.5f

        private const val PUFF_SHORT_DELAY = 0.5f
        private const val PUFF_LONG_DELAY = 1.5f
        private const val PUFF_DUR = 0.3f
        private const val PUFFS_BETWEEN_REST = 3

        private var region: TextureRegion? = null
    }

    private enum class PipePuffState { DELAY, PUFF }

    override lateinit var direction: Direction

    private val loop = Loop(PipePuffState.entries.toGdxArray())
    private val currentState: PipePuffState
        get() = loop.getCurrent()

    private val puffTimer = Timer(PUFF_DUR)
    private var puffs = 0

    private val delayTimer = Timer()

    private lateinit var spawnRoom: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        direction = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())
        when {
            direction.isVertical() -> body.setSize(VERT_BODY_WIDTH * ConstVals.PPM, VERT_BODY_HEIGHT * ConstVals.PPM)
            else -> body.setSize(HORIZ_BODY_WIDTH * ConstVals.PPM, HORIZ_BODY_HEIGHT * ConstVals.PPM)
        }

        val position = DirectionPositionMapper.getPosition(direction)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position.opposite())

        loop.reset()

        puffTimer.reset()
        puffs = 0

        delayTimer.resetDuration(PUFF_SHORT_DELAY)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (currentState) {
            PipePuffState.PUFF -> {
                puffTimer.update(delta)

                if (puffTimer.isFinished()) {
                    puffTimer.reset()

                    puffs++
                    when {
                        puffs >= PUFFS_BETWEEN_REST -> {
                            puffs = 0
                            delayTimer.resetDuration(PUFF_LONG_DELAY)
                        }
                        else -> delayTimer.resetDuration(PUFF_SHORT_DELAY)
                    }

                    loop.next()
                }
            }
            PipePuffState.DELAY -> {
                if (!game.isProperty(ConstKeys.ROOM_TRANSITION, true)) delayTimer.update(delta)

                if (delayTimer.isFinished()) {
                    if (overlapsGameCamera()) requestToPlaySound(SoundAsset.WHOOSH_SOUND, false)

                    loop.next()
                }
            }
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@cull room != spawnRoom
                }
            )
        )
    )

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { if (damagerFixture.isActive()) damagerFixture else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            (damagerFixture.rawShape as GameRectangle).set(body)

            val active = currentState == PipePuffState.PUFF
            damagerFixture.setActive(active)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 15))
                .also { sprite -> sprite.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = currentState != PipePuffState.PUFF
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (currentState == PipePuffState.PUFF) "puff" else null }
                .addAnimation("puff", Animation(region!!, 2, 3, 0.05f, false))
                .build()
        )
        .build()

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
