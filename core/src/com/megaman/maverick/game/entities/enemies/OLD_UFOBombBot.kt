package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

@Deprecated("Should use UFOhNoBot instead")
class OLD_UFOBombBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "UFOBombBot"
        private const val X_VEL = 2f
        private const val DROP_DELAY = 1.5f
        private const val DROP_DURATION = 1.5f
        private var flyRegion: TextureRegion? = null
        private var dropRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val dropDelayTimer = Timer(DROP_DELAY)
    private val dropDurationTimer = Timer(DROP_DURATION)
    private var dropping = false

    override fun init() {
        if (dropRegion == null || flyRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            dropRegion = atlas.findRegion("UFOBombBot/Dropping")
            flyRegion = atlas.findRegion("UFOBombBot/Closed")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        dropping = false
        dropDelayTimer.reset()
        dropDurationTimer.reset()
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    private fun isMegamanUnderMe() = getMegaman().body.getMaxY() <= body.y &&
            getMegaman().body.getCenter().x >= body.x && getMegaman().body.getCenter().x <= body.getMaxX()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (dropping) {
                dropDurationTimer.update(delta)
                if (dropDurationTimer.isFinished()) {
                    dropping = false
                    dropDurationTimer.reset()
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
                }
            } else {
                dropDelayTimer.update(delta)
                if (dropDelayTimer.isFinished() || isMegamanUnderMe()) {
                    dropBomb()
                    dropping = true
                    dropDelayTimer.reset()
                }
            }
        }
    }

    private fun dropBomb() {
        val bomb = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.UFO_BOMB)!!
        bomb.spawn(
            props(
                ConstKeys.POSITION pairTo body.getBottomCenterPoint(),
                ConstKeys.OWNER pairTo this
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM, 0.85f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        val drawables = gdxArrayOf<() -> IDrawableShape?>({ body })
        addComponent(DrawableShapesComponent(debugShapeSuppliers = drawables, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.x = if (dropping) 0f else X_VEL * ConstVals.PPM * facing.value
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (dropping) "drop" else "fly" }
        val animations = objectMapOf<String, IAnimation>(
            "drop" pairTo Animation(dropRegion!!, 1, 3, 0.05f, false),
            "fly" pairTo Animation(flyRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}