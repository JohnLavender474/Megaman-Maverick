package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Ratton(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFaceable {

    companion object {
        const val TAG = "Ratton"
        private const val STAND_DUR = 0.75f
        private const val G_GRAV = -0.01f
        private const val GRAV = -0.15f
        private const val JUMP_X = 5f
        private const val JUMP_Y = 8f
        private const val DEFAULT_FRICTION_X = 1f
        private const val GROUND_FRICTION_X = 5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)

    override fun init() {
        super.init()
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("stand", "jump").forEach {
                val region = atlas.findRegion("$TAG/$it")
                regions.put(it, region)
            }
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        standTimer.reset()
        facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture}

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture}

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV)

            val frictionX = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_FRICTION_X else DEFAULT_FRICTION_X
            body.physics.defaultFrictionOnSelf.x = frictionX

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGEABLE, FixtureType.DAMAGER))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                standTimer.update(it)
                facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
            }

            if (standTimer.isFinished()) {
                standTimer.reset()
                body.physics.velocity.x = JUMP_X * facing.value * ConstVals.PPM
                body.physics.velocity.y = JUMP_Y * ConstVals.PPM
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (body.isSensing(BodySense.FEET_ON_GROUND)) "Stand" else "Jump" }
        val animations = objectMapOf<String, IAnimation>(
            "Stand" pairTo Animation(regions["stand"], 1, 2, gdxArrayOf(0.5f, 0.15f), true),
            "Jump" pairTo Animation(regions["jump"], 1, 2, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
