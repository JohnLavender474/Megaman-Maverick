package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.entities.IGameEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class TubeBeam(game: MegamanMaverickGame) : AbstractProjectile(game), IDirectionRotatable {

    companion object {
        const val TAG = "TubeBeam"
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    override lateinit var directionRotation: Direction

    private lateinit var trajectory: Vector2

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "TubeBeam")
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        directionRotation = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.set(trajectory)
            val size = if (directionRotation.isHorizontal()) Vector2(2f, 0.85f) else Vector2(0.85f, 2f)
            size.scl(ConstVals.PPM.toFloat())
            body.setSize(size)
            (damagerFixture.rawShape as GameRectangle).setSize(size)
        }
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
        sprite.setSize(2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = if (directionRotation.isHorizontal()) 0f else 90f
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}