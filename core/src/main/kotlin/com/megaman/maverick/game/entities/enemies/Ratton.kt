package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.HeadUtils
import com.megaman.maverick.game.world.body.*

class Ratton(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFreezableEntity, IFaceable {

    companion object {
        const val TAG = "Ratton"

        private const val STAND_DUR = 0.75f

        private const val G_GRAV = -0.01f
        private const val GRAV = -0.15f

        private const val JUMP_X = 5f
        private const val JUMP_Y = 8f

        private const val DEFAULT_FRICTION_X = 1f
        private const val GROUND_FRICTION_X = 5f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(1, 2, gdxArrayOf(0.5f, 0.15f), true),
            "jump" pairTo AnimationDef(1, 2, 0.1f, false),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing
    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }

    private val standTimer = Timer(STAND_DUR)
    private val frozenTimer = Timer(ConstVals.STANDARD_FROZEN_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        standTimer.reset()

        frozen = false
        facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged && !frozen && damager is IFreezerEntity) frozen = true
        return damaged
    }

    override fun canDamage(damageable: IDamageable) = !frozen && super.canDamage(damageable)

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val frozenFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(frozenFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV)

            val frictionX = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_FRICTION_X else DEFAULT_FRICTION_X
            body.physics.defaultFrictionOnSelf.x = frictionX

            HeadUtils.stopJumpingIfHitHead(body)

            frozenFixture.setActive(frozen)
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGEABLE, FixtureType.DAMAGER))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (frozen) {
                body.physics.velocity.x = 0f

                frozenTimer.update(delta)
                if (frozenTimer.isFinished()) {
                    damageTimer.reset()
                    IceShard.spawn5(body.getCenter())
                }

                return@add
            }

            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                standTimer.update(delta)
                if (body.physics.velocity.y <= 0f) body.physics.velocity.x = 0f
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
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                frozen -> "frozen"
                body.isSensing(BodySense.FEET_ON_GROUND) -> "stand"
                else -> "jump"
            }
        }
        val animations = ObjectMap<String, IAnimation>().also { animations ->
            animDefs.forEach { entry ->
                val key = entry.key
                val (rows, columns, durations, loop) = entry.value
                animations.put(key, Animation(regions[key], rows, columns, durations, loop))
            }
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
