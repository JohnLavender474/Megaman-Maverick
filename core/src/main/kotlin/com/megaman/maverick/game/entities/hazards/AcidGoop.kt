package com.megaman.maverick.game.entities.hazards


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*

class AcidGoop(game: MegamanMaverickGame) : MegaGameEntity(game), IDamager, IHazard, ISpritesEntity, IAnimatedEntity,
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

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (fallingRegion == null || splatRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            fallingRegion = atlas.findRegion("$TAG/Falling")
            splatRegion = atlas.findRegion("$TAG/Landed")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setTopCenterToPoint(spawn)
        body.physics.gravityOn = false
        dissipateTimer.reset()
        dissipating = false
    }

    fun setToFall() {
        body.physics.gravityOn = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (body.isSensing(BodySense.FEET_ON_GROUND)) dissipating = true
        if (dissipating) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) {
                if (overlapsGameCamera()) playSoundNow(SoundAsset.WHOOSH_SOUND, false)
                spawnSmokePuff()
                destroy()
            }
        }
    })

    private fun spawnSmokePuff() {
        val smokePuff = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SMOKE_PUFF)!!
        smokePuff.spawn(
            props(
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER),
                ConstKeys.OWNER pairTo this
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.125f * ConstVals.PPM
        body.addFixture(feetFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(
                body.getPositionPoint(Position.BOTTOM_CENTER),
                Position.BOTTOM_CENTER
            )
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { if (dissipating) "dissipating" else "falling" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipating" pairTo Animation(splatRegion!!, 1, 2, 0.1f, false),
            "falling" pairTo Animation(fallingRegion!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
