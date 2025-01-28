package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
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
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class SpreadExplosion(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDirectional, IOwnable, IDamager, IHazard {

    companion object {
        const val TAG = "GreenExplosion"
        private const val DURATION = 0.3f
        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var owner: IGameEntity? = null

    private val timer = Timer(
        DURATION, gdxArrayOf(
            TimeMarkedRunnable(0.05f) {
                width = 0.5f
                damagerOffset = 0.25f
            },
            TimeMarkedRunnable(0.1f) {
                width = 1f
                damagerOffset = 0.5f
            },
            TimeMarkedRunnable(0.15f) {
                width = 1.5f
                damagerOffset = 0.75f
            },
            TimeMarkedRunnable(0.2f) {
                width = 1f
                damagerOffset = 1f
            },
            TimeMarkedRunnable(0.25f) {
                width = 0.5f
                damagerOffset = 1.5f
            },
        )
    )
    private var width = 0f
    private var damagerOffset = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class) ?: Direction.UP

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        when (direction) {
            Direction.UP -> body.setBottomCenterToPoint(spawn)
            Direction.DOWN -> body.setTopCenterToPoint(spawn)
            Direction.LEFT -> body.setCenterRightToPoint(spawn)
            Direction.RIGHT -> body.setCenterLeftToPoint(spawn)
        }

        width = 0f
        damagerOffset = 0f

        timer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture1)
        debugShapes.add { damagerFixture1 }

        val damagerFixture2 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setHeight(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2 }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture {
                val fixture = it as Fixture
                (fixture.rawShape as GameRectangle).setWidth(width * ConstVals.PPM)
            }
            damagerFixture1.offsetFromBodyAttachment.x = damagerOffset * ConstVals.PPM
            damagerFixture2.offsetFromBodyAttachment.x = -damagerOffset * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = when (direction) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 3, 2, 0.05f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.EXPLOSION

    override fun getTag() = TAG
}
