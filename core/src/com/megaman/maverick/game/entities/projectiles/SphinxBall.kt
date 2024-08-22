package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensingAny

class SphinxBall(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "SphinxBall"
        private const val SPIN_DELAY = 0.1f
        private const val GRAVITY = -0.15f
        private const val SINK_SPEED = 0.75f
        private var region: TextureRegion? = null
    }

    lateinit var feetFixture: Fixture
        private set
    var spinning = true

    private val spinDelayTimer = Timer(SPIN_DELAY)
    private var xSpeed = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        xSpeed = spawnProps.getOrDefault(ConstKeys.X, 0f, Float::class)
        body.physics.velocity.set(xSpeed * ConstVals.PPM, 0f)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        spinning = spawnProps.getOrDefault(ConstKeys.SPIN, true, Boolean::class)

        spinDelayTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.takeFrictionFromOthers = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.setSize(1.35f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.675f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.675f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.675f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM
            )
        )
        feetFixture.offsetFromBodyCenter.y = -0.675f * ConstVals.PPM
        feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, false)
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        leftFixture.offsetFromBodyCenter.x = -0.75f * ConstVals.PPM
        leftFixture.offsetFromBodyCenter.y = ConstVals.PPM.toFloat()
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.rawShape.color = Color.YELLOW
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        rightFixture.offsetFromBodyCenter.x = 0.75f * ConstVals.PPM
        rightFixture.offsetFromBodyCenter.y = ConstVals.PPM.toFloat()
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.rawShape.color = Color.YELLOW
        debugShapes.add { rightFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravityOn = !body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)
            if (body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) {
                body.physics.velocity.set(0f, -SINK_SPEED * ConstVals.PPM)
                spinning = false
            } else if (body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND))
                body.physics.velocity.set(xSpeed * ConstVals.PPM, 0f)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.15f * ConstVals.PPM)
        sprite.setRegion(region!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setOriginCenter()
            if (spinning) {
                spinDelayTimer.update(delta)
                if (spinDelayTimer.isFinished()) {
                    _sprite.rotation += 45f
                    spinDelayTimer.reset()
                }
            }
        }
        return spritesComponent
    }
}