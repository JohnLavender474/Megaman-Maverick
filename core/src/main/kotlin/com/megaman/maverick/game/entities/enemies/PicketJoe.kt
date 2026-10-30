package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.coerceX
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.TimeMarkedRunnable
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.projectiles.Picket
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class PicketJoe(services: MegaServices) : AbstractEnemy(services, size = Size.MEDIUM), IFaceable {

    companion object {
        const val TAG = "PicketJoe"

        private const val STAND_DUR = 1f
        private const val THROW_DUR = 0.5f
        private const val THROW_TIME = 0.15f

        private const val MAX_IMPULSE_X = 6f
        private const val PICKET_IMPULSE_Y = 10f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    val standing: Boolean
        get() = !standTimer.isFinished()
    val throwingPickets: Boolean
        get() = !throwTimer.isFinished()

    private val standTimer = Timer(STAND_DUR)
    private val throwTimer = Timer(THROW_DUR)
        .addRunnable(TimeMarkedRunnable(THROW_TIME) { throwPicket() })

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("stand", "throw").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.POSITION) -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            spawnProps.containsKey(ConstKeys.POSITION_SUPPLIER) -> (spawnProps.get(ConstKeys.POSITION_SUPPLIER) as () -> Vector2).invoke()
            else -> spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        }
        body.setBottomCenterToPoint(spawn)

        facing = if (targetPlayer.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

        throwTimer.setToEnd()
        standTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            when {
                standing -> {
                    facing = if (targetPlayer.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

                    standTimer.update(it)
                    if (standTimer.isFinished()) setToThrowingPickets()
                }

                throwingPickets -> {
                    throwTimer.update(it)
                    if (throwTimer.isFinished()) setToStanding()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.5f * ConstVals.PPM
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM)
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { if (shieldFixture.isActive()) shieldFixture else null }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.setActive(standing)

            when {
                standing -> {
                    damageableFixture.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * -facing.value
                    shieldFixture.offsetFromBodyAttachment.x = 0.35f * ConstVals.PPM * facing.value
                }

                else -> damageableFixture.offsetFromBodyAttachment.setZero()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = if (invincible) damageBlink else false

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (standing) "stand" else "throw" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"]),
            "throw" pairTo Animation(regions["throw"], 1, 3, gdxArrayOf(0.15f, 0.25f, 0.1f), false)
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
        if (!overlapsGameCamera()) return

        val spawn = body.getCenter()
        spawn.x += 0.25f * ConstVals.PPM * facing.value
        spawn.y += 0.75f * ConstVals.PPM

        val impulse = MegaUtilMethods
            .calculateJumpImpulse(spawn, targetPlayer.body.getCenter(), PICKET_IMPULSE_Y * ConstVals.PPM)
            .let {
                when (facing) {
                    Facing.LEFT -> it.coerceX(-MAX_IMPULSE_X * ConstVals.PPM, 0f)
                    Facing.RIGHT -> it.coerceX(0f, MAX_IMPULSE_X * ConstVals.PPM)
                }
            }

        val picket = MegaEntityFactory.fetch(Picket::class)!!
        picket.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )
    }
}
