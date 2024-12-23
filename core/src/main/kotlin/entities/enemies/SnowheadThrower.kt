package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
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
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class SnowheadThrower(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SnowheadThrower"
        private const val STAND_DUR = 1.25f
        private const val THROW_DUR = 0.6f
        private const val THROW_DELAY = 0.1f
        private const val SNOW_HEAD_X_VEL = 8f
        private const val SNOW_HEAD_Y_VEL = 8f
        private var standRegion: TextureRegion? = null
        private var throwRegion: TextureRegion? = null
    }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val throwTimer = Timer(THROW_DUR)
    private val throwDelay = Timer(THROW_DELAY)
    private var throwing = false

    override fun init() {
        if (standRegion == null || throwRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            standRegion = atlas.findRegion("SnowheadThrower/Stand")
            throwRegion = atlas.findRegion("SnowheadThrower/Throw")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        throwTimer.reset()
        throwDelay.reset()
        standTimer.reset()

        throwing = false
        facing = if (megaman().body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun explode(explosionProps: Properties?) {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    private fun throwHead() {
        val spawn = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.1f * ConstVals.PPM)
        val trajectory = Vector2(SNOW_HEAD_X_VEL * facing.value, SNOW_HEAD_Y_VEL).scl(ConstVals.PPM.toFloat())
        val snowhead = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOW_HEAD)!!
        snowhead.spawn(
            props(
                ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo spawn, ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (throwing) {
                throwDelay.update(delta)
                if (throwDelay.isJustFinished()) throwHead()

                throwTimer.update(delta)
                if (throwTimer.isFinished()) {
                    throwTimer.reset()
                    throwDelay.reset()
                    throwing = false
                }
            } else {
                facing = if (megaman().body.getCenter().x < body.getCenter().x) Facing.LEFT else Facing.RIGHT

                standTimer.update(delta)
                if (standTimer.isFinished()) {
                    standTimer.reset()
                    throwing = true
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (throwing) "throwing" else "standing" }
        val animations = objectMapOf<String, IAnimation>(
            "standing" pairTo Animation(standRegion!!, 1, 3, 0.1f, false),
            "throwing" pairTo Animation(throwRegion!!, 1, 5, 0.075f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

