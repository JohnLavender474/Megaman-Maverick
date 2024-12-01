package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CeilingCrusher(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity,
    IAudioEntity, IHazard, IDamager {

    companion object {
        const val TAG = "CeilingCrusher"
        private const val DROP_SPEED = 10f
        private const val RAISE_SPEED = 3f
        private const val DROP_DELAY = 0.1f
        private const val RAISE_DELAY = 0.75f
        private var crusherRegion: TextureRegion? = null
        private var barRegion: TextureRegion? = null
    }

    enum class CeilingCrusherState {
        WAITING, DROPPING, RAISING
    }

    private val dropDelayTimer = Timer(DROP_DELAY)
    private val raiseDelayTimer = Timer(RAISE_DELAY)
    private var block: Block? = null
    private lateinit var start: Vector2
    private lateinit var ceilingCrusherState: CeilingCrusherState

    private var height = 0

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG

    override fun init() {
        if (crusherRegion == null || barRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            crusherRegion = atlas.findRegion("CeilingCrusher/Crusher")
            barRegion = atlas.findRegion("CeilingCrusher/Bar")
        }
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.onSpawn(spawnProps)
        height = spawnProps.getOrDefault(ConstKeys.HEIGHT, 1, Int::class)
        body.setSize(0.75f * ConstVals.PPM, height * 2.25f * ConstVals.PPM)
        start = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(start)
        for (i in 1 until height) {
            val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
            sprite.setRegion(barRegion!!)
            sprite.setSize(2.25f * ConstVals.PPM)
            sprites.put("bar_$i", sprite)
            putUpdateFunction("bar_$i") { _, _ ->
                val centerX = body.getCenter().x
                val bottomY = body.getPositionPoint(Position.BOTTOM_CENTER).y + (i * 2.25f * ConstVals.PPM)
                sprite.setPosition(Vector2(centerX, bottomY), Position.BOTTOM_CENTER)
            }
        }
        ceilingCrusherState = CeilingCrusherState.WAITING
        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD) as Block
        block!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(2f * ConstVals.PPM, 0.5f * ConstVals.PPM),
                ConstKeys.BLOCK_FILTERS pairTo objectSetOf(TAG),
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
            )
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Destroying CeilingCrusher")
        super.onDestroy()
        val keysToRemove = Array<Any>()
        sprites.keys().forEach { if (it != SpritesComponent.DEFAULT_KEY) keysToRemove.add(it) }
        keysToRemove.forEach { sprites.remove(it) }
        block?.destroy()
        block = null
    }

    private fun setToCrushIfTarget(fixture: IFixture) {
        val entity = fixture.getEntity()
        if (entity is Megaman || entity is AbstractEnemy) {
            GameLogger.debug(TAG, "setToCrushIfTarget: entity = $entity")
            ceilingCrusherState = CeilingCrusherState.DROPPING
            dropDelayTimer.reset()
        }
    }

    private fun setToStopIfBlock(fixture: IFixture) {
        if (fixture.getEntity() != block && fixture.getType() == FixtureType.BLOCK) {
            GameLogger.debug(TAG, "setToStopIfBlock: fixture = $fixture")
            ceilingCrusherState = CeilingCrusherState.RAISING
            body.physics.velocity.setZero()
            raiseDelayTimer.reset()
            requestToPlaySound(SoundAsset.TIME_STOPPER_SOUND, false)
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val consumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        consumerFixture.attachedToBody = false
        consumerFixture.setConsumer { _, fixture -> setToCrushIfTarget(fixture) }
        body.addFixture(consumerFixture)
        consumerFixture.drawingColor = Color.CYAN
        debugShapes.add { consumerFixture }

        val crusherFixture = Fixture(
            body, FixtureType.DEATH, GameRectangle().setSize(
                2f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        crusherFixture.putProperty(ConstKeys.INSTANT, true)
        crusherFixture.attachedToBody = false
        body.addFixture(crusherFixture)
        crusherFixture.drawingColor = Color.RED
        debugShapes.add { crusherFixture }

        val bottomFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                1.15f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        bottomFixture.attachedToBody = false
        bottomFixture.setConsumer { _, fixture -> setToStopIfBlock(fixture) }
        body.addFixture(bottomFixture)
        bottomFixture.drawingColor = Color.GREEN
        debugShapes.add { bottomFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.rawShape as GameRectangle).setSize(0.5f * ConstVals.PPM, body.getHeight())
            (shieldFixture.rawShape as GameRectangle).setSize(0.5f * ConstVals.PPM, body.getHeight())

            bottomFixture.setActive(ceilingCrusherState == CeilingCrusherState.DROPPING)

            consumerFixture.setActive(ceilingCrusherState == CeilingCrusherState.WAITING)
            val consumer = consumerFixture.rawShape as GameRectangle
            consumer.setSize(2f * ConstVals.PPM, 2.25f * ConstVals.PPM * height)
            consumer.setTopCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))

            crusherFixture.setActive(ceilingCrusherState == CeilingCrusherState.DROPPING)
            val crusher = crusherFixture.rawShape as GameRectangle
            crusher.setCenter(body.getPositionPoint(Position.BOTTOM_CENTER))

            val feet = bottomFixture.rawShape as GameRectangle
            feet.setCenter(body.getPositionPoint(Position.BOTTOM_CENTER))

            block!!.body.setBottomCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (ceilingCrusherState) {
            CeilingCrusherState.WAITING -> {
                body.setBottomCenterToPoint(start)
                body.physics.velocity.setZero()
            }

            CeilingCrusherState.DROPPING -> {
                dropDelayTimer.update(delta)
                if (dropDelayTimer.isFinished()) body.physics.velocity.set(0f, -DROP_SPEED * ConstVals.PPM)
            }

            CeilingCrusherState.RAISING -> {
                raiseDelayTimer.update(delta)
                if (raiseDelayTimer.isFinished()) {
                    body.physics.velocity.set(0f, RAISE_SPEED * ConstVals.PPM)
                    if (body.getPositionPoint(Position.BOTTOM_CENTER).y >= start.y) {
                        GameLogger.debug(TAG, "Crusher raised to start")
                        body.setBottomCenterToPoint(start)
                        ceilingCrusherState = CeilingCrusherState.WAITING
                    }
                }
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setRegion(crusherRegion!!)
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(
                body.getPositionPoint(Position.BOTTOM_CENTER),
                Position.BOTTOM_CENTER
            )
        }
        return spritesComponent
    }
}
