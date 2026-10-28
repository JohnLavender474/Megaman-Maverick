package com.megaman.maverick.game.entities.hazards


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.world.body.*

class LavaDrop(game: MegamanMaverickGame) : MegaGameEntity(game), IDamagerEntity, IHazard, ISpritesEntity, IAnimatedEntity,
    IBodyEntity, ICullableEntity {

    companion object {
        const val TAG = "LavaDrop"
        private const val GRAVITY = -22.5f
        private const val DISSIPATE_DUR = 0.125f
        private var fallingRegion: TextureRegion? = null
        private var splatRegion: TextureRegion? = null
    }

    private val dissipateTimer = Timer(DISSIPATE_DUR)
    private var dissipating = false

    override fun getType() = EntityType.HAZARD

    override fun init(vararg params: Any) {
        if (fallingRegion == null || splatRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            fallingRegion = atlas.findRegion("LavaDrop/Falling")
            splatRegion = atlas.findRegion("LavaDrop/Landed")
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
        val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
        puff.spawn(props(ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER), ConstKeys.OWNER pairTo this))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (dissipating) "dissipating" else "falling" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipating" pairTo Animation(splatRegion!!, 1, 2, 0.1f, false),
            "falling" pairTo Animation(fallingRegion!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
