package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
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
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Cart(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity, IOwnable<IGameEntity>, IFaceable {

    companion object {
        const val TAG = "Cart"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val FRICTION_X = 1.5f
        private const val FRICTION_Y = 1.25f

        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    override lateinit var facing: Facing

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.SPECIALS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val specials = MegaGameEntities.getOfType(getType())
        val otherCart = specials.find { it is Cart && it != this }

        val canSpawn = otherCart == null

        GameLogger.debug(TAG, "canSpawn=$canSpawn, this=${this.hashCode()}, other=${otherCart.hashCode()}")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps, this=${this.hashCode()}")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (FacingUtils.isFacingBlock(this)) swapFacing()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        body.physics.defaultFrictionOnSelf.x = FRICTION_X
        body.physics.defaultFrictionOnSelf.y = FRICTION_Y

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val cartFixture =
            Fixture(body, FixtureType.CART, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        cartFixture.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        cartFixture.putProperty(ConstKeys.ENTITY, this)
        body.addFixture(cartFixture)
        debugShapes.add { cartFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -0.65f * ConstVals.PPM
        body.addFixture(leftFixture)

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = 0.65f * ConstVals.PPM
        body.addFixture(rightFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.4f * ConstVals.PPM
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = when {
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY
                else -> GRAVITY
            } * ConstVals.PPM
        }
        body.forEachFixture { fixture -> fixture.putProperty(ConstKeys.DEATH_LISTENER, false) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.SHIELD))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            /*
            val vel = abs(body.physics.velocity.x)
            if (vel > 0.5f * ConstVals.PPM) "moving_fast"
            else if (vel > 0.1f * ConstVals.PPM) "moving_slow"
            else if (vel > 0.05f * ConstVals.PPM) "moving_slowest
            else */ "idle"
        }
        val animations = objectMapOf<String, IAnimation>(
            /*
            "moving_fast" pairTo Animation(region!!, 1, 2, 0.1f, true),
            "moving_slow" pairTo Animation(region!!, 1, 2, 0.25f, true),
            "moving_slowest" pairTo Animation(region!!, 1, 2, 0.35f, true),
             */
            "idle" pairTo Animation(region!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent() =
        CullablesComponent(
            objectMapOf(
                ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(this, objectSetOf(EventType.PLAYER_SPAWN))
            )
        )

    override fun getTag() = TAG

    override fun getType() = EntityType.SPECIAL
}
