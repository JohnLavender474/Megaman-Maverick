package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class HeartTank(game: MegamanMaverickGame) : MegaGameEntity(game), ItemEntity, IBodyEntity, ISpritesEntity,
    IDirectional {

    companion object {
        const val TAG = "HeartTank"
        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    lateinit var heartTank: MegaHeartTank
        private set

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ITEMS_1.source, TAG)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        heartTank = MegaHeartTank.get(spawnProps.get(ConstKeys.VALUE, String::class)!!.uppercase())
        return !megaman.hasHeartTank(heartTank)
    }

    override fun onSpawn(spawnProps: Properties) {
        if (!this::heartTank.isInitialized) throw IllegalStateException("Heart tank value is not initialized")

        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, megaman.direction.name, String::class).uppercase()
        )

        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.positionOnPoint(spawn, position)
    }

    override fun contactWithPlayer(megaman: Megaman) {
        destroy()

        game.eventsMan.submitEvent(Event(EventType.ATTAIN_HEART_TANK, props(ConstKeys.VALUE pairTo heartTank)))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        direction = megaman.direction
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.let {
                when (direction) {
                    Direction.UP -> it.set(0f, -gravity)
                    Direction.DOWN -> it.set(0f, gravity)
                    Direction.LEFT -> it.set(gravity, 0f)
                    Direction.RIGHT -> it.set(-gravity, 0f)
                }.scl(ConstVals.PPM.toFloat())
            }
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.ITEM))
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.ITEM

    override fun getTag() = TAG
}
