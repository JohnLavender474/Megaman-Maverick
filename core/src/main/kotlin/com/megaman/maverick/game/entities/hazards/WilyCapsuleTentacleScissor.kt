package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.special.WavyTentacleOfJoints.TentacleState
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.roundToInt

class WilyCapsuleTentacleScissor(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IDamager, IHazard, IOwnable<WilyCapsuleTentacle> {

    companion object {
        const val TAG = "WilyCapsuleTentacleScissor"

        private const val SPRITE_WIDTH = 3f
        private const val SPRITE_HEIGHT = 2f
        private const val BODY_WIDTH = 3f
        private const val BODY_HEIGHT = 2f

        // Center fixture: small square covering the pivot
        private const val CENTER_DAMAGER_SIZE = 0.75f

        // Blade fixture: changes size based on state
        private const val CLOSED_BLADE_WIDTH = 0.75f
        private const val CLOSED_BLADE_HEIGHT = 1f
        private const val CHOP_BLADE_WIDTH = 2.5f
        private const val CHOP_BLADE_HEIGHT = 0.75f

        // How far the blade fixture is offset from center along the pointing direction
        private const val BLADE_OFFSET = 0.25f

        private const val CHOP_DURATION = 0.1f

        private const val CLOSED_KEY = "closed"
        private const val CHOP_KEY = "chop"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: WilyCapsuleTentacle? = null

    // Rotation computed in updatables, read by sprites and body
    private var rotation = 0f

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            regions.put(CLOSED_KEY, atlas.findRegion("phase_2/scissors/closed"))
            regions.put(CHOP_KEY, atlas.findRegion("phase_2/scissors/chop"))
        }
        super.init(*params)
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        rotation = 0f

        val center = spawnProps.get(ConstKeys.CENTER, Vector2::class)!!
        body.setCenter(center)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ _ ->
        val prevJoint = owner?.getPenultimateJointPosition(GameObjectPools.fetch(Vector2::class))
        if (prevJoint != null) {
            val tipPos = body.getCenter()


            val dx = tipPos.x - prevJoint.x
            val dy = tipPos.y - prevJoint.y
            val rawAngle = MathUtils.atan2(dx, -dy) * MathUtils.radiansToDegrees
            rotation = (rawAngle / 45f).roundToInt() * 45f
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        // Center damager: always at body center
        val centerDamagerFixture = Fixture(
            body, FixtureType.DAMAGER,
            GameRectangle().setSize(CENTER_DAMAGER_SIZE * ConstVals.PPM)
        )
        body.addFixture(centerDamagerFixture)
        debugShapes.add { centerDamagerFixture }

        // Center shield: same size and position as center damager
        val centerShieldFixture = Fixture(
            body, FixtureType.SHIELD,
            GameRectangle().setSize(CENTER_DAMAGER_SIZE * ConstVals.PPM)
        )
        body.addFixture(centerShieldFixture)
        debugShapes.add { centerShieldFixture }

        // Blade damager: offset from center based on rotation
        val bladeDamagerFixture = Fixture(
            body, FixtureType.DAMAGER,
            GameRectangle().setSize(CLOSED_BLADE_WIDTH * ConstVals.PPM, CLOSED_BLADE_HEIGHT * ConstVals.PPM)
        )
        body.addFixture(bladeDamagerFixture)
        debugShapes.add { bladeDamagerFixture }

        // Blade shield: same size and position as blade damager
        val bladeShieldFixture = Fixture(
            body, FixtureType.SHIELD,
            GameRectangle().setSize(CLOSED_BLADE_WIDTH * ConstVals.PPM, CLOSED_BLADE_HEIGHT * ConstVals.PPM)
        )
        body.addFixture(bladeShieldFixture)
        debugShapes.add { bladeShieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val state = owner?.getTentacleState()
            val closed = state == TentacleState.IDLE || state == TentacleState.RETURNING || state == null

            val bladeW: Float
            val bladeH: Float
            if (closed) {
                bladeW = CLOSED_BLADE_WIDTH
                bladeH = CLOSED_BLADE_HEIGHT
            } else {
                bladeW = CHOP_BLADE_WIDTH
                bladeH = CHOP_BLADE_HEIGHT
            }

            val bladeSize = bladeW * ConstVals.PPM to bladeH * ConstVals.PPM
            (bladeDamagerFixture.rawShape as GameRectangle).setSize(bladeSize.first, bladeSize.second)
            (bladeShieldFixture.rawShape as GameRectangle).setSize(bladeSize.first, bladeSize.second)

            val rotRad = rotation * MathUtils.degreesToRadians
            val dirX = MathUtils.sin(rotRad)
            val dirY = -MathUtils.cos(rotRad)
            val offsetX = dirX * BLADE_OFFSET * ConstVals.PPM
            val offsetY = dirY * BLADE_OFFSET * ConstVals.PPM
            bladeDamagerFixture.offsetFromBodyAttachment.set(offsetX, offsetY)
            bladeShieldFixture.offsetFromBodyAttachment.set(offsetX, offsetY)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 6))
                .also { it.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = rotation
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animator = AnimatorBuilder()
            .setKeySupplier supplier@{
                val state = owner?.getTentacleState()
                return@supplier when (state) {
                    TentacleState.IDLE, TentacleState.RETURNING -> CLOSED_KEY
                    else -> CHOP_KEY
                }
            }
            .addAnimation(CLOSED_KEY, Animation(regions[CLOSED_KEY], 1, 1, 1f, false))
            .addAnimation(CHOP_KEY, Animation(regions[CHOP_KEY], 2, 1, CHOP_DURATION, true))
            .build()
        val component = AnimationsComponent(this)
        component.putAnimator(TAG, animator)
        return component
    }

    override fun getType() = EntityType.HAZARD
}
