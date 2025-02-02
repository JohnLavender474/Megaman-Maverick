package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class BouncingPebble(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "BouncingPebble"
        private const val BOUNCE_VEL_SCALAR = 0.75f
        private const val DEFAULT_MAX_BOUNCES = 3
        private const val GRAVITY = -0.1f
        private var region: TextureRegion? = null
    }

    private var maxBounces = DEFAULT_MAX_BOUNCES
    private var bounces = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        maxBounces = spawnProps.getOrDefault("${ConstKeys.MAX}_${ConstKeys.BOUNCE}", DEFAULT_MAX_BOUNCES, Int::class)
        bounces = 0
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun disintegrate() {
        GameLogger.debug(TAG, "disintegrate()")

        destroy()

        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun bounce(direction: Direction) {
        bounces++
        GameLogger.debug(TAG, "bounce(): direction=$direction, bounces=$bounces")
        if (bounces >= maxBounces) {
            disintegrate()
            return
        }

        body.physics.velocity.let { velocity ->
            when (direction) {
                Direction.UP -> velocity.set(velocity.x, abs(velocity.y) * BOUNCE_VEL_SCALAR)
                Direction.DOWN -> velocity.set(velocity.x, -abs(velocity.y) * BOUNCE_VEL_SCALAR)
                Direction.LEFT -> velocity.set(-abs(velocity.x) * BOUNCE_VEL_SCALAR, velocity.y)
                Direction.RIGHT -> velocity.set(abs(velocity.x) * BOUNCE_VEL_SCALAR, velocity.y)
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.UP) }
        feetFixture.bodyAttachmentPosition = Position.BOTTOM_CENTER
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.bodyAttachmentPosition = Position.TOP_CENTER
        headFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.DOWN) }
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        leftFixture.bodyAttachmentPosition = Position.CENTER_LEFT
        leftFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.RIGHT) }
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM))
        rightFixture.bodyAttachmentPosition = Position.CENTER_RIGHT
        rightFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.RIGHT) }
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite(region!!).also { sprite -> sprite.setSize(0.5f * ConstVals.PPM) })
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()
}
