package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import com.megaman.maverick.game.world.setConsumer

class CeilingCrusher(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity, IAudioEntity, IHazard,
    IDamager {

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
    private lateinit var state: CeilingCrusherState

    private var height = 0

    override fun init() {
        if (crusherRegion == null || barRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            crusherRegion = atlas.findRegion("CeilingCrusher/Crusher")
            barRegion = atlas.findRegion("CeilingCrusher/Bar")
        }
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props = $spawnProps")
        super.spawn(spawnProps)
        height = spawnProps.getOrDefault(ConstKeys.HEIGHT, 1, Int::class)
        body.setSize(0.75f * ConstVals.PPM, height * 2.25f * ConstVals.PPM)
        start = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(start)
        for (i in 1 until height) {
            val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
            sprite.setRegion(barRegion!!)
            sprite.setSize(2.25f * ConstVals.PPM)
            sprites.put("bar_$i", sprite)
            putUpdateFunction("bar_$i") { _, _sprite ->
                val centerX = body.getCenter().x
                val bottomY = body.getBottomCenterPoint().y + (i * 2.25f * ConstVals.PPM)
                _sprite.setPosition(Vector2(centerX, bottomY), Position.BOTTOM_CENTER)
            }
        }
        state = CeilingCrusherState.WAITING
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Destroying CeilingCrusher")
        super<GameEntity>.onDestroy()
        val keysToRemove = Array<String>()
        sprites.keys().forEach { if (it != SpritesComponent.SPRITE) keysToRemove.add(it) }
        keysToRemove.forEach { sprites.remove(it) }
        block?.kill()
        block = null
    }

    private fun setToCrushIfTarget(fixture: IFixture) {
        val entity = fixture.getEntity()
        if (entity is Megaman || entity is AbstractEnemy) {
            GameLogger.debug(TAG, "setToCrushIfTarget: entity = $entity")
            state = CeilingCrusherState.DROPPING
            dropDelayTimer.reset()
        }
    }

    private fun setToStopIfBlock(fixture: IFixture) {
        if (fixture.getFixtureType() == FixtureType.BLOCK) {
            GameLogger.debug(TAG, "setToStopIfBlock: fixture = $fixture")
            state = CeilingCrusherState.RAISING
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
        shieldFixture.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        val consumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle())
        consumerFixture.attachedToBody = false
        consumerFixture.setConsumer { _, fixture -> setToCrushIfTarget(fixture) }
        body.addFixture(consumerFixture)
        consumerFixture.rawShape.color = Color.CYAN
        debugShapes.add { consumerFixture.getShape() }

        val crusherFixture = Fixture(
            body, FixtureType.DEATH, GameRectangle().setSize(
                2f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        crusherFixture.putProperty(ConstKeys.INSTANT, true)
        crusherFixture.attachedToBody = false
        body.addFixture(crusherFixture)
        crusherFixture.rawShape.color = Color.RED
        debugShapes.add { crusherFixture.getShape() }

        val bottomFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                1.15f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        bottomFixture.attachedToBody = false
        bottomFixture.setConsumer { _, fixture -> setToStopIfBlock(fixture) }
        body.addFixture(bottomFixture)
        bottomFixture.rawShape.color = Color.GREEN
        debugShapes.add { bottomFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.rawShape as GameRectangle).setSize(0.5f * ConstVals.PPM, body.height)
            (shieldFixture.rawShape as GameRectangle).setSize(0.5f * ConstVals.PPM, body.height)

            bottomFixture.active = state == CeilingCrusherState.DROPPING

            consumerFixture.active = state == CeilingCrusherState.WAITING
            val consumer = consumerFixture.rawShape as GameRectangle
            consumer.setSize(1.15f * ConstVals.PPM, 2.25f * ConstVals.PPM * height)
            consumer.setTopCenterToPoint(body.getBottomCenterPoint())

            crusherFixture.active = state == CeilingCrusherState.DROPPING
            val crusher = crusherFixture.rawShape as GameRectangle
            crusher.setCenter(body.getBottomCenterPoint())

            val feet = bottomFixture.rawShape as GameRectangle
            feet.setCenter(body.getBottomCenterPoint())
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        when (state) {
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
                    if (body.getBottomCenterPoint().y >= start.y) {
                        GameLogger.debug(TAG, "Crusher raised to start")
                        body.setBottomCenterToPoint(start)
                        state = CeilingCrusherState.WAITING
                    }
                }
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setRegion(crusherRegion!!)
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }
}