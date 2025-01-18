package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.UnderwaterFan
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.FallingIcicle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class GapingFish(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "GapingFish"
        private var atlas: TextureAtlas? = null
        private const val HORIZ_SPEED = 2f
        private const val VERT_SPEED = 1.25f
        private const val CHOMP_DUR = 1.25f
    }

    override lateinit var facing: Facing

    private val chompTimer = Timer(CHOMP_DUR)
    private val chomping: Boolean
        get() = !chompTimer.isFinished()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        damageOverrides.putAll(
            UnderwaterFan::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            FallingIcicle::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)

        super.init()

        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        chompTimer.setToEnd()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
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
                "update(): in water = ${body.isSensing(BodySense.IN_WATER)}, invincible = " +
                    "$invincible, chomping = $chomping, position = ${body.getPosition()}"
            )

            chompTimer.update(it)

            val megamanBody = megaman.body

            if (body.getX() >= megamanBody.getMaxX()) facing = Facing.LEFT
            else if (body.getMaxX() <= megamanBody.getX()) facing = Facing.RIGHT

            if (invincible || chomping) body.physics.velocity.setZero()
            else {
                val vel = body.physics.velocity
                vel.x = HORIZ_SPEED * ConstVals.PPM * facing.value
                if (body.isSensing(BodySense.IN_WATER) || megamanBody.getY() < body.getY()) {
                    if (megamanBody.getY() >= body.getY() && megamanBody.getY() <= body.getMaxY()) {
                        if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                        ) vel.y = VERT_SPEED * ConstVals.PPM
                        else vel.y = 0f
                    } else vel.y = VERT_SPEED * ConstVals.PPM * if (megamanBody.getY() >= body.getY()) 1 else -1
                } else vel.y = 0f
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val waterListenerFixture = Fixture(
            body,
            FixtureType.WATER_LISTENER,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), ConstVals.PPM / 2f)
        )
        waterListenerFixture.offsetFromBodyAttachment.y = ConstVals.PPM / 4f
        body.addFixture(waterListenerFixture)
        debugShapes.add { waterListenerFixture }

        val m = GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val leftFixture = Fixture(body, FixtureType.SIDE, m.copy())
        leftFixture.offsetFromBodyAttachment.x = -0.5f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, m.copy())
        rightFixture.offsetFromBodyAttachment.x = 0.5f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        val m1 = GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM)

        val headFixture = Fixture(body, FixtureType.HEAD, m1.copy())
        headFixture.offsetFromBodyAttachment.y = 0.375f * ConstVals.PPM
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, m1.copy())
        feetFixture.offsetFromBodyAttachment.y = -0.375f * ConstVals.PPM
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val m2 = GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, m2.copy())
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, m2.copy())
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            if (chomping) "chomp" else if (invincible) "gaping" else "swimming"
        }
        val animations = objectMapOf<String, IAnimation>(
            "chomp" pairTo Animation(atlas!!.findRegion("GapingFish/Chomping"), 1, 2, 0.1f),
            "gaping" pairTo Animation(atlas!!.findRegion("GapingFish/Gaping"), 1, 2, 0.15f),
            "swimming" pairTo Animation(atlas!!.findRegion("GapingFish/Swimming"), 1, 2, 0.15f)
        )
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }
}
