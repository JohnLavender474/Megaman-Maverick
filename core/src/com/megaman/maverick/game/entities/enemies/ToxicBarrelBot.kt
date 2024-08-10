package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.getRandomBool
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
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class ToxicBarrelBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    enum class ToxicBarrelBotState {
        CLOSED, OPENING_TOP, OPEN_TOP, CLOSING_TOP, OPENING_CENTER, OPEN_CENTER, CLOSING_CENTER
    }

    companion object {
        const val TAG = "ToxicBarrelBot"
        private const val CLOSED_DUR = 1f
        private const val TRANS_DUR = 0.5f
        private const val OPEN_DUR = 1f
        private const val SHOOT_TIME = 0.5f
        private const val BULLET_SPEED = 7.5f
        private const val GOOP_SHOT_X_IMPULSE = 8f
        private var closedRegion: TextureRegion? = null
        private var openCenterRegion: TextureRegion? = null
        private var openTopRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(Bullet::class to dmgNeg(3),
        Bullet::class to dmgNeg(5),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 5 else 3
        })
    override lateinit var facing: Facing

    private val closedTimer = Timer(CLOSED_DUR)
    private val transTimer = Timer(TRANS_DUR)
    private val openTimer = Timer(OPEN_DUR)
    private lateinit var state: ToxicBarrelBotState
    private lateinit var position: Vector2
    private var shot = false

    override fun init() {
        if (closedRegion == null || openCenterRegion == null || openTopRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            closedRegion = atlas.findRegion("ToxicBarrelBot/Closed")
            openCenterRegion = atlas.findRegion("ToxicBarrelBot/OpenCenter")
            openTopRegion = atlas.findRegion("ToxicBarrelBot/OpenTop")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(position)
        closedTimer.reset()
        transTimer.reset()
        openTimer.reset()
        state = ToxicBarrelBotState.CLOSED
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        shot = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                ToxicBarrelBotState.CLOSED -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
                    closedTimer.update(delta)
                    if (closedTimer.isFinished()) {
                        val openingTop = getRandomBool()
                        state = if (openingTop) ToxicBarrelBotState.OPENING_TOP else ToxicBarrelBotState.OPENING_CENTER
                        closedTimer.reset()
                    }
                }

                ToxicBarrelBotState.OPENING_TOP, ToxicBarrelBotState.OPENING_CENTER -> {
                    transTimer.update(delta)
                    if (transTimer.isFinished()) {
                        state = if (state == ToxicBarrelBotState.OPENING_TOP) ToxicBarrelBotState.OPEN_TOP
                        else ToxicBarrelBotState.OPEN_CENTER
                        transTimer.reset()
                    }
                }

                ToxicBarrelBotState.OPEN_TOP, ToxicBarrelBotState.OPEN_CENTER -> {
                    openTimer.update(delta)
                    if (!shot && openTimer.time >= SHOOT_TIME) {
                        shoot()
                        shot = true
                    }
                    if (openTimer.isFinished()) {
                        state = if (state == ToxicBarrelBotState.OPEN_TOP) ToxicBarrelBotState.CLOSING_TOP
                        else ToxicBarrelBotState.CLOSING_CENTER
                        openTimer.reset()
                    }
                }

                ToxicBarrelBotState.CLOSING_TOP, ToxicBarrelBotState.CLOSING_CENTER -> {
                    transTimer.update(delta)
                    if (transTimer.isFinished()) {
                        state = ToxicBarrelBotState.CLOSED
                        transTimer.reset()
                        shot = false
                    }
                }
            }
        }
    }

    private fun shoot() {
        if (state == ToxicBarrelBotState.OPEN_CENTER) {
            val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, -0.05f * ConstVals.PPM)
            val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
            game.engine.spawn(
                bullet, props(
                    ConstKeys.POSITION to spawn,
                    ConstKeys.OWNER to this,
                    ConstKeys.TRAJECTORY to Vector2(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
                )
            )
            requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
        } else {
            val spawn = body.getCenter().add(
                0.25f * ConstVals.PPM * facing.value,
                0.35f * ConstVals.PPM
            )
            val toxicGoopShot = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.TOXIC_GOOP_SHOT)!!
            game.engine.spawn(
                toxicGoopShot, props(
                    ConstKeys.POSITION to spawn,
                    ConstKeys.OWNER to this,
                    ConstKeys.IMPULSE to Vector2(GOOP_SHOT_X_IMPULSE * ConstVals.PPM * facing.value, 0f)
                )
            )
            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setWidth(0.7f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM, 0.65f * ConstVals.PPM)
        )
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.65f * ConstVals.PPM, 0.85f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.CYAN
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.height = if (state.equalsAny(
                    ToxicBarrelBotState.OPENING_TOP,
                    ToxicBarrelBotState.OPEN_TOP,
                    ToxicBarrelBotState.CLOSING_TOP
                )
            ) 1.5f * ConstVals.PPM else 0.85f * ConstVals.PPM
            body.setBottomCenterToPoint(position)

            body.fixtures.forEach { entry ->
                val fixture = entry.second as Fixture
                val bounds = fixture.rawShape as GameRectangle

                when (fixture.type) {
                    FixtureType.DAMAGEABLE -> {
                        val center: Vector2
                        if (state.equalsAny(
                                ToxicBarrelBotState.OPENING_TOP,
                                ToxicBarrelBotState.OPEN_TOP,
                                ToxicBarrelBotState.CLOSING_TOP
                            )
                        ) {
                            bounds.setWidth(0.85f * ConstVals.PPM)
                            center = body.getTopCenterPoint().sub(0f, 0.35f * ConstVals.PPM)
                        } else {
                            bounds.setWidth(0.5f * ConstVals.PPM)
                            center = body.getCenter().sub(0f, 0.05f * ConstVals.PPM)
                        }
                        center.x += 0.2f * ConstVals.PPM * facing.value
                        bounds.setCenter(center)
                        fixture.active = state != ToxicBarrelBotState.CLOSED
                    }

                    FixtureType.SHIELD -> bounds.setBottomCenterToPoint(body.getBottomCenterPoint())
                    else -> bounds.set(body)
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.15f * ConstVals.PPM, 1.85f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.x += 0.1f * ConstVals.PPM * facing.value
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (state) {
                ToxicBarrelBotState.CLOSED -> "closed"
                ToxicBarrelBotState.OPENING_TOP, ToxicBarrelBotState.OPEN_TOP -> "open_top"
                ToxicBarrelBotState.CLOSING_TOP -> "closing_top"
                ToxicBarrelBotState.OPENING_CENTER, ToxicBarrelBotState.OPEN_CENTER -> "open_center"
                ToxicBarrelBotState.CLOSING_CENTER -> "closing_center"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "closed" to Animation(closedRegion!!),
            "open_top" to Animation(openTopRegion!!, 1, 5, 0.1f, false),
            "closing_top" to Animation(openTopRegion!!, 1, 5, 0.1f, false).reversed(),
            "open_center" to Animation(openCenterRegion!!, 2, 2, 0.1f, false),
            "closing_center" to Animation(openCenterRegion!!, 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}