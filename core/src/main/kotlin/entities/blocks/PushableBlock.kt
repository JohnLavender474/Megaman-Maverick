package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*

class PushableBlock(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "PushableBlock"
        private const val DEFAULT_FRICTION_X = 5f
        private const val X_VEL_CLAMP = 8f
        private const val PUSH_IMPULSE = 10f
        private const val GRAVITY = 0.375f
        private const val GROUND_GRAVITY = 0.01f
        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private var block: Block? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf("MetalCrate").forEach {
                regions.put(it, atlas.findRegion(it))
            }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        block!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.BLOCK_FILTERS pairTo TAG,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE),
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(
                    BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM
                ),
            )
        )
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS)
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        block!!.body.set(body)

        if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
            megaman().leftSideFixture.overlaps(body) &&
                megaman().isFacing(Facing.LEFT) &&
                megaman().body.physics.velocity.x < 0f -> {
                val impulse = PUSH_IMPULSE * ConstVals.PPM * delta
                body.physics.velocity.x -= impulse
            }

            megaman().rightSideFixture.overlaps(body) &&
                megaman().isFacing(Facing.RIGHT) &&
                megaman().body.physics.velocity.x > 0f -> {
                val impulse = PUSH_IMPULSE * ConstVals.PPM * delta
                body.physics.velocity.x += impulse
            }
        } else body.physics.velocity.x = 0f
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.x = X_VEL_CLAMP * ConstVals.PPM
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.applyFrictionY = false
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(BODY_WIDTH * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM.toFloat()
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = -gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setRegion(regions["MetalCrate"])
            sprite.setBounds(body.getBodyBounds())
        }
        return spritesComponent
    }

    override fun getEntityType() = EntityType.BLOCK
}
