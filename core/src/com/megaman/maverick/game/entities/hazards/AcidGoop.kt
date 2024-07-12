package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing

class AcidGoop(game: MegamanMaverickGame) : GameEntity(game), IDamager, IHazard, ISpritesEntity, IAnimatedEntity,
    IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "AcidGoop"
        private const val GRAVITY = -0.15f
        private const val DISSIPATE_DUR = 0.125f
        private var fallingRegion: TextureRegion? = null
        private var splatRegion: TextureRegion? = null
    }

    private val dissipateTimer = Timer(DISSIPATE_DUR)
    private var dissipating = false

    override fun init() {
        if (fallingRegion == null || splatRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            fallingRegion = atlas.findRegion("AcidGoop/Falling")
            splatRegion = atlas.findRegion("AcidGoop/Landed")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.gravityOn = false
        dissipateTimer.reset()
        dissipating = false
    }

    fun setToFall() {
        body.physics.gravityOn = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (body.isSensing(BodySense.FEET_ON_GROUND)) dissipating = true
        if (dissipating) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) {
                getMegamanMaverickGame().audioMan.playSound(SoundAsset.WHOOSH_SOUND, false)
                spawnSmokePuff()
                kill()
            }
        }
    })

    private fun spawnSmokePuff() {
        val smokePuff = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SMOKE_PUFF)!!
        game.engine.spawn(smokePuff, props(ConstKeys.POSITION to body.getBottomCenterPoint(), ConstKeys.OWNER to this))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -0.125f * ConstVals.PPM
        body.addFixture(feetFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { if (dissipating) "dissipating" else "falling" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipating" to Animation(splatRegion!!, 1, 2, 0.1f, false),
            "falling" to Animation(fallingRegion!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}