package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball

import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class SwinginJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    private enum class SwinginJoeSetting {
        SWING_EYES_CLOSED, SWING_EYES_OPEN, THROWING
    }

    companion object {
        private var atlas: TextureAtlas? = null
        private const val BALL_SPEED = 9f
        private const val SETTING_DUR = .8f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(5),
        Fireball::class pairTo dmgNeg(15),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 5
        }, ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 5 else 3
        }
    )
    override lateinit var facing: Facing

    private lateinit var setting: SwinginJoeSetting
    private lateinit var type: String
    private val settingTimer = Timer(SETTING_DUR)

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        settingTimer.reset()
        setting = SwinginJoeSetting.SWING_EYES_CLOSED
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)
        type = if (spawnProps.containsKey(ConstKeys.TYPE))
            spawnProps.get(ConstKeys.TYPE, String::class)!! else ""
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture =
            Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM),
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM),
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.5f * ConstVals.PPM, 1.25f * ConstVals.PPM)
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            shieldFixture.active = setting == SwinginJoeSetting.SWING_EYES_CLOSED
            damageableFixture.active = setting != SwinginJoeSetting.SWING_EYES_CLOSED
            if (setting == SwinginJoeSetting.SWING_EYES_CLOSED) {
                damageableFixture.offsetFromBodyCenter.x = 0.05f * ConstVals.PPM * -facing.value
                shieldFixture.offsetFromBodyCenter.x = 0.1f * ConstVals.PPM * facing.value
            } else damageableFixture.offsetFromBodyCenter.x = 0f
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val SpritesComponent = SpritesComponent(sprite)
        SpritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(facing == Facing.LEFT, false)
            _sprite.translateX(-0.25f * ConstVals.PPM * facing.value)
        }
        return SpritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (getMegaman().body.x > body.x) Facing.RIGHT else Facing.LEFT
            settingTimer.update(it)
            if (settingTimer.isJustFinished()) {
                val index = (setting.ordinal + 1) % SwinginJoeSetting.values().size
                setting = SwinginJoeSetting.values()[index]
                if (setting == SwinginJoeSetting.THROWING) shoot()
                settingTimer.reset()
            }
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            type + when (setting) {
                SwinginJoeSetting.SWING_EYES_CLOSED -> "SwingBall1"
                SwinginJoeSetting.SWING_EYES_OPEN -> "SwingBall2"
                SwinginJoeSetting.THROWING -> "ThrowBall"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "SwingBall1" pairTo Animation(atlas!!.findRegion("SwinginJoe/SwingBall1"), 1, 4, 0.1f, true),
            "SwingBall2" pairTo Animation(atlas!!.findRegion("SwinginJoe/SwingBall2"), 1, 4, 0.1f, true),
            "ThrowBall" pairTo Animation(atlas!!.findRegion("SwinginJoe/ThrowBall")),
            "SnowSwingBall1" pairTo Animation(atlas!!.findRegion("SwinginJoe/SnowSwingBall1"), 1, 4, 0.1f, true),
            "SnowSwingBall2" pairTo Animation(atlas!!.findRegion("SwinginJoe/SnowSwingBall2"), 1, 4, 0.1f, true),
            "SnowThrowBall" pairTo Animation(atlas!!.findRegion("SwinginJoe/SnowThrowBall")),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shoot() {
        val spawn = body.getCenter().add(0.2f * facing.value * ConstVals.PPM, 0.15f * ConstVals.PPM)
        val props = props(
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TYPE pairTo type,
            ConstKeys.OWNER pairTo this,
            ConstKeys.TRAJECTORY pairTo Vector2().set(BALL_SPEED * ConstVals.PPM * facing.value, 0f),
            ConstKeys.MASK pairTo objectSetOf<KClass<out IDamageable>>(Megaman::class)
        )
        val joeBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.JOEBALL)!!
        joeBall.spawn(props)
    }
}
