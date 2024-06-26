package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
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
import kotlin.reflect.KClass

class UFOBombBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "UFOBombBot"
        private const val X_VEL = 3f
        private const val DROP_DELAY = 1.5f
        private const val DROP_DURATION = 1f
        private var flyRegion: TextureRegion? = null
        private var dropRegion: TextureRegion? = null
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

    private val dropDelayTimer = Timer(DROP_DELAY)
    private val dropDurationTimer = Timer(DROP_DURATION)
    private var dropping = false

    override fun init() {
        if (dropRegion == null || flyRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            dropRegion = atlas.findRegion("UFOBombBot/Dropping")
            flyRegion = atlas.findRegion("UFOBombBot/Closed")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        dropping = false
        dropDelayTimer.reset()
        dropDurationTimer.reset()
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    private fun isMegamanUnderMe() = megaman.body.getMaxY() <= body.y &&
            megaman.body.getCenter().x >= body.x && megaman.body.getCenter().x <= body.getMaxX()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (dropping) {
                dropDurationTimer.update(delta)
                if (dropDurationTimer.isFinished()) {
                    dropping = false
                    dropDurationTimer.reset()
                    facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
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
        game.engine.spawn(bomb, props(
            ConstKeys.POSITION to body.getBottomCenterPoint(),
            ConstKeys.OWNER to this
        ))
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
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = drawables, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.x = if (dropping) 0f else X_VEL * ConstVals.PPM * facing.value
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
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
            "drop" to Animation(dropRegion!!, 1, 3, 0.05f, false),
            "fly" to Animation(flyRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}