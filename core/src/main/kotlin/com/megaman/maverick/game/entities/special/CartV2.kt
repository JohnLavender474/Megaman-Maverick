package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class CartV2(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CartV2"

        private const val GRAVITY = -0.15f

        private const val FRICTION_X = 1.0125f
        private const val FRICTION_Y = 1f

        private const val MAX_VEL_X = 8f
        private const val IMPULSE_X = 8f

        private var regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("move", "idle").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }

        super.init()

        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
            .setBottomCenterToPoint(
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
            )
        val copyProps = spawnProps.copy()
        copyProps.put(ConstKeys.BOUNDS, bounds)
        copyProps.put(ConstKeys.GRAVITY_ON, true)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$copyProps")

        super.onSpawn(copyProps)

        facing = Facing.valueOf(spawnProps.get(ConstKeys.FACING, String::class)!!.uppercase())
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val shouldImpulse =
            abs(body.physics.velocity.x) < MAX_VEL_X * ConstVals.PPM &&
                body.getBounds().overlaps(megaman.feetFixture.getShape())

        if (shouldImpulse) body.physics.velocity.x += IMPULSE_X * ConstVals.PPM * delta * facing.value
    })

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body
        body.physics.defaultFrictionOnSelf.x = FRICTION_X
        body.physics.defaultFrictionOnSelf.y = FRICTION_Y

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.setEntity(this)
        feetFixture.bodyAttachmentPosition = Position.BOTTOM_CENTER
        body.addFixture(feetFixture)
        debugShapeSuppliers.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.setEntity(this)
        headFixture.bodyAttachmentPosition = Position.TOP_CENTER
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapeSuppliers.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        leftFixture.setEntity(this)
        leftFixture.bodyAttachmentPosition = Position.CENTER_LEFT
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setHitByBodyReceiver { entity, processState ->
            if (processState == ProcessState.BEGIN && entity is AbstractEnemy) entity.depleteHealth()
        }
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapeSuppliers.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        rightFixture.setEntity(this)
        rightFixture.bodyAttachmentPosition = Position.CENTER_RIGHT
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setHitByBodyReceiver { entity, processState ->
            if (processState == ProcessState.BEGIN && entity is AbstractEnemy) entity.depleteHealth()
        }
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapeSuppliers.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            feetFixture.drawingColor = if (body.isSensing(BodySense.FEET_ON_GROUND)) Color.GREEN else Color.DARK_GRAY

            if (body.physics.velocity.x > MAX_VEL_X * ConstVals.PPM)
                body.physics.velocity.x = MAX_VEL_X * ConstVals.PPM
            else if (body.physics.velocity.x < -MAX_VEL_X * ConstVals.PPM)
                body.physics.velocity.x = -MAX_VEL_X * ConstVals.PPM

            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                body.physics.gravity.y = 0f
                body.physics.velocity.y = 0f
            } else body.physics.gravity.y = GRAVITY * ConstVals.PPM

            if (body.physics.velocity.y > 0f && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                body.physics.velocity.y = 0f

            if ((body.physics.velocity.x > 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) ||
                (body.physics.velocity.x < 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
            ) body.physics.velocity.x = 0f
        }

        return component
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (abs(body.physics.velocity.x) < 0.1f * ConstVals.PPM) "idle" else "move" }
                .addAnimations(
                    "idle" pairTo Animation(regions["idle"]),
                    "move" pairTo Animation(regions["move"], 2, 1, 0.1f, true)
                )
                .build()
        )
        .build()

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
