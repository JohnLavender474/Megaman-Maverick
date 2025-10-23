package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.coerceX
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
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.FireMetFlame
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class FireMet(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FireMet"
        private const val MOVE_SPEED = 4f
        private const val MAX_SHOOT_X = 8f
        private const val MAX_SHOOT_Y = 2f
        private const val JUMP_IMPULSE_Y = 7f
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f
        private const val MOVE_DUR = 0.75f
        private const val SHOOT_DUR = 0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FireMetState { MOVE, SHOOT }

    override lateinit var facing: Facing

    private val moveTimer = Timer(MOVE_DUR)
    private val shootTimer = Timer(SHOOT_DUR)
    private lateinit var fireMetState: FireMetState
    private var flame: FireMetFlame? = null

    override fun init() {
        damageOverrides.put(Fireball::class, null)
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("walk", atlas.findRegion("$TAG/Walk"))
            regions.put("jump", atlas.findRegion("$TAG/Jump"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS))
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        spawnFlame()

        fireMetState = FireMetState.MOVE
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        moveTimer.reset()
        shootTimer.setToEnd()
    }

    override fun onDestroy() {
        super.onDestroy()

        flame?.destroy()
        flame = null
    }

    private fun spawnFlame() {
        if (flame != null) throw IllegalStateException("Flame must be null before spawning new flame")

        flame = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIRE_MET_FLAME) as FireMetFlame?
        flame!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER)
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (fireMetState) {
                FireMetState.MOVE -> {
                    flame!!.body.setBottomCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
                    flame!!.whooshing = !body.isSensing(BodySense.FEET_ON_GROUND)
                    flame!!.facing = facing

                    if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                        body.physics.velocity.x = MOVE_SPEED * facing.value * ConstVals.PPM

                        moveTimer.update(delta)
                        if (moveTimer.isFinished()) {
                            facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                            shoot()
                            shootTimer.reset()

                            fireMetState = FireMetState.SHOOT
                        }
                    }
                }

                FireMetState.SHOOT -> {
                    body.physics.velocity.x = 0f

                    shootTimer.update(delta)
                    if (shootTimer.isJustFinished()) {
                        spawnFlame()
                        moveTimer.reset()
                        fireMetState = FireMetState.MOVE
                    }
                }
            }
        }
    }

    private fun jump() {
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(MOVE_SPEED * facing.value, JUMP_IMPULSE_Y)
            .scl(ConstVals.PPM.toFloat())
        body.physics.velocity.set(velocity)
    }

    private fun shoot() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getCenter(), megaman.body.getCenter(), JUMP_IMPULSE_Y * ConstVals.PPM,
        ).coerceX(-MAX_SHOOT_X * ConstVals.PPM, MAX_SHOOT_Y * ConstVals.PPM)
        flame!!.launch(impulse)

        flame!!.body.physics.gravityOn = true
        flame = null
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(
                0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val leftSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        leftSideFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftSideFixture.putProperty(ConstKeys.DEATH_LISTENER, false)
        body.addFixture(leftSideFixture)
        debugShapes.add { leftSideFixture }

        val rightSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM
            )
        )
        rightSideFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightSideFixture.putProperty(ConstKeys.DEATH_LISTENER, false)
        body.addFixture(rightSideFixture)
        debugShapes.add { rightSideFixture }

        val leftConsumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftConsumerFixture.offsetFromBodyAttachment.set(-0.75f, -0.75f).scl(ConstVals.PPM.toFloat())
        leftConsumerFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.DEATH, FixtureType.BLOCK) }
        leftConsumerFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.DEATH -> leftConsumerFixture.putProperty(ConstKeys.DEATH, true)
                FixtureType.BLOCK -> leftConsumerFixture.putProperty(ConstKeys.BLOCK, true)
            }
        }
        body.addFixture(leftConsumerFixture)
        debugShapes.add { leftConsumerFixture }

        val rightConsumerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.2f * ConstVals.PPM))
        rightConsumerFixture.offsetFromBodyAttachment.set(0.75f * ConstVals.PPM, -0.75f * ConstVals.PPM)
        rightConsumerFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.DEATH, FixtureType.BLOCK) }
        rightConsumerFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.DEATH -> rightConsumerFixture.putProperty(ConstKeys.DEATH, true)
                FixtureType.BLOCK -> rightConsumerFixture.putProperty(ConstKeys.BLOCK, true)
            }
        }
        body.addFixture(rightConsumerFixture)
        debugShapes.add { rightConsumerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM

            leftConsumerFixture.putProperty(ConstKeys.DEATH, false)
            leftConsumerFixture.putProperty(ConstKeys.BLOCK, false)
            rightConsumerFixture.putProperty(ConstKeys.DEATH, false)
            rightConsumerFixture.putProperty(ConstKeys.BLOCK, false)
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            when {
                isFacing(Facing.LEFT) -> when {
                    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) ||
                        leftConsumerFixture.isProperty(ConstKeys.DEATH, true) ->
                        facing = Facing.RIGHT

                    fireMetState == FireMetState.MOVE &&
                        body.isSensing(BodySense.FEET_ON_GROUND) &&
                        leftConsumerFixture.isProperty(ConstKeys.BLOCK, false) -> {
                        jump()
                        moveTimer.reset()
                    }
                }

                isFacing(Facing.RIGHT) -> when {
                    body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ||
                        rightConsumerFixture.isProperty(ConstKeys.DEATH, true) ->
                        facing = Facing.LEFT

                    fireMetState == FireMetState.MOVE &&
                        body.isSensing(BodySense.FEET_ON_GROUND) &&
                        rightConsumerFixture.isProperty(ConstKeys.BLOCK, false) -> {
                        jump()
                        moveTimer.reset()
                    }
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                !body.isSensing(BodySense.FEET_ON_GROUND) -> "jump"
                !shootTimer.isFinished() -> "shoot"
                else -> "walk"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "walk" pairTo Animation(regions["walk"], 2, 2, 0.1f, true),
            "shoot" pairTo Animation(regions["shoot"], 1, 2, 0.1f, false),
            "jump" pairTo Animation(regions["jump"], 1, 2, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

