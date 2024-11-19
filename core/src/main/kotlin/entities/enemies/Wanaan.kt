package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionDelta
import kotlin.reflect.KClass

class Wanaan(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    companion object {
        const val TAG = "Wanaan"
        private var region: TextureRegion? = null
        private const val GRAVITY = -0.15f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    val comingDown: Boolean
        get() = body.getPositionDelta().y < 0f
    val cullPoint: Vector2
        get() = body.getCenter()

    private lateinit var direction: Direction

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Wanaan")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        val impulse = spawnProps.get(ConstKeys.IMPULSE, Float::class)!!
        body.physics.velocity = when (direction) {
            Direction.UP -> Vector2(0f, impulse)
            Direction.DOWN -> Vector2(0f, -impulse)
            Direction.LEFT -> Vector2(-impulse, 0f)
            Direction.RIGHT -> Vector2(impulse, 0f)
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.GREEN
        debugShapes.add { shieldFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(1f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(false, comingDown)
            when (direction) {
                Direction.UP -> _sprite.rotation = 0f
                Direction.DOWN -> _sprite.rotation = 180f
                Direction.LEFT -> _sprite.rotation = 90f
                Direction.RIGHT -> _sprite.rotation = 270f
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}