package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class SwinginJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "SwinginJoe"
        private const val BALL_SPEED = 9f
        private const val SETTING_DUR = .8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SwinginJoeState { SWING_EYES_CLOSED, SWING_EYES_OPEN, THROWING }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override lateinit var facing: Facing

    private val loop = Loop(SwinginJoeState.entries.toGdxArray())
    private val currentState: SwinginJoeState
        get() = loop.getCurrent()
    private val stateTimer = Timer(SETTING_DUR)

    override fun init() {
        super.init()
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("swing1", "swing2", "throw").forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                regions.put(key, region)
            }
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        loop.reset()
        stateTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.setActive(currentState == SwinginJoeState.SWING_EYES_CLOSED)
            damageableFixture.setActive(currentState != SwinginJoeState.SWING_EYES_CLOSED)

            when (currentState) {
                SwinginJoeState.SWING_EYES_CLOSED -> {
                    damageableFixture.offsetFromBodyAttachment.x = 0.05f * ConstVals.PPM * -facing.value
                    shieldFixture.offsetFromBodyAttachment.x = 0.1f * ConstVals.PPM * facing.value
                }

                else -> damageableFixture.offsetFromBodyAttachment.x = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

            stateTimer.update(it)
            if (stateTimer.isJustFinished()) {
                val next = loop.next()

                if (next == SwinginJoeState.THROWING) throwBall()

                stateTimer.reset()
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.LEFT, false)
            sprite.translateX(-0.25f * ConstVals.PPM * facing.value)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            when (currentState) {
                SwinginJoeState.SWING_EYES_CLOSED -> "swing1"
                SwinginJoeState.SWING_EYES_OPEN -> "swing2"
                SwinginJoeState.THROWING -> "throw"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "swing1" pairTo Animation(regions["swing1"], 2, 2, 0.1f, true),
            "swing2" pairTo Animation(regions["swing2"], 2, 2, 0.1f, true),
            "throw" pairTo Animation(regions["throw"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwBall() {
        val spawn = body.getCenter().add(0.25f * facing.value * ConstVals.PPM, 0f)

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(BALL_SPEED * ConstVals.PPM * facing.value, 0f)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory,
            ConstKeys.MASK pairTo objectSetOf<KClass<out IDamageable>>(Megaman::class)
        )

        val ball = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.JOEBALL)!!
        ball.spawn(props)
    }
}
