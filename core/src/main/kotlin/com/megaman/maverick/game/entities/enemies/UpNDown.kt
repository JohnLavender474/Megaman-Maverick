package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class UpNDown(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable, IAnimatedEntity {

    companion object {
        const val TAG = "UpNDown"
        const val RED_TYPE = "red"
        const val BLUE_TYPE = "blue"
        private const val VEL_X = 4f
        private const val VEL_Y = 3f
        private const val DEVIATION = 2f
        private var redRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private lateinit var type: String

    private var minX = 0f
    private var maxX = 0f
    private var up = false
    private var left = true

    override fun init() {
        if (redRegion == null || blueRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            redRegion = atlas.findRegion("UpNDown/Red")
            blueRegion = atlas.findRegion("UpNDown/Blue")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS))
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        minX = spawn.x - (DEVIATION * ConstVals.PPM)
        maxX = spawn.x + (DEVIATION * ConstVals.PPM)

        up = spawnProps.getOrDefault(ConstKeys.UP, false, Boolean::class)
        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, RED_TYPE, String::class)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (left && body.getX() <= minX) left = false
            else if (!left && body.getX() >= maxX) left = true

            body.physics.velocity.x = VEL_X * ConstVals.PPM * if (left) -1 else 1
            body.physics.velocity.y = VEL_Y * ConstVals.PPM * if (up) 1 else -1
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { type }
        val animations = objectMapOf<String, IAnimation>(
            RED_TYPE pairTo Animation(redRegion!!, 1, 2, 0.1f, true),
            BLUE_TYPE pairTo Animation(blueRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
