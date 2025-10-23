package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.world.body.*

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

    private var block: Block? = null
    private val spinDelayTimer = Timer(SPIN_DELAY)
    private var xSpeed = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)
        xSpeed = spawnProps.getOrDefault(ConstKeys.X, 0f, Float::class)
        body.physics.velocity.set(xSpeed * ConstVals.PPM, 0f)
        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn
        spinning = spawnProps.getOrDefault(ConstKeys.SPIN, true, Boolean::class)
        spinDelayTimer.reset()
        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        block!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(1.35f * ConstVals.PPM, 0.1f * ConstVals.PPM),
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                )
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        block?.destroy()
        block = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (!spinning) {
            block?.destroy()
            block = null
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.setSize(1.35f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.675f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        debugShapes.add { projectileFixture}

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.6f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.675f * ConstVals.PPM))
        body.addFixture(shieldFixture)

        feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM
            )
        )
        feetFixture.offsetFromBodyAttachment.y = -0.675f * ConstVals.PPM
        feetFixture.setShouldStickToBlock(false)
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        leftFixture.offsetFromBodyAttachment.x = -0.75f * ConstVals.PPM
        leftFixture.offsetFromBodyAttachment.y = ConstVals.PPM.toFloat()
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture}

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        rightFixture.offsetFromBodyAttachment.x = 0.75f * ConstVals.PPM
        rightFixture.offsetFromBodyAttachment.y = ConstVals.PPM.toFloat()
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            block?.body?.setTopCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
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
        spritesComponent.putPreProcess { delta, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            if (spinning) {
                spinDelayTimer.update(delta)
                if (spinDelayTimer.isFinished()) {
                    sprite.rotation += 45f
                    spinDelayTimer.reset()
                }
            }
        }
        return spritesComponent
    }
}
