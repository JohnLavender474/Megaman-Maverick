package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
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
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.math.abs
import kotlin.reflect.KClass

class PicketJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "PicketJoe"
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = 0.5f
        private const val THROW_DUR = 0.4f
        private const val PICKET_IMPULSE_Y = 10f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 5
        }
    )

    override var facing = Facing.RIGHT

    val standing: Boolean
        get() = !standTimer.isFinished()

    val throwingPickets: Boolean
        get() = !throwTimer.isFinished()

    private val standTimer = Timer(STAND_DUR)
    private val throwTimer = Timer(THROW_DUR)

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        throwTimer.setRunnables(gdxArrayOf(TimeMarkedRunnable(0.2f) { throwPicket() }))
        addComponent(defineAnimationsComponent())
    }

    @Suppress("UNCHECKED_CAST")
    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn =
            if (spawnProps.containsKey(ConstKeys.POSITION))
                spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else if (spawnProps.containsKey(ConstKeys.POSITION_SUPPLIER))
                (spawnProps.get(ConstKeys.POSITION_SUPPLIER) as () -> Vector2).invoke()
            else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        throwTimer.setToEnd()
        standTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.x >= body.x) Facing.RIGHT else Facing.LEFT
            if (standing) {
                standTimer.update(it)
                if (standTimer.isFinished()) setToThrowingPickets()
            } else if (throwingPickets) {
                throwTimer.update(it)
                if (throwTimer.isFinished()) setToStanding()
            }
            if (throwTimer.isFinished()) facing = if (megaman.body.x >= body.x) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.YELLOW
        debugShapes.add { bodyFixture.getShape() }

        val feetFixture = Fixture(
            body, FixtureType.FEET,
            GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM)
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            shieldFixture.active = standing
            if (standing) {
                damageableFixture.offsetFromBodyCenter.x = 0.25f * ConstVals.PPM * -facing.value
                shieldFixture.offsetFromBodyCenter.x = 0.35f * ConstVals.PPM * facing.value
            } else damageableFixture.offsetFromBodyCenter.setZero()
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = if (invincible) damageBlink else false
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (standing) "stand" else "throw" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" to Animation(atlas!!.findRegion("PicketJoe/Stand")),
            "throw" to Animation(atlas!!.findRegion("PicketJoe/Throw"), 1, 4, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun setToStanding() {
        standTimer.reset()
        throwTimer.setToEnd()
    }

    private fun setToThrowingPickets() {
        standTimer.setToEnd()
        throwTimer.reset()
    }

    private fun throwPicket() {
        if (!isInGameCamBounds()) return

        val spawn = body.getCenter()
        spawn.x += 0.1f * ConstVals.PPM * facing.value
        spawn.y += 0.25f * ConstVals.PPM

        val picket = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.PICKET)!!
        val xFactor = 1f - ((abs(megaman.body.y - body.y) / ConstVals.PPM) / 10f) + 0.2f
        GameLogger.debug(TAG, "throwPicket(): xFactor: $xFactor")
        val impulseX = (megaman.body.x - body.x) * xFactor

        game.engine.spawn(
            picket, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to spawn,
                ConstKeys.X to impulseX,
                ConstKeys.Y to PICKET_IMPULSE_Y * ConstVals.PPM
            )
        )
    }

}
