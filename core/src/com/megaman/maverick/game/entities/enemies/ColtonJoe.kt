package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
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
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class ColtonJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "ColtonJoe"
        private const val SHOOT_DUR = 1.5f
        private const val SHOOT_DELAY = 0.25f
        private const val BULLET_SPEED = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override lateinit var facing: Facing

    private val shootTimer = Timer(SHOOT_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private lateinit var scanner: GameRectangle

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("stand", atlas.findRegion("$TAG/stand"))
            regions.put("shoot", atlas.findRegion("$TAG/shoot"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { scanner }
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        scanner = GameRectangle().setSize(10f * ConstVals.PPM, ConstVals.PPM.toFloat())
        shootTimer.setToEnd()
        shootDelayTimer.setToEnd()
        facing = if (body.x < getMegaman().body.x) Facing.RIGHT else Facing.LEFT
    }

    private fun shoot() {
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SMALL_MISSILE)!!
        val position = body.getCenter().add(0.65f * ConstVals.PPM * facing.value, 0.125f * ConstVals.PPM)
        game.engine.spawn(
            bullet,
            props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to position,
                ConstKeys.TRAJECTORY to Vector2(BULLET_SPEED * facing.value * ConstVals.PPM, 0f),
                ConstKeys.DIRECTION to if (isFacing(Facing.LEFT)) Direction.LEFT else Direction.RIGHT,
                ConstKeys.GRAVITY_ON to false
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BLAST_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!shootTimer.isFinished()) {
                shootTimer.update(delta)
                return@add
            }

            val position = if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
            scanner.positionOnPoint(body.getCenter(), position)

            if (shootDelayTimer.isFinished() && scanner.overlaps(getMegaman().body as Rectangle)) shootDelayTimer.reset()

            if (!shootDelayTimer.isFinished()) {
                shootDelayTimer.update(delta)
                if (shootDelayTimer.isJustFinished()) {
                    shoot()
                    shootTimer.reset()
                }
            }

            facing = if (body.x < getMegaman().body.x) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (shootTimer.isFinished()) "stand" else "shoot" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" to Animation(regions.get("stand"), 2, 1, gdxArrayOf(0.75f, 0.15f), true),
            "shoot" to Animation(regions.get("shoot"), 5, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}