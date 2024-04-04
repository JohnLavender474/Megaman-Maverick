package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.motion.RotatingLine
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SigmaRatClaw(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    companion object {
        const val TAG = "SigmaRatClaw"
        private const val DEGREES_ON_RESET = 90f
        private var restRegion: TextureRegion? = null
        private var shockRegion: TextureRegion? = null
        private var swipeRegion: TextureRegion? = null
    }

    enum class SigmaRatClawState {
        REST, SHOCK, SWIPE
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    private lateinit var state: SigmaRatClawState
    private lateinit var rotatingLine: RotatingLine

    private var rotating = true

    override fun init() {
        if (restRegion == null || shockRegion == null || swipeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            restRegion = atlas.findRegion("SigmaRat/ClawClosed")
            shockRegion = atlas.findRegion("SigmaRat/ClawOpen")
            swipeRegion = atlas.findRegion("SigmaRat/ClawFlash")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        rotating = spawnProps.getOrDefault(ConstKeys.ROTATION, true, Boolean::class)
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        rotatingLine = RotatingLine(spawn, ConstVals.PPM.toFloat(), speed * ConstVals.PPM, DEGREES_ON_RESET)
        state = SigmaRatClawState.REST
    }

    internal fun setRotatiing(rotating: Boolean) {
        this.rotating = rotating
        if (rotating) rotatingLine.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (rotating) {
                rotatingLine.update(delta)
                body.setCenter(rotatingLine.getMotionValue())
            }
            // TODO: update state
            // TODO: make sigma rat owner of claw shocks
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        body.color = Color.YELLOW
        debugShapes.add { body }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.35f * ConstVals.PPM
            )
        )
        damagerFixture.offsetFromBodyCenter.y = -0.35f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().set(body))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val swiping = state == SigmaRatClawState.SWIPE
            shieldFixture.active = !swiping
            shieldFixture.rawShape.color = if (!swiping) Color.BLUE else Color.GRAY
            damageableFixture.active = swiping
            damageableFixture.rawShape.color = if (swiping) Color.PURPLE else Color.GRAY
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(1.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
            _sprite.translateY(0.25f * ConstVals.PPM)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                SigmaRatClawState.REST -> "rest"
                SigmaRatClawState.SHOCK -> "shock"
                SigmaRatClawState.SWIPE -> "swipe"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "rest" to Animation(restRegion!!),
            "shock" to Animation(shockRegion!!),
            "swipe" to Animation(swipeRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}