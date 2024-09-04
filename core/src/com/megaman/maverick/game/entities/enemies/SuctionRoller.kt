package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball

import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class SuctionRoller(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        private var textureRegion: TextureRegion? = null
        private const val GRAVITY = -.15f
        private const val VEL_X = 2.5f
        private const val VEL_Y = 2.5f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg(15)
    )

    override lateinit var facing: Facing

    private var onWall = false
    private var wasOnWall = false

    override fun init() {
        if (textureRegion == null)
            textureRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "SuctionRoller")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        onWall = false
        wasOnWall = false
        facing = if (game.megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val megaman = game.megaman
            if (!megaman.spawned) return@add

            wasOnWall = onWall
            onWall =
                (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                if (getMegaman().body.getBottomRightPoint().x < body.x) facing = Facing.LEFT
                else if (getMegaman().body.x > body.getBottomRightPoint().x) facing = Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture =
            Fixture(
                body,
                FixtureType.BODY,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        body.addFixture(bodyFixture)

        bodyFixture.rawShape.color = Color.BLUE
        shapes.add { bodyFixture.getShape() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM / 4f, ConstVals.PPM / 32f))
        feetFixture.offsetFromBodyCenter.y = -0.6f * ConstVals.PPM
        body.addFixture(feetFixture)

        feetFixture.rawShape.color = Color.GREEN
        shapes.add { feetFixture.getShape() }

        val leftFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        leftFixture.offsetFromBodyCenter.x = -0.375f * ConstVals.PPM
        leftFixture.offsetFromBodyCenter.y = ConstVals.PPM / 5f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)

        leftFixture.rawShape.color = Color.ORANGE
        shapes.add { leftFixture.getShape() }

        val rightFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        rightFixture.offsetFromBodyCenter.x = 0.375f * ConstVals.PPM
        rightFixture.offsetFromBodyCenter.y = ConstVals.PPM / 5f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)

        rightFixture.rawShape.color = Color.ORANGE
        shapes.add { rightFixture.getShape() }

        val damageableFixture =
            Fixture(
                body,
                FixtureType.DAMAGEABLE,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        body.addFixture(damageableFixture)

        val damagerFixture =
            Fixture(
                body,
                FixtureType.DAMAGER,
                GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            )
        body.addFixture(damagerFixture)

        shapes.add { damagerFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            body.physics.gravity.y =
                if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else GRAVITY * ConstVals.PPM

            if (onWall) {
                if (!wasOnWall) body.physics.velocity.x = 0f
                body.physics.velocity.y = VEL_Y * ConstVals.PPM
            } else {
                if (wasOnWall) body.y += ConstVals.PPM / 10f
                body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
            }
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        sprite.setOriginCenter()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(facing == Facing.RIGHT, false)
            _sprite.hidden = damageBlink

            val position =
                if (onWall) {
                    if (facing == Facing.LEFT) Position.CENTER_LEFT else Position.CENTER_RIGHT
                } else Position.BOTTOM_CENTER

            val bodyPosition =
                if (onWall) {
                    if (position == Position.CENTER_LEFT) body.getCenterLeftPoint()
                    else body.getCenterRightPoint()
                } else body.getBottomCenterPoint()

            _sprite.setPosition(bodyPosition, position)

            _sprite.rotation =
                if (onWall) {
                    if (facing == Facing.LEFT) -90f else 90f
                } else 0f
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(textureRegion!!, 1, 5, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
