package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.hazards.UnderwaterFan
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class GapingFish(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "GapingFish"
        private var atlas: TextureAtlas? = null
        private const val HORIZ_SPEED = 2f
        private const val VERT_SPEED = 1.25f
        private const val CHOMP_DUR = 1.25f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class to dmgNeg(15),
        UnderwaterFan::class to dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    val chomping: Boolean
        get() = !chompTimer.isFinished()

    private val chompTimer = Timer(CHOMP_DUR)

    override fun init() {
        GameLogger.debug(TAG, "Initializing GapingFish")
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning GapingFish with props = $spawnProps")
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        chompTimer.setToEnd()
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "GapingFish on destroy")
        super.onDestroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        super.onDamageInflictedTo(damageable)
        if (damageable is Megaman) chompTimer.reset()
    }


    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            GameLogger.debug(
                TAG,
                "GapingFish update. In water = ${body.isSensing(BodySense.IN_WATER)}. Invincible = " +
                        "$invincible. Chomping = $chomping. Position = ${body.getPosition()}"
            )

            chompTimer.update(it)

            val megamanBody = getMegaman().body

            if (body.x >= megamanBody.getMaxX()) facing = Facing.LEFT
            else if (body.getMaxX() <= megamanBody.x) facing = Facing.RIGHT

            if (invincible || chomping) body.physics.velocity.setZero()
            else {
                val vel = body.physics.velocity
                vel.x = HORIZ_SPEED * ConstVals.PPM * facing.value
                if (body.isSensing(BodySense.IN_WATER) || megamanBody.y < body.y) {
                    if (megamanBody.y >= body.y && megamanBody.y <= body.getMaxY()) {
                        if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                        ) vel.y = VERT_SPEED * ConstVals.PPM
                        else vel.y = 0f
                    } else vel.y = VERT_SPEED * ConstVals.PPM * if (megamanBody.y >= body.y) 1 else -1
                } else vel.y = 0f
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val waterListenerFixture = Fixture(
            body,
            FixtureType.WATER_LISTENER,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), ConstVals.PPM / 2f)
        )
        waterListenerFixture.offsetFromBodyCenter.y = ConstVals.PPM / 4f
        body.addFixture(waterListenerFixture)
        debugShapes.add { waterListenerFixture.getShape() }

        val m = GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val leftFixture = Fixture(body, FixtureType.SIDE, m.copy())
        leftFixture.offsetFromBodyCenter.x = -0.5f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture.getShape() }

        val rightFixture = Fixture(body, FixtureType.SIDE, m.copy())
        rightFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture.getShape() }

        val m1 = GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM)

        val headFixture = Fixture(body, FixtureType.HEAD, m1.copy())
        headFixture.offsetFromBodyCenter.y = 0.375f * ConstVals.PPM
        body.addFixture(headFixture)
        debugShapes.add { headFixture.getShape() }

        val feetFixture = Fixture(body, FixtureType.FEET, m1.copy())
        feetFixture.offsetFromBodyCenter.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture.getShape() }

        val m2 = GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, m2.copy())
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, m2.copy())
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            if (chomping) "chomp" else if (invincible) "gaping" else "swimming"
        }
        val animations = objectMapOf<String, IAnimation>(
            "chomp" to Animation(atlas!!.findRegion("GapingFish/Chomping"), 1, 2, 0.1f),
            "gaping" to Animation(atlas!!.findRegion("GapingFish/Gaping"), 1, 2, 0.15f),
            "swimming" to Animation(atlas!!.findRegion("GapingFish/Swimming"), 1, 2, 0.15f)
        )
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }
}
