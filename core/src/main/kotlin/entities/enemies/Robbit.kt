package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Robbit(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "Robbit"

        private var atlas: TextureAtlas? = null

        private const val STAND_DUR = 0.75f
        private const val CROUCH_DUR = 0.25f
        private const val JUMP_DUR = 0.25f

        private const val G_GRAV = -0.001f
        private const val GRAV = -0.15f

        private const val JUMP_X = 6f
        private const val JUMP_Y = 10f
    }

    private enum class RobbitState { STANDING, CROUCHING, JUMPING }

    override var facing = Facing.RIGHT
    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)

    private val robbitLoop = Loop(RobbitState.entries.toTypedArray().toGdxArray())
    private val robbitTimers =
        objectMapOf(
            RobbitState.STANDING pairTo Timer(STAND_DUR),
            RobbitState.CROUCHING pairTo Timer(CROUCH_DUR),
            RobbitState.JUMPING pairTo Timer(JUMP_DUR)
        )
    private val robbitTimer: Timer
        get() = robbitTimers[robbitLoop.getCurrent()]!!

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        robbitLoop.reset()
        robbitTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(2f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (!body.isSensing(BodySense.FEET_ON_GROUND) &&
                body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
                body.physics.velocity.y > 0f
            ) body.physics.velocity.y = 0f

            body.physics.gravity.y =
                ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV)
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (robbitLoop.getCurrent() != RobbitState.JUMPING)
                facing = if (megaman().body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

            robbitTimer.update(it)
            if (robbitTimer.isJustFinished()) {
                val currentState = robbitLoop.getCurrent()
                GameLogger.debug(TAG, "Current state: $currentState")

                val nextState = robbitLoop.next()
                GameLogger.debug(TAG, "Transitioning to state: $nextState")

                if (nextState == RobbitState.JUMPING) {
                    body.physics.velocity.x = JUMP_X * ConstVals.PPM * facing.value
                    body.physics.velocity.y = JUMP_Y * ConstVals.PPM
                }

                robbitTimer.reset()
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(5f * ConstVals.PPM, 4.375f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!body.isSensing(BodySense.FEET_ON_GROUND)) "jump"
            else when (robbitLoop.getCurrent()) {
                RobbitState.STANDING -> "stand"
                RobbitState.CROUCHING -> "crouch"
                RobbitState.JUMPING -> "jump"
            }
        }
        val animations =
            objectMapOf<String, IAnimation>(
                "stand" pairTo Animation(atlas!!.findRegion("Robbit/Stand")),
                "crouch" pairTo Animation(atlas!!.findRegion("Robbit/Crouch")),
                "jump" pairTo Animation(atlas!!.findRegion("Robbit/Jump"))
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
