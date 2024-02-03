package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
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
import kotlin.reflect.KClass

class PicketJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "PicketJoe"
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = .4f
        private const val THROW_DUR = .5f
        private const val PICKET_IMPULSE_Y = 10f
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, Int>(
            Bullet::class to 5,
            Fireball::class to ConstVals.MAX_HEALTH,
            ChargedShot::class to 20,
            ChargedShotExplosion::class to 5
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

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
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
            if (throwTimer.isFinished())
                facing = if (megaman.body.x >= body.x) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        // shield fixture
        val shieldFixture =
            Fixture(
                GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM), FixtureType.SHIELD
            )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        debugShapes.add { shieldFixture.shape }

        // damager fixture
        val damagerFixture =
            Fixture(
                GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM),
                FixtureType.DAMAGER
            )
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        // damageable fixture
        val damageableFixture =
            Fixture(
                GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM),
                FixtureType.DAMAGEABLE
            )
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        // pre-process
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

        val spritesComponent = SpritesComponent(this, "picket_joe" to sprite)
        spritesComponent.putUpdateFunction("picket_joe") { _, _sprite ->
            _sprite as GameSprite
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (standing) "stand" else "throw" }
        val animations =
            objectMapOf<String, IAnimation>(
                "stand" to Animation(atlas!!.findRegion("PicketJoe/Stand")),
                "throw" to Animation(atlas!!.findRegion("PicketJoe/Throw"), 1, 3, 0.1f, false)
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
        val spawn = body.getCenter()
        spawn.x += 0.1f * ConstVals.PPM * facing.value
        spawn.y += 0.25f * ConstVals.PPM

        val picket = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.PICKET)!!
        val impulseX = (megaman.body.x - body.x) * 0.8f

        game.gameEngine.spawn(
            picket,
            props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to spawn,
                ConstKeys.X to impulseX,
                ConstKeys.Y to PICKET_IMPULSE_Y * ConstVals.PPM
            )
        )
    }
}
