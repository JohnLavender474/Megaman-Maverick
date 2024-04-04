package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SigmaRatElectricBallExplosion(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity,
    IAnimatedEntity, IDamager {

    companion object {
        const val TAG = "SigmaRatElectricBallExplosion"
        private const val SHOCK_DUR = 0.5f
        private const val DISSIPATE_DUR = 0.3f
        private var explosionRegion: TextureRegion? = null
        private var dissipateRegion: TextureRegion? = null
    }

    private val shockTimer = Timer(SHOCK_DUR)
    private val dissipateTimer = Timer(DISSIPATE_DUR)

    override fun init() {
        if (explosionRegion == null || dissipateRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            explosionRegion = atlas.findRegion("SigmaRat/ElectricPulse")
            dissipateRegion = atlas.findRegion("SigmaRat/ElectricDissipate")
        }
        super<GameEntity>.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)
        shockTimer.reset()
        dissipateTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        shockTimer.update(delta)
        if (shockTimer.isFinished()) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) kill()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                0.5f * ConstVals.PPM,
                ConstVals.PPM.toFloat()
            )
        )
        body.addFixture(damagerFixture)
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySuppplier: () -> String? = { if (shockTimer.isFinished()) "dissipate" else "pulse" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipate" to Animation(dissipateRegion!!, 1, 3, 0.1f, false),
            "pulse" to Animation(explosionRegion!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySuppplier, animations)
        return AnimationsComponent(this, animator)
    }

}