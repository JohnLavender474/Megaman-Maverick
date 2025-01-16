package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.getRandomBool
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.equalsAny
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
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

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

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override lateinit var facing: Facing

    private val closedTimer = Timer(CLOSED_DUR)
    private val transTimer = Timer(TRANS_DUR)
    private val openTimer = Timer(OPEN_DUR)

    private lateinit var toxicBarrelBotState: ToxicBarrelBotState

    private val position = Vector2()
    private var shot = false

    override fun init() {
        if (closedRegion == null || openCenterRegion == null || openTopRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            closedRegion = atlas.findRegion("$TAG/Closed")
            openCenterRegion = atlas.findRegion("$TAG/OpenCenter")
            openTopRegion = atlas.findRegion("$TAG/OpenTop")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        position.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER))
        body.setBottomCenterToPoint(position)

        closedTimer.reset()
        transTimer.reset()
        openTimer.reset()

        toxicBarrelBotState = ToxicBarrelBotState.CLOSED
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        shot = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (toxicBarrelBotState) {
                ToxicBarrelBotState.CLOSED -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    closedTimer.update(delta)
                    if (closedTimer.isFinished()) {
                        val openingTop = getRandomBool()
                        toxicBarrelBotState =
                            if (openingTop) ToxicBarrelBotState.OPENING_TOP else ToxicBarrelBotState.OPENING_CENTER

                        closedTimer.reset()
                    }
                }

                ToxicBarrelBotState.OPENING_TOP, ToxicBarrelBotState.OPENING_CENTER -> {
                    transTimer.update(delta)
                    if (transTimer.isFinished()) {
                        toxicBarrelBotState =
                            if (toxicBarrelBotState == ToxicBarrelBotState.OPENING_TOP) ToxicBarrelBotState.OPEN_TOP
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
                        toxicBarrelBotState =
                            if (toxicBarrelBotState == ToxicBarrelBotState.OPEN_TOP) ToxicBarrelBotState.CLOSING_TOP
                            else ToxicBarrelBotState.CLOSING_CENTER

                        openTimer.reset()
                    }
                }

                ToxicBarrelBotState.CLOSING_TOP, ToxicBarrelBotState.CLOSING_CENTER -> {
                    transTimer.update(delta)
                    if (transTimer.isFinished()) {
                        toxicBarrelBotState = ToxicBarrelBotState.CLOSED
                        shot = false

                        transTimer.reset()
                    }
                }
            }
        }
    }

    private fun shoot() {
        if (toxicBarrelBotState == ToxicBarrelBotState.OPEN_CENTER) {
            val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, -0.25f * ConstVals.PPM)

            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

            val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
            bullet.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    ConstKeys.OWNER pairTo this
                )
            )

            requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
        } else {
            val spawn = body.getCenter().add(
                0.25f * ConstVals.PPM * facing.value,
                0.5f * ConstVals.PPM
            )
            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(GOOP_SHOT_X_IMPULSE * ConstVals.PPM * facing.value, 0f)

            val toxicGoopShot = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.TOXIC_GOOP_SHOT)!!
            toxicGoopShot.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.IMPULSE pairTo impulse
                )
            )

            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setWidth(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.GRAY
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM, 0.65f * ConstVals.PPM)
        )
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(0.65f * ConstVals.PPM, 0.85f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.CYAN
        debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.setHeight(
                when {
                    toxicBarrelBotState.equalsAny(
                        ToxicBarrelBotState.OPENING_TOP,
                        ToxicBarrelBotState.OPEN_TOP,
                        ToxicBarrelBotState.CLOSING_TOP
                    ) -> 2f * ConstVals.PPM

                    else -> 1.5f * ConstVals.PPM
                }
            )
            body.setBottomCenterToPoint(position)

            body.forEachFixture { fixture ->
                fixture as Fixture

                val bounds = fixture.rawShape as GameRectangle

                when (fixture.getType()) {
                    FixtureType.DAMAGEABLE -> {
                        val center: Vector2
                        if (toxicBarrelBotState.equalsAny(
                                ToxicBarrelBotState.OPENING_TOP,
                                ToxicBarrelBotState.OPEN_TOP,
                                ToxicBarrelBotState.CLOSING_TOP
                            )
                        ) {
                            bounds.setWidth(0.85f * ConstVals.PPM)
                            center = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.35f * ConstVals.PPM)
                        } else {
                            bounds.setWidth(0.5f * ConstVals.PPM)
                            center = body.getCenter().sub(0f, 0.05f * ConstVals.PPM)
                        }
                        center.x += 0.2f * ConstVals.PPM * facing.value
                        bounds.setCenter(center)
                        fixture.setActive(toxicBarrelBotState != ToxicBarrelBotState.CLOSED)
                    }

                    FixtureType.SHIELD -> bounds.setBottomCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))
                    else -> bounds.set(body)
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.x += 0.1f * ConstVals.PPM * facing.value
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (toxicBarrelBotState) {
                ToxicBarrelBotState.CLOSED -> "closed"
                ToxicBarrelBotState.OPENING_TOP, ToxicBarrelBotState.OPEN_TOP -> "open_top"
                ToxicBarrelBotState.CLOSING_TOP -> "closing_top"
                ToxicBarrelBotState.OPENING_CENTER, ToxicBarrelBotState.OPEN_CENTER -> "open_center"
                ToxicBarrelBotState.CLOSING_CENTER -> "closing_center"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "closed" pairTo Animation(closedRegion!!),
            "open_top" pairTo Animation(openTopRegion!!, 1, 5, 0.1f, false),
            "closing_top" pairTo Animation(openTopRegion!!, 1, 5, 0.1f, false).reversed(),
            "open_center" pairTo Animation(openCenterRegion!!, 2, 2, 0.1f, false),
            "closing_center" pairTo Animation(openCenterRegion!!, 2, 2, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
