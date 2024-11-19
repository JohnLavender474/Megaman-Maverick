package com.megaman.maverick.game.entities.enemies


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class BigFishNeo(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BigFishNeo"
        private const val X_VEL = 4f
        private const val LAUGH_DUR = 1f
        private const val CULL_TIME = 2.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private val laughTimer = Timer(LAUGH_DUR)
    private var minX = 0f
    private var maxX = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("swim", atlas.findRegion("$TAG/swim"))
            regions.put("laugh", atlas.findRegion("$TAG/laugh"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        val xValue = spawnProps.get(ConstKeys.VALUE, Float::class)!! * ConstVals.PPM
        if (xValue < 0f) {
            minX = spawn.x + xValue
            maxX = spawn.x
        } else {
            minX = spawn.x
            maxX = spawn.x + xValue
        }
        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        laughTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!laughTimer.isFinished()) {
                laughTimer.update(delta)
                body.physics.velocity.setZero()
                return@add
            }

            if ((isFacing(Facing.LEFT) && body.getCenter().x <= minX) ||
                (isFacing(Facing.RIGHT) && body.getCenter().x >= maxX)
            ) swapFacing()
            body.physics.velocity.x = X_VEL * facing.value * ConstVals.PPM
        }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (damageable is Megaman) laughTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(4.5f * ConstVals.PPM, 2.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(3.75f * ConstVals.PPM, 2f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM * facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(6f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (!laughTimer.isFinished()) "laugh" else "swim" }
        val animations = objectMapOf<String, IAnimation>(
            "swim" pairTo Animation(regions["swim"]!!, 2, 2, 0.2f, true),
            "laugh" pairTo Animation(regions["laugh"]!!, 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
